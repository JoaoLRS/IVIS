import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useEstadoSessao } from "../../src/estados/estadoSessao";
import { clienteHttp, definirTokenAcesso } from "../../src/servicos/clienteHttp";

vi.mock("../../src/servicos/clienteHttp", () => ({
  clienteHttp: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
  definirTokenAcesso: vi.fn(),
}));

const contaAluno = {
  id: "aluno-1", tipoConta: "ALUNO" as const,
  email: "aluno@ivis.test", nome: "Aluno",
};
const respostaSessao = {
  accessToken: "token-1", tipoToken: "Bearer", expiraEmSegundos: 3600,
  conta: contaAluno,
};

function prepararCsrfEPlano(situacao = "COM_PLANO_VIGENTE", plano = "ATIVO") {
  vi.mocked(clienteHttp.get).mockImplementation(async (url) => {
    if (url === "/v1/sessoes/csrf") return { data: { token: "csrf-1" } } as never;
    if (url === "/v1/conta/plano") {
      return { data: { situacao, planos: [{ situacao: plano, origemAcesso: "INDIVIDUAL" }] } } as never;
    }
    throw new Error(`GET inesperado: ${url}`);
  });
}

describe("estado da sessão", () => {
  afterEach(() => vi.useRealTimers());
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useRealTimers();
    vi.mocked(clienteHttp.get).mockReset();
    vi.mocked(clienteHttp.post).mockReset();
    vi.mocked(clienteHttp.delete).mockReset();
    vi.mocked(definirTokenAcesso).mockReset();
  });

  it("autentica com CSRF e só libera o plano quando há contrato ativo", async () => {
    prepararCsrfEPlano("COM_PLANO_VIGENTE", "ENCERRADO");
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: respostaSessao } as never);
    const sessao = useEstadoSessao();

    await sessao.entrar(contaAluno.email, "Senha#123");

    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/sessoes", { email: contaAluno.email, senha: "Senha#123" },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" } },
    );
    expect(sessao.conta).toEqual(contaAluno);
    expect(sessao.temPlano).toBe(false);
    expect(sessao.restaurada).toBe(true);
    expect(definirTokenAcesso).toHaveBeenCalledWith("token-1");
    expect(await sessao.restaurar()).toBe(true);
    expect(clienteHttp.post).toHaveBeenCalledTimes(1);
  });

  it("renova uma única vez quando duas telas pedem a sessão simultaneamente", async () => {
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: respostaSessao } as never);
    const sessao = useEstadoSessao();

    expect(await Promise.all([sessao.restaurar(), sessao.restaurar()])).toEqual([true, true]);
    expect(clienteHttp.post).toHaveBeenCalledTimes(1);
    expect(clienteHttp.post).toHaveBeenCalledWith(
      "/v1/sessoes/renovacao", {},
      { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" } },
    );
    expect(sessao.temPlano).toBe(true);
  });

  it("limpa credenciais após falha na renovação e não insiste sem uma nova sessão", async () => {
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockRejectedValue(new Error("Sessão expirada"));
    const sessao = useEstadoSessao();

    expect(await sessao.restaurar()).toBe(false);
    expect(await sessao.restaurar()).toBe(false);
    expect(clienteHttp.post).toHaveBeenCalledTimes(1);
    expect(sessao.accessToken).toBeNull();
    expect(sessao.conta).toBeNull();
    expect(sessao.restaurada).toBe(true);
    expect(definirTokenAcesso).toHaveBeenCalledWith(null);
  });

  it("limpa a conta local mesmo quando a revogação remota falha", async () => {
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockResolvedValue({ data: respostaSessao } as never);
    vi.mocked(clienteHttp.delete).mockRejectedValue(new Error("Sem rede"));
    const sessao = useEstadoSessao();
    await sessao.entrar(contaAluno.email, "Senha#123");

    await expect(sessao.sair()).rejects.toThrow("Sem rede");

    expect(clienteHttp.delete).toHaveBeenCalledWith("/v1/sessoes/atual", {
      withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" },
    });
    expect(sessao.accessToken).toBeNull();
    expect(sessao.conta).toBeNull();
    expect(sessao.temPlano).toBeNull();
    expect(definirTokenAcesso).toHaveBeenLastCalledWith(null);
  });

  it("cadastra o aluno e abre a sessão com as credenciais enviadas", async () => {
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockResolvedValueOnce({ data: {} } as never)
      .mockResolvedValueOnce({ data: respostaSessao } as never);
    const dados = {
      nome: "Aluno", email: contaAluno.email, cpf: "12345678909",
      telefone: "11999999999", endereco: "Rua A", senha: "Senha#123",
    };

    await useEstadoSessao().cadastrarAluno(dados);

    expect(clienteHttp.post).toHaveBeenNthCalledWith(1, "/v1/contas/alunos", dados, {
      withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" },
    });
    expect(clienteHttp.post).toHaveBeenNthCalledWith(2, "/v1/sessoes", {
      email: dados.email, senha: dados.senha,
    }, { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" } });
  });

  it("renova o token antes do vencimento e usa a sessão nova", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-11-04T12:00:00Z"));
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockResolvedValueOnce({ data: respostaSessao } as never)
      .mockResolvedValueOnce({
        data: { ...respostaSessao, accessToken: "token-2" },
      } as never);
    const sessao = useEstadoSessao();
    await sessao.entrar(contaAluno.email, "Senha#123");
    vi.setSystemTime(new Date("2026-11-04T12:59:40Z"));

    expect(await sessao.restaurar()).toBe(true);
    expect(clienteHttp.post).toHaveBeenLastCalledWith(
      "/v1/sessoes/renovacao", {},
      { withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" } },
    );
    expect(sessao.accessToken).toBe("token-2");
  });

  it("cadastra instituição pela rota específica e autentica a nova conta", async () => {
    prepararCsrfEPlano();
    vi.mocked(clienteHttp.post).mockResolvedValueOnce({ data: {} } as never)
      .mockResolvedValueOnce({
        data: { ...respostaSessao, conta: { ...contaAluno, tipoConta: "INSTITUICAO" } },
      } as never);
    const dados = {
      nomeResponsavel: "Gestora", nomeInstituicao: "Faculdade IVIS",
      email: "gestora@ivis.test", senha: "Senha#123", cnpj: "12345678000100",
      telefone: "11999999999", endereco: "Rua A",
    };

    await useEstadoSessao().cadastrarInstituicao(dados);

    expect(clienteHttp.post).toHaveBeenNthCalledWith(1, "/v1/contas/instituicoes", dados, {
      withCredentials: true, headers: { "X-XSRF-TOKEN": "csrf-1" },
    });
    expect(useEstadoSessao().conta?.tipoConta).toBe("INSTITUICAO");
  });
});
