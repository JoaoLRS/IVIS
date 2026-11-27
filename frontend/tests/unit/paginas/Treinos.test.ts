import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Treinos from "../../../src/paginas/Treinos.vue";
import { useEstadoAplicacao } from "../../../src/estados/estadoAplicacao";
import {
  concluirTreinoRemoto,
  enviarRespostaTreino,
  iniciarTreinoRemoto,
  type SessaoTreino,
} from "../../../src/servicos/treinos";
import { mensagemCarregamentoGlobal } from "../../../src/servicos/clienteHttp";

const navegar = vi.fn();
const substituir = vi.fn();
vi.mock("vue-router", async (importOriginal) => ({
  ...(await importOriginal<typeof import("vue-router")>()),
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: navegar, replace: substituir }),
}));
vi.mock("../../../src/servicos/treinos", () => ({
  iniciarTreinoRemoto: vi.fn(), enviarRespostaTreino: vi.fn(),
  concluirTreinoRemoto: vi.fn(), cancelarTreinoRemoto: vi.fn(),
  consultarTreinoEmAndamento: vi.fn(),
}));
vi.mock("../../../src/servicos/historico", () => ({ listarHistorico: vi.fn() }));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  listarHabilidades: vi.fn(), listarVagas: vi.fn(),
}));

function sessao(resposta: string | null = null): SessaoTreino {
  return {
    id: "treino-1", tipoContexto: "HABILIDADES", contexto: "Habilidades: Vue.js",
    vagaId: null, habilidades: ["Vue.js"], totalPerguntas: 3,
    perguntas: [{ id: "pergunta-1", numero: 1, texto: "Explique reatividade em Vue", resposta }],
  };
}

describe("página Treinos", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(iniciarTreinoRemoto).mockReset();
    vi.mocked(enviarRespostaTreino).mockReset();
    vi.mocked(concluirTreinoRemoto).mockReset();
    mensagemCarregamentoGlobal.value = "Carregando...";
    navegar.mockReset();
    substituir.mockReset();
  });

  it("só inicia com contexto selecionado e mostra a pergunta recebida do backend", async () => {
    const estado = useEstadoAplicacao();
    estado.adicionarHabilidade({
      id: "h-1", nome: "Vue.js", categoria: "Frontend", nivel: 3,
      criadoEm: "2026-11-01", atualizadoEm: "2026-11-01",
    });
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue(sessao());
    const pagina = mount(Treinos, { global: { stubs: { RouterLink: true } } });
    const iniciar = pagina.findAll("button").find((botao) => botao.text().includes("Iniciar entrevista"));
    expect(iniciar).toBeDefined();
    await pagina.get(".skill-picker-heading button:last-child").trigger("click");
    expect(iniciar?.attributes("disabled")).toBeDefined();
    await pagina.get(".skill-chip").trigger("click");
    await iniciar?.trigger("click");
    await flushPromises();

    expect(iniciarTreinoRemoto).toHaveBeenCalledWith({
      tipoContexto: "HABILIDADES", vagaId: null, habilidades: ["Vue.js"],
    });
    expect(pagina.text()).toContain("Explique reatividade em Vue");
    expect(pagina.text()).toContain("PERGUNTA 1 DE 3");
  });

  it("preserva a resposta e mostra orientação quando o serviço de IA falha", async () => {
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue(sessao());
    vi.mocked(enviarRespostaTreino).mockRejectedValue({
      isAxiosError: true, response: { status: 503 },
    });
    const estado = useEstadoAplicacao();
    await estado.iniciarTreino(undefined, ["Vue.js"]);
    const pagina = mount(Treinos, { global: { stubs: { RouterLink: true } } });
    const campo = pagina.find("textarea");
    expect(campo.exists()).toBe(true);
    await campo.setValue("  Resposta técnica  ");
    await pagina.get(".chat-composer").trigger("submit");
    await flushPromises();

    expect(enviarRespostaTreino).toHaveBeenCalledWith(
      "treino-1", "pergunta-1", "Resposta técnica",
    );
    expect(estado.respostaPendente).toBe("Resposta técnica");
    expect(pagina.get('[role="alert"]').text()).toContain("Sua resposta foi mantida");
  });

  it("atualiza os caracteres restantes enquanto a resposta é digitada", async () => {
    vi.mocked(iniciarTreinoRemoto).mockResolvedValue(sessao());
    const estado = useEstadoAplicacao();
    await estado.iniciarTreino(undefined, ["Vue.js"]);
    const pagina = mount(Treinos, { global: { stubs: { RouterLink: true } } });
    const contador = () => pagina.find('[aria-live="polite"]').text();
    const campo = pagina.find("textarea");

    expect(contador()).toContain("4000 caracteres restantes");
    await campo.setValue("Olá!");
    expect(contador()).toContain("3996 caracteres restantes");
  });

  it("mostra avaliação no carregamento central até concluir o treino", async () => {
    const estado = useEstadoAplicacao();
    estado.treinoAtivo = true;
    estado.perguntaAtual = 3;
    estado.respostas = ["R1", "R2", "R3"];
    estado.perguntasDoTreino = ["P1", "P2", "P3"];
    estado.sessaoTreino = {
      id: "treino-1",
      tipoContexto: "HABILIDADES",
      contexto: "Habilidades: Vue.js",
      vagaId: null,
      habilidades: ["Vue.js"],
      totalPerguntas: 3,
      perguntas: [
        { id: "p1", numero: 1, texto: "P1", resposta: "R1" },
        { id: "p2", numero: 2, texto: "P2", resposta: "R2" },
        { id: "p3", numero: 3, texto: "P3", resposta: "R3" },
      ],
    };
    let concluir!: (
      treino: Awaited<ReturnType<typeof concluirTreinoRemoto>>,
    ) => void;
    vi.mocked(concluirTreinoRemoto).mockImplementation(
      () =>
        new Promise((resolve) => {
          concluir = resolve;
        }),
    );
    const pagina = mount(Treinos, { global: { stubs: { RouterLink: true } } });
    const botao = pagina.get(".chat-composer-finished button");

    await botao.trigger("click");

    expect(botao.text()).toContain("Finalizar e ver feedback");
    expect(botao.attributes("disabled")).toBeDefined();
    expect(mensagemCarregamentoGlobal.value).toBe("Avaliando a sua entrevista");

    concluir({
      id: "treino-1",
      titulo: "Entrevista",
      contexto: "Habilidades: Vue.js",
      nota: 90,
      data: "2026-11-27T00:00:00Z",
      pontosFortes: ["Boa resposta"],
      pontosMelhoria: ["Aprofundar"],
    });
    await flushPromises();

    expect(navegar).toHaveBeenCalledWith("/feedback");
    expect(mensagemCarregamentoGlobal.value).toBe("Carregando...");
  });
});
