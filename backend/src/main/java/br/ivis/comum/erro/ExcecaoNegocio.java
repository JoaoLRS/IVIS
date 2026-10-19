package br.ivis.comum.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Erro previsto de regra de negócio ou de acesso. A mensagem é mostrada ao cliente,
 * portanto não deve conter dados sensíveis nem detalhes internos.
 */
public class ExcecaoNegocio extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final List<ErroCampo> campos;

    public ExcecaoNegocio(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, List.of());
    }

    public ExcecaoNegocio(HttpStatus status, String codigo, String mensagem, List<ErroCampo> campos) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.campos = List.copyOf(campos);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<ErroCampo> getCampos() {
        return campos;
    }
}
