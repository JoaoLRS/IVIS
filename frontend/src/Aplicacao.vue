<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import MenuLateral from "./componentes/MenuSuperior.vue";
import BarraSuperior from "./componentes/BarraSuperior.vue";
import { useEstadoAplicacao } from "./estados/estadoAplicacao";
import { useEstadoSessao } from "./estados/estadoSessao";

const rota = useRoute();
const menuAberto = ref(false);
const sessao = useEstadoSessao();
const estado = useEstadoAplicacao();
watch(
  () => [sessao.conta?.id, sessao.accessToken] as const,
  ([contaId, token], anterior) => {
    const contaAnterior = anterior?.[0];
    if (contaId !== contaAnterior) estado.limparDadosDaConta();
    const conta = sessao.conta;
    if (conta) {
      estado.atualizarDadosAluno({ nome: conta.nome, email: conta.email });
      if (token && conta.tipoConta === "ALUNO") {
        void estado.carregarPerfil(token, conta.id, () => sessao.conta?.id ?? null).catch(() => {
          // Cada tela apresenta seu próprio estado vazio se a API estiver indisponível.
        });
      }
    }
  },
  { immediate: true },
);
const titulos = computed<Record<string, string>>(() => ({
  inicio: "Início",
  vagas: "Vagas de interesse",
  habilidades: "Minhas habilidades",
  treinos: "Meus treinos",
  historico: "Histórico",
  feedback: "Seu feedback",
  conta:
    sessao.conta?.tipoConta === "INSTITUICAO"
      ? "Dados da instituição"
      : "Dados do aluno",
  planos: "Planos disponíveis",
}));
</script>

<template>
  <RouterView v-if="rota.meta.layout === 'autenticacao'" />
  <div v-else class="app-shell">
    <MenuLateral :aberto="menuAberto" @fechar="menuAberto = false" />
    <main class="main-area">
      <BarraSuperior
        :titulo="titulos[String(rota.name)] ?? 'IVIS'"
        @abrir-menu="menuAberto = true"
      />
      <div class="page-content container-fluid">
        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>
      </div>
    </main>
  </div>
</template>
