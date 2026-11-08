package br.ivis.plans.repository;

import br.ivis.plans.model.ContratoPlanoConsultaModel;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompraSimuladaRepository extends Repository<ContratoPlanoConsultaModel, UUID> {
    @Modifying
    @Query(value = """
            insert into contratos (objeto, codigo_objeto_contas, codigo_objeto_planos, situacao,
              quantidade_vagas, preco_centavos_contratado, moeda, iniciado_em, expira_em, encerrado_em)
            values (:id, :contaId, :planoId, 'PENDENTE', :vagas, :preco, :moeda,
              :inicio, :expiracao, :encerramento)
            """, nativeQuery = true)
    void inserirContrato(@Param("id") UUID id, @Param("contaId") UUID contaId,
            @Param("planoId") UUID planoId, @Param("vagas") int vagas,
            @Param("preco") long preco, @Param("moeda") String moeda,
            @Param("inicio") OffsetDateTime inicio, @Param("expiracao") OffsetDateTime expiracao,
            @Param("encerramento") OffsetDateTime encerramento);

    @Modifying
    @Query(value = """
            insert into pagamentos (objeto, codigo_objeto_contratos, modo, situacao,
              valor_centavos, moeda, metodo, referencia_provedor, chave_idempotencia,
              criado_em, atualizado_em, confirmado_em)
            values (:id, :contratoId, 'SIMULADO', :situacao, :valor, :moeda,
              'CARTAO_TESTE', :referencia, :chave, :agora, :agora, :agora)
            """, nativeQuery = true)
    void inserirPagamento(@Param("id") UUID id, @Param("contratoId") UUID contratoId,
            @Param("situacao") String situacao, @Param("valor") long valor,
            @Param("moeda") String moeda, @Param("referencia") String referencia,
            @Param("chave") String chave, @Param("agora") OffsetDateTime agora);

    @Modifying
    @Query(value = """
            update contratos set situacao=:situacao, atualizado_em=:agora, encerrado_em=:encerramento
            where objeto=:id
            """, nativeQuery = true)
    void atualizarContrato(@Param("id") UUID id, @Param("situacao") String situacao,
            @Param("agora") OffsetDateTime agora, @Param("encerramento") OffsetDateTime encerramento);

    @Modifying
    @Query(value = """
            insert into eventos_auditoria (codigo_objeto_contas, acao, resultado,
              tipo_recurso, codigo_objeto_contratos, detalhes)
            values (:contaId, 'CONTRATACAO_PAGAMENTO_SIMULADO', :resultado,
              'CONTRATO', :contratoId, cast(:detalhes as jsonb))
            """, nativeQuery = true)
    void registrarAuditoria(@Param("contaId") UUID contaId,
            @Param("contratoId") UUID contratoId, @Param("resultado") String resultado,
            @Param("detalhes") String detalhes);

    @Modifying
    @Query(value = """
            insert into emails_simulados (objeto, codigo_objeto_contas,
              codigo_objeto_contratos, codigo_objeto_pagamentos, destinatario, assunto, corpo, criado_em)
            values (:id, :contaId, :contratoId, :pagamentoId, :destinatario, :assunto, :corpo, :agora)
            """, nativeQuery = true)
    void inserirEmail(@Param("id") UUID id, @Param("contaId") UUID contaId,
            @Param("contratoId") UUID contratoId, @Param("pagamentoId") UUID pagamentoId,
            @Param("destinatario") String destinatario, @Param("assunto") String assunto,
            @Param("corpo") String corpo, @Param("agora") OffsetDateTime agora);

    @Query(value = """
            select c.objeto as "contratoId", pg.objeto as "pagamentoId", p.objeto as "planoId",
              p.nome as "nomePlano", c.situacao as "situacaoContrato",
              pg.situacao as "situacaoPagamento", pg.modo as "modoPagamento",
              pg.valor_centavos as "valorCentavos", pg.moeda as moeda,
              cast(floor(extract(epoch from c.expira_em) * 1000) as bigint) as "expiraEmMillis",
              e.destinatario as destinatario, e.assunto as assunto, e.corpo as corpo,
              cast(floor(extract(epoch from e.criado_em) * 1000) as bigint) as "emailCriadoEmMillis"
            from pagamentos pg join contratos c on c.objeto=pg.codigo_objeto_contratos
            join planos p on p.objeto=c.codigo_objeto_planos
            left join emails_simulados e on e.codigo_objeto_pagamentos=pg.objeto
            where pg.chave_idempotencia=:chave
            """, nativeQuery = true)
    CompraAnterior buscarCompraAnterior(@Param("chave") String chave);

    @Query(value = """
            select exists (select 1 from vinculos_institucionais v
              join contratos c on c.objeto=v.codigo_objeto_contratos
              where v.codigo_objeto_perfis_aluno=:contaId and v.situacao='ATIVO'
                and v.desvinculado_em is null and v.vinculado_em <= current_timestamp
                and c.situacao='ATIVO' and c.encerrado_em is null
                and (c.expira_em is null or c.expira_em > current_timestamp))
            """, nativeQuery = true)
    boolean temVinculoInstitucionalAtivo(@Param("contaId") UUID contaId);

    @Query(value = """
            select exists (select 1 from contratos c join planos p on p.objeto=c.codigo_objeto_planos
              where c.codigo_objeto_contas=:contaId and p.modalidade=:modalidade
                and c.situacao='ATIVO' and c.encerrado_em is null
                and (c.expira_em is null or c.expira_em > current_timestamp))
            """, nativeQuery = true)
    boolean temContratoAtivo(@Param("contaId") UUID contaId, @Param("modalidade") String modalidade);

    interface CompraAnterior {
        UUID getContratoId();
        UUID getPagamentoId();
        UUID getPlanoId();
        String getNomePlano();
        String getSituacaoContrato();
        String getSituacaoPagamento();
        String getModoPagamento();
        long getValorCentavos();
        String getMoeda();
        Long getExpiraEmMillis();
        String getDestinatario();
        String getAssunto();
        String getCorpo();
        Long getEmailCriadoEmMillis();
    }
}
