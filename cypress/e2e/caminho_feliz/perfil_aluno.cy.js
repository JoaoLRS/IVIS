import { dadosTeste, rotas, textos } from "../constantes.js";
import {
  cadastrarAluno,
  continuarParaArea,
  preencherCamposComunsCadastro,
  prepararCaminhoFelizPerfilAluno,
  prepararLoginPerfilAluno,
  visitar,
  verificarLoginAutomaticoAluno,
  login,
} from "../funcoes/funcoes.js";

describe("caminho feliz do perfil aluno", () => {
  it("CT-IDENT-01: cadastra o aluno, autentica e abre sua área", () => {
    const aluno = dadosTeste.aluno;
    prepararCaminhoFelizPerfilAluno();
    visitar(`${rotas.cadastro}?redirect=${rotas.inicioAluno}`, textos.tituloCadastro);
    preencherCamposComunsCadastro({ ...aluno, senha: dadosTeste.senha });
    cadastrarAluno(aluno);
    verificarLoginAutomaticoAluno(aluno);
    continuarParaArea(rotas.inicioAluno, `Olá, ${aluno.nome}!`);
  });

  it("CT-LOGIN-ALUNO-01: faz login e abre a área do aluno", () => {
    const aluno = dadosTeste.aluno;
    prepararLoginPerfilAluno();
    visitar(rotas.login, textos.tituloLogin);
    login(aluno.email, dadosTeste.senha, rotas.inicioAluno, `Olá, ${aluno.nome}!`);
  });
});
