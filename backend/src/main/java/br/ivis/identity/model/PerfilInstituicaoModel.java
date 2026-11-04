package br.ivis.identity.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "PerfilInstituicao")
@Table(name = "perfis_instituicao")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerfilInstituicaoModel {

    @Id
    @Column(name = "objeto")
    private UUID contaId;

    @Column(name = "codigo_objeto_contas", nullable = false, updatable = false)
    private UUID codigoObjetoConta;

    @Column(name = "nome_instituicao", nullable = false, length = 200)
    private String nomeInstituicao;

    @Column(name = "nome_responsavel", nullable = false, length = 160)
    private String nomeResponsavel;

    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Column(name = "url_logo", length = 2048)
    private String urlLogo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public PerfilInstituicaoModel(UUID contaId, String nomeInstituicao, String nomeResponsavel, Instant agora) {
        this(contaId, nomeInstituicao, nomeResponsavel, null, null, null, agora);
    }

    public PerfilInstituicaoModel(UUID contaId, String nomeInstituicao, String nomeResponsavel,
            String cnpj, String telefone, String endereco, Instant agora) {
        this.contaId = contaId;
        this.codigoObjetoConta = contaId;
        this.nomeInstituicao = nomeInstituicao;
        this.nomeResponsavel = nomeResponsavel;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.endereco = endereco;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNomeInstituicao() {
        return nomeInstituicao;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void atualizarDados(String nomeInstituicao, String nomeResponsavel, String cnpj,
            String telefone, String endereco, Instant agora) {
        this.nomeInstituicao = nomeInstituicao;
        this.nomeResponsavel = nomeResponsavel;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.endereco = endereco;
        this.atualizadoEm = agora;
    }
}
