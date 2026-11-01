export const rotas = {
  cadastro: "/cadastro",
  login: "/login",
  inicioAluno: "/inicio",
  instituicao: "/instituicao",
};

export const seletores = {
  cadastro: {
    administrador: "#nome-administrador",
    nome: "#nome-cadastro",
    email: "#email-cadastro",
    senha: "#senha-cadastro",
    confirmarSenha: "#confirmar-senha",
  },
  login: {
    email: "#login-email",
    senha: "#login-senha",
  },
};

export const classes = {
  confirmacaoCadastro: ".cadastro-success",
  areaInstituicao: ".institution-page",
};

export const textos = {
  aluno: "Sou aluno",
  instituicao: "Sou instituição",
  criarConta: "Criar conta",
  continuar: "Continuar",
  cadastroConcluido: "Conta criada com sucesso",
  tituloCadastro: "Crie sua conta",
  tituloLogin: "Acesse sua conta",
  entrar: "Entrar",
  administrador: "Administrador",
  alunosParticipantes: "Alunos participantes",
  nenhumAluno: "Ainda não há alunos neste plano.",
};

export const endpoints = {
  csrf: "**/api/v1/sessoes/csrf",
  renovarSessao: "**/api/v1/sessoes/renovacao",
  criarAluno: "**/api/v1/contas/alunos",
  criarInstituicao: "**/api/v1/contas/instituicoes",
  sessao: "**/api/v1/sessoes",
  plano: "**/api/v1/conta/plano",
  habilidades: "**/api/v1/habilidades",
  vagas: "**/api/v1/vagas",
  treinos: "**/api/v1/treinos",
  alunosInstituicao: "**/api/v1/instituicao/alunos",
  capacidadeInstituicao: "**/api/v1/instituicao/resumo-vagas",
};

export const dadosTeste = {
  senha: "SenhaSeguraE2E123!",
  aluno: {
    nome: "Aluno Cypress",
    email: "aluno.cypress@example.test",
    id: "aluno-cypress-e2e",
  },
  instituicao: {
    nomeAdministrador: "Administradora Cypress",
    razaoSocial: "Instituto Cypress de Tecnologia",
    email: "instituicao.cypress@example.test",
    id: "instituicao-cypress-e2e",
  },
};
