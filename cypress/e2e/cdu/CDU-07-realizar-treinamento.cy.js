import {
  prepararTreinoComIA,
  iniciarEntrevistaComIA,
} from "../funcoes/funcoes.js";
import {
  abrirTreinoPorHabilidades,
  digitarRespostaDaEntrevista,
  enviarRespostaDaEntrevista,
  conferirProximaPerguntaDaEntrevista,
  conferirAvaliacaoDaEntrevista,
  finalizarEntrevista,
  conferirFeedbackDaEntrevista,
  conferirRespostaPreservadaAposFalha,
  reenviarRespostaPreservada,
} from "../funcoes/cdu.js";

describe("CDU-07 - Realizar treinamento e receber feedback", () => {
  it("preserva respostas e permite repetir pergunta e avaliação após falha da IA", () => {
    prepararTreinoComIA(null, { falharUmaVez: true });
    abrirTreinoPorHabilidades();
    iniciarEntrevistaComIA();
    digitarRespostaDaEntrevista(1);
    enviarRespostaDaEntrevista();
    conferirRespostaPreservadaAposFalha(1);
    reenviarRespostaPreservada();
    conferirProximaPerguntaDaEntrevista(2);
    digitarRespostaDaEntrevista(2);
    enviarRespostaDaEntrevista();
    conferirProximaPerguntaDaEntrevista(3);
    digitarRespostaDaEntrevista(3);
    enviarRespostaDaEntrevista();
    conferirRespostaPreservadaAposFalha(3, true);
    reenviarRespostaPreservada();
    conferirAvaliacaoDaEntrevista();
    finalizarEntrevista();
    conferirFeedbackDaEntrevista();
  });
  it("responde às três perguntas da IA, registra o treino e confere a avaliação", () => {
    prepararTreinoComIA();
    abrirTreinoPorHabilidades();
    iniciarEntrevistaComIA();
    digitarRespostaDaEntrevista(1);
    enviarRespostaDaEntrevista();
    conferirProximaPerguntaDaEntrevista(2);
    digitarRespostaDaEntrevista(2);
    enviarRespostaDaEntrevista();
    conferirProximaPerguntaDaEntrevista(3);
    digitarRespostaDaEntrevista(3);
    enviarRespostaDaEntrevista();
    conferirAvaliacaoDaEntrevista();
    finalizarEntrevista();
    conferirFeedbackDaEntrevista();
  });
});
