import { alunoCdu } from "../funcoes/apoio.js";
import { abrirRedefinicaoDeSenha, informarNovaSenha, confirmarRedefinicao, conferirRedefinicaoConcluida, conferirLinkExpirado } from "../funcoes/acesso-cdu.js";
import {
  prepararRecuperacaoDeAcesso,
  abrirFormularioDeRecuperacao,
  enviarSolicitacaoDeRecuperacao,
  voltarDoFormularioDeRecuperacao,
} from "../funcoes/cdu.js";

describe("CDU-04 - Solicitar recuperação de acesso", () => {
  it("redefine a senha pelo token recebido e orienta voltar ao login", () => {
    abrirRedefinicaoDeSenha();
    informarNovaSenha();
    confirmarRedefinicao();
    conferirRedefinicaoConcluida();
  });
  it("informa quando o link de redefinição expirou", () => {
    abrirRedefinicaoDeSenha({ expirado: true });
    informarNovaSenha();
    confirmarRedefinicao();
    conferirLinkExpirado();
  });
  it("apresenta resposta neutra ao solicitar a redefinição da senha", () => {
    prepararRecuperacaoDeAcesso();
    abrirFormularioDeRecuperacao(alunoCdu.email);
    enviarSolicitacaoDeRecuperacao(alunoCdu.email);
    voltarDoFormularioDeRecuperacao();
  });
});
