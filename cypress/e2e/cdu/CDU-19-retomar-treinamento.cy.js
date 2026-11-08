import {
  prepararRetomadaDaEntrevista,
  abrirEntrevistaEmAndamento,
  conferirEntrevistaRetomada,
  recarregarEntrevista,
  digitarRespostaDaEntrevista,
  enviarRespostaDaEntrevista,
  conferirProximaPerguntaDaEntrevista,
  conferirTerceiraRespostaRegistrada,
  conferirAvaliacaoDaEntrevista,
  finalizarEntrevista,
  conferirAvaliacaoPersistida,
  conferirFeedbackDaEntrevista,
  conferirRetomadaSemNovoTreino,
} from "../funcoes/cdu.js";

describe("CDU-19 - Retomar treinamento em andamento", () => {
  it("recupera as respostas após reabrir e recarregar a página e conclui o treino existente", () => {
    prepararRetomadaDaEntrevista();
    abrirEntrevistaEmAndamento();
    conferirEntrevistaRetomada();
    recarregarEntrevista();
    conferirEntrevistaRetomada();
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
    conferirRetomadaSemNovoTreino();
  });
});
