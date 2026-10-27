import { clienteHttp } from "./clienteHttp";

export interface HabilidadePerfil {
  id: string;
  nome: string;
  categoria: string | null;
  nivel: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface HabilidadePerfilEntrada {
  nome: string;
  categoria?: string | null;
  nivel: number;
}

export interface VagaInteressePerfil {
  id: string;
  titulo: string;
  empresa: string | null;
  senioridade: string;
  descricao: string | null;
  requisitos: string[];
  criadoEm: string;
  atualizadoEm: string;
}

export interface VagaInteressePerfilEntrada {
  titulo: string;
  empresa?: string | null;
  senioridade: string;
  descricao?: string | null;
  requisitos: string[];
}

export async function obterTokenCsrf(): Promise<string> {
  const resposta = await clienteHttp.get<{ token: string }>("/v1/sessoes/csrf", {
    withCredentials: true,
  });
  return resposta.data.token;
}

export async function listarHabilidades(accessToken: string): Promise<HabilidadePerfil[]> {
  const resposta = await clienteHttp.get<HabilidadePerfil[]>("/v1/habilidades", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return resposta.data;
}

export async function criarHabilidade(
  accessToken: string,
  csrfToken: string,
  habilidade: HabilidadePerfilEntrada,
): Promise<HabilidadePerfil> {
  const resposta = await clienteHttp.post<HabilidadePerfil>("/v1/habilidades", habilidade, {
    withCredentials: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
  return resposta.data;
}

export async function atualizarHabilidade(
  accessToken: string,
  csrfToken: string,
  habilidadeId: string,
  habilidade: HabilidadePerfilEntrada,
): Promise<HabilidadePerfil> {
  const resposta = await clienteHttp.put<HabilidadePerfil>(
    `/v1/habilidades/${encodeURIComponent(habilidadeId)}`,
    habilidade,
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

export async function excluirHabilidade(
  accessToken: string,
  csrfToken: string,
  habilidadeId: string,
): Promise<void> {
  await clienteHttp.delete(`/v1/habilidades/${encodeURIComponent(habilidadeId)}`, {
    withCredentials: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
}

export async function listarVagas(accessToken: string): Promise<VagaInteressePerfil[]> {
  const resposta = await clienteHttp.get<VagaInteressePerfil[]>("/v1/vagas", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return resposta.data;
}

export async function criarVaga(
  accessToken: string,
  csrfToken: string,
  vaga: VagaInteressePerfilEntrada,
): Promise<VagaInteressePerfil> {
  const resposta = await clienteHttp.post<VagaInteressePerfil>("/v1/vagas", vaga, {
    withCredentials: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
  return resposta.data;
}

export async function atualizarVaga(
  accessToken: string,
  csrfToken: string,
  vagaId: string,
  vaga: VagaInteressePerfilEntrada,
): Promise<VagaInteressePerfil> {
  const resposta = await clienteHttp.put<VagaInteressePerfil>(
    `/v1/vagas/${encodeURIComponent(vagaId)}`,
    vaga,
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

export async function excluirVaga(
  accessToken: string,
  csrfToken: string,
  vagaId: string,
): Promise<void> {
  await clienteHttp.delete(`/v1/vagas/${encodeURIComponent(vagaId)}`, {
    withCredentials: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
}
