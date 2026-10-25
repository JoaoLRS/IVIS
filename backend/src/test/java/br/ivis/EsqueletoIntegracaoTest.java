package br.ivis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Sobe a aplicação inteira contra um PostgreSQL vazio (Testcontainers); exige Docker. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EsqueletoIntegracaoTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", postgres::getJdbcUrl);
        registro.add("spring.datasource.username", postgres::getUsername);
        registro.add("spring.datasource.password", postgres::getPassword);
        registro.add("ivis.ia.chave-servico", () -> "chave-interna-de-integracao-com-mais-de-32-caracteres");
    }

    @Value("${local.server.port}")
    int porta;

    @Autowired
    DataSource fonteDeDados;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> obter(String caminho, String... cabecalhos) throws Exception {
        HttpRequest.Builder construtor = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api" + caminho));
        for (int i = 0; i < cabecalhos.length; i += 2) {
            construtor.header(cabecalhos[i], cabecalhos[i + 1]);
        }
        return cliente.send(construtor.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void migracoesCriamBancoVazioIntegralmente() throws Exception {
        try (Connection conexao = fonteDeDados.getConnection();
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(
                        "SELECT count(*) FROM flyway_schema_history WHERE success")) {
            resultado.next();
            assertTrue(resultado.getInt(1) >= 1);
        }
    }

    @Test
    void saudeRespondeComIdentificadorDeRequisicao() throws Exception {
        HttpResponse<String> resposta = obter("/v1/saude");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("ATIVO"));
        assertTrue(resposta.headers().firstValue("X-Id-Requisicao").isPresent());
    }

    @Test
    void prontoVerificaOBanco() throws Exception {
        HttpResponse<String> resposta = obter("/v1/pronto");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("PRONTO"));
    }

    @Test
    void erroUsaFormatoUniformeEPreservaIdValido() throws Exception {
        HttpResponse<String> resposta = obter("/v1/nao-existe", "X-Id-Requisicao", "abc-12345678");
        assertEquals(404, resposta.statusCode());
        assertTrue(resposta.body().contains("\"codigo\":\"nao_encontrado\""));
        assertTrue(resposta.body().contains("\"campos\":[]"));
        assertTrue(resposta.body().contains("\"idRequisicao\":\"abc-12345678\""));
        assertEquals("abc-12345678", resposta.headers().firstValue("X-Id-Requisicao").orElse(""));
    }

    @Test
    void idInvalidoEhSubstituidoPorUmGerado() throws Exception {
        HttpResponse<String> resposta = obter("/v1/saude", "X-Id-Requisicao", "curto");
        String id = resposta.headers().firstValue("X-Id-Requisicao").orElse("");
        assertNotEquals("curto", id);
        assertEquals(36, id.length());
    }

    @Test
    void corsSoLiberaOrigensConfiguradas() throws Exception {
        HttpResponse<String> permitida = obter("/v1/saude", "Origin", "http://localhost:8080");
        assertEquals("http://localhost:8080",
                permitida.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

        HttpResponse<String> negada = obter("/v1/saude", "Origin", "http://origem-nao-configurada.example");
        assertFalse(negada.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }
}
