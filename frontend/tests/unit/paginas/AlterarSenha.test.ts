import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AlterarSenha from "../../../src/paginas/AlterarSenha.vue";
import { clienteHttp } from "../../../src/servicos/clienteHttp";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";

vi.mock("../../../src/servicos/clienteHttp", () => ({
  clienteHttp: { post: vi.fn() },
}));
vi.mock("../../../src/servicos/perfilAluno", () => ({
  obterTokenCsrf: vi.fn(),
}));

function montarPagina() {
  return mount(AlterarSenha, {
    global: {
      stubs: {
        RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
      },
    },
  });
}

describe("página Alterar senha", () => {
  beforeEach(() => {
    vi.mocked(clienteHttp.post).mockReset();
    vi.mocked(obterTokenCsrf).mockReset();
    vi.mocked(obterTokenCsrf).mockResolvedValue("csrf-teste");
  });

  it("não busca CSRF nem chama a API quando a nova senha é fraca", async () => {
    const pagina = montarPagina();
    await pagina.get("#senha-atual").setValue("SenhaAtual123!");
    await pagina.get("#nova-senha-conta").setValue("abcdefg1");
    await pagina.get("#confirmar-senha-conta").setValue("abcdefg1");
    await pagina.get("form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toContain("requisitos");
    expect(obterTokenCsrf).not.toHaveBeenCalled();
    expect(clienteHttp.post).not.toHaveBeenCalled();
  });

  it("não chama a API se a nova senha e sua confirmação forem diferentes", async () => {
    const pagina = montarPagina();
    await pagina.get("#senha-atual").setValue("SenhaAtual123!");
    await pagina.get("#nova-senha-conta").setValue("NovaSenhaSegura123!");
    await pagina.get("#confirmar-senha-conta").setValue("SenhaDiferente123!");
    await pagina.get("form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toBe(
      "As senhas devem ser iguais.",
    );
    expect(obterTokenCsrf).not.toHaveBeenCalled();
    expect(clienteHttp.post).not.toHaveBeenCalled();
  });

  it("envia a senha com proteção CSRF e limpa os campos após salvar", async () => {
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: {} } as never);
    const pagina = montarPagina();
    await pagina.get("#senha-atual").setValue("SenhaAtual123!");
    await pagina.get("#nova-senha-conta").setValue("NovaSenhaSegura123!");
    await pagina.get("#confirmar-senha-conta").setValue("NovaSenhaSegura123!");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/conta/senha",
      { senhaAtual: "SenhaAtual123!", novaSenha: "NovaSenhaSegura123!" },
      {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": "csrf-teste" },
      },
    );
    expect(pagina.get('[role="status"]').text()).toContain("Senha alterada");
    expect((pagina.get("#senha-atual").element as HTMLInputElement).value).toBe(
      "",
    );
    expect(
      (pagina.get("#nova-senha-conta").element as HTMLInputElement).value,
    ).toBe("");
    expect(
      (pagina.get("#confirmar-senha-conta").element as HTMLInputElement).value,
    ).toBe("");
  });

  it("exibe a mensagem do backend se a senha atual não for aceita", async () => {
    vi.mocked(clienteHttp.post).mockRejectedValue({
      response: { data: { mensagem: "A senha atual está incorreta." } },
    });
    const pagina = montarPagina();
    await pagina.get("#senha-atual").setValue("SenhaErrada123!");
    await pagina.get("#nova-senha-conta").setValue("NovaSenhaSegura123!");
    await pagina.get("#confirmar-senha-conta").setValue("NovaSenhaSegura123!");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(pagina.get('[role="alert"]').text()).toBe(
      "A senha atual está incorreta.",
    );
    expect(
      (pagina.get("#nova-senha-conta").element as HTMLInputElement).value,
    ).toBe("NovaSenhaSegura123!");
  });

  it("oferece retorno à página de dados da conta", () => {
    const pagina = montarPagina();

    expect(pagina.get('a[href="/conta"]').text()).toContain(
      "Voltar para minha conta",
    );
  });
});
