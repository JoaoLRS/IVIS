import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Instituicao from "../../../src/paginas/Instituicao.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";
import {
  listarAlunosInstituicao, listarCapacidadeInstituicao, vincularAluno,
} from "../../../src/servicos/instituicao";

vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/perfilAluno", () => ({ obterTokenCsrf: vi.fn() }));
vi.mock("../../../src/servicos/instituicao", () => ({
  listarAlunosInstituicao: vi.fn(), listarCapacidadeInstituicao: vi.fn(),
  vincularAluno: vi.fn(), cadastrarEVincularAluno: vi.fn(), removerAlunoVinculado: vi.fn(),
}));

const capacidade = {
  contratoId: "contrato-1", quantidadeVagas: 2,
  vagasUtilizadas: 1, vagasDisponiveis: 1,
};
const aluno = {
  id: "vinculo-1", contratoId: "contrato-1", contaAlunoId: "aluno-1",
  nomeAluno: "Ana", emailAluno: "ana@ivis.test", vinculadoEm: "2026-11-01T00:00:00Z",
};

describe("página Instituicao", () => {
  beforeEach(() => {
    vi.mocked(useEstadoSessao).mockReturnValue({
      conta: { nome: "Instituição", nomeResponsavel: "Responsável" },
    } as never);
    vi.mocked(listarAlunosInstituicao).mockReset().mockResolvedValue([aluno]);
    vi.mocked(listarCapacidadeInstituicao).mockReset().mockResolvedValue([capacidade]);
    vi.mocked(obterTokenCsrf).mockReset().mockResolvedValue("csrf-1");
    vi.mocked(vincularAluno).mockReset();
  });

  it("vincula aluno existente e atualiza a capacidade apresentada", async () => {
    const novoAluno = { ...aluno, id: "vinculo-2", nomeAluno: "Bia", emailAluno: "bia@ivis.test" };
    vi.mocked(vincularAluno).mockResolvedValue({
      vinculo: novoAluno, capacidade: { ...capacidade, vagasUtilizadas: 2, vagasDisponiveis: 0 },
    });
    const pagina = mount(Instituicao);
    await flushPromises();
    await pagina.get("#email-aluno").setValue("  bia@ivis.test  ");
    await pagina.get(".institution-add-form").trigger("submit");
    await flushPromises();

    expect(vincularAluno).toHaveBeenCalledWith("bia@ivis.test", "contrato-1", "csrf-1");
    expect(pagina.text()).toContain("Bia");
    expect(pagina.text()).toContain("2 de 2 vagas ocupadas");
    expect(pagina.get(".institution-add-form button").attributes("disabled")).toBeDefined();
  });

  it("exibe falha do serviço sem perder a lista já carregada", async () => {
    vi.mocked(vincularAluno).mockRejectedValue({
      response: { data: { mensagem: "Vagas esgotadas" } },
    });
    const pagina = mount(Instituicao);
    await flushPromises();
    await pagina.get("#email-aluno").setValue("bia@ivis.test");
    await pagina.get(".institution-add-form").trigger("submit");
    await flushPromises();

    expect(pagina.get('[role="alert"]').text()).toBe("Vagas esgotadas");
    expect(pagina.text()).toContain("Ana");
  });
});
