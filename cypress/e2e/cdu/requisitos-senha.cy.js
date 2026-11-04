import { prepararVisitanteSemSessao } from "../funcoes/funcoes.js";
import { rotas } from "../funcoes/constantes.js";

describe("Requisitos da senha", () => {
  it("mostra em verde cada requisito enquanto a senha é digitada", () => {
    prepararVisitanteSemSessao();
    cy.visit(rotas.cadastroAluno);
    const requisitos = "#requisitos-senha-cadastro";
    cy.get(requisitos).find("li.password-requirement-met").should("have.length", 0);
    cy.get("#senha-cadastro").type("A", { log: false });
    cy.get(requisitos).find("li.password-requirement-met").should("have.length", 1);
    cy.get("#senha-cadastro").type("a1!", { log: false });
    cy.get(requisitos).find("li.password-requirement-met").should("have.length", 4);
    cy.get("#senha-cadastro").type("abcd", { log: false });
    cy.get(requisitos).find("li.password-requirement-met").should("have.length", 5);
    cy.contains(`${requisitos} li`, "Pelo menos 8 caracteres")
      .should("have.css", "color", "rgb(25, 135, 84)");
  });
});
