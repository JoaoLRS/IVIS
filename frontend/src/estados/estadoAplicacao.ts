import { defineStore } from "pinia";
import { ref } from "vue";
import type { Treino } from "../tipos/treino";
import { listarHistorico } from "../servicos/historico";
import {
  listarHabilidades,
  listarVagas,
  type HabilidadePerfil,
  type VagaInteressePerfil,
} from "../servicos/perfilAluno";
import {
  cancelarTreinoRemoto,
  concluirTreinoRemoto,
  consultarTreinoEmAndamento,
  enviarRespostaTreino,
  iniciarTreinoRemoto,
  type SessaoTreino,
} from "../servicos/treinos";

export const useEstadoAplicacao = defineStore("aplicacao", () => {
  const totalPerguntas = 3;
  const nomeAluno = ref("Nome do aluno");
  const emailAluno = ref("aluno@exemplo.com");
  const vagas = ref<VagaInteressePerfil[]>([]);
  const habilidades = ref<HabilidadePerfil[]>([]);
  const historico = ref<Treino[]>([]);
  const treinoAtivo = ref(false);
  const perguntaAtual = ref(0);
  const respostas = ref<string[]>([]);
  const contextoTreino = ref("Perfil de habilidades");
  const vagaEmTreinamento = ref<string | null>(null);
  const vagaIdEmTreinamento = ref<string | null>(null);
  const habilidadesDoTreino = ref<string[]>([]);
  const perguntasDoTreino = ref<string[]>([]);
  const feedbackAtual = ref<Treino | null>(null);
  const respostaPendente = ref<string | null>(null);
  const sessaoTreino = ref<SessaoTreino | null>(null);

  function aplicarSessaoTreino(sessao: SessaoTreino) {
    sessaoTreino.value = sessao;
    treinoAtivo.value = true;
    contextoTreino.value = sessao.contexto;
    vagaIdEmTreinamento.value = sessao.vagaId;
    vagaEmTreinamento.value = sessao.tipoContexto === "VAGA"
      ? sessao.contexto.replace(/^Vaga: /, "").split(" · ")[0]
      : null;
    habilidadesDoTreino.value = sessao.habilidades;
    perguntasDoTreino.value = sessao.perguntas.map((pergunta) => pergunta.texto);
    respostas.value = sessao.perguntas
      .filter((pergunta) => pergunta.resposta !== null)
      .map((pergunta) => pergunta.resposta as string);
    perguntaAtual.value = respostas.value.length;
  }

  function atualizarDadosAluno(dados: { nome: string; email: string }) {
    nomeAluno.value = dados.nome.trim();
    emailAluno.value = dados.email.trim();
  }

  function limparDadosDaConta() {
    nomeAluno.value = "Nome do aluno";
    emailAluno.value = "";
    vagas.value = [];
    habilidades.value = [];
    historico.value = [];
    treinoAtivo.value = false;
    perguntaAtual.value = 0;
    respostas.value = [];
    contextoTreino.value = "Perfil de habilidades";
    vagaEmTreinamento.value = null;
    vagaIdEmTreinamento.value = null;
    habilidadesDoTreino.value = [];
    perguntasDoTreino.value = [];
    feedbackAtual.value = null;
    respostaPendente.value = null;
    sessaoTreino.value = null;
  }

  async function carregarPerfil(token: string, contaId: string, contaAtual: () => string | null) {
    const [habilidadesPersistidas, vagasPersistidas, treinosPersistidos, treinoPendente] = await Promise.all([
      listarHabilidades(token),
      listarVagas(token),
      listarHistorico(),
      consultarTreinoEmAndamento(),
    ]);
    if (contaAtual() !== contaId) return;
    habilidades.value = habilidadesPersistidas;
    vagas.value = vagasPersistidas;
    historico.value = treinosPersistidos.map((treino) => ({
      ...treino,
      data: new Date(treino.data).toLocaleString("pt-BR"),
      duracao: "Entrevista concluída",
      pontosFortes: treino.pontosFortes.join(" "),
      melhorias: treino.pontosMelhoria.join(" "),
    }));
    if (treinoPendente) aplicarSessaoTreino(treinoPendente);
  }

  function adicionarVaga(vaga: VagaInteressePerfil) {
    vagas.value.unshift(vaga);
  }
  function removerVaga(indice: number) {
    vagas.value.splice(indice, 1);
  }
  function editarVaga(indice: number, vaga: VagaInteressePerfil) {
    vagas.value.splice(indice, 1, vaga);
  }
  function adicionarHabilidade(habilidade: HabilidadePerfil) {
    habilidades.value.unshift(habilidade);
  }
  function removerHabilidade(indice: number) {
    habilidades.value.splice(indice, 1);
  }
  function editarHabilidade(indice: number, habilidade: HabilidadePerfil) {
    habilidades.value.splice(indice, 1, habilidade);
  }
  async function iniciarTreino(
    vaga?: VagaInteressePerfil,
    habilidadesSelecionadas?: string[],
  ) {
    const sessao = await iniciarTreinoRemoto({
      tipoContexto: vaga ? "VAGA" : "HABILIDADES",
      vagaId: vaga?.id ?? null,
      habilidades: vaga ? [] : (habilidadesSelecionadas ?? habilidades.value.map((item) => item.nome)),
    });
    respostaPendente.value = null;
    aplicarSessaoTreino(sessao);
  }

  async function responder(resposta: string) {
    const respostaAtual = resposta.trim();
    if (respostaAtual) respostaPendente.value = respostaAtual;
    if (!respostaPendente.value || !sessaoTreino.value) {
      throw new Error("Não há uma resposta pendente para enviar.");
    }
    const pergunta = sessaoTreino.value.perguntas[respostas.value.length];
    if (!pergunta) {
      throw new Error("Não há uma pergunta ativa para receber a resposta.");
    }
    const sessao = await enviarRespostaTreino(
      sessaoTreino.value.id, pergunta.id, respostaPendente.value,
    );
    aplicarSessaoTreino(sessao);
    respostaPendente.value = null;
  }
  function encerrarTreino() {
    treinoAtivo.value = false;
    vagaEmTreinamento.value = null;
    vagaIdEmTreinamento.value = null;
    sessaoTreino.value = null;
    respostaPendente.value = null;
  }
  async function cancelarTreino() {
    if (!sessaoTreino.value) return;
    await cancelarTreinoRemoto(sessaoTreino.value.id);
    encerrarTreino();
  }
  async function concluirTreino() {
    if (!sessaoTreino.value || respostas.value.length !== totalPerguntas) {
      throw new Error("Responda a todas as perguntas antes de finalizar o treino.");
    }
    const salvo = await concluirTreinoRemoto(sessaoTreino.value.id);
    const treino: Treino = {
      id: salvo.id,
      titulo: salvo.titulo,
      contexto: salvo.contexto,
      data: new Date(salvo.data).toLocaleString("pt-BR"),
      nota: salvo.nota,
      duracao: "Entrevista concluída",
      pontosFortes: salvo.pontosFortes.join(" "),
      melhorias: salvo.pontosMelhoria.join(" "),
    };
    historico.value.unshift(treino);
    feedbackAtual.value = treino;
    encerrarTreino();
  }

  return {
    nomeAluno,
    emailAluno,
    vagas,
    habilidades,
    historico,
    treinoAtivo,
    perguntaAtual,
    respostas,
    contextoTreino,
    vagaEmTreinamento,
    vagaIdEmTreinamento,
    habilidadesDoTreino,
    feedbackAtual,
    respostaPendente,
    sessaoTreino,
    carregarPerfil,
    limparDadosDaConta,
    atualizarDadosAluno,
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
    cancelarTreino,
    concluirTreino,
  };
});
