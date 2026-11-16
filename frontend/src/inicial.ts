import { createApp } from "vue";
import { createPinia } from "pinia";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap";
import "./estilos.css";
import Aplicacao from "./Aplicacao.vue";
import roteador from "./roteador/roteador";

window.addEventListener("vite:preloadError", (evento) => {
  const chave = "ivis:ultimo-recarregamento-de-versao";
  const agora = Date.now();
  const ultimoRecarregamento = Number(sessionStorage.getItem(chave) ?? "0");
  if (agora - ultimoRecarregamento < 10_000) return;

  evento.preventDefault();
  sessionStorage.setItem(chave, String(agora));
  window.location.reload();
});

createApp(Aplicacao).use(createPinia()).use(roteador).mount("#root");
