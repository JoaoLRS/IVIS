<script setup lang="ts">
import { Bell, ChevronRight, Menu, Search } from "lucide-vue-next";
defineProps<{ titulo: string }>();
const emit = defineEmits<{ abrirMenu: []; notificar: [] }>();
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
        <span>Workspace</span><ChevronRight :size="14" /><strong>{{
          titulo
        }}</strong>
      </div>
    </div>
    <div class="d-flex align-items-center gap-3">
      <label class="search-field input-group"
        ><span class="input-group-text"><Search :size="15" /></span
        ><input class="form-control" placeholder="Buscar..." /><span
          class="input-group-text search-shortcut"
          >⌘ K</span
        ></label
      ><button
        class="btn btn-light notification-btn"
        aria-label="Notificações"
        @click="emit('notificar')"
      >
        <Bell :size="17" /><i /></button
      ><span class="user-avatar top-avatar">MC</span>
    </div>
  </header>
</template>
