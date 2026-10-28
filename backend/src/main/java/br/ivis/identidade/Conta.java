package br.ivis.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    private UUID id;

    @Column(name = "tipo_conta", nullable = false, length = 20)
    private String tipoConta;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "email_normalizado", nullable = false, unique = true, length = 320)
    private String emailNormalizado;

    @Column(name = "hash_senha", nullable = false, length = 255)
    private String hashSenha;

    @Column(nullable = false, length = 20)
    private String situacao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Conta() {
    }

    public Conta(UUID id, String tipoConta, String email, String emailNormalizado, String hashSenha, Instant agora) {
        this.id = id;
        this.tipoConta = tipoConta;
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.hashSenha = hashSenha;
        this.situacao = "ATIVA";
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getId() {
        return id;
    }

    public String getTipoConta() {
        return tipoConta;
    }

    public String getEmail() {
        return email;
    }

    public String getHashSenha() {
        return hashSenha;
    }

    public void atualizarEmail(String email, String emailNormalizado, Instant agora) {
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.atualizadoEm = agora;
    }

    public void atualizarSenha(String hashSenha, Instant agora) {
        this.hashSenha = hashSenha;
        this.atualizadoEm = agora;
    }

    public boolean estaAtiva() {
        return "ATIVA".equals(situacao);
    }
}
