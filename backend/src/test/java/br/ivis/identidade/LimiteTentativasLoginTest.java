package br.ivis.identidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ivis.comum.erro.ExcecaoNegocio;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.ivis.identidade.servico.LimiteTentativasLogin;

class LimiteTentativasLoginTest {

    @Test
    void limitaTentativasPorContaEOrigemSemGuardarEmailEmClaro() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(2, Duration.ofMinutes(15));

        limite.verificar("aluno@exemplo.test", "127.0.0.1");
        limite.registrarFalha("aluno@exemplo.test", "127.0.0.1");
        limite.verificar("aluno@exemplo.test", "127.0.0.1");
        limite.registrarFalha("aluno@exemplo.test", "127.0.0.1");

        ExcecaoNegocio excecao = assertThrows(
                ExcecaoNegocio.class, () -> limite.verificar("aluno@exemplo.test", "127.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, excecao.getStatus());
        assertEquals("limite_excedido", excecao.getCodigo());
    }

    @Test
    void sucessoLiberaOsContadoresDaContaEOrigem() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(1, Duration.ofMinutes(15));
        limite.registrarFalha("aluno@exemplo.test", "127.0.0.1");
        limite.registrarSucesso("aluno@exemplo.test", "127.0.0.1");

        limite.verificar("aluno@exemplo.test", "127.0.0.1");
    }
}
