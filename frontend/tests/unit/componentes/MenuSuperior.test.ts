import { mount } from "@vue/test-utils";
import { nextTick, ref } from "vue";
import { describe, expect, it, vi } from "vitest";
import MenuSuperior from "../../../src/componentes/MenuSuperior.vue";

const rotaAtual = ref({ fullPath: "/vagas" });

vi.mock("vue-router", async (importOriginal) => ({
  ...(await importOriginal<typeof import("vue-router")>()),
  useRouter: () => ({ currentRoute: rotaAtual, replace: vi.fn() }),
}));
vi.mock("../../../src/estados/estadoSessao", () => ({
  useEstadoSessao: () => ({
    conta: { nome: "Poppy", tipoConta: "ALUNO" },
    sair: vi.fn(),
  }),
}));

describe("menu superior", () => {
  it("mantém apenas um menu aberto por vez", async () => {
    const menu = mount(MenuSuperior, {
      props: { aberto: false },
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });
    const desenvolvimento = menu.get(".nav-section-menu").element as HTMLDetailsElement;
    const perfil = menu.get(".top-menu-profile").element as HTMLDetailsElement;

    desenvolvimento.open = true;
    perfil.open = true;
    await menu.get(".top-menu-profile").trigger("toggle");
    expect(perfil.open).toBe(true);
    expect(desenvolvimento.open).toBe(false);

    desenvolvimento.open = true;
    await menu.get(".nav-section-menu").trigger("toggle");
    expect(desenvolvimento.open).toBe(true);
    expect(perfil.open).toBe(false);

    menu.unmount();
  });

  it("fecha Minha conta ao clicar fora, escolher um link ou navegar", async () => {
    const menu = mount(MenuSuperior, {
      props: { aberto: false },
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });
    const perfil = menu.get(".top-menu-profile").element as HTMLDetailsElement;

    perfil.open = true;
    document.dispatchEvent(new Event("pointerdown"));
    expect(perfil.open).toBe(false);

    perfil.open = true;
    await menu.get(".top-menu-profile-options a").trigger("click");
    expect(perfil.open).toBe(false);

    perfil.open = true;
    rotaAtual.value = { fullPath: "/feedback" };
    await nextTick();
    expect(perfil.open).toBe(false);

    menu.unmount();
  });
});
