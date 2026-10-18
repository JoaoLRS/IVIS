<script setup lang="ts">
import {
  Activity,
  BriefcaseBusiness,
  Code2,
  LayoutDashboard,
  MessageSquareText,
  Sparkles,
} from "lucide-vue-next";
import { RouterLink } from "vue-router";
import type { Component } from "vue";

defineProps<{ aberto: boolean }>();
const emit = defineEmits<{ fechar: [] }>();
type LinkMenu = { rota: string; rotulo: string; icone: Component };
const grupos: { rotulo: string; links: LinkMenu[] }[] = [
  {
    rotulo: "VISÃO GERAL",
    links: [{ rota: "inicio", rotulo: "Início", icone: LayoutDashboard }],
  },
  {
    rotulo: "SEU DESENVOLVIMENTO",
    links: [
      { rota: "treinos", rotulo: "Meus treinos", icone: MessageSquareText },
      { rota: "vagas", rotulo: "Vagas de interesse", icone: BriefcaseBusiness },
      { rota: "habilidades", rotulo: "Minhas habilidades", icone: Code2 },
      { rota: "historico", rotulo: "Histórico", icone: Activity },
    ],
  },
];
</script>

<template>
  <button
    v-if="aberto"
    class="sidebar-backdrop d-lg-none"
    aria-label="Fechar menu"
    @click="emit('fechar')"
  />
  <aside class="app-sidebar app-top-menu d-flex flex-column" :class="{ show: aberto }">
    <RouterLink
      class="brand d-flex align-items-center gap-2 text-decoration-none"
      to="/inicio"
      @click="emit('fechar')"
      ><span class="brand-icon"><Sparkles :size="18" /></span
      ><span>ivis<span class="brand-period">.</span></span
      ><span class="brand-label">ALUNO</span></RouterLink
    >
    <button
      class="workspace-switch btn btn-light d-flex align-items-center text-start gap-2"
    >
      <span class="workspace-avatar">A</span
      ><span class="flex-grow-1"
        ><strong>Minha jornada</strong
        ><small class="d-block">Treinamento</small></span
      ><span class="text-secondary">⌄</span>
    </button>
        <nav class="sidebar-nav flex-grow-1">
      <details
        v-for="grupo in grupos"
        :key="grupo.rotulo"
        class="nav-section nav-section-menu"
      >
        <summary class="nav-section-title">
          {{ grupo.rotulo }}
        </summary>

        <div class="nav-section-links">
          <RouterLink
            v-for="link in grupo.links"
            :key="link.rota"
            :to="`/${link.rota}`"
            class="sidebar-link btn w-100 d-flex align-items-center gap-2 text-start"
            active-class="active"
            @click="emit('fechar')"
          >
            <component :is="link.icone" :size="17" />
            <span>{{ link.rotulo }}</span>
          </RouterLink>
        </div>
      </details>
    </nav>
    <div class="user-profile d-flex align-items-center gap-2">
      <span class="user-avatar">A</span>
      <span
        ><strong>Área do aluno</strong
        ><small class="d-block">Demonstração local</small></span
      >
    </div>
  </aside>
</template>
