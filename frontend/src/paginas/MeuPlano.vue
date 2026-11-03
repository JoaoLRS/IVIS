<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  ArrowRight,
  BadgeCheck,
  Building2,
  CalendarDays,
  CircleHelp,
  GraduationCap,
  RefreshCw,
  Sparkles,
  Users,
} from "lucide-vue-next";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import { clienteHttp } from "../servicos/clienteHttp";

interface Plano {
  contratoId: string;
  nomePlano: string;
  origemAcesso: string;
  expiraEm: string | null;
  beneficios: string[];
  condicoesTitular: {
    quantidadeVagas: number;
    precoCentavosContratado: number;
    moeda: string;
  } | null;
}

const planos = ref<Plano[]>([]);
const carregando = ref(true);
const erro = ref("");

async function carregar() {
  carregando.value = true;
  erro.value = "";
  try {
    planos.value = (await clienteHttp.get<{ planos: Plano[] }>("/v1/conta/plano")).data.planos;
  } catch {
    erro.value = "Não foi possível consultar seu plano. Tente novamente.";
  } finally {
    carregando.value = false;
  }
}

function formatarValidade(expiraEm: string | null) {
  if (!expiraEm) return "Sem data de expiração";
  return new Date(expiraEm).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  });
}

function acessoInstitucional(plano: Plano) {
  return plano.origemAcesso === "VINCULO_INSTITUCIONAL";
}

onMounted(carregar);
</script>

<template>
  <main class="my-plan-page">
    <CabecalhoPagina
      eyebrow="SEU ACESSO"
      titulo="Meu plano"
      descricao="Consulte a validade e os benefícios do acesso à sua área de treinamento."
    />

    <section v-if="carregando" class="plan-message-card" role="status">
      <span class="plan-message-icon"><RefreshCw :size="20" /></span>
      <div>
        <h2>Consultando seu plano</h2>
        <p>Estamos buscando as informações do seu acesso.</p>
      </div>
    </section>

    <section v-else-if="erro" class="plan-message-card plan-message-card--error" role="alert">
      <span class="plan-message-icon"><CircleHelp :size="20" /></span>
      <div class="plan-message-copy">
        <h2>Não foi possível carregar seu plano</h2>
        <p>{{ erro }}</p>
        <button class="btn btn-primary plan-retry-button" type="button" @click="carregar">
          <RefreshCw :size="15" /> Tentar novamente
        </button>
      </div>
    </section>

    <section v-else-if="!planos.length" class="plan-message-card plan-message-card--empty">
      <span class="plan-message-icon"><GraduationCap :size="22" /></span>
      <div class="plan-message-copy">
        <span class="plan-card-kicker">ACESSO IVIS</span>
        <h2>Nenhum plano vigente</h2>
        <p>Escolha uma modalidade para liberar seus treinamentos e acompanhar seu desenvolvimento.</p>
        <RouterLink class="plan-action-link" to="/planos">
          Consultar ofertas <ArrowRight :size="16" />
        </RouterLink>
      </div>
    </section>

    <div v-else class="plan-list">
      <article v-for="plano in planos" :key="plano.contratoId" class="plan-access-card">
        <header class="plan-access-header">
          <div class="plan-access-identity">
            <span class="plan-access-icon">
              <Building2 v-if="acessoInstitucional(plano)" :size="23" />
              <GraduationCap v-else :size="24" />
            </span>
            <div>
              <span class="plan-card-kicker">{{ acessoInstitucional(plano) ? "ACESSO INSTITUCIONAL" : "ACESSO INDIVIDUAL" }}</span>
              <h2>{{ plano.nomePlano }}</h2>
              <p>{{ acessoInstitucional(plano) ? "Acesso por vínculo institucional" : "Plano próprio" }}</p>
            </div>
          </div>
          <span class="plan-status"><BadgeCheck :size="15" /> Vigente</span>
        </header>

        <div class="plan-details-grid">
          <section class="plan-detail-tile">
            <span class="plan-detail-icon"><CalendarDays :size="17" /></span>
            <div>
              <span class="plan-detail-label">Validade</span>
              <strong>{{ formatarValidade(plano.expiraEm) }}</strong>
            </div>
          </section>
          <section v-if="plano.condicoesTitular" class="plan-detail-tile">
            <span class="plan-detail-icon"><Users :size="17" /></span>
            <div>
              <span class="plan-detail-label">Vadas de alunos </span>
              <strong>{{ plano.condicoesTitular.quantidadeVagas }}</strong>
            </div>
          </section>
          <section class="plan-detail-tile">
            <span class="plan-detail-icon"><Sparkles :size="17" /></span>
            <div>
              <span class="plan-detail-label">Modalidade</span>
              <strong>{{ acessoInstitucional(plano) ? "Institucional" : "Individual" }}</strong>
            </div>
          </section>
        </div>

        <section class="plan-benefits">
          <header>
            <div>
              <span class="plan-card-kicker">INCLUÍDO NO SEU ACESSO</span>
              <h3>Benefícios do plano</h3>
            </div>
            <BadgeCheck :size="20" aria-hidden="true" />
          </header>
          <ul v-if="plano.beneficios.length">
            <li v-for="beneficio in plano.beneficios" :key="beneficio">
              <BadgeCheck :size="16" aria-hidden="true" />
              <span>{{ beneficio }}</span>
            </li>
          </ul>
          <p v-else class="plan-no-benefits">Consulte a instituição responsável para conhecer os benefícios disponíveis.</p>
        </section>
      </article>
    </div>
  </main>
</template>

<style scoped>
.my-plan-page {
  width: min(100%, 1040px);
  margin: 0 auto;
  padding: 8px 0 48px;
}

.plan-list {
  display: grid;
  gap: 18px;
}

.plan-access-card,
.plan-message-card {
  overflow: hidden;
  border: 1px solid #e0e2ea;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 30px #2727470b;
}

.plan-access-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px 24px;
  border-bottom: 1px solid #eaebf1;
}

.plan-access-identity {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.plan-access-icon,
.plan-message-icon {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  border-radius: 12px;
  background: #f0edff;
  color: #5c4bd4;
}

.plan-card-kicker {
  display: block;
  margin-bottom: 7px;
  color: #5f50cb;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.09em;
}

.plan-access-header h2,
.plan-message-card h2 {
  margin: 0;
  color: #292a35;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
}

.plan-access-header p,
.plan-message-card p {
  margin: 7px 0 0;
  color: #626477;
  font-size: 13px;
  line-height: 1.6;
}

.plan-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  padding: 7px 10px;
  border: 1px solid #cce8d7;
  border-radius: 99px;
  background: #f0faf4;
  color: #247447;
  font-size: 11px;
  font-weight: 700;
}

.plan-details-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 20px 28px;
  border-bottom: 1px solid #eaebf1;
  background: #fafafe;
}

.plan-detail-tile {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  min-width: 0;
  padding: 14px;
  border: 1px solid #e7e6f1;
  border-radius: 10px;
  background: #fff;
}

.plan-detail-icon {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 8px;
  background: #f5f3ff;
  color: #5e4fd0;
}

.plan-detail-tile > div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.plan-detail-label {
  color: #707284;
  font-size: 11px;
}

.plan-detail-tile strong {
  overflow-wrap: anywhere;
  color: #292a35;
  font-size: 13px;
  font-weight: 700;
}

.plan-benefits {
  padding: 22px 28px 26px;
}

.plan-benefits > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #5e4fd0;
}

.plan-benefits h3 {
  margin: 0;
  color: #292a35;
  font-size: 16px;
  font-weight: 700;
}

.plan-benefits ul {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 20px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.plan-benefits li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  color: #505164;
  font-size: 12px;
  line-height: 1.55;
}

.plan-benefits li svg {
  flex: 0 0 16px;
  margin-top: 1px;
  color: #5e9a70;
}

.plan-no-benefits {
  margin-top: 15px !important;
}

.plan-message-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 26px 28px;
}

.plan-message-card--error .plan-message-icon {
  background: #fff1f1;
  color: #a23949;
}

.plan-message-card--empty .plan-message-icon {
  background: #f0edff;
}

.plan-message-copy {
  max-width: 660px;
}

.plan-retry-button,
.plan-action-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.plan-action-link {
  color: #5849c9;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.plan-action-link:hover {
  color: #3e32a6;
  text-decoration: underline;
}

@media (max-width: 700px) {
  .plan-access-header {
    flex-direction: column;
    padding: 22px 20px;
  }

  .plan-status {
    margin-left: 64px;
  }

  .plan-details-grid {
    grid-template-columns: 1fr;
    padding: 16px 20px;
  }

  .plan-benefits {
    padding: 20px;
  }

  .plan-benefits ul {
    grid-template-columns: 1fr;
  }

  .plan-message-card {
    padding: 22px 20px;
  }
}
</style>
