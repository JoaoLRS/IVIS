package br.ivis.identidade.modelo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "planos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanoContrato {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String modalidade;
public String getModalidade() {
        return modalidade;
    }
}
