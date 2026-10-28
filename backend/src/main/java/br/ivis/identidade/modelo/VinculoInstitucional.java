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
@Table(name = "vinculos_institucionais")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VinculoInstitucional {

    @Id
    private UUID id;

    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;

    @Column(name = "conta_aluno_id", nullable = false)
    private UUID contaAlunoId;

    @Column(nullable = false, length = 20)
    private String situacao;

    @Column(name = "vinculado_em", nullable = false)
    private Instant vinculadoEm;

    @Column(name = "desvinculado_em")
    private Instant desvinculadoEm;

    @Column(name = "criado_por_conta_id", nullable = false)
    private UUID criadoPorContaId;
public VinculoInstitucional(
            UUID id, UUID contratoId, UUID contaAlunoId, UUID criadoPorContaId, Instant vinculadoEm) {
        this.id = id;
        this.contratoId = contratoId;
        this.contaAlunoId = contaAlunoId;
        this.criadoPorContaId = criadoPorContaId;
        this.vinculadoEm = vinculadoEm;
        this.situacao = "ATIVO";
    }

    public UUID getId() {
        return id;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public UUID getContaAlunoId() {
        return contaAlunoId;
    }

    public String getSituacao() {
        return situacao;
    }

    public Instant getVinculadoEm() {
        return vinculadoEm;
    }

    public Instant getDesvinculadoEm() {
        return desvinculadoEm;
    }

    public void encerrar(Instant agora) {
        this.situacao = "ENCERRADO";
        this.desvinculadoEm = agora;
    }
}
