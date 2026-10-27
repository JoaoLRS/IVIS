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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class ServicoVinculoInstitucionalTest {

    private ContaRepositorio contas;
    private PerfilAlunoRepositorio perfis;
    private ContratoInstitucionalRepositorio contratos;
    private VinculoInstitucionalRepositorio vinculos;
    private ServicoVinculoInstitucional servico;
    private UUID instituicaoId;
    private UUID alunoId;
    private UUID contratoId;
    private Conta instituicao;
    private Conta aluno;
    private ContratoInstitucional contrato;

    @BeforeEach
    void preparar() {
        contas = mock(ContaRepositorio.class);
        perfis = mock(PerfilAlunoRepositorio.class);
        contratos = mock(ContratoInstitucionalRepositorio.class);
        vinculos = mock(VinculoInstitucionalRepositorio.class);
        servico = new ServicoVinculoInstitucional(contas, perfis, contratos, vinculos);

        instituicaoId = UUID.randomUUID();
        alunoId = UUID.randomUUID();
        contratoId = UUID.randomUUID();
        Instant agora = Instant.now();
        instituicao = new Conta(instituicaoId, "INSTITUICAO", "org@example.com",
                "org@example.com", "hash", agora);
        aluno = new Conta(alunoId, "ALUNO", "aluno@example.com", "aluno@example.com", "hash", agora);
        contrato = contratoAtivo(2);
    }

    @Test
    void criaVinculoECalculaCapacidadePelosVinculosAtivos() {
        when(contas.buscarPorIdParaAtualizacao(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(contratos.buscarAtivoParaVinculacao(contratoId, instituicaoId)).thenReturn(Optional.of(contrato));
        when(contas.buscarPorIdParaAtualizacao(alunoId)).thenReturn(Optional.of(aluno));
        when(perfis.existsById(alunoId)).thenReturn(true);
        when(vinculos.existsByContaAlunoIdAndSituacao(alunoId, "ATIVO")).thenReturn(false);
        when(vinculos.countByContratoIdAndSituacao(contratoId, "ATIVO")).thenReturn(1L, 2L);
        when(vinculos.saveAndFlush(any(VinculoInstitucional.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(vinculos.buscarResumoAtivoDaInstituicao(any(), any()))
                .thenAnswer(invocacao -> Optional.of(new VinculoInstitucionalResumo(
                        invocacao.getArgument(0), contratoId, alunoId, "Aluno", "aluno@example.com", Instant.now())));

        var resposta = servico.criarVinculo(
                instituicaoId, new ContratoInstitucionalResposta.CriacaoVinculoRequisicao(contratoId, alunoId));

        assertThat(resposta.vinculo().contaAlunoId()).isEqualTo(alunoId);
        assertThat(resposta.capacidade().vagasUtilizadas()).isEqualTo(2);
        assertThat(resposta.capacidade().vagasDisponiveis()).isZero();
    }

    @Test
    void rejeitaCriacaoQuandoCapacidadeEsgotada() {
        when(contas.buscarPorIdParaAtualizacao(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(contratos.buscarAtivoParaVinculacao(contratoId, instituicaoId)).thenReturn(Optional.of(contrato));
        when(contas.buscarPorIdParaAtualizacao(alunoId)).thenReturn(Optional.of(aluno));
        when(perfis.existsById(alunoId)).thenReturn(true);
        when(vinculos.existsByContaAlunoIdAndSituacao(alunoId, "ATIVO")).thenReturn(false);
        when(vinculos.countByContratoIdAndSituacao(contratoId, "ATIVO")).thenReturn(2L);

        assertThatThrownBy(() -> servico.criarVinculo(
                        instituicaoId,
                        new ContratoInstitucionalResposta.CriacaoVinculoRequisicao(contratoId, alunoId)))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(vinculos, never()).saveAndFlush(any());
    }

    @Test
    void naoRevelaVinculoDeOutraInstituicaoDuranteRemocao() {
        UUID outroVinculo = UUID.randomUUID();
        when(contas.buscarPorIdParaAtualizacao(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(vinculos.findAtivoByIdAndInstituicao(outroVinculo, instituicaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.removerVinculo(instituicaoId, outroVinculo))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(contratos, never()).buscarDaInstituicaoParaAtualizacao(any(), any());
    }

    @Test
    void converteViolacaoIndiceUnicoEmConflitoDeVinculo() {
        when(contas.buscarPorIdParaAtualizacao(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(contratos.buscarAtivoParaVinculacao(contratoId, instituicaoId)).thenReturn(Optional.of(contrato));
        when(contas.buscarPorIdParaAtualizacao(alunoId)).thenReturn(Optional.of(aluno));
        when(perfis.existsById(alunoId)).thenReturn(true);
        when(vinculos.existsByContaAlunoIdAndSituacao(alunoId, "ATIVO")).thenReturn(false);
        when(vinculos.countByContratoIdAndSituacao(contratoId, "ATIVO")).thenReturn(0L);
        var constraint = new org.hibernate.exception.ConstraintViolationException(
                "unique", null, "uq_vinculos_aluno_ativo");
        when(vinculos.saveAndFlush(any(VinculoInstitucional.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", constraint));

        assertThatThrownBy(() -> servico.criarVinculo(
                        instituicaoId,
                        new ContratoInstitucionalResposta.CriacaoVinculoRequisicao(contratoId, alunoId)))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getCodigo()).isEqualTo("aluno_ja_vinculado"));
    }

    private ContratoInstitucional contratoAtivo(int capacidade) {
        return new ContratoInstitucional(
                contratoId, instituicaoId, UUID.randomUUID(), "ATIVO", capacidade, Instant.now(), null);
    }
}
