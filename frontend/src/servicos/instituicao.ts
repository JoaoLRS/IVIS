import { clienteHttp } from "./clienteHttp";

export interface VinculoInstitucional {
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

export interface CriacaoVinculoInstitucional {
  contratoId: string;
  contaAlunoId: string;
}

export interface CriacaoVinculoInstitucionalResposta {
  vinculo: VinculoInstitucional;
  capacidade: CapacidadeInstitucional;
}

export async function listarAlunosVinculados(accessToken: string): Promise<VinculoInstitucional[]> {
  const resposta = await clienteHttp.get<VinculoInstitucional[]>("/v1/instituicao/alunos", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return resposta.data;
}

export async function consultarCapacidadeInstitucional(
  accessToken: string,
): Promise<CapacidadeInstitucional[]> {
  const resposta = await clienteHttp.get<CapacidadeInstitucional[]>(
    "/v1/instituicao/resumo-vagas",
    { headers: { Authorization: `Bearer ${accessToken}` } },
  );
  return resposta.data;
}

export async function criarVinculoInstitucional(
  accessToken: string,
  csrfToken: string,
  requisicao: CriacaoVinculoInstitucional,
): Promise<CriacaoVinculoInstitucionalResposta> {
  const resposta = await clienteHttp.post<CriacaoVinculoInstitucionalResposta>(
    "/v1/instituicao/vinculos",
    requisicao,
    {
      withCredentials: true,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "X-XSRF-TOKEN": csrfToken,
      },
    },
  );
  return resposta.data;
}

export async function removerVinculoInstitucional(
  accessToken: string,
  csrfToken: string,
  vinculoId: string,
): Promise<void> {
  await clienteHttp.delete(`/v1/instituicao/vinculos/${encodeURIComponent(vinculoId)}`, {
    withCredentials: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
}
