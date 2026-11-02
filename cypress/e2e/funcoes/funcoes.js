import {
  aliases,
  alunoAtualizadoCaminhoFeliz,
  cabecalhosHttp,
  camposApi,
  classes,
  codigosHttp,
  dadosCaminhoFeliz,
  dadosTeste,
  datasCypress,
  elementos,
  endpoints,
  metodosHttp,
  parametrosUrl,
  rotas,
  seletores,
  textos,
  textosCdu,
  tokensCypress,
  valoresCdu,
  valoresSistema,
} from "./constantes.js";
import {
  abrirComoAluno,
  planoIndividualCdu,
  prepararCatalogo,
  prepararCheckout,
  respostaContratacao,
} from "./apoio.js";

export function prepararVisitanteSemSessao() {
  cy.intercept(metodosHttp.get, endpoints.csrf, {
    statusCode: codigosHttp.sucesso,
    body: { token: tokensCypress.csrf },
  });
  cy.intercept(metodosHttp.post, endpoints.renovarSessao, {
    statusCode: codigosHttp.naoAutorizado,
    body: { mensagem: textosCdu.erroSessaoAusente },
  });
}

export function prepararPlanoAtivo(origemAcesso) {
  cy.intercept(metodosHttp.get, endpoints.plano, {
    statusCode: codigosHttp.sucesso,
    body: {
      situacao: valoresSistema.plano.comVigente,
      planos: [{ situacao: valoresSistema.plano.ativo, origemAcesso }],
    },
  });
}

export function confirmarPlanoIndividualAtivo() {
  cy.wait(aliases.aguardar(aliases.consultarPlanoVigente)).its("response.body").should("deep.equal", {
    situacao: valoresSistema.plano.comVigente,
    planos: [{ situacao: valoresSistema.plano.ativo, origemAcesso: valoresSistema.modalidade.individual }],
  });
  cy.location(parametrosUrl.caminho).should("eq", rotas.inicioAluno);
}

export function preencherCamposComunsCadastro({ nome, email, senha, cpf, cnpj, telefone, endereco }) {
  preencherCampo(seletores.cadastro.nome, nome);
  preencherCampo(seletores.cadastro.email, email);
  if (cpf) preencherCampo(seletores.cadastro.cpf, cpf);
  if (cnpj) preencherCampo(seletores.cadastro.cnpj, cnpj);
  preencherCampo(seletores.cadastro.telefone, telefone);
  preencherCampo(seletores.cadastro.endereco, endereco);
  preencherCampo(seletores.cadastro.senha, senha);
  preencherCampo(seletores.cadastro.confirmarSenha, senha);
}

export function preencherCadastroInstituicao({
  nomeResponsavel,
  nomeInstituicao,
  email,
  senha,
  cnpj,
  telefone,
  endereco,
}) {
  preencherCampo(seletores.cadastro.responsavel, nomeResponsavel);
  preencherCamposComunsCadastro({ nome: nomeInstituicao, email, senha, cnpj, telefone, endereco });
}

export function visitar(url, textoEsperado) {
  cy.visit(url);
  cy.contains(textoEsperado).should("be.visible");
}

export function preencherCampo(seletor, valor) {
  cy.get(seletor).should("be.visible").type(valor).should("have.value", valor);
}

export function selecionarPerfil(textoPerfil) {
  cy.contains(seletores.navegacao.radioPerfil, textoPerfil).click();
  cy.contains(seletores.navegacao.radioPerfil, textoPerfil)
    .should("have.attr", "aria-checked", "true");
}

export function enviarCadastro(aliasCadastro, dadosEnviados) {
  cy.contains(elementos.botao, textos.criarConta).should("be.enabled").click();
  cy.wait(aliasCadastro).its("request.body").should("deep.equal", dadosEnviados);
  cy.get(classes.confirmacaoCadastro)
    .contains(elementos.cabecalho2, textos.cadastroConcluido)
    .should("be.visible");
}

export function verificarAutenticacaoAutomatica(aliasSessao, credenciais) {
  cy.wait(aliasSessao).its("request.body").should("deep.equal", credenciais);
}

export function continuarParaArea(urlEsperada, textoEsperado) {
  cy.contains(elementos.botao, textos.continuar).should("be.visible").click();
  cy.location(parametrosUrl.caminho).should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function acessarRotaDoPerfil(urlAcessada, urlEsperada, textoEsperado) {
  cy.visit(urlAcessada);
  cy.location(parametrosUrl.caminho).should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function login(email, senha, urlEsperada, textoEsperado) {
  preencherCampo(seletores.login.email, email);
  preencherCampo(seletores.login.senha, senha);
  cy.contains(elementos.botao, textos.entrar).should("be.enabled").click();
  cy.wait(aliases.aguardar(aliases.autenticar)).its("request.body").should("deep.equal", {
    email,
    senha,
  });
  cy.location(parametrosUrl.caminho).should("eq", urlEsperada);
  cy.contains(textoEsperado).should("be.visible");
}

export function prepararLogout() {
  cy.intercept(metodosHttp.delete, endpoints.sessaoAtual, {
    statusCode: codigosHttp.semConteudo,
    body: null,
  }).as(aliases.encerrarSessao);
}

export function sairDaConta() {
  cy.get(seletores.navegacao.sairDaConta).click();
  cy.wait(aliases.aguardar(aliases.encerrarSessao));
  cy.location(parametrosUrl.caminho).should("eq", rotas.login);
}

export function prepararAtualizacaoDaConta(resposta, alias = aliases.atualizarConta) {
  cy.intercept(metodosHttp.patch, endpoints.conta, {
    statusCode: codigosHttp.sucesso,
    body: resposta,
  }).as(alias);
}

export function preencherDadosPessoaisDoAluno(conta, objetoInstituicao = null) {
  cy.get(seletores.conta.nome).clear().type(conta.nome);
  cy.get(seletores.conta.email).clear().type(conta.email);
  cy.get(seletores.conta.cpf).clear().type(conta.cpf);
  cy.get(seletores.conta.telefone).clear().type(conta.telefone);
  cy.get(seletores.conta.endereco).clear().type(conta.endereco);
  cy.get(seletores.conta.instituicao)
    .should("have.value", objetoInstituicao ?? textos.conta.semInstituicao)
    .and("have.attr", "readonly");
}

export function atualizarDadosDoAlunoNoCaminhoFeliz() {
  prepararAtualizacaoDaConta(
    { ...alunoAtualizadoCaminhoFeliz, tipoConta: valoresSistema.conta.aluno, objetoInstituicao: null },
    aliases.atualizarContaCaminhoFeliz,
  );
  preencherDadosPessoaisDoAluno(alunoAtualizadoCaminhoFeliz);
  salvarAlteracoesDaConta(
    aliases.atualizarContaCaminhoFeliz,
    {
      email: alunoAtualizadoCaminhoFeliz.email,
      telefone: alunoAtualizadoCaminhoFeliz.telefone,
      endereco: alunoAtualizadoCaminhoFeliz.endereco,
      nome: alunoAtualizadoCaminhoFeliz.nome,
      cpf: alunoAtualizadoCaminhoFeliz.cpf,
    },
    alunoAtualizadoCaminhoFeliz.nome,
  );
}

export function preencherDadosDaInstituicao(conta) {
  cy.get(seletores.conta.responsavel).clear().type(conta.nomeResponsavel);
  cy.get(seletores.conta.nome).clear().type(conta.nomeInstituicao);
  cy.get(seletores.conta.email).clear().type(conta.email);
  cy.get(seletores.conta.cnpj).clear().type(conta.cnpj);
  cy.get(seletores.conta.telefone).clear().type(conta.telefone);
  cy.get(seletores.conta.endereco).clear().type(conta.endereco);
}

export function salvarAlteracoesDaConta(alias, dadosEnviados, nomeEsperado) {
  cy.contains(elementos.botao, textos.conta.salvar).click();
  cy.wait(aliases.aguardar(alias)).its("request.body").should("deep.equal", dadosEnviados);
  cy.contains(textos.conta.atualizada).should("be.visible");
  cy.get(seletores.conta.nome).should("have.value", nomeEsperado);
}

export function prepararPerfilAlunoVazio(habilidades = []) {
  cy.intercept(metodosHttp.get, endpoints.habilidades, { statusCode: codigosHttp.sucesso, body: habilidades });
  cy.intercept(metodosHttp.get, endpoints.vagas, { statusCode: codigosHttp.sucesso, body: [] });
  cy.intercept(metodosHttp.get, endpoints.treinos, { statusCode: codigosHttp.sucesso, body: [] });
}

export function prepararInstituicaoSemAlunos() {
  cy.intercept(metodosHttp.get, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.capacidadeInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [
      {
        contratoId: valoresCdu.contratoInstitucional,
        quantidadeVagas: valoresCdu.capacidadeInstitucionalVazia.total,
        vagasUtilizadas: valoresCdu.capacidadeInstitucionalVazia.usada,
        vagasDisponiveis: valoresCdu.capacidadeInstitucionalVazia.disponivel,
      },
    ],
  });
}

export function prepararLoginPerfilAluno() {
  const aluno = dadosTeste.aluno;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo(valoresSistema.modalidade.individual);
  prepararPerfilAlunoVazio();
  cy.intercept(metodosHttp.post, endpoints.sessao, {
    statusCode: codigosHttp.sucesso,
    body: {
      accessToken: tokensCypress.aluno,
      tipoToken: valoresSistema.tipoToken,
      expiraEmSegundos: datasCypress.expiracaoTokenSegundos,
      conta: {
        id: aluno.id,
        nome: aluno.nome,
        email: aluno.email,
        tipoConta: valoresSistema.conta.aluno,
      },
    },
  }).as(aliases.autenticar);
}

export function prepararLoginPerfilInstituicao() {
  const instituicao = dadosTeste.instituicao;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo(valoresSistema.modalidade.institucional);
  prepararInstituicaoSemAlunos();
  cy.intercept(metodosHttp.post, endpoints.sessao, {
    statusCode: codigosHttp.sucesso,
    body: {
      accessToken: tokensCypress.instituicao,
      tipoToken: valoresSistema.tipoToken,
      expiraEmSegundos: datasCypress.expiracaoTokenSegundos,
      conta: {
        id: instituicao.id,
        nome: instituicao.nomeInstituicao,
        nomeResponsavel: instituicao.nomeResponsavel,
        email: instituicao.email,
        cnpj: instituicao.cnpj.replace(/\D/g, ""),
        telefone: instituicao.telefone,
        endereco: instituicao.endereco,
        tipoConta: valoresSistema.conta.instituicao,
      },
    },
  }).as(aliases.autenticar);
}

export function prepararCaminhoFelizPerfilAluno() {
  const aluno = dadosTeste.aluno;
  prepararVisitanteSemSessao();
  cy.intercept(metodosHttp.get, endpoints.plano, {
    statusCode: codigosHttp.sucesso,
    body: { situacao: valoresSistema.plano.semVigente, planos: [] },
  }).as(aliases.consultarPlanoSemAtivo);
  prepararPerfilAlunoVazio([
    dadosCaminhoFeliz.habilidadeMockInicial,
  ]);
  prepararCatalogo();

  cy.intercept(metodosHttp.post, endpoints.criarAluno, {
    statusCode: codigosHttp.criado,
    body: {
      id: aluno.id,
      nome: aluno.nome,
      email: aluno.email,
      cpf: aluno.cpf,
      telefone: aluno.telefone,
      endereco: aluno.endereco,
      tipo_conta: valoresSistema.conta.aluno,
    },
  }).as(aliases.criarAluno);
  cy.intercept(metodosHttp.post, endpoints.sessao, {
    statusCode: codigosHttp.sucesso,
    body: {
      accessToken: tokensCypress.aluno,
      tipoToken: valoresSistema.tipoToken,
      expiraEmSegundos: datasCypress.expiracaoTokenSegundos,
      conta: {
        id: aluno.id,
        nome: aluno.nome,
        email: aluno.email,
        cpf: aluno.cpf.replace(/\D/g, ""),
        telefone: aluno.telefone,
        endereco: aluno.endereco,
        tipoConta: valoresSistema.conta.aluno,
      },
    },
  }).as(aliases.autenticarAluno);
}

export function selecionarPlanoIndividual() {
  cy.contains(classes.cartaoPlano, planoIndividualCdu.nome)
    .contains(elementos.ancora, textos.plano.continuarPagamento)
    .click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.checkout(planoIndividualCdu.id));
}

export function prepararCheckoutPlanoIndividual({ atualizarPlanoAposCompra = true } = {}) {
  let planoAtivo = false;
  prepararCheckout(planoIndividualCdu);
  if (atualizarPlanoAposCompra) {
    cy.intercept(metodosHttp.get, endpoints.plano, (requisicao) => {
      requisicao.reply({
        statusCode: codigosHttp.sucesso,
        body: planoAtivo
          ? {
              situacao: valoresSistema.plano.comVigente,
              planos: [{ situacao: valoresSistema.plano.ativo, origemAcesso: valoresSistema.modalidade.individual }],
            }
          : { situacao: valoresSistema.plano.semVigente, planos: [] },
      });
    }).as(aliases.consultarPlanoAposCompra);
  }
  cy.intercept(metodosHttp.post, endpoints.contratacoesSimuladas, (requisicao) => {
    if (atualizarPlanoAposCompra) planoAtivo = true;
    requisicao.reply({
      statusCode: codigosHttp.criado,
      body: respostaContratacao({
        plano: planoIndividualCdu,
        emailConfirmacao: {
          destinatario: dadosTeste.aluno.email,
          assunto: textos.plano.assuntoEmailAtivado,
          corpo: textos.plano.corpoEmailSimulado,
        },
      }),
    });
  }).as(aliases.contratarPlanoIndividual);
}

export function simularPagamentoAprovado(plano = planoIndividualCdu) {
  cy.wait(aliases.aguardar(aliases.consultarOfertaCheckout));
  cy.contains(elementos.cabecalho1, textos.plano.tituloCheckout).should("be.visible");
  cy.contains(elementos.botao, textos.plano.cartaoAprovado).click();
  cy.contains(elementos.botao, textos.plano.simularPagamento).click();
  cy.wait(aliases.aguardar(aliases.contratarPlanoIndividual)).then(({ request, response }) => {
    expect(request.body).to.deep.equal({
      planoId: plano.id,
      numeroCartao: dadosCaminhoFeliz.cartaoAprovado.numero,
      validade: dadosCaminhoFeliz.cartaoAprovado.validade,
      cvv: dadosCaminhoFeliz.cartaoAprovado.cvv,
    });
    expect(request.headers[cabecalhosHttp.chaveIdempotencia]).to.be.a("string").and.not.be.empty;
    expect(response.statusCode).to.eq(201);
    expect(response.body.situacaoPagamento).to.eq(valoresSistema.pagamento.aprovado);
  });
  cy.contains(elementos.cabecalho2, textos.plano.aprovado).should("be.visible");
  cy.contains(textos.plano.semCobranca).should("be.visible");
  cy.contains(elementos.resumo, textos.plano.verConfirmacaoEmail).click();
  cy.contains(textos.plano.corpoEmailSimulado).should("be.visible");
}

export function irParaMinhaAreaAposCompra(aluno = dadosTeste.aluno) {
  cy.contains(elementos.ancora, textos.plano.irParaArea).click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.inicioAluno);
  cy.contains(textos.areaAluno.saudacao(aluno.nome)).should("be.visible");
}

export function irParaTreinoPelaAreaDoAluno() {
  cy.contains(elementos.ancora, textos.treino.iniciar).click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.treinos);
  cy.contains(elementos.cabecalho1, textos.treino.tituloPreparacao).should("be.visible");
}

export function selecionarVagaParaTreino(vaga) {
  cy.contains(seletores.vaga.opcaoTreino, vaga.titulo).click();
  cy.contains(classes.resumoTreino, textos.historico.contextoVaga(vaga.titulo, vaga.empresa))
    .should("be.visible");
}

export function prepararTreinoComIA(vaga = null) {
  const tituloTreino = vaga
    ? textos.historico.tituloEntrevistaVaga(vaga.titulo)
    : textos.historico.tituloEntrevistaHabilidades;
  const contextoTreino = vaga
    ? textos.historico.contextoVaga(vaga.titulo, vaga.empresa)
    : textos.historico.contextoHabilidades;

  cy.intercept(metodosHttp.post, endpoints.iaPerguntas, (requisicao) => {
    expect(requisicao.body.tipo_contexto).to.eq(
      vaga ? valoresSistema.contexto.vaga : valoresSistema.contexto.habilidades,
    );
    if (vaga) {
      expect(requisicao.body.vaga).to.deep.equal({
        titulo: vaga.titulo,
        senioridade: vaga.senioridade,
        requisitos: vaga.requisitos,
      });
    }
    const numero = requisicao.body[camposApi.treino.numeroPergunta];
    requisicao.reply({
      statusCode: codigosHttp.sucesso,
      body: {
        pergunta: { texto: textos.treino.perguntaIa(numero) },
        versao_modelo: valoresCdu.versaoIa,
        versao_prompt: valoresCdu.versaoPrompt,
      },
    });
  }).as(aliases.gerarPerguntaIA);
  cy.intercept(metodosHttp.post, endpoints.iaAvaliacoes, {
    statusCode: codigosHttp.sucesso,
    body: {
      nota: valoresCdu.pontuacao,
      pontos_fortes: valoresCdu.pontosFortes,
      pontos_melhoria: valoresCdu.pontosMelhoria,
      versao_modelo: valoresCdu.versaoIa,
      versao_prompt: valoresCdu.versaoPrompt,
    },
  }).as(aliases.avaliarTreinoIA);
  cy.intercept(metodosHttp.post, endpoints.treinosConcluidos, (requisicao) => {
    expect(requisicao.body.tipoContexto).to.eq(
      vaga ? valoresSistema.contexto.vaga : valoresSistema.contexto.habilidades,
    );
    if (vaga) {
      expect(requisicao.body.vagaId).to.eq(vaga.id);
      expect(requisicao.body.contexto).to.eq(contextoTreino);
    }
    requisicao.reply({
      statusCode: codigosHttp.criado,
      body: {
        id: valoresCdu.treinoCaminhoFeliz,
        titulo: tituloTreino,
        contexto: contextoTreino,
        nota: valoresCdu.pontuacao,
        data: datasCypress.treinoConcluido,
        pontosFortes: valoresCdu.pontosFortes,
        pontosMelhoria: valoresCdu.pontosMelhoria,
      },
    });
  }).as(aliases.salvarTreinoConcluido);
}

export function iniciarEntrevistaComIA(vaga = null) {
  cy.contains(elementos.botao, textos.treino.iniciarEntrevista).click();
  cy.wait(aliases.aguardar(aliases.gerarPerguntaIA)).then(({ request }) => {
    expect(request.body[camposApi.treino.numeroPergunta]).to.eq(1);
    expect(request.body.tipo_contexto).to.eq(
      vaga ? valoresSistema.contexto.vaga : valoresSistema.contexto.habilidades,
    );
  });
  cy.contains(textos.treino.perguntaIa(1)).should("be.visible");
}

export function responderEntrevistaComIA(vaga = null) {
  Array.from({ length: dadosCaminhoFeliz.totalPerguntas }, (_, i) => i + 1).forEach((numero) => {
    cy.get(seletores.treino.resposta).clear().type(textos.treino.respostaDemonstracao(numero));
    cy.contains(elementos.botao, textos.treino.enviarResposta).click();
    if (numero < dadosCaminhoFeliz.totalPerguntas) {
      cy.wait(aliases.aguardar(aliases.gerarPerguntaIA))
        .its(`request.body.${camposApi.treino.numeroPergunta}`)
        .should("eq", numero + 1);
      cy.contains(textos.treino.perguntaIa(numero + 1)).should("be.visible");
    }
  });
  cy.wait(aliases.aguardar(aliases.avaliarTreinoIA)).then(({ request }) => {
    expect(request.body.interacoes).to.have.length(dadosCaminhoFeliz.totalPerguntas);
    expect(request.body.tipo_contexto).to.eq(
      vaga ? valoresSistema.contexto.vaga : valoresSistema.contexto.habilidades,
    );
  });
  cy.contains(elementos.botao, textos.treino.finalizar).should("be.enabled");
}

export function concluirTreinoEConferirFeedback(vaga = null) {
  cy.contains(elementos.botao, textos.treino.finalizar).click();
  cy.wait(aliases.aguardar(aliases.salvarTreinoConcluido)).then(({ request }) => {
    expect(request.body.interacoes).to.have.length(dadosCaminhoFeliz.totalPerguntas);
    expect(request.body.tipoContexto).to.eq(
      vaga ? valoresSistema.contexto.vaga : valoresSistema.contexto.habilidades,
    );
    if (vaga) expect(request.body.vagaId).to.eq(vaga.id);
  });
  cy.location(parametrosUrl.caminho).should("eq", rotas.feedback);
  cy.get(seletores.treino.nota).should("have.text", String(valoresCdu.pontuacao));
  cy.contains(valoresCdu.pontosFortes[0]).should("be.visible");
  cy.contains(valoresCdu.pontosMelhoria[0]).should("be.visible");
}

export function abrirHistoricoPeloFeedback() {
  cy.contains(elementos.botao, textos.treino.historico).click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.historico);
  cy.contains(elementos.cabecalho1, textos.historico.tituloPagina).should("be.visible");
}

export function consultarTreinoNoHistorico({
  titulo = textos.historico.tituloEntrevistaHabilidades,
  nota = String(valoresCdu.pontuacao),
  pontoForte = valoresCdu.pontosFortes[0],
  melhoria = valoresCdu.pontosMelhoria[0],
  contexto,
} = {}) {
  const treino = cy.contains(seletores.historico.linhaTreino, titulo).should("contain", nota);
  if (contexto) treino.should("contain", contexto);
  treino.click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.feedback);
  cy.contains(elementos.cabecalho1, textos.feedback.titulo).should("be.visible");
  cy.contains(pontoForte).should("be.visible");
  cy.contains(melhoria).should("be.visible");
}

export function abrirHistoricoDoTreinoDaVaga(vaga) {
  abrirComoAluno(rotas.historico, {
    conta: alunoAtualizadoCaminhoFeliz,
    historico: [
      {
        id: valoresCdu.treinoCaminhoFeliz,
        titulo: textos.historico.tituloEntrevistaVaga(vaga.titulo),
        contexto: textos.historico.contextoVaga(vaga.titulo, vaga.empresa),
        nota: valoresCdu.pontuacao,
        data: datasCypress.treinoConcluido,
        pontosFortes: valoresCdu.pontosFortes,
        pontosMelhoria: valoresCdu.pontosMelhoria,
      },
    ],
  });
}

export function cadastrarAluno(aluno) {
  enviarCadastro(aliases.aguardar(aliases.criarAluno), {
    nome: aluno.nome,
    email: aluno.email,
    cpf: aluno.cpf,
    telefone: aluno.telefone,
    endereco: aluno.endereco,
    senha: dadosTeste.senha,
  });
}

export function verificarLoginAutomaticoAluno(aluno) {
  verificarAutenticacaoAutomatica(aliases.aguardar(aliases.autenticarAluno), {
    email: aluno.email,
    senha: dadosTeste.senha,
  });
}

export function continuarParaPlanos() {
  cy.contains(elementos.botao, textos.continuar).should("be.visible").click();
  cy.location(parametrosUrl.caminho).should("eq", rotas.planos);
  cy.location(parametrosUrl.busca).should("contain", parametrosUrl.acessoNecessario);
  cy.contains(elementos.cabecalho1, textos.plano.tituloPagina).should("be.visible");
  cy.contains(elementos.cabecalho3, planoIndividualCdu.nome).should("be.visible");
}

export function prepararCaminhoFelizPerfilInstituicao() {
  const instituicao = dadosTeste.instituicao;
  prepararVisitanteSemSessao();
  prepararPlanoAtivo(valoresSistema.modalidade.institucional);
  prepararInstituicaoSemAlunos();

  cy.intercept(metodosHttp.post, endpoints.criarInstituicao, {
    statusCode: codigosHttp.criado,
    body: {
      id: instituicao.id,
      nome: instituicao.nomeInstituicao,
      nomeResponsavel: instituicao.nomeResponsavel,
      email: instituicao.email,
      cnpj: instituicao.cnpj.replace(/\D/g, ""),
      telefone: instituicao.telefone,
      endereco: instituicao.endereco,
      tipo_conta: valoresSistema.conta.instituicao,
    },
  }).as(aliases.criarInstituicao);
  cy.intercept(metodosHttp.post, endpoints.sessao, {
    statusCode: codigosHttp.sucesso,
    body: {
      accessToken: tokensCypress.instituicao,
      tipoToken: valoresSistema.tipoToken,
      expiraEmSegundos: datasCypress.expiracaoTokenSegundos,
      conta: {
        id: instituicao.id,
        nome: instituicao.nomeInstituicao,
        nomeResponsavel: instituicao.nomeResponsavel,
        email: instituicao.email,
        cnpj: instituicao.cnpj.replace(/\D/g, ""),
        telefone: instituicao.telefone,
        endereco: instituicao.endereco,
        tipoConta: valoresSistema.conta.instituicao,
      },
    },
  }).as(aliases.autenticarInstituicao);
}

export function cadastrarInstituicao(instituicao) {
  enviarCadastro(aliases.aguardar(aliases.criarInstituicao), {
    nomeResponsavel: instituicao.nomeResponsavel,
    nomeInstituicao: instituicao.nomeInstituicao,
    email: instituicao.email,
    senha: dadosTeste.senha,
    cnpj: instituicao.cnpj,
    telefone: instituicao.telefone,
    endereco: instituicao.endereco,
  });
}

export function verificarLoginAutomaticoInstituicao(instituicao) {
  verificarAutenticacaoAutomatica(aliases.aguardar(aliases.autenticarInstituicao), {
    email: instituicao.email,
    senha: dadosTeste.senha,
  });
}

export function verificarAreaInstituicao(instituicao) {
  cy.location(parametrosUrl.caminho).should("eq", rotas.instituicao);
  cy.get(classes.areaInstituicao)
    .contains(elementos.cabecalho1, instituicao.nomeInstituicao)
    .should("be.visible");
  cy.contains(textos.responsavel).should("be.visible");
  cy.contains(elementos.forte, instituicao.nomeResponsavel).should("be.visible");
  cy.contains(elementos.cabecalho2, textos.alunosParticipantes).should("be.visible");
  cy.contains(textos.nenhumAluno).should("be.visible");
}

export function cadastrarNovoAlunoNaInstituicao(aluno) {
  const contratoId = valoresCdu.contratoInstitucionalCaminhoFeliz;
  cy.intercept(metodosHttp.post, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.criado,
    body: {
      aluno: {
        id: aluno.id,
      tipoConta: valoresSistema.conta.aluno,
        nome: aluno.nome,
        email: aluno.email,
        cpf: aluno.cpf.replace(/\D/g, ""),
        telefone: aluno.telefone,
        endereco: aluno.endereco,
        objetoInstituicao: dadosTeste.instituicao.id,
      },
      vinculacao: {
        vinculo: {
          id: valoresCdu.vinculoAlunoCaminhoFeliz,
          contratoId,
          contaAlunoId: aluno.id,
          nomeAluno: aluno.nome,
          emailAluno: aluno.email,
          vinculadoEm: datasCypress.alunoVinculado,
        },
        capacidade: {
          contratoId,
          quantidadeVagas: valoresCdu.capacidadeInstitucional.total,
          vagasUtilizadas: valoresCdu.capacidadeInstitucional.usada,
          vagasDisponiveis: valoresCdu.capacidadeInstitucional.disponivel,
        },
      },
    },
  }).as(aliases.cadastrarEVincularAluno);

  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.nomeCompleto).find(elementos.input).type(aluno.nome);
    cy.contains(elementos.label, textos.instituicao.email).find(elementos.input).type(aluno.email);
    cy.contains(elementos.label, textos.instituicao.cpf).find(elementos.input).type(aluno.cpf);
    cy.contains(elementos.label, textos.instituicao.telefone).find(elementos.input).type(aluno.telefone);
    cy.contains(elementos.label, textos.instituicao.endereco).find(elementos.input).type(aluno.endereco);
    cy.contains(elementos.label, textos.instituicao.senha).find(elementos.input).type(dadosTeste.senha);
    cy.contains(elementos.label, textos.instituicao.confirmarSenha).find(elementos.input).type(dadosTeste.senha);
    cy.contains(elementos.botao, textos.instituicao.cadastrarEVincular).click();
  });

  cy.wait(aliases.aguardar(aliases.cadastrarEVincularAluno)).then(({ request, response }) => {
    expect(request.body).to.deep.equal({
      contratoId,
      nome: aluno.nome,
      email: aluno.email,
      cpf: aluno.cpf,
      telefone: aluno.telefone,
      endereco: aluno.endereco,
      senha: dadosTeste.senha,
    });
    expect(response.body.aluno.objetoInstituicao).to.eq(dadosTeste.instituicao.id);
    expect(response.body.vinculacao.vinculo.contaAlunoId).to.eq(aluno.id);
  });
  cy.contains(textos.instituicao.mensagemAlunoCriado).should("be.visible");
  cy.contains(elementos.celulaTabela, aluno.nome).should("be.visible");
  cy.contains(textos.instituicao.vagasDisponiveis).parent()
    .should("contain", valoresCdu.capacidadeInstitucional.disponivel);
}
