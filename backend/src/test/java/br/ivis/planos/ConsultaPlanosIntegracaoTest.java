package br.ivis.planos;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ConsultaPlanosIntegracaoTest {
    @Container
    static final PostgreSQLContainer banco = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", banco::getJdbcUrl);
        registro.add("spring.datasource.username", banco::getUsername);
        registro.add("spring.datasource.password", banco::getPassword);
        registro.add("ivis.jwt.chave-assinatura", () -> Base64.getEncoder().encodeToString(new byte[32]));
        registro.add("ivis.ia.chave-servico", () -> "chave-sintetica-interna-com-mais-de-32-caracteres");
    }

    @Autowired JdbcTemplate bancoDados;
    @Autowired JwtEncoder codificador;
    @Autowired ObjectMapper json;
    @Value("${local.server.port}") int porta;

    @Test
    void catalogoPublicoFiltraInativosEOrdenaBeneficios() throws Exception {
        UUID ativo = plano("INDIVIDUAL", true);
        UUID inativo = plano("INDIVIDUAL", false);
        bancoDados.update("insert into beneficios_plano (plano_id, texto_beneficio, ordem) values (?, 'Segundo', 2), (?, 'Primeiro', 1)", ativo, ativo);
        var resposta = obter("/planos/" + ativo, null);
        assertEquals(200, resposta.statusCode());
        var oferta = json.readTree(resposta.body());
        assertEquals(1990, oferta.get("precoCentavos").asLong());
        assertEquals("Primeiro", oferta.get("beneficios").get(0).asString());
        assertEquals("Segundo", oferta.get("beneficios").get(1).asString());
        assertEquals(404, obter("/planos/" + inativo, null).statusCode());
        assertEquals(404, obter("/planos/" + UUID.randomUUID(), null).statusCode());
        var lista = obter("/planos", null);
        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains(ativo.toString()));
        assertFalse(lista.body().contains(inativo.toString()));
        assertEquals(401, obter("/conta/plano", null).statusCode());
    }

    @Test
    void planoProprioUsaPrincipalEPrecoContratadoMesmoComOfertaInativa() throws Exception {
        UUID aluno = conta("ALUNO");
        UUID outro = conta("ALUNO");
        UUID plano = plano("INDIVIDUAL", false);
        UUID contrato = contrato(aluno, plano, "ATIVO");
        contrato(aluno, plano, "PENDENTE");
        var resposta = obter("/conta/plano?contaId=" + outro, aluno);
        assertEquals(200, resposta.statusCode());
        var planos = json.readTree(resposta.body()).get("planos");
        assertEquals(1, planos.size());
        assertEquals(contrato.toString(), planos.get(0).get("contratoId").asString());
        assertEquals(1500, planos.get(0).get("condicoesTitular").get("precoCentavosContratado").asLong());
        assertEquals("SEM_PLANO_VIGENTE", json.readTree(obter("/conta/plano", outro).body()).get("situacao").asString());
        bancoDados.update("update contas set situacao = 'INATIVA' where id = ?", aluno);
        assertEquals(403, obter("/conta/plano", aluno).statusCode());
    }

    @Test
    void vinculoAtivoConcedeConsultaSemExporCondicoesDaInstituicao() throws Exception {
        UUID instituicao = conta("INSTITUICAO");
        UUID outraInstituicao = conta("INSTITUICAO");
        UUID aluno = conta("ALUNO");
        UUID contrato = contrato(instituicao, plano("INSTITUCIONAL", true), "ATIVO");
        bancoDados.update("""
                insert into vinculos_institucionais (contrato_id, conta_aluno_id, criado_por_conta_id)
                values (?, ?, ?)
                """, contrato, aluno, instituicao);
        var planoAluno = json.readTree(obter("/conta/plano", aluno).body()).get("planos").get(0);
        assertEquals("VINCULO_INSTITUCIONAL", planoAluno.get("origemAcesso").asString());
        assertTrue(planoAluno.get("condicoesTitular").isNull());
        assertEquals(0, json.readTree(obter("/conta/plano", outraInstituicao).body()).get("planos").size());
        assertEquals(1, json.readTree(obter("/conta/plano", instituicao).body()).get("planos").size());
        bancoDados.update("update contratos set situacao = 'SUSPENSO' where id = ?", contrato);
        assertEquals(0, json.readTree(obter("/conta/plano", aluno).body()).get("planos").size());
        bancoDados.update("update contratos set situacao = 'ATIVO' where id = ?", contrato);
        bancoDados.update("update vinculos_institucionais set situacao = 'ENCERRADO', desvinculado_em = now() where contrato_id = ?", contrato);
        assertEquals(0, json.readTree(obter("/conta/plano", aluno).body()).get("planos").size());
    }

    private UUID plano(String modalidade, boolean ativo) {
        UUID id = UUID.randomUUID();
        bancoDados.update("insert into planos (id, codigo, modalidade, nome, preco_centavos, ativo) values (?, ?, ?, 'Oferta sintética', 1990, ?)",
                id, id.toString(), modalidade, ativo);
        return id;
    }

    private UUID conta(String tipo) {
        UUID id = UUID.randomUUID();
        String email = id + "@example.test";
        bancoDados.update("insert into contas (id, tipo_conta, email, email_normalizado, hash_senha) values (?, ?, ?, ?, 'nao-utilizado')", id, tipo, email, email);
        if (tipo.equals("ALUNO")) {
            bancoDados.update("insert into perfis_aluno (conta_id, nome_exibicao) values (?, 'Aluno sintético')", id);
        } else {
            bancoDados.update("insert into perfis_instituicao (conta_id, razao_social) values (?, 'Instituição sintética')", id);
        }
        return id;
    }

    private UUID contrato(UUID conta, UUID plano, String situacao) {
        UUID id = UUID.randomUUID();
        bancoDados.update("""
                insert into contratos (id, conta_titular_id, plano_id, situacao, quantidade_vagas,
                    preco_centavos_contratado, iniciado_em) values (?, ?, ?, ?, 1, 1500, now())
                """, id, conta, plano, situacao);
        return id;
    }

    private HttpResponse<String> obter(String caminho, UUID conta) throws Exception {
        var pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1" + caminho));
        if (conta != null) {
            var declaracoes = JwtClaimsSet.builder().issuer("ivis").subject(conta.toString())
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
            String token = codificador.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), declaracoes)).getTokenValue();
            pedido.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(pedido.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
