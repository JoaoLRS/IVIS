package br.ivis.comum.erro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.ivis.comum.requisicao.FiltroIdentificadorRequisicao;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

class TratadorFalhaInesperadaTest {

    @Test
    void respostaEhGenericaEPreservaIdentificadorSemExporDetalhesDaExcecao() {
        String idRequisicao = "abc-12345678";
        MDC.put(FiltroIdentificadorRequisicao.CHAVE_MDC, idRequisicao);
        try {
            var resposta = new TratadorFalhaInesperada()
                    .tratar(new IllegalStateException("detalhe interno sensivel"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
            assertEquals("erro_interno", resposta.getBody().codigo());
            assertEquals(idRequisicao, resposta.getBody().idRequisicao());
            assertFalse(resposta.getBody().mensagem().contains("detalhe interno sensivel"));
        } finally {
            MDC.remove(FiltroIdentificadorRequisicao.CHAVE_MDC);
        }
    }
}
