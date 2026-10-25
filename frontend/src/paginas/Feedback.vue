<script setup lang="ts">
import {
  ArrowUpRight,
  Check,
  CheckCircle2,
  Clock3,
} from "lucide-vue-next";
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
          <div class="eyebrow">SUA PONTUAÇÃO</div>
          <div
            class="score-ring"
            :style="{ '--score': `${treino?.nota ?? 0}%` }"
          >
            <div>
              <strong>{{ treino?.nota ?? 0 }}</strong
              ><small>pontos</small>
            </div>
          </div>
          <Etiqueta tom="green">Acima da média</Etiqueta>
          <p>Resultado demonstrativo gerado ao concluir a simulação.</p>
        </div>
      </section>
    </div>
    <div class="col-md-8">
      <section class="card h-100">
        <div class="card-body feedback-body">
          <div class="eyebrow">ANÁLISE DA ENTREVISTA</div>
          <h2>Uma conversa forte, com espaço para brilhar ainda mais.</h2>
          <div class="feedback-point">
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
          <div class="feedback-point improvement">
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
