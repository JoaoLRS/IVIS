import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRoute } from "vue-router";
import RedefinirSenha from "../../../src/paginas/RedefinirSenha.vue";
import { clienteHttp } from "../../../src/servicos/clienteHttp";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";

vi.mock("vue-router", () => ({ useRoute: vi.fn() }));
vi.mock("../../../src/servicos/clienteHttp", () => ({
  clienteHttp: { post: vi.fn() },
}));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  obterTokenCsrf: vi.fn(),
}));

function montarPagina(token: string) {
  vi.mocked(useRoute).mockReturnValue({ query: { token } } as never);
  return mount(RedefinirSenha, {
    global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
  });
}

describe("página de redefinição de senha", () => {
  beforeEach(() => {
    vi.mocked(clienteHttp.post).mockReset();
    vi.mocked(obterTokenCsrf).mockReset();
    vi.mocked(obterTokenCsrf).mockResolvedValue("csrf-teste");
  });

  it("bloqueia o envio quando o link não contém token", () => {
    const pagina = montarPagina("");

    expect(pagina.get('[role="alert"]').text()).toContain("Link inválido");
    expect(pagina.get("button").attributes("disabled")).toBeDefined();
  });

  it("não envia senhas diferentes para a API", async () => {
    const pagina = montarPagina("token-valido");
    await pagina.get("#nova-senha").setValue("SenhaSegura123!");
    await pagina.get("#confirmar-senha").setValue("OutraSenha123!");
    await pagina.get("form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toBe("As senhas devem ser iguais.");
    expect(obterTokenCsrf).not.toHaveBeenCalled();
    expect(clienteHttp.post).not.toHaveBeenCalled();
  });

  it("envia token e nova senha com CSRF e confirma a redefinição", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina("token-recuperacao");
    await pagina.get("#nova-senha").setValue("NovaSenhaSegura123!");
    await pagina.get("#confirmar-senha").setValue("NovaSenhaSegura123!");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/senhas/redefinicao",
      { token: "token-recuperacao", novaSenha: "NovaSenhaSegura123!" },
      {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": "csrf-teste" },
      },
    );
    expect(pagina.get('[role="status"]').text()).toContain("Senha redefinida");
    expect(pagina.find("form").exists()).toBe(false);
  });
});
