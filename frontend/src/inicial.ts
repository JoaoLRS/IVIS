import { createApp } from "vue";
import { createPinia } from "pinia";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap";
import "./estilos.css";
import Aplicacao from "./Aplicacao.vue";
import roteador from "./roteador/roteador";

createApp(Aplicacao).use(createPinia()).use(roteador).mount("#root");
