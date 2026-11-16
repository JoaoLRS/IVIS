import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RedefinirSenha from "../../../src/paginas/RedefinirSenha.vue";
import { clienteHttp } from "../../../src/servicos/clienteHttp";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";

vi.mock("../../../src/servicos/clienteHttp", () => ({
  clienteHttp: { get: vi.fn(), post: vi.fn() },
}));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  obterTokenCsrf: vi.fn(),
}));

function montarPagina(email = "") {
  if (email) window.history.replaceState({ emailRecuperacao: email }, "", window.location.pathname);
  return mount(RedefinirSenha, {
    global: {
      stubs: {
        RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
      },
    },
  });
}

async function confirmarCodigo(pagina: ReturnType<typeof montarPagina>) {
  await pagina.get("#recovery-code").setValue("12345678");
  await pagina.get("form").trigger("submit");
  await flushPromises();
}

describe("página de redefinição de senha", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", window.location.pathname);
    vi.mocked(clienteHttp.post).mockReset();
    vi.mocked(clienteHttp.get).mockReset().mockRejectedValue(new Error("sem autorização"));
    vi.mocked(obterTokenCsrf).mockReset();
    vi.mocked(obterTokenCsrf).mockResolvedValue("csrf-teste");
  });

  it("não busca CSRF nem chama a API quando a nova senha é fraca", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina("aluno@example.com");
    await confirmarCodigo(pagina);
    await pagina.get("#nova-senha").setValue("abcdefg1");
    await pagina.get("#confirmar-senha").setValue("abcdefg1");
    await pagina.get("form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toContain("requisitos");
    expect(clienteHttp.post).toHaveBeenCalledTimes(1);
  });

  it("não envia senhas diferentes para a API", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina("aluno@example.com");
    await confirmarCodigo(pagina);
    await pagina.get("#nova-senha").setValue("SenhaSegura123!");
    await pagina.get("#confirmar-senha").setValue("OutraSenha123!");
    await pagina.get("form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toBe("As senhas devem ser iguais.");
    expect(clienteHttp.post).toHaveBeenCalledTimes(1);
  });

  it("valida o código antes de mostrar os campos da nova senha", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina("aluno@example.com");
    expect(pagina.text()).toContain("Enviamos um código de 8 dígitos para aluno@example.com");
    expect(pagina.find("#nova-senha").exists()).toBe(false);
    await confirmarCodigo(pagina);

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/senhas/validacao-codigo",
      { email: "aluno@example.com", codigo: "12345678" },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-teste" } },
    );
    expect(pagina.find("#recovery-code").exists()).toBe(false);
    expect(pagina.find("#nova-senha").exists()).toBe(true);
  });

  it("não libera os campos de senha quando o código é recusado", async () => {
    vi.mocked(clienteHttp.post).mockRejectedValue(new Error("código inválido"));
    const pagina = montarPagina("aluno@example.com");
    await confirmarCodigo(pagina);

    expect(pagina.find("#nova-senha").exists()).toBe(false);
    expect(pagina.get('[role="alert"]').text()).toContain("Não foi possível validar o código");
  });

  it("redefine a senha apenas com a autorização em cookie", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina("aluno@example.com");
    await confirmarCodigo(pagina);
    await pagina.get("#nova-senha").setValue("NovaSenhaSegura123!");
    await pagina.get("#confirmar-senha").setValue("NovaSenhaSegura123!");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/senhas/redefinicao",
      { novaSenha: "NovaSenhaSegura123!" },
      {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": "csrf-teste" },
      },
    );
    expect(pagina.get('[role="status"]').text()).toContain("Senha redefinida");
    expect(pagina.find("form").exists()).toBe(false);
  });

  it("mostra o e-mail levado da solicitação sem colocá-lo na URL", () => {
    const pagina = montarPagina("aluno@example.com");
    expect(pagina.text()).toContain("aluno@example.com");
    expect(pagina.find("#recovery-email").exists()).toBe(false);
  });

  it("retoma a etapa da nova senha após atualizar a página com autorização válida", async () => {
    vi.mocked(clienteHttp.get).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina();
    await flushPromises();

    expect(clienteHttp.get).toHaveBeenCalledWith(
      "/v1/senhas/redefinicao/autorizacao",
      { withCredentials: true },
    );
    expect(pagina.find("#nova-senha").exists()).toBe(true);
  });
});
