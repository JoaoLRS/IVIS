package br.ivis.ia;

import static br.ivis.ia.ContratoIa.GerarAvaliacaoEntrada;
import static br.ivis.ia.ContratoIa.GerarPerguntaEntrada;
import static br.ivis.ia.ContratoIa.Habilidade;
import static br.ivis.ia.ContratoIa.Interacao;
import static br.ivis.ia.ContratoIa.TipoContexto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.ivis.comum.erro.ExcecaoNegocio;
import org.springframework.web.client.RestClient;

class ClienteIaTest {

    private static final String CHAVE_SERVICO = "chave-servico-teste-com-mais-de-32-caracteres";
    private static final String RESPOSTA_PERGUNTA =
            "{\"pergunta\":{\"texto\":\"Como você usa Vue?\"},"
                    + "\"versao_modelo\":\"stub-1\",\"versao_prompt\":\"perguntas-1\"}";
    private static final String RESPOSTA_AVALIACAO =
            "{\"nota\":78,\"pontos_fortes\":[\"Exemplos claros.\"],"
                    + "\"pontos_melhoria\":[\"Detalhe os resultados.\"],"
                    + "\"versao_modelo\":\"stub-1\",\"versao_prompt\":\"avaliacao-1\"}";

    private final List<String> caminhos = new CopyOnWriteArrayList<>();
    private final List<String> corpos = new CopyOnWriteArrayList<>();
    private final List<String> chavesServico = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private volatile int status = 200;
    private volatile String resposta = RESPOSTA_PERGUNTA;
    private ClienteIa clienteIa;
    private ValidatorFactory fabricaValidador;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::responder);
        servidor.start();

        String baseUrl = "http://127.0.0.1:" + servidor.getAddress().getPort();
        RestClient clientePerguntas = RestClient.builder().baseUrl(baseUrl).build();
        RestClient clienteAvaliacoes = RestClient.builder().baseUrl(baseUrl).build();
        ConfiguracaoClienteIa configuracao = new ConfiguracaoClienteIa(
                java.net.URI.create(baseUrl), CHAVE_SERVICO, Duration.ofSeconds(12), Duration.ofSeconds(22));
        fabricaValidador = Validation.buildDefaultValidatorFactory();
        Validator validador = fabricaValidador.getValidator();
        clienteIa = new ClienteIa(clientePerguntas, clienteAvaliacoes, configuracao, validador);
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
        if (fabricaValidador != null) {
            fabricaValidador.close();
        }
    }

    @Test
    void chamaOsDoisEndpointsComAutenticacaoEValidaAsRespostas() {
        var pergunta = clienteIa.gerarPergunta(entradaPergunta());
        status = 200;
        resposta = RESPOSTA_AVALIACAO;
        var avaliacao = clienteIa.gerarAvaliacao(entradaAvaliacao());

        assertEquals("Como você usa Vue?", pergunta.pergunta().texto());
        assertEquals(78, avaliacao.nota());
        assertEquals(List.of("Exemplos claros."), avaliacao.pontosFortes());
        assertEquals(List.of("Detalhe os resultados."), avaliacao.pontosMelhoria());
        assertEquals(
                List.of("/v1/perguntas/gerar", "/v1/avaliacoes/gerar"),
                caminhos);
        assertEquals(List.of(CHAVE_SERVICO, CHAVE_SERVICO), chavesServico);
        assertTrue(corpos.get(0).contains("\"id_correlacao\""));
        assertTrue(corpos.get(0).contains("\"numero_pergunta\":1"));
        assertTrue(corpos.get(1).contains("\"resposta\":\"Tenho experiência.\""));
    }

    @Test
    void rejeitaRespostaQueNaoAtendeAoContrato() {
        resposta = "{\"pergunta\":{\"texto\":\"\"},"
                + "\"versao_modelo\":\"stub-1\",\"versao_prompt\":\"perguntas-1\"}";

        ExcecaoNegocio excecao = assertThrows(
                ExcecaoNegocio.class, () -> clienteIa.gerarPergunta(entradaPergunta()));

        assertEquals("saida_invalida", excecao.getCodigo());
        assertEquals(502, excecao.getStatus().value());
    }

    @Test
    void traduzIndisponibilidadeSemRepassarDetalhesDoServico() {
        status = 503;
        resposta = "{\"codigo\":\"provedor_indisponivel\",\"mensagem\":\"detalhe interno\"}";

        ExcecaoNegocio excecao = assertThrows(
                ExcecaoNegocio.class, () -> clienteIa.gerarPergunta(entradaPergunta()));

        assertEquals("provedor_indisponivel", excecao.getCodigo());
        assertEquals(503, excecao.getStatus().value());
        assertFalse(excecao.getMessage().contains("detalhe interno"));
    }

    private GerarPerguntaEntrada entradaPergunta() {
        return new GerarPerguntaEntrada(
                TipoContexto.HABILIDADES,
                List.of(new Habilidade("Vue", 4)),
                null,
                List.of(),
                1,
                3);
    }

    private GerarAvaliacaoEntrada entradaAvaliacao() {
        return new GerarAvaliacaoEntrada(
                TipoContexto.HABILIDADES,
                List.of(new Habilidade("Vue", 4)),
                null,
                List.of(new Interacao(1, "Como usa Vue?", "Tenho experiência.")));
    }

    private void responder(HttpExchange requisicao) throws IOException {
        caminhos.add(requisicao.getRequestURI().getPath());
        corpos.add(new String(requisicao.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        chavesServico.add(requisicao.getRequestHeaders().getFirst("X-Chave-Servico"));
        byte[] corpo = resposta.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json");
        requisicao.sendResponseHeaders(status, corpo.length);
        requisicao.getResponseBody().write(corpo);
        requisicao.close();
    }
}
