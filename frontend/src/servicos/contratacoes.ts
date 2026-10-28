import { clienteHttp } from "./clienteHttp";

export interface EmailSimulado {
  destinatario: string;
  assunto: string;
  corpo: string;
  criadoEm: string;
}

export interface ContratacaoSimulada {
  contratoId: string;
  pagamentoId: string;
  planoId: string;
  nomePlano: string;
  situacaoContrato: "ATIVO" | "ENCERRADO";
  situacaoPagamento: "APROVADO" | "RECUSADO";
  modoPagamento: "SIMULADO";
  valorCentavos: number;
  moeda: string;
  expiraEm: string | null;
  reprocessada: boolean;
  emailConfirmacao: EmailSimulado | null;
}

export interface DadosCartaoTeste {
  planoId: string;
  numeroCartao: string;
  validade: string;
  cvv: string;
}

export async function simularContratacao(
  requisicao: DadosCartaoTeste,
  chaveIdempotencia: string,
  csrfToken: string,
): Promise<ContratacaoSimulada> {
  const resposta = await clienteHttp.post<ContratacaoSimulada>(
    "/v1/contratacoes/simuladas",
    requisicao,
    {
      withCredentials: true,
      headers: {
        "Idempotency-Key": chaveIdempotencia,
        "X-XSRF-TOKEN": csrfToken,
      },
    },
  );
  return resposta.data;
}
