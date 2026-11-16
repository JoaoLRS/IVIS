<script setup lang="ts">
import {
  Activity,
  BriefcaseBusiness,
  Code2,
  MessageSquareText,
  LogOut,
} from "lucide-vue-next";
import { RouterLink, useRouter } from "vue-router";
import type { Component } from "vue";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useEstadoSessao } from "../estados/estadoSessao";

defineProps<{ aberto: boolean }>();
const emit = defineEmits<{ fechar: [] }>();
const sessao = useEstadoSessao();
const primeiroNome = computed(
  () => sessao.conta?.nome.trim().split(/\s+/)[0] || "Conta",
);
const roteador = useRouter();
const menuRef = ref<HTMLElement | null>(null);
type LinkMenu = { rota: string; rotulo: string; icone?: Component };
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
const gruposInstituicao: { rotulo: string; links: LinkMenu[] }[] = [
  {
    rotulo: "Gestão institucional",
    links: [{ rota: "instituicao", rotulo: "Gerenciar alunos" }],
  },
];

async function sair() {
  await sessao.sair();
  emit("fechar");
  await roteador.replace("/login");
}

function fecharMenus() {
  menuRef.value
    ?.querySelectorAll<HTMLDetailsElement>(
      ".nav-section-menu[open], .top-menu-profile[open]",
    )
    .forEach((menu) => {
      menu.open = false;
    });
}

function fecharMenusAoClicarFora(evento: PointerEvent) {
  const alvo = evento.target;
  if (!(alvo instanceof Node)) return;

  menuRef.value
    ?.querySelectorAll<HTMLDetailsElement>(
      ".nav-section-menu[open], .top-menu-profile[open]",
    )
    .forEach((menu) => {
      if (!menu.contains(alvo)) menu.open = false;
    });
}

function fecharOutrosMenus(evento: Event) {
  const aberto = evento.target;
  if (!(aberto instanceof HTMLDetailsElement) || !aberto.open) return;
  menuRef.value
    ?.querySelectorAll<HTMLDetailsElement>(
      ".nav-section-menu[open], .top-menu-profile[open]",
    )
    .forEach((menu) => {
      if (menu !== aberto) menu.open = false;
    });
}

watch(() => roteador.currentRoute.value.fullPath, fecharMenus);

onMounted(() =>
  document.addEventListener("pointerdown", fecharMenusAoClicarFora),
);
onBeforeUnmount(() =>
  document.removeEventListener("pointerdown", fecharMenusAoClicarFora),
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
      :to="
        sessao.conta?.tipoConta === 'INSTITUICAO' ? '/instituicao' : '/inicio'
      "
      @click="emit('fechar')"
    >
      IVIS<span class="brand-period">.</span>
    </RouterLink>
    <nav class="sidebar-nav">
      <RouterLink
        v-if="sessao.conta?.tipoConta === 'ALUNO'"
        class="top-menu-text-link"
        to="/treinos"
        @click="emit('fechar')"
      >
        Iniciar treinamento
      </RouterLink>
      <details
        v-for="grupo in sessao.conta?.tipoConta === 'INSTITUICAO'
          ? gruposInstituicao
          : grupos"
        :key="grupo.rotulo"
        class="nav-section nav-section-menu"
        @toggle="fecharOutrosMenus"
      >
        <summary class="nav-section-title">
          {{ grupo.rotulo }}
        </summary>

        <div class="nav-section-links">
          <RouterLink
            v-for="link in grupo.links"
            :key="link.rota"
            :to="`/${link.rota}`"
            :class="
              link.icone
                ? 'sidebar-link btn w-100 d-flex align-items-center gap-2 text-start'
                : 'institution-menu-link'
            "
            active-class="active"
            @click="emit('fechar')"
          >
            <component :is="link.icone" v-if="link.icone" :size="17" />
            <span>{{ link.rotulo }}</span>
          </RouterLink>
        </div>
      </details>
      <div class="mobile-profile-links">
        <RouterLink to="/conta" @click="emit('fechar')">Meus dados</RouterLink>
        <RouterLink to="/meu-plano" @click="emit('fechar')"
          >Meu plano</RouterLink
        >
        <RouterLink to="/alterar-senha" @click="emit('fechar')"
          >Alterar senha</RouterLink
        >
        <button type="button" @click="sair">Sair</button>
      </div>
    </nav>
    <div class="top-menu-account">
      <details class="top-menu-profile" @toggle="fecharOutrosMenus">
        <summary>
          <span class="profile-label">Minha conta</span>
          <strong class="profile-name">{{ primeiroNome }}</strong>
        </summary>
        <div class="top-menu-profile-options">
          <RouterLink to="/conta" @click="fecharMenus">Meus dados</RouterLink>
          <RouterLink to="/meu-plano" @click="fecharMenus">Meu plano</RouterLink>
          <RouterLink to="/alterar-senha" @click="fecharMenus">Alterar senha</RouterLink>
        </div>
      </details>
      <button class="top-menu-logout" type="button" @click="sair">
        <LogOut :size="16" /> Sair
      </button>
    </div>
  </aside>
</template>
