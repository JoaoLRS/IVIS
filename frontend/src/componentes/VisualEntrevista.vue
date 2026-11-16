<template>
  <div class="interview-preview" aria-label="Prévia de uma entrevista por texto no IVIS">
    <div class="interview-preview-header">
      <span><i class="interview-status" aria-hidden="true" /> Treino por texto</span>
      <span>01 / 03</span>
    </div>

    <div class="interview-preview-question">
      <span class="interview-preview-label">Pergunta contextualizada</span>
      <p>Conte sobre uma decisão técnica que você tomou e como avaliou o resultado.</p>
    </div>

    <div class="interview-preview-answer">
      <span class="interview-preview-label">Sua resposta</span>
      <p>Primeiro, entendi o contexto e comparei as opções disponíveis<span class="interview-caret" aria-hidden="true" /></p>
    </div>

    <div class="interview-preview-feedback">
      <span class="interview-preview-label">Feedback ao final</span>
      <div class="interview-feedback-tags">
        <span>Clareza</span>
        <span>Próximos passos</span>
      </div>
    </div>

    <div class="interview-preview-progress" aria-hidden="true"><span /></div>
  </div>
</template>

<style scoped>
.interview-preview {
  position: relative;
  width: 100%;
  overflow: hidden;
  border: 1px solid #8e89df68;
  background: #10152dd9;
  box-shadow: 0 22px 60px #090b225c;
  text-align: left;
}

.interview-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #8e89df3d;
  color: #dce1ff;
  font-size: 12px;
  font-weight: 700;
}

.interview-preview-header > span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.interview-preview-header > span:last-child {
  color: #a6a9cf;
  font-size: 11px;
}

.interview-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c5f477;
  box-shadow: 0 0 0 4px #c5f47724;
}

.interview-preview-question,
.interview-preview-answer,
.interview-preview-feedback {
  margin: 16px 18px;
  padding: 14px 16px;
  border: 1px solid #7776ae56;
  background: #22244b;
}

.interview-preview-answer {
  margin-left: 54px;
  border-color: #7291dc70;
  background: #172b57;
}

.interview-preview-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-color: #c5f4775c;
  background: #1a2c38;
}

.interview-preview-label {
  display: block;
  color: #b9b8ff;
  font-size: 11px;
  font-weight: 700;
}

.interview-preview-feedback .interview-preview-label {
  color: #d1f59b;
}

.interview-preview p {
  margin: 7px 0 0;
  color: #f6f7ff;
  font-size: 13px;
  line-height: 1.55;
}

.interview-preview-answer p {
  color: #dce8ff;
}

.interview-caret {
  display: inline-block;
  width: 1px;
  height: 1em;
  margin-left: 3px;
  vertical-align: -2px;
  background: #c5f477;
  animation: interview-caret 1s steps(1, end) infinite;
}

.interview-feedback-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.interview-feedback-tags span {
  padding: 5px 7px;
  border: 1px solid #c5f47766;
  color: #d8f7ab;
  font-size: 10px;
  white-space: nowrap;
}

.interview-preview-progress {
  height: 3px;
  background: #7a79b94d;
}

.interview-preview-progress span {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, #615ce0, #c5f477);
  transform-origin: left;
  animation: interview-progress 8s ease-in-out infinite;
}

@keyframes interview-caret {
  50% { opacity: 0; }
}

@keyframes interview-progress {
  0%, 8% { transform: scaleX(0.06); }
  85%, 100% { transform: scaleX(1); }
}

@media (prefers-reduced-motion: reduce) {
  .interview-caret,
  .interview-preview-progress span { animation: none; }
}

@media (max-width: 1050px) {
  .interview-preview-feedback { align-items: flex-start; flex-direction: column; }
  .interview-feedback-tags { justify-content: flex-start; }
}
</style>
