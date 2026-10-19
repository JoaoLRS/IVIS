package br.ivis.comum.erro;

import br.ivis.comum.requisicao.FiltroIdentificadorRequisicao;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TratadorFalhaInesperada {

    /** Fallback executado após os tratadores específicos do Spring MVC e da aplicação. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<RespostaErro> tratar(Exception excecao) {
        RespostaErro corpo = new RespostaErro(
                "erro_interno",
                "Não foi possível concluir a operação. Tente novamente.",
                List.of(),
                MDC.get(FiltroIdentificadorRequisicao.CHAVE_MDC));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo);
    }
}
