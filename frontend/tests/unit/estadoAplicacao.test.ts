import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  listarHistorico,
  salvarTreino,
  type TreinoPersistido,
} from "../../src/servicos/historico";
import { gerarAvaliacao, gerarPergunta } from "../../src/servicos/treinos";
import { useEstadoAplicacao } from "../../src/estados/estadoAplicacao";
import {
  listarHabilidades,
  listarVagas,
} from "../../src/servicos/perfilAluno";

vi.mock("../../src/servicos/historico", () => ({
  listarHistorico: vi.fn(),
  salvarTreino: vi.fn(),
}));
vi.mock("../../src/servicos/perfilAluno", () => ({
  listarHabilidades: vi.fn(),
  listarVagas: vi.fn(),
}));
vi.mock("../../src/servicos/treinos", () => ({
  gerarAvaliacao: vi.fn(),
  gerarPergunta: vi.fn(),
}));

const perguntaGerada = (texto: string) => ({
  pergunta: { texto },
  versao_modelo: "stub-1",
  versao_prompt: "perguntas-1",
});

describe("estado da aplicação", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(gerarPergunta).mockReset();
    vi.mocked(gerarAvaliacao).mockReset();
    vi.mocked(salvarTreino).mockReset();
    vi.mocked(listarHistorico).mockReset();
    vi.mocked(listarHabilidades).mockReset();
    vi.mocked(listarVagas).mockReset();
  });

  it("monta o contexto de IA com a vaga escolhida", async () => {
    const estado = useEstadoAplicacao();
    const vaga = {
      id: "vaga-1",
      titulo: "Desenvolvedora Vue",
      empresa: "Empresa Cypress",
      senioridade: "Pleno",
      descricao: "Desenvolvimento de interfaces",
      requisitos: ["Vue.js", "TypeScript"],
      criadoEm: "2026-11-02T12:00:00Z",
      atualizadoEm: "2026-11-02T12:00:00Z",
    };
    vi.mocked(gerarPergunta).mockResolvedValue(perguntaGerada("Como usa Vue?"));

    await estado.iniciarTreino(vaga);

    expect(gerarPergunta).toHaveBeenCalledWith(
      {
        tipo_contexto: "VAGA",
        habilidades: null,
        vaga: {
          titulo: "Desenvolvedora Vue",
          senioridade: "Pleno",
          requisitos: ["Vue.js", "TypeScript"],
        },
        interacoes: [],
      },
      1,
      3,
    );
    expect(estado.treinoAtivo).toBe(true);
    expect(estado.perguntasDoTreino).toEqual(["Como usa Vue?"]);
    expect(estado.contextoTreino).toBe("Vaga: Desenvolvedora Vue · Empresa Cypress");
  });

  it("descarta o perfil que chega depois da troca para outra conta", async () => {
    let concluirHistorico!: (treinos: TreinoPersistido[]) => void;
    vi.mocked(listarHistorico).mockImplementation(
      () => new Promise((resolve) => { concluirHistorico = resolve; }),
    );
    vi.mocked(listarHabilidades).mockResolvedValue([]);
    vi.mocked(listarVagas).mockResolvedValue([]);
    const estado = useEstadoAplicacao();
    estado.adicionarHabilidade({
      id: "habilidade-conta-a",
      nome: "Habilidade privada A",
      categoria: "Teste",
      nivel: 3,
      criadoEm: "2026-11-02T12:00:00Z",
      atualizadoEm: "2026-11-02T12:00:00Z",
    });
    let contaAtual: string | null = "conta-a";

    const carregamento = estado.carregarPerfil(
      "token-conta-a",
      "conta-a",
      () => contaAtual,
    );
    contaAtual = "conta-b";
    estado.limparDadosDaConta();
    concluirHistorico([{
      id: "treino-conta-a",
      titulo: "Histórico da conta A",
      contexto: "Dados privados",
      nota: 88,
      data: "2026-11-02T12:00:00Z",
      pontosFortes: [],
      pontosMelhoria: [],
    }]);
    await carregamento;

    expect(estado.historico).toEqual([]);
    expect(estado.habilidades).toEqual([]);
    expect(estado.vagas).toEqual([]);
  });

  it("preserva a resposta após falha da IA e a envia ao tentar novamente", async () => {
    const estado = useEstadoAplicacao();
    estado.adicionarHabilidade({
      id: "hab-1",
      nome: "Vue.js",
      categoria: "Frontend",
      nivel: 4,
      criadoEm: "2026-11-02T12:00:00Z",
      atualizadoEm: "2026-11-02T12:00:00Z",
    });
    vi.mocked(gerarPergunta)
      .mockResolvedValueOnce(perguntaGerada("Pergunta 1"))
      .mockRejectedValueOnce(new Error("Serviço de IA indisponível"))
      .mockResolvedValueOnce(perguntaGerada("Pergunta 2"));

    await estado.iniciarTreino(undefined, ["Vue.js"]);
    expect(gerarPergunta).toHaveBeenNthCalledWith(
      1,
      {
        tipo_contexto: "HABILIDADES",
        habilidades: [{ nome: "Vue.js", nivel: 4 }],
        vaga: null,
        interacoes: [],
      },
      1,
      3,
    );

    await expect(estado.responder("  Expliquei a solução  ")).rejects.toThrow(
      "Serviço de IA indisponível",
    );
    expect(estado.respostaPendente).toBe("Expliquei a solução");
    expect(estado.respostas).toEqual([]);
    expect(estado.perguntaAtual).toBe(0);

    await estado.responder("");
    expect(estado.respostaPendente).toBeNull();
    expect(estado.respostas).toEqual(["Expliquei a solução"]);
    expect(estado.perguntaAtual).toBe(1);
    expect(gerarPergunta).toHaveBeenLastCalledWith(
      {
        tipo_contexto: "HABILIDADES",
        habilidades: [{ nome: "Vue.js", nivel: 4 }],
        vaga: null,
        interacoes: [
          { numero: 1, pergunta: "Pergunta 1", resposta: "Expliquei a solução" },
        ],
      },
      2,
      3,
    );
  });

  it("avalia, persiste e adiciona o treino concluído ao histórico", async () => {
    const estado = useEstadoAplicacao();
    vi.mocked(gerarPergunta)
      .mockResolvedValueOnce(perguntaGerada("Pergunta 1"))
      .mockResolvedValueOnce(perguntaGerada("Pergunta 2"))
      .mockResolvedValueOnce(perguntaGerada("Pergunta 3"));
    vi.mocked(gerarAvaliacao).mockResolvedValue({
      nota: 88,
      pontos_fortes: ["Boa comunicação"],
      pontos_melhoria: ["Detalhar alternativas"],
      versao_modelo: "stub-1",
      versao_prompt: "avaliacao-1",
    });
    vi.mocked(salvarTreino).mockResolvedValue({
      id: "treino-1",
      titulo: "Entrevista por habilidades",
      contexto: "Habilidades: nenhuma habilidade cadastrada",
      nota: 88,
      data: "2026-11-02T12:00:00Z",
      pontosFortes: ["Boa comunicação"],
      pontosMelhoria: ["Detalhar alternativas"],
    });

    await estado.iniciarTreino();
    await estado.responder("Resposta 1");
    await estado.responder("Resposta 2");
    await estado.responder("Resposta 3");

    const contextoEsperado = {
      tipo_contexto: "HABILIDADES",
      habilidades: [],
      vaga: null,
      interacoes: [
        { numero: 1, pergunta: "Pergunta 1", resposta: "Resposta 1" },
        { numero: 2, pergunta: "Pergunta 2", resposta: "Resposta 2" },
        { numero: 3, pergunta: "Pergunta 3", resposta: "Resposta 3" },
      ],
    };
    expect(gerarAvaliacao).toHaveBeenCalledWith(contextoEsperado);
    expect(estado.avaliacaoPendente?.nota).toBe(88);

    await estado.concluirTreino();

    expect(salvarTreino).toHaveBeenCalledWith({
      tipoContexto: "HABILIDADES",
      vagaId: null,
      contexto: "Habilidades: nenhuma habilidade cadastrada",
      habilidades: [],
      interacoes: contextoEsperado.interacoes,
    });
    expect(estado.historico[0]).toMatchObject({
      id: "treino-1",
      nota: 88,
      pontosFortes: "Boa comunicação",
      melhorias: "Detalhar alternativas",
    });
    expect(estado.feedbackAtual).toMatchObject({ id: "treino-1", nota: 88 });
    expect(estado.treinoAtivo).toBe(false);
  });
});
