package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.ivis.identity.dto.ContratoIdentidade.CadastroAlunoRequisicao;
import br.ivis.identity.dto.ContratoIdentidade.TrocaSenhaRequisicao;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class PoliticaSenhaTest {
    @Test
    void cadastroExigeTamanhoEMisturaDeCaracteres() {
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator validador = fabrica.getValidator();
            assertThat(validador.validate(cadastro("Abc#1234"))).isEmpty();
            for (String senha : new String[] {
                    "Ab#1234", "abcdefgh1#", "ABCDEFGH1#", "Abcdefgh#", "Abcdefg1"
            }) {
                assertThat(validador.validate(cadastro(senha)))
                        .as("Senha inválida: %s", senha)
                        .anyMatch(erro -> erro.getPropertyPath().toString().equals("senha"));
            }
        }
    }

    @Test
    void trocaDeSenhaUsaAMesmaPoliticaDoCadastro() {
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator validador = fabrica.getValidator();
            assertThat(validador.validate(new TrocaSenhaRequisicao("atual", "NovaSenha#123"))).isEmpty();
            assertThat(validador.validate(new TrocaSenhaRequisicao("atual", "novasenha")))
                    .anyMatch(erro -> erro.getPropertyPath().toString().equals("novaSenha"));
        }
    }

    private CadastroAlunoRequisicao cadastro(String senha) {
        return new CadastroAlunoRequisicao("Aluno", "aluno@ivis.test", "12345678909",
                "11999999999", "Rua A", senha);
    }
}
