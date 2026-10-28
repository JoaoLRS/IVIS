import { clienteHttp } from "./clienteHttp";

export interface AlunoVinculado {
  id: string;
  contratoId: string;
  contaAlunoId: string;
  nomeAluno: string;
  emailAluno: string;
  vinculadoEm: string;
}

export interface CapacidadeInstitucional {
  contratoId: string;
  quantidadeVagas: number;
  vagasUtilizadas: number;
  vagasDisponiveis: number;
}

export async function listarAlunosInstituicao(): Promise<AlunoVinculado[]> {
  const resposta = await clienteHttp.get<AlunoVinculado[]>("/v1/instituicao/alunos");
  return resposta.data;
}

export async function listarCapacidadeInstituicao(): Promise<CapacidadeInstitucional[]> {
  const resposta = await clienteHttp.get<CapacidadeInstitucional[]>("/v1/instituicao/resumo-vagas");
  return resposta.data;
}

export async function vincularAluno(emailAluno: string, contratoId: string, csrf: string) {
  const resposta = await clienteHttp.post<{ vinculo: AlunoVinculado; capacidade: CapacidadeInstitucional }>(
    "/v1/instituicao/vinculos/por-email",
    { emailAluno, contratoId },
    { withCredentials: true, headers: { "X-XSRF-TOKEN": csrf } },
  );
  return resposta.data;
}

export async function removerAlunoVinculado(id: string, csrf: string) {
  await clienteHttp.delete(`/v1/instituicao/vinculos/${id}`, {
    withCredentials: true,
    headers: { "X-XSRF-TOKEN": csrf },
  });
}
