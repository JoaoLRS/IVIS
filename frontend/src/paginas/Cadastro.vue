<script setup lang="ts">
import { computed, ref } from "vue";
import { GraduationCap, School } from "lucide-vue-next";
import { RouterLink } from "vue-router";

type PerfilCadastro = "aluno" | "instituicao";

const perfil = ref<PerfilCadastro>("aluno");
const nome = ref("");
const email = ref("");
const documento = ref("");
const senha = ref("");
const confirmarSenha = ref("");
const mensagem = ref("");
const nomeCampo = computed(() =>
  perfil.value === "aluno" ? "Nome" : "Nome da instituição",
);

function enviarCadastro() {
  mensagem.value =
    senha.value === confirmarSenha.value
      ? "Exemplo de cadastro: esta tela ainda não está conectada ao serviço de contas, então nenhuma conta foi criada."
      : "As senhas não coincidem. Confira e tente novamente.";
}
</script>

<template>
  <main class="cadastro-page">
    <header class="cadastro-top">
      <RouterLink class="cadastro-brand" to="/inicio">IVIS</RouterLink>
      <RouterLink class="cadastro-back" to="/inicio">
        Voltar ao início
      </RouterLink>
    </header>

    <section class="cadastro-card">
      <header class="cadastro-heading">
        <h2>Crie sua conta</h2>
      </header>

      <div class="perfil-options" role="radiogroup" aria-label="Tipo de conta">
        <button
          type="button"
          class="perfil-option"
          :class="{ selecionado: perfil === 'aluno' }"
          role="radio"
          :aria-checked="perfil === 'aluno'"
          @click="perfil = 'aluno'; mensagem = ''"
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
          @click="perfil = 'instituicao'; mensagem = ''"
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
          <label class="form-label" for="nome-cadastro">{{ nomeCampo }}</label>
          <input
            id="nome-cadastro"
            v-model="nome"
            class="form-control"
            :placeholder="perfil === 'aluno' ? 'Nome completo' : 'Nome da instituição de ensino'"
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

        <div v-if="perfil === 'instituicao'">
          <label class="form-label" for="documento-cadastro">
            CNPJ <span class="campo-opcional">opcional neste exemplo</span>
          </label>
          <input
            id="documento-cadastro"
            v-model="documento"
            class="form-control"
            inputmode="numeric"
            placeholder="00.000.000/0000-00"
            autocomplete="organization"
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
              minlength="8"
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
              minlength="8"
              required
            />
          </div>
        </div>

        <p v-if="mensagem" class="cadastro-message" role="status">
          {{ mensagem }}
        </p>

        <button class="btn btn-primary cadastro-submit" type="submit">
          Criar conta
        </button>

        <p class="cadastro-terms">
          Ao continuar, você concorda com os termos de uso e a política de
          privacidade.
        </p>
      </form>
    </section>

    <p class="cadastro-demo">Tela de exemplo · cadastro ainda não integrado</p>
  </main>
</template>
