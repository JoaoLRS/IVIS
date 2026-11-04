import { prepararVisitanteSemSessao } from "./funcoes.js";
import { rotas, seletores } from "./constantes.js";

export function abrirCadastroParaConferirSenha() {
  prepararVisitanteSemSessao();
  cy.visit(rotas.cadastroAluno);
}

export function informarSenhaParaConferirRequisitos(senha) {
  cy.get(seletores.cadastro.senha).clear().type(senha, { log: false });
}

export function conferirQuantidadeDeRequisitosAtendidos(quantidade) {
  cy.get(seletores.requisitosSenha.lista)
    .find(seletores.requisitosSenha.atendidos)
    .should("have.length", quantidade);
}

export function conferirRequisitoEmVerde(texto) {
  cy.contains(`${seletores.requisitosSenha.lista} li`, texto)
    .should("have.class", "password-requirement-met")
    .and("have.css", "color", "rgb(25, 135, 84)");
}

export function conferirRequisitoPendente(texto) {
  cy.contains(`${seletores.requisitosSenha.lista} li`, texto)
    .should("not.have.class", "password-requirement-met");
}
