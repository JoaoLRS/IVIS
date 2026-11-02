import { clienteHttp } from "./clienteHttp";
import { obterTokenCsrf } from "./perfilAluno";

export interface MinhaConta {
  id: string;
  tipoConta: "ALUNO" | "INSTITUICAO";
  email: string;
  nome: string;
  nomeResponsavel?: string | null;
  cpf?: string | null;
  cnpj?: string | null;
  telefone?: string | null;
  endereco?: string | null;
  objetoInstituicao?: string | null;
}

export async function atualizarMinhaConta(dados: {
  nome?: string;
  nomeResponsavel?: string;
  nomeInstituicao?: string;
  email?: string;
  cpf?: string;
  cnpj?: string;
  telefone?: string;
  endereco?: string;
}): Promise<MinhaConta> {
  const csrf = await obterTokenCsrf();
  const resposta = await clienteHttp.patch<MinhaConta>("/v1/conta", dados, {
    withCredentials: true,
    headers: { "X-XSRF-TOKEN": csrf },
  });
  return resposta.data;
}
