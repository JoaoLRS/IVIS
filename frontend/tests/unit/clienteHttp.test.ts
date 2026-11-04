import type { AxiosAdapter, InternalAxiosRequestConfig } from "axios";
import { afterEach, describe, expect, it } from "vitest";
import { clienteHttp, definirTokenAcesso } from "../../src/servicos/clienteHttp";

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
});
