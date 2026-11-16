<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  ArrowRight,
  Building2,
  Check,
  Clock3,
  GraduationCap,
  LogOut,
  ShieldCheck,
  UserRound,
} from "lucide-vue-next";
import { RouterLink, useRoute } from "vue-router";
import { clienteHttp } from "../servicos/clienteHttp";
import { useEstadoSessao } from "../estados/estadoSessao";

interface Oferta {
  id: string;
  codigo: string;
  modalidade: "INDIVIDUAL" | "INSTITUCIONAL";
  nome: string;
  descricao: string | null;
  precoCentavos: number;
  moeda: string;
  duracaoDias: number;
  vagasIncluidas: number | null;
  beneficios: string[];
}

const rota = useRoute();
const sessao = useEstadoSessao();
const ofertas = ref<Oferta[]>([]);
const carregando = ref(true);
const erro = ref("");
const acessoNecessario = rota.query.acesso === "necessario";

async function sairDaConta() {
  await sessao.sair();
}

function preco(oferta: Oferta) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: oferta.moeda,
  }).format(oferta.precoCentavos / 100);
}

function descricaoExibida(descricao: string | null) {
  return (descricao ?? "")
    .replace(/\s*[.;]\s*nenhuma cobrança real é realizada\./i, ".")
    .trim();
}

function destinoCompra(oferta: Oferta) {
  const redirect = `/checkout/${oferta.id}`;
  return sessao.conta
    ? { name: "checkout", params: { planoId: oferta.id } }
    : {
        name: "cadastro",
        query: {
          redirect,
          ...(oferta.modalidade === "INSTITUCIONAL"
            ? { tipo: "instituicao" }
            : {}),
        },
      };
}

onMounted(async () => {
  try {
    const resposta = await clienteHttp.get<Oferta[]>("/v1/planos");
    ofertas.value = resposta.data;
  } catch {
    erro.value =
      "Não foi possível carregar as ofertas. Tente novamente mais tarde.";
  } finally {
    carregando.value = false;
  }
});
</script>

<template>
  <main class="plans-page">
    <header class="plans-header">
      <div class="plans-header-inner">
        <RouterLink class="plans-brand" to="/">IVIS<span>.</span></RouterLink>
        <nav aria-label="Navegação principal">
          <a href="/#como-funciona">Como funciona</a>
          <RouterLink to="/planos" aria-current="page">Planos</RouterLink>
          <RouterLink v-if="sessao.conta && sessao.temPlano" to="/inicio"
            ><UserRound :size="16" /> Minha área</RouterLink
          >
          <RouterLink v-else-if="sessao.conta" to="/planos?acesso=necessario"
            ><UserRound :size="16" /> Ativar acesso</RouterLink
          >
          <RouterLink v-else to="/login"><UserRound :size="16" /> Entrar</RouterLink>
          <button
            v-if="sessao.conta"
            class="plans-logout"
            type="button"
            @click="sairDaConta"
          >
            <LogOut :size="15" /> Sair
          </button>
        </nav>
      </div>
    </header>

    <section class="plans-hero">
      <span class="plans-kicker">ACESSO IVIS</span>
      <h1>Um plano simples para começar a praticar</h1>

      <div
        v-if="
          acessoNecessario ||
          (sessao.conta?.tipoConta === 'ALUNO' && sessao.temPlano === false)
        "
        class="plans-access-notice"
        role="status"
      >
        Sua conta precisa de um plano ativo ou de um vínculo institucional para
        abrir a área de treinamento.
        <RouterLink v-if="sessao.conta?.tipoConta === 'ALUNO'" to="/historico"
          >Seu histórico continua disponível.</RouterLink
        >
      </div>
    </section>

    <section class="plans-content">
      <div class="plans-section-heading">
        <div>
          <span class="plans-section-kicker">OFERTA DE DEMONSTRAÇÃO</span>
          <h2>Escolha sua modalidade</h2>
        </div>
      </div>

      <div v-if="carregando" class="plans-message">Carregando catálogo...</div>
      <div
        v-else-if="erro"
        class="plans-message plans-message-error"
        role="alert"
      >
        {{ erro }}
      </div>
      <div v-else-if="!ofertas.length" class="plans-message">
        O catálogo ainda não tem ofertas ativas. Reinicie o backend para aplicar
        as migrations do catálogo.
      </div>

      <div v-else class="plans-grid" aria-label="Planos disponíveis">
        <article v-for="oferta in ofertas" :key="oferta.id" class="plan-card">
          <div class="plan-card-top">
            <span class="plan-type"
              ><GraduationCap
                v-if="oferta.modalidade === 'INDIVIDUAL'"
                :size="16"
              />
              <Building2 v-else :size="16" />
              {{
                oferta.modalidade === "INDIVIDUAL"
                  ? "Acesso individual"
                  : "Acesso institucional"
              }}
            </span>
            <span class="plan-demo-tag">DEMONSTRAÇÃO</span>
          </div>
          <h3>{{ oferta.nome }}</h3>
          <p class="plan-description">{{ descricaoExibida(oferta.descricao) }}</p>

          <div class="plan-price-line">
            <strong>{{ preco(oferta) }}</strong>
            <span v-if="oferta.modalidade === 'INSTITUCIONAL'">
              {{ oferta.vagasIncluidas === null ? "Alunos ilimitados" : `Até ${oferta.vagasIncluidas} alunos` }} ·
              {{ oferta.duracaoDias }} dias
            </span>
            <span v-else>por {{ oferta.duracaoDias }} dias</span>
          </div>
          <RouterLink
            v-if="sessao.temPlano"
            class="plan-action plan-action-secondary"
            to="/inicio"
          >
            Acessar minha área <ArrowRight :size="16" />
          </RouterLink>
          <RouterLink
            v-else-if="
              (oferta.modalidade === 'INDIVIDUAL' &&
                sessao.conta?.tipoConta === 'ALUNO') ||
              (oferta.modalidade === 'INSTITUCIONAL' &&
                sessao.conta?.tipoConta === 'INSTITUICAO')
            "
            class="plan-action"
            :to="destinoCompra(oferta)"
          >
            {{
              oferta.modalidade === "INSTITUCIONAL"
                ? "Contratar plano institucional"
                : "Continuar para pagamento simulado"
            }}
            <ArrowRight :size="16" />
          </RouterLink>
          <RouterLink
            v-else-if="!sessao.conta"
            class="plan-action"
            :to="destinoCompra(oferta)"
          >
            {{
              oferta.modalidade === "INSTITUCIONAL"
                ? "Criar conta institucional"
                : "Criar conta e continuar"
            }}
            <ArrowRight :size="16" />
          </RouterLink>
          <p v-else class="plan-institution-note">
            {{
              oferta.modalidade === "INSTITUCIONAL"
                ? "Disponível para contas de instituição. A instituição poderá vincular alunos após ativar o plano."
                : "O acesso por convênio é concedido por vínculo ativo criado pela instituição."
            }}
          </p>
          <div class="plan-divider" />
          <ul class="plan-benefits">
            <li v-for="beneficio in oferta.beneficios" :key="beneficio">
              <Check :size="16" /> <span>{{ beneficio }}</span>
            </li>
          </ul>
        </article>
      </div>

      <div class="plans-simulation-panel">
        <div class="plans-simulation-icon"><ShieldCheck :size="22" /></div>
        <div>
          <strong>Compra 100% simulada para a atividade acadêmica</strong>
          <p>
            O simulador valida cartões fictícios no backend, ativa o acesso por
            30 dias e mostra uma confirmação de e-mail simulada dentro do IVIS.
            Nenhuma transação sai do sistema.
          </p>
        </div>
        <div class="plans-test-card">
          <Clock3 :size="15" /> Cartão de teste disponível no checkout
        </div>
      </div>
    </section>

    <footer class="plans-footer">
      <RouterLink to="/">IVIS<span>.</span></RouterLink>
    </footer>
  </main>
</template>

<style scoped>
.plans-page {
  min-height: 100vh;
  color: #25263a;
  background: #f7f7fb;
  font-family: Manrope, Inter, sans-serif;
}
.plans-header {
  background: #141830;
  border-bottom: 1px solid #ffffff1f;
}
.plans-header-inner {
  width: min(100% - 48px, 1120px);
  min-height: 82px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}
.plans-brand,
.plans-footer > a {
  color: #fff;
  font:
    800 27px Manrope,
    sans-serif;
  letter-spacing: -1.4px;
  text-decoration: none;
}
.plans-brand span,
.plans-footer > a span {
  color: #c4f474;
}
.plans-header nav {
  display: flex;
  align-items: center;
  gap: clamp(18px, 3vw, 42px);
}
.plans-header nav a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #e2e5f5;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
}
.plans-header nav a[aria-current="page"] {
  color: #c4f474;
}
.plans-header nav a:last-of-type {
  color: #fff;
}
.plans-logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.plans-header nav a:hover {
  color: #c4f474;
}
.plans-logout:hover {
  color: #c4f474;
}
.plans-header nav a:focus-visible,
.plans-header nav button:focus-visible {
  outline-color: #c4f474;
}
.plans-hero {
  padding: 53px 22px 45px;
  color: #fff;
  background: linear-gradient(112deg, #11152e 0%, #201c50 58%, #39317c 100%);
  text-align: center;
}
.plans-kicker,
.plans-section-kicker {
  color: #aaa2ff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.plans-hero h1 {
  max-width: 700px;
  margin: 12px auto 10px;
  color: #fff;
  font:
    800 clamp(29px, 4vw, 43px)/1.15 Manrope,
    sans-serif;
  letter-spacing: -1.4px;
}
.plans-access-notice {
  width: min(100%, 680px);
  margin: 20px auto 0;
  padding: 12px 15px;
  border: 1px solid #ffffff34;
  border-radius: 10px;
  color: #fff;
  background: #ffffff12;
  font-size: 11px;
}
.plans-content {
  width: min(100% - 48px, 1320px);
  margin: 0 auto;
  padding: 43px 0 55px;
}
.plans-section-heading {
  margin-bottom: 21px;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}
.plans-section-kicker {
  color: #6757df;
}
.plans-section-heading h2 {
  margin: 6px 0 0;
  color: #24263a;
  font:
    800 24px Manrope,
    sans-serif;
  letter-spacing: -0.6px;
}
.plans-message {
  padding: 22px;
  border: 1px solid #e7e7ef;
  border-radius: 12px;
  color: #636576;
  background: #fff;
  font-size: 12px;
  text-align: center;
}
.plans-message-error {
  color: #9b3c43;
}
.plans-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}
.plan-action:focus-visible,
.plans-header a:focus-visible,
.plans-header button:focus-visible {
  outline: 2px solid #6757df;
  outline-offset: 3px;
}
.plan-card {
  --card-text: #202344;
  --card-muted: #5b6079;
  --card-accent: #5142c5;
  --card-benefits: #e7ebff;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 24px 22px 0;
  border: 1px solid #d8dcf0;
  border-radius: 10px;
  color: var(--card-text);
  background: #f7f8ff;
  overflow: hidden;
}
.plan-card:nth-child(2) {
  --card-text: #fff;
  --card-muted: #d9e2ff;
  --card-accent: #e1e8ff;
  --card-benefits: #3735a3;
  border-color: #554fd1;
  background: #4a40bb;
}
.plan-card:nth-child(3) {
  --card-text: #fff;
  --card-muted: #d7dcff;
  --card-accent: #e6e8ff;
  --card-benefits: #252a83;
  border-color: #353d9c;
  background: #292d91;
}
.plan-card:nth-child(4) {
  --card-text: #fff;
  --card-muted: #d1d9f5;
  --card-accent: #e2e9ff;
  --card-benefits: #171e5c;
  border-color: #242b69;
  background: #171b53;
}
.plan-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.plan-type {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--card-accent);
  font-size: 11px;
  font-weight: 800;
}
.plan-demo-tag {
  padding: 6px 8px;
  border-radius: 999px;
  color: #18321d;
  background: #c5f477;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.6px;
}
.plan-card h3 {
  min-height: 65px;
  margin: 28px 0 8px;
  color: var(--card-text);
  font:
    800 25px/1.22 Manrope,
    sans-serif;
}
.plan-description {
  min-height: 90px;
  margin: 0;
  color: var(--card-muted);
  font-size: 12px;
  line-height: 1.6;
}
.plan-price-line {
  min-height: 85px;
  margin-top: 22px;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
}
.plan-price-line strong {
  color: var(--card-text);
  font:
    800 clamp(28px, 2.6vw, 38px)/1.1 Manrope,
    sans-serif;
  letter-spacing: -1.3px;
}
.plan-price-line span {
  color: var(--card-muted);
  font-size: 11px;
}
.plan-divider {
  height: 1px;
  margin: 24px -22px 0;
  background: #ffffff30;
}
.plan-benefits {
  flex: 1 0 auto;
  min-height: 170px;
  margin: 0 -22px;
  padding: 21px 22px 25px;
  display: grid;
  align-content: start;
  gap: 13px;
  background: var(--card-benefits);
  list-style: none;
}
.plan-benefits li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  color: var(--card-text);
  font-size: 12px;
  line-height: 1.5;
}
.plan-benefits li svg {
  flex: 0 0 auto;
  color: var(--card-accent);
}
.plan-action {
  min-height: 52px;
  margin-top: 16px;
  padding: 0 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border-radius: 7px;
  color: #202238;
  background: #c5f477;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
  text-decoration: none;
  transition:
    transform 0.18s ease,
    background 0.18s ease;
}
.plan-action:hover {
  transform: translateY(-2px);
  color: #202238;
  background: #d3ff8c;
}
.plan-action-secondary {
  border: 1px solid #c5f477;
  color: #202238;
  background: #c5f477;
}
.plan-action-secondary:hover {
  color: #202238;
  background: #d3ff8c;
}
.plan-institution-note {
  min-height: 52px;
  margin: 16px 0 0;
  display: flex;
  align-items: center;
  color: var(--card-muted);
  font-size: 11px;
  line-height: 1.6;
}
.plans-simulation-panel {
  margin-top: 24px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 13px;
  border: 1px solid #dfead1;
  border-radius: 13px;
  background: #f6faef;
}
.plans-simulation-icon {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 11px;
  color: #55803b;
  background: #e8f3d9;
}
.plans-simulation-panel strong {
  color: #37472d;
  font-size: 11px;
}
.plans-simulation-panel p {
  margin: 4px 0 0;
  color: #68725f;
  font-size: 10px;
  line-height: 1.7;
}
.plans-test-card {
  margin-left: auto;
  flex: 0 1 auto;
  min-width: 0;
  overflow-wrap: anywhere;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64715a;
  font-size: 9px;
  font-weight: 700;
}
.plans-footer {
  min-height: 72px;
  padding: 14px max(22px, calc((100vw - 1120px) / 2));
  display: flex;
  align-items: center;
  border-top: 1px solid #e9e9f0;
}
.plans-footer > a {
  color: #15172e;
  font-size: 19px;
}
.plans-footer > a span {
  color: #6757df;
}
@media (max-width: 1150px) {
  .plans-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .plan-description {
    min-height: 65px;
  }
}
@media (max-width: 720px) {
  .plans-content {
    width: min(100% - 32px, 1040px);
    padding-top: 34px;
  }
  .plans-section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .plans-simulation-panel {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .plans-test-card {
    width: 100%;
    margin-left: 0;
  }
}
@media (max-width: 680px) {
  .plans-grid {
    grid-template-columns: 1fr;
  }
  .plan-card h3,
  .plan-description {
    min-height: 0;
  }
  .plan-price-line {
    min-height: 0;
  }
}
@media (max-width: 480px) {
  .plans-header-inner {
    width: calc(100% - 32px);
    min-height: 66px;
  }
  .plans-header nav {
    gap: 12px;
  }
  .plans-header nav a:first-child {
    display: none;
  }
  .plans-header nav a {
    font-size: 12px;
  }
  .plans-hero {
    padding: 42px 18px 35px;
  }
  .plan-card {
    padding: 21px;
  }
  .plans-footer {
    padding: 18px;
  }
}
</style>
