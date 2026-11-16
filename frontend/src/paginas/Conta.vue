<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { CheckCircle2, X } from "lucide-vue-next";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import { useEstadoSessao } from "../estados/estadoSessao";
import { atualizarMinhaConta } from "../servicos/conta";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";

const estado = useEstadoAplicacao();
const sessao = useEstadoSessao();
const nome = ref(sessao.conta?.nome ?? estado.nomeAluno);
const nomeResponsavel = ref(sessao.conta?.nomeResponsavel ?? "");
const email = ref(sessao.conta?.email ?? estado.emailAluno);
const cpf = ref(sessao.conta?.cpf ?? "");
const cnpj = ref(sessao.conta?.cnpj ?? "");
const telefone = ref(sessao.conta?.telefone ?? "");
const endereco = ref(sessao.conta?.endereco ?? "");
const mostrarConfirmacao = ref(false);
const erro = ref("");
let temporizadorConfirmacao: ReturnType<typeof setTimeout> | undefined;

const dadosAlterados = computed(() => {
  const conta = sessao.conta;
  if (!conta) return false;

  return (
    nome.value !== conta.nome ||
    email.value !== conta.email ||
    telefone.value !== (conta.telefone ?? "") ||
    endereco.value !== (conta.endereco ?? "") ||
    (conta.tipoConta === "INSTITUICAO"
      ? nomeResponsavel.value !== (conta.nomeResponsavel ?? "") ||
        cnpj.value !== (conta.cnpj ?? "")
      : cpf.value !== (conta.cpf ?? ""))
  );
});

function fecharConfirmacao() {
  mostrarConfirmacao.value = false;
  if (temporizadorConfirmacao) clearTimeout(temporizadorConfirmacao);
  temporizadorConfirmacao = undefined;
}

onBeforeUnmount(fecharConfirmacao);

watch(
  () => sessao.conta,
  (conta) => {
    if (!conta) return;
    nome.value = conta.nome;
    nomeResponsavel.value = conta.nomeResponsavel ?? "";
    email.value = conta.email;
    cpf.value = conta.cpf ?? "";
    cnpj.value = conta.cnpj ?? "";
    telefone.value = conta.telefone ?? "";
    endereco.value = conta.endereco ?? "";
  },
);

async function salvar() {
  if (!dadosAlterados.value) return;
  erro.value = "";
  fecharConfirmacao();
  try {
    const conta = await atualizarMinhaConta({
      email: email.value,
      telefone: telefone.value,
      endereco: endereco.value,
      ...(sessao.conta?.tipoConta === "INSTITUICAO"
        ? {
            nomeInstituicao: nome.value,
            nomeResponsavel: nomeResponsavel.value,
            cnpj: cnpj.value,
          }
        : { nome: nome.value, cpf: cpf.value }),
    });
    estado.atualizarDadosAluno({ nome: conta.nome, email: conta.email });
    sessao.conta = conta;
    nome.value = conta.nome;
    nomeResponsavel.value = conta.nomeResponsavel ?? "";
    email.value = conta.email;
    cpf.value = conta.cpf ?? "";
    cnpj.value = conta.cnpj ?? "";
    telefone.value = conta.telefone ?? "";
    endereco.value = conta.endereco ?? "";
    mostrarConfirmacao.value = true;
    temporizadorConfirmacao = setTimeout(fecharConfirmacao, 5000);
  } catch (falha) {
    const resposta = (falha as { response?: { data?: { mensagem?: string } } })
      .response;
    erro.value =
      resposta?.data?.mensagem ?? "Não foi possível salvar os dados da conta.";
  }
}
</script>

<template>
  <div class="account-page">
    <Transition name="account-toast">
      <div v-if="mostrarConfirmacao" class="account-toast-notice" role="status">
        <CheckCircle2 :size="20" aria-hidden="true" />
        <span>Dados atualizados.</span>
        <button
          type="button"
          aria-label="Fechar aviso"
          @click="fecharConfirmacao"
        >
          <X :size="16" />
        </button>
      </div>
    </Transition>
    <CabecalhoPagina
      :eyebrow="
        sessao.conta?.tipoConta === 'INSTITUICAO'
          ? 'CONTA DA INSTITUIÇÃO'
          : 'CONTA DO ALUNO'
      "
      :titulo="
        sessao.conta?.tipoConta === 'INSTITUICAO'
          ? 'Editar dados da instituição'
          : 'Editar dados do aluno'
      "
      descricao="Atualize os dados persistidos da sua conta."
    />

    <section class="card account-edit-card">
      <form class="card-body" @submit.prevent="salvar">
        <div v-if="sessao.conta?.tipoConta === 'INSTITUICAO'" class="mb-3">
          <label class="form-label" for="nome-responsavel"
            >Nome do responsável</label
          >
          <input
            id="nome-responsavel"
            v-model="nomeResponsavel"
            class="form-control"
            autocomplete="name"
            required
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="nome-conta">{{
            sessao.conta?.tipoConta === "INSTITUICAO"
              ? "Nome da instituição"
              : "Nome"
          }}</label>
          <input
            id="nome-conta"
            v-model="nome"
            class="form-control"
            autocomplete="name"
            required
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="email-conta">E-mail</label>
          <input
            id="email-conta"
            v-model="email"
            class="form-control"
            type="email"
            autocomplete="email"
            required
          />
        </div>
        <div class="mb-3">
          <label
            class="form-label"
            :for="
              sessao.conta?.tipoConta === 'INSTITUICAO'
                ? 'cnpj-conta'
                : 'cpf-conta'
            "
          >
            {{ sessao.conta?.tipoConta === "INSTITUICAO" ? "CNPJ" : "CPF" }}
          </label>
          <input
            v-if="sessao.conta?.tipoConta === 'INSTITUICAO'"
            id="cnpj-conta"
            v-model="cnpj"
            class="form-control"
            inputmode="numeric"
            maxlength="18"
            required
          />
          <input
            v-else
            id="cpf-conta"
            v-model="cpf"
            class="form-control"
            inputmode="numeric"
            maxlength="14"
            required
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="telefone-conta">Telefone</label>
          <input
            id="telefone-conta"
            v-model="telefone"
            class="form-control"
            type="tel"
            autocomplete="tel"
            maxlength="30"
            required
          />
        </div>
        <div class="mb-4">
          <label class="form-label" for="endereco-conta">Endereço</label>
          <input
            id="endereco-conta"
            v-model="endereco"
            class="form-control"
            autocomplete="street-address"
            maxlength="500"
            required
          />
        </div>
        <div v-if="sessao.conta?.tipoConta === 'ALUNO'" class="mb-4">
          <label class="form-label" for="instituicao-conta"
            >Código do objeto da instituição vinculada</label
          >
          <input
            id="instituicao-conta"
            class="form-control"
            :value="
              sessao.conta.objetoInstituicao || 'Sem instituição vinculada'
            "
            readonly
          />
        </div>
        <p v-if="erro" class="alert alert-danger" role="alert">{{ erro }}</p>
        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-primary" type="submit" :disabled="!dadosAlterados">
            Salvar alterações
          </button>
        </div>
      </form>
    </section>
    <section class="card account-security-card mt-4">
      <div class="card-body">
        <div>
          <span class="eyebrow">SEGURANÇA</span>
          <h2>Senha e acesso</h2>
          <p>Altere sua senha em uma página dedicada.</p>
        </div>
        <RouterLink class="btn btn-outline-secondary" to="/alterar-senha">
          Alterar senha
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.account-toast-notice {
  position: fixed;
  top: 72px;
  right: 24px;
  z-index: 1050;
  width: min(360px, calc(100vw - 32px));
  min-height: 56px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #b8dbb0;
  border-left: 4px solid #4f914d;
  border-radius: 8px;
  color: #244c2c;
  background: #f5fbf1;
  box-shadow: 0 14px 32px #151a3640;
  font-size: 14px;
  font-weight: 700;
}
.account-toast-notice > svg {
  flex: none;
  color: #4f914d;
}
.account-toast-notice button {
  margin-left: auto;
  padding: 4px;
  display: inline-flex;
  border: 0;
  color: #45664b;
  background: transparent;
  cursor: pointer;
}
.account-toast-notice button:focus-visible {
  outline: 2px solid #4f914d;
  outline-offset: 2px;
}
.account-toast-enter-active,
.account-toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.account-toast-enter-from,
.account-toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.account-page {
  max-width: 980px;
}

.account-edit-card {
  max-width: 860px;
  background: #fff;
}

.account-security-card {
  max-width: 860px;
  background: #fff;
}

.account-security-card .card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.account-security-card h2 {
  margin: 6px 0;
  color: #292a35;
  font-size: 20px;
  font-weight: 700;
}

.account-security-card p {
  margin: 0;
  color: #626477;
}

@media (max-width: 575.98px) {
  .account-security-card .card-body {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
