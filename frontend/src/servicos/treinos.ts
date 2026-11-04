import { clienteHttp } from "./clienteHttp";
import { obterTokenCsrf } from "./perfilAluno";
import type { TreinoPersistido } from "./historico";

export interface PerguntaTreino {
  id: string;
  numero: number;
  texto: string;
  resposta: string | null;
}

export interface SessaoTreino {
  id: string;
  tipoContexto: "HABILIDADES" | "VAGA";
  contexto: string;
  vagaId: string | null;
  habilidades: string[];
  perguntas: PerguntaTreino[];
  totalPerguntas: number;
}

async function configuracaoEscrita(timeout = 105_000) {
  const csrf = await obterTokenCsrf();
  return {
    withCredentials: true,
    timeout,
    headers: { "X-XSRF-TOKEN": csrf },
  };
}

export async function consultarTreinoEmAndamento(): Promise<SessaoTreino | null> {
  const resposta = await clienteHttp.get<SessaoTreino>("/v1/treinos/em-andamento");
  return resposta.status === 204 ? null : resposta.data;
}

export async function iniciarTreinoRemoto(entrada: {
  tipoContexto: "HABILIDADES" | "VAGA";
  vagaId: string | null;
  habilidades: string[];
}): Promise<SessaoTreino> {
  return (await clienteHttp.post<SessaoTreino>(
    "/v1/treinos", entrada, await configuracaoEscrita(),
  )).data;
}

export async function enviarRespostaTreino(
  treinoId: string, perguntaId: string, resposta: string,
): Promise<SessaoTreino> {
  return (await clienteHttp.post<SessaoTreino>(
    `/v1/treinos/${treinoId}/respostas`,
    { perguntaId, resposta },
    await configuracaoEscrita(),
  )).data;
}

export async function concluirTreinoRemoto(treinoId: string): Promise<TreinoPersistido> {
  return (await clienteHttp.post<TreinoPersistido>(
    `/v1/treinos/${treinoId}/concluir`, {}, await configuracaoEscrita(110_000),
  )).data;
}

export async function cancelarTreinoRemoto(treinoId: string): Promise<void> {
  await clienteHttp.delete(`/v1/treinos/${treinoId}`, await configuracaoEscrita());
}
