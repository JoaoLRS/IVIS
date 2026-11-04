import { test } from "node:test";
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

test("limpeza seletiva remove dependências e preserva outras contas; execução reverte os fixtures", () => {
  const limpeza = readFileSync(new URL("./limpar-dados-cypress.sql", import.meta.url), "utf8");
  const sql = `BEGIN;
    CREATE TEMP TABLE fixtures_limpeza ON COMMIT DROP AS
      SELECT gen_random_uuid() alvo, gen_random_uuid() preservado;
    INSERT INTO contas (objeto,tipo_conta,email,email_normalizado,hash_senha)
      SELECT alvo,'ALUNO',alvo::text || '@limpeza.test',alvo::text || '@limpeza.test','fixture' FROM fixtures_limpeza
      UNION ALL SELECT preservado,'ALUNO',preservado::text || '@limpeza.test',preservado::text || '@limpeza.test','fixture' FROM fixtures_limpeza;
    INSERT INTO perfis_aluno (objeto,codigo_objeto_contas,nome)
      SELECT alvo,alvo,'Fixture alvo' FROM fixtures_limpeza
      UNION ALL SELECT preservado,preservado,'Fixture preservado' FROM fixtures_limpeza;
    INSERT INTO contratos (codigo_objeto_contas,codigo_objeto_planos,situacao,preco_centavos_contratado,iniciado_em)
      SELECT c.objeto,p.objeto,'ATIVO',p.preco_centavos,now() FROM contas c CROSS JOIN planos p
      WHERE c.objeto IN (SELECT alvo FROM fixtures_limpeza UNION ALL SELECT preservado FROM fixtures_limpeza)
        AND p.codigo='IVIS-DEMO-INDIVIDUAL';
    INSERT INTO eventos_auditoria (codigo_objeto_contas,acao,resultado,tipo_recurso,codigo_objeto_contratos)
      SELECT ct.codigo_objeto_contas,'TESTE_LIMPEZA','SUCESSO','CONTRATO',ct.objeto FROM contratos ct
      WHERE ct.codigo_objeto_contas IN (SELECT alvo FROM fixtures_limpeza UNION ALL SELECT preservado FROM fixtures_limpeza);
    CREATE TEMP TABLE contas_cypress ON COMMIT DROP AS SELECT alvo AS objeto FROM fixtures_limpeza;
    ${limpeza}
    DO $$ BEGIN
      IF EXISTS (SELECT 1 FROM contas WHERE objeto=(SELECT alvo FROM fixtures_limpeza))
        OR EXISTS (SELECT 1 FROM contratos WHERE codigo_objeto_contas=(SELECT alvo FROM fixtures_limpeza))
        OR EXISTS (SELECT 1 FROM eventos_auditoria WHERE codigo_objeto_contas=(SELECT alvo FROM fixtures_limpeza)) THEN
        RAISE EXCEPTION 'Dados alvo não foram removidos';
      END IF;
      IF (SELECT count(*) FROM contas WHERE objeto=(SELECT preservado FROM fixtures_limpeza)) <> 1
        OR (SELECT count(*) FROM perfis_aluno WHERE objeto=(SELECT preservado FROM fixtures_limpeza)) <> 1
        OR (SELECT count(*) FROM contratos WHERE codigo_objeto_contas=(SELECT preservado FROM fixtures_limpeza)) <> 1
        OR (SELECT count(*) FROM eventos_auditoria WHERE codigo_objeto_contas=(SELECT preservado FROM fixtures_limpeza)) <> 1 THEN
        RAISE EXCEPTION 'Dados de outra conta foram afetados';
      END IF;
    END $$;
    -- Executar novamente também deve funcionar quando o alvo já foi removido.
    ${limpeza}
    ROLLBACK;`;
  execFileSync("docker", ["compose", "exec", "-T", "ivis-banco", "sh", "-lc",
    'PGPASSWORD="$POSTGRES_PASSWORD" psql --quiet --set ON_ERROR_STOP=1 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"'],
  { input: sql, encoding: "utf8", timeout: 30000, cwd: fileURLToPath(new URL("../../", import.meta.url)) });
});
