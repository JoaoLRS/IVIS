package br.ivis.common.development;

import br.ivis.common.error.ExcecaoNegocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ambiente/limpar-dados")
@Profile({"dev & !prod", "homolog & !prod", "homologacao & !prod"})
public class LimpezaDadosController {
    private final LimpezaDadosService servico;
    private final String chave;

    public LimpezaDadosController(LimpezaDadosService servico,
            @Value("${IVIS_LIMPEZA_CHAVE:}") String chave) {
        this.servico = servico;
        this.chave = chave;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limpar(@RequestHeader(value = "X-IVIS-Limpeza-Chave", required = false) String chaveInformada,
            @Valid @RequestBody DocumentoEntrada entrada) {
        if (chave.length() < 32) {
            throw new ExcecaoNegocio(HttpStatus.SERVICE_UNAVAILABLE, "limpeza_indisponivel",
                    "A limpeza de dados não está configurada neste ambiente.");
        }
        if (chaveInformada == null || !MessageDigest.isEqual(
                chave.getBytes(StandardCharsets.UTF_8), chaveInformada.getBytes(StandardCharsets.UTF_8))) {
            throw new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado", "Acesso negado.");
        }
        servico.limpar(entrada.documento());
    }

    public record DocumentoEntrada(@NotBlank String documento) { }
}
