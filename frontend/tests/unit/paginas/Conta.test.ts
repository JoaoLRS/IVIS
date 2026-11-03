import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Conta from "../../../src/paginas/Conta.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { atualizarMinhaConta } from "../../../src/servicos/conta";

vi.mock("../../../src/servicos/conta", () => ({
  atualizarMinhaConta: vi.fn(),
}));

function montarPagina(tipoConta: "ALUNO" | "INSTITUICAO" = "ALUNO") {
  const pinia = createPinia();
  setActivePinia(pinia);
  const sessao = useEstadoSessao(pinia);
  sessao.conta =
    tipoConta === "ALUNO"
      ? {
          id: "aluno-1",
          tipoConta,
          email: "ana@ivis.dev",
          nome: "Ana Silva",
          cpf: "12345678900",
          telefone: "11999990000",
          endereco: "Rua A",
        }
      : {
          id: "instituicao-1",
          tipoConta,
          email: "contato@escola.dev",
          nome: "Escola Exemplo",
          nomeResponsavel: "João Responsável",
          cnpj: "12345678000199",
          telefone: "11999990000",
          endereco: "Rua B",
        };
  return mount(Conta, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
      },
    },
  });
}

describe("página de conta", () => {
  beforeEach(() => {
    vi.mocked(atualizarMinhaConta).mockReset();
  });

  it("oferece acesso à página separada de alteração de senha", () => {
    const pagina = montarPagina();

    expect(pagina.get('a[href="/alterar-senha"]').text()).toContain(
      "Alterar senha",
    );
  });

  it("salva os campos de perfil de aluno e mostra confirmação", async () => {
    vi.mocked(atualizarMinhaConta).mockResolvedValue({
      id: "aluno-1",
      tipoConta: "ALUNO",
      email: "ana.nova@ivis.dev",
      nome: "Ana Silva",
      cpf: "12345678900",
      telefone: "11988887777",
      endereco: "Rua Nova",
    });
    const pagina = montarPagina();
    await pagina.get("#nome-conta").setValue("Ana Silva");
    await pagina.get("#email-conta").setValue("ana.nova@ivis.dev");
    await pagina.get("#telefone-conta").setValue("11988887777");
    await pagina.get("#endereco-conta").setValue("Rua Nova");
    await pagina.findAll("form")[0].trigger("submit");
    await flushPromises();

    expect(atualizarMinhaConta).toHaveBeenCalledWith({
      email: "ana.nova@ivis.dev",
      telefone: "11988887777",
      endereco: "Rua Nova",
      nome: "Ana Silva",
      cpf: "12345678900",
    });
    expect(pagina.text()).toContain("Dados atualizados.");
  });

  it("envia os campos institucionais sem payload de aluno", async () => {
    vi.mocked(atualizarMinhaConta).mockResolvedValue({
      id: "instituicao-1",
      tipoConta: "INSTITUICAO",
      email: "contato@escola.dev",
      nome: "Escola Nova",
      nomeResponsavel: "João Responsável",
      cnpj: "12345678000199",
    });
    const pagina = montarPagina("INSTITUICAO");
    await pagina.get("#nome-conta").setValue("Escola Nova");
    await pagina.findAll("form")[0].trigger("submit");
    await flushPromises();

    expect(atualizarMinhaConta).toHaveBeenCalledWith({
      email: "contato@escola.dev",
      telefone: "11999990000",
      endereco: "Rua B",
      nomeInstituicao: "Escola Nova",
      nomeResponsavel: "João Responsável",
      cnpj: "12345678000199",
    });
  });

  it("mostra a falha ao salvar e preserva os valores digitados", async () => {
    vi.mocked(atualizarMinhaConta).mockRejectedValue({
      response: { data: { mensagem: "O e-mail já está em uso." } },
    });
    const pagina = montarPagina();
    await pagina.get("#email-conta").setValue("ocupado@ivis.dev");
    await pagina.findAll("form")[0].trigger("submit");
    await flushPromises();

    expect(pagina.get('[role="alert"]').text()).toBe(
      "O e-mail já está em uso.",
    );
    expect(
      (pagina.get("#email-conta").element as HTMLInputElement).value,
    ).toBe("ocupado@ivis.dev");
  });
});
