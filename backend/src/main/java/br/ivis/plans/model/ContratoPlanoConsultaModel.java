package br.ivis.plans.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Projeção de leitura do contrato, independente da gestão de vínculos. */
@Entity(name = "ContratoPlanoConsulta")
@Immutable
@Table(name = "contratos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContratoPlanoConsultaModel {
    @Id
    @Column(name = "objeto")
    private UUID id;
    @Column(name = "codigo_objeto_contas")
    private UUID contaTitularId;
    @Column(name = "codigo_objeto_planos")
    private UUID planoId;
    private String situacao;
    @Column(name = "quantidade_vagas")
    private int quantidadeVagas;
    @Column(name = "preco_centavos_contratado")
    private long precoCentavosContratado;
    @Column(columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String moeda;
    @Column(name = "iniciado_em")
    private Instant iniciadoEm;
    @Column(name = "encerrado_em")
    private Instant encerradoEm;
    @Column(name = "expira_em")
    private Instant expiraEm;
public UUID getId() { return id; }
    public UUID getContaTitularId() { return contaTitularId; }
    public UUID getPlanoId() { return planoId; }
    public String getSituacao() { return situacao; }
    public int getQuantidadeVagas() { return quantidadeVagas; }
    public long getPrecoCentavosContratado() { return precoCentavosContratado; }
    public String getMoeda() { return moeda; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getEncerradoEm() { return encerradoEm; }
    public Instant getExpiraEm() { return expiraEm; }
}
