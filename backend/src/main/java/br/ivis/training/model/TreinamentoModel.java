package br.ivis.training.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity(name = "Treinamento")
@Immutable
@Table(name = "treinamentos")
public class TreinamentoModel {
    @Id
    @Column(name = "objeto")
    private UUID id;

    @Column(name = "codigo_objeto_perfis_aluno")
    private UUID contaAlunoId;

    @Column(name = "codigo_objeto_vagas_interesse")
    private UUID vagaId;

    @Column(name = "tipo_contexto")
    private String tipoContexto;

    private String situacao;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    protected TreinamentoModel() { }

    public UUID getId() { return id; }
}
