package br.ivis.training.repository;

import static br.ivis.ai.dto.ContratoIa.*;

import br.ivis.training.model.TreinamentoModel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreinoRepository extends JpaRepository<TreinamentoModel, UUID> {
    @Modifying
    @Query(value = """
            insert into treinamentos (objeto, codigo_objeto_perfis_aluno,
              codigo_objeto_vagas_interesse, tipo_contexto, situacao, contexto_registrado)
            values (:id, :contaId, :vagaId, :tipo, 'EM_ANDAMENTO', cast(:contexto as jsonb))
            """, nativeQuery = true)
    void inserirTreino(@Param("id") UUID id, @Param("contaId") UUID contaId,
            @Param("vagaId") UUID vagaId, @Param("tipo") String tipo, @Param("contexto") String contexto);

    default void criar(UUID id, UUID contaId, UUID vagaId, TipoContexto tipo, String contexto) {
        inserirTreino(id, contaId, vagaId, tipo.name(), contexto);
    }

    @Query(value = """
            select objeto as id, tipo_contexto as tipo, situacao,
              codigo_objeto_vagas_interesse as "vagaId",
              contexto_registrado ->> 'descricao' as descricao
            from treinamentos where objeto=:id and codigo_objeto_perfis_aluno=:contaId
            """, nativeQuery = true)
    TreinoLinha buscarLinha(@Param("id") UUID id, @Param("contaId") UUID contaId);

    default Treino buscar(UUID id, UUID contaId) {
        TreinoLinha linha = buscarLinha(id, contaId);
        return linha == null ? null : new Treino(linha.getId(), TipoContexto.valueOf(linha.getTipo()),
                linha.getSituacao(), linha.getVagaId(), linha.getDescricao());
    }

    @Query(value = """
            select objeto from treinamentos where codigo_objeto_perfis_aluno=:contaId
              and situacao='EM_ANDAMENTO' order by iniciado_em desc limit 1
            """, nativeQuery = true)
    UUID ativo(@Param("contaId") UUID contaId);

    @Query(value = """
            select situacao from treinamentos where objeto=:id
              and codigo_objeto_perfis_aluno=:contaId for update
            """, nativeQuery = true)
    String situacaoParaAtualizacao(@Param("id") UUID id, @Param("contaId") UUID contaId);

    @Query(value = """
            select p.objeto as id, p.numero_sequencial as numero,
              p.texto_pergunta as texto, r.texto_resposta as resposta
            from perguntas_entrevista p
            left join respostas_entrevista r on r.codigo_objeto_perguntas_entrevista=p.objeto
            where p.codigo_objeto_treinamentos=:id order by p.numero_sequencial
            """, nativeQuery = true)
    List<PerguntaLinha> buscarPerguntas(@Param("id") UUID id);

    default List<Pergunta> perguntas(UUID id) {
        return buscarPerguntas(id).stream()
                .map(item -> new Pergunta(item.getId(), item.getNumero(), item.getTexto(), item.getResposta()))
                .toList();
    }

    @Modifying
    @Query(value = """
            insert into perguntas_entrevista (objeto, codigo_objeto_treinamentos, numero_sequencial,
              texto_pergunta, versao_modelo, versao_prompt, origem)
            values (:id, :treinoId, :numero, :texto, :modelo, :prompt, 'IA')
            """, nativeQuery = true)
    void inserirPerguntaDados(@Param("id") UUID id, @Param("treinoId") UUID treinoId,
            @Param("numero") int numero, @Param("texto") String texto,
            @Param("modelo") String modelo, @Param("prompt") String prompt);

    default void inserirPergunta(UUID treinoId, int numero, GerarPerguntaResposta pergunta) {
        inserirPerguntaDados(UUID.randomUUID(), treinoId, numero, pergunta.pergunta().texto(),
                pergunta.versaoModelo(), pergunta.versaoPrompt());
    }

    @Modifying
    @Query(value = """
            insert into respostas_entrevista (objeto, codigo_objeto_perguntas_entrevista, texto_resposta)
            values (:id, :perguntaId, :resposta)
            """, nativeQuery = true)
    void inserirRespostaDados(@Param("id") UUID id, @Param("perguntaId") UUID perguntaId,
            @Param("resposta") String resposta);

    default void inserirResposta(UUID perguntaId, String resposta) {
        inserirRespostaDados(UUID.randomUUID(), perguntaId, resposta);
    }

    @Modifying
    @Query(value = """
            insert into avaliacoes_treinamento (objeto, codigo_objeto_treinamentos,
              nota, versao_modelo, versao_prompt) values (:id, :treinoId, :nota, :modelo, :prompt)
            """, nativeQuery = true)
    void inserirAvaliacaoDados(@Param("id") UUID id, @Param("treinoId") UUID treinoId,
            @Param("nota") Integer nota, @Param("modelo") String modelo, @Param("prompt") String prompt);

    default void inserirAvaliacao(UUID id, UUID treinoId, GerarAvaliacaoResposta avaliacao) {
        inserirAvaliacaoDados(id, treinoId, avaliacao.nota(),
                avaliacao.versaoModelo(), avaliacao.versaoPrompt());
    }

    @Modifying
    @Query(value = """
            insert into itens_feedback_treinamento
              (objeto, codigo_objeto_avaliacoes_treinamento, tipo, ordem, texto)
            values (:id, :avaliacaoId, :tipo, :ordem, :texto)
            """, nativeQuery = true)
    void inserirItem(@Param("id") UUID id, @Param("avaliacaoId") UUID avaliacaoId,
            @Param("tipo") String tipo, @Param("ordem") int ordem, @Param("texto") String texto);

    default void inserirItens(UUID avaliacaoId, String tipo, List<String> itens) {
        for (int i = 0; i < itens.size(); i++) {
            inserirItem(UUID.randomUUID(), avaliacaoId, tipo, i + 1, itens.get(i));
        }
    }

    @Modifying
    @Query(value = """
            update treinamentos set situacao='CONCLUIDO', finalizado_em=current_timestamp where objeto=:id
            """, nativeQuery = true)
    void concluir(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            update treinamentos set situacao='CANCELADO', finalizado_em=current_timestamp
            where objeto=:id and codigo_objeto_perfis_aluno=:contaId and situacao='EM_ANDAMENTO'
            """, nativeQuery = true)
    void cancelar(@Param("id") UUID id, @Param("contaId") UUID contaId);

    @Query(value = """
            select objeto from treinamentos where codigo_objeto_perfis_aluno=:contaId
              and situacao='CONCLUIDO' order by finalizado_em desc
            """, nativeQuery = true)
    List<UUID> concluidos(@Param("contaId") UUID contaId);

    @Query(value = """
            select h.nome as nome, h.nivel as nivel from treinamentos t,
              jsonb_to_recordset(t.contexto_registrado -> 'habilidades') as h(nome text, nivel integer)
            where t.objeto=:id
            """, nativeQuery = true)
    List<HabilidadeLinha> buscarHabilidadesDoContexto(@Param("id") UUID id);

    default List<Habilidade> habilidadesDoContexto(UUID id) {
        return buscarHabilidadesDoContexto(id).stream()
                .map(item -> new Habilidade(item.getNome(), item.getNivel())).toList();
    }

    @Query(value = """
            select contexto_registrado -> 'vaga' ->> 'titulo' from treinamentos where objeto=:id
            """, nativeQuery = true)
    String tituloVaga(@Param("id") UUID id);

    @Query(value = """
            select contexto_registrado -> 'vaga' ->> 'senioridade' from treinamentos where objeto=:id
            """, nativeQuery = true)
    String senioridadeVaga(@Param("id") UUID id);

    @Query(value = """
            select jsonb_array_elements_text(contexto_registrado -> 'vaga' -> 'requisitos')
            from treinamentos where objeto=:id
            """, nativeQuery = true)
    List<String> requisitosVaga(@Param("id") UUID id);

    default Vaga vagaDoContexto(UUID id) {
        return new Vaga(tituloVaga(id), senioridadeVaga(id), requisitosVaga(id));
    }

    @Query(value = """
            select a.nota as nota,
              cast(floor(extract(epoch from t.finalizado_em) * 1000) as bigint) as "dataMillis"
            from treinamentos t join avaliacoes_treinamento a on a.codigo_objeto_treinamentos=t.objeto
            where t.objeto=:id
            """, nativeQuery = true)
    AvaliacaoLinha buscarAvaliacao(@Param("id") UUID id);

    @Query(value = """
            select texto from itens_feedback_treinamento
            where codigo_objeto_avaliacoes_treinamento =
              (select objeto from avaliacoes_treinamento where codigo_objeto_treinamentos=:id)
              and tipo=:tipo order by ordem
            """, nativeQuery = true)
    List<String> itensAvaliacao(@Param("id") UUID id, @Param("tipo") String tipo);

    default Avaliacao avaliacao(UUID id) {
        AvaliacaoLinha linha = buscarAvaliacao(id);
        return new Avaliacao(linha.getNota() == null ? 0 : linha.getNota(),
                Instant.ofEpochMilli(linha.getDataMillis()), itensAvaliacao(id, "PONTO_FORTE"),
                itensAvaliacao(id, "PONTO_MELHORIA"));
    }

    @Query(value = """
            select exists (
                select 1 from contas c
                where c.objeto=:contaId and c.tipo_conta='ALUNO' and c.situacao='ATIVA'
                  and (exists (select 1 from contratos ct join planos p on p.objeto=ct.codigo_objeto_planos
                    where ct.codigo_objeto_contas=c.objeto and ct.situacao='ATIVO' and ct.encerrado_em is null
                      and (ct.expira_em is null or ct.expira_em > current_timestamp)
                      and p.modalidade='INDIVIDUAL')
                    or exists (select 1 from vinculos_institucionais v join contratos ct on ct.objeto=v.codigo_objeto_contratos
                      where v.codigo_objeto_perfis_aluno=c.objeto and v.situacao='ATIVO' and v.desvinculado_em is null
                        and ct.situacao='ATIVO' and ct.encerrado_em is null
                        and (ct.expira_em is null or ct.expira_em > current_timestamp)))
            )
            """, nativeQuery = true)
    boolean alunoComAcesso(@Param("contaId") UUID contaId);

    interface TreinoLinha {
        UUID getId();
        String getTipo();
        String getSituacao();
        UUID getVagaId();
        String getDescricao();
    }
    interface PerguntaLinha {
        UUID getId();
        int getNumero();
        String getTexto();
        String getResposta();
    }
    interface HabilidadeLinha {
        String getNome();
        int getNivel();
    }
    interface AvaliacaoLinha {
        Integer getNota();
        long getDataMillis();
    }

    record Treino(UUID id, TipoContexto tipo, String situacao, UUID vagaId, String descricao) { }
    record Pergunta(UUID id, int numero, String texto, String resposta) { }
    record Avaliacao(int nota, Instant data, List<String> pontosFortes, List<String> pontosMelhoria) { }
}
