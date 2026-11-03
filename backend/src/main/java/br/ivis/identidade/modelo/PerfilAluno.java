package br.ivis.identidade.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "perfis_aluno")
public class PerfilAluno {

    protected PerfilAluno() {
    }

    @Id
    @Column(name = "objeto")
    private UUID contaId;

    @Column(name = "codigo_objeto_contas", nullable = false, updatable = false)
    @SuppressWarnings("unused")
    private UUID codigoObjetoConta;

    @Column(name = "nome", nullable = false, length = 160)
    private String nome;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Column(name = "codigo_objeto_perfis_instituicao")
    private UUID objetoInstituicao;

    @Column(name = "url_foto", length = 2048)
    private String urlFoto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public PerfilAluno(UUID contaId, String nome, Instant agora) {
        this(contaId, nome, null, null, null, null, agora);
    }

    public PerfilAluno(UUID contaId, String nome, String cpf, String telefone,
            String endereco, UUID objetoInstituicao, Instant agora) {
        this.contaId = contaId;
        this.codigoObjetoConta = contaId;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.endereco = endereco;
        this.objetoInstituicao = objetoInstituicao;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public UUID getObjetoInstituicao() {
        return objetoInstituicao;
    }

    public void atualizarDados(String nome, String cpf, String telefone, String endereco, Instant agora) {
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.endereco = endereco;
        this.atualizadoEm = agora;
    }

    public void atualizarInstituicao(UUID objetoInstituicao, Instant agora) {
        this.objetoInstituicao = objetoInstituicao;
        this.atualizadoEm = agora;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void atualizarNome(String nome, Instant agora) {
        this.nome = nome;
        this.atualizadoEm = agora;
    }
}
