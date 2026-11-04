package br.ivis.identity.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import br.ivis.identity.dto.ContratoIdentidade.ContaResposta;

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

    public record CadastroAlunoRequisicao(
            @NotNull UUID contratoId,
            @NotBlank @Size(max = 160) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 11, max = 18) String cpf,
            @NotBlank @Size(max = 30) String telefone,
            @NotBlank @Size(max = 500) String endereco,
            @NotBlank @Size(min = 8, max = 128)
            @Pattern(regexp = PoliticaSenha.EXPRESSAO, message = PoliticaSenha.MENSAGEM) String senha) {
    }

    public record CriacaoVinculoResposta(VinculoInstitucionalResumo vinculo, Capacidade capacidade) {
    }

    public record CadastroAlunoResposta(ContaResposta aluno, CriacaoVinculoResposta vinculacao) {
    }
}
