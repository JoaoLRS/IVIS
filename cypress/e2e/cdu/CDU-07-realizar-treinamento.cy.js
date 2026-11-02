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
} from "../funcoes/cdu.js";

describe("CDU-07 - Realizar treinamento e receber feedback", () => {
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
