import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Treinos from "../../../src/paginas/Treinos.vue";
import { useEstadoAplicacao } from "../../../src/estados/estadoAplicacao";
import { enviarRespostaTreino, iniciarTreinoRemoto, type SessaoTreino } from "../../../src/servicos/treinos";

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
});
