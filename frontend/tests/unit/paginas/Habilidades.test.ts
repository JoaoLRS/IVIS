import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Habilidades from "../../../src/paginas/Habilidades.vue";
import { useEstadoAplicacao } from "../../../src/estados/estadoAplicacao";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { criarHabilidade, excluirHabilidade, obterTokenCsrf } from "../../../src/servicos/perfilAluno";

vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  criarHabilidade: vi.fn(), atualizarHabilidade: vi.fn(),
  excluirHabilidade: vi.fn(), obterTokenCsrf: vi.fn(),
}));

const habilidade = {
  id: "habilidade-1", nome: "Vue.js", categoria: "Frontend", nivel: 4,
  criadoEm: "2026-11-01T00:00:00Z", atualizadoEm: "2026-11-01T00:00:00Z",
};

describe("página Habilidades", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(useEstadoSessao).mockReturnValue({ accessToken: "token-1" } as never);
    vi.mocked(obterTokenCsrf).mockReset().mockResolvedValue("csrf-1");
    vi.mocked(criarHabilidade).mockReset();
    vi.mocked(excluirHabilidade).mockReset();
  });

  it("persiste a habilidade antes de adicioná-la ao perfil", async () => {
    vi.mocked(criarHabilidade).mockResolvedValue(habilidade);
    const pagina = mount(Habilidades);
    await pagina.get("button.btn-primary").trigger("click");
    const campos = pagina.findAll('[role="dialog"] input');
    await campos[0].setValue("Vue.js");
    await campos[1].setValue("Frontend");
    await pagina.get('[role="dialog"] select').setValue("4");
    await pagina.get('[role="dialog"] form').trigger("submit");
    await flushPromises();

    expect(criarHabilidade).toHaveBeenCalledWith("token-1", "csrf-1", {
      nome: "Vue.js", categoria: "Frontend", nivel: 4,
    });
    expect(useEstadoAplicacao().habilidades).toEqual([habilidade]);
    expect(pagina.emitted("notificar")?.[0]).toEqual(["Habilidade adicionada ao perfil."]);
  });

  it("só remove a habilidade local após confirmação e sucesso da API", async () => {
    const estado = useEstadoAplicacao();
    estado.adicionarHabilidade(habilidade);
    vi.mocked(excluirHabilidade).mockResolvedValue(undefined);
    const pagina = mount(Habilidades);
    await pagina.get('[aria-label="Remover Vue.js"]').trigger("click");
    expect(estado.habilidades).toHaveLength(1);
    await pagina.get('[role="dialog"] .btn-danger').trigger("click");
    await flushPromises();

    expect(excluirHabilidade).toHaveBeenCalledWith("token-1", "csrf-1", "habilidade-1");
    expect(estado.habilidades).toEqual([]);
  });
});
