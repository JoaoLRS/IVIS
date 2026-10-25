<script setup lang="ts">
import { computed } from "vue";
import {
  ArrowRight,
  ArrowUpRight,
  BriefcaseBusiness,
  Check,
  Code2,
  History,
  MessageSquareText,
  TrendingUp,
} from "lucide-vue-next";
import { useEstadoAplicacao } from "../estados/estadoAplicacao";

const estado = useEstadoAplicacao();
const totalTreinos = computed(() => estado.historico.length);
const mediaNotas = computed(() =>
  totalTreinos.value
    ? Math.round(
        estado.historico.reduce((soma, treino) => soma + treino.nota, 0) /
          totalTreinos.value,
      )
    : 0,
);
const variacaoRecente = computed(() => {
  if (totalTreinos.value < 2) return null;
  return estado.historico[0].nota - estado.historico[1].nota;
});
const ultimoTreino = computed(() => estado.historico[0] ?? null);
</script>

<template>
  <section class="home-hero">
    <div class="home-hero-copy">
      <span class="home-hero-kicker">SUA PRÓXIMA CONQUISTA COMEÇA AQUI</span>
      <h1>Olá, {{ estado.nomeAluno }}!</h1>
      <p>
        Pratique entrevistas no seu ritmo, use suas habilidades ou uma vaga como
        contexto e acompanhe seus resultados.
      </p>
      <RouterLink class="home-hero-cta" to="/treinos">
        Começar uma simulação <ArrowUpRight :size="17" />
      </RouterLink>
    </div>
    <div class="home-hero-art" aria-hidden="true">
      <div class="hero-orbit hero-orbit-one"></div>
      <div class="hero-orbit hero-orbit-two"></div>
      <div class="hero-note">
        <MessageSquareText :size="25" />
        <span>Pratique<br />uma resposta por vez</span>
      </div>
    </div>
  </section>

  <section class="home-progress" aria-label="Resumo do seu progresso">
    <article class="progress-card">
      <span class="progress-icon"><MessageSquareText :size="18" /></span>
      <span class="progress-label">Treinos concluídos</span>
      <strong>{{ totalTreinos }}</strong>
      <small>registrados no histórico</small>
    </article>
    <article class="progress-card">
      <span class="progress-icon"><TrendingUp :size="18" /></span>
      <span class="progress-label">Média das notas</span>
      <strong>{{ totalTreinos ? `${mediaNotas}/100` : "—" }}</strong>
      <small>calculada com seus resultados</small>
    </article>
    <article class="progress-card">
      <span class="progress-icon"><ArrowUpRight :size="18" /></span>
      <span class="progress-label">Última variação</span>
      <strong v-if="variacaoRecente !== null">
        {{ variacaoRecente > 0 ? "+" : "" }}{{ variacaoRecente }} pontos
      </strong>
      <strong v-else>—</strong>
      <small>comparação com o treino anterior</small>
    </article>
  </section>

  <section class="home-section">
    <div class="home-section-heading">
      <div>
        <span class="home-section-kicker">CONTINUE DE ONDE PAROU</span>
        <h2>Atalhos para sua jornada</h2>
      </div>
    </div>
    <div class="home-actions">
      <RouterLink class="home-action-card" to="/habilidades">
        <span class="home-action-icon"><Code2 :size="19" /></span>
        <span class="home-action-copy">
          <strong>Adicionar habilidade</strong>
          <small>{{ estado.habilidades.length }} no seu perfil</small>
        </span>
        <ArrowRight :size="17" />
      </RouterLink>
      <RouterLink class="home-action-card" to="/vagas">
        <span class="home-action-icon"><BriefcaseBusiness :size="19" /></span>
        <span class="home-action-copy">
          <strong>Cadastrar vaga</strong>
          <small>{{ estado.vagas.length }} vagas de interesse</small>
        </span>
        <ArrowRight :size="17" />
      </RouterLink>
      <RouterLink class="home-action-card" to="/treinos">
        <span class="home-action-icon"><MessageSquareText :size="19" /></span>
        <span class="home-action-copy">
          <strong>Iniciar treinamento</strong>
          <small>Pratique por habilidades ou vaga</small>
        </span>
        <ArrowRight :size="17" />
      </RouterLink>
    </div>
  </section>

  <section class="home-latest">
    <div class="home-section-heading">
      <div>
        <span class="home-section-kicker">SEU DESEMPENHO</span>
        <h2>Treino mais recente</h2>
      </div>
      <RouterLink class="home-text-link" to="/historico">
        Ver histórico <ArrowRight :size="15" />
      </RouterLink>
    </div>

    <RouterLink
      v-if="ultimoTreino"
      class="latest-training-card"
      :to="{ path: '/feedback', query: { indice: 0 } }"
    >
      <span class="latest-training-icon"><History :size="19" /></span>
      <span class="latest-training-copy">
        <strong>{{ ultimoTreino.titulo }}</strong>
        <small>{{ ultimoTreino.data }} · {{ ultimoTreino.contexto }}</small>
        <span class="latest-feedback-link">Ver feedback <ArrowRight :size="14" /></span>
      </span>
      <span class="latest-score">
        <strong>{{ ultimoTreino.nota }}</strong><small>/100</small>
      </span>
    </RouterLink>
    <div v-else class="home-empty-state">
      <span class="home-action-icon"><MessageSquareText :size="20" /></span>
      <div>
        <strong>Seu histórico começa com o primeiro treino.</strong>
        <p>Escolha suas habilidades ou uma vaga e faça uma simulação.</p>
      </div>
      <RouterLink class="home-empty-link" to="/treinos">Começar agora</RouterLink>
    </div>
  </section>

  <section class="how-it-works">
    <div class="home-section-heading">
      <div>
        <span class="home-section-kicker">SIMPLES ASSIM</span>
        <h2>Como funciona</h2>
      </div>
      <p>Uma prática guiada para você chegar mais preparado à entrevista.</p>
    </div>
    <div class="steps-grid">
      <article class="step-card">
        <span class="step-number">01</span>
        <span class="step-icon"><Code2 :size="21" /></span>
        <h3>Escolha seu contexto</h3>
        <p>Use suas habilidades cadastradas ou selecione uma vaga de interesse.</p>
      </article>
      <article class="step-card">
        <span class="step-number">02</span>
        <span class="step-icon"><MessageSquareText :size="21" /></span>
        <h3>Responda às perguntas</h3>
        <p>Treine suas respostas em uma simulação textual demonstrativa.</p>
      </article>
      <article class="step-card">
        <span class="step-number">03</span>
        <span class="step-icon"><Check :size="21" /></span>
        <h3>Revise seu resultado</h3>
        <p>Consulte a avaliação e acompanhe suas práticas no histórico.</p>
      </article>
    </div>
  </section>

</template>

<style scoped>
.home-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 280px;
  margin: 0 0 24px;
  padding: 38px 48px;
  overflow: hidden;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(112deg, #11152e 0%, #201c50 58%, #39317c 100%);
}

.home-hero-copy {
  position: relative;
  z-index: 1;
  max-width: 650px;
}

.home-hero-kicker,
.home-section-kicker {
  color: #9f97f7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.home-hero h1 {
  margin: 12px 0 10px;
  color: #fff;
  font: 800 clamp(28px, 3.5vw, 42px)/1.12 Manrope, sans-serif;
  letter-spacing: -1.5px;
}

.home-hero-copy p {
  max-width: 560px;
  margin: 0 0 22px;
  color: #dcdaef;
  font-size: 14px;
  line-height: 1.7;
}

.home-hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 999px;
  color: #201c50;
  background: #c5f477;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
  transition: transform .18s ease, background .18s ease;
}

.home-hero-cta:hover {
  transform: translateY(-2px);
  color: #201c50;
  background: #d3ff8c;
}

.home-hero-art {
  position: absolute;
  inset: 0 0 0 auto;
  width: 42%;
  overflow: hidden;
  background: repeating-linear-gradient(132deg, transparent 0 44px, #625bd033 45px 92px);
  clip-path: polygon(32% 0, 100% 0, 100% 100%, 0 100%);
}

.hero-orbit {
  position: absolute;
  right: 8%;
  width: 245px;
  height: 245px;
  border: 1px solid #d3ccff40;
  border-radius: 50%;
}

.hero-orbit-one {
  top: -95px;
}

.hero-orbit-two {
  top: -50px;
  right: 18%;
  width: 170px;
  height: 170px;
}

.hero-note {
  position: absolute;
  right: 13%;
  bottom: 48px;
  display: grid;
  gap: 10px;
  width: 160px;
  padding: 16px;
  border: 1px solid #ffffff24;
  border-radius: 14px;
  color: #fff;
  background: #ffffff12;
  backdrop-filter: blur(8px);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.home-progress,
.home-actions,
.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.home-progress {
  margin-bottom: 34px;
}

.progress-card,
.home-action-card,
.step-card,
.latest-training-card,
.home-empty-state {
  border: 1px solid #e6e7ef;
  border-radius: 14px;
  background: #fff;
}

.progress-card {
  display: grid;
  grid-template-columns: 38px 1fr;
  column-gap: 12px;
  align-items: center;
  padding: 17px;
}

.progress-icon,
.home-action-icon,
.latest-training-icon,
.step-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  color: #6757df;
  background: #f0edff;
}

.progress-icon {
  grid-row: span 3;
}

.progress-label,
.progress-card small {
  color: var(--ivis-text-secondary);
  font-size: 14px;
}

.progress-card strong {
  color: #252631;
  font: 700 22px/1.25 Manrope, sans-serif;
}

.home-section {
  margin-bottom: 34px;
}

.home-section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.home-section-kicker {
  color: var(--ivis-text-secondary);
  font-size: 12px;
  letter-spacing: 1.2px;
}

.home-section-heading h2 {
  margin: 5px 0 0;
  color: #292a35;
  font: 700 22px/1.3 Manrope, sans-serif;
  letter-spacing: -.4px;
}

.home-action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 88px;
  padding: 16px;
  color: #343540;
  text-decoration: none;
  transition: border-color .18s ease, transform .18s ease, box-shadow .18s ease;
}

.home-action-card > svg {
  margin-left: auto;
  color: var(--ivis-text-secondary);
}

.home-action-card:hover {
  transform: translateY(-2px);
  border-color: #c9c2ff;
  color: #343540;
  box-shadow: 0 8px 22px #26204c0c;
}

.home-action-copy {
  display: grid;
  gap: 4px;
}

.home-action-copy strong {
  font-size: 16px;
  font-weight: 700;
}

.home-action-copy small {
  color: var(--ivis-text-secondary);
  font-size: 13px;
}

.home-latest {
  margin-bottom: 34px;
}

.home-text-link,
.latest-feedback-link,
.home-empty-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #5b4cce;
  font-size: 11px;
  font-weight: 600;
  text-decoration: none;
}

.latest-training-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  color: inherit;
  text-decoration: none;
  transition: border-color .18s ease, box-shadow .18s ease;
}

.latest-training-card:hover {
  border-color: #c9c2ff;
  box-shadow: 0 8px 22px #26204c0c;
}

.latest-training-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.latest-training-copy > strong {
  color: #292a35;
  font-size: 16px;
  font-weight: 700;
}

.latest-training-copy > small {
  overflow: hidden;
  color: var(--ivis-text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.latest-feedback-link {
  margin-top: 4px;
}

.latest-score {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-left: auto;
  color: #298b68;
  white-space: nowrap;
}

.latest-score strong {
  font: 700 21px Manrope, sans-serif;
}

.latest-score small {
  font-size: 10px;
}

.home-empty-state {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.home-empty-state strong {
  color: #292a35;
  font-size: 13px;
}

.home-empty-state p {
  margin: 5px 0 0;
  color: var(--ivis-text-secondary);
  font-size: 13px;
}

.home-empty-link {
  margin-left: auto;
  white-space: nowrap;
}

.how-it-works {
  margin: 0 -20px 18px;
  padding: 25px 20px 28px;
  border-radius: 18px;
  background: #e8efff;
}

.how-it-works .home-section-heading {
  margin-bottom: 18px;
}

.how-it-works .home-section-heading > p {
  max-width: 350px;
  margin: 0;
  color: var(--ivis-text-secondary);
  font-size: 13px;
  text-align: right;
}

.step-card {
  position: relative;
  min-height: 178px;
  padding: 19px;
}

.step-number {
  position: absolute;
  top: 15px;
  right: 16px;
  color: #c2c3cf;
  font: 700 12px Manrope, sans-serif;
}

.step-icon {
  margin-bottom: 14px;
  background: #f0edff;
}

.step-card h3 {
  margin: 0 0 7px;
  color: #292a35;
  font: 700 13px Manrope, sans-serif;
}

.step-card p {
  margin: 0;
  color: var(--ivis-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.home-demo-note {
  margin: 0;
  color: var(--ivis-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .home-hero {
    min-height: 250px;
    padding: 30px;
  }

  .home-hero-art {
    width: 32%;
    opacity: .7;
  }

  .home-progress,
  .home-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .home-hero {
    min-height: 250px;
    padding: 25px 20px;
  }

  .home-hero-copy {
    max-width: 100%;
  }

  .home-hero-copy p {
    max-width: 90%;
    font-size: 12px;
  }

  .home-hero-art {
    right: -18%;
    width: 52%;
    opacity: .35;
  }

  .hero-note {
    display: none;
  }

  .home-progress,
  .home-actions,
  .steps-grid {
    grid-template-columns: 1fr;
  }

  .home-section-heading {
    align-items: start;
  }

  .home-section-heading h2 {
    font-size: 22px;
  }

  .home-latest .home-section-heading {
    align-items: end;
  }

  .latest-training-card {
    align-items: flex-start;
    gap: 10px;
    padding: 13px;
  }

  .latest-training-copy > small {
    max-width: 55vw;
  }

  .home-empty-state {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .home-empty-link {
    margin-left: 52px;
  }

  .how-it-works {
    margin-right: -8px;
    margin-left: -8px;
    padding: 20px 14px;
  }

  .how-it-works .home-section-heading {
    display: block;
  }

  .how-it-works .home-section-heading > p {
    margin-top: 8px;
    text-align: left;
  }
}
</style>
