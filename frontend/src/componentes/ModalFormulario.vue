<script setup lang="ts">
withDefaults(defineProps<{ titulo: string; rotuloAcao?: string }>(), {
  rotuloAcao: "Salvar",
});
const emit = defineEmits<{ fechar: []; enviar: [evento: SubmitEvent] }>();
</script>

<template>
  <div
    class="ivis-modal-layer"
    role="presentation"
  >
    <div
      class="modal d-block"
      role="dialog"
      aria-modal="true"
      :aria-label="titulo"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <form @submit.prevent="emit('enviar', $event)">
            <div class="modal-header">
              <div>
                <div class="eyebrow">SEU PERFIL IVIS</div>
                <h2 class="modal-title fs-5">{{ titulo }}</h2>
              </div>
              <button
                type="button"
                class="btn-close"
                aria-label="Fechar"
                @click="emit('fechar')"
              />
            </div>
            <div class="modal-body d-grid gap-3"><slot /></div>
            <div class="modal-footer">
              <button
                type="button"
                class="btn btn-outline-secondary"
                @click="emit('fechar')"
              >
                Cancelar</button
              ><button class="btn btn-primary">{{ rotuloAcao }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
