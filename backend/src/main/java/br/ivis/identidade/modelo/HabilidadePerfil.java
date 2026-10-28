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
@Table(name = "habilidades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabilidadePerfil {

    @Id
    private UUID id;

    @Column(name = "conta_aluno_id", nullable = false)
    private UUID contaAlunoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 120)
    private String nomeNormalizado;

    @Column(length = 80)
    private String categoria;

    @Column(nullable = false)
    private short nivel;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
public HabilidadePerfil(
            UUID id, UUID contaAlunoId, String nome, String nomeNormalizado,
            String categoria, short nivel, Instant agora) {
        this.id = id;
        this.contaAlunoId = contaAlunoId;
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.categoria = categoria;
        this.nivel = nivel;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaAlunoId() {
        return contaAlunoId;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public short getNivel() {
        return nivel;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void atualizar(String nome, String nomeNormalizado, String categoria, short nivel, Instant agora) {
        this.nome = nome;
        this.nomeNormalizado = nomeNormalizado;
        this.categoria = categoria;
        this.nivel = nivel;
        this.atualizadoEm = agora;
    }
}
