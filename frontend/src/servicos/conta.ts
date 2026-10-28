import { clienteHttp } from "./clienteHttp";
import { obterTokenCsrf } from "./perfilAluno";

export interface MinhaConta {
  id: string;
  tipoConta: "ALUNO" | "INSTITUICAO";
  email: string;
  nome: string;
  nomeAdministrador?: string | null;
}

export async function atualizarMinhaConta(dados: {
  nome?: string;
  nomeAdministrador?: string;
  razaoSocial?: string;
  email?: string;
}): Promise<MinhaConta> {
  const csrf = await obterTokenCsrf();
  const resposta = await clienteHttp.patch<MinhaConta>("/v1/conta", dados, {
    withCredentials: true,
    headers: { "X-XSRF-TOKEN": csrf },
  });
  return resposta.data;
}
