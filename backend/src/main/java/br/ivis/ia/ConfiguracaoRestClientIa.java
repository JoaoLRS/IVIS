package br.ivis.ia;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ConfiguracaoClienteIa.class)
public class ConfiguracaoRestClientIa {

    @Bean
    RestClient clienteIaPerguntas(ConfiguracaoClienteIa configuracao) {
        return criarCliente(configuracao.url(), configuracao.timeoutPergunta());
    }

    @Bean
    RestClient clienteIaAvaliacoes(ConfiguracaoClienteIa configuracao) {
        return criarCliente(configuracao.url(), configuracao.timeoutAvaliacao());
    }

    private static RestClient criarCliente(URI url, Duration timeout) {
        HttpClient clienteJdk = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteJdk);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(url.toString())
                .requestFactory(fabrica)
                .build();
    }
}
