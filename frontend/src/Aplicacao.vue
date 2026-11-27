<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import roteador, { navegacaoEmAndamento } from "./roteador/roteador";
import MenuLateral from "./componentes/MenuSuperior.vue";
import BarraSuperior from "./componentes/BarraSuperior.vue";
import { useEstadoAplicacao } from "./estados/estadoAplicacao";
import { useEstadoSessao } from "./estados/estadoSessao";
import {
  mensagemCarregamentoGlobal,
  requisicoesEmAndamento,
} from "./servicos/clienteHttp";

const rota = useRoute();
const carregandoGlobal = computed(() => requisicoesEmAndamento.value > 0 || navegacaoEmAndamento.value);
const menuAberto = ref(false);
const navegacaoPronta = ref(false);
void roteador.isReady().then(() => {
  navegacaoPronta.value = true;
});
const sessao = useEstadoSessao();
onMounted(() => {
  void sessao.restaurar();
});
const estado = useEstadoAplicacao();
watch(
  () => [sessao.conta?.id, sessao.accessToken, sessao.temPlano] as const,
  ([contaId, token], anterior) => {
    const contaAnterior = anterior?.[0];
    if (contaId !== contaAnterior) estado.limparDadosDaConta();
    const conta = sessao.conta;
    if (conta) {
      estado.atualizarDadosAluno({ nome: conta.nome, email: conta.email });
      if (token && conta.tipoConta === "ALUNO" && sessao.temPlano !== null) {
        void (
          sessao.temPlano
            ? estado.carregarPerfil(
                token,
                conta.id,
                () => sessao.conta?.id ?? null,
              )
            : estado.carregarHistorico(conta.id, () => sessao.conta?.id ?? null)
        ).catch(() => {
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
  "alterar-senha": "Alterar senha",
  planos: "Planos disponíveis",
  "meu-plano": "Meu plano",
  instituicao: "Área da instituição",
}));
</script>

<template>
  <div :inert="carregandoGlobal" :aria-busy="carregandoGlobal">
    <RouterView v-if="navegacaoPronta && rota.meta.layout === 'autenticacao'" />
    <div v-else-if="navegacaoPronta" class="app-shell">
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
  </div>
  <div
    v-if="carregandoGlobal"
    class="global-loading-overlay"
    :class="{
      'global-loading-overlay--avaliando':
        mensagemCarregamentoGlobal === 'Avaliando a sua entrevista',
    }"
    role="status"
    aria-live="polite"
  >
    <span class="global-loading-spinner" aria-hidden="true" />
    <span class="global-loading-label">
      <template v-if="mensagemCarregamentoGlobal === 'Avaliando a sua entrevista'">
        Avaliando a sua<br />entrevista
      </template>
      <template v-else>{{ mensagemCarregamentoGlobal }}</template>
    </span>
  </div>
</template>
