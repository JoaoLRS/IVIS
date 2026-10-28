package br.ivis.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessoes_autenticacao")
public class SessaoAutenticacao {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(name = "hash_token_renovacao", nullable = false, unique = true, length = 255)
    private String hashTokenRenovacao;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Column(name = "substituida_por_id")
    private UUID substituidaPorId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected SessaoAutenticacao() {
    }

    public SessaoAutenticacao(UUID id, UUID contaId, String hashTokenRenovacao, Instant expiraEm, Instant agora) {
        this.id = id;
        this.contaId = contaId;
        this.hashTokenRenovacao = hashTokenRenovacao;
        this.expiraEm = expiraEm;
        this.criadoEm = agora;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean estaAtivaEm(Instant agora) {
        return revogadaEm == null && expiraEm.isAfter(agora);
    }

    public void revogar(Instant agora, UUID substitutaId) {
        this.revogadaEm = agora;
        this.substituidaPorId = substitutaId;
    }
}
