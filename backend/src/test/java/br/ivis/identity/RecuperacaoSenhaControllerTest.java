package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ivis.identity.controller.RecuperacaoSenhaController;
import br.ivis.identity.dto.ContratoRecuperacaoSenha.RedefinicaoRequisicao;
import br.ivis.identity.dto.ContratoRecuperacaoSenha.ValidacaoCodigoRequisicao;
import br.ivis.identity.service.RecuperacaoSenhaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RecuperacaoSenhaControllerTest {

    @Test
    void codigoValidoCriaCookieRestritoEConsumoRemoveCookie() {
        RecuperacaoSenhaService servico = mock(RecuperacaoSenhaService.class);
        RecuperacaoSenhaController controller = new RecuperacaoSenhaController(servico, true);
        MockHttpServletRequest pedido = new MockHttpServletRequest();
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        String autorizacao = "A".repeat(43);
        when(servico.validarCodigo("aluno@example.com", "12345678", pedido.getRemoteAddr()))
                .thenReturn(autorizacao);

        controller.validarCodigo(new ValidacaoCodigoRequisicao("aluno@example.com", "12345678"),
                pedido, resposta);

        String cookie = resposta.getHeader("Set-Cookie");
        assertThat(cookie).contains("ivis_redefinicao=" + autorizacao, "HttpOnly", "Secure",
                "SameSite=Strict", "Path=/api/v1/senhas/redefinicao", "Max-Age=600");

        MockHttpServletResponse respostaFinal = new MockHttpServletResponse();
        controller.redefinir(new RedefinicaoRequisicao("NovaSenha123!"), autorizacao,
                pedido, respostaFinal);

        verify(servico).redefinir(autorizacao, "NovaSenha123!", pedido.getRemoteAddr());
        assertThat(respostaFinal.getHeader("Set-Cookie"))
                .contains("ivis_redefinicao=", "Max-Age=0", "HttpOnly");
    }
}
