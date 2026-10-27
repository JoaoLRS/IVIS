package br.ivis.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ivis.comum.erro.ExcecaoNegocio;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LimiteRecuperacaoSenhaTest {

    @Test
    void limitaSolicitacoesPorEmailEPorIp() {
        LimiteRecuperacaoSenha limite = new LimiteRecuperacaoSenha(2, Duration.ofMinutes(15));
        limite.registrarSolicitacao("aluno@example.com", "127.0.0.1");
        limite.registrarSolicitacao("aluno@example.com", "127.0.0.1");

        assertThatThrownBy(() -> limite.registrarSolicitacao("aluno@example.com", "127.0.0.1"))
                .isInstanceOfSatisfying(ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> limite.registrarSolicitacao("outro@example.com", "127.0.0.1"))
                .isInstanceOf(ExcecaoNegocio.class);
    }
}
