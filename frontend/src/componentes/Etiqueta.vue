<script setup lang="ts">
withDefaults(
  defineProps<{ tom?: "neutral" | "purple" | "green" | "orange" }>(),
  { tom: "neutral" },
);
</script>

<template>
  <span class="ivis-badge" :class="tom"><slot /></span>
</template>
