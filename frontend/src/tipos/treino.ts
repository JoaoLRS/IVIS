export interface CriterioAvaliacaoTreino {
  criterio: "CORRECAO_TECNICA" | "COMPLETUDE" | "CLAREZA";
  nota: number;
  justificativa: string;
  trechoResposta: string;
}

export interface AvaliacaoPerguntaTreino {
  numero: number;
  pergunta: string;
  criterios: CriterioAvaliacaoTreino[];
  nivelConfianca: "BAIXA" | "MEDIA" | "ALTA";
  limitacao: string;
}

export interface Treino {
  id?: string;
  titulo: string;
  contexto: string;
  data: string;
  nota: number;
  duracao: string;
  pontosFortes?: string;
  melhorias?: string;
  avaliacaoQualitativa?: boolean;
  nivelConfianca?: "BAIXA" | "MEDIA" | "ALTA";
  limitacoes?: string;
  avaliacoesPorPergunta?: AvaliacaoPerguntaTreino[];
}
