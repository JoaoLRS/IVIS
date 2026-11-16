<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ArrowLeft, LockKeyhole, LogOut, ShieldCheck } from "lucide-vue-next";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";
import { obterTokenCsrf } from "../servicos/perfilAluno";
import {
  simularContratacao,
  type ContratacaoSimulada,
} from "../servicos/contratacoes";
import { clienteHttp } from "../servicos/clienteHttp";
import { novaChaveIdempotencia } from "../servicos/chaveIdempotencia";

interface Oferta {
  id: string;
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
const router = useRouter();
const sessao = useEstadoSessao();
const oferta = ref<Oferta | null>(null);
const carregando = ref(true);
const enviando = ref(false);
const erro = ref("");
const resultado = ref<ContratacaoSimulada | null>(null);
const numeroCartao = ref("4242 4242 4242 4242");
const validade = ref("12/30");
const cvv = ref("123");
const chaveIdempotencia = ref(novaChaveIdempotencia());

async function sairDaConta() {
  await sessao.sair();
  await router.replace("/");
}

const valorFormatado = computed(() => {
  if (!oferta.value) return "";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: oferta.value.moeda,
  }).format(oferta.value.precoCentavos / 100);
});

const descricaoExibida = computed(() =>
  (oferta.value?.descricao ?? "")
    .replace(/\s*[.;]\s*nenhuma cobrança real é realizada\./i, ".")
    .trim(),
);

function erroDaResposta(erroApi: unknown) {
  if (
    typeof erroApi === "object" &&
    erroApi !== null &&
    "response" in erroApi
  ) {
    const resposta = (
      erroApi as {
        response?: { data?: { mensagem?: string; idRequisicao?: string } };
      }
    ).response;
    const mensagem =
      resposta?.data?.mensagem ?? "Não foi possível processar a simulação.";
    return resposta?.data?.idRequisicao
      ? `${mensagem} (referência: ${resposta.data.idRequisicao})`
      : mensagem;
  }
  return "Não foi possível conectar ao IVIS. Tente novamente.";
}

async function carregarOferta() {
  carregando.value = true;
  erro.value = "";
  try {
    const resposta = await clienteHttp.get<Oferta>(
      `/v1/planos/${rota.params.planoId}`,
    );
    const modalidadeEsperada =
      sessao.conta?.tipoConta === "INSTITUICAO"
        ? "INSTITUCIONAL"
        : "INDIVIDUAL";
    if (resposta.data.modalidade !== modalidadeEsperada) {
      erro.value = "Esta oferta não corresponde ao tipo da sua conta.";
      return;
    }
    oferta.value = resposta.data;
  } catch {
    erro.value =
      "Não foi possível encontrar este plano. Volte à lista de planos.";
  } finally {
    carregando.value = false;
  }
}

function selecionarCenario(cenario: "aprovado" | "recusado") {
  numeroCartao.value =
    cenario === "aprovado" ? "4242 4242 4242 4242" : "4000 0000 0000 0002";
  validade.value = "12/30";
  cvv.value = "123";
  resultado.value = null;
  erro.value = "";
}

async function confirmarCompra() {
  if (!oferta.value) return;
  enviando.value = true;
  erro.value = "";
  try {
    const csrf = await obterTokenCsrf();
    resultado.value = await simularContratacao(
      {
        planoId: oferta.value.id,
        numeroCartao: numeroCartao.value,
        validade: validade.value,
        cvv: cvv.value,
      },
      chaveIdempotencia.value,
      csrf,
    );
    numeroCartao.value = "";
    cvv.value = "";
    if (resultado.value.situacaoPagamento === "APROVADO") {
      await sessao.consultarPlano();
    } else {
      chaveIdempotencia.value = novaChaveIdempotencia();
    }
  } catch (erroApi) {
    erro.value = erroDaResposta(erroApi);
  } finally {
    enviando.value = false;
  }
}

onMounted(carregarOferta);
</script>

<template>
  <main class="checkout-page">
    <header class="checkout-header">
      <RouterLink class="checkout-brand" to="/">IVIS<span>.</span></RouterLink>
      <span class="checkout-step"
        ><LockKeyhole :size="15" /> Simulação de pagamento</span
      >
      <button class="checkout-logout" type="button" @click="sairDaConta">
        <LogOut :size="15" /> Sair
      </button>
    </header>

    <section class="checkout-content">
      <RouterLink class="checkout-back" to="/planos"
        ><ArrowLeft :size="15" /> Voltar aos planos</RouterLink
      >

      <div v-if="carregando" class="checkout-state">Carregando oferta...</div>
      <div
        v-else-if="erro && !oferta"
        class="checkout-state checkout-error"
        role="alert"
      >
        {{ erro }}
      </div>
      <template v-else-if="oferta">
        <div class="checkout-heading">
          <span class="checkout-kicker">SEU PLANO</span>
          <h1>Revise e confirme seu acesso</h1>
          <p>Confira o resumo e escolha um cartão de teste para continuar.</p>
        </div>

        <div class="checkout-grid">
          <section class="checkout-form-card">
            <div class="checkout-panel-heading">
              <span aria-hidden="true">01</span>
              <div>
                <h2>Pagamento de teste</h2>
                <p>Selecione um resultado para a simulação.</p>
              </div>
            </div>
            <div class="checkout-simulation-notice">
              <ShieldCheck :size="20" />
              <div>
                <strong>Nenhum valor será cobrado</strong>
                <p>Use somente os cartões fictícios desta página.</p>
              </div>
            </div>

            <div v-if="!resultado" class="checkout-card-form">
              <div class="checkout-scenarios" aria-label="Cenário de teste">
                <button
                  type="button"
                  class="checkout-scenario"
                  :class="{ 'checkout-scenario-active': numeroCartao === '4242 4242 4242 4242' }"
                  :aria-pressed="numeroCartao === '4242 4242 4242 4242'"
                  @click="selecionarCenario('aprovado')"
                >
                  <strong>Aprovação</strong>
                  <small>4242 4242 4242 4242</small>
                </button>
                <button
                  type="button"
                  class="checkout-scenario"
                  :class="{ 'checkout-scenario-active': numeroCartao === '4000 0000 0000 0002' }"
                  :aria-pressed="numeroCartao === '4000 0000 0000 0002'"
                  @click="selecionarCenario('recusado')"
                >
                  <strong>Recusa</strong>
                  <small>4000 0000 0000 0002</small>
                </button>
              </div>

              <form autocomplete="off" @submit.prevent="confirmarCompra">
                <label class="form-label" for="checkout-cartao"
                  >Número do cartão fictício</label
                >
                <input
                  id="checkout-cartao"
                  v-model="numeroCartao"
                  class="form-control"
                  inputmode="numeric"
                  autocomplete="off"
                  maxlength="23"
                  required
                />

                <div class="checkout-card-fields">
                  <div>
                    <label class="form-label" for="checkout-validade"
                      >Validade (MM/AA)</label
                    >
                    <input
                      id="checkout-validade"
                      v-model="validade"
                      class="form-control"
                      autocomplete="off"
                      placeholder="12/30"
                      required
                    />
                  </div>
                  <div>
                    <label class="form-label" for="checkout-cvv"
                      >CVV de teste</label
                    >
                    <input
                      id="checkout-cvv"
                      v-model="cvv"
                      class="form-control"
                      inputmode="numeric"
                      autocomplete="off"
                      maxlength="4"
                      required
                    />
                  </div>
                </div>

                <p v-if="erro" class="checkout-error-message" role="alert">
                  {{ erro }}
                </p>
                <button
                  class="checkout-submit"
                  type="submit"
                  :disabled="enviando"
                >
                  {{
                    enviando
                      ? "Processando simulação..."
                      : `Simular pagamento de ${valorFormatado}`
                  }}
                </button>
              </form>
            </div>

            <div
              v-else
              class="checkout-result"
              :class="
                resultado.situacaoPagamento === 'APROVADO'
                  ? 'checkout-result-success'
                  : 'checkout-result-declined'
              "
              role="status"
            >
              <span class="checkout-result-mark">{{
                resultado.situacaoPagamento === "APROVADO" ? "✓" : "!"
              }}</span>
              <h2>
                {{
                  resultado.situacaoPagamento === "APROVADO"
                    ? "Simulação aprovada"
                    : "Simulação recusada"
                }}
              </h2>
              <p v-if="resultado.situacaoPagamento === 'APROVADO'">
                {{
                  oferta.modalidade === "INSTITUCIONAL"
                    ? `O plano institucional está ativo ${oferta.vagasIncluidas === null ? "sem limite de alunos" : `para até ${oferta.vagasIncluidas} alunos`} até`
                    : "Seu plano está ativo até"
                }}
                {{
                  new Date(resultado.expiraEm ?? "").toLocaleDateString(
                    "pt-BR",
                  )
                }}. Nenhum valor foi cobrado.
              </p>
              <p v-else>
                O cartão de teste foi recusado. Nenhum valor foi cobrado e o
                plano não foi ativado.
              </p>
              <p v-if="resultado.situacaoPagamento === 'APROVADO'" class="checkout-email-fallback">
                A confirmação foi encaminhada ao serviço de e-mail de demonstração.
                No ambiente local, consulte o Mailpit em localhost:8025.
              </p>
              <RouterLink
                v-if="resultado.situacaoPagamento === 'APROVADO' && !enviando"
                class="checkout-submit"
                :to="oferta.modalidade === 'INSTITUCIONAL' ? '/instituicao' : '/inicio'"
              >
                Ir para minha área
              </RouterLink>
              <button
                v-else-if="resultado.situacaoPagamento === 'RECUSADO'"
                class="checkout-retry"
                type="button"
                @click="resultado = null"
              >
                Tentar novamente
              </button>
              <p v-else role="status">Confirmando seu acesso...</p>
            </div>
          </section>

          <aside class="checkout-summary">
            <span class="checkout-kicker">RESUMO DO PLANO</span>
            <h2>{{ oferta.nome }}</h2>
            <p>{{ descricaoExibida }}</p>
            <ul>
              <li v-for="beneficio in oferta.beneficios" :key="beneficio">
                {{ beneficio }}
              </li>
            </ul>
            <div class="checkout-summary-duration">
              <span>Período de acesso</span
              ><strong>{{ oferta.duracaoDias }} dias</strong>
            </div>
            <div
              v-if="oferta.modalidade === 'INSTITUCIONAL'"
              class="checkout-summary-duration"
            >
              <span>Capacidade incluída</span
              ><strong>{{ oferta.vagasIncluidas === null ? "Alunos ilimitados" : `${oferta.vagasIncluidas} alunos` }}</strong>
            </div>
            <div class="checkout-total">
              <span>Total simulado</span><strong>{{ valorFormatado }}</strong>
            </div>
          </aside>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.checkout-page {
  min-height: 100vh;
  color: #171b39;
  background: #f2f5fc;
}
.checkout-header {
  min-height: 80px;
  padding: 0 max(24px, calc((100vw - 1160px) / 2));
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #141830;
}
.checkout-brand {
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -1px;
  text-decoration: none;
}
.checkout-brand span {
  color: #c4f474;
}
.checkout-step {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #c8cce3;
  font-size: 12px;
}
.checkout-logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.checkout-content {
  width: min(100% - 48px, 1160px);
  margin: 0 auto;
  padding: 34px 0 80px;
}
.checkout-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 34px;
  color: #505875;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}
.checkout-back:hover {
  color: #5142c5;
}
.checkout-heading {
  margin-bottom: 28px;
}
.checkout-kicker {
  color: #5d50d8;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.4px;
}
.checkout-heading h1 {
  margin: 9px 0 8px;
  color: #151a36;
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 800;
  letter-spacing: -1.2px;
}
.checkout-heading p {
  margin: 0;
  color: #626b84;
  font-size: 15px;
}
.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.85fr);
  gap: 24px;
  align-items: start;
}
.checkout-form-card,
.checkout-summary {
  padding: 30px;
  border: 1px solid #e0e5f0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 32px #1b25400a;
}
.checkout-panel-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}
.checkout-panel-heading > span {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: #5951d4;
  font-size: 13px;
  font-weight: 800;
}
.checkout-panel-heading h2 {
  margin: 0 0 3px;
  font-size: 19px;
}
.checkout-panel-heading p {
  margin: 0;
  color: #68718a;
  font-size: 13px;
}
.checkout-simulation-notice {
  display: flex;
  gap: 12px;
  padding: 15px;
  border: 1px solid #d9ebbd;
  border-radius: 8px;
  color: #354b2b;
  background: #f5fbe9;
}
.checkout-simulation-notice > svg {
  flex: 0 0 auto;
  color: #568137;
}
.checkout-simulation-notice strong {
  font-size: 14px;
}
.checkout-simulation-notice p {
  margin: 4px 0 0;
  color: #5e6b55;
  font-size: 12px;
  line-height: 1.6;
}
.checkout-scenarios {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 24px 0;
}
.checkout-scenario {
  min-height: 72px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  border: 1px solid #d7dbea;
  border-radius: 8px;
  color: #242a48;
  background: #fff;
  text-align: left;
  cursor: pointer;
}
.checkout-scenario strong {
  font-size: 14px;
}
.checkout-scenario small {
  color: #68718b;
  font-size: 12px;
}
.checkout-scenario:hover,
.checkout-scenario-active {
  border-color: #6255e0;
  background: #f4f2ff;
}
.checkout-scenario:focus-visible,
.checkout-submit:focus-visible,
.checkout-logout:focus-visible,
.checkout-back:focus-visible {
  outline: 3px solid #7970ef;
  outline-offset: 3px;
}
.checkout-card-form form {
  display: grid;
  gap: 10px;
}
.checkout-card-form .form-label {
  margin: 8px 0 0;
  color: #303651;
  font-size: 13px;
  font-weight: 700;
}
.checkout-card-form .form-control {
  min-height: 50px;
  border-color: #b8bfd0;
  border-radius: 6px;
  font-size: 14px;
  box-shadow: none;
}
.checkout-card-form .form-control:focus {
  border-color: #6255e0;
  box-shadow: 0 0 0 3px #6255e026;
}
.checkout-card-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.checkout-submit {
  width: 100%;
  min-height: 52px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 7px;
  color: #202238;
  background: #c5f477;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
}
.checkout-submit:hover {
  color: #202238;
  background: #d3ff8c;
}
.checkout-submit:disabled {
  opacity: 0.65;
  cursor: wait;
}
.checkout-summary {
  position: sticky;
  top: 20px;
  border-color: #232a54;
  color: #fff;
  background: linear-gradient(150deg, #171b3b, #252667);
}
.checkout-summary .checkout-kicker {
  color: #c4f474;
}
.checkout-summary h2 {
  margin: 16px 0 8px;
  color: #fff;
  font-size: 23px;
  font-weight: 800;
}
.checkout-summary > p {
  color: #cbd0e8;
  font-size: 13px;
  line-height: 1.6;
}
.checkout-summary ul {
  margin: 24px 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.checkout-summary li {
  position: relative;
  padding-left: 22px;
  color: #e2e5f5;
  font-size: 13px;
  line-height: 1.5;
}
.checkout-summary li:before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #c4f474;
  font-weight: 800;
}
.checkout-summary-duration,
.checkout-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-top: 1px solid #ffffff2b;
  color: #cbd0e8;
  font-size: 12px;
}
.checkout-summary-duration strong {
  color: #fff;
  text-align: right;
}
.checkout-total {
  margin-top: 8px;
  padding-top: 20px;
  font-size: 14px;
}
.checkout-total strong {
  color: #c4f474;
  font-size: 28px;
  font-weight: 800;
  white-space: nowrap;
}
.checkout-error-message,
.checkout-error {
  color: #a32939;
}
.checkout-error-message {
  margin: 5px 0;
  font-size: 12px;
}
.checkout-state {
  padding: 30px;
  border-radius: 12px;
  background: #fff;
  text-align: center;
}
.checkout-result {
  padding: 16px 8px 5px;
  text-align: center;
}
.checkout-result-mark {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 25px;
  font-weight: 800;
}
.checkout-result-success .checkout-result-mark {
  color: #2e7554;
  background: #e8f6e8;
}
.checkout-result-declined .checkout-result-mark {
  color: #9b3c43;
  background: #fceced;
}
.checkout-result h2 {
  margin: 0 0 7px;
  font-size: 21px;
  font-weight: 800;
}
.checkout-result p {
  max-width: 550px;
  margin: 0 auto 16px;
  color: #696b7c;
  font-size: 12px;
  line-height: 1.7;
}
.checkout-email-fallback {
  padding: 10px;
  border-radius: 8px;
  background: #f5f5fa;
}
.checkout-retry {
  padding: 10px 18px;
  border: 1px solid #d9d5ff;
  border-radius: 999px;
  color: #5142c5;
  background: #fff;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
@media (max-width: 760px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
  .checkout-summary {
    position: static;
    grid-row: 1;
  }
}
@media (max-width: 460px) {
  .checkout-header {
    padding: 0 18px;
  }
  .checkout-step {
    display: none;
  }
  .checkout-content {
    width: min(100% - 28px, 1080px);
  }
  .checkout-form-card,
  .checkout-summary {
    padding: 22px;
  }
  .checkout-scenarios {
    grid-template-columns: 1fr;
  }
  .checkout-total strong {
    font-size: 23px;
  }
}
</style>
