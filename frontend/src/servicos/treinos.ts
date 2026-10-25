import { clienteHttp } from "./clienteHttp";

export interface HabilidadeIa {
  nome: string;
  nivel: number;
}

export interface VagaIa {
  titulo: string;
  senioridade: string;
  requisitos: string[];
}

export interface InteracaoIa {
  numero: number;
  pergunta: string;
  resposta: string;
}

export interface ContextoTreinoIa {
  tipo_contexto: "HABILIDADES" | "VAGA";
  habilidades: HabilidadeIa[] | null;
  vaga: VagaIa | null;
  interacoes: InteracaoIa[];
}

export interface GerarPerguntaResposta {
  pergunta: { texto: string };
  versao_modelo: string;
  versao_prompt: string;
}

export interface GerarAvaliacaoResposta {
  nota: number;
  pontos_fortes: string[];
  pontos_melhoria: string[];
  versao_modelo: string;
  versao_prompt: string;
}

export async function gerarPergunta(
  contexto: ContextoTreinoIa,
  numeroPergunta: number,
  totalPerguntas: number,
): Promise<GerarPerguntaResposta> {
  const resposta = await clienteHttp.post<GerarPerguntaResposta>(
    "/v1/ia/perguntas/gerar",
    {
      ...contexto,
      numero_pergunta: numeroPergunta,
      total_perguntas: totalPerguntas,
    },
    { timeout: 15_000 },
  );
  return resposta.data;
}

export async function gerarAvaliacao(
  contexto: ContextoTreinoIa,
): Promise<GerarAvaliacaoResposta> {
  const resposta = await clienteHttp.post<GerarAvaliacaoResposta>(
    "/v1/ia/avaliacoes/gerar",
    contexto,
    { timeout: 25_000 },
  );
  return resposta.data;
}
