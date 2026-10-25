<script setup lang="ts">
defineProps<{ titulo: string; mensagem: string }>();
const emit = defineEmits<{ fechar: []; confirmar: [] }>();
</script>

<template>
  <div class="ivis-modal-layer" role="presentation">
    <div
      class="modal d-block"
      role="dialog"
      aria-modal="true"
      :aria-label="titulo"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h2 class="modal-title modal-title-confirmacao fs-5">
              {{ titulo }}
            </h2>
            <button
              type="button"
              class="btn-close"
              aria-label="Fechar confirmação"
              @click="emit('fechar')"
            />
          </div>
          <div class="modal-body">
            <p class="mb-0">{{ mensagem }}</p>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-outline-secondary"
              @click="emit('fechar')"
            >
              Cancelar
            </button>
            <button
              type="button"
              class="btn btn-danger"
              @click="emit('confirmar')"
            >
              Excluir
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
