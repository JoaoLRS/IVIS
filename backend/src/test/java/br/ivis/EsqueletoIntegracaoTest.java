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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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

import br.ivis.identidade.modelo.Conta;
import br.ivis.planos.modelo.Plano;

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
        registro.add("ivis.jwt.chave-assinatura",
                () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
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
    void cadastrarAlunoPersisteContaPerfilEHashDaSenha() throws Exception {
        String corpo = """
                {
                  "nome": "Ana Aluna",
                  "email": "Ana@Exemplo.com",
                  "senha": "uma-senha-forte-123"
                }
                """;
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        HttpResponse<String> resposta = postarComCsrf("/v1/contas/alunos", corpo, csrf, null);
        assertEquals(201, resposta.statusCode());
        assertTrue(resposta.body().contains("\"tipoConta\":\"ALUNO\""));
        assertTrue(resposta.body().contains("\"email\":\"Ana@Exemplo.com\""));
        try (Connection conexao = fonteDeDados.getConnection();
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(
                        "SELECT c.email_normalizado, c.hash_senha, p.nome_exibicao "
                                + "FROM contas c JOIN perfis_aluno p ON p.conta_id = c.id "
                                + "WHERE c.email_normalizado = 'ana@exemplo.com'")) {
            assertTrue(resultado.next());
            assertEquals("ana@exemplo.com", resultado.getString("email_normalizado"));
            assertNotEquals("uma-senha-forte-123", resultado.getString("hash_senha"));
            assertEquals("Ana Aluna", resultado.getString("nome_exibicao"));
        }
    }

    @Test
    void cadastroInstitucionalCriaContaComPerfilCorreto() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        HttpResponse<String> resposta = postarComCsrf(
                "/v1/contas/instituicoes",
                """
                {
                  "razaoSocial": "Instituto de Teste",
                  "email": "contato@instituto-teste.example",
                  "senha": "outra-senha-forte-456"
                }
                """,
                csrf,
                null);

        assertEquals(201, resposta.statusCode());
        assertTrue(resposta.body().contains("\"tipoConta\":\"INSTITUICAO\""));
        try (Connection conexao = fonteDeDados.getConnection();
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(
                        "SELECT c.tipo_conta, p.razao_social FROM contas c "
                                + "JOIN perfis_instituicao p ON p.conta_id = c.id "
                                + "WHERE c.email_normalizado = 'contato@instituto-teste.example'")) {
            assertTrue(resultado.next());
            assertEquals("INSTITUICAO", resultado.getString("tipo_conta"));
            assertEquals("Instituto de Teste", resultado.getString("razao_social"));
        }
    }

    @Test
    void loginNaoRevelaSeEmailExiste() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String emailExistente = "login-" + UUID.randomUUID() + "@exemplo.test";
        assertEquals(201, postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Login Teste","email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(emailExistente),
                csrf,
                null).statusCode());

        HttpResponse<String> senhaIncorreta = postarComCsrf(
                "/v1/sessoes",
                """
                {"email":"%s","senha":"senha-errada-123"}
                """.formatted(emailExistente),
                csrf,
                null);
        HttpResponse<String> emailInexistente = postarComCsrf(
                "/v1/sessoes",
                """
                {"email":"inexistente-%s@exemplo.test","senha":"senha-errada-123"}
                """.formatted(UUID.randomUUID()),
                csrf,
                null);

        assertEquals(401, senhaIncorreta.statusCode());
        assertEquals(401, emailInexistente.statusCode());
        assertEquals(json().readTree(senhaIncorreta.body()).get("codigo").asText(),
                json().readTree(emailInexistente.body()).get("codigo").asText());
        assertEquals(json().readTree(senhaIncorreta.body()).get("mensagem").asText(),
                json().readTree(emailInexistente.body()).get("mensagem").asText());
    }

    @Test
    void loginRenovaTokenUmaUnicaVezELogoutRevogaSessao() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String email = "sessao-" + UUID.randomUUID() + "@exemplo.test";
        HttpResponse<String> cadastro = postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Sessao Teste","email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null);
        assertEquals(201, cadastro.statusCode());

        HttpResponse<String> login = postarComCsrf(
                "/v1/sessoes",
                """
                {"email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null);
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"tipoToken\":\"Bearer\""));
        String accessToken = json().readTree(login.body()).get("accessToken").asText();
        String tokenAntigo = valorCookie(login, "ivis_refresh");

        HttpResponse<String> renovacao = postarComCsrf(
                "/v1/sessoes/renovacao", "{}", csrf, tokenAntigo);
        assertEquals(200, renovacao.statusCode());
        String accessTokenNovo = json().readTree(renovacao.body()).get("accessToken").asText();
        String tokenNovo = valorCookie(renovacao, "ivis_refresh");
        assertNotEquals(tokenAntigo, tokenNovo);

        assertEquals(401, postarComCsrf("/v1/sessoes/renovacao", "{}", csrf, tokenAntigo).statusCode());
        HttpRequest sair = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/sessoes/atual"))
                .header("Cookie", csrf.cookie() + "; ivis_refresh=" + tokenNovo)
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Authorization", "Bearer " + accessTokenNovo)
                .DELETE()
                .build();
        HttpResponse<String> respostaSair = cliente.send(sair, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, respostaSair.statusCode());
        assertNotEquals(accessToken, accessTokenNovo);
        assertEquals(401, postarComCsrf("/v1/sessoes/renovacao", "{}", csrf, tokenNovo).statusCode());
    }

    @Test
    void crudPerfilAlunoIsolaTitularEArquivaVagaSemAlterarSnapshotDeTreino() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String emailAluno = "perfil-" + UUID.randomUUID() + "@exemplo.test";
        String emailOutroAluno = "outro-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(emailAluno, csrf);
        cadastrarAluno(emailOutroAluno, csrf);
        String tokenAluno = autenticar(emailAluno, csrf);
        String tokenOutroAluno = autenticar(emailOutroAluno, csrf);

        HttpResponse<String> habilidadeCriada = postarAutenticado(
                "/v1/habilidades",
                """
                {"nome":" Vue 3 ","categoria":"Front-end","nivel":4}
                """,
                csrf,
                tokenAluno);
        assertEquals(201, habilidadeCriada.statusCode());
        String habilidadeId = json().readTree(habilidadeCriada.body()).get("id").asText();
        assertEquals(409, postarAutenticado(
                "/v1/habilidades",
                """
                {"nome":"vue 3","categoria":"Front-end","nivel":3}
                """,
                csrf,
                tokenAluno).statusCode());
        assertEquals(404, deletarAutenticado("/v1/habilidades/" + habilidadeId, csrf, tokenOutroAluno)
                .statusCode());
        assertEquals(1, json().readTree(obterAutenticado("/v1/habilidades", tokenAluno).body()).size());
        assertEquals(0, json().readTree(obterAutenticado("/v1/habilidades", tokenOutroAluno).body()).size());

        HttpResponse<String> vagaCriada = postarAutenticado(
                "/v1/vagas",
                """
                {
                  "titulo":"Desenvolvedor Java",
                  "empresa":"Empresa Exemplo",
                  "senioridade":"Pleno",
                  "descricao":"Criar serviços backend",
                  "requisitos":["Java","PostgreSQL"]
                }
                """,
                csrf,
                tokenAluno);
        assertEquals(201, vagaCriada.statusCode());
        String vagaId = json().readTree(vagaCriada.body()).get("id").asText();
        assertEquals("Java", json().readTree(vagaCriada.body()).get("requisitos").get(0).asText());

        String snapshot = """
                {"titulo":"Desenvolvedor Java","requisitos":["Java","PostgreSQL"]}
                """;
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "INSERT INTO treinamentos "
                                + "(id, conta_aluno_id, vaga_interesse_id, tipo_contexto, contexto_registrado) "
                                + "VALUES (?, (SELECT id FROM contas WHERE email_normalizado = ?), ?, 'VAGA', ?::jsonb)")) {
            comando.setObject(1, UUID.randomUUID());
            comando.setString(2, emailAluno.toLowerCase(java.util.Locale.ROOT));
            comando.setObject(3, UUID.fromString(vagaId));
            comando.setString(4, snapshot);
            assertEquals(1, comando.executeUpdate());
        }

        HttpResponse<String> atualizada = cliente.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1/vagas/" + vagaId))
                        .header("Content-Type", "application/json")
                        .header("Cookie", csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .header("Authorization", "Bearer " + tokenAluno)
                        .PUT(HttpRequest.BodyPublishers.ofString("""
                                {
                                  "titulo":"Desenvolvedor Java Sênior",
                                  "empresa":"Empresa Exemplo",
                                  "senioridade":"Sênior",
                                  "descricao":"Liderar serviços backend",
                                  "requisitos":["Java","Arquitetura"]
                                }
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, atualizada.statusCode());
        assertEquals(204, deletarAutenticado("/v1/vagas/" + vagaId, csrf, tokenAluno).statusCode());
        assertEquals(0, json().readTree(obterAutenticado("/v1/vagas", tokenAluno).body()).size());

        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT v.arquivado_em, t.vaga_interesse_id, t.contexto_registrado->>'titulo' AS titulo "
                                + "FROM vagas_interesse v JOIN treinamentos t "
                                + "ON t.vaga_interesse_id = v.id WHERE v.id = ?")) {
            comando.setObject(1, UUID.fromString(vagaId));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertTrue(resultado.getTimestamp("arquivado_em") != null);
                assertEquals(UUID.fromString(vagaId), resultado.getObject("vaga_interesse_id", UUID.class));
                assertEquals("Desenvolvedor Java", resultado.getString("titulo"));
            }
        }
    }

    @Test
    void vinculosInstitucionaisIsolamInstituicoesContamCapacidadeESerializamConcorrencia() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String sufixo = UUID.randomUUID().toString();
        String emailAlunoA = "vinculo-a-" + sufixo + "@exemplo.test";
        String emailAlunoB = "vinculo-b-" + sufixo + "@exemplo.test";
        String emailInstituicaoA = "instituicao-a-" + sufixo + "@exemplo.test";
        String emailInstituicaoB = "instituicao-b-" + sufixo + "@exemplo.test";
        cadastrarAluno(emailAlunoA, csrf);
        cadastrarAluno(emailAlunoB, csrf);
        cadastrarInstituicao(emailInstituicaoA, csrf);
        cadastrarInstituicao(emailInstituicaoB, csrf);
        String tokenInstituicaoA = autenticar(emailInstituicaoA, csrf);
        String tokenInstituicaoB = autenticar(emailInstituicaoB, csrf);
        UUID contratoA = criarContratoInstitucional(emailInstituicaoA, 1, "contrato-a-" + sufixo);
        criarContratoInstitucional(emailInstituicaoB, 1, "contrato-b-" + sufixo);

        CountDownLatch inicio = new CountDownLatch(1);
        CompletableFuture<HttpResponse<String>> primeira = CompletableFuture.supplyAsync(() -> {
            aguardar(inicio);
            return postarAutenticadoSemChecked(
                    "/v1/instituicao/vinculos",
                    """
                    {"contratoId":"%s","contaAlunoId":"%s"}
                    """.formatted(contratoA, idConta(emailAlunoA)),
                    csrf,
                    tokenInstituicaoA);
        });
        CompletableFuture<HttpResponse<String>> segunda = CompletableFuture.supplyAsync(() -> {
            aguardar(inicio);
            return postarAutenticadoSemChecked(
                    "/v1/instituicao/vinculos",
                    """
                    {"contratoId":"%s","contaAlunoId":"%s"}
                    """.formatted(contratoA, idConta(emailAlunoB)),
                    csrf,
                    tokenInstituicaoA);
        });
        inicio.countDown();
        HttpResponse<String> respostaPrimeira = primeira.get();
        HttpResponse<String> respostaSegunda = segunda.get();

        assertEquals(1, List.of(respostaPrimeira.statusCode(), respostaSegunda.statusCode()).stream()
                .filter(status -> status == 201)
                .count());
        assertEquals(1, List.of(respostaPrimeira.statusCode(), respostaSegunda.statusCode()).stream()
                .filter(status -> status == 409)
                .count());

        HttpResponse<String> listaInstituicaoA =
                obterAutenticado("/v1/instituicao/alunos", tokenInstituicaoA);
        HttpResponse<String> listaInstituicaoB =
                obterAutenticado("/v1/instituicao/alunos", tokenInstituicaoB);
        assertEquals(1, json().readTree(listaInstituicaoA.body()).size());
        assertEquals(0, json().readTree(listaInstituicaoB.body()).size());

        var vinculo = json().readTree(listaInstituicaoA.body()).get(0);
        String vinculoId = vinculo.get("id").asText();
        assertTrue(vinculo.get("contaAlunoId").asText().equals(idConta(emailAlunoA).toString())
                || vinculo.get("contaAlunoId").asText().equals(idConta(emailAlunoB).toString()));
        assertEquals(404, deletarAutenticado(
                "/v1/instituicao/vinculos/" + vinculoId, csrf, tokenInstituicaoB).statusCode());
        assertEquals(204, deletarAutenticado(
                "/v1/instituicao/vinculos/" + vinculoId, csrf, tokenInstituicaoA).statusCode());

        HttpResponse<String> capacidade = obterAutenticado("/v1/instituicao/resumo-vagas", tokenInstituicaoA);
        var resumo = json().readTree(capacidade.body()).get(0);
        assertEquals(1, resumo.get("quantidadeVagas").asInt());
        assertEquals(0, resumo.get("vagasUtilizadas").asInt());
        assertEquals(1, resumo.get("vagasDisponiveis").asInt());
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT situacao, desvinculado_em FROM vinculos_institucionais WHERE id = ?")) {
            comando.setObject(1, UUID.fromString(vinculoId));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals("ENCERRADO", resultado.getString("situacao"));
                assertTrue(resultado.getTimestamp("desvinculado_em") != null);
            }
        }
    }

    private void cadastrarAluno(String email, ProtecaoCsrf csrf) throws Exception {
        assertEquals(201, postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Perfil Teste","email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null).statusCode());
    }

    private void cadastrarInstituicao(String email, ProtecaoCsrf csrf) throws Exception {
        assertEquals(201, postarComCsrf(
                "/v1/contas/instituicoes",
                """
                {"razaoSocial":"Instituicao de Teste","email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null).statusCode());
    }

    private UUID criarContratoInstitucional(String emailInstituicao, int quantidadeVagas, String codigoPlano)
            throws Exception {
        UUID idPlano = UUID.randomUUID();
        UUID idContrato = UUID.randomUUID();
        try (Connection conexao = fonteDeDados.getConnection();
                var plano = conexao.prepareStatement(
                        "INSERT INTO planos (id, codigo, modalidade, nome, preco_centavos) "
                                + "VALUES (?, ?, 'INSTITUCIONAL', 'Plano institucional teste', 0)");
                var contrato = conexao.prepareStatement(
                        "INSERT INTO contratos "
                                + "(id, conta_titular_id, plano_id, situacao, quantidade_vagas, "
                                + "preco_centavos_contratado, iniciado_em) "
                                + "VALUES (?, (SELECT id FROM contas WHERE email_normalizado = ?), ?, "
                                + "'ATIVO', ?, 0, now())")) {
            plano.setObject(1, idPlano);
            plano.setString(2, codigoPlano);
            assertEquals(1, plano.executeUpdate());
            contrato.setObject(1, idContrato);
            contrato.setString(2, emailInstituicao.toLowerCase(java.util.Locale.ROOT));
            contrato.setObject(3, idPlano);
            contrato.setInt(4, quantidadeVagas);
            assertEquals(1, contrato.executeUpdate());
        }
        return idContrato;
    }

    private UUID idConta(String email) {
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT id FROM contas WHERE email_normalizado = ?")) {
            comando.setString(1, email.toLowerCase(java.util.Locale.ROOT));
            try (ResultSet resultado = comando.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalStateException("Conta de teste não foi encontrada.");
                }
                return resultado.getObject(1, UUID.class);
            }
        } catch (java.sql.SQLException excecao) {
            throw new IllegalStateException("Falha ao carregar conta de teste.", excecao);
        }
    }

    private static void aguardar(CountDownLatch inicio) {
        try {
            inicio.await();
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teste de concorrência interrompido.", excecao);
        }
    }

    private String autenticar(String email, ProtecaoCsrf csrf) throws Exception {
        HttpResponse<String> login = postarComCsrf(
                "/v1/sessoes",
                """
                {"email":"%s","senha":"senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null);
        assertEquals(200, login.statusCode());
        return json().readTree(login.body()).get("accessToken").asText();
    }

    private HttpResponse<String> postarAutenticado(
            String caminho, String corpo, ProtecaoCsrf csrf, String accessToken) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api" + caminho))
                .header("Content-Type", "application/json")
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postarAutenticadoSemChecked(
            String caminho, String corpo, ProtecaoCsrf csrf, String accessToken) {
        try {
            return postarAutenticado(caminho, corpo, csrf, accessToken);
        } catch (Exception excecao) {
            throw new IllegalStateException("Falha na chamada concorrente de vínculo.", excecao);
        }
    }

    private HttpResponse<String> obterAutenticado(String caminho, String accessToken) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api" + caminho))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deletarAutenticado(
            String caminho, ProtecaoCsrf csrf, String accessToken) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api" + caminho))
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private ProtecaoCsrf obterProtecaoCsrf() throws Exception {
        HttpResponse<String> resposta = obter("/v1/sessoes/csrf");
        String cookieCsrf = resposta.headers().allValues("Set-Cookie").stream()
                .filter(valor -> valor.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        String token = json().readTree(resposta.body()).get("token").asText();
        assertTrue(Arrays.stream(cookieCsrf.split(";"))
                .map(String::trim)
                .anyMatch("Path=/"::equals));
        String cookie = cookieCsrf.split(";", 2)[0];
        assertEquals(token, cookie.substring("XSRF-TOKEN=".length()));
        return new ProtecaoCsrf(cookie, token);
    }

    private HttpResponse<String> postarComCsrf(
            String caminho, String corpo, ProtecaoCsrf csrf, String tokenRefresh) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api" + caminho))
                .header("Content-Type", "application/json")
                .header("Cookie", tokenRefresh == null
                        ? csrf.cookie()
                        : csrf.cookie() + "; ivis_refresh=" + tokenRefresh)
                .header("X-XSRF-TOKEN", csrf.token())
                .POST(HttpRequest.BodyPublishers.ofString(corpo));
        return cliente.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String valorCookie(HttpResponse<String> resposta, String nome) {
        return resposta.headers().allValues("Set-Cookie").stream()
                .filter(valor -> valor.startsWith(nome + "="))
                .findFirst().orElseThrow()
                .split(";", 2)[0]
                .substring(nome.length() + 1);
    }

    private static tools.jackson.databind.ObjectMapper json() {
        return new tools.jackson.databind.ObjectMapper();
    }

    private record ProtecaoCsrf(String cookie, String token) {
    }

    @Test
    void rotaNaoAutorizadaUsaFormatoUniformeEPreservaIdValido() throws Exception {
        HttpResponse<String> resposta = obter("/v1/nao-existe", "X-Id-Requisicao", "abc-12345678");
        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("\"codigo\":\"nao_autenticado\""));
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
