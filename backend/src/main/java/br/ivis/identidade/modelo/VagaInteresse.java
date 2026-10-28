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
@Table(name = "vagas_interesse")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VagaInteresse {

    @Id
    private UUID id;

    @Column(name = "conta_aluno_id", nullable = false)
    private UUID contaAlunoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 200)
    private String empresa;

    @Column(nullable = false, length = 80)
    private String senioridade;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "arquivado_em")
    private Instant arquivadoEm;
public VagaInteresse(
            UUID id, UUID contaAlunoId, String titulo, String empresa, String senioridade,
            String descricao, Instant agora) {
        this.id = id;
        this.contaAlunoId = contaAlunoId;
        this.titulo = titulo;
        this.empresa = empresa;
        this.senioridade = senioridade;
        this.descricao = descricao;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaAlunoId() {
        return contaAlunoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getSenioridade() {
        return senioridade;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getArquivadoEm() {
        return arquivadoEm;
    }

    public boolean estaAtiva() {
        return arquivadoEm == null;
    }

    public void atualizar(String titulo, String empresa, String senioridade, String descricao, Instant agora) {
        this.titulo = titulo;
        this.empresa = empresa;
        this.senioridade = senioridade;
        this.descricao = descricao;
        this.atualizadoEm = agora;
    }

    public void arquivar(Instant agora) {
        this.arquivadoEm = agora;
        this.atualizadoEm = agora;
    }
}
