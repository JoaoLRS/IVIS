<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import MenuLateral from "./componentes/MenuSuperior.vue";
import BarraSuperior from "./componentes/BarraSuperior.vue";

const rota = useRoute();
const menuAberto = ref(false);
const titulos: Record<string, string> = {
  inicio: "Início",
  vagas: "Vagas de interesse",
  habilidades: "Minhas habilidades",
  treinos: "Meus treinos",
  historico: "Histórico",
  feedback: "Seu feedback",
  conta: "Dados do aluno",
};
function notificar() {
  window.alert("Você está em dia!");
}
</script>

<template>
  <div class="app-shell">
    <MenuLateral :aberto="menuAberto" @fechar="menuAberto = false" />
    <main class="main-area">
      <BarraSuperior
        :titulo="titulos[String(rota.name)] ?? 'IVIS'"
        @abrir-menu="menuAberto = true"
      />
      <div class="page-content container-fluid">
        <RouterView v-slot="{ Component }"
          ><component :is="Component" @notificar="notificar"
        /></RouterView>
      </div>
    </main>
  </div>
</template>
