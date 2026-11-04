import { beforeEach, describe, expect, it, vi } from "vitest";
import roteador, { verificarAcesso } from "../../src/roteador/roteador";
import { useEstadoSessao } from "../../src/estados/estadoSessao";

vi.mock("../../src/estados/estadoSessao", () => ({ useEstadoSessao: vi.fn() }));

const restaurar = vi.fn();
const sessao = {
  restaurar,
  temPlano: false,
  conta: null as null | { tipoConta: "ALUNO" | "INSTITUICAO" },
};

async function acesso(caminho: string) {
  return verificarAcesso(roteador.resolve(caminho) as never);
}

describe("proteção das rotas", () => {
  beforeEach(() => {
    restaurar.mockReset();
    restaurar.mockResolvedValue(false);
    sessao.temPlano = false;
    sessao.conta = null;
    vi.mocked(useEstadoSessao).mockReturnValue(sessao as never);
  });

  it("preserva o destino ao enviar visitante para o login", async () => {
    expect(await acesso("/treinos?vaga=abc")).toEqual({
      name: "login", query: { redirect: "/treinos?vaga=abc" },
    });
    expect(restaurar).toHaveBeenCalledOnce();
  });

  it("permite páginas públicas e leva usuário autenticado para sua área", async () => {
    expect(await acesso("/redefinir-senha")).toBe(true);
    restaurar.mockResolvedValue(true);
    sessao.temPlano = true;
    sessao.conta = { tipoConta: "INSTITUICAO" };
    expect(await acesso("/cadastro")).toBe("/instituicao");
    sessao.conta = { tipoConta: "ALUNO" };
    expect(await acesso("/login")).toBe("/inicio");
    sessao.temPlano = false;
    expect(await acesso("/login")).toBe("/planos");
  });

  it("barra acesso ao perfil de outro tipo de conta", async () => {
    restaurar.mockResolvedValue(true);
    sessao.temPlano = true;
    sessao.conta = { tipoConta: "ALUNO" };
    expect(await acesso("/instituicao")).toEqual({ name: "inicio" });
    sessao.conta = { tipoConta: "INSTITUICAO" };
    expect(await acesso("/vagas")).toEqual({ name: "instituicao" });
  });

  it("exige plano nas áreas de trabalho, mas permite conta e checkout", async () => {
    restaurar.mockResolvedValue(true);
    sessao.conta = { tipoConta: "ALUNO" };
    expect(await acesso("/treinos")).toEqual({
      name: "planos", query: { acesso: "necessario" },
    });
    expect(await acesso("/conta")).toBe(true);
    expect(await acesso("/checkout/plano-1")).toBe(true);
    sessao.temPlano = true;
    expect(await acesso("/treinos")).toBe(true);
  });
});
