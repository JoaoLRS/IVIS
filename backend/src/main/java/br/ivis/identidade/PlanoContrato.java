package br.ivis.identidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "planos")
public class PlanoContrato {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String modalidade;

    protected PlanoContrato() {
    }

    public String getModalidade() {
        return modalidade;
    }
}
