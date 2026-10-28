import { clienteHttp } from "./clienteHttp";
import { obterTokenCsrf } from "./perfilAluno";

export interface TreinoPersistido {
  id: string; titulo: string; contexto: string; nota: number; data: string;
  pontosFortes: string[]; pontosMelhoria: string[];
}
export async function listarHistorico(): Promise<TreinoPersistido[]> {
  return (await clienteHttp.get<TreinoPersistido[]>("/v1/treinos")).data;
}
export async function salvarTreino(entrada: object): Promise<TreinoPersistido> {
  const csrf = await obterTokenCsrf();
  return (await clienteHttp.post<TreinoPersistido>("/v1/treinos/concluidos", entrada, { withCredentials: true, headers: { "X-XSRF-TOKEN": csrf } })).data;
}
