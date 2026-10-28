package br.ivis.identidade;

import static br.ivis.identidade.ContratoPerfilAluno.HabilidadeRequisicao;
import static br.ivis.identidade.ContratoPerfilAluno.HabilidadeResposta;
import static br.ivis.identidade.ContratoPerfilAluno.VagaRequisicao;
import static br.ivis.identidade.ContratoPerfilAluno.VagaResposta;

import br.ivis.comum.erro.ErroCampo;
import br.ivis.comum.erro.ExcecaoNegocio;
import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoPerfilAluno {

    private static final int MAXIMO_REQUISITOS = 20;

    private final PerfilAlunoRepositorio perfisAlunos;
    private final HabilidadePerfilRepositorio habilidades;
    private final VagaInteresseRepositorio vagas;
    private final RequisitoVagaRepositorio requisitos;

    public ServicoPerfilAluno(
            PerfilAlunoRepositorio perfisAlunos,
            HabilidadePerfilRepositorio habilidades,
            VagaInteresseRepositorio vagas,
            RequisitoVagaRepositorio requisitos) {
        this.perfisAlunos = perfisAlunos;
        this.habilidades = habilidades;
        this.vagas = vagas;
        this.requisitos = requisitos;
    }

    @Transactional(readOnly = true)
    public List<HabilidadeResposta> listarHabilidades(UUID contaId) {
        exigirPerfilAluno(contaId);
        return habilidades.findAllByContaAlunoIdOrderByNomeNormalizado(contaId).stream()
                .map(ServicoPerfilAluno::respostaHabilidade)
                .toList();
    }

    @Transactional
    public HabilidadeResposta criarHabilidade(UUID contaId, HabilidadeRequisicao requisicao) {
        exigirPerfilAluno(contaId);
        DadosHabilidade dados = validarHabilidade(requisicao);
        if (habilidades.existsByContaAlunoIdAndNomeNormalizado(contaId, dados.nomeNormalizado())) {
            throw habilidadeDuplicada();
        }
        Instant agora = Instant.now();
        try {
            return respostaHabilidade(habilidades.saveAndFlush(new HabilidadePerfil(
                    UUID.randomUUID(), contaId, dados.nome(), dados.nomeNormalizado(),
                    dados.categoria(), dados.nivel(), agora)));
        } catch (DataIntegrityViolationException excecao) {
            if (violacaoUnicidadeHabilidade(excecao)) {
                throw habilidadeDuplicada();
            }
            throw excecao;
        }
    }

    @Transactional
    public HabilidadeResposta atualizarHabilidade(UUID contaId, UUID habilidadeId, HabilidadeRequisicao requisicao) {
        exigirPerfilAluno(contaId);
        HabilidadePerfil habilidade = habilidades.findByIdAndContaAlunoId(habilidadeId, contaId)
                .orElseThrow(ServicoPerfilAluno::naoEncontrado);
        DadosHabilidade dados = validarHabilidade(requisicao);
        if (habilidades.existsByContaAlunoIdAndNomeNormalizadoAndIdNot(
                contaId, dados.nomeNormalizado(), habilidadeId)) {
            throw habilidadeDuplicada();
        }
        habilidade.atualizar(
                dados.nome(), dados.nomeNormalizado(), dados.categoria(), dados.nivel(), Instant.now());
        try {
            return respostaHabilidade(habilidades.saveAndFlush(habilidade));
        } catch (DataIntegrityViolationException excecao) {
            if (violacaoUnicidadeHabilidade(excecao)) {
                throw habilidadeDuplicada();
            }
            throw excecao;
        }
    }

    @Transactional
    public void excluirHabilidade(UUID contaId, UUID habilidadeId) {
        exigirPerfilAluno(contaId);
        HabilidadePerfil habilidade = habilidades.findByIdAndContaAlunoId(habilidadeId, contaId)
                .orElseThrow(ServicoPerfilAluno::naoEncontrado);
        habilidades.delete(habilidade);
    }

    @Transactional(readOnly = true)
    public List<VagaResposta> listarVagas(UUID contaId) {
        exigirPerfilAluno(contaId);
        return vagas.findAllByContaAlunoIdAndArquivadoEmIsNullOrderByCriadoEmDesc(contaId).stream()
                .map(this::respostaVaga)
                .toList();
    }

    @Transactional
    public VagaResposta criarVaga(UUID contaId, VagaRequisicao requisicao) {
        exigirPerfilAluno(contaId);
        DadosVaga dados = validarVaga(requisicao);
        Instant agora = Instant.now();
        VagaInteresse vaga = vagas.saveAndFlush(new VagaInteresse(
                UUID.randomUUID(), contaId, dados.titulo(), dados.empresa(), dados.senioridade(),
                dados.descricao(), agora));
        salvarRequisitos(vaga.getId(), dados.requisitos());
        return respostaVaga(vaga);
    }

    @Transactional
    public VagaResposta atualizarVaga(UUID contaId, UUID vagaId, VagaRequisicao requisicao) {
        exigirPerfilAluno(contaId);
        VagaInteresse vaga = vagas.findByIdAndContaAlunoIdAndArquivadoEmIsNull(vagaId, contaId)
                .orElseThrow(ServicoPerfilAluno::naoEncontrado);
        DadosVaga dados = validarVaga(requisicao);
        vaga.atualizar(dados.titulo(), dados.empresa(), dados.senioridade(), dados.descricao(), Instant.now());
        vagas.saveAndFlush(vaga);
        requisitos.excluirTodosDaVaga(vagaId);
        salvarRequisitos(vagaId, dados.requisitos());
        return respostaVaga(vaga);
    }

    @Transactional
    public void excluirVaga(UUID contaId, UUID vagaId) {
        exigirPerfilAluno(contaId);
        VagaInteresse vaga = vagas.findByIdAndContaAlunoIdAndArquivadoEmIsNull(vagaId, contaId)
                .orElseThrow(ServicoPerfilAluno::naoEncontrado);
        vaga.arquivar(Instant.now());
    }

    private void exigirPerfilAluno(UUID contaId) {
        if (!perfisAlunos.existsById(contaId)) {
            throw new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado",
                    "Esta operação está disponível somente para contas de aluno.");
        }
    }

    private VagaResposta respostaVaga(VagaInteresse vaga) {
        List<String> textos = requisitos.findAllByVagaInteresseIdOrderByOrdem(vaga.getId()).stream()
                .map(RequisitoVaga::getTexto)
                .toList();
        return new VagaResposta(
                vaga.getId(), vaga.getTitulo(), vaga.getEmpresa(), vaga.getSenioridade(),
                vaga.getDescricao(), textos, vaga.getCriadoEm(), vaga.getAtualizadoEm());
    }

    private void salvarRequisitos(UUID vagaId, List<String> textos) {
        for (int indice = 0; indice < textos.size(); indice++) {
            requisitos.save(new RequisitoVaga(
                    UUID.randomUUID(), vagaId, textos.get(indice), (short) (indice + 1)));
        }
    }

    private static HabilidadeResposta respostaHabilidade(HabilidadePerfil habilidade) {
        return new HabilidadeResposta(habilidade.getId(), habilidade.getNome(), habilidade.getCategoria(),
                habilidade.getNivel(), habilidade.getCriadoEm(), habilidade.getAtualizadoEm());
    }

    private static DadosHabilidade validarHabilidade(HabilidadeRequisicao requisicao) {
        String nome = requisicao.nome().trim();
        if (nome.isBlank()) {
            throw campoInvalido("nome", "O nome da habilidade não pode ficar vazio.");
        }
        String categoria = requisicao.categoria() == null ? null : requisicao.categoria().trim();
        if (categoria != null && categoria.isBlank()) {
            categoria = null;
        }
        return new DadosHabilidade(nome, normalizar(nome), categoria, requisicao.nivel().shortValue());
    }

    private static DadosVaga validarVaga(VagaRequisicao requisicao) {
        String titulo = requisicao.titulo().trim();
        String senioridade = requisicao.senioridade().trim();
        if (titulo.isBlank()) {
            throw campoInvalido("titulo", "O título da vaga não pode ficar vazio.");
        }
        if (senioridade.isBlank()) {
            throw campoInvalido("senioridade", "A senioridade não pode ficar vazia.");
        }
        String empresa = apararOpcional(requisicao.empresa());
        String descricao = apararOpcional(requisicao.descricao());
        if (requisicao.requisitos() == null || requisicao.requisitos().isEmpty()
                || requisicao.requisitos().size() > MAXIMO_REQUISITOS) {
            throw campoInvalido("requisitos", "Informe entre 1 e 20 requisitos.");
        }
        List<String> normalizados = requisicao.requisitos().stream()
                .map(String::trim)
                .toList();
        if (normalizados.stream().anyMatch(String::isBlank)) {
            throw campoInvalido("requisitos", "Os requisitos não podem ficar vazios.");
        }
        Set<String> unicos = new HashSet<>();
        for (String requisito : normalizados) {
            if (!unicos.add(normalizar(requisito))) {
                throw campoInvalido("requisitos", "A lista contém requisitos duplicados.");
            }
        }
        return new DadosVaga(titulo, empresa, senioridade, descricao, normalizados);
    }

    private static String apararOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String aparado = valor.trim();
        return aparado.isBlank() ? null : aparado;
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static boolean violacaoUnicidadeHabilidade(DataIntegrityViolationException excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "uq_habilidades_aluno_nome".equals(violacao.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private static ExcecaoNegocio habilidadeDuplicada() {
        return new ExcecaoNegocio(HttpStatus.CONFLICT, "habilidade_duplicada",
                "Já existe uma habilidade com esse nome no perfil.");
    }

    private static ExcecaoNegocio naoEncontrado() {
        return new ExcecaoNegocio(HttpStatus.NOT_FOUND, "recurso_nao_encontrado",
                "Habilidade ou vaga não encontrada.");
    }

    private static ExcecaoNegocio campoInvalido(String campo, String mensagem) {
        return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "dados_invalidos", mensagem,
                List.of(new ErroCampo(campo, mensagem)));
    }

    private record DadosHabilidade(String nome, String nomeNormalizado, String categoria, short nivel) {
    }

    private record DadosVaga(
            String titulo, String empresa, String senioridade, String descricao, List<String> requisitos) {
    }
}
