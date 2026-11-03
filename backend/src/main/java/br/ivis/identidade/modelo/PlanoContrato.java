package br.ivis.identidade.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "planos")
public class PlanoContrato {

    protected PlanoContrato() {
    }

    @Id
    @Column(name = "objeto")
    private UUID id;

    @Column(nullable = false, length = 20)
    private String modalidade;
public String getModalidade() {
        return modalidade;
    }

public UUID getId() {
    return id;
}
}
