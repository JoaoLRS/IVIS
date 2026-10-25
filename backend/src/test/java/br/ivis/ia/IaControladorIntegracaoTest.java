package br.ivis.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                    + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "spring.datasource.password=teste-local"
        })
@Import(IaControladorIntegracaoTest.ConfiguracaoTeste.class)
class IaControladorIntegracaoTest {

    private static final String CHAVE_SERVICO = "chave-servico-integracao-com-mais-de-32-caracteres";
    private static final String RESPOSTA_PERGUNTA =
            "{\"pergunta\":{\"texto\":\"Como você usa Vue?\"},"
                    + "\"versao_modelo\":\"stub-1\",\"versao_prompt\":\"perguntas-1\"}";
    private static final String RESPOSTA_AVALIACAO =
            "{\"nota\":78,\"pontos_fortes\":[\"Exemplos claros.\"],"
                    + "\"pontos_melhoria\":[\"Detalhe os resultados.\"],"
                    + "\"versao_modelo\":\"stub-1\",\"versao_prompt\":\"avaliacao-1\"}";
    private static final List<String> caminhos = new CopyOnWriteArrayList<>();
    private static final List<String> chaves = new CopyOnWriteArrayList<>();
    private static volatile String corpoServicoIa = RESPOSTA_PERGUNTA;
    private static final HttpServer servidorIa = criarServidorIa();

    @LocalServerPort
    private int porta;

    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void configurarIa(DynamicPropertyRegistry propriedades) {
        propriedades.add("ivis.ia.url", () -> "http://127.0.0.1:" + servidorIa.getAddress().getPort());
        propriedades.add("ivis.ia.chave-servico", () -> CHAVE_SERVICO);
    }

    @BeforeEach
    void limparChamadasAnteriores() {
        caminhos.clear();
        chaves.clear();
        corpoServicoIa = RESPOSTA_PERGUNTA;
    }

    @AfterAll
    static void pararServidorIa() {
        servidorIa.stop(0);
    }

    @Test
    void backendEncaminhaGeracaoDePerguntaAoServicoInterno() throws Exception {
        HttpResponse<String> resposta = postar(
                "/api/v1/ia/perguntas/gerar",
                """
                {
                  "tipo_contexto": "HABILIDADES",
                  "habilidades": [{"nome": "Vue", "nivel": 4}],
                  "vaga": null,
                  "interacoes": [],
                  "numero_pergunta": 1,
                  "total_perguntas": 3
                }
                """);

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"texto\":\"Como você usa Vue?\""));
        assertEquals(List.of("/v1/perguntas/gerar"), caminhos);
        assertEquals(List.of(CHAVE_SERVICO), chaves);
    }

    @Test
    void backendEncaminhaAvaliacaoEInteracoesAoServicoInterno() throws Exception {
        corpoServicoIa = RESPOSTA_AVALIACAO;
        HttpResponse<String> resposta = postar(
                "/api/v1/ia/avaliacoes/gerar",
                """
                {
                  "tipo_contexto": "HABILIDADES",
                  "habilidades": [{"nome": "Vue", "nivel": 4}],
                  "vaga": null,
                  "interacoes": [
                    {"numero": 1, "pergunta": "Como usa Vue?", "resposta": "Tenho experiência."}
                  ]
                }
                """);

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"nota\":78"));
        assertEquals(List.of("/v1/avaliacoes/gerar"), caminhos);
        assertEquals(List.of(CHAVE_SERVICO), chaves);
    }

    private HttpResponse<String> postar(String caminho, String corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer criarServidorIa() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/", IaControladorIntegracaoTest::responderServicoIa);
            servidor.start();
            return servidor;
        } catch (IOException excecao) {
            throw new ExceptionInInitializerError(excecao);
        }
    }

    private static void responderServicoIa(HttpExchange requisicao) throws IOException {
        caminhos.add(requisicao.getRequestURI().getPath());
        chaves.add(requisicao.getRequestHeaders().getFirst("X-Chave-Servico"));
        byte[] corpo = corpoServicoIa.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json");
        requisicao.sendResponseHeaders(200, corpo.length);
        requisicao.getResponseBody().write(corpo);
        requisicao.close();
    }

    @TestConfiguration
    static class ConfiguracaoTeste {

        @Bean
        DataSource fonteDeDados() {
            return mock(DataSource.class);
        }
    }
}
