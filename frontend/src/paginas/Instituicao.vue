<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { UserPlus, Users, Building2, UserRoundX, RefreshCw } from "lucide-vue-next";
import { useEstadoSessao } from "../estados/estadoSessao";
import { obterTokenCsrf } from "../servicos/perfilAluno";
import {
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
      <div class="institution-admin"><Building2 :size="18" /><span><small>Administrador</small><strong>{{ sessao.conta?.nomeAdministrador || sessao.conta?.nome }}</strong></span></div>
    </header>

    <div class="institution-stats">
      <article><Users :size="20" /><span>Alunos vinculados<strong>{{ alunos.length }}</strong></span></article>
      <article><Building2 :size="20" /><span>Vagas contratadas<strong>{{ vagasTotais }}</strong></span></article>
      <article><UserPlus :size="20" /><span>Vagas disponíveis<strong>{{ vagasLivres }}</strong></span></article>
    </div>

    <section class="institution-card">
      <header><div><h2>Alunos participantes</h2><p>Adicione alunos cadastrados pelo e-mail da conta IVIS.</p></div><button type="button" class="institution-refresh" :disabled="carregando" @click="carregar"><RefreshCw :size="16" /> Atualizar</button></header>
      <form v-if="contratoId" class="institution-add-form" @submit.prevent="adicionarAluno">
        <label for="email-aluno">E-mail da conta do aluno</label>
        <div><input id="email-aluno" v-model="emailAluno" type="email" autocomplete="email" placeholder="aluno@exemplo.com" required /><button type="submit" :disabled="processando || !vagasLivres"><UserPlus :size="16" /> {{ processando ? "Salvando..." : "Vincular aluno" }}</button></div>
        <small>{{ vagasUsadas }} de {{ vagasTotais }} vagas ocupadas. Remover um aluno libera uma vaga.</small>
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
      <div v-else-if="contratoId" class="institution-empty">Ainda não há alunos neste plano. Vincule uma conta IVIS usando o e-mail do aluno.</div>
    </section>
  </section>
</template>

<style scoped>
.institution-page{max-width:1180px;margin:0 auto;padding:20px 0 48px;color:#282936}.institution-heading{display:flex;justify-content:space-between;align-items:end;gap:24px;margin:10px 0 26px}.institution-kicker{color:#6555dc;font-size:11px;font-weight:700;letter-spacing:1.2px}.institution-heading h1{margin:7px 0;color:#292a35;font:700 28px Manrope,sans-serif}.institution-heading p{margin:0;color:#707284;font-size:14px}.institution-admin{display:flex;align-items:center;gap:10px;padding:13px 16px;border:1px solid #e4e4ed;background:#fff}.institution-admin span{display:grid;gap:3px}.institution-admin small{color:#777988;font-size:11px}.institution-admin strong{font-size:13px}.institution-stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;margin-bottom:20px}.institution-stats article{display:flex;align-items:center;gap:13px;padding:18px;border:1px solid #e4e4ed;background:#fff;color:#6655dc}.institution-stats span{display:grid;gap:4px;color:#707284;font-size:12px}.institution-stats strong{color:#22232f;font-size:22px}.institution-card{border:1px solid #e4e4ed;background:#fff}.institution-card>header{display:flex;justify-content:space-between;align-items:center;padding:20px 22px;border-bottom:1px solid #ededf2}.institution-card h2{margin:0;color:#292a35;font:700 18px Manrope,sans-serif}.institution-card header p{margin:5px 0 0;color:#777988;font-size:12px}.institution-refresh,.institution-remove{display:inline-flex;align-items:center;gap:7px;border:1px solid #dedde9;background:#fff;padding:9px 12px;color:#5144bf;font-size:12px;font-weight:600}.institution-add-form{display:grid;gap:8px;padding:18px 22px;border-bottom:1px solid #ededf2}.institution-add-form label{font-size:12px;font-weight:700}.institution-add-form>div{display:flex;gap:9px}.institution-add-form input{flex:1;min-width:0;height:42px;padding:0 12px;border:1px solid #dcdce7}.institution-add-form button{display:inline-flex;align-items:center;gap:7px;padding:0 15px;border:0;background:#604fe0;color:#fff;font-weight:600}.institution-add-form button:disabled{opacity:.55}.institution-add-form small{color:#777988;font-size:11px}.institution-table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;text-align:left}th,td{padding:14px 18px;border-bottom:1px solid #ededf2;font-size:12px}th{color:#777988;font-weight:600}td{color:#383946}.institution-remove{color:#a13b49}.institution-empty{padding:22px;color:#777988;font-size:13px}.institution-alert{margin:14px 22px;padding:11px 13px;font-size:12px}.institution-alert.error{background:#fff0f0;color:#a13b49}.institution-alert.success{background:#ecf8ef;color:#287a49}@media(max-width:700px){.institution-heading{align-items:flex-start;flex-direction:column}.institution-stats{grid-template-columns:1fr}.institution-card>header{align-items:flex-start;gap:12px;flex-direction:column}.institution-add-form>div{align-items:stretch;flex-direction:column}.institution-add-form button{justify-content:center;height:42px}}
</style>
