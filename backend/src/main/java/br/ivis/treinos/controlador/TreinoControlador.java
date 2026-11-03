package br.ivis.treinos.controlador;

import br.ivis.comum.erro.ExcecaoNegocio;
import br.ivis.ia.servico.ClienteIa;
import static br.ivis.ia.dto.ContratoIa.*;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/treinos")
public class TreinoControlador {
    private final JdbcTemplate banco;
    private final ClienteIa clienteIa;
    private final ObjectMapper mapeadorJson;

    public TreinoControlador(
            JdbcTemplate banco, ClienteIa clienteIa, ObjectMapper mapeadorJson) {
        this.banco = banco;
        this.clienteIa = clienteIa;
        this.mapeadorJson = mapeadorJson;
    }

    @PostMapping("/concluidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Resumo concluir(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Conclusao entrada) {
        UUID contaId = UUID.fromString(jwt.getSubject());
        validarAlunoComAcesso(contaId);
        validarContexto(contaId, entrada);
        UUID treinoId = UUID.randomUUID();
        Instant agora = Instant.now();
        Timestamp agoraSql = Timestamp.from(agora);
        GerarAvaliacaoResposta avaliacao = avaliar(contaId, entrada);
        banco.update("""
                insert into treinamentos (objeto, codigo_objeto_perfis_aluno, codigo_objeto_vagas_interesse, tipo_contexto, situacao,
                  contexto_registrado, iniciado_em, finalizado_em, criado_em)
                values (?, ?, ?, ?, 'CONCLUIDO', jsonb_build_object('descricao', ?), ?, ?, ?)
                """, treinoId, contaId, entrada.vagaId(), entrada.tipoContexto(), entrada.contexto(), agoraSql, agoraSql, agoraSql);
        for (int i = 0; i < entrada.interacoes().size(); i++) {
            Interacao item = entrada.interacoes().get(i);
            UUID perguntaId = UUID.randomUUID();
            banco.update("""
                    insert into perguntas_entrevista (objeto, codigo_objeto_treinamentos, numero_sequencial,
                    texto_pergunta, versao_modelo, versao_prompt, criado_em) values (?, ?, ?, ?, 'ivis', 'v1', ?)
                    """,
                    perguntaId, treinoId, i + 1, item.pergunta(), agoraSql);
            banco.update("insert into respostas_entrevista (objeto, codigo_objeto_perguntas_entrevista, texto_resposta, criado_em) values (?, ?, ?, ?)",
                    UUID.randomUUID(), perguntaId, item.resposta(), agoraSql);
        }
        UUID avaliacaoId = UUID.randomUUID();
        banco.update("""
                insert into avaliacoes_treinamento (
                  objeto, codigo_objeto_treinamentos, nota, versao_modelo, versao_prompt, gerado_em,
                  avaliacao_qualitativa, nivel_confianca, limitacoes, avaliacoes_por_pergunta)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """, avaliacaoId, treinoId, avaliacao.nota(),
                avaliacao.versaoModelo(), avaliacao.versaoPrompt(), agoraSql,
                avaliacao.avaliacaoQualitativa(), avaliacao.nivelConfianca().name(),
                avaliacao.limitacoes(),
                serializarAvaliacoes(avaliacao.avaliacoesPorPergunta()));
        inserirItens(avaliacaoId, "PONTO_FORTE", avaliacao.pontosFortes());
        inserirItens(avaliacaoId, "PONTO_MELHORIA", avaliacao.pontosMelhoria());
        return new Resumo(treinoId, titulo(entrada, contaId), entrada.contexto(), avaliacao.nota(), agora,
                avaliacao.pontosFortes(), avaliacao.pontosMelhoria(),
                avaliacao.avaliacaoQualitativa(), avaliacao.nivelConfianca().name(),
                avaliacao.limitacoes(), avaliacao.avaliacoesPorPergunta());
    }

    @GetMapping
    public List<Resumo> listar(@AuthenticationPrincipal Jwt jwt) {
        UUID contaId = UUID.fromString(jwt.getSubject());
        validarAlunoComAcesso(contaId);
        return banco.query("""
                select t.objeto as id, t.tipo_contexto, t.iniciado_em,
                       case when a.avaliacao_qualitativa then a.nota end as nota,
                       v.titulo, v.empresa,
                       a.avaliacao_qualitativa, a.nivel_confianca, a.limitacoes,
                       a.avaliacoes_por_pergunta::text as avaliacoes_por_pergunta,
                       t.contexto_registrado ->> 'descricao' as contexto
                from treinamentos t join avaliacoes_treinamento a on a.codigo_objeto_treinamentos=t.objeto
                left join vagas_interesse v on v.objeto=t.codigo_objeto_vagas_interesse
                where t.codigo_objeto_perfis_aluno=? and t.situacao='CONCLUIDO' order by t.finalizado_em desc
                """, (rs, linha) -> {
                UUID treinoId = rs.getObject("id", UUID.class);
                return new Resumo(treinoId,
                "VAGA".equals(rs.getString("tipo_contexto")) ? "Entrevista · " + rs.getString("titulo") : "Entrevista por habilidades",
                rs.getString("contexto"), rs.getObject("nota", Integer.class),
                rs.getTimestamp("iniciado_em").toInstant(),
                listarItens(treinoId, "PONTO_FORTE"), listarItens(treinoId, "PONTO_MELHORIA"),
                rs.getBoolean("avaliacao_qualitativa"), rs.getString("nivel_confianca"),
                rs.getString("limitacoes"), desserializarAvaliacoes(
                        rs.getString("avaliacoes_por_pergunta")));
                }, contaId);
    }

    private void inserirItens(UUID avaliacaoId, String tipo, List<String> itens) {
        for (int i = 0; i < itens.size(); i++) {
            banco.update("insert into itens_feedback_treinamento (objeto, codigo_objeto_avaliacoes_treinamento, tipo, ordem, texto) values (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), avaliacaoId, tipo, i + 1, itens.get(i));
        }
    }
    private List<String> listarItens(UUID treinoId, String tipo) {
        return banco.query("""
                select i.texto from itens_feedback_treinamento i
                join avaliacoes_treinamento a on a.objeto=i.codigo_objeto_avaliacoes_treinamento
                where a.codigo_objeto_treinamentos=? and i.tipo=? order by i.ordem
                """, (rs, linha) -> rs.getString("texto"), treinoId, tipo);
    }

    private String serializarAvaliacoes(List<AvaliacaoPergunta> avaliacoes) {
        try {
            return mapeadorJson.writeValueAsString(avaliacoes);
        } catch (JacksonException erro) {
            throw new IllegalStateException("Não foi possível serializar a rubrica da avaliação.", erro);
        }
    }

    private List<AvaliacaoPergunta> desserializarAvaliacoes(String avaliacoes) {
        try {
            return mapeadorJson.readValue(
                    avaliacoes, new TypeReference<List<AvaliacaoPergunta>>() {});
        } catch (JacksonException erro) {
            throw new IllegalStateException("Não foi possível ler a rubrica salva.", erro);
        }
    }

    private GerarAvaliacaoResposta avaliar(UUID contaId, Conclusao entrada) {
        List<br.ivis.ia.dto.ContratoIa.Interacao> interacoes = entrada.interacoes().stream()
                .map(item -> new br.ivis.ia.dto.ContratoIa.Interacao(item.numero(), item.pergunta(), item.resposta()))
                .toList();
        if ("VAGA".equals(entrada.tipoContexto())) {
            Vaga vaga = banco.queryForObject("""
                    select titulo, senioridade from vagas_interesse where objeto=? and codigo_objeto_perfis_aluno=?
                    """, (rs, linha) -> new Vaga(rs.getString("titulo"), rs.getString("senioridade"),
                    banco.query("select texto_requisito from requisitos_vaga where codigo_objeto_vagas_interesse=? order by ordem",
                            (requisitos, ordem) -> requisitos.getString("texto_requisito"), entrada.vagaId())), entrada.vagaId(), contaId);
            return clienteIa.gerarAvaliacao(new GerarAvaliacaoEntrada(TipoContexto.VAGA, null, vaga, interacoes));
        }
        List<Habilidade> habilidades = banco.query("""
                select nome, nivel from habilidades where codigo_objeto_perfis_aluno=? order by nome_normalizado
                """, (rs, linha) -> new Habilidade(rs.getString("nome"), rs.getInt("nivel")), contaId).stream()
                .filter(habilidade -> entrada.habilidades().contains(habilidade.nome()))
                .toList();
        return clienteIa.gerarAvaliacao(new GerarAvaliacaoEntrada(TipoContexto.HABILIDADES, habilidades, null, interacoes));
    }

    private String titulo(Conclusao entrada, UUID contaId) {
        if (!"VAGA".equals(entrada.tipoContexto())) return "Entrevista por habilidades";
        return banco.queryForObject("select titulo from vagas_interesse where objeto=? and codigo_objeto_perfis_aluno=?",
                String.class, entrada.vagaId(), contaId);
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
        if (!Boolean.TRUE.equals(permitido)) throw acessoNegado();
    }

    private void validarContexto(UUID contaId, Conclusao entrada) {
        boolean vaga = "VAGA".equals(entrada.tipoContexto());
        boolean habilidades = "HABILIDADES".equals(entrada.tipoContexto());
        if ((!vaga && !habilidades) || (vaga && entrada.vagaId() == null) || (habilidades && (entrada.vagaId() != null || entrada.habilidades().isEmpty()))) {
            throw dadosInvalidos();
        }
        if (vaga && !Boolean.TRUE.equals(banco.queryForObject(
                "select exists (select 1 from vagas_interesse where objeto=? and codigo_objeto_perfis_aluno=? and arquivado_em is null)",
                Boolean.class, entrada.vagaId(), contaId))) throw dadosInvalidos();
    }

    private static ExcecaoNegocio acessoNegado() { return new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado", "É necessário um acesso ativo para realizar treinamentos."); }
    private static ExcecaoNegocio dadosInvalidos() { return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "contexto_invalido", "O contexto informado para o treinamento é inválido."); }

    public record Interacao(@NotNull @Min(1) Integer numero, @NotBlank @Size(max=1000) String pergunta, @NotBlank @Size(max=4000) String resposta) { }
    public record Conclusao(@NotBlank String tipoContexto, UUID vagaId, @NotBlank String contexto,
                            @NotEmpty @Size(max=10) List<@Valid Interacao> interacoes,
                            @NotNull @Size(max=30) List<@NotBlank String> habilidades) { }
    public record Resumo(
            UUID id, String titulo, String contexto, Integer nota, Instant data,
            List<String> pontosFortes, List<String> pontosMelhoria,
            boolean avaliacaoQualitativa, String nivelConfianca, String limitacoes,
            List<AvaliacaoPergunta> avaliacoesPorPergunta) { }
}
