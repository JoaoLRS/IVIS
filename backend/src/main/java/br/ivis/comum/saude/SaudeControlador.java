package br.ivis.comum.saude;

import br.ivis.comum.erro.ExcecaoNegocio;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness ({@code /saude}) e readiness ({@code /pronto}), sem detalhes internos. */
@RestController
@RequestMapping("/v1")
public class SaudeControlador {

    private final DataSource fonteDeDados;

    public SaudeControlador(DataSource fonteDeDados) {
        this.fonteDeDados = fonteDeDados;
    }

    @GetMapping("/saude")
    public Map<String, String> saude() {
        return Map.of("situacao", "ATIVO");
    }

    @GetMapping("/pronto")
    public Map<String, String> pronto() {
        try (Connection conexao = fonteDeDados.getConnection()) {
            if (conexao.isValid(2)) {
                return Map.of("situacao", "PRONTO");
            }
        } catch (SQLException excecao) {
            // detalhes ficam fora da resposta
        }
        throw new ExcecaoNegocio(HttpStatus.SERVICE_UNAVAILABLE, "indisponivel",
                "Serviço temporariamente indisponível.");
    }
}
