import { prepararVisitante, prepararSessao } from "./apoio.js";
import {
  aliases,
  codigosHttp,
  datasCypress,
  endpoints,
  metodosHttp,
  valoresSistema,
} from "./constantes.js";

export const contasIsolamentoCdu = {
  alunoA: {
    id: "aluno-isolamento-a",
    nome: "Aluna Conta A",
    email: "conta-a@ivis.test",
  },
  alunoB: {
    id: "aluno-isolamento-b",
    nome: "Aluno Conta B",
    email: "conta-b@ivis.test",
  },
};

const historicosIsolamento = {
  "aluno-isolamento-a": [{
    id: "treino-privado-a",
    titulo: "Treino privado da conta A",
    contexto: "Histórico exclusivo da conta A",
    nota: 80,
    data: "2026-11-02T12:00:00Z",
    pontosFortes: [],
    pontosMelhoria: [],
  }],
  "aluno-isolamento-b": [{
    id: "treino-privado-b",
    titulo: "Treino privado da conta B",
    contexto: "Histórico exclusivo da conta B",
    nota: 90,
    data: "2026-11-02T12:00:00Z",
    pontosFortes: [],
    pontosMelhoria: [],
  }],
};

export function prepararAcessoEntreContas() {
  prepararVisitante();
  cy.intercept(metodosHttp.post, endpoints.sessao, (requisicao) => {
    const conta = Object.values(contasIsolamentoCdu).find(
      (candidata) => candidata.email === requisicao.body.email,
    );
    expect(conta, "conta usada no login").to.exist;
    requisicao.reply({
      statusCode: codigosHttp.sucesso,
      body: {
        accessToken: conta.id,
        tipoToken: valoresSistema.tipoToken,
        expiraEmSegundos: datasCypress.expiracaoTokenSegundos,
        conta: { ...conta, tipoConta: valoresSistema.conta.aluno },
      },
    });
  }).as(aliases.autenticar);
  cy.intercept(metodosHttp.get, endpoints.plano, {
    statusCode: codigosHttp.sucesso,
    body: {
      situacao: valoresSistema.plano.comVigente,
      planos: [{ situacao: valoresSistema.plano.ativo, origemAcesso: valoresSistema.modalidade.individual }],
    },
  });
  cy.intercept(metodosHttp.get, endpoints.habilidades, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.vagas, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.treinos, (requisicao) => {
    const token = requisicao.headers.authorization?.replace(/^Bearer\s+/i, "");
    requisicao.reply({
      statusCode: codigosHttp.sucesso,
      body: historicosIsolamento[token] ?? [],
    });
  }).as("historicoPorConta");
  cy.intercept(metodosHttp.delete, endpoints.sessaoAtual, {
    statusCode: codigosHttp.semConteudo,
    body: null,
  }).as(aliases.encerrarSessao);
}

export function abrirRedefinicaoDeSenha({ expirado = false } = {}) {
  prepararVisitante();
  cy.intercept("POST", "**/api/v1/senhas/redefinicao", expirado
    ? { statusCode: 400, body: { mensagem: "O link é inválido ou expirou." } }
    : { statusCode: 204 }).as("redefinirSenha");
  cy.visit("/redefinir-senha?token=token-sintetico");
}
export function informarNovaSenha() {
  cy.get("#nova-senha").type("NovaSenhaSintetica123!", { log: false });
  cy.get("#confirmar-senha").type("NovaSenhaSintetica123!", { log: false });
}
export function confirmarRedefinicao() {
  cy.contains("button", "Redefinir senha").click();
  cy.wait("@redefinirSenha").its("request.body").should("deep.equal", {
    token: "token-sintetico", novaSenha: "NovaSenhaSintetica123!",
  });
}
export function conferirRedefinicaoConcluida() {
  cy.contains('[role="status"]', "Senha redefinida").should("be.visible");
  cy.get("#nova-senha").should("not.exist");
}
export function conferirLinkExpirado() {
  cy.contains('[role="alert"]', "O link é inválido ou expirou.").should("be.visible");
}
export function abrirDetalhesDoPlano({ semPlano = false } = {}) {
  prepararSessao({ planoAtivo: !semPlano });
  cy.intercept("GET", "**/api/v1/conta/plano", { statusCode: 200, body: {
    situacao: semPlano ? "SEM_PLANO_VIGENTE" : "COM_PLANO_VIGENTE",
    planos: semPlano ? [] : [{ contratoId: "contrato-cdu", nomePlano: "Plano Individual IVIS", situacao: "ATIVO",
      origemAcesso: "CONTRATO_PROPRIO", expiraEm: "2026-12-02T12:00:00Z", beneficios: ["Treinamentos técnicos"],
      condicoesTitular: { quantidadeVagas: 1, precoCentavosContratado: 1990, moeda: "BRL" } }],
  }});
  cy.visit("/meu-plano");
}
export function conferirDetalhesDoPlano() {
  cy.get(".plan-access-card").should("be.visible");
  cy.get(".plan-status").should("contain.text", "Vigente");
  cy.get("body").should("have.css", "font-family").and("match", /Roboto Mono/i);
  cy.contains("h2", "Plano Individual IVIS").should("be.visible");
  cy.contains(".plan-detail-tile", "Validade")
    .should("contain.text", "02 de dezembro de 2026");
  cy.contains("Treinamentos técnicos").should("be.visible");
}
export function conferirAusenciaDePlano() {
  cy.get(".plan-message-card--empty").should("be.visible");
  cy.contains("Nenhum plano vigente").should("be.visible");
  cy.contains("a", "Consultar ofertas").should("have.attr", "href", "/planos");
}

export function abrirTrocaDeSenha() {
  prepararSessao();
  cy.intercept("POST", "**/api/v1/conta/senha", { statusCode: 204 }).as("trocarSenha");
  cy.visit("/conta");
  cy.get("#nome-conta")
    .should("have.css", "background-color", "rgb(255, 255, 255)")
    .and("have.css", "border-top-color", "rgb(146, 152, 167)");
  cy.contains("a", "Alterar senha").click();
  cy.location("pathname").should("eq", "/alterar-senha");
  cy.contains("h1", "Alterar senha").should("be.visible");
  cy.get("#senha-atual")
    .should("have.css", "background-color", "rgb(255, 255, 255)")
    .and("have.css", "border-top-color", "rgb(146, 152, 167)");
}
export function informarSenhasDaConta() {
  cy.get("#senha-atual").type("SenhaAtualSintetica123!", { log: false });
  cy.get("#nova-senha-conta").type("NovaSenhaSintetica123!", { log: false });
  cy.get("#confirmar-senha-conta").type("NovaSenhaSintetica123!", { log: false });
}
export function confirmarTrocaDeSenha() {
  cy.contains("button", "Salvar nova senha").click();
  cy.wait("@trocarSenha").its("request.body").should("deep.equal", {
    senhaAtual: "SenhaAtualSintetica123!", novaSenha: "NovaSenhaSintetica123!",
  });
}
export function conferirSenhaAlterada() {
  cy.contains('[role="status"]', "Senha alterada.").should("be.visible");
  cy.get("#senha-atual").should("have.value", "");
  cy.get("#nova-senha-conta").should("have.value", "");
}
