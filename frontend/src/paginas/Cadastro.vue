<script setup lang="ts">
import { computed, ref } from "vue";
import { GraduationCap, LogOut, School } from "lucide-vue-next";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";

type PerfilCadastro = "aluno" | "instituicao";

const rota = useRoute();
const perfil = ref<PerfilCadastro>(
  rota.query.tipo === "instituicao" ? "instituicao" : "aluno",
);
const nome = ref("");
const email = ref("");
const senha = ref("");
const confirmarSenha = ref("");
const mensagem = ref("");
const processando = ref(false);
const cadastroConcluido = ref(false);
const emailSimuladorAberto = ref(false);
const sessao = useEstadoSessao();
const roteador = useRouter();
const nomeCampo = computed(() =>
  perfil.value === "aluno" ? "Nome" : "Nome da instituição",
);

async function sairDaConta() {
  await sessao.sair();
  cadastroConcluido.value = false;
  emailSimuladorAberto.value = false;
  await roteador.replace("/");
}

function mensagemErro(erro: unknown) {
  if (typeof erro === "object" && erro !== null && "response" in erro) {
    const resposta = (erro as { response?: { data?: { mensagem?: string } } })
      .response;
    return resposta?.data?.mensagem ?? "Não foi possível criar a conta.";
  }
  return "Não foi possível conectar ao IVIS. Tente novamente.";
}

async function enviarCadastro() {
  mensagem.value = "";
  cadastroConcluido.value = false;
  if (senha.value !== confirmarSenha.value) {
    mensagem.value = "As senhas não coincidem. Confira e tente novamente.";
    return;
  }
  processando.value = true;
  try {
    if (perfil.value === "aluno") {
      await sessao.cadastrarAluno({
        nome: nome.value,
        email: email.value,
        senha: senha.value,
      });
    } else {
      await sessao.cadastrarInstituicao({
        razaoSocial: nome.value,
        email: email.value,
        senha: senha.value,
      });
    }
    cadastroConcluido.value = true;
  } catch (erro) {
    mensagem.value = mensagemErro(erro);
  } finally {
    processando.value = false;
  }
}

async function continuarAposCadastro() {
  const redirecionamento = rota.query.redirect;
  const destino =
    typeof redirecionamento === "string" &&
    redirecionamento.startsWith("/") &&
    !redirecionamento.startsWith("//")
      ? redirecionamento
      : "/inicio";
  await roteador.replace(
    sessao.temPlano || destino.startsWith("/checkout/")
      ? destino
      : "/planos?acesso=necessario",
  );
}
</script>

<template>
  <main class="cadastro-page">
    <header class="cadastro-top">
      <RouterLink class="cadastro-brand" to="/planos">IVIS</RouterLink>
      <div class="cadastro-top-actions">
        <RouterLink class="cadastro-back" to="/planos">Ver planos</RouterLink>
        <button
          v-if="sessao.conta"
          class="cadastro-logout"
          type="button"
          @click="sairDaConta"
        >
          <LogOut :size="14" /> Sair
        </button>
      </div>
    </header>

    <section class="cadastro-card">
      <template v-if="cadastroConcluido">
        <div class="cadastro-success" role="status" aria-live="polite">
          <span class="cadastro-success-mark" aria-hidden="true">✓</span>
          <h2>Conta criada com sucesso</h2>
          <p>
            Seu cadastro foi concluído. Você também pode conferir a mensagem de
            boas-vindas no simulador de e-mail do IVIS.
          </p>
          <button
            class="btn cadastro-email-button"
            type="button"
            @click="emailSimuladorAberto = true"
          >
            Abrir simulador de e-mail
          </button>
          <button
            class="btn btn-primary cadastro-submit"
            type="button"
            @click="continuarAposCadastro"
          >
            Continuar
          </button>
        </div>
      </template>
      <template v-else>
        <header class="cadastro-heading">
          <h2>Crie sua conta</h2>
        </header>

        <div
          class="perfil-options"
          role="radiogroup"
          aria-label="Tipo de conta"
        >
          <button
            type="button"
            class="perfil-option"
            :class="{ selecionado: perfil === 'aluno' }"
            role="radio"
            :aria-checked="perfil === 'aluno'"
            @click="
              perfil = 'aluno';
              mensagem = '';
            "
          >
            <span class="perfil-icon"><GraduationCap :size="19" /></span>
            <span class="perfil-option-copy">
              <strong>Sou aluno</strong>
              <small>Quero praticar entrevistas</small>
            </span>
            <span class="perfil-radio" />
          </button>

          <button
            type="button"
            class="perfil-option"
            :class="{ selecionado: perfil === 'instituicao' }"
            role="radio"
            :aria-checked="perfil === 'instituicao'"
            @click="
              perfil = 'instituicao';
              mensagem = '';
            "
          >
            <span class="perfil-icon"><School :size="19" /></span>
            <span class="perfil-option-copy">
              <strong>Sou instituição</strong>
              <small>Quero apoiar meus alunos</small>
            </span>
            <span class="perfil-radio" />
          </button>
        </div>

        <form class="cadastro-fields" @submit.prevent="enviarCadastro">
          <div>
            <label class="form-label" for="nome-cadastro">{{
              nomeCampo
            }}</label>
            <input
              id="nome-cadastro"
              v-model="nome"
              class="form-control"
              :placeholder="
                perfil === 'aluno'
                  ? 'Nome completo'
                  : 'Nome da instituição de ensino'
              "
              autocomplete="name"
              required
            />
          </div>

          <div>
            <label class="form-label" for="email-cadastro">E-mail</label>
            <input
              id="email-cadastro"
              v-model="email"
              class="form-control"
              type="email"
              placeholder="voce@exemplo.com"
              autocomplete="email"
              required
            />
          </div>

          <div class="cadastro-passwords">
            <div>
              <label class="form-label" for="senha-cadastro">Senha</label>
              <input
                id="senha-cadastro"
                v-model="senha"
                class="form-control"
                type="password"
                placeholder="Crie uma senha"
                autocomplete="new-password"
                minlength="12"
                required
              />
            </div>
            <div>
              <label class="form-label" for="confirmar-senha">
                Confirmar senha
              </label>
              <input
                id="confirmar-senha"
                v-model="confirmarSenha"
                class="form-control"
                type="password"
                placeholder="Digite novamente"
                autocomplete="new-password"
                minlength="12"
                required
              />
            </div>
          </div>

          <p v-if="mensagem" class="cadastro-message" role="status">
            {{ mensagem }}
          </p>

          <button
            class="btn btn-primary cadastro-submit"
            type="submit"
            :disabled="processando"
          >
            {{ processando ? "Criando conta..." : "Criar conta" }}
          </button>

          <p class="cadastro-terms">
            Ao continuar, você concorda com os termos de uso e a política de
            privacidade.
          </p>
        </form>

        <p class="cadastro-login-link">
          Já tem uma conta?
          <RouterLink :to="{ name: 'login', query: rota.query }"
            >Entrar</RouterLink
          >
        </p>
      </template>
    </section>

    <p class="cadastro-demo">Sua conta é protegida pelo IVIS.</p>

    <div
      v-if="emailSimuladorAberto"
      class="email-simulator-backdrop"
      role="presentation"
      @click.self="emailSimuladorAberto = false"
    >
      <section
        class="email-simulator"
        role="dialog"
        aria-modal="true"
        aria-labelledby="email-simulator-title"
      >
        <header>
          <div>
            <span class="email-simulator-label">CAIXA DE ENTRADA SIMULADA</span>
            <h2 id="email-simulator-title">Mensagem de boas-vindas</h2>
          </div>
          <button
            type="button"
            aria-label="Fechar simulador de e-mail"
            @click="emailSimuladorAberto = false"
          >
            ×
          </button>
        </header>
        <dl>
          <div>
            <dt>De</dt>
            <dd>IVIS · no-reply@ivis.local</dd>
          </div>
          <div>
            <dt>Para</dt>
            <dd>{{ sessao.conta?.email }}</dd>
          </div>
          <div>
            <dt>Assunto</dt>
            <dd>Bem-vindo ao IVIS</dd>
          </div>
        </dl>
        <p>Olá, {{ sessao.conta?.nome }}!</p>
        <p>
          Sua conta IVIS foi criada com sucesso. A partir de agora, você pode
          escolher um plano individual ou, se sua instituição tiver um plano
          acadêmico ativo, acessar pelo vínculo institucional.
        </p>
        <p class="email-simulator-note">
          Esta mensagem é apenas uma simulação acadêmica. Nenhum e-mail real foi
          enviado.
        </p>
      </section>
    </div>
  </main>
</template>
