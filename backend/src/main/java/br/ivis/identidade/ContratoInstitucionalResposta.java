package br.ivis.identidade;

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

    public record CriacaoVinculoResposta(VinculoInstitucionalResumo vinculo, Capacidade capacidade) {
    }
}
