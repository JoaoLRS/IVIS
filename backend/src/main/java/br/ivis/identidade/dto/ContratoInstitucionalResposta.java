package br.ivis.identidade.dto;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public final class ContratoInstitucionalResposta {

    private ContratoInstitucionalResposta() {
    }

    public record Capacidade(
            UUID contratoId, int quantidadeVagas, long vagasUtilizadas, long vagasDisponiveis) {
    }

    public record CriacaoVinculoRequisicao(@NotNull UUID contratoId, @NotNull UUID contaAlunoId) {
    }

    public record CriacaoVinculoPorEmailRequisicao(
            @NotNull UUID contratoId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String emailAluno) {
    }

    public record CriacaoVinculoResposta(VinculoInstitucionalResumo vinculo, Capacidade capacidade) {
    }
}
