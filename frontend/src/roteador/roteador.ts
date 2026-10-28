import { createRouter, createWebHistory } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";

declare module "vue-router" {
  interface RouteMeta {
    publico?: boolean;
    layout?: string;
    exigePlano?: boolean;
    perfil?: "ALUNO" | "INSTITUICAO";
  }
}

const roteador = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "apresentacao",
      component: () => import("../paginas/Apresentacao.vue"),
      meta: { layout: "autenticacao", publico: true },
    },
    {
      path: "/cadastro",
      name: "cadastro",
      component: () => import("../paginas/Cadastro.vue"),
      meta: { layout: "autenticacao", publico: true },
    },
    {
      path: "/login",
      name: "login",
      component: () => import("../paginas/Login.vue"),
      meta: { layout: "autenticacao", publico: true },
    },
    {
      path: "/planos",
      name: "planos",
      component: () => import("../paginas/Planos.vue"),
      meta: { layout: "autenticacao", publico: true },
    },
    {
      path: "/checkout/:planoId",
      name: "checkout",
      component: () => import("../paginas/CheckoutSimulado.vue"),
      meta: { layout: "autenticacao", exigePlano: false },
    },
    {
      path: "/inicio",
      name: "inicio",
      component: () => import("../paginas/Inicio.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/instituicao",
      name: "instituicao",
      component: () => import("../paginas/Instituicao.vue"),
      meta: { perfil: "INSTITUICAO" },
    },
    {
      path: "/vagas",
      name: "vagas",
      component: () => import("../paginas/Vagas.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/habilidades",
      name: "habilidades",
      component: () => import("../paginas/Habilidades.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/treinos",
      name: "treinos",
      component: () => import("../paginas/Treinos.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/historico",
      name: "historico",
      component: () => import("../paginas/Historico.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/feedback",
      name: "feedback",
      component: () => import("../paginas/Feedback.vue"),
      meta: { perfil: "ALUNO" },
    },
    {
      path: "/conta",
      name: "conta",
      component: () => import("../paginas/Conta.vue"),
    },
    { path: "/:caminho(.*)*", redirect: "/" },
  ],
});

export default roteador;

roteador.beforeEach(async (to) => {
  const sessao = useEstadoSessao();
  const autenticado = await sessao.restaurar();

  if (to.meta.publico) {
    if (autenticado && (to.name === "login" || to.name === "cadastro")) {
      return sessao.temPlano
        ? (sessao.conta?.tipoConta === "INSTITUICAO" ? "/instituicao" : "/inicio")
        : "/planos";
    }
    return true;
  }

  if (!autenticado) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.perfil && sessao.conta?.tipoConta !== to.meta.perfil) {
    return { name: sessao.conta?.tipoConta === "INSTITUICAO" ? "instituicao" : "inicio" };
  }
  if (to.meta.exigePlano !== false && !sessao.temPlano) {
    return { name: "planos", query: { acesso: "necessario" } };
  }
  return true;
});
