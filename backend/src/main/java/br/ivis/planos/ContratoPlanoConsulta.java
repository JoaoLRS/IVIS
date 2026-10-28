package br.ivis.planos;

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
@Entity
@Immutable
@Table(name = "contratos")
public class ContratoPlanoConsulta {
    @Id
    private UUID id;
    @Column(name = "conta_titular_id")
    private UUID contaTitularId;
    @Column(name = "plano_id")
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

    protected ContratoPlanoConsulta() { }

    public UUID getId() { return id; }
    public UUID getContaTitularId() { return contaTitularId; }
    public UUID getPlanoId() { return planoId; }
    public String getSituacao() { return situacao; }
    public int getQuantidadeVagas() { return quantidadeVagas; }
    public long getPrecoCentavosContratado() { return precoCentavosContratado; }
    public String getMoeda() { return moeda; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getEncerradoEm() { return encerradoEm; }
}
