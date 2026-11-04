package br.ivis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.ivis.ai.service.IaClient;
import br.ivis.ai.dto.ContratoIa.*;
import br.ivis.common.error.ExcecaoNegocio;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Sobe a aplicação inteira contra um PostgreSQL vazio (Testcontainers); exige Docker. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FluxosAplicacaoIntegracaoTest {

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

    @MockitoBean
    IaClient clienteIa;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> obter(String caminho, String... cabecalhos) throws Exception {
        HttpRequest.Builder construtor = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api" + caminho));
        for (int i = 0; i < cabecalhos.length; i += 2) {
            construtor.header(cabecalhos[i], cabecalhos[i + 1]);
        }
        return cliente.send(construtor.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void limpezaDeDadosNaoExisteSemPerfilDeDesenvolvimentoOuHomologacao() throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + "/api/v1/ambiente/limpar-dados"))
                .header("Content-Type", "application/json")
                .header("X-IVIS-Limpeza-Chave", "chave-de-teste-com-mais-de-32-caracteres")
                .POST(HttpRequest.BodyPublishers.ofString("{\"documento\":\"12345678901\"}"))
                .build();
        assertEquals(404, cliente.send(requisicao, HttpResponse.BodyHandlers.ofString()).statusCode());
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
                  "cpf": "12345678901",
                  "telefone": "81999990000",
                  "endereco": "Rua de teste, 10",
                  "senha": "Uma-senha-forte-123"
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
                        "SELECT c.email_normalizado, c.hash_senha, p.nome "
                                + "FROM contas c JOIN perfis_aluno p ON p.codigo_objeto_contas = c.objeto "
                                + "WHERE c.email_normalizado = 'ana@exemplo.com'")) {
            assertTrue(resultado.next());
            assertEquals("ana@exemplo.com", resultado.getString("email_normalizado"));
            assertNotEquals("Uma-senha-forte-123", resultado.getString("hash_senha"));
            assertEquals("Ana Aluna", resultado.getString("nome"));
        }
    }

    @Test
    void cadastroExigeOitoCaracteresEMisturaDeCaracteresNaSenha() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        for (String senha : List.of("Aa1!aaa", "aaaaaaa1!", "AAAAAAA1!", "Abcdefg!", "Abcdefg1")) {
            HttpResponse<String> resposta = postarComCsrf("/v1/contas/alunos", """
                    {"nome":"Teste Senha","email":"politica-%s@exemplo.test", "cpf":"12345678901",
                     "telefone":"81999990000","endereco":"Rua de teste, 10","senha":"%s"}
                    """.formatted(UUID.randomUUID(), senha), csrf, null);
            assertEquals(400, resposta.statusCode(), resposta.body());
            assertEquals("dados_invalidos", json().readTree(resposta.body()).get("codigo").asString());
        }
        HttpResponse<String> valida = postarComCsrf("/v1/contas/alunos", """
                {"nome":"Teste Senha","email":"politica-%s@exemplo.test", "cpf":"%s",
                 "telefone":"81999990000","endereco":"Rua de teste, 10","senha":"Aa1!abcd"}
                """.formatted(UUID.randomUUID(), cpfTeste()), csrf, null);
        assertEquals(201, valida.statusCode(), valida.body());
    }

    @Test
    void cadastroInstitucionalCriaContaComPerfilCorreto() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        HttpResponse<String> resposta = postarComCsrf(
                "/v1/contas/instituicoes",
                """
                {
                  "nomeResponsavel": "Responsável de Teste",
                  "nomeInstituicao": "Instituto de Teste",
                  "email": "contato@instituto-teste.example",
                  "senha": "Outra-senha-forte-456",
                  "cnpj": "12345678000195",
                  "telefone": "8133334444",
                  "endereco": "Rua de teste, 20"
                }
                """,
                csrf,
                null);

        assertEquals(201, resposta.statusCode());
        assertTrue(resposta.body().contains("\"tipoConta\":\"INSTITUICAO\""));
        try (Connection conexao = fonteDeDados.getConnection();
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(
                        "SELECT c.tipo_conta, p.nome_instituicao FROM contas c "
                                + "JOIN perfis_instituicao p ON p.codigo_objeto_contas = c.objeto "
                                + "WHERE c.email_normalizado = 'contato@instituto-teste.example'")) {
            assertTrue(resultado.next());
            assertEquals("INSTITUICAO", resultado.getString("tipo_conta"));
            assertEquals("Instituto de Teste", resultado.getString("nome_instituicao"));
        }
    }

    @Test
    void loginNaoRevelaSeEmailExiste() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String emailExistente = "login-" + UUID.randomUUID() + "@exemplo.test";
        assertEquals(201, postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Login Teste","email":"%s","cpf":"12345678902","telefone":"81999990001","endereco":"Rua de teste, 21","senha":"Senha-de-teste-segura-123"}
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
        assertEquals(json().readTree(senhaIncorreta.body()).get("codigo").asString(),
                json().readTree(emailInexistente.body()).get("codigo").asString());
        assertEquals(json().readTree(senhaIncorreta.body()).get("mensagem").asString(),
                json().readTree(emailInexistente.body()).get("mensagem").asString());
    }

    @Test
    void loginRenovaTokenUmaUnicaVezELogoutRevogaSessao() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String email = "sessao-" + UUID.randomUUID() + "@exemplo.test";
        HttpResponse<String> cadastro = postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Sessao Teste","email":"%s","cpf":"%s","telefone":"81999990003","endereco":"Rua de teste, 24","senha":"Senha-de-teste-segura-123"}
                """.formatted(email, cpfTeste()),
                csrf,
                null);
        assertEquals(201, cadastro.statusCode());

        HttpResponse<String> login = postarComCsrf(
                "/v1/sessoes",
                """
                {"email":"%s","senha":"Senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null);
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"tipoToken\":\"Bearer\""));
        String accessToken = json().readTree(login.body()).get("accessToken").asString();
        String tokenAntigo = valorCookie(login, "ivis_refresh");

        HttpResponse<String> renovacao = postarComCsrf(
                "/v1/sessoes/renovacao", "{}", csrf, tokenAntigo);
        assertEquals(200, renovacao.statusCode());
        String accessTokenNovo = json().readTree(renovacao.body()).get("accessToken").asString();
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
    void contratacaoRegistraAuditoriaUmaUnicaVezQuandoChaveEhReprocessada() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String email = "auditoria-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        String accessToken = autenticar(email, csrf);
        UUID contaId = idConta(email);
        UUID planoId;
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT objeto FROM planos WHERE codigo = 'IVIS-DEMO-INDIVIDUAL' AND ativo = true");
                ResultSet resultado = comando.executeQuery()) {
            assertTrue(resultado.next());
            planoId = resultado.getObject("objeto", UUID.class);
        }

        YearMonth validade = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        String corpo = """
                {"planoId":"%s","numeroCartao":"4242424242424242","validade":"%02d/%02d","cvv":"123"}
                """.formatted(planoId, validade.getMonthValue(), validade.getYear() % 100);
        String chaveIdempotencia = "cypress-" + UUID.randomUUID();

        HttpResponse<String> primeira = postarAutenticado(
                "/v1/contratacoes/simuladas", corpo, csrf, accessToken, chaveIdempotencia);
        HttpResponse<String> reprocessada = postarAutenticado(
                "/v1/contratacoes/simuladas", corpo, csrf, accessToken, chaveIdempotencia);

        assertEquals(200, primeira.statusCode());
        assertEquals(200, reprocessada.statusCode());
        var resultadoInicial = json().readTree(primeira.body());
        var resultadoReprocessado = json().readTree(reprocessada.body());
        assertFalse(resultadoInicial.get("reprocessada").asBoolean());
        assertTrue(resultadoReprocessado.get("reprocessada").asBoolean());
        assertEquals(resultadoInicial.get("contratoId").asString(),
                resultadoReprocessado.get("contratoId").asString());
        assertEquals(resultadoInicial.get("pagamentoId").asString(),
                resultadoReprocessado.get("pagamentoId").asString());

        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement("""
                        SELECT e.codigo_objeto_contas, e.acao, e.resultado, e.tipo_recurso,
                               e.detalhes->>'modo' AS modo, e.detalhes->>'situacaoPagamento' AS situacao_pagamento,
                               (SELECT count(*) FROM eventos_auditoria a
                                WHERE a.codigo_objeto_contratos = e.codigo_objeto_contratos
                                  AND a.acao = e.acao) AS eventos,
                               (SELECT count(*) FROM pagamentos p
                                WHERE p.codigo_objeto_contratos = e.codigo_objeto_contratos) AS pagamentos
                        FROM eventos_auditoria e
                        WHERE e.codigo_objeto_contratos = ?
                        """)) {
            comando.setObject(1, UUID.fromString(resultadoInicial.get("contratoId").asString()));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(contaId, resultado.getObject("codigo_objeto_contas", UUID.class));
                assertEquals("CONTRATACAO_PAGAMENTO_SIMULADO", resultado.getString("acao"));
                assertEquals("SUCESSO", resultado.getString("resultado"));
                assertEquals("CONTRATO", resultado.getString("tipo_recurso"));
                assertEquals("SIMULADO", resultado.getString("modo"));
                assertEquals("APROVADO", resultado.getString("situacao_pagamento"));
                assertEquals(1, resultado.getInt("eventos"));
                assertEquals(1, resultado.getInt("pagamentos"));
                assertFalse(resultado.next());
            }
        }
    }

    @Test
    void pagamentoRecusadoRegistraAuditoriaDeFalhaSemCriarEmailDeConfirmacao() throws Exception {
        ProtecaoCsrf csrf = obterProtecaoCsrf();
        String email = "pagamento-recusado-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        String accessToken = autenticar(email, csrf);
        UUID planoId;
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT objeto FROM planos WHERE codigo = 'IVIS-DEMO-INDIVIDUAL' AND ativo = true");
                ResultSet resultado = comando.executeQuery()) {
            assertTrue(resultado.next());
            planoId = resultado.getObject("objeto", UUID.class);
        }

        YearMonth validade = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        String corpo = """
                {"planoId":"%s","numeroCartao":"4000000000000002","validade":"%02d/%02d","cvv":"123"}
                """.formatted(planoId, validade.getMonthValue(), validade.getYear() % 100);
        HttpResponse<String> resposta = postarAutenticado(
                "/v1/contratacoes/simuladas", corpo, csrf, accessToken, "recusado-" + UUID.randomUUID());

        assertEquals(200, resposta.statusCode());
        var resultadoCompra = json().readTree(resposta.body());
        assertEquals("RECUSADO", resultadoCompra.get("situacaoPagamento").asString());
        assertEquals("ENCERRADO", resultadoCompra.get("situacaoContrato").asString());

        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement("""
                        SELECT e.acao, e.resultado, e.tipo_recurso,
                               e.detalhes->>'modo' AS modo, e.detalhes->>'situacaoPagamento' AS situacao_pagamento,
                               (SELECT count(*) FROM emails_simulados m
                                WHERE m.codigo_objeto_contratos = e.codigo_objeto_contratos) AS emails
                        FROM eventos_auditoria e
                        WHERE e.codigo_objeto_contratos = ?
                        """)) {
            comando.setObject(1, UUID.fromString(resultadoCompra.get("contratoId").asString()));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals("CONTRATACAO_PAGAMENTO_SIMULADO", resultado.getString("acao"));
                assertEquals("FALHA", resultado.getString("resultado"));
                assertEquals("CONTRATO", resultado.getString("tipo_recurso"));
                assertEquals("SIMULADO", resultado.getString("modo"));
                assertEquals("RECUSADO", resultado.getString("situacao_pagamento"));
                assertEquals(0, resultado.getInt("emails"));
                assertFalse(resultado.next());
            }
        }
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
        String habilidadeId = json().readTree(habilidadeCriada.body()).get("id").asString();
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
        String vagaId = json().readTree(vagaCriada.body()).get("id").asString();
        assertEquals("Java", json().readTree(vagaCriada.body()).get("requisitos").get(0).asString());

        String snapshot = """
                {"titulo":"Desenvolvedor Java","requisitos":["Java","PostgreSQL"]}
                """;
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "INSERT INTO treinamentos "
                                + "(objeto, codigo_objeto_perfis_aluno, codigo_objeto_vagas_interesse, tipo_contexto, contexto_registrado) "
                                + "VALUES (?, (SELECT objeto FROM contas WHERE email_normalizado = ?), ?, 'VAGA', ?::jsonb)")) {
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
                        "SELECT v.arquivado_em, t.codigo_objeto_vagas_interesse, t.contexto_registrado->>'titulo' AS titulo "
                                + "FROM vagas_interesse v JOIN treinamentos t "
                                + "ON t.codigo_objeto_vagas_interesse = v.objeto WHERE v.objeto = ?")) {
            comando.setObject(1, UUID.fromString(vagaId));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertTrue(resultado.getTimestamp("arquivado_em") != null);
                assertEquals(UUID.fromString(vagaId), resultado.getObject("codigo_objeto_vagas_interesse", UUID.class));
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
        String vinculoId = vinculo.get("id").asString();
        assertTrue(vinculo.get("contaAlunoId").asString().equals(idConta(emailAlunoA).toString())
                || vinculo.get("contaAlunoId").asString().equals(idConta(emailAlunoB).toString()));
        assertEquals(404, deletarAutenticado(
                "/v1/instituicao/vinculos/" + vinculoId, csrf, tokenInstituicaoB).statusCode());
        assertEquals(204, deletarAutenticado(
                "/v1/instituicao/vinculos/" + vinculoId, csrf, tokenInstituicaoA).statusCode());

        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.prepareStatement(
                "SELECT acao,codigo_objeto_contas,ocorrido_em FROM eventos_auditoria WHERE codigo_objeto_contratos=? ORDER BY ocorrido_em")) {
            comando.setObject(1, contratoA);
            try (var eventos = comando.executeQuery()) {
                assertTrue(eventos.next());
                assertEquals("VINCULO_ALUNO_CRIADO", eventos.getString("acao"));
                assertEquals(idConta(emailInstituicaoA), eventos.getObject("codigo_objeto_contas", UUID.class));
                assertTrue(eventos.getTimestamp("ocorrido_em") != null);
                assertTrue(eventos.next());
                assertEquals("VINCULO_ALUNO_REMOVIDO", eventos.getString("acao"));
                assertFalse(eventos.next());
            }
            try (var alterar = conexao.prepareStatement("UPDATE eventos_auditoria SET resultado='FALHA' WHERE codigo_objeto_contratos=?");
                    var remover = conexao.prepareStatement("DELETE FROM eventos_auditoria WHERE codigo_objeto_contratos=?")) {
                alterar.setObject(1, contratoA); remover.setObject(1, contratoA);
                assertEquals("42501", assertThrows(java.sql.SQLException.class, alterar::executeUpdate).getSQLState());
                assertEquals("42501", assertThrows(java.sql.SQLException.class, remover::executeUpdate).getSQLState());
            }
        }

        HttpResponse<String> capacidade = obterAutenticado("/v1/instituicao/resumo-vagas", tokenInstituicaoA);
        var resumo = json().readTree(capacidade.body()).get(0);
        assertEquals(1, resumo.get("quantidadeVagas").asInt());
        assertEquals(0, resumo.get("vagasUtilizadas").asInt());
        assertEquals(1, resumo.get("vagasDisponiveis").asInt());
        try (Connection conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "SELECT situacao, desvinculado_em FROM vinculos_institucionais WHERE objeto = ?")) {
            comando.setObject(1, UUID.fromString(vinculoId));
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals("ENCERRADO", resultado.getString("situacao"));
                assertTrue(resultado.getTimestamp("desvinculado_em") != null);
            }
        }
    }

    @Test
    void alunoVinculadoNaoContrataPlanoIndividual() throws Exception {
        var csrf = obterProtecaoCsrf();
        String aluno = "restrito-" + UUID.randomUUID() + "@exemplo.test";
        String instituicao = "titular-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(aluno, csrf);
        cadastrarInstituicao(instituicao, csrf);
        UUID contrato = criarContratoInstitucional(instituicao, 1, "limite-" + UUID.randomUUID());
        assertEquals(201, postarAutenticado("/v1/instituicao/vinculos",
                """
                {"contratoId":"%s","contaAlunoId":"%s"}
                """.formatted(contrato, idConta(aluno)), csrf, autenticar(instituicao, csrf)).statusCode());
        UUID plano;
        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.createStatement();
                var linhas = comando.executeQuery("SELECT objeto FROM planos WHERE codigo='IVIS-DEMO-INDIVIDUAL'")) {
            assertTrue(linhas.next()); plano = linhas.getObject(1, UUID.class);
        }
        var validade = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        var resposta = postarAutenticado("/v1/contratacoes/simuladas", """
                {"planoId":"%s","numeroCartao":"4242424242424242","validade":"%02d/%02d","cvv":"123"}
                """.formatted(plano, validade.getMonthValue(), validade.getYear() % 100),
                csrf, autenticar(aluno, csrf), UUID.randomUUID().toString());
        assertEquals(409, resposta.statusCode());
        assertEquals("aluno_vinculado_a_instituicao", json().readTree(resposta.body()).get("codigo").asString());
        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.prepareStatement(
                "SELECT count(*) FROM contratos WHERE codigo_objeto_contas=?")) {
            comando.setObject(1, idConta(aluno));
            try (var linhas = comando.executeQuery()) { linhas.next(); assertEquals(0, linhas.getInt(1)); }
        }
    }

    @Test
    void instituicaoNaoConsultaNemConcluiTreinoDeAluno() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "perfil-treino-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarInstituicao(email, csrf);
        String token = autenticar(email, csrf);
        assertEquals(403, obterAutenticado("/v1/treinos", token).statusCode());
        assertEquals(403, postarAutenticado("/v1/treinos", """
                {"tipoContexto":"HABILIDADES","habilidades":["Java"]}
                """, csrf, token).statusCode());
    }

    @Test
    void alunoSemPlanoNaoIniciaTreinoNemGravaPergunta() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "sem-plano-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        String token = autenticar(email, csrf);

        var resposta = postarAutenticado("/v1/treinos",
                "{\"tipoContexto\":\"HABILIDADES\",\"habilidades\":[\"Java\"]}", csrf, token);

        assertEquals(403, resposta.statusCode(), resposta.body());
        try (var conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement(
                        "select count(*) from treinamentos where codigo_objeto_perfis_aluno=?")) {
            comando.setObject(1, idConta(email));
            try (var linhas = comando.executeQuery()) {
                assertTrue(linhas.next());
                assertEquals(0, linhas.getInt(1));
            }
        }
    }

    @Test
    void falhaDaIaPreservaPerguntaERetentativaSalvaRespostaUmaVez() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "retentativa-treino-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        UUID aluno = idConta(email);
        String token = autenticar(email, csrf);
        try (var conexao = fonteDeDados.getConnection();
                var contrato = conexao.prepareStatement("""
                    INSERT INTO contratos (codigo_objeto_contas,codigo_objeto_planos,situacao,preco_centavos_contratado,iniciado_em)
                    SELECT ?,objeto,'ATIVO',preco_centavos,now() FROM planos WHERE codigo='IVIS-DEMO-INDIVIDUAL'
                    """)) {
            contrato.setObject(1, aluno);
            contrato.executeUpdate();
        }
        assertEquals(201, postarAutenticado("/v1/habilidades",
                "{\"nome\":\"Java\",\"nivel\":3}", csrf, token).statusCode());
        AtomicBoolean falhar = new AtomicBoolean(true);
        when(clienteIa.gerarPergunta(any())).thenAnswer(invocacao -> {
            GerarPerguntaEntrada entrada = invocacao.getArgument(0);
            if (entrada.numeroPergunta() == 2 && falhar.getAndSet(false)) {
                throw new ExcecaoNegocio(HttpStatus.SERVICE_UNAVAILABLE, "ia_indisponivel", "IA indisponível.");
            }
            return new GerarPerguntaResposta(new PerguntaGerada("Pergunta " + entrada.numeroPergunta()),
                    "modelo-teste", "perguntas-v2");
        });

        var inicio = postarAutenticado("/v1/treinos",
                "{\"tipoContexto\":\"HABILIDADES\",\"habilidades\":[\"Java\"]}", csrf, token);
        assertEquals(201, inicio.statusCode(), inicio.body());
        var sessao = json().readTree(inicio.body());
        String treinoId = sessao.get("id").asString();
        String perguntaId = sessao.get("perguntas").get(0).get("id").asString();
        String caminho = "/v1/treinos/" + treinoId + "/respostas";
        String corpo = "{\"perguntaId\":\"%s\",\"resposta\":\"Resposta preservada\"}"
                .formatted(perguntaId);

        assertEquals(503, postarAutenticado(caminho, corpo, csrf, token).statusCode());
        var retomada = json().readTree(obterAutenticado("/v1/treinos/em-andamento", token).body());
        assertEquals(1, retomada.get("perguntas").size());
        assertTrue(retomada.get("perguntas").get(0).get("resposta").isNull());

        var repeticao = postarAutenticado(caminho, corpo, csrf, token);
        assertEquals(200, repeticao.statusCode(), repeticao.body());
        var atualizada = json().readTree(repeticao.body());
        assertEquals("Resposta preservada", atualizada.get("perguntas").get(0).get("resposta").asString());
        assertEquals(2, atualizada.get("perguntas").size());
        assertEquals(200, postarAutenticado(caminho, corpo, csrf, token).statusCode());
    }

    @Test
    void sessaoTreinoPersistePerguntasRespostasERetomaAposNovaConsulta() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "sessao-treino-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        UUID aluno = idConta(email);
        String token = autenticar(email, csrf);
        try (var conexao = fonteDeDados.getConnection();
                var contrato = conexao.prepareStatement("""
                    INSERT INTO contratos (codigo_objeto_contas,codigo_objeto_planos,situacao,preco_centavos_contratado,iniciado_em)
                    SELECT ?,objeto,'ATIVO',preco_centavos,now() FROM planos WHERE codigo='IVIS-DEMO-INDIVIDUAL'
                    """)) {
            contrato.setObject(1, aluno);
            contrato.executeUpdate();
        }
        assertEquals(201, postarAutenticado("/v1/habilidades",
                "{\"nome\":\"Java\",\"nivel\":3}", csrf, token).statusCode());
        when(clienteIa.gerarPergunta(any())).thenAnswer(invocacao -> {
            GerarPerguntaEntrada entrada = invocacao.getArgument(0);
            return new GerarPerguntaResposta(new PerguntaGerada("Pergunta " + entrada.numeroPergunta()),
                    "modelo-teste", "perguntas-v2");
        });
        when(clienteIa.gerarAvaliacao(any())).thenReturn(new GerarAvaliacaoResposta(
                82, List.of("Exemplo concreto"), List.of("Detalhar resultados"), "modelo-teste", "avaliacao-v2"));

        var inicio = postarAutenticado("/v1/treinos",
                "{\"tipoContexto\":\"HABILIDADES\",\"habilidades\":[\"Java\"]}", csrf, token);
        assertEquals(201, inicio.statusCode(), inicio.body());
        var sessao = json().readTree(inicio.body());
        String treinoId = sessao.get("id").asString();
        assertEquals("Pergunta 1", sessao.get("perguntas").get(0).get("texto").asString());
        assertEquals(treinoId, json().readTree(obterAutenticado(
                "/v1/treinos/em-andamento", token).body()).get("id").asString());

        for (int numero = 1; numero <= 3; numero++) {
            String perguntaId = sessao.get("perguntas").get(numero - 1).get("id").asString();
            String corpo = "{\"perguntaId\":\"%s\",\"resposta\":\"Resposta %d\"}"
                    .formatted(perguntaId, numero);
            var resposta = postarAutenticado("/v1/treinos/" + treinoId + "/respostas", corpo, csrf, token);
            assertEquals(200, resposta.statusCode(), resposta.body());
            sessao = json().readTree(resposta.body());
            assertEquals(numero, sessao.get("perguntas").get(numero - 1).get("numero").asInt());
            assertEquals(200, postarAutenticado("/v1/treinos/" + treinoId + "/respostas",
                    corpo, csrf, token).statusCode());
        }
        var conclusao = postarAutenticado("/v1/treinos/" + treinoId + "/concluir", "{}", csrf, token);
        assertEquals(200, conclusao.statusCode(), conclusao.body());
        assertEquals(82, json().readTree(conclusao.body()).get("nota").asInt());
        assertEquals(200, postarAutenticado("/v1/treinos/" + treinoId + "/concluir",
                "{}", csrf, token).statusCode());
        assertEquals(204, obterAutenticado("/v1/treinos/em-andamento", token).statusCode());
        try (var conexao = fonteDeDados.getConnection();
                var comando = conexao.prepareStatement("""
                    SELECT count(*), count(*) FILTER (WHERE origem='IA' AND versao_modelo='modelo-teste')
                    FROM perguntas_entrevista WHERE codigo_objeto_treinamentos=?
                    """)) {
            comando.setObject(1, UUID.fromString(treinoId));
            try (var linhas = comando.executeQuery()) {
                assertTrue(linhas.next());
                assertEquals(3, linhas.getInt(1));
                assertEquals(3, linhas.getInt(2));
            }
        }
        var vagaCriada = postarAutenticado("/v1/vagas", """
                {"titulo":"Desenvolvedor Java","senioridade":"Pleno", "requisitos":["Spring"]}
                """, csrf, token);
        assertEquals(201, vagaCriada.statusCode(), vagaCriada.body());
        String vagaId = json().readTree(vagaCriada.body()).get("id").asString();
        var treinoVaga = postarAutenticado("/v1/treinos", """
                {"tipoContexto":"VAGA","vagaId":"%s","habilidades":[]}
                """.formatted(vagaId), csrf, token);
        assertEquals(201, treinoVaga.statusCode(), treinoVaga.body());
        assertEquals(vagaId, json().readTree(treinoVaga.body()).get("vagaId").asString());
    }

    @Test
    void falhaNaAvaliacaoFinalPreservaRespostasERetentativaConcluiUmaUnicaVez() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "avaliacao-falha-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        concederPlanoIndividual(email);
        String token = autenticar(email, csrf);
        String treinoId = iniciarTreinoCruzado(csrf, token);
        var sessao = json().readTree(obterAutenticado("/v1/treinos/em-andamento", token).body());
        for (int numero = 1; numero <= 3; numero++) {
            String perguntaId = sessao.get("perguntas").get(numero - 1).get("id").asString();
            var resposta = postarAutenticado("/v1/treinos/" + treinoId + "/respostas", """
                    {"perguntaId":"%s","resposta":"Resposta %d"}
                    """.formatted(perguntaId, numero), csrf, token);
            assertEquals(200, resposta.statusCode(), resposta.body());
            sessao = json().readTree(resposta.body());
        }
        when(clienteIa.gerarAvaliacao(any()))
                .thenThrow(new ExcecaoNegocio(HttpStatus.SERVICE_UNAVAILABLE,
                        "ia_indisponivel", "IA indisponível."))
                .thenReturn(new GerarAvaliacaoResposta(82, List.of("Exemplo concreto"),
                        List.of("Detalhar resultados"), "modelo-teste", "avaliacao-v2"));

        String caminho = "/v1/treinos/" + treinoId + "/concluir";
        var falha = postarAutenticado(caminho, "{}", csrf, token);
        assertEquals(503, falha.statusCode(), falha.body());
        assertEquals("ia_indisponivel", json().readTree(falha.body()).get("codigo").asString());
        var retomada = obterAutenticado("/v1/treinos/em-andamento", token);
        assertEquals(200, retomada.statusCode(), retomada.body());
        assertEquals(sessao, json().readTree(retomada.body()));
        assertEquals(0, json().readTree(obterAutenticado("/v1/treinos", token).body()).size());
        verificarPersistenciaTreino(treinoId, "EM_ANDAMENTO", 3, 0);

        // Uma nova sessão HTTP deve retomar o treino persistido e permitir a retentativa.
        token = autenticar(email, csrf);
        var conclusao = postarAutenticado(caminho, "{}", csrf, token);
        assertEquals(200, conclusao.statusCode(), conclusao.body());
        assertEquals(82, json().readTree(conclusao.body()).get("nota").asInt());
        var repeticao = postarAutenticado(caminho, "{}", csrf, token);
        assertEquals(200, repeticao.statusCode(), repeticao.body());
        assertEquals(json().readTree(conclusao.body()), json().readTree(repeticao.body()));
        assertEquals(204, obterAutenticado("/v1/treinos/em-andamento", token).statusCode());
        var historico = json().readTree(obterAutenticado("/v1/treinos", token).body());
        assertEquals(1, historico.size());
        assertEquals(treinoId, historico.get(0).get("id").asString());
        verificarPersistenciaTreino(treinoId, "CONCLUIDO", 3, 1);
        org.mockito.Mockito.verify(clienteIa, org.mockito.Mockito.times(2)).gerarAvaliacao(any());
    }

    @Test
    void outroAlunoNaoRespondeConcluiNemCancelaTreinoDoTitular() throws Exception {
        var csrf = obterProtecaoCsrf();
        String titular = "treino-titular-" + UUID.randomUUID() + "@exemplo.test";
        String outro = "treino-outro-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(titular, csrf);
        cadastrarAluno(outro, csrf);
        concederPlanoIndividual(titular);
        concederPlanoIndividual(outro);
        String tokenTitular = autenticar(titular, csrf);
        String tokenOutro = autenticar(outro, csrf);
        String treinoId = iniciarTreinoCruzado(csrf, tokenTitular);
        var sessao = json().readTree(obterAutenticado("/v1/treinos/em-andamento", tokenTitular).body());
        String perguntaId = sessao.get("perguntas").get(0).get("id").asString();

        assertEquals(404, postarAutenticado("/v1/treinos/" + treinoId + "/respostas", """
                {"perguntaId":"%s","resposta":"Resposta de outra conta"}
                """.formatted(perguntaId), csrf, tokenOutro).statusCode());
        assertEquals(404, postarAutenticado("/v1/treinos/" + treinoId + "/concluir",
                "{}", csrf, tokenOutro).statusCode());
        assertEquals(404, deletarAutenticado("/v1/treinos/" + treinoId, csrf, tokenOutro).statusCode());
        assertEquals(204, obterAutenticado("/v1/treinos/em-andamento", tokenOutro).statusCode());
        assertEquals(sessao, json().readTree(obterAutenticado(
                "/v1/treinos/em-andamento", tokenTitular).body()));
        verificarPersistenciaTreino(treinoId, "EM_ANDAMENTO", 0, 0);
        org.mockito.Mockito.verify(clienteIa, org.mockito.Mockito.times(1)).gerarPergunta(any());
        org.mockito.Mockito.verify(clienteIa, org.mockito.Mockito.never()).gerarAvaliacao(any());
    }

    @Test
    void removerVinculoBloqueiaTreinoComTokenExistenteELiberaVagaParaOutroAluno() throws Exception {
        var csrf = obterProtecaoCsrf();
        String aluno = "acesso-vinculo-" + UUID.randomUUID() + "@exemplo.test";
        String outro = "nova-vaga-" + UUID.randomUUID() + "@exemplo.test";
        String instituicao = "revoga-acesso-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(aluno, csrf);
        cadastrarAluno(outro, csrf);
        cadastrarInstituicao(instituicao, csrf);
        String tokenInstituicao = autenticar(instituicao, csrf);
        String tokenAluno = autenticar(aluno, csrf);
        UUID contrato = criarContratoInstitucional(instituicao, 1, "revogacao-" + UUID.randomUUID());
        var vinculo = postarAutenticado("/v1/instituicao/vinculos", """
                {"contratoId":"%s","contaAlunoId":"%s"}
                """.formatted(contrato, idConta(aluno)), csrf, tokenInstituicao);
        assertEquals(201, vinculo.statusCode(), vinculo.body());
        var lista = json().readTree(obterAutenticado("/v1/instituicao/alunos", tokenInstituicao).body());
        String vinculoId = lista.get(0).get("id").asString();
        String treinoId = iniciarTreinoCruzado(csrf, tokenAluno);
        var sessao = json().readTree(obterAutenticado("/v1/treinos/em-andamento", tokenAluno).body());
        String perguntaId = sessao.get("perguntas").get(0).get("id").asString();

        assertEquals(204, deletarAutenticado("/v1/instituicao/vinculos/" + vinculoId,
                csrf, tokenInstituicao).statusCode());
        assertEquals(403, obterAutenticado("/v1/treinos/em-andamento", tokenAluno).statusCode());
        assertEquals(403, postarAutenticado("/v1/treinos", """
                {"tipoContexto":"HABILIDADES","habilidades":["Java"]}
                """, csrf, tokenAluno).statusCode());
        assertEquals(403, postarAutenticado("/v1/treinos/" + treinoId + "/respostas", """
                {"perguntaId":"%s","resposta":"Tentativa após remoção"}
                """.formatted(perguntaId), csrf, tokenAluno).statusCode());
        assertEquals(403, postarAutenticado("/v1/treinos/" + treinoId + "/concluir",
                "{}", csrf, tokenAluno).statusCode());
        verificarPersistenciaTreino(treinoId, "EM_ANDAMENTO", 0, 0);
        org.mockito.Mockito.verify(clienteIa, org.mockito.Mockito.times(1)).gerarPergunta(any());
        org.mockito.Mockito.verify(clienteIa, org.mockito.Mockito.never()).gerarAvaliacao(any());

        var capacidade = json().readTree(obterAutenticado(
                "/v1/instituicao/resumo-vagas", tokenInstituicao).body()).get(0);
        assertEquals(0, capacidade.get("vagasUtilizadas").asInt());
        assertEquals(1, capacidade.get("vagasDisponiveis").asInt());
        var novoVinculo = postarAutenticado("/v1/instituicao/vinculos", """
                {"contratoId":"%s","contaAlunoId":"%s"}
                """.formatted(contrato, idConta(outro)), csrf, tokenInstituicao);
        assertEquals(201, novoVinculo.statusCode(), novoVinculo.body());
        String tokenOutro = autenticar(outro, csrf);
        String novoTreinoId = iniciarTreinoCruzado(csrf, tokenOutro);
        assertNotEquals(treinoId, novoTreinoId);
        assertEquals(403, obterAutenticado("/v1/treinos/em-andamento", tokenAluno).statusCode());
    }

    private void concederPlanoIndividual(String email) throws Exception {
        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.prepareStatement("""
                INSERT INTO contratos (codigo_objeto_contas,codigo_objeto_planos,situacao,preco_centavos_contratado,iniciado_em)
                SELECT ?,objeto,'ATIVO',preco_centavos,now() FROM planos WHERE codigo='IVIS-DEMO-INDIVIDUAL'
                """)) {
            comando.setObject(1, idConta(email));
            assertEquals(1, comando.executeUpdate());
        }
    }

    private String iniciarTreinoCruzado(ProtecaoCsrf csrf, String token) throws Exception {
        assertEquals(201, postarAutenticado("/v1/habilidades",
                "{\"nome\":\"Java\",\"nivel\":3}", csrf, token).statusCode());
        org.mockito.Mockito.doAnswer(invocacao -> {
            GerarPerguntaEntrada entrada = invocacao.getArgument(0);
            return new GerarPerguntaResposta(new PerguntaGerada("Pergunta " + entrada.numeroPergunta()),
                    "modelo-teste", "perguntas-v2");
        }).when(clienteIa).gerarPergunta(any());
        var inicio = postarAutenticado("/v1/treinos", """
                {"tipoContexto":"HABILIDADES","habilidades":["Java"]}
                """, csrf, token);
        assertEquals(201, inicio.statusCode(), inicio.body());
        return json().readTree(inicio.body()).get("id").asString();
    }

    private void verificarPersistenciaTreino(String treinoId, String situacao, int respostas, int avaliacoes)
            throws Exception {
        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.prepareStatement("""
                SELECT t.situacao, t.finalizado_em,
                  (SELECT count(*) FROM respostas_entrevista r JOIN perguntas_entrevista p
                    ON p.objeto=r.codigo_objeto_perguntas_entrevista WHERE p.codigo_objeto_treinamentos=t.objeto),
                  (SELECT count(*) FROM avaliacoes_treinamento a WHERE a.codigo_objeto_treinamentos=t.objeto)
                FROM treinamentos t WHERE t.objeto=?
                """)) {
            comando.setObject(1, UUID.fromString(treinoId));
            try (var linhas = comando.executeQuery()) {
                assertTrue(linhas.next());
                assertEquals(situacao, linhas.getString(1));
                assertEquals("CONCLUIDO".equals(situacao), linhas.getTimestamp(2) != null);
                assertEquals(respostas, linhas.getInt(3));
                assertEquals(avaliacoes, linhas.getInt(4));
            }
        }
    }

    @Test
    void historicoIgnoraContaInformadaNaUrlERetornaSomenteTreinosDoTitular() throws Exception {
        var csrf = obterProtecaoCsrf();
        String emailA = "historico-a-" + UUID.randomUUID() + "@exemplo.test";
        String emailB = "historico-b-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(emailA, csrf); cadastrarAluno(emailB, csrf);
        UUID alunoA = idConta(emailA), alunoB = idConta(emailB);
        UUID treinoA = prepararHistorico(alunoA), treinoB = prepararHistorico(alunoB);
        var listaA = json().readTree(obterAutenticado("/v1/treinos?contaId=" + alunoB, autenticar(emailA, csrf)).body());
        var listaB = json().readTree(obterAutenticado("/v1/treinos", autenticar(emailB, csrf)).body());
        assertEquals(1, listaA.size()); assertEquals(1, listaB.size());
        assertEquals(treinoA.toString(), listaA.get(0).get("id").asString());
        assertEquals(treinoB.toString(), listaB.get(0).get("id").asString());
    }

    private UUID prepararHistorico(UUID aluno) throws Exception {
        UUID treino = UUID.randomUUID();
        try (var conexao = fonteDeDados.getConnection();
                var contrato = conexao.prepareStatement("""
                    INSERT INTO contratos (codigo_objeto_contas,codigo_objeto_planos,situacao,preco_centavos_contratado,iniciado_em)
                    SELECT ?,objeto,'ATIVO',preco_centavos,now() FROM planos WHERE codigo='IVIS-DEMO-INDIVIDUAL'
                    """);
                var treinamento = conexao.prepareStatement("""
                    INSERT INTO treinamentos (objeto,codigo_objeto_perfis_aluno,tipo_contexto,situacao,contexto_registrado,finalizado_em)
                    VALUES (?,?,'HABILIDADES','CONCLUIDO','{"descricao":"Java"}',now())
                    """);
                var avaliacao = conexao.prepareStatement("""
                    INSERT INTO avaliacoes_treinamento (codigo_objeto_treinamentos,nota,versao_modelo,versao_prompt)
                    VALUES (?,80,'sintetico','v1')
                    """)) {
            contrato.setObject(1, aluno); contrato.executeUpdate();
            treinamento.setObject(1, treino); treinamento.setObject(2, aluno); treinamento.executeUpdate();
            avaliacao.setObject(1, treino); avaliacao.executeUpdate();
        }
        return treino;
    }

    @Test
    void trocaDeSenhaExigeSenhaAtualERevogaRenovacoesAnteriores() throws Exception {
        var csrf = obterProtecaoCsrf();
        String email = "senha-" + UUID.randomUUID() + "@exemplo.test";
        cadastrarAluno(email, csrf);
        String token = autenticar(email, csrf);
        assertEquals(401, postarAutenticado("/v1/conta/senha", """
                {"senhaAtual":"senha-incorreta","novaSenha":"Nova-senha-segura-456"}
                """, csrf, token).statusCode());
        assertEquals(204, postarAutenticado("/v1/conta/senha", """
                {"senhaAtual":"Senha-de-teste-segura-123","novaSenha":"Nova-senha-segura-456"}
                """, csrf, token).statusCode());
        assertEquals(401, postarComCsrf("/v1/sessoes", """
                {"email":"%s","senha":"Senha-de-teste-segura-123"}
                """.formatted(email), csrf, null).statusCode());
        try (var conexao = fonteDeDados.getConnection(); var comando = conexao.prepareStatement(
                "SELECT count(*) FROM sessoes_autenticacao WHERE codigo_objeto_contas=? AND revogada_em IS NULL")) {
            comando.setObject(1, idConta(email));
            try (var linhas = comando.executeQuery()) { linhas.next(); assertEquals(0, linhas.getInt(1)); }
        }
        assertEquals(200, postarComCsrf("/v1/sessoes", """
                {"email":"%s","senha":"Nova-senha-segura-456"}
                """.formatted(email), csrf, null).statusCode());
    }

    private void cadastrarAluno(String email, ProtecaoCsrf csrf) throws Exception {
        assertEquals(201, postarComCsrf(
                "/v1/contas/alunos",
                """
                {"nome":"Perfil Teste","email":"%s","cpf":"%s","telefone":"81999990002","endereco":"Rua de teste, 22","senha":"Senha-de-teste-segura-123"}
                """.formatted(email, cpfTeste()),
                csrf,
                null).statusCode());
    }

    private void cadastrarInstituicao(String email, ProtecaoCsrf csrf) throws Exception {
        assertEquals(201, postarComCsrf(
                "/v1/contas/instituicoes",
                """
                {"nomeResponsavel":"Responsável de Teste","nomeInstituicao":"Instituicao de Teste","email":"%s","senha":"Senha-de-teste-segura-123","cnpj":"%s","telefone":"8133334445","endereco":"Rua de teste, 23"}
                """.formatted(email, cnpjTeste()),
                csrf,
                null).statusCode());
    }

    private UUID criarContratoInstitucional(String emailInstituicao, int quantidadeVagas, String codigoPlano)
            throws Exception {
        UUID idPlano = UUID.randomUUID();
        UUID idContrato = UUID.randomUUID();
        try (Connection conexao = fonteDeDados.getConnection();
                var plano = conexao.prepareStatement(
                        "INSERT INTO planos (objeto, codigo, modalidade, nome, preco_centavos) "
                                + "VALUES (?, ?, 'INSTITUCIONAL', 'Plano institucional teste', 0)");
                var contrato = conexao.prepareStatement(
                        "INSERT INTO contratos "
                                + "(objeto, codigo_objeto_contas, codigo_objeto_planos, situacao, quantidade_vagas, "
                                + "preco_centavos_contratado, iniciado_em) "
                                + "VALUES (?, (SELECT objeto FROM contas WHERE email_normalizado = ?), ?, "
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
                        "SELECT objeto FROM contas WHERE email_normalizado = ?")) {
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

    private static String cpfTeste() {
        return String.format("%011d", Long.remainderUnsigned(UUID.randomUUID().getLeastSignificantBits(), 100_000_000_000L));
    }

    private static String cnpjTeste() {
        return String.format("%014d", Long.remainderUnsigned(UUID.randomUUID().getLeastSignificantBits(), 100_000_000_000_000L));
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
                {"email":"%s","senha":"Senha-de-teste-segura-123"}
                """.formatted(email),
                csrf,
                null);
        assertEquals(200, login.statusCode());
        return json().readTree(login.body()).get("accessToken").asString();
    }

    private HttpResponse<String> postarAutenticado(
            String caminho, String corpo, ProtecaoCsrf csrf, String accessToken) throws Exception {
        return postarAutenticado(caminho, corpo, csrf, accessToken, null);
    }

    private HttpResponse<String> postarAutenticado(
            String caminho, String corpo, ProtecaoCsrf csrf, String accessToken, String chaveIdempotencia)
            throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api" + caminho))
                .header("Content-Type", "application/json")
                .header("Cookie", csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Authorization", "Bearer " + accessToken);
        if (chaveIdempotencia != null) {
            requisicao.header("Idempotency-Key", chaveIdempotencia);
        }
        return cliente.send(
                requisicao.POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),
                HttpResponse.BodyHandlers.ofString());
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
        String token = json().readTree(resposta.body()).get("token").asString();
        assertTrue(Arrays.stream(cookieCsrf.split(";"))
                .map(valor -> valor.trim())
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

        HttpResponse<String> vercel = obter("/v1/saude", "Origin", "https://ivis-ia.vercel.app");
        assertEquals("https://ivis-ia.vercel.app",
                vercel.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

        HttpResponse<String> previewVercel = obter(
                "/v1/saude", "Origin", "https://ivis-b4s65t63k-lucifer-son.vercel.app");
        assertEquals("https://ivis-b4s65t63k-lucifer-son.vercel.app",
                previewVercel.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

        HttpResponse<String> negada = obter("/v1/saude", "Origin", "http://origem-nao-configurada.example");
        assertFalse(negada.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }
}
