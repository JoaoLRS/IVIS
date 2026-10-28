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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import br.ivis.identidade.repositorio.ContaRepositorio;
import br.ivis.identidade.repositorio.ContratoInstitucionalRepositorio;
import br.ivis.identidade.repositorio.HabilidadePerfilRepositorio;
import br.ivis.identidade.repositorio.PerfilAlunoRepositorio;
import br.ivis.identidade.repositorio.PerfilInstituicaoRepositorio;
import br.ivis.identidade.repositorio.RequisitoVagaRepositorio;
import br.ivis.identidade.repositorio.TokenRecuperacaoSenhaRepositorio;
import br.ivis.identidade.repositorio.VagaInteresseRepositorio;
import br.ivis.identidade.repositorio.VinculoInstitucionalRepositorio;
import br.ivis.planos.repositorio.ContratoPlanoConsultaRepositorio;
import br.ivis.planos.repositorio.PlanoRepositorio;

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

    @org.springframework.beans.factory.annotation.Autowired
    private JwtEncoder codificadorJwt;

    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void configurarIa(DynamicPropertyRegistry propriedades) {
        propriedades.add("ivis.ia.url", () -> "http://127.0.0.1:" + servidorIa.getAddress().getPort());
        propriedades.add("ivis.ia.chave-servico", () -> CHAVE_SERVICO);
        propriedades.add("ivis.jwt.chave-assinatura",
                () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
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

    @Test
    void rotaPrivadaAindaNaoConfiguradaEhNegadaComErroUniforme() throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/conta"))
                .header("X-Id-Requisicao", "abc-12345678")
                .GET()
                .build();
        HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("\"codigo\":\"nao_autenticado\""));
        assertTrue(resposta.body().contains("\"idRequisicao\":\"abc-12345678\""));
    }

    @Test
    void endpointCsrfEntregaTokenEGravaCookie() throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/sessoes/csrf"))
                .GET()
                .build();
        HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"token\":"));
        assertTrue(resposta.headers().allValues("Set-Cookie").stream()
                .anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN=")));
    }

    @Test
    void corsAceitaPreflightComPatchECredenciais() throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/habilidades"))
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resposta = clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resposta.statusCode());
        assertEquals("http://localhost:8080",
                resposta.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("true", resposta.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
        assertTrue(resposta.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("PATCH"));
    }

    private HttpResponse<String> postar(String caminho, String corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenAluno())
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private String tokenAluno() {
        var agora = java.time.Instant.now();
        return codificadorJwt.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("ivis")
                        .subject("123e4567-e89b-12d3-a456-426614174000")
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(900))
                        .claim("tipo_conta", "ALUNO")
                        .build()))
                .getTokenValue();
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
        br.ivis.planos.repositorio.PlanoRepositorio planos() {
            return mock(br.ivis.planos.repositorio.PlanoRepositorio.class);
        }

        @Bean
        br.ivis.planos.repositorio.ContratoPlanoConsultaRepositorio contratosPlanos() {
            return mock(br.ivis.planos.repositorio.ContratoPlanoConsultaRepositorio.class);
        }

        @Bean
        DataSource fonteDeDados() {
            return mock(DataSource.class);
        }

        @Bean
        br.ivis.identidade.repositorio.ContaRepositorio contas() {
            return mock(br.ivis.identidade.repositorio.ContaRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.PerfilAlunoRepositorio perfisAlunos() {
            return mock(br.ivis.identidade.repositorio.PerfilAlunoRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.PerfilInstituicaoRepositorio perfisInstituicoes() {
            return mock(br.ivis.identidade.repositorio.PerfilInstituicaoRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.SessaoRepositorio sessoes() {
            return mock(br.ivis.identidade.repositorio.SessaoRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.TokenRecuperacaoSenhaRepositorio tokensRecuperacaoSenha() {
            return mock(br.ivis.identidade.repositorio.TokenRecuperacaoSenhaRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.HabilidadePerfilRepositorio habilidadesPerfil() {
            return mock(br.ivis.identidade.repositorio.HabilidadePerfilRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.VagaInteresseRepositorio vagasInteresse() {
            return mock(br.ivis.identidade.repositorio.VagaInteresseRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.RequisitoVagaRepositorio requisitosVaga() {
            return mock(br.ivis.identidade.repositorio.RequisitoVagaRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.ContratoInstitucionalRepositorio contratosInstitucionais() {
            return mock(br.ivis.identidade.repositorio.ContratoInstitucionalRepositorio.class);
        }

        @Bean
        br.ivis.identidade.repositorio.VinculoInstitucionalRepositorio vinculosInstitucionais() {
            return mock(br.ivis.identidade.repositorio.VinculoInstitucionalRepositorio.class);
        }
    }
}
