<script setup lang="ts">
import { ref } from "vue";
import { Eye, EyeOff } from "lucide-vue-next";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";
import { clienteHttp } from "../servicos/clienteHttp";

const email = ref("");
const senha = ref("");
const mostrarSenha = ref(false);
const rota = useRoute();
const modoRecuperacao = ref(rota.query.recuperar === "1");
const mensagem = ref("");
const processando = ref(false);
const sessao = useEstadoSessao();
const roteador = useRouter();

function mensagemErro(erro: unknown) {
  if (typeof erro === "object" && erro !== null && "response" in erro) {
    const resposta = (
      erro as {
        response?: {
          status?: number;
          data?: { mensagem?: string; codigo?: string };
        };
      }
    ).response;
    if (resposta?.status === 403 && resposta.data?.codigo === "acesso_negado")
      return "Não foi possível validar sua sessão. Atualize a página e tente novamente.";
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
    <section class="login-panel">
      <header class="login-top">
        <RouterLink class="login-brand" to="/">IVIS<span>.</span></RouterLink>
        <RouterLink class="login-back" to="/planos">Ver planos</RouterLink>
      </header>

      <div class="login-card">
      <header class="login-heading">
        <h1>
          {{ modoRecuperacao ? "Recupere seu acesso" : "Entrar na conta" }}
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
          <div class="login-password-field">
            <input
              id="login-senha"
              v-model="senha"
              class="form-control"
              :type="mostrarSenha ? 'text' : 'password'"
              placeholder="Digite sua senha"
              autocomplete="current-password"
              required
            />
            <button
              class="login-password-toggle"
              type="button"
              :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
              :aria-pressed="mostrarSenha"
              @click="mostrarSenha = !mostrarSenha"
            >
              <EyeOff v-if="mostrarSenha" :size="18" />
              <Eye v-else :size="18" />
            </button>
          </div>
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
      </div>
    </section>

    <aside class="login-visual" aria-label="Conheça o IVIS">
      <div class="login-visual-content">
        <div class="login-visual-brand">IVIS<span>.</span></div>
        <p>Pratique para sua próxima entrevista.</p>
        <div v-if="!modoRecuperacao" class="login-promo">
          <strong>Ainda não tem uma conta?</strong>
          <RouterLink :to="{ name: 'cadastro', query: rota.query }"
            >Criar conta</RouterLink
          >
        </div>
      </div>
    </aside>
  </main>
</template>
