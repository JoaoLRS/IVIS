import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CheckoutSimulado from "../../../src/paginas/CheckoutSimulado.vue";
import { useEstadoSessao } from "../../../src/estados/estadoSessao";
import { clienteHttp } from "../../../src/servicos/clienteHttp";
import { simularContratacao } from "../../../src/servicos/contratacoes";
import { obterTokenCsrf } from "../../../src/servicos/perfilAluno";

vi.mock("vue-router", async (importOriginal) => ({
  ...(await importOriginal<typeof import("vue-router")>()),
  useRoute: () => ({ params: { planoId: "plano-1" } }),
  useRouter: () => ({ replace: vi.fn() }),
}));
vi.mock("../../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));
vi.mock("../../../src/servicos/clienteHttp", () => ({ clienteHttp: { get: vi.fn() } }));
vi.mock("../../../src/servicos/perfilAluno", () => ({ obterTokenCsrf: vi.fn() }));
vi.mock("../../../src/servicos/contratacoes", () => ({ simularContratacao: vi.fn() }));

const consultarPlano = vi.fn();
const sessao = { conta: { tipoConta: "ALUNO" }, consultarPlano, sair: vi.fn() };
const oferta = {
  id: "plano-1", modalidade: "INDIVIDUAL", nome: "Individual",
  descricao: "Plano individual", precoCentavos: 2900, moeda: "BRL",
  duracaoDias: 30, vagasIncluidas: 0, beneficios: ["Treinos"],
};
const resultado = {
  contratoId: "contrato-1", pagamentoId: "pagamento-1", planoId: "plano-1",
  nomePlano: "Individual", situacaoContrato: "ATIVO", modoPagamento: "SIMULADO",
  valorCentavos: 2900, moeda: "BRL", expiraEm: "2027-12-01T00:00:00Z",
  reprocessada: false, emailConfirmacao: null,
};

async function montar() {
  const pagina = mount(CheckoutSimulado, {
    global: { stubs: { RouterLink: true } },
  });
  await flushPromises();
  return pagina;
}

describe("checkout simulado", () => {
  beforeEach(() => {
    vi.mocked(clienteHttp.get).mockReset().mockResolvedValue({ data: oferta } as never);
    vi.mocked(obterTokenCsrf).mockReset().mockResolvedValue("csrf-1");
    vi.mocked(simularContratacao).mockReset();
    consultarPlano.mockReset().mockResolvedValue(true);
    sessao.conta.tipoConta = "ALUNO";
    vi.mocked(useEstadoSessao).mockReturnValue(sessao as never);
  });

  it("impede contratar oferta de modalidade incompatível", async () => {
    sessao.conta.tipoConta = "INSTITUICAO";
    const pagina = await montar();

    expect(pagina.get('[role="alert"]').text()).toContain("não corresponde ao tipo da sua conta");
    expect(pagina.find("form").exists()).toBe(false);
    expect(simularContratacao).not.toHaveBeenCalled();
  });

  it("envia cartão de teste com CSRF e confirma o acesso após aprovação", async () => {
    vi.mocked(simularContratacao).mockResolvedValue({
      ...resultado, situacaoPagamento: "APROVADO",
    } as never);
    const pagina = await montar();
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(simularContratacao).toHaveBeenCalledWith(
      { planoId: "plano-1", numeroCartao: "4242 4242 4242 4242", validade: "12/30", cvv: "123" },
      expect.any(String), "csrf-1",
    );
    expect(consultarPlano).toHaveBeenCalledOnce();
    expect(pagina.get(".checkout-result-success").text()).toContain("Simulação aprovada");
    expect(pagina.find("form").exists()).toBe(false);
  });

  it("não ativa o plano quando a simulação é recusada e permite tentar novamente", async () => {
    vi.mocked(simularContratacao).mockResolvedValue({
      ...resultado, situacaoPagamento: "RECUSADO",
    } as never);
    const pagina = await montar();
    await pagina.get(".checkout-scenario:nth-child(2)").trigger("click");
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(simularContratacao).toHaveBeenCalledWith(
      expect.objectContaining({ numeroCartao: "4000 0000 0000 0002" }),
      expect.any(String), "csrf-1",
    );
    expect(consultarPlano).not.toHaveBeenCalled();
    expect(pagina.get(".checkout-result-declined").text()).toContain("Simulação recusada");
    await pagina.get(".checkout-retry").trigger("click");
    expect(pagina.find("form").exists()).toBe(true);
  });

  it("mostra a mensagem e a referência do servidor quando a contratação falha", async () => {
    vi.mocked(simularContratacao).mockRejectedValue({
      response: { data: { mensagem: "Pagamento indisponível", idRequisicao: "req-1" } },
    });
    const pagina = await montar();
    await pagina.get("form").trigger("submit");
    await flushPromises();

    expect(pagina.get('[role="alert"]').text()).toContain("Pagamento indisponível (referência: req-1)");
    expect(pagina.get(".checkout-submit").attributes("disabled")).toBeUndefined();
    expect(consultarPlano).not.toHaveBeenCalled();
  });
});
