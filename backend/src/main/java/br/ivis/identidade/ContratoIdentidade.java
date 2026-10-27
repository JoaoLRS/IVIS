package br.ivis.identidade;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ContratoIdentidade {

    private ContratoIdentidade() {
    }

    public record CadastroAlunoRequisicao(
            @NotBlank @Size(max = 160) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 128) String senha) {
    }

    public record CadastroInstituicaoRequisicao(
            @NotBlank @Size(max = 200) String razaoSocial,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 128) String senha) {
    }

    public record LoginRequisicao(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 1, max = 128) String senha) {
    }

    public record AtualizacaoContaRequisicao(
            @Size(max = 160) String nome,
            @Size(max = 200) String razaoSocial,
            @Email @Size(max = 254) String email) {
    }

    public record TrocaSenhaRequisicao(
            @NotBlank @Size(max = 128) String senhaAtual,
            @NotBlank @Size(min = 12, max = 128) String novaSenha) {
    }

    public record ContaResposta(UUID id, String tipoConta, String email, String nome) {
    }

    public record SessaoResposta(String accessToken, String tipoToken, long expiraEmSegundos, ContaResposta conta) {
    }

    public record TokenCsrfResposta(String token) {
    }
}
