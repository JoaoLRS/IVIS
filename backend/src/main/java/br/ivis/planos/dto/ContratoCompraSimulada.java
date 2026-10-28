package br.ivis.planos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ContratoCompraSimulada {
    private ContratoCompraSimulada() { }

    public record Requisicao(
            @NotNull UUID planoId,
            @NotBlank @Size(max = 23) String numeroCartao,
            @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])/(\\d{2}|\\d{4})$") String validade,
            @NotBlank @Pattern(regexp = "^\\d{3,4}$") String cvv) { }

    public record EmailSimulado(
            String destinatario,
            String assunto,
            String corpo,
            Instant criadoEm) { }

    public record Resposta(
            UUID contratoId,
            UUID pagamentoId,
            UUID planoId,
            String nomePlano,
            String situacaoContrato,
            String situacaoPagamento,
            String modoPagamento,
            long valorCentavos,
            String moeda,
            Instant expiraEm,
            boolean reprocessada,
            EmailSimulado emailConfirmacao) { }
}
