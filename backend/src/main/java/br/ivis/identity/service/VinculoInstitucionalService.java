package br.ivis.identity.service;

import static br.ivis.identity.dto.ContratoInstitucionalResposta.Capacidade;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoResposta;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoPorEmailRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CadastroAlunoRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CadastroAlunoResposta;

import br.ivis.common.error.ExcecaoNegocio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identity.dto.VinculoInstitucionalResumo;
import br.ivis.identity.dto.ContratoIdentidade;
import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.ContratoInstitucionalModel;
import br.ivis.identity.model.PerfilAlunoModel;
import br.ivis.identity.model.VinculoInstitucionalModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.ContratoInstitucionalRepository;
import br.ivis.identity.repository.PerfilAlunoRepository;
import br.ivis.identity.repository.VinculoInstitucionalRepository;

@Service
public class VinculoInstitucionalService {

    private static final String VINCULO_ATIVO = "ATIVO";
    private static final String CONSTRAINT_ALUNO_VINCULADO = "uq_vinculos_aluno_ativo";

    private final ContaRepository contas;
    private final PerfilAlunoRepository perfisAlunos;
    private final ContratoInstitucionalRepository contratos;
    private final VinculoInstitucionalRepository vinculos;
    private final IdentidadeService identidade;

    @Autowired
    public VinculoInstitucionalService(
            ContaRepository contas,
            PerfilAlunoRepository perfisAlunos,
            ContratoInstitucionalRepository contratos,
            VinculoInstitucionalRepository vinculos,
            IdentidadeService identidade) {
        this.contas = contas;
        this.perfisAlunos = perfisAlunos;
        this.contratos = contratos;
        this.vinculos = vinculos;
        this.identidade = identidade;
    }

    public VinculoInstitucionalService(
            ContaRepository contas,
            PerfilAlunoRepository perfisAlunos,
            ContratoInstitucionalRepository contratos,
            VinculoInstitucionalRepository vinculos) {
        this(contas, perfisAlunos, contratos, vinculos, null);
    }

    @Transactional(readOnly = true)
    public List<VinculoInstitucionalResumo> listarAlunos(UUID instituicaoId) {
        validarInstituicao(instituicaoId);
        return vinculos.listarAtivosDaInstituicao(instituicaoId);
    }

    @Transactional(readOnly = true)
    public List<Capacidade> resumirCapacidade(UUID instituicaoId) {
        validarInstituicao(instituicaoId);
        return contratos.listarAtivosDaInstituicao(instituicaoId).stream()
                .map(this::resumirCapacidade)
                .toList();
    }

    @Transactional
    public CriacaoVinculoResposta criarVinculo(UUID instituicaoId, CriacaoVinculoRequisicao requisicao) {
        return criarVinculo(instituicaoId, requisicao.contratoId(), requisicao.contaAlunoId());
    }

    @Transactional
    public CriacaoVinculoResposta criarVinculoPorEmail(UUID instituicaoId, CriacaoVinculoPorEmailRequisicao requisicao) {
        carregarInstituicaoParaAtualizacao(instituicaoId);
        ContaModel aluno = contas.findByEmailNormalizado(requisicao.emailAluno().trim().toLowerCase(java.util.Locale.ROOT))
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "ALUNO".equals(conta.getTipoConta()))
                .filter(conta -> perfisAlunos.existsById(conta.getId()))
                .orElseThrow(VinculoInstitucionalService::alunoNaoEncontrado);
        return criarVinculo(instituicaoId, requisicao.contratoId(), aluno.getId());
    }

    @Transactional
    public CadastroAlunoResposta cadastrarEVincularAluno(UUID instituicaoId, CadastroAlunoRequisicao requisicao) {
        ContaModel instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        ContratoInstitucionalModel contrato = contratos.buscarAtivoParaVinculacao(
                        requisicao.contratoId(), instituicao.getId())
                .orElseThrow(VinculoInstitucionalService::contratoNaoEncontrado);
        if (vinculos.countByContratoIdAndSituacao(contrato.getId(), VINCULO_ATIVO)
                >= contrato.getQuantidadeVagas()) {
            throw capacidadeEsgotada();
        }

        ContratoIdentidade.ContaResposta aluno = identidade.cadastrarAluno(
                new ContratoIdentidade.CadastroAlunoRequisicao(
                        requisicao.nome(), requisicao.email(), requisicao.cpf(), requisicao.telefone(),
                        requisicao.endereco(), requisicao.senha()));
        CriacaoVinculoResposta vinculacao = criarVinculo(instituicao.getId(), contrato.getId(), aluno.id());
        return new CadastroAlunoResposta(
                new ContratoIdentidade.ContaResposta(aluno.id(), aluno.tipoConta(), aluno.email(), aluno.nome(),
                        null, aluno.cpf(), aluno.telefone(), aluno.endereco(), instituicao.getId(), null),
                vinculacao);
    }

    private CriacaoVinculoResposta criarVinculo(UUID instituicaoId, UUID contratoId, UUID contaAlunoId) {
        ContaModel instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        ContratoInstitucionalModel contrato = contratos.buscarAtivoParaVinculacao(
                        contratoId, instituicao.getId())
                .orElseThrow(VinculoInstitucionalService::contratoNaoEncontrado);

        ContaModel aluno = contas.buscarPorIdParaAtualizacao(contaAlunoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "ALUNO".equals(conta.getTipoConta()))
                .filter(conta -> perfisAlunos.existsById(conta.getId()))
                .orElseThrow(VinculoInstitucionalService::alunoNaoEncontrado);

        if (vinculos.existsByContaAlunoIdAndSituacao(aluno.getId(), VINCULO_ATIVO)) {
            throw alunoJaVinculado();
        }
        long utilizados = vinculos.countByContratoIdAndSituacao(contrato.getId(), VINCULO_ATIVO);
        if (utilizados >= contrato.getQuantidadeVagas()) {
            throw capacidadeEsgotada();
        }

        Instant agora = Instant.now();
        VinculoInstitucionalModel vinculo = new VinculoInstitucionalModel(
                UUID.randomUUID(), contrato.getId(), aluno.getId(), instituicao.getId(), agora);
        try {
            vinculos.saveAndFlush(vinculo);
        } catch (DataIntegrityViolationException excecao) {
            if (violacaoVinculoAtivoUnico(excecao)) {
                throw alunoJaVinculado();
            }
            throw excecao;
        }
        PerfilAlunoModel perfil = perfisAlunos.findByContaId(aluno.getId())
                .orElseThrow(VinculoInstitucionalService::alunoNaoEncontrado);
        perfil.atualizarInstituicao(instituicao.getId(), agora);
        perfisAlunos.save(perfil);
        vinculos.registrarAuditoria(instituicaoId, "VINCULO_ALUNO_CRIADO", contratoId, vinculo.getId(), aluno.getId());
        VinculoInstitucionalResumo resumo = vinculos
                .buscarResumoAtivoDaInstituicao(vinculo.getId(), instituicao.getId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);
        return new CriacaoVinculoResposta(resumo, resumirCapacidade(contrato));
    }

    @Transactional
    public void removerVinculo(UUID instituicaoId, UUID vinculoId) {
        ContaModel instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        VinculoInstitucionalModel referencia = vinculos
                .findAtivoByIdAndInstituicao(vinculoId, instituicao.getId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);

        ContratoInstitucionalModel contrato = contratos.buscarDaInstituicaoParaAtualizacao(
                        referencia.getContratoId(), instituicao.getId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);
        contas.buscarPorIdParaAtualizacao(referencia.getContaAlunoId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);

        VinculoInstitucionalModel vinculo = vinculos
                .findAtivoByIdAndContrato(vinculoId, contrato.getId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);
        vinculo.encerrar(Instant.now());
        vinculos.save(vinculo);
        vinculos.registrarAuditoria(instituicaoId, "VINCULO_ALUNO_REMOVIDO", contrato.getId(), vinculoId, vinculo.getContaAlunoId());
        PerfilAlunoModel perfilAluno = perfisAlunos.findByContaId(vinculo.getContaAlunoId())
                .orElseThrow(VinculoInstitucionalService::vinculoNaoEncontrado);
        if (instituicao.getId().equals(perfilAluno.getObjetoInstituicao())) {
            perfilAluno.atualizarInstituicao(null, Instant.now());
            perfisAlunos.save(perfilAluno);
        }
    }

    private ContaModel carregarInstituicaoParaAtualizacao(UUID instituicaoId) {
        return contas.buscarPorIdParaAtualizacao(instituicaoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "INSTITUICAO".equals(conta.getTipoConta()))
                .orElseThrow(VinculoInstitucionalService::instituicaoNaoEncontrada);
    }

    private void validarInstituicao(UUID instituicaoId) {
        contas.findById(instituicaoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "INSTITUICAO".equals(conta.getTipoConta()))
                .orElseThrow(VinculoInstitucionalService::instituicaoNaoEncontrada);
    }

    private Capacidade resumirCapacidade(ContratoInstitucionalModel contrato) {
        long utilizados = vinculos.countByContratoIdAndSituacao(contrato.getId(), VINCULO_ATIVO);
        if (utilizados > contrato.getQuantidadeVagas()) {
            throw new IllegalStateException("A quantidade de vínculos ativos excede a capacidade contratada.");
        }
        return new Capacidade(
                contrato.getId(),
                contrato.getQuantidadeVagas(),
                utilizados,
                contrato.getQuantidadeVagas() - utilizados);
    }

    private static boolean violacaoVinculoAtivoUnico(DataIntegrityViolationException excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && CONSTRAINT_ALUNO_VINCULADO.equals(violacao.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private static ExcecaoNegocio instituicaoNaoEncontrada() {
        return new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado",
                "Esta operação está disponível somente para contas de instituição.");
    }

    private static ExcecaoNegocio contratoNaoEncontrado() {
        return new ExcecaoNegocio(HttpStatus.NOT_FOUND, "contrato_nao_encontrado",
                "Contrato institucional ativo não encontrado.");
    }

    private static ExcecaoNegocio alunoNaoEncontrado() {
        return new ExcecaoNegocio(HttpStatus.NOT_FOUND, "aluno_nao_encontrado",
                "Aluno não encontrado.");
    }

    private static ExcecaoNegocio vinculoNaoEncontrado() {
        return new ExcecaoNegocio(HttpStatus.NOT_FOUND, "vinculo_nao_encontrado",
                "Vínculo ativo não encontrado.");
    }

    private static ExcecaoNegocio alunoJaVinculado() {
        return new ExcecaoNegocio(HttpStatus.CONFLICT, "aluno_ja_vinculado",
                "O aluno já possui um vínculo institucional ativo.");
    }

    private static ExcecaoNegocio capacidadeEsgotada() {
        return new ExcecaoNegocio(HttpStatus.CONFLICT, "capacidade_esgotada",
                "Não há vagas institucionais disponíveis neste contrato.");
    }
}
