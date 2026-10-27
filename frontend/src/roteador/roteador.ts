import { createRouter, createWebHistory } from "vue-router";

const roteador = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: "/inicio",
    },
    {
      path: "/cadastro",
      name: "cadastro",
      component: () => import("../paginas/Cadastro.vue"),
      meta: { layout: "autenticacao" },
    },
    {
      path: "/inicio",
      name: "inicio",
      component: () => import("../paginas/Inicio.vue"),
    },
    {
      path: "/vagas",
      name: "vagas",
      component: () => import("../paginas/Vagas.vue"),
    },
    {
      path: "/habilidades",
      name: "habilidades",
      component: () => import("../paginas/Habilidades.vue"),
    },
    {
      path: "/treinos",
      name: "treinos",
      component: () => import("../paginas/Treinos.vue"),
    },
    {
      path: "/historico",
      name: "historico",
      component: () => import("../paginas/Historico.vue"),
    },
    {
      path: "/feedback",
      name: "feedback",
      component: () => import("../paginas/Feedback.vue"),
    },
    {
      path: "/conta",
      name: "conta",
      component: () => import("../paginas/Conta.vue"),
    },
    { path: "/:caminho(.*)*", redirect: "/inicio" },
  ],
});

export default roteador;

