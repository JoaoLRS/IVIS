import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRoute, useRouter } from "vue-router";
import Cadastro from "../../../src/paginas/Cadastro.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";

vi.mock("vue-router", () => ({
  useRoute: vi.fn(),
  useRouter: vi.fn(),
  RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
}));
vi.mock("../../../src/estados/estadoSessao", () => ({
  useEstadoSessao: vi.fn(),
}));

const cadastrarAluno = vi.fn();
const cadastrarInstituicao = vi.fn();

function montarPagina(tipo: "aluno" | "instituicao" = "aluno") {
  vi.mocked(useRoute).mockReturnValue({ query: { tipo } } as never);
  return mount(Cadastro);
}

describe("senha no cadastro", () => {
  beforeEach(() => {
    cadastrarAluno.mockReset();
    cadastrarInstituicao.mockReset();
    vi.mocked(useRouter).mockReturnValue({ replace: vi.fn() } as never);
    vi.mocked(useEstadoSessao).mockReturnValue({
      conta: null,
      cadastrarAluno,
      cadastrarInstituicao,
    } as never);
  });

  it.each(["aluno", "instituicao"] as const)(
    "bloqueia cadastro de %s com senha incompleta antes de chamar a API",
    async (tipo) => {
      const pagina = montarPagina(tipo);
      await pagina.get("#senha-cadastro").setValue("Abcdefg1");
      await pagina.get("#confirmar-senha").setValue("Abcdefg1");
      await pagina.get("form").trigger("submit");

      expect(pagina.get(".cadastro-message").text()).toContain("requisitos");
      expect(cadastrarAluno).not.toHaveBeenCalled();
      expect(cadastrarInstituicao).not.toHaveBeenCalled();
      expect(pagina.findAll(".password-requirement-met")).toHaveLength(4);
    },
  );

  it("bloqueia senhas diferentes mesmo quando a senha atende aos requisitos", async () => {
    const pagina = montarPagina();
    await pagina.get("#senha-cadastro").setValue("Abcdefg1!");
    await pagina.get("#confirmar-senha").setValue("Abcdefg2!");

    expect(pagina.get("#erro-confirmacao").text()).toContain("não coincidem");
    expect(pagina.get("#confirmar-senha").attributes("aria-invalid")).toBe(
      "true",
    );
    expect(
      pagina.find('label[for="confirmar-senha"] .bi-check-lg').exists(),
    ).toBe(false);

    await pagina.get("form").trigger("submit");

    expect(pagina.get(".cadastro-message").text()).toContain("não coincidem");
    expect(cadastrarAluno).not.toHaveBeenCalled();
    expect(pagina.findAll(".password-requirement-met")).toHaveLength(5);
  });

  it("envia a senha válida e confirmada no cadastro do aluno", async () => {
    cadastrarAluno.mockResolvedValue(undefined);
    const pagina = montarPagina();
    await pagina.get("#senha-cadastro").setValue("Abcdefg1!");
    await pagina.get("#confirmar-senha").setValue("Abcdefg1!");
    expect(pagina.find("#erro-confirmacao").exists()).toBe(false);
    expect(
      pagina.find('label[for="confirmar-senha"] .bi-check-lg').exists(),
    ).toBe(true);
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(cadastrarAluno).toHaveBeenCalledWith(
      expect.objectContaining({ senha: "Abcdefg1!" }),
    );
    expect(pagina.get(".cadastro-success").text()).toContain("Conta criada");
    expect(pagina.find(".email-simulator").exists()).toBe(false);
    expect(pagina.get('.cadastro-success a[href="http://localhost:8025"]').text()).toBe("Abrir Mailpit");
  });
});
