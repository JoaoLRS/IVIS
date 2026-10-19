package br.ivis.comum.erro;

import br.ivis.comum.requisicao.FiltroIdentificadorRequisicao;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte erros de negócio, validação e MVC no formato {@link RespostaErro}. As mensagens
 * são fixas por situação: nunca repetem texto vindo da exceção, que pode conter dados do usuário.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ExcecaoNegocio.class)
    ResponseEntity<Object> tratarNegocio(ExcecaoNegocio excecao) {
        return resposta(excecao.getStatus(), excecao.getCodigo(), excecao.getMessage(), excecao.getCampos());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> tratarViolacao(ConstraintViolationException excecao) {
        List<ErroCampo> campos = excecao.getConstraintViolations().stream()
                .map(violacao -> new ErroCampo(violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "dados_invalidos", "Há dados inválidos na requisição.", campos);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        List<ErroCampo> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "dados_invalidos", "Há dados inválidos na requisição.", campos);
    }

    /** Ponto único por onde passam as demais exceções padrão do Spring MVC (404, 405, 415...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception excecao, Object corpo,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        return resposta(status, codigoPara(status), mensagemPara(status), List.of());
    }

    private static ResponseEntity<Object> resposta(HttpStatusCode status, String codigo, String mensagem,
            List<ErroCampo> campos) {
        RespostaErro corpo = new RespostaErro(codigo, mensagem, campos,
                MDC.get(FiltroIdentificadorRequisicao.CHAVE_MDC));
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }

    private static String codigoPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> "nao_autenticado";
            case 403 -> "acesso_negado";
            case 404 -> "nao_encontrado";
            case 405 -> "metodo_nao_permitido";
            case 406 -> "formato_nao_aceito";
            case 415 -> "tipo_midia_nao_suportado";
            case 429 -> "limite_excedido";
            case 503 -> "indisponivel";
            default -> status.is5xxServerError() ? "erro_interno" : "requisicao_invalida";
        };
    }

    private static String mensagemPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> "Autenticação necessária.";
            case 403 -> "Acesso negado.";
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método não permitido para este recurso.";
            case 406 -> "Formato de resposta não aceito.";
            case 415 -> "Tipo de conteúdo não suportado.";
            case 429 -> "Muitas requisições. Tente novamente mais tarde.";
            case 503 -> "Serviço temporariamente indisponível.";
            default -> status.is5xxServerError()
                    ? "Não foi possível concluir a operação. Tente novamente."
                    : "Requisição inválida.";
        };
    }
}
