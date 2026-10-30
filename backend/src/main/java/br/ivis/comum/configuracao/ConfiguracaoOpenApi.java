package br.ivis.comum.configuracao;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class ConfiguracaoOpenApi {

    private static final Set<String> OPERACOES_PUBLICAS = Set.of(
            "POST /v1/sessoes",
            "POST /v1/sessoes/renovacao",
            "POST /v1/contas/instituicoes",
            "POST /v1/contas/alunos",
            "POST /v1/senhas/recuperacao",
            "POST /v1/senhas/redefinicao",
            "GET /v1/sessoes/csrf",
            "GET /v1/planos",
            "GET /v1/planos/{id}",
            "GET /v1/saude",
            "GET /v1/pronto");

    @Bean
    OpenAPI especificacaoOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("IVIS Backend API")
                        .description("""
                                API do IVIS para identidade, sessões, perfil, recuperação de senha,
                                habilidades, vagas e operações institucionais. As operações protegidas
                                exigem o token de acesso Bearer emitido pelo endpoint de sessão.
                                """)
                        .version("v1"));
    }

    @Bean
    OpenApiCustomizer segurancaPorOperacao() {
        return openApi -> openApi.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, operation) -> {
                    String chave = method.name() + " " + path;
                    if (OPERACOES_PUBLICAS.contains(chave)) {
                        operation.setSecurity(List.of());
                    } else {
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                    }
                }));
    }
}
