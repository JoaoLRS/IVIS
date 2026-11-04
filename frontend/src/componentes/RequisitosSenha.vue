<script setup lang="ts">
import { requisitosSenha } from "../validacao/senha";

defineProps<{ senha: string; id: string }>();
</script>

<template>
  <div :id="id" class="password-requirements" aria-live="polite">
    <p class="password-requirements-title">Sua senha deve conter:</p>
    <ul>
      <li
        v-for="requisito in requisitosSenha"
        :key="requisito.id"
        :class="{ 'password-requirement-met': requisito.verificar(senha) }"
      >
        <span aria-hidden="true">{{ requisito.verificar(senha) ? "✓" : "○" }}</span>
        {{ requisito.texto }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
.password-requirements {
  margin: 10px 0 18px;
  color: #667085;
  font-size: 12px;
}

.password-requirements-title {
  margin: 0 0 7px;
  font-weight: 600;
}

.password-requirements ul {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-requirements li {
  display: flex;
  align-items: baseline;
  gap: 7px;
}

.password-requirements li span {
  flex: none;
  width: 12px;
}

.password-requirements .password-requirement-met {
  color: #198754;
  font-weight: 600;
}

@media (max-width: 520px) {
  .password-requirements ul { grid-template-columns: 1fr; }
}
</style>
