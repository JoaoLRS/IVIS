package br.ivis.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("ivis.ia")
public record ConfiguracaoClienteIa(
        @NotNull URI url,
        @NotBlank @Size(min = 32) String chaveServico,
        @NotNull Duration timeoutPergunta,
        @NotNull Duration timeoutAvaliacao) {
}
