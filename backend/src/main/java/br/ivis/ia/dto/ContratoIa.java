package br.ivis.ia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class ContratoIa {

    private ContratoIa() {
    }

    public enum TipoContexto {
        HABILIDADES,
        VAGA
    }

    public record Habilidade(
            @NotBlank String nome,
            @NotNull @Min(1) @Max(5) Integer nivel) {
    }

    public record Vaga(
            @NotBlank String titulo,
            @NotBlank String senioridade,
            @NotNull @Size(max = 20) List<@NotBlank String> requisitos) {
    }

    public record Interacao(
            @NotNull @Min(1) Integer numero,
            @NotBlank @Size(max = 1000) String pergunta,
            @NotNull @Size(max = 4000) String resposta) {
    }

    public record GerarPerguntaEntrada(
            @NotNull @JsonProperty("tipo_contexto") TipoContexto tipoContexto,
            @Size(max = 30) List<@Valid Habilidade> habilidades,
            @Valid Vaga vaga,
            @NotNull @Size(max = 10) List<@Valid Interacao> interacoes,
            @NotNull @Min(1) @JsonProperty("numero_pergunta") Integer numeroPergunta,
            @NotNull @Min(1) @Max(10) @JsonProperty("total_perguntas") Integer totalPerguntas) {

        @AssertTrue(message = "O contexto informado não corresponde ao tipo selecionado.")
        public boolean isContextoConsistente() {
            return contextoValido(tipoContexto, habilidades, vaga);
        }
    }

    public record GerarAvaliacaoEntrada(
            @NotNull @JsonProperty("tipo_contexto") TipoContexto tipoContexto,
            @Size(max = 30) List<@Valid Habilidade> habilidades,
            @Valid Vaga vaga,
            @NotNull @Size(max = 10) List<@Valid Interacao> interacoes) {

        @AssertTrue(message = "O contexto informado não corresponde ao tipo selecionado.")
        public boolean isContextoConsistente() {
            return contextoValido(tipoContexto, habilidades, vaga);
        }
    }

    public record GerarPerguntaRequisicao(
            @JsonProperty("id_correlacao") UUID idCorrelacao,
            @JsonProperty("tipo_contexto") TipoContexto tipoContexto,
            List<Habilidade> habilidades,
            Vaga vaga,
            List<Interacao> interacoes,
            @JsonProperty("numero_pergunta") Integer numeroPergunta,
            @JsonProperty("total_perguntas") Integer totalPerguntas) {
    }

    public record GerarAvaliacaoRequisicao(
            @JsonProperty("id_correlacao") UUID idCorrelacao,
            @JsonProperty("tipo_contexto") TipoContexto tipoContexto,
            List<Habilidade> habilidades,
            Vaga vaga,
            List<Interacao> interacoes) {
    }

    public record PerguntaGerada(
            @NotBlank @Size(max = 1000) String texto) {
    }

    public record GerarPerguntaResposta(
            @NotNull @Valid PerguntaGerada pergunta,
            @NotBlank @JsonProperty("versao_modelo") String versaoModelo,
            @NotBlank @JsonProperty("versao_prompt") String versaoPrompt) {
    }

    public record GerarAvaliacaoResposta(
            @NotNull @Min(0) @Max(100) Integer nota,
            @NotNull @Size(min = 1, max = 5) @JsonProperty("pontos_fortes")
                    List<@NotBlank String> pontosFortes,
            @NotNull @Size(min = 1, max = 5) @JsonProperty("pontos_melhoria")
                    List<@NotBlank String> pontosMelhoria,
            @NotBlank @JsonProperty("versao_modelo") String versaoModelo,
            @NotBlank @JsonProperty("versao_prompt") String versaoPrompt) {
    }

    private static boolean contextoValido(
            TipoContexto tipoContexto, List<Habilidade> habilidades, Vaga vaga) {
        if (tipoContexto == TipoContexto.HABILIDADES) {
            return habilidades != null && !habilidades.isEmpty() && vaga == null;
        }
        return tipoContexto == TipoContexto.VAGA && vaga != null && habilidades == null;
    }
}
