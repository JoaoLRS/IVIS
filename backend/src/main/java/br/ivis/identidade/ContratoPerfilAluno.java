package br.ivis.identidade;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContratoPerfilAluno {

    private ContratoPerfilAluno() {
    }

    public record HabilidadeRequisicao(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 80) String categoria,
            @NotNull @Min(1) @Max(5) Integer nivel) {
    }

    public record HabilidadeResposta(
            UUID id, String nome, String categoria, short nivel, Instant criadoEm, Instant atualizadoEm) {
    }

    public record VagaRequisicao(
            @NotBlank @Size(max = 200) String titulo,
            @Size(max = 200) String empresa,
            @NotBlank @Size(max = 80) String senioridade,
            @Size(max = 10000) String descricao,
            @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 500) String> requisitos) {
    }

    public record VagaResposta(
            UUID id,
            String titulo,
            String empresa,
            String senioridade,
            String descricao,
            List<String> requisitos,
            Instant criadoEm,
            Instant atualizadoEm) {
    }
}
