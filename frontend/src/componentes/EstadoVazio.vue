<script setup lang="ts">
import { Plus } from "lucide-vue-next";

defineProps<{
  titulo: string;
  descricao: string;
  acao: string;
}>();

const emit = defineEmits<{ acionar: [] }>();
</script>

<template>
  <section class="profile-empty-state">
    <div class="profile-empty-icon" aria-hidden="true"><slot name="icone" /></div>
    <h3>{{ titulo }}</h3>
    <p>{{ descricao }}</p>
    <button class="btn btn-primary profile-empty-action" type="button" @click="emit('acionar')">
      <Plus :size="16" aria-hidden="true" /> {{ acao }}
    </button>
  </section>
</template>

<style scoped>
.profile-empty-state {
  min-height: 228px;
  padding: 30px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border: 1px solid #e4e3f0;
  background: #fff;
  text-align: center;
}

.profile-empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  display: grid;
  place-items: center;
  border: 1px solid #ddd9fb;
  background: #f1efff;
  color: #6252df;
}

.profile-empty-state h3 {
  margin: 0;
  color: #20233c;
  font-size: 18px;
  font-weight: 700;
}

.profile-empty-state p {
  max-width: 450px;
  margin: 8px 0 20px;
  color: #5b6074;
  font-size: 13px;
  line-height: 1.55;
}

.profile-empty-action {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 15px;
  font-size: 13px;
  font-weight: 700;
}

.profile-empty-action:focus-visible {
  outline: 2px solid #6252df;
  outline-offset: 3px;
}
</style>
