<script setup lang="ts">
import { ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";
import { clienteHttp } from "../servicos/clienteHttp";

const email = ref("");
const senha = ref("");
const modoRecuperacao = ref(false);
const mensagem = ref("");
const processando = ref(false);
const sessao = useEstadoSessao();
const rota = useRoute();
const roteador = useRouter();

function mensagemErro(erro: unknown) {
  if (typeof erro === "object" && erro !== null && "response" in erro) {
    const resposta = (erro as { response?: { data?: { mensagem?: string } } })
      .response;
    return (
      resposta?.data?.mensagem ?? "Não foi possível concluir a solicitação."
    );
  }
  return "Não foi possível conectar ao IVIS. Tente novamente.";
}

async function enviarFormulario() {
  processando.value = true;
  mensagem.value = "";
  try {
    if (modoRecuperacao.value) {
      const csrf = await clienteHttp.get<{ token: string }>(
        "/v1/sessoes/csrf",
        {
          withCredentials: true,
        },
      );
      await clienteHttp.post(
        "/v1/senhas/recuperacao",
        { email: email.value },
        {
          withCredentials: true,
          headers: { "X-XSRF-TOKEN": csrf.data.token },
        },
      );
      mensagem.value =
        "Se este e-mail estiver cadastrado, você receberá instruções para recuperar o acesso.";
    } else {
      await sessao.entrar(email.value, senha.value);
      const destino =
        typeof rota.query.redirect === "string" &&
        rota.query.redirect.startsWith("/") &&
        !rota.query.redirect.startsWith("//")
          ? rota.query.redirect
          : "/inicio";
      const podeContinuarCompra = destino.startsWith("/checkout/");
      await roteador.replace(
        sessao.temPlano || podeContinuarCompra ? destino : "/planos",
      );
    }
  } catch (erro) {
    mensagem.value = mensagemErro(erro);
  } finally {
    processando.value = false;
  }
}

function abrirRecuperacao() {
  modoRecuperacao.value = true;
  mensagem.value = "";
}

function voltarAoLogin() {
  modoRecuperacao.value = false;
  mensagem.value = "";
}
</script>

<template>
  <main class="login-page">
    <header class="login-top">
      <RouterLink class="login-brand" to="/planos">IVIS</RouterLink>
      <RouterLink class="login-back" to="/planos">Ver planos</RouterLink>
    </header>

    <section class="login-card">
      <header class="login-heading">
        <span class="login-eyebrow">
          {{ modoRecuperacao ? "RECUPERE SEU ACESSO" : "BEM-VINDO(A)" }}
        </span>
        <h1>
          {{ modoRecuperacao ? "Esqueceu sua senha?" : "Acesse sua conta" }}
        </h1>
        <p>
          {{
            modoRecuperacao
              ? "Informe seu e-mail e enviaremos as instruções de recuperação."
              : "Entre para acessar seus treinamentos e acompanhar seu desenvolvimento."
          }}
        </p>
      </header>

      <form class="login-fields" @submit.prevent="enviarFormulario">
        <div>
          <label class="form-label" for="login-email">E-mail</label>
          <input
            id="login-email"
            v-model="email"
            class="form-control"
            type="email"
            placeholder="voce@exemplo.com"
            autocomplete="email"
            required
          />
        </div>

        <div v-if="!modoRecuperacao">
          <div class="login-password-label">
            <label class="form-label" for="login-senha">Senha</label>
            <button
              class="login-text-button"
              type="button"
              @click="abrirRecuperacao"
            >
              Esqueci minha senha
            </button>
          </div>
          <input
            id="login-senha"
            v-model="senha"
            class="form-control"
            type="password"
            placeholder="Digite sua senha"
            autocomplete="current-password"
            required
          />
        </div>

        <p v-if="mensagem" class="login-message" role="status">
          {{ mensagem }}
        </p>

        <button
          class="btn btn-primary login-submit"
          type="submit"
          :disabled="processando"
        >
          {{
            processando
              ? "Aguarde..."
              : modoRecuperacao
                ? "Enviar instruções"
                : "Entrar"
          }}
        </button>
        <button
          v-if="modoRecuperacao"
          class="login-return"
          type="button"
          @click="voltarAoLogin"
        >
          Voltar para o login
        </button>
      </form>

      <div v-if="!modoRecuperacao" class="login-signup">
        <span>Ainda não tem uma conta?</span>
        <RouterLink :to="{ name: 'cadastro', query: rota.query }"
          >Criar conta</RouterLink
        >
      </div>
    </section>

    <p class="login-demo">Sua conta é protegida pelo IVIS.</p>
  </main>
</template>
