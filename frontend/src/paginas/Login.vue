<script setup lang="ts">
import { ref } from "vue";
import { RouterLink } from "vue-router";

const email = ref("");
const senha = ref("");
const modoRecuperacao = ref(false);
const mensagem = ref("");

function enviarFormulario() {
  mensagem.value = modoRecuperacao.value
    ? "Se este e-mail estiver cadastrado, você receberá as instruções para recuperar o acesso. Esta demonstração não envia e-mails."
    : "A autenticação ainda não está integrada ao serviço de contas. Nenhuma sessão foi iniciada.";
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
      <RouterLink class="login-brand" to="/inicio">IVIS</RouterLink>
      <RouterLink class="login-back" to="/inicio">
        Voltar ao início
      </RouterLink>
    </header>

    <section class="login-card">
      <header class="login-heading">
        <span class="login-eyebrow">
          {{ modoRecuperacao ? "RECUPERE SEU ACESSO" : "BEM-VINDO(A)" }}
        </span>
        <h1>{{ modoRecuperacao ? "Esqueceu sua senha?" : "Acesse sua conta" }}</h1>
        <p>
          {{ modoRecuperacao
            ? "Informe seu e-mail e enviaremos as instruções de recuperação."
            : "Entre para começar sua preparação para entrevistas." }}
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
            <button class="login-text-button" type="button" @click="abrirRecuperacao">
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

        <button class="btn btn-primary login-submit" type="submit">
          {{ modoRecuperacao ? "Enviar instruções" : "Entrar" }}
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
        <RouterLink to="/cadastro">Criar conta</RouterLink>
      </div>
    </section>

    <p class="login-demo">Tela de exemplo · autenticação ainda não integrada</p>
  </main>
</template>
