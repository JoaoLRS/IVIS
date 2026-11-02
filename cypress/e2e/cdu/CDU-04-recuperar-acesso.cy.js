import { alunoCdu } from "../funcoes/apoio.js";
import {
  prepararRecuperacaoDeAcesso,
  abrirFormularioDeRecuperacao,
  enviarSolicitacaoDeRecuperacao,
  voltarDoFormularioDeRecuperacao,
} from "../funcoes/cdu.js";

describe("CDU-04 - Solicitar recuperação de acesso", () => {
  it("apresenta resposta neutra ao solicitar a redefinição da senha", () => {
    prepararRecuperacaoDeAcesso();
    abrirFormularioDeRecuperacao(alunoCdu.email);
    enviarSolicitacaoDeRecuperacao(alunoCdu.email);
    voltarDoFormularioDeRecuperacao();
  });
});
