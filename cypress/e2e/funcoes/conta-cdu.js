import {
  alunoAtualizadoCdu,
  instituicaoAtualizadaCdu,
  objetoInstituicaoCdu,
  alunoCdu,
  rotas,
  seletores,
  textos,
  aliases,
  valoresSistema,
} from "./constantes.js";
import { abrirComoAluno, abrirComoInstituicao } from "./apoio.js";
import { prepararAtualizacaoDaConta } from "./funcoes.js";

export function abrirContaDoAlunoVinculado() {
  abrirComoAluno(rotas.conta, {
    conta: { ...alunoCdu, objetoInstituicao: objetoInstituicaoCdu },
  });
}
export function prepararEdicaoDaContaDoAluno() {
  prepararAtualizacaoDaConta(alunoAtualizadoCdu, "salvarAluno");
}
export function abrirContaDaInstituicao() {
  abrirComoInstituicao(rotas.conta);
}
export function prepararEdicaoDaContaDaInstituicao() {
  prepararAtualizacaoDaConta(
    {
      ...instituicaoAtualizadaCdu,
      tipoConta: valoresSistema.conta.instituicao,
      nome: instituicaoAtualizadaCdu.nomeInstituicao,
      cnpj: instituicaoAtualizadaCdu.cnpj.replace(/\D/g, ""),
    },
    "salvarInstituicao",
  );
}
export function conferirInstituicaoDaContaPreservada() {
  cy.get(seletores.conta.instituicao)
    .should("have.value", objetoInstituicaoCdu)
    .and("have.attr", "readonly");
}
export function salvarDadosDaContaEditada() {
  cy.contains("button", textos.conta.salvar).click();
}

export function alterarNomeDaContaAluno() {
  cy.get(seletores.conta.nome).clear().type(alunoAtualizadoCdu.nome);
}

export function alterarEmailDaContaAluno() {
  cy.get(seletores.conta.email).clear().type(alunoAtualizadoCdu.email);
}

export function alterarCpfDaContaAluno() {
  cy.get(seletores.conta.cpf).clear().type(alunoAtualizadoCdu.cpf);
}

export function alterarTelefoneDaContaAluno() {
  cy.get(seletores.conta.telefone).clear().type(alunoAtualizadoCdu.telefone);
}

export function alterarEnderecoDaContaAluno() {
  cy.get(seletores.conta.endereco).clear().type(alunoAtualizadoCdu.endereco);
}

export function conferirContaAlunoAtualizada() {
  cy.wait(aliases.aguardar("salvarAluno"))
    .its("request.body")
    .should("deep.equal", {
      nome: alunoAtualizadoCdu.nome,
      email: alunoAtualizadoCdu.email,
      cpf: alunoAtualizadoCdu.cpf,
      telefone: alunoAtualizadoCdu.telefone,
      endereco: alunoAtualizadoCdu.endereco,
    });
  cy.contains(textos.conta.atualizada).should("be.visible");
  cy.get(seletores.conta.nome).should("have.value", alunoAtualizadoCdu.nome);
}

export function alterarResponsavelDaContaInstituicao() {
  cy.get(seletores.conta.responsavel)
    .clear()
    .type(instituicaoAtualizadaCdu.nomeResponsavel);
}

export function alterarNomeDaContaInstituicao() {
  cy.get(seletores.conta.nome)
    .clear()
    .type(instituicaoAtualizadaCdu.nomeInstituicao);
}

export function alterarEmailDaContaInstituicao() {
  cy.get(seletores.conta.email).clear().type(instituicaoAtualizadaCdu.email);
}

export function alterarCnpjDaContaInstituicao() {
  cy.get(seletores.conta.cnpj).clear().type(instituicaoAtualizadaCdu.cnpj);
}

export function alterarTelefoneDaContaInstituicao() {
  cy.get(seletores.conta.telefone)
    .clear()
    .type(instituicaoAtualizadaCdu.telefone);
}

export function alterarEnderecoDaContaInstituicao() {
  cy.get(seletores.conta.endereco)
    .clear()
    .type(instituicaoAtualizadaCdu.endereco);
}

export function conferirContaInstituicaoAtualizada() {
  cy.wait(aliases.aguardar("salvarInstituicao"))
    .its("request.body")
    .should("deep.equal", {
      nomeResponsavel: instituicaoAtualizadaCdu.nomeResponsavel,
      nomeInstituicao: instituicaoAtualizadaCdu.nomeInstituicao,
      email: instituicaoAtualizadaCdu.email,
      cnpj: instituicaoAtualizadaCdu.cnpj,
      telefone: instituicaoAtualizadaCdu.telefone,
      endereco: instituicaoAtualizadaCdu.endereco,
    });
  cy.contains(textos.conta.atualizada).should("be.visible");
  cy.get(seletores.conta.nome).should(
    "have.value",
    instituicaoAtualizadaCdu.nomeInstituicao,
  );
}
