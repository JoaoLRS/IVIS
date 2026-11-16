import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { clienteHttp, definirTokenAcesso, requisicoesEmAndamento } from "../../src/servicos/clienteHttp";

const chamadas: InternalAxiosRequestConfig[] = [];
const adapter: AxiosAdapter = async (config) => {
  chamadas.push(config);
  return { data: {}, status: 200, statusText: "OK", headers: {}, config };
};

describe("cliente HTTP", () => {
  afterEach(() => {
    chamadas.length = 0;
    definirTokenAcesso(null);
  });

  it("anexa token somente a rotas privadas e não o envia na renovação da sessão", async () => {
    definirTokenAcesso("token-antigo");
    await clienteHttp.get("/v1/treinos", { adapter });
    await clienteHttp.get("/v1/sessoes/csrf", { adapter });
    await clienteHttp.post("/v1/sessoes/renovacao", {}, { adapter });

    expect(chamadas[0].headers.get("Authorization")).toBe("Bearer token-antigo");
    expect(chamadas[1].headers.get("Authorization")).toBeUndefined();
    expect(chamadas[2].headers.get("Authorization")).toBeUndefined();
  });

  it("remove o token de chamadas futuras ao encerrar a sessão", async () => {
    definirTokenAcesso("token-1");
    await clienteHttp.get("/v1/conta", { adapter });
    definirTokenAcesso(null);
    await clienteHttp.get("/v1/conta", { adapter });

    expect(chamadas[0].headers.get("Authorization")).toBe("Bearer token-1");
    expect(chamadas[1].headers.get("Authorization")).toBeUndefined();
  });

  it("mantém a interface ocupada até todas as chamadas visíveis terminarem", async () => {
    const liberacoes: Array<() => void> = [];
    const lento: AxiosAdapter = (config) => new Promise((resolve) => {
      liberacoes.push(() => resolve({ data: {}, status: 200, statusText: "OK", headers: {}, config }));
    });
    const primeira = clienteHttp.get("/v1/planos", { adapter: lento });
    const segunda = clienteHttp.get("/v1/treinos", { adapter: lento });
    await vi.waitFor(() => expect(liberacoes).toHaveLength(2));
    expect(requisicoesEmAndamento.value).toBe(2);
    liberacoes[0]();
    await primeira;
    expect(requisicoesEmAndamento.value).toBe(1);
    liberacoes[1]();
    await segunda;
    expect(requisicoesEmAndamento.value).toBe(0);
  });

  it("libera a interface após erro e ignora manutenção da sessão", async () => {
    const falha: AxiosAdapter = (config) => Promise.reject(new AxiosError("Sem rede", "ERR_NETWORK", config));
    await expect(clienteHttp.get("/v1/planos", { adapter: falha })).rejects.toThrow("Sem rede");
    expect(requisicoesEmAndamento.value).toBe(0);
    await clienteHttp.post("/v1/sessoes/atividade", {}, { adapter });
    expect(requisicoesEmAndamento.value).toBe(0);
  });
});
