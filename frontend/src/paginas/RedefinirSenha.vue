<script setup lang="ts">
import axios from "axios";
import { ref } from "vue";
import { ArrowLeft, CheckCircle2, Eye, EyeOff, KeyRound } from "lucide-vue-next";
import { useRoute } from "vue-router";
import { clienteHttp } from "../servicos/clienteHttp";
import { obterTokenCsrf } from "../servicos/perfilAluno";
import RequisitosSenha from "../componentes/RequisitosSenha.vue";
import { senhaValida } from "../validacao/senha";

const rota = useRoute();
const token = typeof rota.query.token === "string" ? rota.query.token : "";
const senha = ref("");
const confirmacao = ref("");
const mostrarSenha = ref(false);
const mostrarConfirmacao = ref(false);
const erro = ref("");
const concluido = ref(false);
const processando = ref(false);

async function redefinir() {
  if (processando.value || !token) return;
  erro.value = "";
  if (!senhaValida(senha.value)) {
    erro.value = "A nova senha ainda não atende a todos os requisitos informados.";
    return;
  }
  if (senha.value !== confirmacao.value) {
    erro.value = "As senhas devem ser iguais.";
    return;
  }

  processando.value = true;
  try {
    const csrf = await obterTokenCsrf();
    await clienteHttp.post(
      "/v1/senhas/redefinicao",
      { token, novaSenha: senha.value },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": csrf } },
    );
    senha.value = "";
    confirmacao.value = "";
    concluido.value = true;
  } catch (falha) {
    erro.value = axios.isAxiosError(falha)
      ? falha.response?.data?.mensagem ??
        "Não foi possível redefinir a senha. Solicite um novo link ou tente novamente."
      : "Não foi possível redefinir a senha.";
  } finally {
    processando.value = false;
  }
}
</script>

<template>
  <main class="login-page recovery-page">
    <section class="login-panel">
      <header class="login-top">
        <RouterLink class="login-brand" to="/">IVIS<span>.</span></RouterLink>
        <RouterLink class="login-back recovery-back" to="/login">
          <ArrowLeft :size="16" /> Voltar ao login
        </RouterLink>
      </header>

      <div class="login-card">
        <section v-if="concluido" class="recovery-state" role="status">
          <CheckCircle2 :size="36" aria-hidden="true" />
          <h1>Senha redefinida</h1>
          <p>Agora você pode entrar com sua nova senha.</p>
          <RouterLink class="recovery-action" to="/login">Entrar na conta</RouterLink>
        </section>

        <section v-else-if="!token" class="recovery-state" role="alert">
          <KeyRound :size="36" aria-hidden="true" />
          <h1>Link de recuperação inválido</h1>
          <p>Solicite um novo link para redefinir sua senha.</p>
          <RouterLink class="recovery-action" to="/login?recuperar=1">
            Solicitar novo link
          </RouterLink>
        </section>

        <template v-else>
          <header class="login-heading">
            <h1>Redefina sua senha</h1>
            <p>Crie uma nova senha para voltar a acessar sua conta.</p>
          </header>

          <form class="login-fields" @submit.prevent="redefinir">
            <div>
              <label for="nova-senha" class="form-label">Nova senha</label>
              <div class="login-password-field">
                <input
                  id="nova-senha"
                  v-model="senha"
                  :type="mostrarSenha ? 'text' : 'password'"
                  autocomplete="new-password"
                  minlength="8"
                  maxlength="128"
                  required
                  aria-describedby="requisitos-redefinicao"
                  class="form-control"
                />
                <button
                  class="login-password-toggle"
                  type="button"
                  :aria-label="mostrarSenha ? 'Ocultar nova senha' : 'Mostrar nova senha'"
                  :aria-pressed="mostrarSenha"
                  @click="mostrarSenha = !mostrarSenha"
                >
                  <EyeOff v-if="mostrarSenha" :size="18" />
                  <Eye v-else :size="18" />
                </button>
              </div>
              <RequisitosSenha id="requisitos-redefinicao" :senha="senha" />
            </div>

            <div>
              <label for="confirmar-senha" class="form-label">
                Confirme a nova senha
              </label>
              <div class="login-password-field">
                <input
                  id="confirmar-senha"
                  v-model="confirmacao"
                  :type="mostrarConfirmacao ? 'text' : 'password'"
                  autocomplete="new-password"
                  minlength="8"
                  maxlength="128"
                  required
                  class="form-control"
                />
                <button
                  class="login-password-toggle"
                  type="button"
                  :aria-label="mostrarConfirmacao ? 'Ocultar confirmação' : 'Mostrar confirmação'"
                  :aria-pressed="mostrarConfirmacao"
                  @click="mostrarConfirmacao = !mostrarConfirmacao"
                >
                  <EyeOff v-if="mostrarConfirmacao" :size="18" />
                  <Eye v-else :size="18" />
                </button>
              </div>
            </div>

            <p v-if="erro" class="recovery-error" role="alert">{{ erro }}</p>
            <button class="btn btn-primary login-submit" type="submit" :disabled="processando">
              {{ processando ? "Salvando..." : "Redefinir senha" }}
            </button>
          </form>
          <RouterLink class="recovery-new-link" to="/login?recuperar=1">
            Precisa de outro link? Solicite aqui
          </RouterLink>
        </template>
      </div>
    </section>

    <aside class="login-visual" aria-label="Conheça o IVIS">
      <div class="login-visual-content">
        <div class="login-visual-brand">IVIS<span>.</span></div>
        <p>Pratique para sua próxima entrevista.</p>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.recovery-page {
  --requirements-text: #b9c1d5;
  --requirements-met: #c5f477;
}
.recovery-page .login-top {
  margin-bottom: clamp(44px, 7vh, 86px);
}
.recovery-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.recovery-page .login-card {
  padding-bottom: 40px;
}
.recovery-page .login-submit {
  width: 100%;
  margin-top: 6px;
}
.recovery-error {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #ee8f9c66;
  border-radius: 5px;
  color: #ffe5e8;
  background: #5b2632;
  font-size: 13px;
  line-height: 1.5;
}
.recovery-new-link {
  display: block;
  margin-top: 24px;
  color: #c5f477;
  font-size: 13px;
  text-align: center;
  text-decoration: none;
}
.recovery-new-link:hover {
  text-decoration: underline;
}
.recovery-state > svg {
  margin-bottom: 24px;
  color: #c5f477;
}
.recovery-state h1 {
  margin: 0 0 12px;
  color: #fff;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.15;
}
.recovery-state p {
  margin: 0;
  color: #b9c1d5;
  font-size: 15px;
  line-height: 1.6;
}
.recovery-action {
  min-height: 52px;
  margin-top: 30px;
  padding: 14px 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: #fff;
  background: #3157e8;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}
.recovery-action:hover {
  color: #fff;
  background: #466aff;
}
.recovery-page :is(a, button, input):focus-visible {
  outline: 2px solid #c5f477;
  outline-offset: 3px;
}
</style>
