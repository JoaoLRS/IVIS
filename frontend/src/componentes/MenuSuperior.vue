<script setup lang="ts">
import {
  Activity,
  BriefcaseBusiness,
  Code2,
  MessageSquareText,
  UserRound,
} from "lucide-vue-next";
import { RouterLink } from "vue-router";
import type { Component } from "vue";
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";

defineProps<{ aberto: boolean }>();
const emit = defineEmits<{ fechar: [] }>();
const estado = useEstadoAplicacao();
const menuRef = ref<HTMLElement | null>(null);
type LinkMenu = { rota: string; rotulo: string; icone: Component };
const grupos: { rotulo: string; links: LinkMenu[] }[] = [
  {
    rotulo: "Seu desenvolvimento",
    links: [
      { rota: "treinos", rotulo: "Meus treinos", icone: MessageSquareText },
      { rota: "vagas", rotulo: "Vagas de interesse", icone: BriefcaseBusiness },
      { rota: "habilidades", rotulo: "Minhas habilidades", icone: Code2 },
      { rota: "historico", rotulo: "Histórico", icone: Activity },
    ],
  },
];

function fecharGruposAoClicarFora(evento: PointerEvent) {
  const alvo = evento.target;
  if (!(alvo instanceof Node)) return;

  menuRef.value
    ?.querySelectorAll<HTMLDetailsElement>(".nav-section-menu[open]")
    .forEach((grupo) => {
      if (!grupo.contains(alvo)) grupo.open = false;
    });
}

onMounted(() =>
  document.addEventListener("pointerdown", fecharGruposAoClicarFora),
);
onBeforeUnmount(() =>
  document.removeEventListener("pointerdown", fecharGruposAoClicarFora),
);
</script>

<template>
  <button
    v-if="aberto"
    class="sidebar-backdrop d-lg-none"
    aria-label="Fechar menu"
    @click="emit('fechar')"
  />
  <aside
    ref="menuRef"
    class="app-sidebar app-top-menu"
    :class="{ show: aberto }"
  >
    <RouterLink
      class="brand d-flex align-items-center text-decoration-none"
      to="/inicio"
      @click="emit('fechar')"
    >
      IVIS
    </RouterLink>
    <nav class="sidebar-nav">
      <RouterLink
        class="top-menu-text-link"
        to="/treinos"
        @click="emit('fechar')"
      >
        Iniciar treinamento
      </RouterLink>
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
    <RouterLink
      class="top-menu-profile"
      to="/conta"
      aria-label="Editar dados do aluno"
      @click="emit('fechar')"
    >
      <UserRound :size="18" aria-hidden="true" />
      <span>Aluno - {{ estado.nomeAluno }}</span>
    </RouterLink>
  </aside>
</template>
