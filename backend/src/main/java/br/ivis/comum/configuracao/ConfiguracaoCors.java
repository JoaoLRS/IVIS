package br.ivis.comum.configuracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS restrito às origens do frontend (variável {@code IVIS_CORS_ORIGENS}, separadas por vírgula).
 * Credenciais ficam habilitadas porque o ADR-0011 propõe o token de renovação em cookie HttpOnly;
 * por isso nunca se usa origem curinga.
 *
 * <p>Na Etapa 2, ao incluir o Spring Security, esta configuração deve migrar para o
 * {@code SecurityFilterChain} (o CORS precisa ser tratado antes da autenticação).
 */
@Configuration
public class ConfiguracaoCors implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public ConfiguracaoCors(@Value("${ivis.cors.origens-permitidas}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Id-Requisicao")
                .exposedHeaders("X-Id-Requisicao")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
