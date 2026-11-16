package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.TokenRecuperacaoSenhaModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.SessaoRepository;
import br.ivis.identity.repository.TokenRecuperacaoSenhaRepository;
import br.ivis.identity.service.LimiteRecuperacaoSenha;
import br.ivis.identity.service.RecuperacaoSenhaService;

class RecuperacaoSenhaServiceTest {

    private ContaRepository contas;
    private TokenRecuperacaoSenhaRepository tokens;
    private SessaoRepository sessoes;
    private JavaMailSender remetente;
    private PasswordEncoder codificadorSenha;
    private RecuperacaoSenhaService servico;

    @BeforeEach
    void preparar() {
        contas = mock(ContaRepository.class);
        tokens = mock(TokenRecuperacaoSenhaRepository.class);
        sessoes = mock(SessaoRepository.class);
        remetente = mock(JavaMailSender.class);
        codificadorSenha = mock(PasswordEncoder.class);
        servico = new RecuperacaoSenhaService(
                contas,
                tokens,
                sessoes,
                codificadorSenha,
                new LimiteRecuperacaoSenha(5, Duration.ofMinutes(15)),
                remetente,
                Duration.ofMinutes(30),
                "no-reply@ivis.local",
                "http://localhost:8080/redefinir-senha");
    }

    @Test
    void solicitacaoRespondeNeutroSemContaEnaoEnviaEmail() {
        when(contas.findByEmailNormalizado("ausente@example.com")).thenReturn(Optional.empty());

        String resposta = servico.solicitar("Ausente@example.com", "127.0.0.1");

        assertThat(resposta).contains("Se existir uma conta");
        verify(remetente, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void codigoEnviadoEhArmazenadoComoHashEConsumoRedefineSenhaRevogandoSessoes() {
        UUID contaId = UUID.randomUUID();
        ContaModel conta = new ContaModel(contaId, "ALUNO", "aluno@example.com",
                "aluno@example.com", "hash-antigo", Instant.now());
        when(contas.findByEmailNormalizado("aluno@example.com")).thenReturn(Optional.of(conta));
        when(tokens.save(any(TokenRecuperacaoSenhaModel.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(codificadorSenha.encode(any())).thenAnswer(invocacao ->
                "senha-nova-segura".equals(invocacao.getArgument(0)) ? "hash-novo" : "hash-do-codigo");
        when(codificadorSenha.encode("senha-nova-segura")).thenReturn("hash-novo");

        servico.solicitar("aluno@example.com", "127.0.0.1");

        var mensagemCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remetente).send(mensagemCaptor.capture());
        String corpoMensagem = mensagemCaptor.getValue().getText();
        String codigo = corpoMensagem.replaceFirst("(?s).*código de 8 dígitos em até 30 minutos:\\s*([0-9]{8}).*", "$1");
        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(TokenRecuperacaoSenhaModel.class);
        verify(tokens).save(tokenCaptor.capture());
        TokenRecuperacaoSenhaModel registro = tokenCaptor.getValue();
        assertThat(codigo).matches("[0-9]{8}");
        assertThat(corpoMensagem).doesNotContain("#token=", "?token=");
        assertThat(registro.getHashToken()).isEqualTo("hash-do-codigo").isNotEqualTo(codigo);

        when(tokens.buscarAtivosDaContaParaAtualizacao(contaId)).thenReturn(List.of(registro));
        when(codificadorSenha.matches(codigo, "hash-do-codigo")).thenReturn(true);
        when(contas.buscarPorIdParaAtualizacao(contaId)).thenReturn(Optional.of(conta));
        String autorizacao = servico.validarCodigo("aluno@example.com", codigo, "127.0.0.1");
        assertThat(autorizacao).matches("[A-Za-z0-9_-]{43}");
        assertThat(registro.estaValidoEm(Instant.now())).isFalse();
        var registrosCaptor = org.mockito.ArgumentCaptor.forClass(TokenRecuperacaoSenhaModel.class);
        verify(tokens, times(2)).save(registrosCaptor.capture());
        TokenRecuperacaoSenhaModel sessaoRedefinicao = registrosCaptor.getAllValues().get(1);
        assertThat(sessaoRedefinicao.getHashToken()).isEqualTo(hash(autorizacao));
        when(tokens.buscarPorHashParaAtualizacao(hash(autorizacao)))
                .thenReturn(Optional.of(sessaoRedefinicao));
        when(tokens.findByHashToken(hash(autorizacao))).thenReturn(Optional.of(sessaoRedefinicao));
        servico.verificarAutorizacao(autorizacao);
        servico.redefinir(autorizacao, "senha-nova-segura", "127.0.0.1");

        assertThat(conta.getHashSenha()).isEqualTo("hash-novo");
        assertThat(sessaoRedefinicao.estaValidoEm(Instant.now())).isFalse();
        verify(sessoes).revogarTodasDaConta(eq(contaId), any(Instant.class));
        assertThatThrownBy(() -> servico.verificarAutorizacao(autorizacao))
                .isInstanceOf(br.ivis.common.error.ExcecaoNegocio.class);
        assertThatThrownBy(() -> servico.redefinir(autorizacao, "outra-senha-segura", "127.0.0.1"))
                .isInstanceOf(br.ivis.common.error.ExcecaoNegocio.class);
    }

    @Test
    void rejeitaCodigoExpirado() {
        UUID contaId = UUID.randomUUID();
        ContaModel conta = new ContaModel(contaId, "ALUNO", "aluno@example.com",
                "aluno@example.com", "hash-antigo", Instant.now());
        TokenRecuperacaoSenhaModel expirado = new TokenRecuperacaoSenhaModel(
                UUID.randomUUID(), contaId, "hash", Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(60));
        when(contas.findByEmailNormalizado("aluno@example.com")).thenReturn(Optional.of(conta));
        when(tokens.buscarAtivosDaContaParaAtualizacao(contaId)).thenReturn(List.of(expirado));

        assertThatThrownBy(() -> servico.validarCodigo("aluno@example.com", "12345678", "127.0.0.1"))
                .isInstanceOfSatisfying(br.ivis.common.error.ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(sessoes, never()).revogarTodasDaConta(any(UUID.class), any(Instant.class));
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception excecao) {
            throw new AssertionError(excecao);
        }
    }

}
