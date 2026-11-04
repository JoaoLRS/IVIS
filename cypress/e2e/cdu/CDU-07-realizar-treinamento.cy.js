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
  prepararRetomadaDaEntrevista,
  recarregarEntrevista,
  conferirEntrevistaRetomada,
  conferirTerceiraRespostaRegistrada,
  conferirAvaliacaoPersistida,
} from "../funcoes/cdu.js";

describe("CDU-07 - Realizar treinamento e receber feedback", () => {
  it("retoma a pergunta e a resposta registradas após recarregar a página", () => {
    prepararTreinoComIA();
    abrirTreinoPorHabilidades();
    iniciarEntrevistaComIA();
    digitarRespostaDaEntrevista(1);
    enviarRespostaDaEntrevista();
    conferirProximaPerguntaDaEntrevista(2);

    prepararRetomadaDaEntrevista();
    recarregarEntrevista();
    conferirEntrevistaRetomada();
  });

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
    conferirTerceiraRespostaRegistrada();
    conferirAvaliacaoDaEntrevista();
    finalizarEntrevista();
    conferirRespostaPreservadaAposFalha(3, true);
    reenviarRespostaPreservada(true);
    conferirAvaliacaoPersistida();
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
    conferirTerceiraRespostaRegistrada();
    conferirAvaliacaoDaEntrevista();
    finalizarEntrevista();
    conferirAvaliacaoPersistida();
    conferirFeedbackDaEntrevista();
  });
});
