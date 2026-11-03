import { dadosTeste, seletores, textos } from "./constantes.js";

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
