package br.ivis.identidade.dto;

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
            @Size(max = 160) String nomeAdministrador,
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
            @Size(max = 160) String nomeAdministrador,
            @Size(max = 200) String razaoSocial,
            @Email @Size(max = 254) String email) {
    }

    public record TrocaSenhaRequisicao(
            @NotBlank @Size(max = 128) String senhaAtual,
            @NotBlank @Size(min = 12, max = 128) String novaSenha) {
    }

    public record ContaResposta(UUID id, String tipoConta, String email, String nome, String nomeAdministrador) {
        public ContaResposta(UUID id, String tipoConta, String email, String nome) {
            this(id, tipoConta, email, nome, null);
        }
    }

    public record SessaoResposta(String accessToken, String tipoToken, long expiraEmSegundos, ContaResposta conta) {
    }

    public record TokenCsrfResposta(String token) {
    }
}
