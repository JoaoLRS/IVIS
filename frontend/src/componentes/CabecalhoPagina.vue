<script setup lang="ts">
defineProps<{ eyebrow: string; titulo: string; descricao?: string }>();
</script>

<template>
  <div
    class="page-heading d-flex align-items-end justify-content-between gap-3 mb-4"
  >
    <div>
      <div class="eyebrow">{{ eyebrow }}</div>
      <h1>{{ titulo }}</h1>
      <p v-if="descricao">{{ descricao }}</p>
    </div>
    <div class="heading-action"><slot /></div>
  </div>
</template>
