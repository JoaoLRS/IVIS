package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.identity.dto.ContratoIdentidade.AtualizacaoContaRequisicao;
import br.ivis.identity.dto.ContratoIdentidade.LoginRequisicao;
import br.ivis.identity.dto.ContratoIdentidade.TrocaSenhaRequisicao;
import br.ivis.identity.model.ContaModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.PerfilAlunoRepository;
import br.ivis.identity.repository.PerfilInstituicaoRepository;
import br.ivis.identity.repository.SessaoRepository;
import br.ivis.identity.service.IdentidadeService;
import br.ivis.identity.service.LimiteTentativasLogin;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class IdentidadeServiceTest {
    private ContaRepository contas;
    private PerfilAlunoRepository alunos;
    private PerfilInstituicaoRepository instituicoes;
    private SessaoRepository sessoes;
    private PasswordEncoder senhas;
    private LimiteTentativasLogin limite;
    private IdentidadeService servico;
    private UUID contaId;
    private ContaModel conta;

    @BeforeEach
    void preparar() {
        contas = mock(ContaRepository.class);
        alunos = mock(PerfilAlunoRepository.class);
        instituicoes = mock(PerfilInstituicaoRepository.class);
        sessoes = mock(SessaoRepository.class);
        senhas = mock(PasswordEncoder.class);
        limite = mock(LimiteTentativasLogin.class);
        servico = new IdentidadeService(contas, alunos, instituicoes, sessoes,
                senhas, mock(JwtEncoder.class), limite);
        contaId = UUID.randomUUID();
        conta = new ContaModel(contaId, "ALUNO", "aluno@ivis.test", "aluno@ivis.test",
                "hash-antigo", Instant.now());
    }

    @Test
    void loginDeContaInexistenteEhNeutroEContaComoTentativaFalha() {
        when(contas.findByEmailNormalizado("ausente@ivis.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.autenticar(
                new LoginRequisicao(" AUSENTE@IVIS.TEST ", "Senha#123"), "127.0.0.1"))
                .isInstanceOfSatisfying(ExcecaoNegocio.class, erro -> {
                    assertThat(erro.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(erro.getCodigo()).isEqualTo("credenciais_invalidas");
                });

        verify(limite).verificar("ausente@ivis.test", "127.0.0.1");
        verify(limite).registrarFalha("ausente@ivis.test", "127.0.0.1");
        verify(senhas).matches(any(), any());
        verify(sessoes, never()).save(any());
    }

    @Test
    void camposDeInstituicaoSaoRecusadosNaContaDeAluno() {
        when(contas.buscarPorIdParaAtualizacao(contaId)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> servico.atualizarMinhaConta(contaId,
                new AtualizacaoContaRequisicao(null, null, "Instituto indevido", null,
                        null, null, null, null)))
                .isInstanceOfSatisfying(ExcecaoNegocio.class, erro -> {
                    assertThat(erro.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(erro.getCampos()).extracting("campo").contains("nomeInstituicao");
                });

        verify(alunos, never()).saveAndFlush(any());
        verify(contas, never()).saveAndFlush(any());
    }

    @Test
    void senhaAtualIncorretaNaoRevogaSessoes() {
        when(contas.buscarPorIdParaAtualizacao(contaId)).thenReturn(Optional.of(conta));
        when(senhas.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> servico.trocarSenha(contaId,
                new TrocaSenhaRequisicao("errada", "NovaSenha#123")))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        erro -> {
                            assertThat(erro.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                            assertThat(erro.getCodigo()).isEqualTo("senha_atual_invalida");
                        });

        assertThat(conta.getHashSenha()).isEqualTo("hash-antigo");
        verify(sessoes, never()).revogarTodasDaConta(any(), any());
    }

    @Test
    void trocaValidaAtualizaHashERevogaTodasAsRenovacoes() {
        when(contas.buscarPorIdParaAtualizacao(contaId)).thenReturn(Optional.of(conta));
        when(senhas.matches("Senha#123", "hash-antigo")).thenReturn(true);
        when(senhas.encode("NovaSenha#123")).thenReturn("hash-novo");

        servico.trocarSenha(contaId, new TrocaSenhaRequisicao("Senha#123", "NovaSenha#123"));

        assertThat(conta.getHashSenha()).isEqualTo("hash-novo");
        verify(sessoes).revogarTodasDaConta(any(), any());
    }

    @Test
    void tokenDeRenovacaoMalformadoNaoConsultaBanco() {
        assertThatThrownBy(() -> servico.renovar("token-invalido"))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        erro -> assertThat(erro.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(sessoes, never()).buscarPorHashParaAtualizacao(any());
    }
}
