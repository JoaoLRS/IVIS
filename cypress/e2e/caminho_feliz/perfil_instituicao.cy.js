import { dadosTeste, rotas, seletores, textos, classes } from "../funcoes/constantes.js";
import { preencherCadastroInstituicao } from "../funcoes/funcoes.js";
import {
  conferirCadastroDoAlunoConcluido,
  continuarDoCadastroParaOfertas,
  preencherCartaoAprovado,
  enviarPagamentoSimulado,
  conferirPagamentoAprovado,
  irParaAreaDoAluno,
} from "../funcoes/caminho-feliz.js";
import {
  cadastrarAlunoDaInstituicao,
  conferirAlunoDaInstituicao,
  removerAlunoDaInstituicao,
} from "../funcoes/instituicao.js";

describe("caminho feliz do perfil instituição", { testIsolation: false, retries: 0 }, () => {
  it("cadastra a conta real da instituição e segue para as ofertas", () => {
    cy.visit(rotas.cadastroInstituicao);
    preencherCadastroInstituicao({ ...dadosTeste.instituicao, senha: dadosTeste.senha });
    cy.contains("button", textos.criarConta).click();
    conferirCadastroDoAlunoConcluido();
    continuarDoCadastroParaOfertas();
    cy.location("pathname").should("eq", rotas.planos);
  });

  it("contrata o plano institucional e confere as vagas disponíveis", () => {
    cy.contains(classes.cartaoPlano, textos.plano.acessoInstitucional)
      .contains("a", textos.plano.contratarInstitucional).click();
    cy.contains("h1", textos.plano.tituloCheckoutInstitucional).should("be.visible");
    preencherCartaoAprovado();
    enviarPagamentoSimulado();
    conferirPagamentoAprovado();
    irParaAreaDoAluno();
    cy.location("pathname").should("eq", rotas.instituicao);
    cy.contains("h1", dadosTeste.instituicao.nomeInstituicao).should("be.visible");
    cy.contains(textos.instituicao.vagasDisponiveis).find("strong")
      .should("have.text", textos.instituicao.quantidadeVagas);
  });

  it("cadastra e vincula um aluno, preservando o vínculo ao recarregar", () => {
    cadastrarAlunoDaInstituicao();
    conferirAlunoDaInstituicao();
    cy.reload();
    conferirAlunoDaInstituicao();
  });

  it("encerra a sessão e autentica o aluno com acesso institucional", () => {
    cy.get(seletores.navegacao.sairDaConta).click();
    cy.location("pathname").should("eq", rotas.login);
    cy.get(seletores.login.email).type(dadosTeste.aluno.email);
    cy.get(seletores.login.senha).type(dadosTeste.senha);
    cy.contains("button", textos.login.entrar).click();
    cy.location("pathname").should("eq", rotas.inicioAluno);
    cy.contains(textos.areaAluno.saudacao(dadosTeste.aluno.nome)).should("be.visible");
  });

  it("retorna à instituição, remove o vínculo e libera a vaga", () => {
    cy.get(seletores.navegacao.sairDaConta).click();
    cy.location("pathname").should("eq", rotas.login);
    cy.get(seletores.login.email).type(dadosTeste.instituicao.email);
    cy.get(seletores.login.senha).type(dadosTeste.senha);
    cy.contains("button", textos.login.entrar).click();
    cy.location("pathname").should("eq", rotas.instituicao);
    removerAlunoDaInstituicao();
    cy.reload();
    cy.contains("td", dadosTeste.aluno.email).should("not.exist");
    cy.contains(textos.instituicao.vagasDisponiveis).find("strong")
      .should("have.text", textos.instituicao.quantidadeVagas);
  });
});
