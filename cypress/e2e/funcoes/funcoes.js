import {
  classes,
  dadosTeste,
  endpoints,
  rotas,
  seletores,
  textos,
} from "../constantes.js";

export function prepararVisitanteSemSessao() {
  cy.intercept("GET", endpoints.csrf, {
    statusCode: 200,
    body: { token: "csrf-cypress" },
  });
  cy.intercept("POST", endpoints.renovarSessao, {
    statusCode: 401,
    body: { mensagem: "Sessão não encontrada." },
  });
}

export function prepararPlanoAtivo(origemAcesso) {
  cy.intercept("GET", endpoints.plano, {
    statusCode: 200,
    body: {
      situacao: "COM_PLANO_VIGENTE",
      planos: [{ situacao: "ATIVO", origemAcesso }],
    },
  });
}

export function preencherCamposComunsCadastro({ nome, email, senha }) {
  preencherCampo(seletores.cadastro.nome, nome);
  preencherCampo(seletores.cadastro.email, email);
  preencherCampo(seletores.cadastro.senha, senha);
  preencherCampo(seletores.cadastro.confirmarSenha, senha);
}

export function preencherCadastroInstituicao({
  nomeAdministrador,
  razaoSocial,
  email,
  senha,
}) {
  preencherCampo(seletores.cadastro.administrador, nomeAdministrador);
  preencherCamposComunsCadastro({ nome: razaoSocial, email, senha });
}

export function visitar(url, textoEsperado) {
  cy.visit(url);
  cy.contains(textoEsperado).should("be.visible");
}

export function preencherCampo(seletor, valor) {
  cy.get(seletor).should("be.visible").type(valor).should("have.value", valor);
}

export function selecionarPerfil(textoPerfil) {
  cy.contains('[role="radio"]', textoPerfil).click();
  cy.contains('[role="radio"]', textoPerfil)
    .should("have.attr", "aria-checked", "true");
}

export function enviarCadastro(aliasCadastro, dadosEnviados) {
  cy.contains("button", textos.criarConta).should("be.enabled").click();
  cy.wait(aliasCadastro).its("request.body").should("deep.equal", dadosEnviados);
  cy.get(classes.confirmacaoCadastro)
    .contains("h2", textos.cadastroConcluido)
    .should("be.visible");
}

export function verificarAutenticacaoAutomatica(aliasSessao, credenciais) {
  cy.wait(aliasSessao).its("request.body").should("deep.equal", credenciais);
}

export function continuarParaArea(urlEsperada, textoEsperado) {
  cy.contains("button", textos.continuar).should("be.visible").click();
  cy.location("pathname").should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function acessarRotaDoPerfil(urlAcessada, urlEsperada, textoEsperado) {
  cy.visit(urlAcessada);
  cy.location("pathname").should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function login(email, senha, urlEsperada, textoEsperado) {
  preencherCampo(seletores.login.email, email);
  preencherCampo(seletores.login.senha, senha);
  cy.contains("button", textos.entrar).should("be.enabled").click();
  cy.wait("@autenticar").its("request.body").should("deep.equal", {
    email,
    senha,
  });
  cy.location("pathname").should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function prepararPerfilAlunoVazio() {
  cy.intercept("GET", endpoints.habilidades, { statusCode: 200, body: [] });
  cy.intercept("GET", endpoints.vagas, { statusCode: 200, body: [] });
  cy.intercept("GET", endpoints.treinos, { statusCode: 200, body: [] });
}

export function prepararInstituicaoSemAlunos() {
  cy.intercept("GET", endpoints.alunosInstituicao, {
    statusCode: 200,
    body: [],
  });
  cy.intercept("GET", endpoints.capacidadeInstituicao, {
    statusCode: 200,
    body: [
      {
        contratoId: "contrato-institucional-cypress",
        quantidadeVagas: 10,
        vagasUtilizadas: 0,
        vagasDisponiveis: 10,
      },
    ],
  });
}

export function prepararLoginPerfilAluno() {
  const aluno = dadosTeste.aluno;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo("INDIVIDUAL");
  prepararPerfilAlunoVazio();
  cy.intercept("POST", endpoints.sessao, {
    statusCode: 200,
    body: {
      accessToken: "token-aluno-cypress",
      tipoToken: "Bearer",
      expiraEmSegundos: 300,
      conta: {
        id: aluno.id,
        nome: aluno.nome,
        email: aluno.email,
        tipoConta: "ALUNO",
      },
    },
  }).as("autenticar");
}

export function prepararLoginPerfilInstituicao() {
  const instituicao = dadosTeste.instituicao;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo("INSTITUCIONAL");
  prepararInstituicaoSemAlunos();
  cy.intercept("POST", endpoints.sessao, {
    statusCode: 200,
    body: {
      accessToken: "token-instituicao-cypress",
      tipoToken: "Bearer",
      expiraEmSegundos: 300,
      conta: {
        id: instituicao.id,
        nome: instituicao.razaoSocial,
        nomeAdministrador: instituicao.nomeAdministrador,
        email: instituicao.email,
        tipoConta: "INSTITUICAO",
      },
    },
  }).as("autenticar");
}

export function prepararCaminhoFelizPerfilAluno() {
  const aluno = dadosTeste.aluno;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo("INDIVIDUAL");
  prepararPerfilAlunoVazio();

  cy.intercept("POST", endpoints.criarAluno, {
    statusCode: 201,
    body: {
      id: aluno.id,
      nome: aluno.nome,
      email: aluno.email,
      tipo_conta: "ALUNO",
    },
  }).as("criarAluno");
  cy.intercept("POST", endpoints.sessao, {
    statusCode: 200,
    body: {
      accessToken: "token-aluno-cypress",
      tipoToken: "Bearer",
      expiraEmSegundos: 300,
      conta: {
        id: aluno.id,
        nome: aluno.nome,
        email: aluno.email,
        tipoConta: "ALUNO",
      },
    },
  }).as("autenticarAluno");
}

export function cadastrarAluno(aluno) {
  enviarCadastro("@criarAluno", {
    nome: aluno.nome,
    email: aluno.email,
    senha: dadosTeste.senha,
  });
}

export function verificarLoginAutomaticoAluno(aluno) {
  verificarAutenticacaoAutomatica("@autenticarAluno", {
    email: aluno.email,
    senha: dadosTeste.senha,
  });
}

export function prepararCaminhoFelizPerfilInstituicao() {
  const instituicao = dadosTeste.instituicao;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo("INSTITUCIONAL");
  prepararInstituicaoSemAlunos();

  cy.intercept("POST", endpoints.criarInstituicao, {
    statusCode: 201,
    body: {
      id: instituicao.id,
      nome: instituicao.razaoSocial,
      nomeAdministrador: instituicao.nomeAdministrador,
      email: instituicao.email,
      tipo_conta: "INSTITUICAO",
    },
  }).as("criarInstituicao");
  cy.intercept("POST", endpoints.sessao, {
    statusCode: 200,
    body: {
      accessToken: "token-instituicao-cypress",
      tipoToken: "Bearer",
      expiraEmSegundos: 300,
      conta: {
        id: instituicao.id,
        nome: instituicao.razaoSocial,
        nomeAdministrador: instituicao.nomeAdministrador,
        email: instituicao.email,
        tipoConta: "INSTITUICAO",
      },
    },
  }).as("autenticarInstituicao");
}

export function cadastrarInstituicao(instituicao) {
  enviarCadastro("@criarInstituicao", {
    nomeAdministrador: instituicao.nomeAdministrador,
    razaoSocial: instituicao.razaoSocial,
    email: instituicao.email,
    senha: dadosTeste.senha,
  });
}

export function verificarLoginAutomaticoInstituicao(instituicao) {
  verificarAutenticacaoAutomatica("@autenticarInstituicao", {
    email: instituicao.email,
    senha: dadosTeste.senha,
  });
}

export function verificarAreaInstituicao(instituicao) {
  cy.location("pathname").should("eq", rotas.instituicao);
  cy.get(classes.areaInstituicao)
    .contains("h1", instituicao.razaoSocial)
    .should("be.visible");
  cy.contains(textos.administrador).should("be.visible");
  cy.contains("strong", instituicao.nomeAdministrador).should("be.visible");
  cy.contains("h2", textos.alunosParticipantes).should("be.visible");
  cy.contains(textos.nenhumAluno).should("be.visible");
}
