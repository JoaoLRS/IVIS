import { clienteHttp } from "./clienteHttp";
import { obterTokenCsrf } from "./perfilAluno";
import type { AvaliacaoPergunta } from "./treinos";

export interface TreinoPersistido {
  id: string;
  titulo: string;
  contexto: string;
  nota: number | null;
  data: string;
  pontosFortes: string[];
  pontosMelhoria: string[];
  avaliacaoQualitativa: boolean;
  nivelConfianca: "BAIXA" | "MEDIA" | "ALTA";
  limitacoes: string;
  avaliacoesPorPergunta: AvaliacaoPergunta[];
}
export async function listarHistorico(): Promise<TreinoPersistido[]> {
  return (await clienteHttp.get<TreinoPersistido[]>("/v1/treinos")).data;
}
export async function salvarTreino(entrada: object): Promise<TreinoPersistido> {
  const csrf = await obterTokenCsrf();
  return (
    await clienteHttp.post<TreinoPersistido>(
      "/v1/treinos/concluidos",
      entrada,
      {
        timeout: 120_000,
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": csrf },
      },
    )
  ).data;
}
