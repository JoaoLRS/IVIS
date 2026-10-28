package br.ivis.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "perfis_aluno")
public class PerfilAluno {

    @Id
    @Column(name = "conta_id")
    private UUID contaId;

    @Column(name = "nome_exibicao", nullable = false, length = 160)
    private String nomeExibicao;

    @Column(name = "url_foto", length = 2048)
    private String urlFoto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PerfilAluno() {
    }

    public PerfilAluno(UUID contaId, String nomeExibicao, Instant agora) {
        this.contaId = contaId;
        this.nomeExibicao = nomeExibicao;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public void atualizarNome(String nome, Instant agora) {
        this.nomeExibicao = nome;
        this.atualizadoEm = agora;
    }
}
