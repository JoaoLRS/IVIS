package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ivis.common.error.ExcecaoNegocio;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import br.ivis.identity.dto.ContratoInstitucionalResposta;
import br.ivis.identity.dto.VinculoInstitucionalResumo;
import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.ContratoInstitucionalModel;
import br.ivis.identity.model.PerfilAlunoModel;
import br.ivis.identity.model.VinculoInstitucionalModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.ContratoInstitucionalRepository;
import br.ivis.identity.repository.PerfilAlunoRepository;
import br.ivis.identity.repository.VinculoInstitucionalRepository;
import br.ivis.identity.service.VinculoInstitucionalService;

class VinculoInstitucionalServiceTest {

    private ContaRepository contas;
    private PerfilAlunoRepository perfis;
    private ContratoInstitucionalRepository contratos;
    private VinculoInstitucionalRepository vinculos;
    private VinculoInstitucionalService servico;
    private UUID instituicaoId;
    private UUID alunoId;
    private UUID contratoId;
    private ContaModel instituicao;
    private ContaModel aluno;
    private PerfilAlunoModel perfilAluno;
    private ContratoInstitucionalModel contrato;

    @BeforeEach
    void preparar() {
        contas = mock(ContaRepository.class);
        perfis = mock(PerfilAlunoRepository.class);
        contratos = mock(ContratoInstitucionalRepository.class);
        vinculos = mock(VinculoInstitucionalRepository.class);
        servico = new VinculoInstitucionalService(contas, perfis, contratos, vinculos);

        instituicaoId = UUID.randomUUID();
        alunoId = UUID.randomUUID();
        contratoId = UUID.randomUUID();
        Instant agora = Instant.now();
        instituicao = new ContaModel(instituicaoId, "INSTITUICAO", "org@example.com",
                "org@example.com", "hash", agora);
        aluno = new ContaModel(alunoId, "ALUNO", "aluno@example.com", "aluno@example.com", "hash", agora);
        perfilAluno = new PerfilAlunoModel(alunoId, "Aluno", agora);
        contrato = contratoAtivo(2);
    }

    @Test
    void criaVinculoECalculaCapacidadePelosVinculosAtivos() {
        when(contas.buscarPorIdParaAtualizacao(instituicaoId)).thenReturn(Optional.of(instituicao));
        when(contratos.buscarAtivoParaVinculacao(contratoId, instituicaoId)).thenReturn(Optional.of(contrato));
        when(contas.buscarPorIdParaAtualizacao(alunoId)).thenReturn(Optional.of(aluno));
        when(perfis.existsById(alunoId)).thenReturn(true);
        when(perfis.findByContaId(alunoId)).thenReturn(Optional.of(perfilAluno));
        when(vinculos.existsByContaAlunoIdAndSituacao(alunoId, "ATIVO")).thenReturn(false);
        when(vinculos.countByContratoIdAndSituacao(contratoId, "ATIVO")).thenReturn(1L, 2L);
        when(vinculos.saveAndFlush(any(VinculoInstitucionalModel.class)))
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
        when(vinculos.saveAndFlush(any(VinculoInstitucionalModel.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", constraint));

        assertThatThrownBy(() -> servico.criarVinculo(
                        instituicaoId,
                        new ContratoInstitucionalResposta.CriacaoVinculoRequisicao(contratoId, alunoId)))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getCodigo()).isEqualTo("aluno_ja_vinculado"));
    }

    private ContratoInstitucionalModel contratoAtivo(int capacidade) {
        return new ContratoInstitucionalModel(
                contratoId, instituicaoId, UUID.randomUUID(), "ATIVO", capacidade, Instant.now(), null);
    }
}
