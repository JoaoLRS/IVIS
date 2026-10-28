package br.ivis.planos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Leitura do catálogo; administração de ofertas não faz parte desta API pública. */
@Entity
@Immutable
@Table(name = "planos")
public class Plano {
    @Id
    private UUID id;
    private String codigo;
    private String modalidade;
    private String nome;
    @Column(columnDefinition = "text")
    private String descricao;
    @Column(name = "preco_centavos")
    private long precoCentavos;
    @Column(columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String moeda;
    private boolean ativo;

    protected Plano() { }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getModalidade() { return modalidade; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public long getPrecoCentavos() { return precoCentavos; }
    public String getMoeda() { return moeda; }
    public boolean isAtivo() { return ativo; }
}
