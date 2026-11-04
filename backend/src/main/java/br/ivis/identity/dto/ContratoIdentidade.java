package br.ivis.identity.dto;

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
            @NotBlank @Size(min = 11, max = 18) String cpf,
            @NotBlank @Size(max = 30) String telefone,
            @NotBlank @Size(max = 500) String endereco,
            @NotBlank @Size(min = 12, max = 128) String senha) {
    }

    public record CadastroInstituicaoRequisicao(
            @NotBlank @Size(max = 160) String nomeResponsavel,
            @NotBlank @Size(max = 200) String nomeInstituicao,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 128) String senha,
            @NotBlank @Size(min = 14, max = 18) String cnpj,
            @NotBlank @Size(max = 30) String telefone,
            @NotBlank @Size(max = 500) String endereco) {
    }

    public record LoginRequisicao(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 1, max = 128) String senha) {
    }

    public record AtualizacaoContaRequisicao(
            @Size(max = 160) String nome,
            @Size(max = 160) String nomeResponsavel,
            @Size(max = 200) String nomeInstituicao,
            @Email @Size(max = 254) String email,
            @Size(min = 11, max = 18) String cpf,
            @Size(max = 30) String telefone,
            @Size(max = 500) String endereco,
            @Size(min = 14, max = 18) String cnpj) {
    }

    public record TrocaSenhaRequisicao(
            @NotBlank @Size(max = 128) String senhaAtual,
            @NotBlank @Size(min = 12, max = 128) String novaSenha) {
    }

    public record ContaResposta(
            UUID id,
            String tipoConta,
            String email,
            String nome,
            String nomeResponsavel,
            String cpf,
            String telefone,
            String endereco,
            UUID objetoInstituicao,
            String cnpj) {
    }

    public record SessaoResposta(String accessToken, String tipoToken, long expiraEmSegundos, ContaResposta conta) {
    }

    public record TokenCsrfResposta(String token) {
    }
}
