import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import RequisitosSenha from "../../src/componentes/RequisitosSenha.vue";
import { senhaValida } from "../../src/validacao/senha";

describe("requisitos de senha", () => {
  it("exige oito caracteres, maiúscula, minúscula, número e especial", () => {
    for (const senha of ["Aa1!aaa", "aaaaaaa1!", "AAAAAAA1!", "Abcdefg!", "Abcdefg1"]) {
      expect(senhaValida(senha)).toBe(false);
    }
    expect(senhaValida("Aa1!abcd")).toBe(true);
    expect(senhaValida("Áa1!abcd")).toBe(true);
    expect(senhaValida("Aa1!" + "x".repeat(125))).toBe(false);
  });

  it("marca cada requisito em verde assim que é atendido", async () => {
    const componente = mount(RequisitosSenha, { props: { id: "requisitos", senha: "" } });
    expect(componente.findAll(".password-requirement-met")).toHaveLength(0);

    await componente.setProps({ senha: "Aa1!" });
    expect(componente.findAll(".password-requirement-met")).toHaveLength(4);
    expect(componente.text()).toContain("Pelo menos 8 caracteres");

    await componente.setProps({ senha: "Aa1!abcd" });
    expect(componente.findAll(".password-requirement-met")).toHaveLength(5);
  });

  it("atualiza cada indicador ao digitar e remover caracteres", async () => {
    const componente = mount(RequisitosSenha, { props: { id: "requisitos", senha: "" } });
    for (const [senha, atendidos] of [
      ["A", ["Uma letra maiúscula"]],
      ["Aa", ["Uma letra maiúscula", "Uma letra minúscula"]],
      ["Aa1", ["Uma letra maiúscula", "Uma letra minúscula", "Um número"]],
      ["Aa1!", ["Uma letra maiúscula", "Uma letra minúscula", "Um número", "Um caractere especial"]],
      ["Aa1!abcd", ["Pelo menos 8 caracteres", "Uma letra maiúscula", "Uma letra minúscula", "Um número", "Um caractere especial"]],
      ["a1!abcd", ["Uma letra minúscula", "Um número", "Um caractere especial"]],
    ] as const) {
      await componente.setProps({ senha });
      expect(componente.findAll(".password-requirement-met").map((item) => item.text())).toEqual(
        atendidos.map((texto) => `✓ ${texto}`),
      );
    }
  });
});
