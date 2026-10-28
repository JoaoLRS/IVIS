package br.ivis.comum.configuracao;

import br.ivis.comum.erro.RespostaErro;
import br.ivis.comum.requisicao.FiltroIdentificadorRequisicao;
import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain filtroSeguranca(
            HttpSecurity http,
            ObjectMapper mapeadorJson,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource configuracaoCors,
            CookieCsrfTokenRepository repositorioCsrf,
            JwtAuthenticationConverter conversorJwt) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (requisicao, resposta, excecao) ->
                escreverErro(mapeadorJson, resposta, HttpStatus.UNAUTHORIZED,
                        "nao_autenticado", "Autenticação necessária.");
        AccessDeniedHandler acessoNegado = (requisicao, resposta, excecao) ->
                escreverErro(mapeadorJson, resposta, HttpStatus.FORBIDDEN,
                        "acesso_negado", "Acesso negado.");

        http
                .cors(cors -> cors.configurationSource(configuracaoCors))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repositorioCsrf)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/v1/ia/perguntas/gerar",
                                "/v1/ia/avaliacoes/gerar"))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/saude", "/v1/pronto").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/sessoes/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/planos", "/v1/planos/*").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/v1/contas/alunos",
                                "/v1/contas/instituicoes",
                                "/v1/sessoes",
                                "/v1/sessoes/renovacao",
                                "/v1/senhas/recuperacao",
                                "/v1/senhas/redefinicao")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt)));

        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository repositorioCsrf(
            @Value("${ivis.sessao.cookie-seguro:true}") boolean cookieSeguro) {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookieCustomizer(cookie -> cookie.path("/api").secure(cookieSeguro).sameSite("Strict"));
        return repositorio;
    }

    @Bean
    JwtAuthenticationConverter conversorJwt() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String tipoConta = jwt.getClaimAsString("tipo_conta");
            if ("ALUNO".equals(tipoConta) || "INSTITUICAO".equals(tipoConta)) {
                return List.of(new SimpleGrantedAuthority("ROLE_" + tipoConta));
            }
            return List.of();
        });
        return conversor;
    }

    private static void escreverErro(
            ObjectMapper mapeadorJson,
            jakarta.servlet.http.HttpServletResponse resposta,
            HttpStatus status,
            String codigo,
            String mensagem) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapeadorJson.writeValue(resposta.getOutputStream(), new RespostaErro(
                codigo,
                mensagem,
                List.of(),
                MDC.get(FiltroIdentificadorRequisicao.CHAVE_MDC)));
    }
}
