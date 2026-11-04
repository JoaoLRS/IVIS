import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { readFileSync } from "node:fs";
import { dadosTeste, alunoAtualizadoCaminhoFeliz } from "../e2e/funcoes/constantes.js";

const raizProjeto = fileURLToPath(new URL("../../", import.meta.url));

export function limparBancoDeDados() {
  const emails = [dadosTeste.aluno.email, alunoAtualizadoCaminhoFeliz.email, dadosTeste.instituicao.email];
  const valores = emails.map((email) => `'${email.replaceAll("'", "''")}'`).join(",");
  const limpeza = readFileSync(new URL("./limpar-dados-cypress.sql", import.meta.url), "utf8");
  executarPsql(`BEGIN;
    CREATE TEMP TABLE contas_cypress ON COMMIT DROP AS
      SELECT objeto FROM contas WHERE email_normalizado IN (${valores});
    ${limpeza}
    COMMIT;`);
  return null;
}

export function consultarAuditoriaPorContrato(contratoId) {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(contratoId ?? "")) {
    throw new Error("Identificador de contrato inválido para consulta de auditoria.");
  }

  const resultado = executarPsql(`SELECT COALESCE(json_agg(json_build_object(
    'atorContaId', codigo_objeto_contas,
    'acao', acao,
    'resultado', resultado,
    'tipoRecurso', tipo_recurso,
    'contratoId', codigo_objeto_contratos,
    'detalhes', detalhes
  )), '[]'::json)::text
  FROM eventos_auditoria
  WHERE codigo_objeto_contratos = '${contratoId}'
    AND acao = 'CONTRATACAO_PAGAMENTO_SIMULADO';\n`);
  return JSON.parse(resultado.trim());
}

function executarPsql(sql) {
  const comandoPsql =
    'PGPASSWORD="$POSTGRES_PASSWORD" psql --quiet --set ON_ERROR_STOP=1 --tuples-only --no-align --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"';
  return execFileSync(
    "docker",
    [
      "compose",
      "exec",
      "-T",
      "ivis-banco",
      "sh",
      "-lc",
      comandoPsql,
    ],
    {
      input: sql,
      cwd: raizProjeto,
      encoding: "utf8",
      timeout: 30000,
    },
  );
}
