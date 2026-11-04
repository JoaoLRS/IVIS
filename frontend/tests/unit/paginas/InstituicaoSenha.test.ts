import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Instituicao from "../../../src/paginas/Instituicao.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";
import {
  cadastrarEVincularAluno,
  listarAlunosInstituicao,
  listarCapacidadeInstituicao,
} from "../../../src/servicos/instituicao";

vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/perfilAluno", () => ({ obterTokenCsrf: vi.fn() }));
vi.mock("../../../src/servicos/instituicao", () => ({
  cadastrarEVincularAluno: vi.fn(),
  listarAlunosInstituicao: vi.fn(),
  listarCapacidadeInstituicao: vi.fn(),
  removerAlunoVinculado: vi.fn(),
  vincularAluno: vi.fn(),
}));

describe("senha no cadastro institucional de aluno", () => {
  beforeEach(() => {
    vi.mocked(cadastrarEVincularAluno).mockReset();
    vi.mocked(obterTokenCsrf).mockReset();
    vi.mocked(useEstadoSessao).mockReturnValue({ conta: { nome: "Instituto" } } as never);
    vi.mocked(listarAlunosInstituicao).mockResolvedValue([]);
    vi.mocked(listarCapacidadeInstituicao).mockResolvedValue([{
      contratoId: "contrato-1",
      quantidadeVagas: 2,
      vagasUtilizadas: 0,
      vagasDisponiveis: 2,
    }] as never);
  });

  it.each([
    ["Abcdefg1", "Abcdefg1", "requisitos"],
    ["Abcdefg1!", "Abcdefg2!", "não coincidem"],
  ])("bloqueia senha %s com confirmação %s", async (senha, confirmacao, mensagem) => {
    const pagina = mount(Instituicao);
    await flushPromises();
    const campos = pagina.findAll('.institution-register-form input[type="password"]');
    await campos[0]!.setValue(senha);
    await campos[1]!.setValue(confirmacao);
    await pagina.get(".institution-register-form").trigger("submit");

    expect(pagina.get('[role="alert"]').text()).toContain(mensagem);
    expect(obterTokenCsrf).not.toHaveBeenCalled();
    expect(cadastrarEVincularAluno).not.toHaveBeenCalled();
  });
});
