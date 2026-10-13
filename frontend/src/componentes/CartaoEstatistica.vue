<script setup lang="ts">
defineProps<{ rotulo: string; valor: string; detalhe: string; tom?: string }>();
</script>

<template>
  <div class="stat-card card h-100">
    <div class="card-body">
      <div class="stat-icon" :class="tom"><slot /></div>
      <div class="stat-label">{{ rotulo }}</div>
      <div class="stat-value">{{ valor }}</div>
      <div class="stat-detail">{{ detalhe }}</div>
    </div>
  </div>
</template>
