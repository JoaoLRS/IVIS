package br.ivis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia de segurança exclusiva da porta de gerenciamento (produção). Libera apenas a
 * saúde e as métricas Prometheus e nega todo o resto. A porta 9091 não passa pelo Caddy
 * e, na AWS, o grupo de segurança só a abre para a EC2 de monitoramento.
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class SegurancaMetricasConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain filtroSegurancaMetricas(
            HttpSecurity http,
            @Value("${management.server.port:-1}") int portaGerenciamento) throws Exception {
        http
                .securityMatcher(requisicao -> requisicao.getLocalPort() == portaGerenciamento)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/prometheus").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
