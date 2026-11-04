<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { UserPlus, Users, Building2, UserRoundX, RefreshCw } from "lucide-vue-next";
import { useEstadoSessao } from "../estados/estadoSessao";
import { obterTokenCsrf } from "../servicos/perfilAluno";
import RequisitosSenha from "../componentes/RequisitosSenha.vue";
import { senhaValida } from "../validacao/senha";
import {
  cadastrarEVincularAluno,
  listarAlunosInstituicao,
  listarCapacidadeInstituicao,
  removerAlunoVinculado,
  vincularAluno,
  type AlunoVinculado,
  type CapacidadeInstitucional,
} from "../servicos/instituicao";

const sessao = useEstadoSessao();
const alunos = ref<AlunoVinculado[]>([]);
const capacidades = ref<CapacidadeInstitucional[]>([]);
const emailAluno = ref("");
const novoAluno = ref({
  nome: "",
  email: "",
  cpf: "",
  telefone: "",
  endereco: "",
  senha: "",
  confirmarSenha: "",
});
const carregando = ref(true);
const processando = ref(false);
const erro = ref("");
const sucesso = ref("");
const vagasTotais = computed(() => capacidades.value.reduce((soma, item) => soma + item.quantidadeVagas, 0));
const vagasUsadas = computed(() => capacidades.value.reduce((soma, item) => soma + item.vagasUtilizadas, 0));
const vagasLivres = computed(() => capacidades.value.reduce((soma, item) => soma + item.vagasDisponiveis, 0));
const contratoId = computed(() => capacidades.value[0]?.contratoId ?? "");

async function carregar() {
  carregando.value = true;
  erro.value = "";
  try {
    const [lista, capacidade] = await Promise.all([
      listarAlunosInstituicao(),
      listarCapacidadeInstituicao(),
    ]);
    alunos.value = lista;
    capacidades.value = capacidade;
  } catch {
    erro.value = "Não foi possível carregar a gestão da instituição. Atualize a sessão e tente novamente.";
  } finally {
    carregando.value = false;
  }
}

async function adicionarAluno() {
  if (!contratoId.value || !emailAluno.value.trim()) return;
  processando.value = true;
  erro.value = "";
  sucesso.value = "";
  try {
    const csrf = await obterTokenCsrf();
    const resultado = await vincularAluno(emailAluno.value.trim(), contratoId.value, csrf);
    alunos.value.unshift(resultado.vinculo);
    capacidades.value = capacidades.value.map((item) => item.contratoId === resultado.capacidade.contratoId ? resultado.capacidade : item);
    emailAluno.value = "";
    sucesso.value = "Aluno vinculado ao plano da instituição.";
  } catch (falha) {
    const resposta = (falha as { response?: { data?: { mensagem?: string } } }).response;
    erro.value = resposta?.data?.mensagem ?? "Não foi possível vincular o aluno. Confira o e-mail e as vagas disponíveis.";
  } finally {
    processando.value = false;
  }
}

async function cadastrarAluno() {
  if (!contratoId.value || !vagasLivres.value) return;
  erro.value = "";
  sucesso.value = "";
  if (!senhaValida(novoAluno.value.senha)) {
    erro.value = "A senha do aluno ainda não atende a todos os requisitos informados.";
    return;
  }
  if (novoAluno.value.senha !== novoAluno.value.confirmarSenha) {
    erro.value = "As senhas não coincidem. Confira e tente novamente.";
    return;
  }
  processando.value = true;
  try {
    const csrf = await obterTokenCsrf();
    const resultado = await cadastrarEVincularAluno(
      {
        contratoId: contratoId.value,
        nome: novoAluno.value.nome.trim(),
        email: novoAluno.value.email.trim(),
        cpf: novoAluno.value.cpf,
        telefone: novoAluno.value.telefone,
        endereco: novoAluno.value.endereco,
        senha: novoAluno.value.senha,
      },
      csrf,
    );
    alunos.value.unshift(resultado.vinculacao.vinculo);
    capacidades.value = capacidades.value.map((item) =>
      item.contratoId === resultado.vinculacao.capacidade.contratoId
        ? resultado.vinculacao.capacidade
        : item,
    );
    novoAluno.value = {
      nome: "",
      email: "",
      cpf: "",
      telefone: "",
      endereco: "",
      senha: "",
      confirmarSenha: "",
    };
    sucesso.value = "Conta criada e aluno vinculado ao plano da instituição.";
  } catch (falha) {
    const resposta = (falha as { response?: { data?: { mensagem?: string } } }).response;
    erro.value = resposta?.data?.mensagem ?? "Não foi possível cadastrar e vincular o aluno.";
  } finally {
    processando.value = false;
  }
}

async function removerAluno(aluno: AlunoVinculado) {
  processando.value = true;
  erro.value = "";
  sucesso.value = "";
  try {
    const csrf = await obterTokenCsrf();
    await removerAlunoVinculado(aluno.id, csrf);
    alunos.value = alunos.value.filter((item) => item.id !== aluno.id);
    await carregarCapacidade();
    sucesso.value = `${aluno.nomeAluno} foi removido do plano institucional.`;
  } catch (falha) {
    const resposta = (falha as { response?: { data?: { mensagem?: string } } }).response;
    erro.value = resposta?.data?.mensagem ?? "Não foi possível remover o aluno.";
  } finally {
    processando.value = false;
  }
}

async function carregarCapacidade() {
  capacidades.value = await listarCapacidadeInstituicao();
}

onMounted(carregar);
</script>

<template>
  <section class="institution-page">
    <header class="institution-heading">
      <div>
        <span class="institution-kicker">ÁREA DA INSTITUIÇÃO</span>
        <h1>{{ sessao.conta?.nome }}</h1>
        <p>Gerencie os alunos que usam o plano da sua instituição.</p>
      </div>
      <div class="institution-admin"><Building2 :size="18" /><span><small>Responsável</small><strong>{{ sessao.conta?.nomeResponsavel || sessao.conta?.nome }}</strong></span></div>
    </header>

    <div class="institution-stats">
      <article><Users :size="20" /><span>Alunos vinculados<strong>{{ alunos.length }}</strong></span></article>
      <article><Building2 :size="20" /><span>Vadas de alunos <strong>{{ vagasTotais }}</strong></span></article>
      <article><UserPlus :size="20" /><span>Vagas disponíveis<strong>{{ vagasLivres }}</strong></span></article>
    </div>

    <section class="institution-card">
      <header><div><h2>Alunos participantes</h2><p>Vincule uma conta existente ou cadastre um novo aluno.</p></div><button type="button" class="institution-refresh" :disabled="carregando" @click="carregar"><RefreshCw :size="16" /> Atualizar</button></header>
      <form v-if="contratoId" class="institution-add-form" @submit.prevent="adicionarAluno">
        <label for="email-aluno">E-mail da conta do aluno</label>
        <div><input id="email-aluno" v-model="emailAluno" type="email" autocomplete="email" placeholder="aluno@exemplo.com" required /><button type="submit" :disabled="processando || !vagasLivres"><UserPlus :size="16" /> {{ processando ? "Salvando..." : "Vincular aluno" }}</button></div>
        <small>{{ vagasUsadas }} de {{ vagasTotais }} vagas ocupadas. Remover um aluno libera uma vaga.</small>
      </form>
      <form v-if="contratoId" class="institution-register-form" @submit.prevent="cadastrarAluno">
        <div class="institution-register-heading"><h3>Cadastrar novo aluno</h3><p>Crie a conta com os dados do aluno. O vínculo será feito com esta instituição.</p></div>
        <div class="institution-register-grid">
          <label>Nome completo<input v-model="novoAluno.nome" autocomplete="name" maxlength="160" required /></label>
          <label>E-mail<input v-model="novoAluno.email" type="email" autocomplete="email" maxlength="254" required /></label>
          <label>CPF<input v-model="novoAluno.cpf" inputmode="numeric" maxlength="14" placeholder="000.000.000-00" required /></label>
          <label>Telefone<input v-model="novoAluno.telefone" type="tel" autocomplete="tel" maxlength="30" required /></label>
          <label class="institution-register-wide">Endereço<input v-model="novoAluno.endereco" autocomplete="street-address" maxlength="500" required /></label>
          <label>Senha<input v-model="novoAluno.senha" type="password" autocomplete="new-password" minlength="8" maxlength="128" aria-describedby="requisitos-senha-aluno" required /></label>
          <label>Confirmar senha<input v-model="novoAluno.confirmarSenha" type="password" autocomplete="new-password" minlength="8" maxlength="128" required /></label>
        </div>
        <RequisitosSenha id="requisitos-senha-aluno" :senha="novoAluno.senha" />
        <button type="submit" :disabled="processando || !vagasLivres"><UserPlus :size="16" /> {{ processando ? "Salvando..." : "Cadastrar e vincular" }}</button>
      </form>
      <p v-else class="institution-empty">Nenhum plano institucional ativo foi encontrado. Contrate um plano para liberar as vagas para seus alunos.</p>
      <p v-if="erro" class="institution-alert error" role="alert">{{ erro }}</p>
      <p v-if="sucesso" class="institution-alert success" role="status">{{ sucesso }}</p>
      <div v-if="carregando" class="institution-empty">Carregando os alunos vinculados...</div>
      <div v-else-if="alunos.length" class="institution-table-wrap">
        <table><thead><tr><th>Aluno</th><th>E-mail</th><th>Vinculado em</th><th></th></tr></thead>
          <tbody><tr v-for="aluno in alunos" :key="aluno.id"><td>{{ aluno.nomeAluno }}</td><td>{{ aluno.emailAluno }}</td><td>{{ new Date(aluno.vinculadoEm).toLocaleDateString('pt-BR') }}</td><td><button type="button" class="institution-remove" :disabled="processando" :aria-label="`Remover ${aluno.nomeAluno}`" @click="removerAluno(aluno)"><UserRoundX :size="16" /> Remover</button></td></tr></tbody>
        </table>
      </div>
      <div v-else-if="contratoId" class="institution-empty">Ainda não há alunos neste plano. Use uma das opções acima para adicionar o primeiro aluno.</div>
    </section>
  </section>
</template>

<style scoped>
.institution-page{max-width:1180px;margin:0 auto;padding:20px 0 48px;color:#282936}.institution-heading{display:flex;justify-content:space-between;align-items:end;gap:24px;margin:10px 0 26px}.institution-kicker{color:#6555dc;font-size:11px;font-weight:700;letter-spacing:1.2px}.institution-heading h1{margin:7px 0;color:#292a35;font:700 28px Manrope,sans-serif}.institution-heading p{margin:0;color:#707284;font-size:14px}.institution-admin{display:flex;align-items:center;gap:10px;padding:13px 16px;border:1px solid #e4e4ed;background:#fff}.institution-admin span{display:grid;gap:3px}.institution-admin small{color:#777988;font-size:11px}.institution-admin strong{font-size:13px}.institution-stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;margin-bottom:20px}.institution-stats article{display:flex;align-items:center;gap:13px;padding:18px;border:1px solid #e4e4ed;background:#fff;color:#6655dc}.institution-stats span{display:grid;gap:4px;color:#707284;font-size:12px}.institution-stats strong{color:#22232f;font-size:22px}.institution-card{border:1px solid #e4e4ed;background:#fff}.institution-card>header{display:flex;justify-content:space-between;align-items:center;padding:20px 22px;border-bottom:1px solid #ededf2}.institution-card h2{margin:0;color:#292a35;font:700 18px Manrope,sans-serif}.institution-card header p{margin:5px 0 0;color:#777988;font-size:12px}.institution-refresh,.institution-remove{display:inline-flex;align-items:center;gap:7px;border:1px solid #dedde9;background:#fff;padding:9px 12px;color:#5144bf;font-size:12px;font-weight:600}.institution-add-form{display:grid;gap:8px;padding:18px 22px;border-bottom:1px solid #ededf2}.institution-add-form label{font-size:12px;font-weight:700}.institution-add-form>div{display:flex;gap:9px}.institution-add-form input{flex:1;min-width:0;height:42px;padding:0 12px;border:1px solid #dcdce7}.institution-add-form button,.institution-register-form>button{display:inline-flex;align-items:center;gap:7px;padding:0 15px;border:0;background:#604fe0;color:#fff;font-weight:600}.institution-add-form button:disabled,.institution-register-form>button:disabled{opacity:.55}.institution-add-form small{color:#777988;font-size:11px}.institution-register-form{display:grid;gap:14px;padding:20px 22px;border-bottom:1px solid #ededf2;background:#fbfbfe}.institution-register-heading h3{margin:0;color:#292a35;font:700 15px Manrope,sans-serif}.institution-register-heading p{margin:5px 0 0;color:#777988;font-size:12px}.institution-register-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.institution-register-grid label{display:grid;gap:6px;color:#444552;font-size:12px;font-weight:700}.institution-register-grid input{min-width:0;height:40px;padding:0 11px;border:1px solid #dcdce7;background:#fff;font:400 13px sans-serif}.institution-register-wide{grid-column:1/-1}.institution-register-form>button{justify-self:start;height:40px}.institution-table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;text-align:left}th,td{padding:14px 18px;border-bottom:1px solid #ededf2;font-size:12px}th{color:#777988;font-weight:600}td{color:#383946}.institution-remove{color:#a13b49}.institution-empty{padding:22px;color:#777988;font-size:13px}.institution-alert{margin:14px 22px;padding:11px 13px;font-size:12px}.institution-alert.error{background:#fff0f0;color:#a13b49}.institution-alert.success{background:#ecf8ef;color:#287a49}@media(max-width:700px){.institution-heading{align-items:flex-start;flex-direction:column}.institution-stats{grid-template-columns:1fr}.institution-card>header{align-items:flex-start;gap:12px;flex-direction:column}.institution-add-form>div{align-items:stretch;flex-direction:column}.institution-add-form button{justify-content:center;height:42px}.institution-register-grid{grid-template-columns:1fr}.institution-register-wide{grid-column:auto}.institution-register-form>button{width:100%;justify-content:center}}
  .institution-page input:not([type="radio"]):not([type="checkbox"]) {
    background: #fff;
    border: 1px solid #9298a7;
    color: #292a35;
    transition:
      border-color 0.15s ease,
      box-shadow 0.15s ease;
  }

  .institution-page input:not([type="radio"]):not([type="checkbox"]):hover {
    border-color: #687084;
  }

  .institution-page input:not([type="radio"]):not([type="checkbox"]):focus {
    border-color: #6757df;
    box-shadow: 0 0 0 0.2rem #6757df1b;
    outline: 0;
  }
</style>
