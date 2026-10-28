import { defineStore } from "pinia";
import { ref } from "vue";
import type { Treino } from "../tipos/treino";
import { listarHistorico, salvarTreino } from "../servicos/historico";
import {
  listarHabilidades,
  listarVagas,
  type HabilidadePerfil,
  type VagaInteressePerfil,
} from "../servicos/perfilAluno";
import {
  gerarAvaliacao,
  gerarPergunta,
  type ContextoTreinoIa,
  type GerarAvaliacaoResposta,
  type InteracaoIa,
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
  const avaliacaoPendente = ref<GerarAvaliacaoResposta | null>(null);
  const contextoIa = ref<Omit<ContextoTreinoIa, "interacoes"> | null>(null);

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
    avaliacaoPendente.value = null;
    contextoIa.value = null;
  }

  async function carregarPerfil(token: string, contaId: string, contaAtual: () => string | null) {
    const [habilidadesPersistidas, vagasPersistidas, treinosPersistidos] = await Promise.all([
      listarHabilidades(token),
      listarVagas(token),
      listarHistorico(),
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
    perguntaAtual.value = 0;
    respostas.value = [];
    respostaPendente.value = null;
    avaliacaoPendente.value = null;
    perguntasDoTreino.value = [];
    vagaEmTreinamento.value = vaga?.titulo ?? null;
    vagaIdEmTreinamento.value = vaga?.id ?? null;
    const temas = vaga
      ? vaga.requisitos
      : (habilidadesSelecionadas ??
        habilidades.value.map((habilidade) => habilidade.nome));
    habilidadesDoTreino.value = [...temas];
    contextoTreino.value = vaga
      ? `Vaga: ${vaga.titulo} · ${vaga.empresa}`
      : `Habilidades: ${temas.join(", ") || "nenhuma habilidade cadastrada"}`;
    contextoIa.value = vaga
      ? {
          tipo_contexto: "VAGA",
          habilidades: null,
          vaga: {
            titulo: vaga.titulo,
            senioridade: vaga.senioridade,
            requisitos: vaga.requisitos,
          },
        }
      : {
          tipo_contexto: "HABILIDADES",
          habilidades: temas.map((nome) => ({
            nome,
            nivel:
              habilidades.value.find((habilidade) => habilidade.nome === nome)
                ?.nivel ?? 1,
          })),
          vaga: null,
        };

    const primeiraPergunta = await gerarPergunta(
      { ...contextoIa.value, interacoes: [] },
      1,
      totalPerguntas,
    );
    perguntasDoTreino.value = [primeiraPergunta.pergunta.texto];
    treinoAtivo.value = true;
  }

  async function responder(resposta: string) {
    const respostaAtual = resposta.trim();
    if (respostaAtual) respostaPendente.value = respostaAtual;
    if (!respostaPendente.value || !contextoIa.value) {
      throw new Error("Não há uma resposta pendente para enviar.");
    }

    const interacoesAnteriores: InteracaoIa[] = respostas.value.map(
      (respostaRegistrada, indice) => {
        const pergunta = perguntasDoTreino.value[indice];
        if (!pergunta) {
          throw new Error("Não foi possível recuperar uma pergunta do treino.");
        }
        return {
          numero: indice + 1,
          pergunta,
          resposta: respostaRegistrada,
        };
      },
    );
    const perguntaAtualTexto = perguntasDoTreino.value[respostas.value.length];
    if (!perguntaAtualTexto) {
      throw new Error("Não há uma pergunta ativa para receber a resposta.");
    }
    const interacoes = [
      ...interacoesAnteriores,
      {
        numero: respostas.value.length + 1,
        pergunta: perguntaAtualTexto,
        resposta: respostaPendente.value,
      },
    ];
    const contextoAtual: ContextoTreinoIa = {
      ...contextoIa.value,
      interacoes,
    };

    if (interacoes.length === totalPerguntas) {
      avaliacaoPendente.value = await gerarAvaliacao(contextoAtual);
    } else {
      const proximaPergunta = await gerarPergunta(
        contextoAtual,
        interacoes.length + 1,
        totalPerguntas,
      );
      perguntasDoTreino.value.push(proximaPergunta.pergunta.texto);
    }
    respostas.value.push(respostaPendente.value);
    respostaPendente.value = null;
    perguntaAtual.value = respostas.value.length;
  }
  function encerrarTreino() {
    treinoAtivo.value = false;
    vagaEmTreinamento.value = null;
    vagaIdEmTreinamento.value = null;
    contextoIa.value = null;
    respostaPendente.value = null;
    avaliacaoPendente.value = null;
  }
  async function concluirTreino() {
    if (!avaliacaoPendente.value) {
      throw new Error("A avaliação do treino ainda não está disponível.");
    }
    const avaliacao = avaliacaoPendente.value;
    const treino: Treino = {
      titulo: vagaEmTreinamento.value
        ? `Entrevista · ${vagaEmTreinamento.value}`
        : "Entrevista por habilidades",
      contexto: contextoTreino.value,
      data: new Date().toLocaleString("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      }),
      nota: avaliacao.nota,
      duracao: "Sessão atual",
      pontosFortes: avaliacao.pontos_fortes.join(" "),
      melhorias: avaliacao.pontos_melhoria.join(" "),
    };
    const salvo = await salvarTreino({
      tipoContexto: vagaIdEmTreinamento.value ? "VAGA" : "HABILIDADES",
      vagaId: vagaIdEmTreinamento.value,
      contexto: contextoTreino.value,
      habilidades: habilidadesDoTreino.value,
      interacoes: respostas.value.map((resposta, indice) => ({
        numero: indice + 1,
        pergunta: perguntasDoTreino.value[indice],
        resposta,
      })),
    });
    treino.id = salvo.id;
    treino.nota = salvo.nota;
    treino.pontosFortes = salvo.pontosFortes.join(" ");
    treino.melhorias = salvo.pontosMelhoria.join(" ");
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
    avaliacaoPendente,
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
    concluirTreino,
  };
});
