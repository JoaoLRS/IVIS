package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ivis.common.error.ExcecaoNegocio;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.ivis.identity.service.LimiteRecuperacaoSenha;

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
