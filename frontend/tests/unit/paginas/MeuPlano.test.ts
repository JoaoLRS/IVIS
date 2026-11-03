import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import MeuPlano from "../../../src/paginas/MeuPlano.vue";
import { clienteHttp } from "../../../src/servicos/clienteHttp";

vi.mock("../../../src/servicos/clienteHttp", () => ({
  clienteHttp: { get: vi.fn() },
}));

const planoInstitucional = {
  contratoId: "contrato-1",
  nomePlano: "Plano Acadêmico IVIS",
  origemAcesso: "VINCULO_INSTITUCIONAL",
  expiraEm: "2027-11-02T12:00:00Z",
  beneficios: ["Treinos ilimitados", "Histórico de entrevistas"],
  condicoesTitular: {
    quantidadeVagas: 10,
    precoCentavosContratado: 9000,
    moeda: "BRL",
  },
};

describe("página Meu plano", () => {
  beforeEach(() => vi.mocked(clienteHttp.get).mockReset());

  it("exibe o acesso institucional, a capacidade e os benefícios do plano", async () => {
    vi.mocked(clienteHttp.get).mockResolvedValue({
      data: { planos: [planoInstitucional] },
    } as never);
    const pagina = mount(MeuPlano, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });
    await flushPromises();

    expect(pagina.text()).toContain("Plano Acadêmico IVIS");
    expect(pagina.text()).toContain("Acesso por vínculo institucional");
    expect(pagina.get(".plan-detail-tile:nth-child(2)").text()).toContain("Vadas de alunos ");
    expect(pagina.get(".plan-detail-tile:nth-child(2)").text()).toContain("10");
    expect(pagina.text()).toContain("Treinos ilimitados");
    expect(pagina.text()).toContain("Histórico de entrevistas");
    expect(pagina.find(".plan-access-card").exists()).toBe(true);
    expect(pagina.get(".plan-status").text()).toContain("Vigente");
    expect(pagina.findAll(".plan-details-grid .plan-detail-tile")).toHaveLength(3);
    expect(pagina.findAll(".plan-benefits li")).toHaveLength(2);
    expect(pagina.find('[role="status"]').exists()).toBe(false);
  });

  it("orienta o aluno sem plano a consultar as ofertas", async () => {
    vi.mocked(clienteHttp.get).mockResolvedValue({ data: { planos: [] } } as never);
    const pagina = mount(MeuPlano, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });
    await flushPromises();

    expect(pagina.text()).toContain("Nenhum plano vigente");
    expect(pagina.text()).toContain("Consultar ofertas");
    expect(pagina.find(".plan-message-card--empty").exists()).toBe(true);
  });

  it("permite repetir a consulta quando o serviço falha", async () => {
    vi.mocked(clienteHttp.get)
      .mockRejectedValueOnce(new Error("API indisponível"))
      .mockResolvedValueOnce({ data: { planos: [planoInstitucional] } } as never);
    const pagina = mount(MeuPlano);
    await flushPromises();

    expect(pagina.get('[role="alert"]').text()).toContain(
      "Não foi possível consultar seu plano",
    );
    await pagina.get("button").trigger("click");
    await flushPromises();

    expect(pagina.text()).toContain("Plano Acadêmico IVIS");
    expect(clienteHttp.get).toHaveBeenCalledTimes(2);
  });
});
