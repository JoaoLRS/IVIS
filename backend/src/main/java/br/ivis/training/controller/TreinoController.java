package br.ivis.training.controller;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.ai.service.IaClient;
import static br.ivis.ai.dto.ContratoIa.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate banco;
    private final IaClient clienteIa;
    private final TransactionTemplate transacao;
    private final ObjectMapper json;

    public TreinoController(JdbcTemplate banco, IaClient clienteIa,
            PlatformTransactionManager gerenciadorTransacoes, ObjectMapper json) {
        this.banco = banco;
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
                banco.update("""
                        insert into treinamentos (objeto, codigo_objeto_perfis_aluno,
                          codigo_objeto_vagas_interesse, tipo_contexto, situacao, contexto_registrado)
                        values (?, ?, ?, ?, 'EM_ANDAMENTO', ?::jsonb)
                        """, treinoId, contaId, contexto.vagaId(), contexto.tipo().name(), contexto.json());
                inserirPergunta(treinoId, 1, pergunta);
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
            String situacao = banco.queryForObject("""
                    select situacao from treinamentos where objeto=? and codigo_objeto_perfis_aluno=? for update
                    """, String.class, treinoId, contaId);
            if (!"EM_ANDAMENTO".equals(situacao)) throw conflito();
            Pergunta atual = pergunta(treinoId, entrada.perguntaId());
            if (atual.resposta() != null) {
                if (!atual.resposta().equals(resposta)) throw conflito();
                return sessao(treinoId, contaId);
            }
            if (!perguntas(treinoId).getLast().id().equals(atual.id())) throw conflito();
            banco.update("""
                    insert into respostas_entrevista (objeto, codigo_objeto_perguntas_entrevista, texto_resposta)
                    values (?, ?, ?)
                    """, UUID.randomUUID(), atual.id(), resposta);
            if (perguntaGerada != null) inserirPergunta(treinoId, atual.numero() + 1, perguntaGerada);
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
            String situacao = banco.queryForObject("""
                    select situacao from treinamentos where objeto=? and codigo_objeto_perfis_aluno=? for update
                    """, String.class, treinoId, contaId);
            if ("CONCLUIDO".equals(situacao)) return resumo(treinoId, contaId);
            if (!"EM_ANDAMENTO".equals(situacao)) throw conflito();
            UUID avaliacaoId = UUID.randomUUID();
            banco.update("""
                    insert into avaliacoes_treinamento (objeto, codigo_objeto_treinamentos,
                      nota, versao_modelo, versao_prompt) values (?, ?, ?, ?, ?)
                    """, avaliacaoId, treinoId, avaliacao.nota(),
                    avaliacao.versaoModelo(), avaliacao.versaoPrompt());
            inserirItens(avaliacaoId, "PONTO_FORTE", avaliacao.pontosFortes());
            inserirItens(avaliacaoId, "PONTO_MELHORIA", avaliacao.pontosMelhoria());
            banco.update("""
                    update treinamentos set situacao='CONCLUIDO', finalizado_em=current_timestamp where objeto=?
                    """, treinoId);
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
                banco.update("""
                        update treinamentos set situacao='CANCELADO', finalizado_em=current_timestamp
                        where objeto=? and codigo_objeto_perfis_aluno=? and situacao='EM_ANDAMENTO'
                        """, treinoId, contaId);
            }
        });
    }

    @GetMapping
    public List<Resumo> listar(@AuthenticationPrincipal Jwt jwt) {
        UUID contaId = contaId(jwt);
        validarAlunoComAcesso(contaId);
        List<UUID> ids = banco.query("""
                select objeto from treinamentos where codigo_objeto_perfis_aluno=?
                  and situacao='CONCLUIDO' order by finalizado_em desc
                """, (rs, linha) -> rs.getObject("objeto", UUID.class), contaId);
        return ids.stream().map(id -> resumo(id, contaId)).toList();
    }

    private Contexto prepararContexto(UUID contaId, Inicio entrada) {
        Map<String, Object> registro = new HashMap<>();
        if ("VAGA".equals(entrada.tipoContexto())) {
            if (entrada.vagaId() == null || !entrada.habilidades().isEmpty()) throw dadosInvalidos();
            Vaga vaga = banco.query("""
                    select titulo, senioridade from vagas_interesse
                    where objeto=? and codigo_objeto_perfis_aluno=? and arquivado_em is null
                    """, (rs, linha) -> new Vaga(rs.getString("titulo"), rs.getString("senioridade"), List.of()),
                    entrada.vagaId(), contaId).stream().findFirst().orElseThrow(TreinoController::dadosInvalidos);
            List<String> requisitos = banco.query("""
                    select texto_requisito from requisitos_vaga
                    where codigo_objeto_vagas_interesse=? order by ordem
                    """, (rs, linha) -> rs.getString(1), entrada.vagaId());
            vaga = new Vaga(vaga.titulo(), vaga.senioridade(), requisitos);
            String empresa = banco.queryForObject("select empresa from vagas_interesse where objeto=?", String.class, entrada.vagaId());
            registro.put("descricao", "Vaga: " + vaga.titulo() + (empresa == null ? "" : " · " + empresa));
            registro.put("vaga", vaga);
            return new Contexto(TipoContexto.VAGA, null, vaga, entrada.vagaId(), serializar(registro));
        }
        if (!"HABILIDADES".equals(entrada.tipoContexto()) || entrada.vagaId() != null
                || entrada.habilidades().isEmpty()
                || entrada.habilidades().stream().distinct().count() != entrada.habilidades().size()) throw dadosInvalidos();
        List<Habilidade> habilidades = banco.query("""
                select nome, nivel from habilidades where codigo_objeto_perfis_aluno=?
                """, (rs, linha) -> new Habilidade(rs.getString("nome"), rs.getInt("nivel")), contaId).stream()
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
        return banco.query("""
                select objeto, tipo_contexto, situacao, codigo_objeto_vagas_interesse as vaga_id,
                  contexto_registrado ->> 'descricao' as descricao
                from treinamentos where objeto=? and codigo_objeto_perfis_aluno=?
                """, (rs, linha) -> new Treino(rs.getObject("objeto", UUID.class),
                TipoContexto.valueOf(rs.getString("tipo_contexto")), rs.getString("situacao"),
                rs.getObject("vaga_id", UUID.class), rs.getString("descricao")),
                treinoId, contaId).stream().findFirst().orElseThrow(TreinoController::naoEncontrado);
    }

    private UUID treinoAtivo(UUID contaId) {
        return banco.query("""
                select objeto from treinamentos where codigo_objeto_perfis_aluno=?
                  and situacao='EM_ANDAMENTO' order by iniciado_em desc limit 1
                """, (rs, linha) -> rs.getObject(1, UUID.class), contaId).stream().findFirst().orElse(null);
    }

    private SessaoTreino sessao(UUID treinoId, UUID contaId) {
        Treino treino = treino(treinoId, contaId);
        List<Pergunta> perguntas = perguntas(treinoId);
        List<String> nomes = treino.tipo() == TipoContexto.HABILIDADES
                ? habilidades(treino).stream().map(Habilidade::nome).toList()
                : vaga(treino).requisitos();
        return new SessaoTreino(treinoId, treino.tipo().name(), treino.descricao(),
                treino.vagaId(), nomes, perguntas, TOTAL_PERGUNTAS);
    }

    private List<Pergunta> perguntas(UUID treinoId) {
        return banco.query("""
                select p.objeto, p.numero_sequencial, p.texto_pergunta, r.texto_resposta
                from perguntas_entrevista p
                left join respostas_entrevista r on r.codigo_objeto_perguntas_entrevista=p.objeto
                where p.codigo_objeto_treinamentos=? order by p.numero_sequencial
                """, (rs, linha) -> new Pergunta(rs.getObject("objeto", UUID.class),
                rs.getInt("numero_sequencial"), rs.getString("texto_pergunta"),
                rs.getString("texto_resposta")), treinoId);
    }

    private Pergunta pergunta(UUID treinoId, UUID perguntaId) {
        return perguntas(treinoId).stream().filter(item -> item.id().equals(perguntaId))
                .findFirst().orElseThrow(TreinoController::conflito);
    }

    private List<Habilidade> habilidades(Treino treino) {
        if (treino.tipo() != TipoContexto.HABILIDADES) return null;
        return banco.query("""
                select h.nome, h.nivel from treinamentos t,
                  jsonb_to_recordset(t.contexto_registrado -> 'habilidades') as h(nome text, nivel integer)
                where t.objeto=?
                """, (rs, linha) -> new Habilidade(rs.getString(1), rs.getInt(2)), treino.id());
    }

    private Vaga vaga(Treino treino) {
        if (treino.tipo() != TipoContexto.VAGA) return null;
        String titulo = banco.queryForObject("""
                select contexto_registrado -> 'vaga' ->> 'titulo' from treinamentos where objeto=?
                """, String.class, treino.id());
        String senioridade = banco.queryForObject("""
                select contexto_registrado -> 'vaga' ->> 'senioridade' from treinamentos where objeto=?
                """, String.class, treino.id());
        List<String> requisitos = banco.query("""
                select jsonb_array_elements_text(contexto_registrado -> 'vaga' -> 'requisitos')
                from treinamentos where objeto=?
                """, (rs, linha) -> rs.getString(1), treino.id());
        return new Vaga(titulo, senioridade, requisitos);
    }

    private static List<br.ivis.ai.dto.ContratoIa.Interacao> interacoes(List<Pergunta> perguntas) {
        List<br.ivis.ai.dto.ContratoIa.Interacao> itens = new ArrayList<>();
        for (Pergunta pergunta : perguntas) {
            if (pergunta.resposta() != null) itens.add(new br.ivis.ai.dto.ContratoIa.Interacao(
                    pergunta.numero(), pergunta.texto(), pergunta.resposta()));
        }
        return itens;
    }

    private void inserirPergunta(UUID treinoId, int numero, GerarPerguntaResposta pergunta) {
        banco.update("""
                insert into perguntas_entrevista (objeto, codigo_objeto_treinamentos, numero_sequencial,
                  texto_pergunta, versao_modelo, versao_prompt, origem)
                values (?, ?, ?, ?, ?, ?, 'IA')
                """, UUID.randomUUID(), treinoId, numero, pergunta.pergunta().texto(),
                pergunta.versaoModelo(), pergunta.versaoPrompt());
    }

    private void inserirItens(UUID avaliacaoId, String tipo, List<String> itens) {
        for (int i = 0; i < itens.size(); i++) {
            banco.update("""
                    insert into itens_feedback_treinamento
                      (objeto, codigo_objeto_avaliacoes_treinamento, tipo, ordem, texto)
                    values (?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), avaliacaoId, tipo, i + 1, itens.get(i));
        }
    }

    private Resumo resumo(UUID treinoId, UUID contaId) {
        Treino treino = treino(treinoId, contaId);
        String titulo = treino.tipo() == TipoContexto.VAGA
                ? "Entrevista · " + vaga(treino).titulo() : "Entrevista por habilidades";
        return banco.queryForObject("""
                select a.nota, t.finalizado_em,
                  (select jsonb_agg(texto order by ordem) from itens_feedback_treinamento
                    where codigo_objeto_avaliacoes_treinamento=a.objeto and tipo='PONTO_FORTE') as fortes,
                  (select jsonb_agg(texto order by ordem) from itens_feedback_treinamento
                    where codigo_objeto_avaliacoes_treinamento=a.objeto and tipo='PONTO_MELHORIA') as melhorias
                from treinamentos t join avaliacoes_treinamento a on a.codigo_objeto_treinamentos=t.objeto
                where t.objeto=?
                """, (rs, linha) -> new Resumo(treinoId,
                titulo,
                treino.descricao(), rs.getInt("nota"), rs.getTimestamp("finalizado_em").toInstant(),
                lerItens(rs.getString("fortes")), lerItens(rs.getString("melhorias"))), treinoId);
    }

    private List<String> lerItens(String valor) {
        if (valor == null) return List.of();
        return json.readValue(valor, new tools.jackson.core.type.TypeReference<List<String>>() {});
    }

    private void validarAlunoComAcesso(UUID contaId) {
        Boolean permitido = banco.queryForObject("""
                select exists (
                    select 1 from contas c
                    where c.objeto=? and c.tipo_conta='ALUNO' and c.situacao='ATIVA'
                      and (exists (select 1 from contratos ct join planos p on p.objeto=ct.codigo_objeto_planos
                        where ct.codigo_objeto_contas=c.objeto and ct.situacao='ATIVO' and ct.encerrado_em is null
                          and (ct.expira_em is null or ct.expira_em > current_timestamp)
                          and p.modalidade='INDIVIDUAL')
                        or exists (select 1 from vinculos_institucionais v join contratos ct on ct.objeto=v.codigo_objeto_contratos
                          where v.codigo_objeto_perfis_aluno=c.objeto and v.situacao='ATIVO' and v.desvinculado_em is null
                            and ct.situacao='ATIVO' and ct.encerrado_em is null
                            and (ct.expira_em is null or ct.expira_em > current_timestamp)))
                )
                """, Boolean.class, contaId);
        if (!Boolean.TRUE.equals(permitido)) throw new ExcecaoNegocio(HttpStatus.FORBIDDEN,
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
    public record Pergunta(UUID id, int numero, String texto, String resposta) { }
    public record SessaoTreino(UUID id, String tipoContexto, String contexto, UUID vagaId,
            List<String> habilidades, List<Pergunta> perguntas, int totalPerguntas) { }
    public record Resumo(UUID id, String titulo, String contexto, int nota, Instant data,
            List<String> pontosFortes, List<String> pontosMelhoria) { }
    private record Contexto(TipoContexto tipo, List<Habilidade> habilidades,
            Vaga vaga, UUID vagaId, String json) { }
    private record Treino(UUID id, TipoContexto tipo, String situacao, UUID vagaId, String descricao) { }
}
