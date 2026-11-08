<script setup lang="ts">
import { ArrowUpRight, Check, CheckCircle2, Clock3 } from "lucide-vue-next";
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";
import CabecalhoPagina from "../componentes/CabecalhoPagina.vue";
import Etiqueta from "../componentes/Etiqueta.vue";
const roteador = useRouter();
const rota = useRoute();
const estado = useEstadoAplicacao();
const treino = computed(() => {
  const indice = Number(rota.query.indice);
  return Number.isInteger(indice) && indice >= 0
    ? estado.historico[indice]
    : (estado.feedbackAtual ?? estado.historico[0]);
});
const avaliacoesPorPergunta = computed(
  () => treino.value?.avaliacoesPorPergunta ?? [],
);
const temResumo = computed(() => Boolean(treino.value?.pontosFortes && treino.value?.melhorias));
const nomesCriterios = {
  CORRECAO_TECNICA: "Correção técnica",
  COMPLETUDE: "Completude",
  CLAREZA: "Clareza",
};
const descricoesCriterios = {
  CORRECAO_TECNICA: "Adequação técnica ao contexto fornecido",
  COMPLETUDE: "Cobertura do que a pergunta solicita",
  CLAREZA: "Organização e compreensão do raciocínio",
};
function confiancaEmPortugues(nivel?: "BAIXA" | "MEDIA" | "ALTA") {
  return { BAIXA: "Baixa", MEDIA: "Média", ALTA: "Alta" }[nivel ?? "BAIXA"];
}
</script>

<template>
  <CabecalhoPagina
    titulo="Feedback da entrevista"
    :descricao="`${treino?.titulo ?? 'Treinamento'} · ${treino?.contexto ?? ''}`"
  />
  <div class="row g-3">
    <div class="col-md-4">
      <section class="card score-card h-100">
        <div class="card-body text-center">
          <div class="eyebrow">
            {{
              !treino
                ? "SEM RESULTADO"
                : treino.avaliacaoQualitativa
                  ? "ÍNDICE DE REFERÊNCIA"
                  : "RESULTADO DEMONSTRATIVO"
            }}
          </div>
          <div
            class="score-ring"
            :style="{ '--score': `${treino?.nota ?? 0}%` }"
          >
            <div>
              <strong>{{ treino?.nota ?? "—" }}</strong
              ><small>pontos</small>
            </div>
          </div>
          <Etiqueta
            :tom="
              !treino
                ? 'neutral'
                : treino.avaliacaoQualitativa
                  ? 'green'
                  : 'orange'
            "
          >
            {{
              !treino
                ? "Nenhum treino selecionado"
                : treino.avaliacaoQualitativa
                  ? "Rubrica aplicada"
                    : temResumo
                      ? "Feedback resumido"
                      : "Sem avaliação qualitativa"
            }}
          </Etiqueta>
          <p>
            {{
              !treino
                ? "Conclua um treino para ver o resultado."
                : treino.avaliacaoQualitativa
                  ? "Índice calculado pela média dos critérios, não é garantia de acerto."
                  : "A nota demonstrativa não mede a qualidade nem a correção das respostas."
            }}
          </p>
        </div>
      </section>
    </div>
    <div class="col-md-8">
      <section class="card h-100">
        <div class="card-body feedback-body">
          <div class="eyebrow">ANÁLISE DA ENTREVISTA</div>
          <h2>Resumo da sua entrevista</h2>
          <div v-if="temResumo" class="feedback-point">
            <span class="point-icon"><Check :size="15" /></span>
            <div>
              <strong>Seus pontos fortes</strong>
              <p>
                {{
                  treino?.pontosFortes ??
                  "Conclua um treino para registrar o feedback."
                }}
              </p>
            </div>
          </div>
          <div
            v-if="temResumo"
            class="feedback-point improvement"
          >
            <span class="point-icon"><ArrowUpRight :size="15" /></span>
            <div>
              <strong>Para continuar evoluindo</strong>
              <p>
                {{
                  treino?.melhorias ??
                  "Continue praticando e usando exemplos concretos nas respostas."
                }}
              </p>
            </div>
          </div>
          <p v-else class="feedback-disclaimer">
            A avaliação qualitativa não está disponível neste modo. Ative um
            provedor de IA para analisar os critérios e gerar feedback.
          </p>
          <div class="feedback-meta">
            <span><Clock3 :size="14" /> {{ treino?.duracao ?? "—" }}</span
            ><span
              ><CheckCircle2 :size="14" />
              {{ treino?.contexto ?? "Sem contexto" }}</span
            >
          </div>
        </div>
      </section>
    </div>
  </div>
  <section v-if="treino" class="card mt-3">
    <div class="card-body">
      <div class="eyebrow">CONFIANÇA E LIMITAÇÕES</div>
      <p class="feedback-disclaimer">
        A confiança é uma estimativa qualitativa não calibrada. A IA pode errar;
        confirme conteúdos técnicos em fontes confiáveis.
      </p>
      <div class="feedback-confidence">
        <strong>Confiança indicada: {{ confiancaEmPortugues(treino.nivelConfianca) }}</strong>
        <p>{{ treino.limitacoes ?? "Não há detalhes adicionais de limitação." }}</p>
      </div>
    </div>
  </section>
  <section
    v-if="avaliacoesPorPergunta.length"
    class="card mt-3"
  >
    <div class="card-body">
      <div class="eyebrow">RUBRICA POR PERGUNTA</div>
      <p class="feedback-disclaimer">
        Os trechos mostram o que a IA considerou; não comprovam, por si só, que
        a análise técnica está correta.
      </p>
      <article
        v-for="avaliacao in avaliacoesPorPergunta"
        :key="avaliacao.numero"
        class="question-review"
      >
        <h3>Pergunta {{ avaliacao.numero }}</h3>
        <p class="question-review-prompt">{{ avaliacao.pergunta }}</p>
        <template v-if="avaliacao.criterios.length">
          <div
            v-for="criterio in avaliacao.criterios"
            :key="criterio.criterio"
            class="criterion-review"
          >
            <div class="criterion-review-heading">
              <strong>{{ nomesCriterios[criterio.criterio] }}</strong>
              <span>{{ criterio.nota }}/5</span>
            </div>
            <small>{{ descricoesCriterios[criterio.criterio] }}</small>
            <p>{{ criterio.justificativa }}</p>
            <blockquote v-if="criterio.trechoResposta">
              “{{ criterio.trechoResposta }}”
            </blockquote>
            <small v-else>Sem trecho citado da resposta.</small>
          </div>
        </template>
        <p v-else class="feedback-disclaimer">
          A rubrica não foi aplicada neste treino demonstrativo.
        </p>
        <div class="feedback-confidence">
          <strong>Confiança indicada: {{ confiancaEmPortugues(avaliacao.nivelConfianca) }}</strong>
          <p>{{ avaliacao.limitacao }}</p>
        </div>
      </article>
    </div>
  </section>
  <div class="d-flex justify-content-start gap-2 mt-4">
    <button
      class="btn btn-outline-secondary"
      @click="roteador.push('/historico')"
    >
      Ver histórico</button
    ><button class="btn btn-primary" @click="roteador.push('/treinos')">
      Fazer outro treino
    </button>
  </div>
</template>
