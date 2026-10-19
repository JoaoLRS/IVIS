<script setup lang="ts">
import { ChevronRight, House, Menu } from "lucide-vue-next";
defineProps<{ titulo: string }>();
const emit = defineEmits<{ abrirMenu: [] }>();
</script>

<template>
  <header class="topbar d-flex align-items-center justify-content-between">
    <div class="d-flex align-items-center gap-2">
      <button
        class="btn btn-light mobile-menu d-lg-none"
        aria-label="Abrir menu"
        @click="emit('abrirMenu')"
      >
        <Menu :size="18" />
      </button>
      <div class="breadcrumb-line d-flex align-items-center gap-2">
        <RouterLink class="breadcrumb-home" to="/inicio" aria-label="Início">
          <House :size="15" />
        </RouterLink>
        <ChevronRight :size="14" /><strong>{{
          titulo
        }}</strong>
      </div>
    </div>
  </header>
</template>
