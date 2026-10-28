package br.ivis.identidade.modelo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contratos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContratoInstitucional {

    @Id
    private UUID id;

    @Column(name = "conta_titular_id", nullable = false)
    private UUID contaTitularId;

    @Column(name = "plano_id", nullable = false)
    private UUID planoId;

    @Column(nullable = false, length = 20)
    private String situacao;

    @Column(name = "quantidade_vagas", nullable = false)
    private int quantidadeVagas;

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Column(name = "expira_em")
    private Instant expiraEm;
public ContratoInstitucional(
            UUID id, UUID contaTitularId, UUID planoId, String situacao, int quantidadeVagas,
            Instant iniciadoEm, Instant encerradoEm) {
        this.id = id;
        this.contaTitularId = contaTitularId;
        this.planoId = planoId;
        this.situacao = situacao;
        this.quantidadeVagas = quantidadeVagas;
        this.iniciadoEm = iniciadoEm;
        this.encerradoEm = encerradoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaTitularId() {
        return contaTitularId;
    }

    public UUID getPlanoId() {
        return planoId;
    }

    public String getSituacao() {
        return situacao;
    }

    public int getQuantidadeVagas() {
        return quantidadeVagas;
    }

    public boolean estaAtivo() {
        return "ATIVO".equals(situacao) && encerradoEm == null
                && (expiraEm == null || expiraEm.isAfter(Instant.now()));
    }
}
