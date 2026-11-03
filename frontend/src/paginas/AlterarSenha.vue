<script setup lang="ts">
import { ref } from "vue";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import { clienteHttp } from "../servicos/clienteHttp";
import { obterTokenCsrf } from "../servicos/perfilAluno";

const senhaAtual = ref("");
const novaSenha = ref("");
const confirmacaoSenha = ref("");
const mensagem = ref("");
const erro = ref("");
const processando = ref(false);

async function alterarSenha() {
  if (processando.value) return;
  erro.value = "";
  mensagem.value = "";

  if (novaSenha.value !== confirmacaoSenha.value) {
    erro.value = "As senhas devem ser iguais.";
    return;
  }

  processando.value = true;
  try {
    const csrf = await obterTokenCsrf();
    await clienteHttp.post(
      "/v1/conta/senha",
      { senhaAtual: senhaAtual.value, novaSenha: novaSenha.value },
      {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": csrf },
      },
    );
    senhaAtual.value = "";
    novaSenha.value = "";
    confirmacaoSenha.value = "";
    mensagem.value = "Senha alterada. Use a nova senha no próximo acesso.";
  } catch (falha) {
    erro.value =
      (falha as { response?: { data?: { mensagem?: string } } }).response?.data
        ?.mensagem ?? "Não foi possível alterar a senha.";
  } finally {
    processando.value = false;
  }
}
</script>

<template>
  <div class="password-page">
    <CabecalhoPagina
      eyebrow="SEGURANÇA DA CONTA"
      titulo="Alterar senha"
      descricao="Confirme sua senha atual e escolha uma nova senha para acessar o IVIS."
    >
      <RouterLink class="btn btn-outline-secondary" to="/conta">
        Voltar para minha conta
      </RouterLink>
    </CabecalhoPagina>

    <section class="card password-card">
      <form class="card-body" @submit.prevent="alterarSenha">
        <div class="password-card-heading">
          <h2>Atualize sua senha</h2>
          <p>
            Use pelo menos 12 caracteres. A alteração encerra as renovações de
            sessão anteriores.
          </p>
        </div>

        <div class="mb-4">
          <label for="senha-atual" class="form-label">Senha atual</label>
          <input
            id="senha-atual"
            v-model="senhaAtual"
            type="password"
            autocomplete="current-password"
            required
            maxlength="128"
            class="form-control"
          />
        </div>

        <div class="mb-4">
          <label for="nova-senha-conta" class="form-label">Nova senha</label>
          <input
            id="nova-senha-conta"
            v-model="novaSenha"
            type="password"
            autocomplete="new-password"
            required
            minlength="12"
            maxlength="128"
            class="form-control"
          />
        </div>

        <div class="mb-4">
          <label for="confirmar-senha-conta" class="form-label"
            >Confirme a nova senha</label
          >
          <input
            id="confirmar-senha-conta"
            v-model="confirmacaoSenha"
            type="password"
            autocomplete="new-password"
            required
            minlength="12"
            maxlength="128"
            class="form-control"
          />
        </div>

        <p v-if="erro" role="alert" class="alert alert-danger">{{ erro }}</p>
        <p v-if="mensagem" role="status" class="alert alert-success">
          {{ mensagem }}
        </p>

        <div class="password-actions">
          <button class="btn btn-primary" type="submit" :disabled="processando">
            {{ processando ? "Salvando..." : "Salvar nova senha" }}
          </button>
          <RouterLink class="btn btn-link" to="/conta">Cancelar</RouterLink>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.password-page {
  max-width: 980px;
}

.password-card {
  max-width: 760px;
  background: #fff;
}

.password-card .card-body {
  padding: 28px;
}

.password-card-heading {
  margin-bottom: 24px;
}

.password-card-heading h2 {
  margin: 0 0 8px;
  color: #292a35;
  font-size: 20px;
  font-weight: 700;
}

.password-card-heading p {
  margin: 0;
  color: #626477;
}

.password-card .form-label {
  display: block;
  margin-bottom: 8px;
  color: #454653;
  font-weight: 600;
}

.password-card .form-control {
  min-height: 46px;
  background-color: #fff;
  border-color: #9298a7;
  color: #292a35;
}

.password-card .form-control:focus {
  border-color: #8d80ea;
  box-shadow: 0 0 0 0.2rem #6757df1b;
}

.password-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

@media (max-width: 575.98px) {
  .password-card .card-body {
    padding: 20px;
  }

  .password-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
