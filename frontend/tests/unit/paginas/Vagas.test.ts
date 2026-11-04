import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Vagas from "../../../src/paginas/Vagas.vue";
import { useEstadoAplicacao } from "../../../src/estados/estadoAplicacao";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { criarVaga, excluirVaga, obterTokenCsrf } from "../../../src/servicos/perfilAluno";

const navegar = vi.fn();
vi.mock("vue-router", async (importOriginal) => ({
  ...(await importOriginal<typeof import("vue-router")>()),
  useRouter: () => ({ push: navegar }),
}));
vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  criarVaga: vi.fn(), atualizarVaga: vi.fn(),
  excluirVaga: vi.fn(), obterTokenCsrf: vi.fn(),
}));

const vaga = {
  id: "vaga-1", titulo: "Engenheira Vue", empresa: "Empresa",
  senioridade: "Pleno", descricao: null, requisitos: ["Vue", "TypeScript"],
  criadoEm: "2026-11-01T00:00:00Z", atualizadoEm: "2026-11-01T00:00:00Z",
};

describe("página Vagas", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(useEstadoSessao).mockReturnValue({ accessToken: "token-1" } as never);
    vi.mocked(obterTokenCsrf).mockReset().mockResolvedValue("csrf-1");
    vi.mocked(criarVaga).mockReset();
    vi.mocked(excluirVaga).mockReset();
    navegar.mockReset();
  });

  it("normaliza requisitos antes de salvar e usa o ID ao iniciar treino", async () => {
    vi.mocked(criarVaga).mockResolvedValue(vaga);
    const pagina = mount(Vagas);
    await pagina.get("button.btn-primary").trigger("click");
    const campos = pagina.findAll('[role="dialog"] input');
    await campos[0].setValue("  Engenheira Vue  ");
    await campos[1].setValue("  Empresa  ");
    await pagina.get('[role="dialog"] select').setValue("Pleno");
    await campos[2].setValue(" Vue, , TypeScript ");
    await pagina.get('[role="dialog"] form').trigger("submit");
    await flushPromises();

    expect(criarVaga).toHaveBeenCalledWith("token-1", "csrf-1", {
      titulo: "Engenheira Vue", empresa: "Empresa", senioridade: "Pleno",
      requisitos: ["Vue", "TypeScript"], descricao: null,
    });
    await pagina.get(".job-card .btn-primary").trigger("click");
    expect(navegar).toHaveBeenCalledWith({ path: "/treinos", query: { vaga: "vaga-1" } });
  });

  it("impede excluir a vaga usada no treino em andamento", async () => {
    const estado = useEstadoAplicacao();
    estado.adicionarVaga(vaga);
    estado.vagaEmTreinamento = vaga.titulo;
    const pagina = mount(Vagas);

    expect(pagina.get('[aria-label="Excluir vaga Engenheira Vue"]').attributes("disabled")).toBeDefined();
    expect(estado.vagas).toHaveLength(1);
    expect(excluirVaga).not.toHaveBeenCalled();
  });
});
