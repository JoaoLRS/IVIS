import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Login from "../../../src/paginas/Login.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { clienteHttp } from "../../../src/servicos/clienteHttp";

const substituir = vi.fn();
const rota = { query: { redirect: "/treinos" } };
vi.mock("vue-router", async (importOriginal) => ({
  ...(await importOriginal<typeof import("vue-router")>()),
  useRoute: () => rota,
  useRouter: () => ({ replace: substituir }),
}));
vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/clienteHttp", () => ({
  clienteHttp: { get: vi.fn(), post: vi.fn() },
}));

const entrar = vi.fn();
const sessao = { entrar, temPlano: true };
function montar() {
  return mount(Login, { global: { stubs: { RouterLink: true } } });
}

describe("página Login", () => {
  beforeEach(() => {
    entrar.mockReset().mockResolvedValue(undefined);
    substituir.mockReset().mockResolvedValue(undefined);
    vi.mocked(clienteHttp.get).mockReset();
    vi.mocked(clienteHttp.post).mockReset();
    vi.mocked(useEstadoSessao).mockReturnValue(sessao as never);
    sessao.temPlano = true;
    rota.query.redirect = "/treinos";
  });

  it("retoma a página solicitada depois do login", async () => {
    const pagina = montar();
    await pagina.get("#login-email").setValue("aluno@ivis.test");
    await pagina.get("#login-senha").setValue("Senha#123");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(entrar).toHaveBeenCalledWith("aluno@ivis.test", "Senha#123");
    expect(substituir).toHaveBeenCalledWith("/treinos");
  });

  it("redireciona para planos quando falta acesso, mantendo checkout disponível", async () => {
    sessao.temPlano = false;
    const pagina = montar();
    await pagina.get("#login-email").setValue("aluno@ivis.test");
    await pagina.get("#login-senha").setValue("Senha#123");
    await pagina.get("form").trigger("submit");
    await flushPromises();
    expect(substituir).toHaveBeenCalledWith("/planos");

    rota.query.redirect = "/checkout/plano-1";
    await pagina.get("form").trigger("submit");
    await flushPromises();
    expect(substituir).toHaveBeenLastCalledWith("/checkout/plano-1");
  });

  it("descarta redirecionamento externo recebido na URL", async () => {
    rota.query.redirect = "//externo.test";
    const pagina = montar();
    await pagina.get("#login-email").setValue("aluno@ivis.test");
    await pagina.get("#login-senha").setValue("Senha#123");
    await pagina.get("form").trigger("submit");
    await flushPromises();
    expect(substituir).toHaveBeenCalledWith("/inicio");
  });

  it("envia recuperação de senha com CSRF sem autenticar o usuário", async () => {
    vi.mocked(clienteHttp.get).mockResolvedValue({ data: { token: "csrf-1" } } as never);
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montar();
    await pagina.get(".login-text-button").trigger("click");
    await pagina.get("#login-email").setValue("aluno@ivis.test");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/senhas/recuperacao", { email: "aluno@ivis.test" },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" } },
    );
    expect(entrar).not.toHaveBeenCalled();
    expect(pagina.get('[role="status"]').text()).toContain("instruções para recuperar");
  });
});
