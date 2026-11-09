import { classes, dadosTeste, rotas, seletores, textos } from "./constantes.js";

export function visitarCadastroDaInstituicao() {
  cy.visit(rotas.cadastroInstituicao);
}

export function enviarCadastroDaInstituicao() {
  cy.contains("button", textos.criarConta).click();
}

export function conferirOfertasDaInstituicao() {
  cy.location("pathname").should("eq", rotas.planos);
}

export function selecionarOfertaInstitucional() {
  cy.contains(classes.cartaoPlano, textos.plano.acessoInstitucional)
    .contains("a", textos.plano.contratarInstitucional).click();
  cy.contains("h1", textos.plano.tituloCheckoutInstitucional).should("be.visible");
}

export function conferirAreaDaInstituicao() {
  cy.location("pathname").should("eq", rotas.instituicao);
  cy.contains("h1", dadosTeste.instituicao.nomeInstituicao).should("be.visible");
  conferirVagasDisponiveis();
}

export function conferirVagasDisponiveis() {
  cy.contains(textos.instituicao.vagasDisponiveis).find("strong")
    .should("have.text", textos.instituicao.quantidadeVagas);
}

export function recarregarAreaDaInstituicao() {
  cy.reload();
}

export function conferirVinculoRemovido() {
  cy.contains("td", dadosTeste.aluno.email).should("not.exist");
  conferirVagasDisponiveis();
}

export function autenticarNaJornadaDaInstituicao(email, destino) {
  cy.get(seletores.navegacao.sairDaConta).click();
  cy.location("pathname").should("eq", rotas.login);
  cy.get(seletores.login.email).type(email);
  cy.get(seletores.login.senha).type(dadosTeste.senha);
  cy.contains("button", textos.login.entrar).click();
  cy.location("pathname").should("eq", destino);
}

export function conferirSaudacaoDoAlunoVinculado() {
  cy.contains(textos.areaAluno.saudacao(dadosTeste.aluno.nome)).should("be.visible");
}

export function cadastrarAlunoDaInstituicao() {
  const aluno = dadosTeste.aluno;
  const campos = [
    [textos.instituicao.nomeCompleto, aluno.nome],
    [textos.instituicao.email, aluno.email],
    [textos.instituicao.cpf, aluno.cpf],
    [textos.instituicao.telefone, aluno.telefone],
    [textos.instituicao.endereco, aluno.endereco],
    [textos.instituicao.senha, dadosTeste.senha],
    [textos.instituicao.confirmarSenha, dadosTeste.senha],
  ];
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    campos.forEach(([rotulo, valor]) => {
      cy.contains("label", rotulo).find("input").type(valor);
    });
    cy.contains("button", textos.instituicao.cadastrarEVincular).click();
  });
  cy.contains(textos.instituicao.mensagemAlunoCriado).should("be.visible");
}

export function conferirAlunoDaInstituicao() {
  cy.contains("tr", dadosTeste.aluno.email).should("contain", dadosTeste.aluno.nome);
  cy.contains(textos.instituicao.vagasDisponiveis).find("strong")
    .should("have.text", textos.instituicao.vagasLivresAposCadastro);
}

export function removerAlunoDaInstituicao() {
  cy.get(seletores.instituicao.removerAluno(dadosTeste.aluno.nome)).click();
  cy.contains(textos.instituicao.removerVinculo(dadosTeste.aluno.nome)).should("be.visible");
  cy.contains("td", dadosTeste.aluno.email).should("not.exist");
}
