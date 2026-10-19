<script setup lang="ts">
import { ref } from "vue";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";

const estado = useEstadoAplicacao();
const nome = ref(estado.nomeAluno);
const email = ref(estado.emailAluno);
const salvo = ref(false);

function salvar() {
  estado.atualizarDadosAluno({ nome: nome.value, email: email.value });
  nome.value = estado.nomeAluno;
  email.value = estado.emailAluno;
  salvo.value = true;
}
</script>

<template>
  <CabecalhoPagina
    eyebrow="CONTA DO ALUNO"
    titulo="Editar dados do aluno"
    descricao="Atualize seu nome e e-mail de demonstração."
  />

  <section class="card account-edit-card">
    <form class="card-body" @submit.prevent="salvar">
      <div class="mb-3">
        <label class="form-label" for="nome-aluno">Nome do aluno</label>
        <input
          id="nome-aluno"
          v-model="nome"
          class="form-control"
          autocomplete="name"
          required
        />
      </div>
      <div class="mb-4">
        <label class="form-label" for="email-aluno">E-mail</label>
        <input
          id="email-aluno"
          v-model="email"
          class="form-control"
          type="email"
          autocomplete="email"
          required
        />
      </div>
      <div class="d-flex align-items-center gap-3">
        <button class="btn btn-primary" type="submit">Salvar alterações</button>
        <span v-if="salvo" class="text-success small" role="status">
          Dados atualizados.
        </span>
      </div>
    </form>
  </section>
</template>

<style scoped>
.account-edit-card {
  max-width: 680px;
}
</style>
