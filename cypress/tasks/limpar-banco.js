import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";

const arquivoCompose = [
  resolve(process.cwd(), "docker-compose.yml"),
  resolve(process.cwd(), "../../docker-compose.yml"),
].find(existsSync);

const tabelasTransacionais = `
  public.emails_simulados,
  public.eventos_auditoria,
  public.pagamentos,
  public.vinculos_institucionais,
  public.contratos,
  public.sessoes_autenticacao,
  public.tokens_recuperacao_senha,
  public.itens_feedback_treinamento,
  public.avaliacoes_treinamento,
  public.respostas_entrevista,
  public.perguntas_entrevista,
  public.treinamentos,
  public.requisitos_vaga,
  public.vagas_interesse,
  public.habilidades,
  public.perfis_instituicao,
  public.perfis_aluno,
  public.contas`;

export function limparBancoDeDados() {
  validarAmbienteDeTeste();
  if (!arquivoCompose) {
    throw new Error("Não foi possível localizar docker-compose.yml para limpar o banco de testes.");
  }

  executarPsql(`DO $$ BEGIN
    IF current_database() <> 'ivis' THEN
      RAISE EXCEPTION 'A limpeza do Cypress só pode ser executada no banco local ivis.';
    END IF;
  END $$;
  TRUNCATE TABLE ${tabelasTransacionais} RESTART IDENTITY CASCADE;\n`);
  return null;
}

export function consultarAuditoriaPorContrato(contratoId) {
  validarAmbienteDeTeste();
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(contratoId ?? "")) {
    throw new Error("Identificador de contrato inválido para consulta de auditoria.");
  }

  const resultado = executarPsql(`DO $$ BEGIN
    IF current_database() <> 'ivis' THEN
      RAISE EXCEPTION 'A consulta do Cypress só pode ser executada no banco local ivis.';
    END IF;
  END $$;
  SELECT COALESCE(json_agg(json_build_object(
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

function validarAmbienteDeTeste() {
  if (process.env.IVIS_CYPRESS_LIMPAR_BANCO !== "1") {
    throw new Error(
      "Acesse o banco somente após confirmar que o Compose usa um banco local descartável; " +
        "defina IVIS_CYPRESS_LIMPAR_BANCO=1 para autorizar esta suíte.",
    );
  }
}

function executarPsql(sql) {
  if (!arquivoCompose) {
    throw new Error("Não foi possível localizar docker-compose.yml para acessar o banco de testes.");
  }

  const comandoPsql =
    'PGPASSWORD="$POSTGRES_PASSWORD" psql --set ON_ERROR_STOP=1 --tuples-only --no-align --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"';
  return execFileSync(
    "docker",
    [
      "compose",
      "--file",
      arquivoCompose,
      "exec",
      "--no-TTY",
      "ivis-banco",
      "sh",
      "-lc",
      comandoPsql,
    ],
    {
      cwd: dirname(arquivoCompose),
      input: sql,
      encoding: "utf8",
      timeout: 30000,
    },
  );
}
