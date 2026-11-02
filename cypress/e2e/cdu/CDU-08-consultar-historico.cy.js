import { rotas } from "../funcoes/constantes.js";
import { historicoCdu, abrirComoAluno } from "../funcoes/apoio.js";
import {
  conferirTreinoListadoNoHistorico,
  abrirDetalhesDoTreinoNoHistorico,
  conferirDetalhesDoTreinoNoHistorico,
} from "../funcoes/cdu.js";

describe("CDU-08 - Consultar histórico de treinos", () => {
  it("lista treinos concluídos e abre os pontos fortes e de melhoria", () => {
    abrirComoAluno(rotas.historico, { historico: historicoCdu });
    conferirTreinoListadoNoHistorico();
    abrirDetalhesDoTreinoNoHistorico();
    conferirDetalhesDoTreinoNoHistorico();
  });
});
