<script setup lang="ts">
import axios from "axios";
import { ref } from "vue";
import { useRoute } from "vue-router";
import { clienteHttp } from "../servicos/clienteHttp";
import { obterTokenCsrf } from "../servicos/perfilAluno";

const rota = useRoute();
const token = typeof rota.query.token === "string" ? rota.query.token : "";
const senha = ref("");
const confirmacao = ref("");
const erro = ref(token ? "" : "Link inválido. Solicite um novo link de recuperação.");
const concluido = ref(false);
const processando = ref(false);
async function redefinir() {
  if (processando.value || !token) return;
  erro.value = "";
  if (senha.value !== confirmacao.value) {
    erro.value = "As senhas devem ser iguais.";
    return;
  }
  processando.value = true;
  try {
    const csrf = await obterTokenCsrf();
    await clienteHttp.post("/v1/senhas/redefinicao", { token, novaSenha: senha.value }, {
      withCredentials: true, headers: { "X-XSRF-TOKEN": csrf },
    });
    senha.value = confirmacao.value = "";
    concluido.value = true;
  } catch (falha) {
    erro.value = axios.isAxiosError(falha)
      ? falha.response?.data?.mensagem ?? "Não foi possível redefinir a senha. Solicite um novo link ou tente novamente."
      : "Não foi possível redefinir a senha.";
  } finally { processando.value = false; }
}
</script>

<template>
  <main class="container py-5" style="max-width: 520px">
    <h1>Redefina sua senha</h1>
    <p v-if="concluido" role="status">Senha redefinida. Entre com sua nova senha.</p>
    <form v-else @submit.prevent="redefinir">
      <p>Informe uma nova senha com pelo menos 12 caracteres.</p>
      <label for="nova-senha" class="form-label">Nova senha</label>
      <input id="nova-senha" v-model="senha" type="password" autocomplete="new-password" minlength="12" maxlength="128" required class="form-control mb-3" />
      <label for="confirmar-senha" class="form-label">Confirme a nova senha</label>
      <input id="confirmar-senha" v-model="confirmacao" type="password" autocomplete="new-password" minlength="12" maxlength="128" required class="form-control mb-3" />
      <p v-if="erro" role="alert" class="alert alert-danger">{{ erro }}</p>
      <button class="btn btn-primary" :disabled="processando || !token">{{ processando ? 'Salvando...' : 'Redefinir senha' }}</button>
    </form>
    <RouterLink to="/login" class="d-block mt-3">Voltar ao login</RouterLink>
  </main>
</template>
