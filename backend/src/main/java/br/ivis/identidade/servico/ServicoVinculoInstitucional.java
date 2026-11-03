package br.ivis.identidade.servico;

import static br.ivis.identidade.dto.ContratoInstitucionalResposta.Capacidade;
import static br.ivis.identidade.dto.ContratoInstitucionalResposta.CriacaoVinculoRequisicao;
import static br.ivis.identidade.dto.ContratoInstitucionalResposta.CriacaoVinculoResposta;
import static br.ivis.identidade.dto.ContratoInstitucionalResposta.CriacaoVinculoPorEmailRequisicao;
import static br.ivis.identidade.dto.ContratoInstitucionalResposta.CadastroAlunoRequisicao;
import static br.ivis.identidade.dto.ContratoInstitucionalResposta.CadastroAlunoResposta;

import br.ivis.comum.erro.ExcecaoNegocio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identidade.dto.VinculoInstitucionalResumo;
import br.ivis.identidade.dto.ContratoIdentidade;
import br.ivis.identidade.modelo.Conta;
import br.ivis.identidade.modelo.ContratoInstitucional;
import br.ivis.identidade.modelo.PerfilAluno;
import br.ivis.identidade.modelo.VinculoInstitucional;
import br.ivis.identidade.repositorio.ContaRepositorio;
import br.ivis.identidade.repositorio.ContratoInstitucionalRepositorio;
import br.ivis.identidade.repositorio.PerfilAlunoRepositorio;
import br.ivis.identidade.repositorio.VinculoInstitucionalRepositorio;

@Service
public class ServicoVinculoInstitucional {

    private static final String VINCULO_ATIVO = "ATIVO";
    private static final String CONSTRAINT_ALUNO_VINCULADO = "uq_vinculos_aluno_ativo";

    private final ContaRepositorio contas;
    private final PerfilAlunoRepositorio perfisAlunos;
    private final ContratoInstitucionalRepositorio contratos;
    private final VinculoInstitucionalRepositorio vinculos;
    private final ServicoIdentidade identidade;

    @Autowired
    public ServicoVinculoInstitucional(
            ContaRepositorio contas,
            PerfilAlunoRepositorio perfisAlunos,
            ContratoInstitucionalRepositorio contratos,
            VinculoInstitucionalRepositorio vinculos,
            ServicoIdentidade identidade) {
        this.contas = contas;
        this.perfisAlunos = perfisAlunos;
        this.contratos = contratos;
        this.vinculos = vinculos;
        this.identidade = identidade;
    }

    public ServicoVinculoInstitucional(
            ContaRepositorio contas,
            PerfilAlunoRepositorio perfisAlunos,
            ContratoInstitucionalRepositorio contratos,
            VinculoInstitucionalRepositorio vinculos) {
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
        Conta aluno = contas.findByEmailNormalizado(requisicao.emailAluno().trim().toLowerCase(java.util.Locale.ROOT))
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "ALUNO".equals(conta.getTipoConta()))
                .filter(conta -> perfisAlunos.existsById(conta.getId()))
                .orElseThrow(ServicoVinculoInstitucional::alunoNaoEncontrado);
        return criarVinculo(instituicaoId, requisicao.contratoId(), aluno.getId());
    }

    @Transactional
    public CadastroAlunoResposta cadastrarEVincularAluno(UUID instituicaoId, CadastroAlunoRequisicao requisicao) {
        Conta instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        ContratoInstitucional contrato = contratos.buscarAtivoParaVinculacao(
                        requisicao.contratoId(), instituicao.getId())
                .orElseThrow(ServicoVinculoInstitucional::contratoNaoEncontrado);
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
        Conta instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        ContratoInstitucional contrato = contratos.buscarAtivoParaVinculacao(
                        contratoId, instituicao.getId())
                .orElseThrow(ServicoVinculoInstitucional::contratoNaoEncontrado);

        Conta aluno = contas.buscarPorIdParaAtualizacao(contaAlunoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "ALUNO".equals(conta.getTipoConta()))
                .filter(conta -> perfisAlunos.existsById(conta.getId()))
                .orElseThrow(ServicoVinculoInstitucional::alunoNaoEncontrado);

        if (vinculos.existsByContaAlunoIdAndSituacao(aluno.getId(), VINCULO_ATIVO)) {
            throw alunoJaVinculado();
        }
        long utilizados = vinculos.countByContratoIdAndSituacao(contrato.getId(), VINCULO_ATIVO);
        if (utilizados >= contrato.getQuantidadeVagas()) {
            throw capacidadeEsgotada();
        }

        Instant agora = Instant.now();
        VinculoInstitucional vinculo = new VinculoInstitucional(
                UUID.randomUUID(), contrato.getId(), aluno.getId(), instituicao.getId(), agora);
        try {
            vinculos.saveAndFlush(vinculo);
        } catch (DataIntegrityViolationException excecao) {
            if (violacaoVinculoAtivoUnico(excecao)) {
                throw alunoJaVinculado();
            }
            throw excecao;
        }
        PerfilAluno perfil = perfisAlunos.findByContaId(aluno.getId())
                .orElseThrow(ServicoVinculoInstitucional::alunoNaoEncontrado);
        perfil.atualizarInstituicao(instituicao.getId(), agora);
        perfisAlunos.save(perfil);
        vinculos.registrarAuditoria(instituicaoId, "VINCULO_ALUNO_CRIADO", contratoId, vinculo.getId(), aluno.getId());
        VinculoInstitucionalResumo resumo = vinculos
                .buscarResumoAtivoDaInstituicao(vinculo.getId(), instituicao.getId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);
        return new CriacaoVinculoResposta(resumo, resumirCapacidade(contrato));
    }

    @Transactional
    public void removerVinculo(UUID instituicaoId, UUID vinculoId) {
        Conta instituicao = carregarInstituicaoParaAtualizacao(instituicaoId);
        VinculoInstitucional referencia = vinculos
                .findAtivoByIdAndInstituicao(vinculoId, instituicao.getId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);

        ContratoInstitucional contrato = contratos.buscarDaInstituicaoParaAtualizacao(
                        referencia.getContratoId(), instituicao.getId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);
        contas.buscarPorIdParaAtualizacao(referencia.getContaAlunoId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);

        VinculoInstitucional vinculo = vinculos
                .findAtivoByIdAndContrato(vinculoId, contrato.getId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);
        vinculo.encerrar(Instant.now());
        vinculos.save(vinculo);
        vinculos.registrarAuditoria(instituicaoId, "VINCULO_ALUNO_REMOVIDO", contrato.getId(), vinculoId, vinculo.getContaAlunoId());
        PerfilAluno perfilAluno = perfisAlunos.findByContaId(vinculo.getContaAlunoId())
                .orElseThrow(ServicoVinculoInstitucional::vinculoNaoEncontrado);
        if (instituicao.getId().equals(perfilAluno.getObjetoInstituicao())) {
            perfilAluno.atualizarInstituicao(null, Instant.now());
            perfisAlunos.save(perfilAluno);
        }
    }

    private Conta carregarInstituicaoParaAtualizacao(UUID instituicaoId) {
        return contas.buscarPorIdParaAtualizacao(instituicaoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "INSTITUICAO".equals(conta.getTipoConta()))
                .orElseThrow(ServicoVinculoInstitucional::instituicaoNaoEncontrada);
    }

    private void validarInstituicao(UUID instituicaoId) {
        contas.findById(instituicaoId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .filter(conta -> "INSTITUICAO".equals(conta.getTipoConta()))
                .orElseThrow(ServicoVinculoInstitucional::instituicaoNaoEncontrada);
    }

    private Capacidade resumirCapacidade(ContratoInstitucional contrato) {
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
