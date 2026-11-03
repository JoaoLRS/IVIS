package br.ivis.identidade.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "requisitos_vaga")
public class RequisitoVaga {

    protected RequisitoVaga() {
    }

    @Id
    @Column(name = "objeto")
    private UUID id;

    @Column(name = "codigo_objeto_vagas_interesse", nullable = false)
    private UUID vagaInteresseId;

    @Column(name = "texto_requisito", nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private short ordem;
public RequisitoVaga(UUID id, UUID vagaInteresseId, String texto, short ordem) {
        this.id = id;
        this.vagaInteresseId = vagaInteresseId;
        this.texto = texto;
        this.ordem = ordem;
    }

    public String getTexto() {
        return texto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVagaInteresseId() {
        return vagaInteresseId;
    }

    public short getOrdem() {
        return ordem;
    }
}
