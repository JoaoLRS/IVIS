import { dadosTeste, rotas, textos } from "../constantes.js";
import {
  cadastrarInstituicao,
  acessarRotaDoPerfil,
  continuarParaArea,
  preencherCadastroInstituicao,
  prepararCaminhoFelizPerfilInstituicao,
  prepararLoginPerfilInstituicao,
  selecionarPerfil,
  visitar,
  verificarAreaInstituicao,
  verificarLoginAutomaticoInstituicao,
  login,
} from "../funcoes/funcoes.js";

describe("caminho feliz do perfil instituição", () => {
  it("CT-CONTA-03: cadastra a instituição, autentica e abre sua área", () => {
    const instituicao = dadosTeste.instituicao;
    prepararCaminhoFelizPerfilInstituicao();
    visitar(
      `${rotas.cadastro}?tipo=instituicao&redirect=${rotas.instituicao}`,
      textos.tituloCadastro,
    );
    selecionarPerfil(textos.instituicao);
    preencherCadastroInstituicao({ ...instituicao, senha: dadosTeste.senha });
    cadastrarInstituicao(instituicao);
    verificarLoginAutomaticoInstituicao(instituicao);
    continuarParaArea(rotas.instituicao, instituicao.razaoSocial);
    verificarAreaInstituicao(instituicao);
  });

  it("CT-IDENT-02: faz login e continua na área da instituição ao acessar uma rota de aluno", () => {
    const instituicao = dadosTeste.instituicao;
    prepararLoginPerfilInstituicao();
    visitar(rotas.login, textos.tituloLogin);
    login(
      instituicao.email,
      dadosTeste.senha,
      rotas.instituicao,
      instituicao.razaoSocial,
    );
    acessarRotaDoPerfil(
      rotas.inicioAluno,
      rotas.instituicao,
      instituicao.razaoSocial,
    );
    verificarAreaInstituicao(instituicao);
  });
});
