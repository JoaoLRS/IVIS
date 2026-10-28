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

interface Oferta {
  id: string;
  modalidade: "INDIVIDUAL" | "INSTITUCIONAL";
  nome: string;
  descricao: string | null;
  precoCentavos: number;
  moeda: string;
  duracaoDias: number;
  vagasIncluidas: number;
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
const chaveIdempotencia = ref(crypto.randomUUID());

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
      chaveIdempotencia.value = crypto.randomUUID();
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
        ><LockKeyhole :size="15" /> Checkout seguro de demonstração</span
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
          <span class="checkout-kicker">ATIVAÇÃO DE ACESSO</span>
          <h1>
            {{
              oferta.modalidade === "INSTITUCIONAL"
                ? "Confirme o plano acadêmico"
                : "Confirme seu plano"
            }}
          </h1>
          <p>
            Revise a oferta e use um cartão fictício para testar a simulação.
          </p>
        </div>

        <div class="checkout-grid">
          <section class="checkout-form-card">
            <div class="checkout-simulation-notice">
              <ShieldCheck :size="20" />
              <div>
                <strong>Ambiente acadêmico: sem cobrança real</strong>
                <p>
                  O IVIS não se conecta a adquirentes. Use somente os cartões de
                  teste exibidos abaixo.
                </p>
              </div>
            </div>

            <div v-if="!resultado" class="checkout-card-form">
              <div class="checkout-scenarios" aria-label="Cenário de teste">
                <button
                  type="button"
                  class="checkout-scenario"
                  @click="selecionarCenario('aprovado')"
                >
                  Preencher cartão aprovado
                </button>
                <button
                  type="button"
                  class="checkout-scenario"
                  @click="selecionarCenario('recusado')"
                >
                  Preencher cartão recusado
                </button>
              </div>

              <div class="checkout-test-cards">
                <p>
                  <strong>Aprovação:</strong> 4242 4242 4242 4242 · 12/30 · CVV
                  123
                </p>
                <p>
                  <strong>Recusa:</strong> 4000 0000 0000 0002 · 12/30 · CVV 123
                </p>
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
                    ? `O plano acadêmico está ativo para até ${oferta.vagasIncluidas} alunos até`
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
              <details
                v-if="resultado.emailConfirmacao"
                class="checkout-email-preview"
              >
                <summary>Ver confirmação de e-mail simulada</summary>
                <div>
                  <small
                    >CAIXA DE ENTRADA SIMULADA ·
                    {{ resultado.emailConfirmacao.destinatario }}</small
                  >
                  <strong>{{ resultado.emailConfirmacao.assunto }}</strong>
                  <pre>{{ resultado.emailConfirmacao.corpo }}</pre>
                </div>
              </details>
              <p
                v-else-if="resultado.situacaoPagamento === 'APROVADO'"
                class="checkout-email-fallback"
              >
                A compra foi salva. A confirmação simulada não pôde ser exibida;
                tente novamente com a mesma sessão.
              </p>
              <RouterLink
                v-if="resultado.situacaoPagamento === 'APROVADO'"
                class="checkout-submit"
                to="/inicio"
              >
                Ir para minha área
              </RouterLink>
              <button
                v-else
                class="checkout-retry"
                type="button"
                @click="resultado = null"
              >
                Tentar novamente
              </button>
            </div>
          </section>

          <aside class="checkout-summary">
            <span class="checkout-kicker">RESUMO DO PEDIDO</span>
            <h2>{{ oferta.nome }}</h2>
            <p>{{ oferta.descricao }}</p>
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
              ><strong>{{ oferta.vagasIncluidas }} alunos</strong>
            </div>
            <div class="checkout-total">
              <span>Total simulado</span><strong>{{ valorFormatado }}</strong>
            </div>
            <p class="checkout-no-charge">
              Valor ilustrativo. A simulação não movimenta dinheiro.
            </p>
          </aside>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.checkout-page {
  min-height: 100vh;
  color: #27283a;
  background: #f7f7fb;
  font-family: Manrope, Inter, sans-serif;
}
.checkout-header {
  min-height: 76px;
  padding: 0 max(24px, calc((100vw - 1080px) / 2));
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8ef;
  background: #fff;
}
.checkout-brand {
  color: #15172e;
  font:
    800 25px Manrope,
    sans-serif;
  letter-spacing: -1px;
  text-decoration: none;
}
.checkout-brand span {
  color: #6757df;
}
.checkout-step {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #656779;
  font-size: 12px;
}
.checkout-logout {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #5142c5;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.checkout-content {
  width: min(100% - 40px, 1080px);
  margin: 0 auto;
  padding: 30px 0 64px;
}
.checkout-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 26px;
  color: #5c5e70;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}
.checkout-back:hover {
  color: #5142c5;
}
.checkout-heading {
  margin-bottom: 24px;
}
.checkout-kicker {
  color: #6757df;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.4px;
}
.checkout-heading h1 {
  margin: 7px 0 5px;
  color: #21233a;
  font:
    800 30px Manrope,
    sans-serif;
  letter-spacing: -0.8px;
}
.checkout-heading p {
  margin: 0;
  color: #747687;
  font-size: 13px;
}
.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.8fr);
  gap: 20px;
  align-items: start;
}
.checkout-form-card,
.checkout-summary {
  padding: 24px;
  border: 1px solid #e7e7ef;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 26px #1b1b3c08;
}
.checkout-simulation-notice {
  display: flex;
  gap: 12px;
  padding: 15px;
  border: 1px solid #d9ebbd;
  border-radius: 12px;
  color: #354b2b;
  background: #f5fbe9;
}
.checkout-simulation-notice > svg {
  flex: 0 0 auto;
  color: #568137;
}
.checkout-simulation-notice strong {
  font-size: 13px;
}
.checkout-simulation-notice p {
  margin: 4px 0 0;
  color: #5e6b55;
  font-size: 11px;
  line-height: 1.6;
}
.checkout-scenarios {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 20px 0 11px;
}
.checkout-scenario {
  padding: 8px 11px;
  border: 1px solid #d9d5ff;
  border-radius: 999px;
  color: #5045a4;
  background: #fff;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.checkout-scenario:hover {
  background: #f5f3ff;
}
.checkout-test-cards {
  margin-bottom: 18px;
  padding: 11px 13px;
  border-radius: 10px;
  background: #f5f5fa;
}
.checkout-test-cards p {
  margin: 3px 0;
  color: #66687a;
  font-size: 10px;
}
.checkout-card-form form {
  display: grid;
  gap: 8px;
}
.checkout-card-form .form-label {
  margin: 5px 0 0;
  color: #4e5063;
  font-size: 11px;
  font-weight: 700;
}
.checkout-card-form .form-control {
  min-height: 43px;
  border-color: #dfe0e9;
  border-radius: 9px;
  font-size: 13px;
  box-shadow: none;
}
.checkout-card-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.checkout-submit {
  width: 100%;
  min-height: 46px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  color: #202238;
  background: #c5f477;
  font-size: 12px;
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
}
.checkout-summary h2 {
  margin: 10px 0 7px;
  color: #292a3c;
  font:
    700 19px Manrope,
    sans-serif;
}
.checkout-summary > p {
  color: #737586;
  font-size: 11px;
  line-height: 1.7;
}
.checkout-summary ul {
  margin: 18px 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.checkout-summary li {
  position: relative;
  padding-left: 18px;
  color: #56586b;
  font-size: 11px;
  line-height: 1.5;
}
.checkout-summary li:before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #568137;
  font-weight: 800;
}
.checkout-summary-duration,
.checkout-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-top: 1px solid #eeeef3;
  color: #696b7d;
  font-size: 11px;
}
.checkout-summary-duration strong {
  color: #34364a;
}
.checkout-total strong {
  color: #25263b;
  font:
    800 22px Manrope,
    sans-serif;
}
.checkout-summary .checkout-no-charge {
  margin: 0;
  padding: 10px;
  border-radius: 8px;
  color: #59634d;
  background: #f5fbe9;
  font-size: 10px;
  text-align: center;
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
  font:
    800 21px Manrope,
    sans-serif;
}
.checkout-result p {
  max-width: 550px;
  margin: 0 auto 16px;
  color: #696b7c;
  font-size: 12px;
  line-height: 1.7;
}
.checkout-email-preview {
  max-width: 620px;
  margin: 20px auto;
  border: 1px solid #e3e3ed;
  border-radius: 12px;
  text-align: left;
}
.checkout-email-preview summary {
  padding: 13px 15px;
  color: #4f459f;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.checkout-email-preview > div {
  padding: 15px;
  border-top: 1px solid #eeeef3;
}
.checkout-email-preview small,
.checkout-email-preview strong {
  display: block;
  margin-bottom: 8px;
  color: #4d4f61;
}
.checkout-email-preview small {
  color: #737586;
  font-size: 9px;
  letter-spacing: 0.6px;
}
.checkout-email-preview strong {
  font-size: 13px;
}
.checkout-email-preview pre {
  margin: 0;
  white-space: pre-wrap;
  color: #555769;
  font-family: inherit;
  font-size: 11px;
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
    font-size: 10px;
  }
  .checkout-content {
    width: min(100% - 28px, 1080px);
  }
  .checkout-form-card,
  .checkout-summary {
    padding: 18px;
  }
}
</style>
