import {
  prepararTreinoComIA,
  iniciarEntrevistaComIA,
} from "../funcoes/funcoes.js";
import {
  abrirTreinoPorHabilidades,
  conferirContadorInicialDeCaracteres,
  digitarRespostaDaEntrevista,
  enviarRespostaDaEntrevista,
  conferirProximaPerguntaDaEntrevista,
  conferirAvaliacaoDaEntrevista,
  finalizarEntrevista,
  conferirFeedbackDaEntrevista,
  conferirRespostaPreservadaAposFalha,
  reenviarRespostaPreservada,
  conferirTerceiraRespostaRegistrada,
  conferirAvaliacaoPersistida,
} from "../funcoes/cdu.js";

describe("CDU-07 - Realizar treinamento e receber feedback", () => {
  it("atualiza o contador de caracteres enquanto a resposta é digitada", () => {
    prepararTreinoComIA();
    abrirTreinoPorHabilidades();
    iniciarEntrevistaComIA();
    conferirContadorInicialDeCaracteres();
    digitarRespostaDaEntrevista(1);
  });

  it("preserva respostas e permite repetir pergunta e avaliação após falha da IA", () => {
    prepararTreinoComIA(null, { falharUmaVez: true });
    abrirTreinoPorHabilidades();
    iniciarEntrevistaComIA();
    conferirContadorInicialDeCaracteres();
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
    conferirContadorInicialDeCaracteres();
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
