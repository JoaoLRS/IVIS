<script setup lang="ts">
import axios from "axios";
import { computed, ref, watch } from "vue";
import {
  ArrowRight,
  BriefcaseBusiness,
  Check,
  ChevronLeft,
  Clock3,
  Code2,
  MessageSquareText,
  Plus,
} from "lucide-vue-next";
import { useRoute, useRouter } from "vue-router";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import Etiqueta from "../componentes/Etiqueta.vue";

const estado = useEstadoAplicacao();
const roteador = useRouter();
const rota = useRoute();
const vagaDaRota = typeof rota.query.vaga === "string" ? rota.query.vaga : "";
const modo = ref<"habilidades" | "vaga">(
  estado.vagaEmTreinamento || vagaDaRota ? "vaga" : "habilidades",
);
const vagaSelecionada = ref(estado.vagas.find((vaga) => vaga.id === vagaDaRota)?.id ?? "");
const habilidadesSelecionadas = ref<string[]>([]);
const respostaEmEdicao = ref(estado.respostaPendente ?? "");
const mensagemErro = ref("");
const processando = ref(false);
const totalPerguntas = 3;
const perguntas = computed(() => estado.perguntasDoTreino);
watch(
  () => estado.vagaEmTreinamento,
  (vaga) => { if (estado.treinoAtivo) modo.value = vaga ? "vaga" : "habilidades"; },
);
const resumoContexto = computed(() => {
  if (modo.value === "vaga") {
    const vaga = estado.vagas.find(
      (item) => item.id === vagaSelecionada.value,
    );
    return vaga
      ? `${vaga.titulo} · ${vaga.empresa}`
      : "Selecione uma vaga para continuar";
  }
  return habilidadesSelecionadas.value.length
    ? `${habilidadesSelecionadas.value.length} habilidade${habilidadesSelecionadas.value.length === 1 ? "" : "s"} selecionada${habilidadesSelecionadas.value.length === 1 ? "" : "s"}`
    : "Selecione pelo menos uma habilidade para continuar";
});

function alternarHabilidade(id: string) {
  modo.value = "habilidades";
  habilidadesSelecionadas.value = habilidadesSelecionadas.value.includes(id)
    ? habilidadesSelecionadas.value.filter((habilidadeId) => habilidadeId !== id)
    : [...habilidadesSelecionadas.value, id];
}

function selecionarTodasHabilidades() {
  modo.value = "habilidades";
  habilidadesSelecionadas.value = estado.habilidades.map((habilidade) => habilidade.id);
}

function limparSelecaoHabilidades() {
  modo.value = "habilidades";
  habilidadesSelecionadas.value = [];
}

function mensagemDaFalha(erro: unknown, respostaPreservada: boolean) {
  if (!axios.isAxiosError(erro)) throw erro;
  const detalhe = respostaPreservada
    ? " Sua resposta foi mantida; tente novamente."
    : " Tente novamente em instantes.";
  if (erro.response?.status === 503 || erro.response?.status === 504) {
    return `O serviço de IA está temporariamente indisponível.${detalhe}`;
  }
  if (erro.response?.status === 409) {
    return "O estado do treino mudou. Atualize a página para continuar.";
  }
  return `Não foi possível concluir a solicitação ao serviço de IA.${detalhe}`;
}

async function iniciar() {
  const vaga =
    modo.value === "vaga"
      ? estado.vagas.find((item) => item.id === vagaSelecionada.value)
      : undefined;
  if (modo.value === "vaga" && !vaga) return;
  processando.value = true;
  mensagemErro.value = "";
  try {
    const nomesHabilidades = estado.habilidades
      .filter((habilidade) => habilidadesSelecionadas.value.includes(habilidade.id))
      .map((habilidade) => habilidade.nome);
    await estado.iniciarTreino(vaga, nomesHabilidades);
    if (rota.query.vaga) await roteador.replace({ path: "/treinos" });
  } catch (erro) {
    mensagemErro.value = mensagemDaFalha(erro, false);
  } finally {
    processando.value = false;
  }
}

async function encerrar() {
  if (estado.respostas.length !== totalPerguntas) return;
  processando.value = true;
  mensagemErro.value = "";
  try {
    await estado.concluirTreino();
    await roteador.push("/feedback");
  } catch (erro) {
    mensagemErro.value = mensagemDaFalha(erro, true);
  } finally {
    processando.value = false;
  }
}

async function responder() {
  if (!respostaEmEdicao.value.trim() && !estado.respostaPendente) return;
  processando.value = true;
  mensagemErro.value = "";
  try {
    await estado.responder(respostaEmEdicao.value);
    respostaEmEdicao.value = "";
  } catch (erro) {
    mensagemErro.value = mensagemDaFalha(erro, true);
  } finally {
    processando.value = false;
  }
}

async function cancelar() {
  processando.value = true;
  mensagemErro.value = "";
  try {
    await estado.cancelarTreino();
  } catch (erro) {
    mensagemErro.value = mensagemDaFalha(erro, false);
  } finally {
    processando.value = false;
  }
}
</script>

<template>
  <template v-if="!estado.treinoAtivo">
    <CabecalhoPagina
      eyebrow="NOVO TREINO"
      titulo="Prepare seu treino"
      descricao="Escolha o contexto que será usado para personalizar suas perguntas."
    >
      <RouterLink class="btn btn-outline-secondary" to="/historico"
        ><Clock3 :size="15" /> Histórico</RouterLink
      >
    </CabecalhoPagina>
    <section class="training-setup">
      <div class="training-context-grid">
        <article
          class="context-card"
          :class="{ 'context-card-selected': modo === 'habilidades' }"
        >
          <label class="context-card-heading">
            <input
              v-model="modo"
              class="form-check-input"
              type="radio"
              value="habilidades"
            />
            <span class="context-card-icon"><Code2 :size="18" /></span>
            <span class="context-card-title">
              <strong>Treinar com habilidades</strong>
              <small>
                Ideal para praticar temas específicos. Selecione as habilidades
                do seu perfil e as perguntas serão criadas com base nelas.
              </small>
            </span>
            <span class="context-count">{{ estado.habilidades.length }}</span>
          </label>

          <div v-if="estado.habilidades.length" class="skill-picker">
            <div class="skill-picker-heading">
              <span>Habilidades do perfil</span>
              <div>
                <button type="button" @click="selecionarTodasHabilidades">
                  Selecionar todas
                </button>
                <button type="button" @click="limparSelecaoHabilidades">
                  Limpar
                </button>
              </div>
            </div>
            <div class="skill-chip-list">
              <button
                v-for="habilidade in estado.habilidades"
                :key="habilidade.id"
                type="button"
                class="skill-chip"
                :class="{
                  'skill-chip-selected': habilidadesSelecionadas.includes(habilidade.id),
                }"
                :aria-pressed="
                  habilidadesSelecionadas.includes(habilidade.id)
                "
                @click="alternarHabilidade(habilidade.id)"
              >
                {{ habilidade.nome }}
                <small>{{ habilidade.nivel }}/5</small>
              </button>
            </div>
            <RouterLink class="context-manage-link" to="/habilidades">
              <Plus :size="14" /> Gerenciar habilidades
            </RouterLink>
          </div>
          <div v-else class="context-empty">
            <p>Seu perfil ainda não tem habilidades cadastradas.</p>
            <RouterLink to="/habilidades">Adicionar habilidades</RouterLink>
          </div>
        </article>

        <article
          class="context-card"
          :class="{ 'context-card-selected': modo === 'vaga' }"
        >
          <label class="context-card-heading">
            <input
              v-model="modo"
              class="form-check-input"
              type="radio"
              value="vaga"
              :disabled="!estado.vagas.length"
            />
            <span class="context-card-icon"
              ><BriefcaseBusiness :size="18"
            /></span>
            <span class="context-card-title">
              <strong>Treinar para uma vaga</strong>
              <small>
                Ideal para se preparar para uma oportunidade. As perguntas vão
                considerar o cargo e os requisitos da vaga escolhida.
              </small>
            </span>
            <span class="context-count">{{ estado.vagas.length }}</span>
          </label>

          <div v-if="estado.vagas.length" class="vacancy-picker">
            <label
              v-for="vaga in estado.vagas"
              :key="vaga.id"
              class="vacancy-option"
              :class="{
                'vacancy-option-selected': vagaSelecionada === vaga.id,
              }"
            >
              <input
                v-model="vagaSelecionada"
                class="form-check-input"
                type="radio"
                :value="vaga.id"
                @change="modo = 'vaga'"
              />
              <span class="vacancy-option-copy">
                <strong>{{ vaga.titulo }}</strong>
                <small>{{ vaga.empresa }} · {{ vaga.senioridade }}</small>
                <span class="vacancy-tags">
                  <span
                    v-for="habilidade in vaga.requisitos.slice(0, 3)"
                    :key="habilidade"
                  >
                    {{ habilidade }}
                  </span>
                </span>
              </span>
            </label>
          </div>
          <div v-else class="context-empty">
            <p>Você ainda não cadastrou vagas de interesse.</p>
            <RouterLink to="/vagas">Cadastrar uma vaga</RouterLink>
          </div>
        </article>
      </div>

      <div class="training-setup-footer">
        <div class="training-context-summary">
          <span
            ><small>CONTEXTO DO TREINO</small
            ><strong>{{ resumoContexto }}</strong></span
          >
        </div>
        <div class="training-start-actions">
          <button
            class="btn btn-primary"
            :disabled="
              modo === 'habilidades'
                ? habilidadesSelecionadas.length === 0 || processando
                : !estado.vagas.length || !vagaSelecionada || processando
            "
            @click="iniciar"
          >
            {{
              processando ? "Conectando ao serviço..." : "Iniciar entrevista"
            }}
            <ArrowRight v-if="!processando" :size="15" />
          </button>
        </div>
      </div>
      <div
        v-if="mensagemErro"
        class="alert alert-danger mt-3 mb-0"
        role="alert"
      >
        {{ mensagemErro }}
      </div>
    </section>
  </template>
  <template v-else>
    <div class="interview-top">
      <button
        class="btn btn-link p-0"
        :disabled="estado.respostas.length !== totalPerguntas || processando"
        @click="encerrar"
      >
        <ChevronLeft :size="16" /> Finalizar treino</button
      ><span class="interview-status"><i /> ENTREVISTA EM ANDAMENTO</span
      ><button class="btn btn-link text-danger p-0" :disabled="processando" @click="cancelar">Cancelar treino</button
      ><span class="text-secondary small"
        ><Clock3 :size="14" /> Entrevista técnica</span
      >
    </div>
    <div class="row g-3">
      <div class="col-xl-8">
        <section class="card interview-card">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 interview-heading">
              <div class="flex-grow-1">
                <strong>IVIS <Etiqueta tom="purple">Simulação</Etiqueta></strong
                ><small class="d-block">{{ estado.contextoTreino }}</small>
              </div>
            </div>
            <div class="alert alert-warning mt-3 mb-0">
              As perguntas e a avaliação são solicitadas pelo backend. Suas
              respostas são salvas no servidor e podem ser retomadas após
              recarregar a página.
            </div>
            <div
              v-if="mensagemErro"
              class="alert alert-danger mt-3 mb-0"
              role="alert"
            >
              {{ mensagemErro }}
            </div>
            <div class="question-progress">
              <span v-if="estado.perguntaAtual < perguntas.length"
                >PERGUNTA {{ estado.perguntaAtual + 1 }} DE
                {{ totalPerguntas }}</span
              ><span v-else>ENTREVISTA CONCLUÍDA</span>
              <div class="progress">
                <div
                  class="progress-bar"
                  :style="{
                    width: `${Math.min((estado.perguntaAtual / 3) * 100, 100)}%`,
                  }"
                />
              </div>
            </div>
            <div class="chat-panel">
              <div class="chat-thread">
                <div
                  v-for="(resposta, indice) in estado.respostas"
                  :key="indice"
                >
                  <div v-if="perguntas[indice]" class="chat-message">
                    <div>
                      <strong>IVIS <small>pergunta</small></strong>
                      <p>{{ perguntas[indice] }}</p>
                    </div>
                  </div>
                  <div class="chat-message user-message">
                    <div>
                      <strong
                        >Você <small>resposta {{ indice + 1 }}</small></strong
                      >
                      <p>{{ resposta }}</p>
                    </div>
                  </div>
                </div>
                <div
                  v-if="estado.perguntaAtual < perguntas.length"
                  class="chat-message"
                >
                  <div>
                    <strong>IVIS <small>pergunta</small></strong>
                    <p>{{ perguntas[estado.perguntaAtual] }}</p>
                  </div>
                </div>
                <div
                  v-if="estado.perguntaAtual >= perguntas.length"
                  class="chat-message"
                >
                  <div>
                    <strong>IVIS <small>concluído</small></strong>
                    <p>
                      Você respondeu às três perguntas. Finalize para registrar
                      a avaliação e consultar o feedback.
                    </p>
                  </div>
                </div>
              </div>
              <form
                v-if="estado.perguntaAtual < perguntas.length"
                class="chat-composer"
                @submit.prevent="responder"
              >
                <textarea
                  v-model="respostaEmEdicao"
                  class="form-control answer-input"
                  name="resposta"
                  rows="2"
                  required
                  maxlength="4000"
                  :disabled="processando"
                  placeholder="Escreva sua resposta aqui..."
                />
                <div
                  class="d-flex justify-content-between align-items-center mt-2"
                >
                  <small class="text-secondary">{{
                    estado.respostaPendente
                      ? "Sua resposta está preservada para nova tentativa."
                      : "Limite de 4.000 caracteres."
                  }}</small
                  ><button class="btn btn-primary btn-sm">
                    {{
                      processando
                        ? "Aguardando serviço..."
                        : estado.respostaPendente
                          ? "Tentar novamente"
                          : "Enviar resposta"
                    }}
                    <ArrowRight v-if="!processando" :size="14" />
                  </button>
                </div>
              </form>
              <div v-else class="chat-composer chat-composer-finished">
                <button class="btn btn-primary" :disabled="processando" @click="encerrar">
                  <Check :size="16" /> Finalizar e ver feedback
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>
      <div class="col-xl-4">
        <aside class="card">
          <div class="card-body">
            <h2 class="panel-title">
              Contexto deste treino
            </h2>
            <hr />
            <div class="interview-detail">
              <small>OBJETIVO</small
              ><strong>{{ estado.contextoTreino }}</strong>
            </div>
            <div class="interview-detail" v-if="modo === 'habilidades'">
              <small>HABILIDADES DO PERFIL</small>
              <div class="d-flex flex-wrap gap-2">
                <Etiqueta
                  v-for="habilidade in estado.habilidadesDoTreino"
                  :key="habilidade"
                  tom="purple"
                  >{{ habilidade }}</Etiqueta
                ><span
                  v-if="!estado.habilidadesDoTreino.length"
                  class="text-secondary"
                  >Nenhuma habilidade cadastrada</span
                >
              </div>
            </div>
            <div class="interview-detail">
              <small>FORMATO</small
              ><strong>Entrevista técnica por texto</strong>
            </div>
            <div class="tip-card">
              <MessageSquareText :size="16" /><strong
                >Condição de encerramento</strong
              >
              <p>
                Responda às {{ totalPerguntas }} perguntas e finalize para ver
                o feedback.
              </p>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </template>
</template>
