import { defineStore } from "pinia";
import { ref } from "vue";
import {
  habilidadesIniciais,
  historicoInicial,
  perguntasDemonstracao,
  vagasIniciais,
  type Habilidade,
  type Treino,
  type Vaga,
} from "../dados/dadosDemonstracao";

export const useEstadoAplicacao = defineStore("aplicacao", () => {
  const vagas = ref<Vaga[]>([...vagasIniciais]);
  const habilidades = ref<Habilidade[]>([...habilidadesIniciais]);
  const historico = ref<Treino[]>([...historicoInicial]);
  const treinoAtivo = ref(false);
  const perguntaAtual = ref(0);
  const respostas = ref<string[]>([]);
  const contextoTreino = ref("Perfil de habilidades");
  const vagaEmTreinamento = ref<string | null>(null);
  const perguntasDoTreino = ref<string[]>([]);
  const feedbackAtual = ref<Treino | null>(null);

  function adicionarVaga(vaga: Vaga) {
    vagas.value.unshift(vaga);
  }
  function removerVaga(indice: number) {
    vagas.value.splice(indice, 1);
  }
  function editarVaga(indice: number, vaga: Vaga) {
    vagas.value.splice(indice, 1, vaga);
  }
  function adicionarHabilidade(habilidade: Habilidade) {
    habilidades.value.unshift(habilidade);
  }
  function removerHabilidade(indice: number) {
    habilidades.value.splice(indice, 1);
  }
  function editarHabilidade(indice: number, habilidade: Habilidade) {
    habilidades.value.splice(indice, 1, habilidade);
  }
  function iniciarTreino(vaga?: Vaga) {
    treinoAtivo.value = true;
    perguntaAtual.value = 0;
    respostas.value = [];
    vagaEmTreinamento.value = vaga?.titulo ?? null;
    contextoTreino.value = vaga
      ? `Vaga: ${vaga.titulo} · ${vaga.empresa}`
      : `Habilidades: ${habilidades.value.map((item) => item.nome).join(", ") || "nenhuma habilidade cadastrada"}`;
    const temas = vaga
      ? vaga.habilidades
      : habilidades.value.map((habilidade) => habilidade.nome);
    perguntasDoTreino.value = temas.length
      ? temas
          .slice(0, 3)
          .map(
            (tema) =>
              `Conte sobre sua experiência com ${tema}. Como você aplicaria esse conhecimento em um desafio técnico?`,
          )
      : [...perguntasDemonstracao];
  }
  function responder(resposta: string) {
    respostas.value.push(resposta);
    perguntaAtual.value += 1;
  }
  function encerrarTreino() {
    treinoAtivo.value = false;
    vagaEmTreinamento.value = null;
  }
  function concluirTreino() {
    const treino: Treino = {
      titulo: vagaEmTreinamento.value
        ? `Entrevista · ${vagaEmTreinamento.value}`
        : "Entrevista por habilidades",
      contexto: contextoTreino.value,
      data: new Date().toLocaleString("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      }),
      nota: 86,
      duracao: "24 min",
      pontosFortes:
        "Respostas registradas e participação nas perguntas técnicas.",
      melhorias:
        "Continue praticando e inclua exemplos concretos nas respostas.",
    };
    historico.value.unshift(treino);
    feedbackAtual.value = treino;
    encerrarTreino();
  }

  return {
    vagas,
    habilidades,
    historico,
    treinoAtivo,
    perguntaAtual,
    respostas,
    contextoTreino,
    vagaEmTreinamento,
    feedbackAtual,
    perguntasDemonstracao,
    perguntasDoTreino,
    adicionarVaga,
    removerVaga,
    editarVaga,
    adicionarHabilidade,
    removerHabilidade,
    editarHabilidade,
    iniciarTreino,
    responder,
    encerrarTreino,
    concluirTreino,
  };
});
