import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listarHistorico, type TreinoPersistido } from "../../src/servicos/historico";
import {
  cancelarTreinoRemoto,
  concluirTreinoRemoto,
  consultarTreinoEmAndamento,
  enviarRespostaTreino,
  iniciarTreinoRemoto,
  type SessaoTreino,
} from "../../src/servicos/treinos";
import { useEstadoAplicacao } from "../../src/estados/estadoAplicacao";
import { listarHabilidades, listarVagas } from "../../src/servicos/perfilAluno";

vi.mock("../../src/servicos/historico", () => ({ listarHistorico: vi.fn() }));
vi.mock("../../src/servicos/perfilAluno", () => ({
  listarHabilidades: vi.fn(),
  listarVagas: vi.fn(),
}));
vi.mock("../../src/servicos/treinos", () => ({
  cancelarTreinoRemoto: vi.fn(),
  concluirTreinoRemoto: vi.fn(),
  consultarTreinoEmAndamento: vi.fn(),
  enviarRespostaTreino: vi.fn(),
  iniciarTreinoRemoto: vi.fn(),
}));

function sessao(respostas: Array<string | null> = [null]): SessaoTreino {
  return {
    id: "treino-1",
    tipoContexto: "HABILIDADES",
    contexto: "Habilidades: Vue.js",
    vagaId: null,
    habilidades: ["Vue.js"],
    perguntas: respostas.map((resposta, indice) => ({
      id: `pergunta-${indice + 1}`,
      numero: indice + 1,
      texto: `Pergunta ${indice + 1}`,
      resposta,
    })),
    totalPerguntas: 3,
  };
}

describe("sessão de treino", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(iniciarTreinoRemoto).mockReset();
    vi.mocked(enviarRespostaTreino).mockReset();
    vi.mocked(concluirTreinoRemoto).mockReset();
    vi.mocked(cancelarTreinoRemoto).mockReset();
    vi.mocked(consultarTreinoEmAndamento).mockReset();
    vi.mocked(listarHistorico).mockReset();
    vi.mocked(listarHabilidades).mockReset();
    vi.mocked(listarVagas).mockReset();
  });

  it("envia apenas o ID da vaga e usa a pergunta registrada pelo servidor", async () => {
    const estado = useEstadoAplicacao();
    const vaga = {
      id: "vaga-1", titulo: "Desenvolvedora Vue", empresa: "Empresa",
      senioridade: "Pleno", descricao: "Interfaces", requisitos: ["Vue.js"],
      criadoEm: "2026-11-02T12:00:00Z", atualizadoEm: "2026-11-02T12:00:00Z",
    };
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue({
      ...sessao(), tipoContexto: "VAGA", contexto: "Vaga: Desenvolvedora Vue · Empresa",
      vagaId: "vaga-1",
    });

    await estado.iniciarTreino(vaga);

    expect(iniciarTreinoRemoto).toHaveBeenCalledWith({
      tipoContexto: "VAGA", vagaId: "vaga-1", habilidades: [],
    });
    expect(estado.perguntasDoTreino).toEqual(["Pergunta 1"]);
    expect(estado.sessaoTreino?.perguntas[0].id).toBe("pergunta-1");
  });

  it("retoma perguntas e respostas persistidas após carregar o perfil", async () => {
    vi.mocked(listarHabilidades).mockResolvedValue([]);
    vi.mocked(listarVagas).mockResolvedValue([]);
    vi.mocked(listarHistorico).mockResolvedValue([]);
    vi.mocked(consultarTreinoEmAndamento).mockResolvedValue(sessao(["Resposta 1", null]));
    const estado = useEstadoAplicacao();

    await estado.carregarPerfil("token", "conta-1", () => "conta-1");

    expect(estado.treinoAtivo).toBe(true);
    expect(estado.respostas).toEqual(["Resposta 1"]);
    expect(estado.perguntaAtual).toBe(1);
    expect(estado.perguntasDoTreino).toEqual(["Pergunta 1", "Pergunta 2"]);
  });

  it("preserva uma resposta se a chamada falhar e repete pelo mesmo ID", async () => {
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue(sessao());
    vi.mocked(enviarRespostaTreino)
      .mockRejectedValueOnce(new Error("IA indisponível"))
      .mockResolvedValueOnce(sessao(["Resposta 1", null]));
    const estado = useEstadoAplicacao();
    await estado.iniciarTreino(undefined, ["Vue.js"]);

    await expect(estado.responder("  Resposta 1  ")).rejects.toThrow("IA indisponível");
    expect(estado.respostaPendente).toBe("Resposta 1");
    await estado.responder("");

    expect(enviarRespostaTreino).toHaveBeenCalledTimes(2);
    expect(enviarRespostaTreino).toHaveBeenLastCalledWith(
      "treino-1", "pergunta-1", "Resposta 1",
    );
    expect(estado.respostas).toEqual(["Resposta 1"]);
  });

  it("conclui pelo ID sem reenviar perguntas ou respostas", async () => {
    const estado = useEstadoAplicacao();
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue(sessao(["Resposta 1", "Resposta 2", "Resposta 3"]));
    const salvo: TreinoPersistido = {
      id: "treino-1", titulo: "Entrevista por habilidades",
      contexto: "Habilidades: Vue.js", nota: 88,
      data: "2026-11-02T12:00:00Z", pontosFortes: ["Boa comunicação"],
      pontosMelhoria: ["Detalhar alternativas"],
    };
    vi.mocked(concluirTreinoRemoto).mockResolvedValue(salvo);
    await estado.iniciarTreino(undefined, ["Vue.js"]);

    await estado.concluirTreino();

    expect(concluirTreinoRemoto).toHaveBeenCalledWith("treino-1");
    expect(estado.historico[0]).toMatchObject({ id: "treino-1", nota: 88 });
    expect(estado.treinoAtivo).toBe(false);
  });
});
