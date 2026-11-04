package br.ivis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import br.ivis.common.development.LimpezaDadosController;
import br.ivis.common.development.LimpezaDadosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Testcontainers
class LimpezaDadosIntegracaoTest {
    private static final String CHAVE = "limpeza-teste-chave-longa-e-aleatoria";

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", postgres::getJdbcUrl);
        registro.add("spring.datasource.username", postgres::getUsername);
        registro.add("spring.datasource.password", postgres::getPassword);
        registro.add("IVIS_LIMPEZA_CHAVE", () -> CHAVE);
        registro.add("ivis.ia.chave-servico", () -> "chave-ia-integracao-com-mais-de-32-caracteres");
        registro.add("ivis.jwt.chave-assinatura",
                () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    JdbcTemplate banco;

    @LocalServerPort
    int porta;

    @Test
    void perfilProdDesabilitaLimpezaMesmoComDevAtivo() {
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.getEnvironment().setActiveProfiles("dev", "prod");
            contexto.register(LimpezaDadosController.class, LimpezaDadosService.class);
            contexto.refresh();
            assertFalse(contexto.containsBeanDefinition("limpezaDadosController"));
            assertFalse(contexto.containsBeanDefinition("limpezaDadosService"));
        }
    }

    @Test
    void limpaSomenteContaIdentificadaESeusDadosLigados() throws Exception {
        UUID aluno = aluno("12345678931", "limpeza-aluno@exemplo.test");
        UUID outro = aluno("12345678932", "limpeza-outro@exemplo.test");
        UUID contrato = contrato(aluno);
        UUID pagamento = UUID.randomUUID();
        UUID vaga = UUID.randomUUID();
        UUID treino = UUID.randomUUID();
        UUID pergunta = UUID.randomUUID();
        UUID avaliacao = UUID.randomUUID();

        banco.update("insert into habilidades (codigo_objeto_perfis_aluno,nome,nome_normalizado,nivel) values (?,'Java','java',3)", aluno);
        banco.update("insert into vagas_interesse (objeto,codigo_objeto_perfis_aluno,titulo,senioridade) values (?,?,'Backend','Pleno')", vaga, aluno);
        banco.update("insert into requisitos_vaga (codigo_objeto_vagas_interesse,texto_requisito,ordem) values (?,'Java',1)", vaga);
        banco.update("insert into treinamentos (objeto,codigo_objeto_perfis_aluno,tipo_contexto,situacao,contexto_registrado) values (?,?,'HABILIDADES','CONCLUIDO','{}'::jsonb)", treino, aluno);
        banco.update("insert into perguntas_entrevista (objeto,codigo_objeto_treinamentos,numero_sequencial,texto_pergunta,versao_modelo,versao_prompt) values (?,?,1,'Pergunta','teste','v1')", pergunta, treino);
        banco.update("insert into respostas_entrevista (codigo_objeto_perguntas_entrevista,texto_resposta) values (?,'Resposta')", pergunta);
        banco.update("insert into avaliacoes_treinamento (objeto,codigo_objeto_treinamentos,nota,versao_modelo,versao_prompt) values (?,?,80,'teste','v1')", avaliacao, treino);
        banco.update("insert into itens_feedback_treinamento (codigo_objeto_avaliacoes_treinamento,tipo,ordem,texto) values (?,'PONTO_FORTE',1,'Bom')", avaliacao);
        banco.update("insert into pagamentos (objeto,codigo_objeto_contratos,modo,situacao,valor_centavos,chave_idempotencia) values (?,?,'SIMULADO','APROVADO',100,?)", pagamento, contrato, UUID.randomUUID().toString());
        banco.update("insert into emails_simulados (codigo_objeto_contas,codigo_objeto_contratos,codigo_objeto_pagamentos,destinatario,assunto,corpo) values (?,?,?,'limpeza-aluno@exemplo.test','Teste','Mensagem')", aluno, contrato, pagamento);
        banco.update("insert into eventos_auditoria (codigo_objeto_contas,acao,resultado,tipo_recurso,codigo_objeto_contratos) values (?,'TESTE','SUCESSO','CONTRATO',?)", aluno, contrato);
        banco.update("insert into sessoes_autenticacao (codigo_objeto_contas,hash_token_renovacao,expira_em) values (?,?,now()+interval '1 day')", aluno, UUID.randomUUID().toString());
        banco.update("insert into tokens_recuperacao_senha (codigo_objeto_contas,hash_token,expira_em) values (?,?,now()+interval '1 day')", aluno, UUID.randomUUID().toString());

        assertEquals(403, limpar("123.456.789-31", "chave-incorreta").statusCode());
        assertEquals(1, contar("contas", "objeto", aluno));
        assertEquals(204, limpar("123.456.789-31", CHAVE).statusCode());
        assertEquals(0, contar("contas", "objeto", aluno));
        assertEquals(0, contar("perfis_aluno", "objeto", aluno));
        assertEquals(0, contar("habilidades", "codigo_objeto_perfis_aluno", aluno));
        assertEquals(0, contar("vagas_interesse", "objeto", vaga));
        assertEquals(0, contar("requisitos_vaga", "codigo_objeto_vagas_interesse", vaga));
        assertEquals(0, contar("treinamentos", "objeto", treino));
        assertEquals(0, contar("perguntas_entrevista", "objeto", pergunta));
        assertEquals(0, contar("respostas_entrevista", "codigo_objeto_perguntas_entrevista", pergunta));
        assertEquals(0, contar("avaliacoes_treinamento", "objeto", avaliacao));
        assertEquals(0, contar("itens_feedback_treinamento", "codigo_objeto_avaliacoes_treinamento", avaliacao));
        assertEquals(0, contar("contratos", "objeto", contrato));
        assertEquals(0, contar("pagamentos", "objeto", pagamento));
        assertEquals(0, contar("emails_simulados", "codigo_objeto_contas", aluno));
        assertEquals(0, contar("eventos_auditoria", "codigo_objeto_contas", aluno));
        assertEquals(0, contar("sessoes_autenticacao", "codigo_objeto_contas", aluno));
        assertEquals(0, contar("tokens_recuperacao_senha", "codigo_objeto_contas", aluno));
        assertEquals(1, contar("contas", "objeto", outro));
        assertEquals(404, limpar("12345678931", CHAVE).statusCode());
        assertEquals(400, limpar("123", CHAVE).statusCode());
    }

    @Test
    void limpaInstituicaoSemApagarAlunosVinculados() throws Exception {
        UUID instituicao = UUID.randomUUID();
        banco.update("insert into contas (objeto,tipo_conta,email,email_normalizado,hash_senha) values (?,'INSTITUICAO','limpeza-inst@exemplo.test','limpeza-inst@exemplo.test','hash')", instituicao);
        banco.update("insert into perfis_instituicao (objeto,codigo_objeto_contas,nome_instituicao,nome_responsavel,cnpj) values (?,?,'Instituto','Responsavel','12345678000195')", instituicao, instituicao);
        UUID aluno = aluno("12345678933", "vinculado@exemplo.test");
        banco.update("update perfis_aluno set codigo_objeto_perfis_instituicao=? where objeto=?", instituicao, aluno);
        UUID contrato = contrato(instituicao);
        banco.update("insert into vinculos_institucionais (codigo_objeto_contratos,codigo_objeto_perfis_aluno,codigo_objeto_contas) values (?,?,?)", contrato, aluno, instituicao);
        banco.update("insert into eventos_auditoria (codigo_objeto_contas,acao,resultado,tipo_recurso,codigo_objeto_contratos,detalhes) values (?,'TESTE','SUCESSO','CONTRATO',?,jsonb_build_object('alunoId',?::text))", instituicao, contrato, aluno);

        assertEquals(204, limpar("12.345.678/0001-95", CHAVE).statusCode());
        assertEquals(0, contar("contas", "objeto", instituicao));
        assertEquals(0, contar("perfis_instituicao", "objeto", instituicao));
        assertEquals(0, contar("contratos", "objeto", contrato));
        assertEquals(0, contar("vinculos_institucionais", "codigo_objeto_contratos", contrato));
        assertEquals(0, contar("eventos_auditoria", "codigo_objeto_contratos", contrato));
        assertEquals(1, contar("contas", "objeto", aluno));
        assertEquals(null, banco.queryForObject(
                "select codigo_objeto_perfis_instituicao from perfis_aluno where objeto=?", UUID.class, aluno));
    }

    private UUID aluno(String cpf, String email) {
        UUID id = UUID.randomUUID();
        banco.update("insert into contas (objeto,tipo_conta,email,email_normalizado,hash_senha) values (?,'ALUNO',?,?,'hash')", id, email, email);
        banco.update("insert into perfis_aluno (objeto,codigo_objeto_contas,nome,cpf) values (?,?,'Aluno',?)", id, id, cpf);
        return id;
    }

    private UUID contrato(UUID contaId) {
        UUID id = UUID.randomUUID();
        banco.update("""
                insert into contratos (objeto,codigo_objeto_contas,codigo_objeto_planos,situacao,
                  preco_centavos_contratado,iniciado_em)
                select ?,?,objeto,'ATIVO',preco_centavos,now()
                from planos where codigo='IVIS-DEMO-INDIVIDUAL'
                """, id, contaId);
        return id;
    }

    private int contar(String tabela, String coluna, UUID id) {
        return banco.queryForObject("select count(*) from " + tabela + " where " + coluna + "=?", Integer.class, id);
    }

    private HttpResponse<String> limpar(String documento, String chave) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + "/api/v1/ambiente/limpar-dados"))
                .header("Content-Type", "application/json")
                .header("X-IVIS-Limpeza-Chave", chave)
                .POST(HttpRequest.BodyPublishers.ofString("{\"documento\":\"" + documento + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
