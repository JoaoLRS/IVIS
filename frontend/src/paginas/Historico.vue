<script setup lang="ts">
import {
  Clock3,
  MessageSquareText,
  Plus,
  TrendingUp,
  Zap,
} from "lucide-vue-next";
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
const estado = useEstadoAplicacao();
const roteador = useRouter();
const mediaNotas = computed(() => {
  if (!estado.historico.length) return 0;
  return Math.round(estado.historico.reduce((total, treino) => total + treino.nota, 0) / estado.historico.length);
});
</script>

<template>
  <CabecalhoPagina
    eyebrow="SUA JORNADA"
    titulo="Histórico de treinos"
    descricao="Acompanhe suas práticas e veja sua evolução ao longo do tempo."
    ><button class="btn btn-primary" @click="roteador.push('/treinos')">
      <Plus :size="16" /> Novo treino
    </button></CabecalhoPagina
  >
  <div class="row g-3 mb-3">
    <div class="col-md-4">
      <div class="history-stat card">
        <MessageSquareText /><span
          ><small>TREINOS REALIZADOS</small
          ><strong>{{ estado.historico.length }}</strong></span
        >
      </div>
    </div>
    <div class="col-md-4">
      <div class="history-stat card">
        <TrendingUp /><span
          ><small>PONTUAÇÃO MÉDIA</small
          ><strong>{{ mediaNotas || "—" }} <small v-if="mediaNotas">/100</small></strong></span
        >
      </div>
    </div>
    <div class="col-md-4">
      <div class="history-stat card">
        <Zap /><span
          ><small>REGISTROS</small><strong>{{ estado.historico.length }}</strong></span
        >
      </div>
    </div>
  </div>
  <section class="card">
    <div class="card-body">
      <div class="panel-heading">
        <div>
          <h2 class="panel-title">Treinos recentes</h2>
          <p>Suas entrevistas mais recentes aparecem aqui.</p>
        </div>
        <select class="form-select form-select-sm history-filter">
          <option>Todos os resultados</option>
          <option>Acima de 80</option>
        </select>
      </div>
      <RouterLink
        v-for="(treino, indice) in estado.historico"
        :key="`${treino.titulo}-${indice}`"
        class="history-list-row"
        :to="{ path: '/feedback', query: { indice } }"
        ><span class="history-symbol"><MessageSquareText :size="17" /></span
        ><span class="row-copy"
          ><strong>{{ treino.titulo }}</strong
          ><small>{{ treino.data }} · {{ treino.duracao }}</small
          ><small class="d-block">{{ treino.contexto }}</small></span
        ><span class="history-result"
          ><small>RESULTADO</small
          ><strong>{{ treino.nota }}<small>/100</small></strong></span
        ><span class="result-label">{{
          treino.nota >= 80 ? "Ótimo resultado" : "Boa evolução"
        }}</span></RouterLink
      >
    </div>
  </section>
  <div class="d-flex align-items-center gap-2 text-secondary small mt-3">
    <Clock3 :size="14" /> Seu histórico é persistido e isolado por conta.
  </div>
</template>
