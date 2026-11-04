export const rotas = {
  apresentacao: "/",
  cadastro: "/cadastro",
  login: "/login",
  inicioAluno: "/inicio",
  instituicao: "/instituicao",
  planos: "/planos",
  treinos: "/treinos",
  historico: "/historico",
  feedback: "/feedback",
  conta: "/conta",
  habilidades: "/habilidades",
  vagas: "/vagas",
  cadastroAluno: "/cadastro?redirect=/inicio",
  cadastroInstituicao: "/cadastro?tipo=instituicao",
  checkoutPadrao: /^\/checkout\//,
  checkout: (planoId) => `/checkout/${planoId}`,
  cadastroAlunoComDestino: (destino) => `/cadastro?redirect=${destino}`,
  cadastroInstituicaoComDestino: (destino) =>
    `/cadastro?tipo=instituicao&redirect=${destino}`,
  cadastroInstituicaoCheckout: (planoId) =>
    `/cadastro?tipo=instituicao&redirect=/checkout/${planoId}`,
};

export const senhasRequisitosCdu = {
  maiuscula: "A",
  maiusculaEMinuscula: "Aa",
  categoriasCompletas: "Aa1!",
  valida: "Aa1!abcd",
  semMaiuscula: "a1!abcd",
};

export const parametrosUrl = {
  busca: "search",
  caminho: "pathname",
  tipo: "tipo",
  redirecionamento: "redirect",
  acessoNecessario: "acesso=necessario",
};

export const metodosHttp = {
  get: "GET",
  post: "POST",
  put: "PUT",
  patch: "PATCH",
  delete: "DELETE",
};

export const codigosHttp = {
  sucesso: 200,
  criado: 201,
  aceito: 202,
  semConteudo: 204,
  naoAutorizado: 401,
};

export const cabecalhosHttp = {
  chaveIdempotencia: "idempotency-key",
};

export const camposApi = {
  treino: {
    tipoContexto: "tipo_contexto",
    tipoContextoConcluido: "tipoContexto",
    numeroPergunta: "numero_pergunta",
    vagaId: "vagaId",
  },
};

export const elementos = {
  ancora: "a",
  botao: "button",
  cabecalho1: "h1",
  cabecalho2: "h2",
  cabecalho3: "h3",
  label: "label",
  input: "input",
  select: "select",
  paragrafo: "p",
  resumo: "summary",
  forte: "strong",
  pequeno: "small",
  celulaTabela: "td",
  corpo: "body",
  detalhes: "details",
};

export const seletores = {
  cadastro: {
    responsavel: "#nome-responsavel",
    nome: "#nome-cadastro",
    email: "#email-cadastro",
    cpf: "#cpf-cadastro",
    cnpj: "#cnpj-cadastro",
    telefone: "#telefone-cadastro",
    endereco: "#endereco-cadastro",
    senha: "#senha-cadastro",
    confirmarSenha: "#confirmar-senha",
    perfil: '[role="radio"]',
  },
  requisitosSenha: {
    lista: "#requisitos-senha-cadastro",
    atendidos: "li.password-requirement-met",
  },
  login: {
    email: "#login-email",
    senha: "#login-senha",
  },
  conta: {
    responsavel: "#nome-responsavel",
    nome: "#nome-conta",
    email: "#email-conta",
    cpf: "#cpf-conta",
    cnpj: "#cnpj-conta",
    telefone: "#telefone-conta",
    endereco: "#endereco-conta",
    instituicao: "#instituicao-conta",
  },
  navegacao: {
    perfilConta: '[aria-label="Editar dados da conta"]',
    sairDaConta: '[aria-label="Sair da conta"]',
    grupoDesenvolvimento: 'summary:contains("Seu desenvolvimento")',
    linksDesenvolvimento: ".nav-section-links a",
    iniciarTreinamento: ".top-menu-text-link",
    radioPerfil: '[role="radio"]',
  },
  habilidade: {
    botaoAdicionar: "button:contains('Adicionar habilidade')",
    dialogo: '[role="dialog"]',
    nome: '[role="dialog"] input[placeholder="Ex.: Vue.js"]',
    categoria: '[role="dialog"] input[placeholder="Ex.: Front-end"]',
    nivel: '[role="dialog"] select',
    cartao: ".skill-card",
    campoNome: 'label:contains("Nome da habilidade") input',
    campoCategoria: 'label:contains("Categoria") input',
    campoNivel: 'label:contains("Nível atual") select',
    botaoEditar: (nome) => `[aria-label="Editar ${nome}"]`,
    botaoRemover: (nome) => `[aria-label="Remover ${nome}"]`,
  },
  vaga: {
    botaoAdicionar: "button:contains('Adicionar vaga')",
    dialogo: '[role="dialog"]',
    titulo:
      '[role="dialog"] input[placeholder="Ex.: Desenvolvedor(a) front-end"]',
    empresa: '[role="dialog"] input[placeholder="Ex.: Nubank"]',
    nivel: '[role="dialog"] select',
    requisitos: '[role="dialog"] input[placeholder="Vue, TypeScript, CSS"]',
    cartao: ".job-card",
    opcaoTreino: ".vacancy-option",
    botaoEditar: (titulo) => `[aria-label="Editar vaga ${titulo}"]`,
    botaoExcluir: (titulo) => `[aria-label="Excluir vaga ${titulo}"]`,
  },
  treino: {
    cartaoContexto: ".context-card",
    modoVaga: 'input[type="radio"][value="vaga"]',
    radioVaga: 'input[type="radio"]',
    resumoContexto: ".training-context-summary",
    progressoPergunta: ".question-progress",
    mensagem: ".chat-thread .chat-message",
    resposta: 'textarea[name="resposta"]',
    finalizar: ".chat-composer-finished button",
    nota: ".score-ring strong",
    metaFeedback: ".feedback-meta",
  },
  historico: {
    linhaTreino: ".history-list-row",
    notaTreino: ".history-result strong",
  },
  instituicao: {
    formularioAluno: ".institution-register-form",
    emailAluno: "#email-aluno",
    removerAluno: (nome) => `button[aria-label="Remover ${nome}"]`,
  },
};

export const classes = {
  confirmacaoCadastro: ".cadastro-success",
  areaInstituicao: ".institution-page",
  cartaoPlano: ".plan-card",
  cartaoHabilidade: ".skill-card",
  cartaoVaga: ".job-card",
  resumoTreino: ".training-context-summary",
  linhaHistorico: ".history-list-row",
  dialogo: '[role="dialog"]',
};

export const textos = {
  requisitosSenha: {
    maiuscula: "Uma letra maiúscula",
    minuscula: "Uma letra minúscula",
    numero: "Um número",
    especial: "Um caractere especial",
    tamanho: "Pelo menos 8 caracteres",
  },
  aluno: "Sou aluno",
  perfilInstituicao: "Sou instituição",
  criarConta: "Criar conta",
  excluir: "Excluir",
  continuar: "Continuar",
  cadastroConcluido: "Conta criada com sucesso",
  tituloCadastro: "Crie sua conta",
  tituloLogin: "Acesse sua conta",
  entrar: "Entrar",
  responsavel: "Responsável",
  alunosParticipantes: "Alunos participantes",
  nenhumAluno: "Ainda não há alunos neste plano.",
  cadastro: {
    titulo: "Crie sua conta",
    botaoCriar: "Criar conta",
    sucesso: "Conta criada com sucesso",
    continuar: "Continuar",
  },
  login: {
    titulo: "Acesse sua conta",
    entrar: "Entrar",
    esqueciSenha: "Esqueci minha senha",
    tituloRecuperacao: "Esqueceu sua senha?",
    enviarInstrucoes: "Enviar instruções",
    voltarLogin: "Voltar para o login",
    senhaRecuperada:
      "Se este e-mail estiver cadastrado, você receberá instruções para recuperar o acesso.",
  },
  conta: {
    tituloAluno: "Editar dados do aluno",
    salvar: "Salvar alterações",
    atualizada: "Dados atualizados.",
    semInstituicao: "Sem instituição vinculada",
  },
  plano: {
    tituloPagina: "Um plano simples para começar a praticar",
    acessoIndividual: "Acesso individual",
    acessoInstitucional: "Acesso institucional",
    continuarPagamento: "Continuar para pagamento simulado",
    contratarInstitucional: "Contratar plano acadêmico",
    criarContaInstitucional: "Criar conta institucional",
    ativarAcesso: "Ativar acesso",
    tituloCheckout: "Confirme seu plano",
    tituloCheckoutInstitucional: "Confirme o plano acadêmico",
    cartaoAprovado: "Preencher cartão aprovado",
    cartaoRecusado: "Preencher cartão recusado",
    simularPagamento: "Simular pagamento",
    aprovado: "Simulação aprovada",
    recusado: "Simulação recusada",
    semCobranca: "Nenhum valor foi cobrado.",
    irParaArea: "Ir para minha área",
    verConfirmacaoEmail: "Ver confirmação de e-mail simulada",
    corpoEmailSimulado: "Esta é uma confirmação simulada.",
    assuntoEmailAtivado: "Plano ativado",
    alunosIncluidos: "10 alunos",
    planoInstitucionalAtivo: "O plano acadêmico está ativo para até 10 alunos",
    tentarNovamente: "Tentar novamente",
    cartaoRecusadoMensagem: "O cartão de teste foi recusado.",
    planoNaoAtivado: "Nenhum valor foi cobrado e o plano não foi ativado.",
    acessoNecessario:
      "Sua conta precisa de um plano ativo ou de um vínculo institucional para abrir a área de treinamento.",
  },
  habilidades: {
    tituloPagina: "Minhas habilidades",
    adicionar: "Adicionar habilidade",
    nomeCampo: "Nome da habilidade",
    categoriaCampo: "Categoria",
    nivelCampo: "Nível atual",
    salvar: "Adicionar",
    nenhuma: "Nenhuma habilidade cadastrada.",
    placeholderNome: "Ex.: Vue.js",
    placeholderCategoria: "Ex.: Front-end",
  },
  vagas: {
    tituloPagina: "Vagas de interesse",
    adicionar: "Adicionar vaga",
    cargoCampo: "Nome do cargo",
    empresaCampo: "Empresa",
    nivelCampo: "Nível",
    requisitosCampo: "Habilidades e requisitos",
    salvar: "Adicionar",
    nenhuma: "Nenhuma vaga encontrada.",
    placeholderTitulo: "Ex.: Desenvolvedor(a) front-end",
    placeholderEmpresa: "Ex.: Nubank",
    placeholderRequisitos: "Vue, TypeScript, CSS",
  },
  treino: {
    iniciar: "Iniciar treinamento",
    tituloPreparacao: "Prepare seu treino",
    modoVaga: "Treinar para uma vaga",
    modoHabilidades: "Treinar com habilidades",
    iniciarEntrevista: "Iniciar entrevista",
    enviarResposta: "Enviar resposta",
    finalizar: "Finalizar e ver feedback",
    tituloFeedback: "Feedback da entrevista",
    historico: "Ver histórico",
    pergunta: (numero) => `PERGUNTA ${numero} DE 3`,
    perguntaIa: (numero) => `Pergunta da entrevista ${numero}`,
    respostaDemonstracao: (numero) => `Resposta de demonstração ${numero}`,
    treinoConcluido: "ENTREVISTA CONCLUÍDA",
  },
  historico: {
    tituloPagina: "Histórico de treinos",
    tituloEntrevistaHabilidades: "Entrevista por habilidades",
    tituloEntrevistaVaga: (titulo) => `Entrevista · ${titulo}`,
    contextoVaga: (titulo, empresa) => `Vaga: ${titulo} · ${empresa}`,
    contextoResumo: (titulo, empresa) => `${titulo} · ${empresa}`,
    contextoHabilidades: "Habilidades: React",
  },
  feedback: {
    titulo: "Feedback da entrevista",
    pontoForte: "Seus pontos fortes",
    melhoria: "Para continuar evoluindo",
  },
  areaAluno: {
    saudacao: (nome) => `Olá, ${nome}!`,
  },
  navegacao: {
    desenvolvimento: "Seu desenvolvimento",
  },
  instituicao: {
    nomeCompleto: "Nome completo",
    responsavel: "Nome do responsável",
    email: "E-mail",
    cpf: "CPF",
    telefone: "Telefone",
    endereco: "Endereço",
    senha: "Senha",
    confirmarSenha: "Confirmar senha",
    cadastrarEVincular: "Cadastrar e vincular",
    vincularAluno: "Vincular aluno",
    mensagemAlunoCriado:
      "Conta criada e aluno vinculado ao plano da instituição.",
    mensagemAlunoVinculado: "Aluno vinculado ao plano da instituição.",
    vagasDisponiveis: "Vagas disponíveis",
    removerVinculo: (nome) => `${nome} foi removido do plano institucional.`,
    tituloAlunos: "Alunos e vagas",
    quantidadeVagas: "10",
    vagasUtilizadas: "1",
    vagasLivresAposCadastro: "9",
  },
};

export const endpoints = {
  csrf: "**/api/v1/sessoes/csrf",
  renovarSessao: "**/api/v1/sessoes/renovacao",
  criarAluno: "**/api/v1/contas/alunos",
  criarInstituicao: "**/api/v1/contas/instituicoes",
  sessao: "**/api/v1/sessoes",
  sessaoAtual: "**/api/v1/sessoes/atual",
  conta: "**/api/v1/conta",
  plano: "**/api/v1/conta/plano",
  planos: "**/api/v1/planos",
  habilidades: "**/api/v1/habilidades",
  vagas: "**/api/v1/vagas",
  treinos: "**/api/v1/treinos",
  treinoEmAndamento: "**/api/v1/treinos/em-andamento",
  alunosInstituicao: "**/api/v1/instituicao/alunos",
  capacidadeInstituicao: "**/api/v1/instituicao/resumo-vagas",
  recuperacaoSenha: "**/api/v1/senhas/recuperacao",
  contratacoesSimuladas: "**/api/v1/contratacoes/simuladas",
  iniciarTreino: "**/api/v1/treinos",
  responderTreino: "**/api/v1/treinos/*/respostas",
  concluirTreino: "**/api/v1/treinos/*/concluir",
  vincularAlunoPorEmail: "**/api/v1/instituicao/vinculos/por-email",
  ofertaCheckout: (id) => `**/api/v1/planos/${id}`,
  habilidade: (id) => `**/api/v1/habilidades/${id}`,
  vaga: (id) => `**/api/v1/vagas/${id}`,
  vinculoInstitucional: (id) => `**/api/v1/instituicao/vinculos/${id}`,
};

export const aliases = {
  consultarCatalogo: "consultarCatalogo",
  consultarOfertaCheckout: "consultarOfertaCheckout",
  consultarPlanoVigente: "consultarPlanoVigente",
  restaurarSessao: "restaurarSessao",
  autenticar: "autenticar",
  autenticarAluno: "autenticarAluno",
  autenticarInstituicao: "autenticarInstituicao",
  encerrarSessao: "encerrarSessao",
  criarAluno: "criarAluno",
  criarInstituicao: "criarInstituicao",
  atualizarContaCaminhoFeliz: "atualizarDadosAlunoCaminhoFeliz",
  criarContaAluno: "criarContaAluno",
  iniciarSessao: "iniciarSessao",
  iniciarSessaoInstituicao: "iniciarSessaoInstituicao",
  atualizarConta: "atualizarConta",
  atualizarContaAlunoCdu: "salvarAluno",
  atualizarContaInstituicaoCdu: "salvarInstituicao",
  criarHabilidade: "criarHabilidade",
  atualizarHabilidade: "atualizarHabilidade",
  removerHabilidade: "removerHabilidade",
  criarVaga: "criarVaga",
  atualizarVaga: "atualizarVaga",
  arquivarVaga: "arquivarVaga",
  solicitarRecuperacao: "solicitarRecuperacao",
  contratarPlanoIndividual: "contratarPlanoIndividual",
  contratarPlanoInstitucional: "contratarPlanoInstitucional",
  recusarPagamento: "recusarPagamento",
  contratarParaAuditoria: "contratarParaAuditoria",
  cadastrarEVincular: "cadastrarEVincular",
  cadastrarEVincularAluno: "cadastrarEVincularAluno",
  removerVinculo: "removerVinculo",
  vincularExistente: "vincularExistente",
  consultarPlanoSemAtivo: "consultarPlanoSemAtivo",
  consultarPlanoAposCompra: "consultarPlanoAposCompra",
  gerarPerguntaIA: "gerarPerguntaIA",
  retomarTreino: "retomarTreino",
  avaliarTreinoIA: "avaliarTreinoIA",
  salvarTreinoConcluido: "salvarTreinoConcluido",
  aguardar: (nome) => `@${nome}`,
};

export const tokensCypress = {
  csrf: "csrf-cypress",
  aluno: "token-aluno-cypress",
  instituicao: "token-instituicao-cypress",
  alunoCaminhoFeliz: "token-aluno-cypress",
};

export const datasCypress = {
  expiracaoTokenSegundos: 300,
  expiracaoContrato: "2026-12-02T00:00:00Z",
  treinoConcluido: "2026-11-02T10:00:00Z",
  alunoVinculado: "2026-11-02T10:00:00Z",
};

export const valoresSistema = {
  conta: {
    aluno: "ALUNO",
    instituicao: "INSTITUICAO",
  },
  modalidade: {
    individual: "INDIVIDUAL",
    institucional: "INSTITUCIONAL",
  },
  plano: {
    ativo: "ATIVO",
    semVigente: "SEM_PLANO_VIGENTE",
    comVigente: "COM_PLANO_VIGENTE",
  },
  pagamento: {
    aprovado: "APROVADO",
    recusado: "RECUSADO",
    encerrado: "ENCERRADO",
    simulado: "SIMULADO",
    ativo: "ATIVO",
  },
  contexto: {
    habilidades: "HABILIDADES",
    vaga: "VAGA",
  },
  perfilCadastro: {
    aluno: "aluno",
    instituicao: "instituicao",
  },
  tipoToken: "Bearer",
  nivel: {
    habilidadeAvancada: "Avançado",
  },
};

export const dadosTeste = {
  senha: "SenhaSeguraE2E123!",
  aluno: {
    nome: "Poppy",
    email: "poppy.teste@gmail.com",
    cpf: "529.982.247-25",
    telefone: "(81) 99999-0000",
    endereco: "Rua Cypress, 10, Recife/PE",
    id: "aluno-cypress-e2e",
  },
  instituicao: {
    nomeResponsavel: "Responsável Cypress",
    nomeInstituicao: "Instituto Cypress de Tecnologia",
    email: "instituicao.cypress@example.test",
    cnpj: "12.345.678/0001-95",
    telefone: "(81) 3333-4444",
    endereco: "Av. Cypress, 20, Recife/PE",
    id: "instituicao-cypress-e2e",
  },
};

export const alunoAtualizadoCaminhoFeliz = {
  ...dadosTeste.aluno,
  nome: "Poppy Atualizada",
  email: "poppy.atualizada@gmail.com",
  cpf: "111.444.777-35",
  telefone: "(81) 98888-2222",
  endereco: "Rua Cypress, 500, Recife/PE",
};

export const dadosCdu = {
  contaAlunoAtualizada: {
    nome: "Poppy Atualizada",
    email: "poppy.atualizada@gmail.com",
    cpf: "111.444.777-35",
    telefone: "(81) 98888-2222",
    endereco: "Rua do Limoeiro, 500, Recife/PE",
  },
  contaInstituicaoAtualizada: {
    nomeInstituicao: "Instituto Atualizado CDU",
    nomeResponsavel: "Responsável Atualizado CDU",
    email: "instituicao.atualizada.cdu@gmail.com",
    cnpj: "11.222.333/0001-81",
    telefone: "(81) 3222-4444",
    endereco: "Av. Atualizada, 600, Recife/PE",
  },
};

export const dadosCaminhoFeliz = {
  totalPerguntas: 3,
  habilidadeMockInicial: {
    id: "habilidade-caminho-feliz",
    nome: "React",
    categoria: "Front-end",
    nivel: 4,
  },
  habilidade: {
    nome: "Vue.js",
    categoria: "Front-end",
    nivel: "4",
  },
  vaga: {
    titulo: "Desenvolvedora Vue",
    empresa: "Empresa Cypress",
    nivel: "Pleno",
    requisitos: "Vue.js, TypeScript, testes automatizados",
  },
  respostasTreino: [
    "Eu começaria entendendo o objetivo da funcionalidade e os critérios de aceitação. Depois dividiria a interface em componentes Vue reutilizáveis, manteria o estado próximo de quem o utiliza e validaria os fluxos com testes automatizados.",
    "Eu avaliaria impacto para a pessoa usuária, risco técnico e dependências. Entregaria primeiro uma solução pequena e observável, acompanharia erros e desempenho e ajustaria com base nos resultados e no retorno da equipe.",
    "Em um projeto de interface, eu alinharia a solução com produto e backend, documentaria as decisões e faria uma revisão com a equipe. Se surgisse um problema em produção, reproduziria o caso, corrigiria a causa e acrescentaria um teste de regressão.",
  ],
  contaAtualizada: {
    telefone: "(81) 98888-2222",
    endereco: "Rua Cypress, 500, Recife/PE",
  },
  cartaoAprovado: {
    numero: "4242 4242 4242 4242",
    validade: "12/30",
    cvv: "123",
  },
};

export const credenciaisCdu = {
  senha: "SenhaSeguraCypress123!",
  csrf: "csrf-cdu-e2e",
};

export const alunoCdu = {
  id: "aluno-cdu-e2e",
  tipoConta: "ALUNO",
  nome: "Poppy",
  email: "poppy.cdu@gmail.com",
  cpf: "529.982.247-25",
  telefone: "(81) 99999-1111",
  endereco: "Rua dos Testes, 100, Recife/PE",
  objetoInstituicao: null,
};

export const instituicaoCdu = {
  id: "instituicao-cdu-e2e",
  tipoConta: "INSTITUICAO",
  nome: "Instituto CDU Cypress",
  nomeResponsavel: "Responsável CDU Cypress",
  email: "instituicao.cdu@example.test",
  cnpj: "04.252.011/0001-10",
  telefone: "(81) 3333-1111",
  endereco: "Av. dos Testes, 200, Recife/PE",
  objetoInstituicao: null,
};

export const planoIndividualCdu = {
  id: "plano-individual-cdu",
  codigo: "IVIS-INDIVIDUAL-CDU",
  modalidade: "INDIVIDUAL",
  nome: "Plano Individual CDU",
  descricao: "Acesso individual para os testes Cypress.",
  precoCentavos: 1990,
  moeda: "BRL",
  duracaoDias: 30,
  vagasIncluidas: 1,
  beneficios: ["Treinamento por habilidades", "Histórico de resultados"],
};

export const planoInstitucionalCdu = {
  id: "plano-institucional-cdu",
  codigo: "IVIS-INSTITUCIONAL-CDU",
  modalidade: "INSTITUCIONAL",
  nome: "Plano Institucional CDU",
  descricao: "Acesso institucional para os testes Cypress.",
  precoCentavos: 9900,
  moeda: "BRL",
  duracaoDias: 30,
  vagasIncluidas: 10,
  beneficios: ["Gestão de alunos", "Dez vagas institucionais"],
};

export const historicoCdu = [
  {
    id: "treino-cdu-01",
    titulo: "Entrevista por habilidades",
    contexto: "Habilidades: React, TypeScript",
    nota: 92,
    data: "2026-10-25T14:30:00Z",
    pontosFortes: ["Explicação objetiva", "Exemplos concretos"],
    pontosMelhoria: ["Aprofundar métricas"],
  },
];

export const habilidadeTesteCdu = {
  id: "habilidade-cdu",
  nome: "React",
  categoria: "Front-end",
  nivel: 4,
};

export const vagaTesteCdu = {
  id: "vaga-cdu",
  titulo: "Desenvolvedora Vue",
  empresa: "Empresa Cypress",
  senioridade: "Pleno",
  requisitos: ["Vue", "TypeScript"],
  descricao: null,
};

export const valoresCdu = {
  tokenAluno: "token-aluno-cdu",
  tokenAlunoLogin: "token-aluno-login-cdu",
  tokenInstituicao: "token-instituicao-cdu",
  contratoInstitucional: "contrato-institucional-cdu",
  contrato: "contrato-cdu-e2e",
  pagamento: "pagamento-cdu-e2e",
  alunoInstitucional: {
    id: "aluno-institucional-cdu",
    nome: "Poppy",
    email: "poppy.nova@gmail.com",
    cpf: "529.982.247-25",
    telefone: "(81) 98888-3333",
    endereco: "Rua Nova, 300, Recife/PE",
  },
  alunoExistente: {
    id: "aluno-existente-cdu",
    nome: "Poppy",
    email: "poppy.existente@gmail.com",
  },
  vinculoAluno: "vinculo-cdu-e2e",
  vinculoAlunoExistente: "vinculo-existente-cdu",
  cpfInstituicaoAtualizada: "11.222.333/0001-81",
  vagaAtualizada: "Desenvolvedora Vue Sênior",
  habilidadeAtualizada: "React avançado",
  pontuacao: 92,
  pontosFortes: ["Resposta clara e estruturada"],
  pontosMelhoria: ["Aprofundar os resultados obtidos"],
  versaoIa: "cypress",
  versaoPrompt: "cypress",
  treinoCaminhoFeliz: "treino-caminho-feliz-cypress",
  tipoToken: "Bearer",
  expiraEmSegundos: 3600,
  respostasEsperadasTreino: 3,
  requisitoVaga: "Vue, TypeScript",
  nivelHabilidade: "Avançado",
  alunoVinculado: {
    tipoConta: "ALUNO",
    objetoInstituicao: null,
  },
  capacidadeInstitucional: {
    total: 10,
    usada: 1,
    disponivel: 9,
  },
  capacidadeInstitucionalVazia: {
    total: 10,
    usada: 0,
    disponivel: 10,
  },
  cartaoAprovado: {
    numero: "4242 4242 4242 4242",
    validade: "12/30",
    cvv: "123",
  },
  cartaoRecusado: {
    numero: "4000 0000 0000 0002",
    validade: "12/30",
    cvv: "123",
  },
  objetoInstituicao: "objeto-instituicao-cdu",
  contratoInstitucionalCaminhoFeliz: "contrato-institucional-cypress",
  vinculoAlunoCaminhoFeliz: "vinculo-aluno-cypress",
  quantidadePerguntasTreino: 3,
};

export const textosCdu = {
  recuperarSenha:
    "Se este e-mail estiver cadastrado, você receberá instruções para recuperar o acesso.",
  cadastroAlunoVinculado:
    "Conta criada e aluno vinculado ao plano da instituição.",
  alunoVinculado: "Aluno vinculado ao plano da instituição.",
  alunoRemovido: (nome) => `${nome} foi removido do plano institucional.`,
  erroSessaoAusente: "Sessão não encontrada.",
  contextoHabilidades: "Habilidades: React",
  treinamentoPorHabilidades: "Treinamento por habilidades",
  historicoDeResultados: "Histórico de resultados",
  gestaoDeAlunos: "Gestão de alunos",
  dezVagasInstitucionais: "Dez vagas institucionais",
  explicacaoObjetiva: "Explicação objetiva",
  exemplosConcretos: "Exemplos concretos",
  aprofundarMetricas: "Aprofundar métricas",
  respostaClara: "Resposta clara e estruturada",
  aprofundarResultados: "Aprofundar os resultados obtidos",
  cadastroContaCriada: "Conta criada com sucesso",
  acessoNecessarioQuery: "acesso=necessario",
  modalidadeInstituicaoQuery: "tipo",
  modalidadeInstituicao: "instituicao",
  redirecionamentoQuery: "redirect",
  erroAcessoSemPlano:
    "Sua conta precisa de um plano ativo ou de um vínculo institucional",
  planoIndividualAtivo: "INDIVIDUAL",
  planoInstitucionalAtivo: "INSTITUCIONAL",
  campoNomeCargo: "Nome do cargo",
  campoNivelVaga: "Nível",
  requisitosVaga: "Habilidades e requisitos",
  campoNivelHabilidade: "Nível atual",
  nomeHabilidade: "Nome da habilidade",
  categoriaHabilidade: "Categoria",
  senhaEsquecida: "Esqueci minha senha",
  tituloRecuperacao: "Esqueceu sua senha?",
  enviarInstrucoes: "Enviar instruções",
  voltarLogin: "Voltar para o login",
  instrucoesRecuperacao:
    "Se este e-mail estiver cadastrado, você receberá instruções para recuperar o acesso.",
  campoResponsavel: "Nome do responsável",
  empresa: "Empresa",
  excluir: "Excluir",
  editarDadosConta: "Salvar alterações",
  pagamentoRecusado: "O cartão de teste foi recusado.",
  tenteNovamente: "Tentar novamente",
  confirmacaoEmailPlano: "Plano ativado",
  corpoEmailPlano: "Esta é uma confirmação simulada.",
  pontoForteExplicacao: "Explicação objetiva",
  pontoForteExemplo: "Exemplos concretos",
  melhoriaMetrica: "Aprofundar métricas",
};

export const objetoInstituicaoCdu = "objeto-instituicao-cdu";
export const alunoAtualizadoCdu = {
  ...alunoCdu,
  objetoInstituicao: objetoInstituicaoCdu,
  nome: "Poppy Atualizada",
  email: "poppy.atualizada@gmail.com",
  cpf: "111.444.777-35",
  telefone: "(81) 98888-2222",
  endereco: "Rua do limoeiro, 500, Recife/PE",
};
export const instituicaoAtualizadaCdu = {
  ...instituicaoCdu,
  nomeInstituicao: "Instituto Atualizado CDU",
  nomeResponsavel: "Responsável Atualizado CDU",
  email: "instituicao.atualizada.cdu@example.test",
  cnpj: "11.222.333/0001-81",
  telefone: "(81) 3222-4444",
  endereco: "Av. Atualizada, 600, Recife/PE",
};
