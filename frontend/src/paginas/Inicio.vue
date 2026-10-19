<script setup lang="ts">
import {
  ArrowRight,
  BriefcaseBusiness,
  Code2,
  History,
  MessageSquareText,
} from "lucide-vue-next";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";

const estado = useEstadoAplicacao();
</script>

<template>
  <CabecalhoPagina
    eyebrow="MÓDULO FRONTEND · DEMONSTRAÇÃO"
    titulo="Treine para sua próxima entrevista"
    descricao="Gerencie seu perfil técnico e suas vagas de interesse, pratique entrevistas por texto e consulte seus resultados."
  />
  <div class="row g-3 mb-4">
    <div class="col-sm-6 col-xl-3">
      <RouterLink to="/habilidades" class="card h-100 text-decoration-none"
        ><div class="card-body">
          <span class="skill-icon"><Code2 :size="18" /></span>
          <h2 class="panel-title mt-3">Habilidades</h2>
          <p class="text-secondary mb-0">
            {{ estado.habilidades.length }} cadastradas · editar perfil
          </p>
        </div></RouterLink
      >
    </div>
    <div class="col-sm-6 col-xl-3">
      <RouterLink to="/vagas" class="card h-100 text-decoration-none"
        ><div class="card-body">
          <span class="skill-icon"><BriefcaseBusiness :size="18" /></span>
          <h2 class="panel-title mt-3">Vagas de interesse</h2>
          <p class="text-secondary mb-0">
            {{ estado.vagas.length }} cadastradas · preparar treino
          </p>
        </div></RouterLink
      >
    </div>
    <div class="col-sm-6 col-xl-3">
      <RouterLink to="/treinos" class="card h-100 text-decoration-none"
        ><div class="card-body">
          <span class="skill-icon"><MessageSquareText :size="18" /></span>
          <h2 class="panel-title mt-3">Simulação</h2>
          <p class="text-secondary mb-0">Entrevista técnica por texto</p>
        </div></RouterLink
      >
    </div>
    <div class="col-sm-6 col-xl-3">
      <RouterLink to="/historico" class="card h-100 text-decoration-none"
        ><div class="card-body">
          <span class="skill-icon"><History :size="18" /></span>
          <h2 class="panel-title mt-3">Histórico</h2>
          <p class="text-secondary mb-0">
            {{ estado.historico.length }} resultados registrados
          </p>
        </div></RouterLink
      >
    </div>
  </div>
  <section class="card">
    <div class="card-body">
      <div class="panel-heading">
        <div>
          <div class="eyebrow">ÚLTIMOS RESULTADOS</div>
          <h2 class="panel-title">Seu histórico de treinamento</h2>
        </div>
        <RouterLink class="btn btn-link" to="/historico"
          >Ver histórico <ArrowRight :size="14"
        /></RouterLink>
      </div>
      <div
        v-for="(treino, indice) in estado.historico.slice(0, 3)"
        :key="`${treino.titulo}-${indice}`"
        class="compact-row"
      >
        <span class="history-symbol"><MessageSquareText :size="16" /></span>
        <div class="row-copy">
          <strong>{{ treino.titulo }}</strong
          ><small>{{ treino.data }} · {{ treino.contexto }}</small>
        </div>
        <strong class="match-score">{{ treino.nota }}/100</strong>
      </div>
    </div>
  </section>
  <p class="small text-secondary mt-3 mb-0">
    Interface de demonstração do frontend. Os dados são mantidos apenas na
    memória do navegador.
  </p>
</template>
