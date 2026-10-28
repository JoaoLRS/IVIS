package br.ivis.comum.configuracao;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS restrito às origens do frontend (variável {@code IVIS_CORS_ORIGENS}, separadas por vírgula).
 * Credenciais ficam habilitadas porque o ADR-0011 usa o token de renovação em cookie HttpOnly;
 * por isso nunca se usa origem curinga.
 *
 * <p>O CORS é conectado ao filtro de segurança para processar preflight antes da autorização.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoCors {

    private final String[] origensPermitidas;

    public ConfiguracaoCors(@Value("${ivis.cors.origens-permitidas}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas.clone();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of(origensPermitidas));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Idempotency-Key",
                        "X-Id-Requisicao", "X-XSRF-TOKEN"));
        configuracao.setExposedHeaders(List.of("X-Id-Requisicao"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);
        return origem;
    }
}
