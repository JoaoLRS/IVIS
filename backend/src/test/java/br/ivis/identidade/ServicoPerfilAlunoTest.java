package br.ivis.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ivis.comum.erro.ExcecaoNegocio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.ivis.identidade.dto.ContratoPerfilAluno;
import br.ivis.identidade.modelo.HabilidadePerfil;
import br.ivis.identidade.modelo.VagaInteresse;
import br.ivis.identidade.repositorio.HabilidadePerfilRepositorio;
import br.ivis.identidade.repositorio.PerfilAlunoRepositorio;
import br.ivis.identidade.repositorio.RequisitoVagaRepositorio;
import br.ivis.identidade.repositorio.VagaInteresseRepositorio;
import br.ivis.identidade.servico.ServicoPerfilAluno;

class ServicoPerfilAlunoTest {

    private PerfilAlunoRepositorio perfis;
    private HabilidadePerfilRepositorio habilidades;
    private VagaInteresseRepositorio vagas;
    private RequisitoVagaRepositorio requisitos;
    private ServicoPerfilAluno servico;
    private UUID titular;

    @BeforeEach
    void preparar() {
        perfis = mock(PerfilAlunoRepositorio.class);
        habilidades = mock(HabilidadePerfilRepositorio.class);
        vagas = mock(VagaInteresseRepositorio.class);
        requisitos = mock(RequisitoVagaRepositorio.class);
        servico = new ServicoPerfilAluno(perfis, habilidades, vagas, requisitos);
        titular = UUID.randomUUID();
        when(perfis.existsById(titular)).thenReturn(true);
    }

    @Test
    void listaSomenteHabilidadesDoTitularEOrdenaARespostaDoRepositorio() {
        when(habilidades.findAllByContaAlunoIdOrderByNomeNormalizado(titular))
                .thenReturn(List.of(habilidade(titular, "Java")));

        var resposta = servico.listarHabilidades(titular);

        assertThat(resposta).extracting("nome").containsExactly("Java");
        verify(habilidades).findAllByContaAlunoIdOrderByNomeNormalizado(titular);
    }

    @Test
    void naoPermiteDuplicidadeDeHabilidadeIgnorandoMaiusculasEEspacos() {
        when(habilidades.existsByContaAlunoIdAndNomeNormalizado(titular, "vue 3")).thenReturn(true);

        assertThatThrownBy(() -> servico.criarHabilidade(
                        titular, new ContratoPerfilAluno.HabilidadeRequisicao(" Vue 3 ", "Front-end", 4)))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(habilidades, never()).saveAndFlush(any());
    }

    @Test
    void impedeInstituicaoDeAcessarRecursosDeAluno() {
        UUID contaInstituicao = UUID.randomUUID();
        when(perfis.existsById(contaInstituicao)).thenReturn(false);

        assertThatThrownBy(() -> servico.listarHabilidades(contaInstituicao))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(habilidades, never()).findAllByContaAlunoIdOrderByNomeNormalizado(any());
    }

    @Test
    void idsDeOutroTitularNaoLocalizamHabilidade() {
        UUID idOutraConta = UUID.randomUUID();
        when(habilidades.findByIdAndContaAlunoId(idOutraConta, titular)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.excluirHabilidade(titular, idOutraConta))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(habilidades, never()).delete(any());
    }

    @Test
    void rejeitaRequisitosVaziosOuDuplicados() {
        var comDuplicidade = new ContratoPerfilAluno.VagaRequisicao(
                "Desenvolvedor", "Empresa", "Pleno", null, List.of("Java", " java "));
        var vazio = new ContratoPerfilAluno.VagaRequisicao(
                "Desenvolvedor", "Empresa", "Pleno", null, List.of("  "));

        assertThatThrownBy(() -> servico.criarVaga(titular, comDuplicidade))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> servico.criarVaga(titular, vazio))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(vagas, never()).saveAndFlush(any());
    }

    @Test
    void excluirVagaArquivaSemApagarRegistroUsadoComoReferenciaHistorica() {
        UUID vagaId = UUID.randomUUID();
        VagaInteresse vaga = new VagaInteresse(
                vagaId, titular, "Desenvolvedor", "Empresa", "Pleno", null, Instant.now());
        when(vagas.findByIdAndContaAlunoIdAndArquivadoEmIsNull(vagaId, titular))
                .thenReturn(Optional.of(vaga));

        servico.excluirVaga(titular, vagaId);

        assertThat(vaga.estaAtiva()).isFalse();
        assertThat(vaga.getArquivadoEm()).isNotNull();
        verify(vagas, never()).delete(any());
    }

    private static HabilidadePerfil habilidade(UUID contaId, String nome) {
        return new HabilidadePerfil(
                UUID.randomUUID(), contaId, nome, nome.toLowerCase(), null, (short) 4, Instant.now());
    }
}
