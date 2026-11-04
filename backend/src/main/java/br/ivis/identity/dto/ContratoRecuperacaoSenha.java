package br.ivis.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ContratoRecuperacaoSenha {

    private ContratoRecuperacaoSenha() {
    }

    public record SolicitacaoRequisicao(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record RedefinicaoRequisicao(
            @NotBlank @Size(max = 128) String token,
            @NotBlank @Size(min = 8, max = 128)
            @Pattern(regexp = PoliticaSenha.EXPRESSAO, message = PoliticaSenha.MENSAGEM) String novaSenha) {
    }

    public record SolicitacaoResposta(String mensagem) {
    }
}
