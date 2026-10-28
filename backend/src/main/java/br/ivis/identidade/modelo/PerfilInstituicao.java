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
@Table(name = "perfis_instituicao")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerfilInstituicao {

    @Id
    @Column(name = "conta_id")
    private UUID contaId;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "nome_administrador", nullable = false, length = 160)
    private String nomeAdministrador;

    @Column(name = "documento_fiscal", length = 32)
    private String documentoFiscal;

    @Column(name = "url_logo", length = 2048)
    private String urlLogo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
public PerfilInstituicao(UUID contaId, String razaoSocial, String nomeAdministrador, Instant agora) {
        this.contaId = contaId;
        this.razaoSocial = razaoSocial;
        this.nomeAdministrador = nomeAdministrador;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeAdministrador() {
        return nomeAdministrador;
    }

    public void atualizarNomeAdministrador(String nomeAdministrador, Instant agora) {
        this.nomeAdministrador = nomeAdministrador;
        this.atualizadoEm = agora;
    }

    public void atualizarRazaoSocial(String razaoSocial, Instant agora) {
        this.razaoSocial = razaoSocial;
        this.atualizadoEm = agora;
    }
}
