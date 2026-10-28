package br.ivis.planos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContratoConsultaPlanos {
    private ContratoConsultaPlanos() { }

    public record Oferta(UUID id, String codigo, String modalidade, String nome, String descricao,
                         long precoCentavos, String moeda, List<String> beneficios) { }

    public record PlanoVigente(UUID contratoId, UUID planoId, String nomePlano,
                              String origemAcesso, String situacao, Instant iniciadoEm,
                              @Schema(description = "Data de encerramento, não é uma data de expiração prevista.")
                              Instant encerradoEm,
                              @Schema(description = "Somente o titular recebe preço e capacidade do contrato.")
                              CondicoesTitular condicoesTitular,
                              List<String> beneficios) { }

    public record CondicoesTitular(int quantidadeVagas, long precoCentavosContratado, String moeda) { }

    public record PlanoDaConta(String situacao, List<PlanoVigente> planos) { }
}
