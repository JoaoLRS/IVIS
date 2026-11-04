import { clienteHttp } from "./clienteHttp";

export interface TreinoPersistido {
  id: string; titulo: string; contexto: string; nota: number; data: string;
  pontosFortes: string[]; pontosMelhoria: string[];
}
export async function listarHistorico(): Promise<TreinoPersistido[]> {
  return (await clienteHttp.get<TreinoPersistido[]>("/v1/treinos")).data;
}
