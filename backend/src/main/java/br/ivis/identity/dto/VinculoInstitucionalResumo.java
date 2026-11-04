package br.ivis.identity.dto;

import java.time.Instant;
import java.util.UUID;

public record VinculoInstitucionalResumo(
        UUID id, UUID contratoId, UUID contaAlunoId, String nomeAluno, String emailAluno, Instant vinculadoEm) {
}
