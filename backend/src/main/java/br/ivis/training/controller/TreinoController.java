package br.ivis.training.controller;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.ai.service.IaClient;
import br.ivis.identity.repository.HabilidadePerfilRepository;
import br.ivis.identity.repository.RequisitoVagaRepository;
import br.ivis.identity.repository.VagaInteresseRepository;
import br.ivis.training.repository.TreinoRepository;
import br.ivis.training.repository.TreinoRepository.Pergunta;
import br.ivis.training.repository.TreinoRepository.Treino;
import static br.ivis.ai.dto.ContratoIa.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/v1/treinos")
public class TreinoController {
    private static final int TOTAL_PERGUNTAS = 3;
    private final TreinoRepository treinos;
    private final VagaInteresseRepository vagas;
    private final RequisitoVagaRepository requisitos;
    private final HabilidadePerfilRepository habilidadesPerfil;
    private final IaClient clienteIa;
    private final TransactionTemplate transacao;
    private final ObjectMapper json;

    public TreinoController(TreinoRepository treinos, VagaInteresseRepository vagas,
            RequisitoVagaRepository requisitos, HabilidadePerfilRepository habilidadesPerfil,
            IaClient clienteIa, PlatformTransactionManager gerenciadorTransacoes, ObjectMapper json) {
        this.treinos = treinos;
        this.vagas = vagas;
        this.requisitos = requisitos;
        this.habilidadesPerfil = habilidadesPerfil;
        this.clienteIa = clienteIa;
        this.transacao = new TransactionTemplate(gerenciadorTransacoes);
        this.json = json;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoTreino iniciar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Inicio entrada) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        UUID ativo = treinoAtivo(contaId);
        if (ativo != null) return sessao(ativo, contaId);

        Contexto contexto = prepararContexto(contaId, entrada);
        GerarPerguntaResposta pergunta = clienteIa.gerarPergunta(new GerarPerguntaEntrada(
                contexto.tipo(), contexto.habilidades(), contexto.vaga(), List.of(), 1, TOTAL_PERGUNTAS));
        try {
            return transacao.execute(status -> {
                UUID existente = treinoAtivo(contaId);
                if (existente != null) return sessao(existente, contaId);
                UUID treinoId = UUID.randomUUID();
                treinos.criar(treinoId, contaId, contexto.vagaId(), contexto.tipo(), contexto.json());
                treinos.inserirPergunta(treinoId, 1, pergunta);
                return sessao(treinoId, contaId);
            });
        } catch (DataIntegrityViolationException erro) {
            UUID criadoEmParalelo = treinoAtivo(contaId);
            if (criadoEmParalelo != null) return sessao(criadoEmParalelo, contaId);
            throw erro;
        }
    }

    @GetMapping("/em-andamento")
    public ResponseEntity<SessaoTreino> emAndamento(@AuthenticationPrincipal Jwt jwt) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        UUID ativo = treinoAtivo(contaId);
        return ativo == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(sessao(ativo, contaId));
    }

    @PostMapping("/{treinoId}/respostas")
    public SessaoTreino responder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID treinoId,
            @Valid @RequestBody Resposta entrada) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        Treino treino = treino(treinoId, contaId);
        Pergunta pergunta = pergunta(treinoId, entrada.perguntaId());
        String resposta = entrada.resposta().trim();
        if (pergunta.resposta() != null) {
            if (!pergunta.resposta().equals(resposta)) throw conflito();
            return sessao(treinoId, contaId);
        }
        if (!"EM_ANDAMENTO".equals(treino.situacao())) throw conflito();
        List<Pergunta> perguntas = perguntas(treinoId);
        if (!perguntas.getLast().id().equals(pergunta.id())) throw conflito();

        GerarPerguntaResposta proxima = null;
        if (pergunta.numero() < TOTAL_PERGUNTAS) {
            List<br.ivis.ai.dto.ContratoIa.Interacao> interacoes = interacoes(perguntas);
            interacoes.add(new br.ivis.ai.dto.ContratoIa.Interacao(
                    pergunta.numero(), pergunta.texto(), resposta));
            proxima = clienteIa.gerarPergunta(new GerarPerguntaEntrada(
                    treino.tipo(), habilidades(treino), vaga(treino), interacoes,
                    pergunta.numero() + 1, TOTAL_PERGUNTAS));
        }
        GerarPerguntaResposta perguntaGerada = proxima;
        return transacao.execute(status -> {
            String situacao = treinos.situacaoParaAtualizacao(treinoId, contaId);
            if (!"EM_ANDAMENTO".equals(situacao)) throw conflito();
            Pergunta atual = pergunta(treinoId, entrada.perguntaId());
            if (atual.resposta() != null) {
                if (!atual.resposta().equals(resposta)) throw conflito();
                return sessao(treinoId, contaId);
            }
            if (!perguntas(treinoId).getLast().id().equals(atual.id())) throw conflito();
            treinos.inserirResposta(atual.id(), resposta);
            if (perguntaGerada != null) treinos.inserirPergunta(treinoId, atual.numero() + 1, perguntaGerada);
            return sessao(treinoId, contaId);
        });
    }

    @PostMapping("/{treinoId}/concluir")
    public Resumo concluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID treinoId) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        Treino treino = treino(treinoId, contaId);
        if ("CONCLUIDO".equals(treino.situacao())) return resumo(treinoId, contaId);
        if (!"EM_ANDAMENTO".equals(treino.situacao())) throw conflito();
        List<Pergunta> perguntas = perguntas(treinoId);
        if (perguntas.size() != TOTAL_PERGUNTAS
                || perguntas.stream().anyMatch(item -> item.resposta() == null)) throw conflito();

        GerarAvaliacaoResposta avaliacao = clienteIa.gerarAvaliacao(new GerarAvaliacaoEntrada(
                treino.tipo(), habilidades(treino), vaga(treino), interacoes(perguntas)));
        return transacao.execute(status -> {
            String situacao = treinos.situacaoParaAtualizacao(treinoId, contaId);
            if ("CONCLUIDO".equals(situacao)) return resumo(treinoId, contaId);
            if (!"EM_ANDAMENTO".equals(situacao)) throw conflito();
            UUID avaliacaoId = UUID.randomUUID();
            treinos.inserirAvaliacao(avaliacaoId, treinoId, avaliacao);
            treinos.inserirItens(avaliacaoId, "PONTO_FORTE", avaliacao.pontosFortes());
            treinos.inserirItens(avaliacaoId, "PONTO_MELHORIA", avaliacao.pontosMelhoria());
            treinos.concluir(treinoId);
            return resumo(treinoId, contaId);
        });
    }

    @DeleteMapping("/{treinoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID treinoId) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        transacao.executeWithoutResult(status -> {
            Treino treino = treino(treinoId, contaId);
            if ("EM_ANDAMENTO".equals(treino.situacao())) {
                treinos.cancelar(treinoId, contaId);
            }
        });
    }

    @GetMapping
    public List<Resumo> listar(@AuthenticationPrincipal Jwt jwt) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        List<UUID> ids = treinos.concluidos(contaId);
        return ids.stream().map(id -> resumo(id, contaId)).toList();
    }

    private Contexto prepararContexto(UUID contaId, Inicio entrada) {
        Map<String, Object> registro = new HashMap<>();
        if ("VAGA".equals(entrada.tipoContexto())) {
            if (entrada.vagaId() == null || !entrada.habilidades().isEmpty()) throw dadosInvalidos();
            var vagaPerfil = vagas.findByIdAndContaAlunoIdAndArquivadoEmIsNull(entrada.vagaId(), contaId)
                    .orElseThrow(TreinoController::dadosInvalidos);
            List<String> requisitosVaga = requisitos.findAllByVagaInteresseIdOrderByOrdem(entrada.vagaId())
                    .stream().map(item -> item.getTexto()).toList();
            Vaga vaga = new Vaga(vagaPerfil.getTitulo(), vagaPerfil.getSenioridade(), requisitosVaga);
            registro.put("descricao", "Vaga: " + vaga.titulo()
                    + (vagaPerfil.getEmpresa() == null ? "" : " · " + vagaPerfil.getEmpresa()));
            registro.put("vaga", vaga);
            return new Contexto(TipoContexto.VAGA, null, vaga, entrada.vagaId(), serializar(registro));
        }
        if (!"HABILIDADES".equals(entrada.tipoContexto()) || entrada.vagaId() != null
                || entrada.habilidades().isEmpty()
                || entrada.habilidades().stream().distinct().count() != entrada.habilidades().size()) throw dadosInvalidos();
        List<Habilidade> habilidades = habilidadesPerfil.findAllByContaAlunoIdOrderByNomeNormalizado(contaId).stream()
                .map(item -> new Habilidade(item.getNome(), (int) item.getNivel()))
                .filter(item -> entrada.habilidades().contains(item.nome())).toList();
        if (habilidades.size() != entrada.habilidades().size()) throw dadosInvalidos();
        registro.put("descricao", "Habilidades: " + String.join(", ", entrada.habilidades()));
        registro.put("habilidades", habilidades);
        return new Contexto(TipoContexto.HABILIDADES, habilidades, null, null, serializar(registro));
    }

    private String serializar(Object valor) {
        return json.writeValueAsString(valor);
    }

    private Treino treino(UUID treinoId, UUID contaId) {
        Treino encontrado = treinos.buscar(treinoId, contaId);
        if (encontrado == null) throw naoEncontrado();
        return encontrado;
    }

    private UUID treinoAtivo(UUID contaId) {
        return treinos.ativo(contaId);
    }

    private SessaoTreino sessao(UUID treinoId, UUID contaId) {
        Treino treino = treino(treinoId, contaId);
        List<Pergunta> perguntas = perguntas(treinoId);
        List<String> nomes = treino.tipo() == TipoContexto.HABILIDADES
                ? habilidades(treino).stream().map(habilidade -> habilidade.nome()).toList()
                : vaga(treino).requisitos();
        return new SessaoTreino(treinoId, treino.tipo().name(), treino.descricao(),
                treino.vagaId(), nomes, perguntas, TOTAL_PERGUNTAS);
    }

    private List<Pergunta> perguntas(UUID treinoId) {
        return treinos.perguntas(treinoId);
    }

    private Pergunta pergunta(UUID treinoId, UUID perguntaId) {
        return perguntas(treinoId).stream().filter(item -> item.id().equals(perguntaId))
                .findFirst().orElseThrow(TreinoController::conflito);
    }

    private List<Habilidade> habilidades(Treino treino) {
        if (treino.tipo() != TipoContexto.HABILIDADES) return null;
        return treinos.habilidadesDoContexto(treino.id());
    }

    private Vaga vaga(Treino treino) {
        if (treino.tipo() != TipoContexto.VAGA) return null;
        return treinos.vagaDoContexto(treino.id());
    }

    private static List<br.ivis.ai.dto.ContratoIa.Interacao> interacoes(List<Pergunta> perguntas) {
        List<br.ivis.ai.dto.ContratoIa.Interacao> itens = new ArrayList<>();
        for (Pergunta pergunta : perguntas) {
            if (pergunta.resposta() != null) itens.add(new br.ivis.ai.dto.ContratoIa.Interacao(
                    pergunta.numero(), pergunta.texto(), pergunta.resposta()));
        }
        return itens;
    }

    private Resumo resumo(UUID treinoId, UUID contaId) {
        Treino treino = treino(treinoId, contaId);
        String titulo = treino.tipo() == TipoContexto.VAGA
                ? "Entrevista · " + vaga(treino).titulo() : "Entrevista por habilidades";
        var avaliacao = treinos.avaliacao(treinoId);
        return new Resumo(treinoId, titulo, treino.descricao(), avaliacao.nota(),
                avaliacao.data(), avaliacao.pontosFortes(), avaliacao.pontosMelhoria());
    }

    private void validarAlunoComAcesso(UUID contaId) {
        if (!treinos.alunoComAcesso(contaId)) throw new ExcecaoNegocio(HttpStatus.FORBIDDEN,
                "acesso_negado", "É necessário um acesso ativo para realizar treinamentos.");
    }

    private static UUID contaId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private static ExcecaoNegocio dadosInvalidos() { return new ExcecaoNegocio(HttpStatus.BAD_REQUEST,
            "contexto_invalido", "O contexto informado para o treinamento é inválido."); }
    private static ExcecaoNegocio naoEncontrado() { return new ExcecaoNegocio(HttpStatus.NOT_FOUND,
            "treino_nao_encontrado", "Treinamento não encontrado."); }
    private static ExcecaoNegocio conflito() { return new ExcecaoNegocio(HttpStatus.CONFLICT,
            "estado_invalido", "O estado do treinamento mudou. Atualize a página e tente novamente."); }

    public record Inicio(@NotBlank String tipoContexto, UUID vagaId,
            @NotNull @Size(max=30) List<@NotBlank String> habilidades) { }
    public record Resposta(@NotNull UUID perguntaId, @NotBlank @Size(max=4000) String resposta) { }
    public record SessaoTreino(UUID id, String tipoContexto, String contexto, UUID vagaId,
            List<String> habilidades, List<Pergunta> perguntas, int totalPerguntas) { }
    public record Resumo(UUID id, String titulo, String contexto, int nota, Instant data,
            List<String> pontosFortes, List<String> pontosMelhoria) { }
    private record Contexto(TipoContexto tipo, List<Habilidade> habilidades,
            Vaga vaga, UUID vagaId, String json) { }
}
