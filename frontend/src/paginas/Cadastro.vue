<script setup lang="ts">
import { computed, ref } from "vue";
import { GraduationCap, LogOut, School } from "lucide-vue-next";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";
import RequisitosSenha from "../componentes/RequisitosSenha.vue";
import BootstrapCheck from "../componentes/BootstrapCheck.vue";
import { senhaValida } from "../validacao/senha";

type PerfilCadastro = "aluno" | "instituicao";

const rota = useRoute();
const perfil = ref<PerfilCadastro>(
  rota.query.tipo === "instituicao" ? "instituicao" : "aluno",
);
const nome = ref("");
const nomeResponsavel = ref("");
const email = ref("");
const cpf = ref("");
const cnpj = ref("");
const telefone = ref("");
const endereco = ref("");
const senha = ref("");
const confirmarSenha = ref("");
const mensagem = ref("");
const processando = ref(false);
const cadastroConcluido = ref(false);
const sessao = useEstadoSessao();
const roteador = useRouter();
const nomeCampo = computed(() =>
  perfil.value === "aluno" ? "Nome" : "Nome da instituição",
);
const preenchido = (valor: string) => valor.trim().length > 0;
const documentoValido = (valor: string, tamanho: number) =>
  valor.replace(/\D/g, "").length === tamanho;
const emailValido = computed(() =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()),
);
const erroConfirmacao = computed(() => {
  if (!confirmarSenha.value) return "";
  if (confirmarSenha.value !== senha.value) return "As senhas não coincidem.";
  if (!senhaValida(senha.value))
    return "A senha ainda não atende a todos os requisitos.";
  return "";
});
const confirmacaoValida = computed(
  () => confirmarSenha.value.length > 0 && !erroConfirmacao.value,
);
const mailpitLocal = computed(() =>
  ["localhost", "127.0.0.1"].includes(window.location.hostname),
);

async function sairDaConta() {
  await sessao.sair();
  cadastroConcluido.value = false;
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
  if (!senhaValida(senha.value)) {
    mensagem.value =
      "A senha ainda não atende a todos os requisitos informados.";
    return;
  }
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
        cpf: cpf.value,
        telefone: telefone.value,
        endereco: endereco.value,
        senha: senha.value,
      });
    } else {
      await sessao.cadastrarInstituicao({
        nomeResponsavel: nomeResponsavel.value,
        nomeInstituicao: nome.value,
        email: email.value,
        senha: senha.value,
        cnpj: cnpj.value,
        telefone: telefone.value,
        endereco: endereco.value,
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
            Seu cadastro foi concluído. A mensagem de boas-vindas foi encaminhada
            ao serviço de e-mail de demonstração.
          </p>
          <a v-if="mailpitLocal" class="btn cadastro-email-button" href="http://localhost:8025" target="_blank" rel="noopener noreferrer">Abrir Mailpit</a>
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
          <p class="cadastro-required-note">
            <span aria-hidden="true">*</span> Campos obrigatórios
          </p>
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
          <div v-if="perfil === 'instituicao'">
            <label class="form-label" for="nome-responsavel"
              >Nome do responsável
              <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck
                v-if="preenchido(nomeResponsavel)"
                class="campo-valido"
            /></label>
            <input
              id="nome-responsavel"
              v-model="nomeResponsavel"
              class="form-control"
              placeholder="Nome completo da pessoa responsável"
              autocomplete="name"
              required
            />
          </div>

          <div v-if="perfil === 'aluno'">
            <label class="form-label" for="cpf-cadastro"
              >CPF <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck
                v-if="documentoValido(cpf, 11)"
                class="campo-valido"
            /></label>
            <input
              id="cpf-cadastro"
              v-model="cpf"
              class="form-control"
              inputmode="numeric"
              autocomplete="off"
              placeholder="000.000.000-00"
              maxlength="14"
              required
            />
          </div>
          <div v-else>
            <label class="form-label" for="cnpj-cadastro"
              >CNPJ <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck
                v-if="documentoValido(cnpj, 14)"
                class="campo-valido"
            /></label>
            <input
              id="cnpj-cadastro"
              v-model="cnpj"
              class="form-control"
              inputmode="numeric"
              autocomplete="off"
              placeholder="00.000.000/0000-00"
              maxlength="18"
              required
            />
          </div>

          <div>
            <label class="form-label" for="telefone-cadastro"
              >Telefone
              <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck v-if="preenchido(telefone)" class="campo-valido"
            /></label>
            <input
              id="telefone-cadastro"
              v-model="telefone"
              class="form-control"
              type="tel"
              autocomplete="tel"
              placeholder="(00) 00000-0000"
              maxlength="30"
              required
            />
          </div>

          <div>
            <label class="form-label" for="endereco-cadastro"
              >Endereço
              <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck v-if="preenchido(endereco)" class="campo-valido"
            /></label>
            <input
              id="endereco-cadastro"
              v-model="endereco"
              class="form-control"
              autocomplete="street-address"
              placeholder="Rua, número, bairro, cidade e CEP"
              maxlength="500"
              required
            />
          </div>
          <div>
            <label class="form-label" for="nome-cadastro"
              >{{ nomeCampo }}
              <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck v-if="preenchido(nome)" class="campo-valido"
            /></label>
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
            <label class="form-label" for="email-cadastro"
              >E-mail <span class="campo-obrigatorio" aria-hidden="true">*</span
              ><BootstrapCheck v-if="emailValido" class="campo-valido"
            /></label>
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
              <label class="form-label" for="senha-cadastro"
                >Senha
                <span class="campo-obrigatorio" aria-hidden="true">*</span
                ><BootstrapCheck v-if="senhaValida(senha)" class="campo-valido"
              /></label>
              <input
                id="senha-cadastro"
                v-model="senha"
                class="form-control"
                type="password"
                placeholder="Crie uma senha"
                autocomplete="new-password"
                minlength="8"
                maxlength="128"
                aria-describedby="requisitos-senha-cadastro"
                required
              />
            </div>
            <div>
              <label class="form-label" for="confirmar-senha">
                Confirmar senha
                <span class="campo-obrigatorio" aria-hidden="true">*</span
                ><BootstrapCheck
                  v-if="confirmacaoValida"
                  class="campo-valido"
                />
              </label>
              <input
                id="confirmar-senha"
                v-model="confirmarSenha"
                class="form-control"
                type="password"
                placeholder="Digite novamente"
                autocomplete="new-password"
                minlength="8"
                maxlength="128"
                :aria-invalid="Boolean(erroConfirmacao)"
                :aria-describedby="
                  erroConfirmacao ? 'erro-confirmacao' : undefined
                "
                required
              />
              <p
                v-if="erroConfirmacao"
                id="erro-confirmacao"
                class="cadastro-confirmation-error"
                role="status"
              >
                {{ erroConfirmacao }}
              </p>
            </div>
          </div>

          <RequisitosSenha id="requisitos-senha-cadastro" :senha="senha" />

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

  </main>
</template>
