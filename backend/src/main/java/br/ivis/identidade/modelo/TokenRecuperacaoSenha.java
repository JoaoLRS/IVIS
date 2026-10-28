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
@Table(name = "tokens_recuperacao_senha")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenRecuperacaoSenha {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(name = "hash_token", nullable = false, unique = true, length = 255)
    private String hashToken;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "consumido_em")
    private Instant consumidoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
public TokenRecuperacaoSenha(UUID id, UUID contaId, String hashToken, Instant expiraEm, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.hashToken = hashToken;
        this.expiraEm = expiraEm;
        this.criadoEm = criadoEm;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getHashToken() {
        return hashToken;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean estaValidoEm(Instant agora) {
        return consumidoEm == null && expiraEm.isAfter(agora);
    }

    public void consumir(Instant agora) {
        this.consumidoEm = agora;
    }
}
