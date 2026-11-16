import { alunoCdu } from "../funcoes/apoio.js";
import { abrirRedefinicaoDeSenha, validarCodigoDeRecuperacao, informarNovaSenha, confirmarRedefinicao, conferirRedefinicaoConcluida, conferirCodigoInvalido } from "../funcoes/acesso-cdu.js";
import {
  prepararRecuperacaoDeAcesso,
  abrirFormularioDeRecuperacao,
  enviarSolicitacaoDeRecuperacao,
  voltarDoFormularioDeRecuperacao,
} from "../funcoes/cdu.js";

describe("CDU-04 - Solicitar recuperação de acesso", () => {
  it("redefine a senha após validar o código recebido", () => {
    abrirRedefinicaoDeSenha();
    validarCodigoDeRecuperacao();
    informarNovaSenha();
    confirmarRedefinicao();
    conferirRedefinicaoConcluida();
  });
  it("informa quando o código de recuperação é inválido ou expirou", () => {
    abrirRedefinicaoDeSenha({ codigoInvalido: true });
    conferirCodigoInvalido();
  });
  it("apresenta resposta neutra ao solicitar a redefinição da senha", () => {
    prepararRecuperacaoDeAcesso();
    abrirFormularioDeRecuperacao(alunoCdu.email);
    enviarSolicitacaoDeRecuperacao(alunoCdu.email);
    voltarDoFormularioDeRecuperacao();
  });
});
