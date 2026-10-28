<script setup lang="ts">
import { ref } from "vue";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import { useEstadoSessao } from "../estados/estadoSessao";
import { atualizarMinhaConta } from "../servicos/conta";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";

const estado = useEstadoAplicacao();
const sessao = useEstadoSessao();
const nome = ref(estado.nomeAluno);
const nomeAdministrador = ref(sessao.conta?.nomeAdministrador ?? "");
const email = ref(estado.emailAluno);
const salvo = ref(false);

async function salvar() {
  const conta = await atualizarMinhaConta({
    email: email.value,
    ...(sessao.conta?.tipoConta === "INSTITUICAO"
      ? { razaoSocial: nome.value, nomeAdministrador: nomeAdministrador.value }
      : { nome: nome.value }),
  });
  estado.atualizarDadosAluno({ nome: conta.nome, email: conta.email });
  sessao.conta = conta;
  nome.value = conta.nome;
  nomeAdministrador.value = conta.nomeAdministrador ?? "";
  email.value = conta.email;
  salvo.value = true;
}
</script>

<template>
  <CabecalhoPagina
    :eyebrow="sessao.conta?.tipoConta === 'INSTITUICAO' ? 'CONTA DA INSTITUIÇÃO' : 'CONTA DO ALUNO'"
    :titulo="sessao.conta?.tipoConta === 'INSTITUICAO' ? 'Editar dados da instituição' : 'Editar dados do aluno'"
    descricao="Atualize os dados persistidos da sua conta."
  />

  <section class="card account-edit-card">
    <form class="card-body" @submit.prevent="salvar">
      <div v-if="sessao.conta?.tipoConta === 'INSTITUICAO'" class="mb-3">
        <label class="form-label" for="nome-administrador">Nome do administrador</label>
        <input id="nome-administrador" v-model="nomeAdministrador" class="form-control" autocomplete="name" required />
      </div>
      <div class="mb-3">
        <label class="form-label" for="nome-aluno">{{ sessao.conta?.tipoConta === 'INSTITUICAO' ? 'Razão social' : 'Nome' }}</label>
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
