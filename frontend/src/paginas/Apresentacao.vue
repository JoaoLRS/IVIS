<script setup lang="ts">
import { computed } from "vue";
import {
  ArrowRight,
  ArrowUpRight,
  BrainCircuit,
  Building2,
  LogOut,
  MessageSquareText,
  Sparkles,
  UserRound,
} from "lucide-vue-next";
import { RouterLink } from "vue-router";
import { useEstadoSessao } from "../estados/estadoSessao";

const sessao = useEstadoSessao();

async function sairDaConta() {
  await sessao.sair();
}

const destinoPrincipal = computed(() => {
  if (sessao.temPlano) return "/treinos";
  if (sessao.conta) return "/planos?acesso=necessario";
  return "/cadastro";
});

const rotuloPrincipal = computed(() => {
  if (sessao.temPlano) return "Iniciar treinamento";
  if (sessao.conta) return "Ver opções de acesso";
  return "Começar agora";
});
</script>

<template>
  <main class="public-home">
    <header class="public-header">
      <div class="public-header-inner">
        <RouterLink
          class="public-brand"
          to="/"
          aria-label="IVIS, página inicial"
        >
          IVIS<span>.</span>
        </RouterLink>

        <nav class="public-nav" aria-label="Navegação principal">
          <a href="#como-funciona">Como funciona</a>
          <RouterLink to="/planos">Planos</RouterLink>
          <RouterLink
            v-if="sessao.conta"
            class="public-login"
            :to="sessao.temPlano ? '/inicio' : '/planos'"
          >
            <UserRound :size="16" />
            {{ sessao.temPlano ? "Minha área" : "Ativar acesso" }}
          </RouterLink>
          <RouterLink v-else class="public-login" to="/login">
            <UserRound :size="16" /> Entrar
          </RouterLink>
          <button
            v-if="sessao.conta"
            class="public-login public-logout"
            type="button"
            @click="sairDaConta"
          >
            <LogOut :size="16" /> Sair
          </button>
        </nav>
      </div>
    </header>

    <section class="public-hero">
      <div class="public-hero-pattern" aria-hidden="true">
        <span class="public-hero-shape public-hero-shape-one" />
        <span class="public-hero-shape public-hero-shape-two" />
        <span class="public-hero-shape public-hero-shape-three" />
      </div>
      <div class="public-hero-content">
        <span class="public-beta">TREINAMENTO COM IA</span>
        <h1>Pratique para sua próxima entrevista</h1>
        <p>
          Prepare-se com entrevistas técnicas por texto, perguntas
          contextualizadas e feedback para orientar seus próximos passos.
        </p>
        <RouterLink class="public-primary-cta" :to="destinoPrincipal">
          {{ rotuloPrincipal }} <ArrowUpRight :size="18" />
        </RouterLink>
      </div>
      <div class="public-hero-note" aria-hidden="true">
        <span><MessageSquareText :size="22" /></span>
        <strong>Treine no seu ritmo</strong>
        <small>Uma pergunta por vez</small>
      </div>
    </section>

    <section id="como-funciona" class="public-benefits">
      <div class="public-benefits-inner">
        <div class="public-benefit-card">
          <span class="public-benefit-icon public-benefit-icon-purple"
            ><BrainCircuit :size="23"
          /></span>
          <h2>Perguntas contextualizadas</h2>
          <p>
            Use suas habilidades ou uma vaga de interesse como contexto para
            praticar.
          </p>
        </div>
        <div class="public-benefit-card">
          <span class="public-benefit-icon public-benefit-icon-blue"
            ><MessageSquareText :size="23"
          /></span>
          <h2>Entrevista por texto</h2>
          <p>
            Responda às perguntas no seu tempo e mantenha o foco no conteúdo.
          </p>
        </div>
        <div class="public-benefit-card">
          <span class="public-benefit-icon public-benefit-icon-green"
            ><Sparkles :size="23"
          /></span>
          <h2>Feedback ao final</h2>
          <p>
            Receba uma avaliação com pontos fortes e aspectos que pode
            desenvolver.
          </p>
        </div>
      </div>

      <div class="public-benefits-actions">
        <RouterLink class="public-secondary-cta" to="/planos">
          Conhecer planos <ArrowRight :size="16" />
        </RouterLink>
        <RouterLink class="public-outline-cta" :to="destinoPrincipal">
          {{ rotuloPrincipal }}
        </RouterLink>
      </div>
    </section>

    <section class="public-audience">
      <div>
        <span class="public-section-kicker">ACESSO PARA CADA JORNADA</span>
        <h2>Treine individualmente ou com sua instituição</h2>
        <p>
          O IVIS oferece acesso por plano individual e por vínculo
          institucional. Consulte as modalidades disponíveis e veja qual se
          aplica a você.
        </p>
      </div>
      <div class="public-audience-actions">
        <RouterLink to="/planos"
          ><UserRound :size="18" /> Planos individuais</RouterLink
        >
        <RouterLink to="/planos"
          ><Building2 :size="18" /> Acesso institucional</RouterLink
        >
      </div>
    </section>

    <footer class="public-footer">
      <RouterLink class="public-brand" to="/">IVIS<span>.</span></RouterLink>
      <span>Pratique. Aprenda. Evolua.</span>
      <RouterLink to="/login">Acessar minha conta</RouterLink>
    </footer>
  </main>
</template>

<style scoped>
.public-home {
  min-height: 100vh;
  color: #22243a;
  background: #fff;
  font-family: Manrope, Inter, sans-serif;
}

.public-header {
  position: relative;
  z-index: 2;
  background: #fff;
  border-bottom: 1px solid #ececf2;
}

.public-header-inner {
  width: min(100% - 48px, 1120px);
  min-height: 82px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.public-brand {
  color: #15172e;
  font:
    800 27px/1 Manrope,
    sans-serif;
  letter-spacing: -1.4px;
  text-decoration: none;
}

.public-brand span {
  color: #7768ec;
}

.public-nav {
  display: flex;
  align-items: center;
  gap: clamp(18px, 3vw, 42px);
}

.public-nav > a {
  color: #4a4c61;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.18s ease;
}

.public-nav > a:hover,
.public-footer > a:last-child:hover {
  color: #6757df;
}

.public-nav .public-login {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 17px;
  border: 0;
  border-radius: 0;
  color: #4d43a4;
  background: transparent;
  text-decoration: none;
}

.public-nav .public-login:hover {
  color: #6757df;
}

.public-nav .public-logout {
  background: #fff;
  font: inherit;
  cursor: pointer;
}

.public-hero {
  position: relative;
  min-height: 405px;
  display: flex;
  align-items: center;
  overflow: hidden;
  color: white;
  background: linear-gradient(112deg, #11152e 0%, #201c50 58%, #39317c 100%);
}

.public-hero-pattern {
  position: absolute;
  inset: 0 0 0 auto;
  width: 46%;
  overflow: hidden;
  background: repeating-linear-gradient(
    132deg,
    transparent 0 46px,
    #766fe024 47px 96px
  );
  clip-path: polygon(38% 0, 100% 0, 100% 100%, 0 100%);
}

.public-hero-shape {
  position: absolute;
  display: block;
  width: 320px;
  height: 320px;
  border: 1px solid #d3ccff35;
  transform: rotate(45deg);
}

.public-hero-shape-one {
  top: -150px;
  right: 12%;
}
.public-hero-shape-two {
  top: -75px;
  right: -15%;
  width: 390px;
  height: 390px;
}
.public-hero-shape-three {
  top: 180px;
  right: 45%;
  width: 220px;
  height: 220px;
}

.public-hero-content {
  position: relative;
  z-index: 1;
  width: min(100% - 48px, 1120px);
  margin: 0 auto;
  padding: 55px 0 62px;
}

.public-beta,
.public-section-kicker {
  color: #aaa2ff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.6px;
}

.public-hero h1 {
  max-width: 690px;
  margin: 15px 0 13px;
  color: #fff;
  font:
    800 clamp(36px, 5vw, 58px)/1.08 Manrope,
    sans-serif;
  letter-spacing: -2.2px;
}

.public-hero-content > p {
  max-width: 610px;
  margin: 0 0 27px;
  color: #dfdef0;
  font-size: 15px;
  line-height: 1.8;
}

.public-primary-cta,
.public-secondary-cta,
.public-outline-cta {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 22px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.public-primary-cta,
.public-secondary-cta {
  color: #1d2035;
  background: #c5f477;
}

.public-primary-cta:hover,
.public-secondary-cta:hover {
  transform: translateY(-2px);
  color: #1d2035;
  background: #d3ff8c;
  box-shadow: 0 8px 22px #0002;
}

.public-hero-note {
  position: absolute;
  right: max(6vw, calc((100vw - 1120px) / 2));
  bottom: 70px;
  z-index: 1;
  display: grid;
  gap: 8px;
  width: 168px;
  padding: 17px;
  border: 1px solid #ffffff2e;
  border-radius: 14px;
  background: #ffffff12;
  backdrop-filter: blur(8px);
}

.public-hero-note > span {
  color: #c5f477;
}
.public-hero-note strong {
  font-size: 12px;
}
.public-hero-note small {
  color: #d8d5ee;
  font-size: 11px;
}

.public-benefits {
  padding: 54px 24px 50px;
  background: #e8efff;
}

.public-benefits-inner {
  width: min(100%, 1120px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.public-benefit-card {
  min-height: 190px;
  padding: 27px 25px;
  border: 1px solid #e4e7f3;
  border-radius: 16px;
  background: #fff;
  text-align: center;
  box-shadow: 0 5px 18px #15172e08;
}

.public-benefit-icon {
  width: 46px;
  height: 46px;
  margin: 0 auto 15px;
  display: grid;
  place-items: center;
  border-radius: 14px;
}

.public-benefit-icon-purple {
  color: #6757df;
  background: #f0edff;
}
.public-benefit-icon-blue {
  color: #4385e8;
  background: #eaf2ff;
}
.public-benefit-icon-green {
  color: #3f8c68;
  background: #edf8e2;
}

.public-benefit-card h2 {
  margin: 0 0 8px;
  color: #26283c;
  font:
    700 16px/1.4 Manrope,
    sans-serif;
}

.public-benefit-card p {
  max-width: 290px;
  margin: 0 auto;
  color: #6c6e7d;
  font-size: 12px;
  line-height: 1.7;
}

.public-benefits-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 27px;
}

.public-outline-cta {
  border: 1.5px solid #27294a;
  color: #27294a;
  background: transparent;
}

.public-outline-cta:hover {
  color: #fff;
  background: #27294a;
}

.public-audience {
  width: min(100% - 48px, 1120px);
  margin: 0 auto;
  padding: 68px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
}

.public-audience > div:first-child {
  max-width: 610px;
}
.public-section-kicker {
  color: #6757df;
}

.public-audience h2 {
  margin: 10px 0;
  color: #24263b;
  font:
    800 clamp(24px, 3vw, 34px)/1.2 Manrope,
    sans-serif;
  letter-spacing: -0.8px;
}

.public-audience p {
  margin: 0;
  color: #686a79;
  font-size: 14px;
  line-height: 1.8;
}

.public-audience-actions {
  display: grid;
  gap: 11px;
  min-width: 235px;
}
.public-audience-actions a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e6e7ef;
  border-radius: 12px;
  color: #3c3e54;
  background: #fff;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}
.public-audience-actions a:hover {
  border-color: #c9c2ff;
  color: #5142c5;
}

.public-footer {
  min-height: 82px;
  padding: 20px max(24px, calc((100vw - 1120px) / 2));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-top: 1px solid #ececf2;
  color: #777988;
  font-size: 12px;
}

.public-footer > a:last-child {
  color: #5142c5;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 760px) {
  .public-header-inner {
    width: min(100% - 32px, 1120px);
    min-height: 70px;
  }
  .public-nav {
    gap: 15px;
  }
  .public-nav > a {
    font-size: 11px;
  }
  .public-nav > a:first-child {
    display: none;
  }
  .public-nav .public-login {
    padding: 8px 12px;
  }
  .public-hero {
    min-height: 395px;
  }
  .public-hero-content {
    width: min(100% - 40px, 1120px);
  }
  .public-hero h1 {
    max-width: 540px;
  }
  .public-hero-note {
    display: none;
  }
  .public-hero-pattern {
    width: 38%;
    opacity: 0.55;
  }
  .public-benefits-inner {
    grid-template-columns: 1fr;
    max-width: 480px;
  }
  .public-benefit-card {
    min-height: 0;
  }
  .public-audience {
    width: min(100% - 40px, 1120px);
    padding: 48px 0;
    align-items: stretch;
    flex-direction: column;
  }
  .public-audience-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .public-footer {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .public-nav {
    gap: 10px;
  }
  .public-nav > a {
    font-size: 10px;
  }
  .public-nav .public-login {
    gap: 5px;
    padding: 8px 9px;
  }
  .public-hero h1 {
    font-size: 36px;
    letter-spacing: -1.5px;
  }
  .public-hero-content > p {
    font-size: 13px;
  }
  .public-audience-actions {
    grid-template-columns: 1fr;
  }
  .public-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
