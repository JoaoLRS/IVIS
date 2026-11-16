<script setup lang="ts">
import axios from "axios";
import { onMounted, ref } from "vue";
import { ArrowLeft, CheckCircle2 } from "lucide-vue-next";
import { clienteHttp } from "../servicos/clienteHttp";
import { obterTokenCsrf } from "../servicos/perfilAluno";
import RequisitosSenha from "../componentes/RequisitosSenha.vue";
import VisualEntrevista from "../componentes/VisualEntrevista.vue";
import { senhaValida } from "../validacao/senha";

const email = ref(
  typeof window.history.state?.emailRecuperacao === "string"
    ? window.history.state.emailRecuperacao
    : "",
);
const codigo = ref("");
const etapa = ref<"codigo" | "senha">("codigo");
const senha = ref("");
const confirmacao = ref("");
const erro = ref("");
const concluido = ref(false);
const processando = ref(false);
const endereco = new URL(window.location.href);
if (endereco.hash.startsWith("#token=") || endereco.searchParams.has("token")) {
  endereco.hash = "";
  endereco.searchParams.delete("token");
  window.history.replaceState(window.history.state, "", endereco.pathname + endereco.search);
  erro.value = "O link antigo não pode mais ser usado. Solicite um novo código por e-mail.";
}

async function redefinir() {
  if (processando.value) return;
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
      { novaSenha: senha.value },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": csrf } },
    );
    senha.value = "";
    confirmacao.value = "";
    codigo.value = "";
    concluido.value = true;
  } catch (falha) {
    erro.value = axios.isAxiosError(falha)
      ? falha.response?.data?.mensagem ??
        "Não foi possível redefinir a senha. Solicite um novo código ou tente novamente."
      : "Não foi possível redefinir a senha.";
  } finally {
    processando.value = false;
  }
}

async function validarCodigo() {
  if (processando.value) return;
  erro.value = "";
  processando.value = true;
  try {
    const csrf = await obterTokenCsrf();
    await clienteHttp.post(
      "/v1/senhas/validacao-codigo",
      { email: email.value.trim(), codigo: codigo.value },
      { withCredentials: true, headers: { "X-XSRF-TOKEN": csrf } },
    );
    codigo.value = "";
    etapa.value = "senha";
  } catch (falha) {
    erro.value = axios.isAxiosError(falha)
      ? falha.response?.data?.mensagem ?? "Não foi possível validar o código. Tente novamente."
      : "Não foi possível validar o código. Tente novamente.";
  } finally {
    processando.value = false;
  }
}

onMounted(async () => {
  try {
    await clienteHttp.get("/v1/senhas/redefinicao/autorizacao", { withCredentials: true });
    etapa.value = "senha";
  } catch {
    // Sem autorização ativa, a pessoa deve informar o código recebido.
  }
});
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

        <template v-else-if="etapa === 'codigo'">
          <header class="login-heading">
            <h1>Confira seu e-mail</h1>
            <p v-if="email">Enviamos um código de 8 dígitos para <strong class="recovery-email">{{ email }}</strong>. Digite o código abaixo para continuar.</p>
            <p v-else>Digite o e-mail da sua conta e o código de 8 dígitos que enviamos para você.</p>
          </header>
          <form class="login-fields" @submit.prevent="validarCodigo">
            <div v-if="!email">
              <label for="recovery-email" class="form-label">E-mail da conta</label>
              <input id="recovery-email" v-model="email" class="form-control" type="email" autocomplete="email" required />
            </div>
            <div>
              <label for="recovery-code" class="form-label">Código de recuperação</label>
              <input id="recovery-code" v-model="codigo" class="form-control" type="text" inputmode="numeric" pattern="[0-9]{8}" maxlength="8" autocomplete="one-time-code" placeholder="8 dígitos" required />
            </div>
            <p v-if="erro" class="recovery-error" role="alert">{{ erro }}</p>
            <button class="btn btn-primary login-submit" type="submit" :disabled="processando">
              {{ processando ? "Verificando..." : "Verificar código" }}
            </button>
          </form>
          <RouterLink class="recovery-new-link" to="/login?recuperar=1">
            Não recebeu o código? Solicite outro
          </RouterLink>
        </template>

        <template v-else>
          <header class="login-heading">
            <h1>Crie sua nova senha</h1>
            <p>O código foi confirmado. Escolha uma nova senha para sua conta.</p>
          </header>
          <form class="login-fields" @submit.prevent="redefinir">
            <div>
              <label for="nova-senha" class="form-label">Nova senha</label>
              <input
                id="nova-senha"
                v-model="senha"
                type="password"
                autocomplete="new-password"
                minlength="8"
                maxlength="128"
                required
                aria-describedby="requisitos-redefinicao"
                class="form-control"
              />
              <RequisitosSenha id="requisitos-redefinicao" :senha="senha" />
            </div>
            <div>
              <label for="confirmar-senha" class="form-label">Confirme a nova senha</label>
              <input
                id="confirmar-senha"
                v-model="confirmacao"
                type="password"
                autocomplete="new-password"
                minlength="8"
                maxlength="128"
                required
                class="form-control"
              />
            </div>
            <p v-if="erro" class="recovery-error" role="alert">{{ erro }}</p>
            <button class="btn btn-primary login-submit" type="submit" :disabled="processando">
              {{ processando ? "Salvando..." : "Redefinir senha" }}
            </button>
          </form>
          <RouterLink class="recovery-new-link" to="/login?recuperar=1">
            A verificação expirou? Solicite outro código
          </RouterLink>
        </template>
      </div>
    </section>

    <aside class="login-visual" aria-label="Conheça o IVIS">
      <div class="login-visual-content">
        <div class="login-visual-brand">IVIS<span>.</span></div>
        <p>Pratique para sua próxima entrevista.</p>
        <VisualEntrevista />
      </div>
    </aside>
  </main>
</template>

<style scoped>
.recovery-page {
  --requirements-text: #b9c1d5;
  --requirements-met: #c5f477;
}
.recovery-email {
  color: #fff;
  overflow-wrap: anywhere;
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
