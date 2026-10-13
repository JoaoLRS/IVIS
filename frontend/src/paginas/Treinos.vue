<script setup lang="ts">
import { computed, ref } from "vue";
import {
  ArrowRight,
  Check,
  ChevronLeft,
  Clock3,
  Code2,
  MessageSquareText,
  Sparkles,
  Target,
} from "lucide-vue-next";
import { useRouter } from "vue-router";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import Etiqueta from "../componentes/Etiqueta.vue";

const estado = useEstadoAplicacao();
const roteador = useRouter();
const modo = ref<"habilidades" | "vaga">(
  estado.vagaEmTreinamento ? "vaga" : "habilidades",
);
const vagaSelecionada = ref(estado.vagas[0]?.titulo ?? "");
const perguntas = computed(() => estado.perguntasDoTreino);
const emit = defineEmits<{ notificar: [mensagem: string] }>();

function iniciar() {
  const vaga =
    modo.value === "vaga"
      ? estado.vagas.find((item) => item.titulo === vagaSelecionada.value)
      : undefined;
  estado.iniciarTreino(vaga);
}

function encerrar() {
  estado.concluirTreino();
  roteador.push("/feedback");
}

function responder(evento: SubmitEvent) {
  const formulario = evento.target as HTMLFormElement;
  const dados = new FormData(formulario);
  const resposta = String(dados.get("resposta") ?? "").trim();
  if (resposta) estado.responder(resposta);
  formulario.reset();
}
</script>

<template>
  <template v-if="!estado.treinoAtivo">
    <CabecalhoPagina
      eyebrow="RF07 · SIMULAÇÃO DE ENTREVISTA"
      titulo="Treinamento"
      descricao="Escolha o contexto para iniciar uma entrevista técnica por texto."
    >
      <RouterLink class="btn btn-outline-secondary" to="/historico"
        ><Clock3 :size="15" /> Histórico</RouterLink
      >
    </CabecalhoPagina>
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="panel-title">Como deseja se preparar?</h2>
        <p class="text-secondary">
          A entrevista demonstrativa apresenta perguntas e permite registrar
          suas respostas nesta sessão.
        </p>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="training-option card h-100 p-3"
              ><span class="d-flex align-items-center gap-2"
                ><input
                  v-model="modo"
                  class="form-check-input"
                  type="radio"
                  value="habilidades"
                /><Code2 :size="18" /><strong
                  >Usar meu perfil de habilidades</strong
                ></span
              ><small class="mt-2">{{
                estado.habilidades.map((item) => item.nome).join(", ") ||
                "Nenhuma habilidade cadastrada"
              }}</small></label
            >
          </div>
          <div class="col-md-6">
            <label class="training-option card h-100 p-3"
              ><span class="d-flex align-items-center gap-2"
                ><input
                  v-model="modo"
                  class="form-check-input"
                  type="radio"
                  value="vaga"
                  :disabled="!estado.vagas.length"
                /><Target :size="18" /><strong
                  >Usar uma vaga de interesse</strong
                ></span
              ><select
                v-model="vagaSelecionada"
                class="form-select mt-3"
                :disabled="modo !== 'vaga' || !estado.vagas.length"
              >
                <option
                  v-for="vaga in estado.vagas"
                  :key="vaga.titulo"
                  :value="vaga.titulo"
                >
                  {{ vaga.titulo }} · {{ vaga.empresa }}
                </option>
              </select></label
            >
          </div>
        </div>
        <button
          class="btn btn-primary mt-3"
          :disabled="modo === 'vaga' && !vagaSelecionada"
          @click="iniciar"
        >
          <Sparkles :size="16" /> Iniciar entrevista
        </button>
        <p class="small text-secondary mt-3 mb-0">
          Esta versão contém somente o frontend. As perguntas, respostas e
          feedback são demonstrativos; não há processamento por IA real nem
          persistência em servidor.
        </p>
      </div>
    </section>
  </template>
  <template v-else>
    <div class="interview-top">
      <button class="btn btn-link p-0" @click="encerrar">
        <ChevronLeft :size="16" /> Finalizar treino</button
      ><span class="interview-status"><i /> ENTREVISTA EM ANDAMENTO</span
      ><span class="text-secondary small"
        ><Clock3 :size="14" /> Demonstração</span
      >
    </div>
    <div class="row g-3">
      <div class="col-xl-8">
        <section class="card interview-card">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 interview-heading">
              <span class="ai-avatar"><Sparkles :size="20" /></span>
              <div class="flex-grow-1">
                <strong>IVIS <Etiqueta tom="purple">Simulação</Etiqueta></strong
                ><small class="d-block">{{ estado.contextoTreino }}</small>
              </div>
            </div>
            <div class="alert alert-warning mt-3 mb-0">
              Simulação local: as respostas não são enviadas a um serviço de
              inteligência artificial.
            </div>
            <div class="question-progress">
              <span v-if="estado.perguntaAtual < perguntas.length"
                >PERGUNTA {{ estado.perguntaAtual + 1 }} DE
                {{ perguntas.length }}</span
              ><span v-else>ENTREVISTA CONCLUÍDA</span>
              <div class="progress">
                <div
                  class="progress-bar"
                  :style="{
                    width: `${Math.min((estado.perguntaAtual / perguntas.length) * 100, 100)}%`,
                  }"
                />
              </div>
            </div>
            <div class="chat-thread">
              <div
                v-if="estado.perguntaAtual < perguntas.length"
                class="chat-message"
              >
                <span class="chat-avatar"><Sparkles :size="14" /></span>
                <div>
                  <strong>IVIS <small>pergunta</small></strong>
                  <p>{{ perguntas[estado.perguntaAtual] }}</p>
                </div>
              </div>
              <div
                v-for="(resposta, indice) in estado.respostas"
                :key="indice"
                class="chat-message user-message"
              >
                <div>
                  <strong
                    >Você <small>resposta {{ indice + 1 }}</small></strong
                  >
                  <p>{{ resposta }}</p>
                </div>
                <span class="user-avatar">A</span>
              </div>
              <div
                v-if="estado.perguntaAtual >= perguntas.length"
                class="chat-message"
              >
                <span class="chat-avatar"><Sparkles :size="14" /></span>
                <div>
                  <strong>IVIS <small>concluído</small></strong>
                  <p>
                    Você respondeu às perguntas da simulação. Finalize para
                    registrar o resultado demonstrativo e consultar o feedback.
                  </p>
                </div>
              </div>
            </div>
            <form
              v-if="estado.perguntaAtual < perguntas.length"
              @submit.prevent="responder"
            >
              <textarea
                class="form-control answer-input"
                name="resposta"
                rows="3"
                required
                placeholder="Escreva sua resposta aqui..."
              />
              <div
                class="d-flex justify-content-between align-items-center mt-2"
              >
                <small class="text-secondary"
                  >Resposta mantida apenas nesta sessão.</small
                ><button class="btn btn-primary btn-sm">
                  Enviar resposta <ArrowRight :size="14" />
                </button>
              </div>
            </form>
            <button v-else class="btn btn-primary mt-3" @click="encerrar">
              <Check :size="16" /> Finalizar e ver feedback
            </button>
          </div>
        </section>
      </div>
      <div class="col-xl-4">
        <aside class="card">
          <div class="card-body">
            <h2 class="panel-title">
              <Target :size="17" /> Contexto deste treino
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
                  v-for="habilidade in estado.habilidades"
                  :key="habilidade.nome"
                  tom="purple"
                  >{{ habilidade.nome }}</Etiqueta
                ><span v-if="!estado.habilidades.length" class="text-secondary"
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
                Responda às {{ perguntas.length }} perguntas e finalize para ver
                o feedback.
              </p>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </template>
</template>
