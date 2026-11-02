<script setup lang="ts">
import { ref, watch } from "vue";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import { useEstadoSessao } from "../estados/estadoSessao";
import { atualizarMinhaConta } from "../servicos/conta";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";

const estado = useEstadoAplicacao();
const sessao = useEstadoSessao();
const nome = ref(sessao.conta?.nome ?? estado.nomeAluno);
const nomeResponsavel = ref(sessao.conta?.nomeResponsavel ?? "");
const email = ref(sessao.conta?.email ?? estado.emailAluno);
const cpf = ref(sessao.conta?.cpf ?? "");
const cnpj = ref(sessao.conta?.cnpj ?? "");
const telefone = ref(sessao.conta?.telefone ?? "");
const endereco = ref(sessao.conta?.endereco ?? "");
const salvo = ref(false);
const erro = ref("");

watch(
  () => sessao.conta,
  (conta) => {
    if (!conta) return;
    nome.value = conta.nome;
    nomeResponsavel.value = conta.nomeResponsavel ?? "";
    email.value = conta.email;
    cpf.value = conta.cpf ?? "";
    cnpj.value = conta.cnpj ?? "";
    telefone.value = conta.telefone ?? "";
    endereco.value = conta.endereco ?? "";
  },
);

async function salvar() {
  erro.value = "";
  salvo.value = false;
  try {
    const conta = await atualizarMinhaConta({
      email: email.value,
      telefone: telefone.value,
      endereco: endereco.value,
      ...(sessao.conta?.tipoConta === "INSTITUICAO"
        ? {
            nomeInstituicao: nome.value,
            nomeResponsavel: nomeResponsavel.value,
            cnpj: cnpj.value,
          }
        : { nome: nome.value, cpf: cpf.value }),
    });
    estado.atualizarDadosAluno({ nome: conta.nome, email: conta.email });
    sessao.conta = conta;
    nome.value = conta.nome;
    nomeResponsavel.value = conta.nomeResponsavel ?? "";
    email.value = conta.email;
    cpf.value = conta.cpf ?? "";
    cnpj.value = conta.cnpj ?? "";
    telefone.value = conta.telefone ?? "";
    endereco.value = conta.endereco ?? "";
    salvo.value = true;
  } catch (falha) {
    const resposta = (falha as { response?: { data?: { mensagem?: string } } }).response;
    erro.value = resposta?.data?.mensagem ?? "Não foi possível salvar os dados da conta.";
  }
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
        <label class="form-label" for="nome-responsavel">Nome do responsável</label>
        <input id="nome-responsavel" v-model="nomeResponsavel" class="form-control" autocomplete="name" required />
      </div>
      <div class="mb-3">
        <label class="form-label" for="nome-conta">{{ sessao.conta?.tipoConta === 'INSTITUICAO' ? 'Nome da instituição' : 'Nome' }}</label>
        <input id="nome-conta" v-model="nome" class="form-control" autocomplete="name" required />
      </div>
      <div class="mb-3">
        <label class="form-label" for="email-conta">E-mail</label>
        <input id="email-conta" v-model="email" class="form-control" type="email" autocomplete="email" required />
      </div>
      <div class="mb-3">
        <label class="form-label" :for="sessao.conta?.tipoConta === 'INSTITUICAO' ? 'cnpj-conta' : 'cpf-conta'">
          {{ sessao.conta?.tipoConta === 'INSTITUICAO' ? 'CNPJ' : 'CPF' }}
        </label>
        <input
          v-if="sessao.conta?.tipoConta === 'INSTITUICAO'"
          id="cnpj-conta"
          v-model="cnpj"
          class="form-control"
          inputmode="numeric"
          maxlength="18"
          required
        />
        <input v-else id="cpf-conta" v-model="cpf" class="form-control" inputmode="numeric" maxlength="14" required />
      </div>
      <div class="mb-3">
        <label class="form-label" for="telefone-conta">Telefone</label>
        <input id="telefone-conta" v-model="telefone" class="form-control" type="tel" autocomplete="tel" maxlength="30" required />
      </div>
      <div class="mb-4">
        <label class="form-label" for="endereco-conta">Endereço</label>
        <input id="endereco-conta" v-model="endereco" class="form-control" autocomplete="street-address" maxlength="500" required />
      </div>
      <div v-if="sessao.conta?.tipoConta === 'ALUNO'" class="mb-4">
        <label class="form-label" for="instituicao-conta">Código do objeto da instituição vinculada</label>
        <input
          id="instituicao-conta"
          class="form-control"
          :value="sessao.conta.objetoInstituicao || 'Sem instituição vinculada'"
          readonly
        />
      </div>
      <p v-if="erro" class="alert alert-danger" role="alert">{{ erro }}</p>
      <div class="d-flex align-items-center gap-3">
        <button class="btn btn-primary" type="submit">Salvar alterações</button>
        <span v-if="salvo" class="text-success small" role="status">Dados atualizados.</span>
      </div>
    </form>
  </section>
</template>

<style scoped>
.account-edit-card {
  max-width: 680px;
}
</style>
