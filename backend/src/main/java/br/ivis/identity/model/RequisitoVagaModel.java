package br.ivis.identity.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity(name = "RequisitoVaga")
@Table(name = "requisitos_vaga")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequisitoVagaModel {

    @Id
    @Column(name = "objeto")
    private UUID id;

    @Column(name = "codigo_objeto_vagas_interesse", nullable = false)
    private UUID vagaInteresseId;

    @Column(name = "texto_requisito", nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private short ordem;
public RequisitoVagaModel(UUID id, UUID vagaInteresseId, String texto, short ordem) {
        this.id = id;
        this.vagaInteresseId = vagaInteresseId;
        this.texto = texto;
        this.ordem = ordem;
    }

    public String getTexto() {
        return texto;
    }

    public short getOrdem() {
        return ordem;
    }
}
