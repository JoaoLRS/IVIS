import {
  alunoCdu,
  abrirComoAluno,
  abrirComoInstituicao,
  credenciaisCdu,
  instituicaoCdu,
  planoIndividualCdu,
  planoInstitucionalCdu,
  prepararCatalogo,
  prepararCheckout,
  prepararSessao,
  prepararVisitante,
  respostaContratacao,
} from "./apoio.js";
import {
  aliases,
  camposApi,
  dadosCaminhoFeliz,
  cabecalhosHttp,
  classes,
  codigosHttp,
  datasCypress,
  elementos,
  endpoints,
  habilidadeTesteCdu,
  historicoCdu,
  metodosHttp,
  parametrosUrl,
  rotas,
  seletores,
  textos,
  valoresCdu,
  valoresSistema,
  vagaTesteCdu,
} from "./constantes.js";

export function prepararConfirmacaoDemoradaDoPlano() {
  cy.wait(aliases.aguardar(aliases.consultarPlanoVigente));
  cy.intercept(metodosHttp.get, endpoints.plano, {
    delay: 2000,
    statusCode: codigosHttp.sucesso,
    body: {
      situacao: valoresSistema.plano.comVigente,
      planos: [
        {
          situacao: valoresSistema.plano.ativo,
          origemAcesso: valoresSistema.modalidade.individual,
        },
      ],
    },
  }).as(aliases.consultarPlanoAposCompra);
}

export function conferirAcessoAguardandoConfirmacaoDoPlano() {
  cy.contains(elementos.cabecalho2, textos.plano.aprovado).should("be.visible");
  cy.contains(elementos.ancora, textos.plano.irParaArea).should("not.exist");
}

export function aguardarConfirmacaoDoPlano() {
  cy.wait(aliases.aguardar(aliases.consultarPlanoAposCompra))
    .its("response.body.situacao")
    .should("eq", valoresSistema.plano.comVigente);
}

export const habilidadeTeste = habilidadeTesteCdu;

export function prepararTelaDeHabilidades(habilidades = [], opcoes = {}) {
  abrirComoAluno(rotas.habilidades, { ...opcoes, habilidades });
}

export function prepararCadastroDeHabilidade() {
  cy.intercept(metodosHttp.post, endpoints.habilidades, (requisicao) => {
    requisicao.reply({
      statusCode: codigosHttp.criado,
      body: { id: habilidadeTeste.id, ...requisicao.body },
    });
  }).as(aliases.criarHabilidade);
}

export function abrirCadastroDeHabilidade() {
  cy.contains(elementos.botao, textos.habilidades.adicionar).click();
}

export function informarNomeDaHabilidadeCdu() {
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.label, textos.habilidades.nomeCampo)
      .find(elementos.input)
      .type(habilidadeTeste.nome);
  });
}

export function informarCategoriaDaHabilidadeCdu() {
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.label, textos.habilidades.categoriaCampo)
      .find(elementos.input)
      .type(habilidadeTeste.categoria);
  });
}

export function selecionarNivelDaHabilidadeCdu() {
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.label, textos.habilidades.nivelCampo)
      .find(elementos.select)
      .select(valoresCdu.nivelHabilidade);
  });
}

export function confirmarCadastroDaHabilidadeCdu() {
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.botao, textos.habilidades.salvar).click();
  });
}

export function conferirCadastroDeHabilidade() {
  cy.wait(aliases.aguardar(aliases.criarHabilidade))
    .its("request.body")
    .should("deep.equal", {
      nome: habilidadeTeste.nome,
      categoria: habilidadeTeste.categoria,
      nivel: habilidadeTeste.nivel,
    });
  cy.contains(elementos.cabecalho3, habilidadeTeste.nome).should("be.visible");
}

export function prepararEdicaoDeHabilidade() {
  cy.intercept(
    metodosHttp.put,
    endpoints.habilidade(habilidadeTeste.id),
    (requisicao) => {
      requisicao.reply({
        statusCode: codigosHttp.sucesso,
        body: { id: habilidadeTeste.id, ...requisicao.body },
      });
    },
  ).as(aliases.atualizarHabilidade);
}

export function abrirEdicaoDeHabilidade() {
  cy.get(seletores.habilidade.botaoEditar(habilidadeTeste.nome)).click();
}

export function alterarNomeDaHabilidadeCdu() {
  const nomeAtualizado = valoresCdu.habilidadeAtualizada;
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.label, textos.habilidades.nomeCampo)
      .find(elementos.input)
      .clear()
      .type(nomeAtualizado);
  });
}

export function confirmarEdicaoDaHabilidadeCdu() {
  cy.get(seletores.habilidade.dialogo).within(() => {
    cy.contains(elementos.botao, textos.conta.salvar).click();
  });
}

export function conferirEdicaoDeHabilidade() {
  const nomeAtualizado = valoresCdu.habilidadeAtualizada;
  cy.wait(aliases.aguardar(aliases.atualizarHabilidade))
    .its("request.body.nome")
    .should("eq", nomeAtualizado);
  cy.contains(elementos.cabecalho3, nomeAtualizado).should("be.visible");
}

export function prepararRemocaoDeHabilidade() {
  cy.intercept(metodosHttp.delete, endpoints.habilidade(habilidadeTeste.id), {
    statusCode: codigosHttp.semConteudo,
    body: null,
  }).as(aliases.removerHabilidade);
}

export function solicitarRemocaoDeHabilidade() {
  const nomeAtualizado = valoresCdu.habilidadeAtualizada;
  cy.get(seletores.habilidade.botaoRemover(nomeAtualizado)).click();
}

export function confirmarRemocaoDeHabilidade() {
  cy.get(seletores.habilidade.dialogo).within(() =>
    cy.contains(elementos.botao, textos.excluir).click(),
  );
}

export function conferirRemocaoDeHabilidade() {
  cy.wait(aliases.aguardar(aliases.removerHabilidade));
  cy.contains(textos.habilidades.nenhuma).should("be.visible");
}

export function prepararRecuperacaoDeAcesso() {
  prepararVisitante();
  cy.intercept(metodosHttp.post, endpoints.recuperacaoSenha, {
    statusCode: codigosHttp.aceito,
    body: {},
  }).as(aliases.solicitarRecuperacao);
}

export function abrirFormularioDeRecuperacao(email) {
  cy.visit(rotas.login);
  cy.contains(elementos.botao, textos.login.esqueciSenha).click();
  cy.contains(elementos.cabecalho1, textos.login.tituloRecuperacao).should(
    "be.visible",
  );
  cy.get(seletores.login.email).type(email);
}

export function enviarSolicitacaoDeRecuperacao(email) {
  cy.contains(elementos.botao, textos.login.enviarInstrucoes).click();
  cy.wait(aliases.aguardar(aliases.solicitarRecuperacao))
    .its("request.body")
    .should("deep.equal", { email });
  cy.contains(textos.login.senhaRecuperada).should("be.visible");
}

export function voltarDoFormularioDeRecuperacao() {
  cy.contains(elementos.botao, textos.login.voltarLogin).click();
  cy.get(seletores.login.senha).should("be.visible");
}

export function conferirPlanoVigenteInstitucional() {
  cy.wait(aliases.aguardar(aliases.consultarPlanoVigente))
    .its("response.body")
    .should("deep.equal", {
      situacao: valoresSistema.plano.comVigente,
      planos: [
        {
          situacao: valoresSistema.plano.ativo,
          origemAcesso: valoresSistema.modalidade.institucional,
        },
      ],
    });
  cy.location(parametrosUrl.caminho).should("eq", rotas.inicioAluno);
  cy.contains(
    elementos.cabecalho1,
    textos.areaAluno.saudacao(alunoCdu.nome),
  ).should("be.visible");
  cy.get(elementos.corpo).should("not.contain", instituicaoCdu.nome);
}

export const vagaTeste = vagaTesteCdu;

export function prepararTelaDeVagas(vagas = [], opcoes = {}) {
  abrirComoAluno(rotas.vagas, { ...opcoes, vagas });
}

export function prepararCadastroDeVaga() {
  cy.intercept(metodosHttp.post, endpoints.vagas, (requisicao) => {
    requisicao.reply({
      statusCode: codigosHttp.criado,
      body: { id: vagaTeste.id, ...requisicao.body },
    });
  }).as(aliases.criarVaga);
}

export function abrirCadastroDeVaga() {
  cy.contains(elementos.botao, textos.vagas.adicionar).click();
}

export function informarTituloDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.label, textos.vagas.cargoCampo)
      .find(elementos.input)
      .type(vagaTeste.titulo);
  });
}

export function informarEmpresaDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.label, textos.vagas.empresaCampo)
      .find(elementos.input)
      .type(vagaTeste.empresa);
  });
}

export function selecionarNivelDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.label, textos.vagas.nivelCampo)
      .find(elementos.select)
      .select(vagaTeste.senioridade);
  });
}

export function informarRequisitosDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.label, textos.vagas.requisitosCampo)
      .find(elementos.input)
      .type(valoresCdu.requisitoVaga);
  });
}

export function confirmarCadastroDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.botao, textos.vagas.salvar).click();
  });
}

export function conferirCadastroDeVaga() {
  cy.wait(aliases.aguardar(aliases.criarVaga))
    .its("request.body")
    .should("deep.equal", {
      titulo: vagaTeste.titulo,
      empresa: vagaTeste.empresa,
      senioridade: vagaTeste.senioridade,
      requisitos: vagaTeste.requisitos,
      descricao: null,
    });
  cy.contains(elementos.cabecalho2, vagaTeste.titulo).should("be.visible");
}

export function prepararEdicaoDeVaga() {
  cy.intercept(metodosHttp.put, endpoints.vaga(vagaTeste.id), (requisicao) => {
    requisicao.reply({
      statusCode: codigosHttp.sucesso,
      body: { id: vagaTeste.id, ...requisicao.body },
    });
  }).as(aliases.atualizarVaga);
}

export function abrirEdicaoDeVaga() {
  cy.get(seletores.vaga.botaoEditar(vagaTeste.titulo)).click();
}

export function alterarTituloDaVagaCdu() {
  const tituloAtualizado = valoresCdu.vagaAtualizada;
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.label, textos.vagas.cargoCampo)
      .find(elementos.input)
      .clear()
      .type(tituloAtualizado);
  });
}

export function confirmarEdicaoDaVagaCdu() {
  cy.get(seletores.vaga.dialogo).within(() => {
    cy.contains(elementos.botao, textos.conta.salvar).click();
  });
}

export function conferirEdicaoDeVaga() {
  const tituloAtualizado = valoresCdu.vagaAtualizada;
  cy.wait(aliases.aguardar(aliases.atualizarVaga))
    .its("request.body.titulo")
    .should("eq", tituloAtualizado);
  cy.contains(elementos.cabecalho2, tituloAtualizado).should("be.visible");
}

export function prepararExclusaoDeVaga() {
  cy.intercept(metodosHttp.delete, endpoints.vaga(vagaTeste.id), {
    statusCode: codigosHttp.semConteudo,
    body: null,
  }).as(aliases.arquivarVaga);
}

export function solicitarExclusaoDeVaga() {
  const tituloAtualizado = valoresCdu.vagaAtualizada;
  cy.get(seletores.vaga.botaoExcluir(tituloAtualizado)).click();
}

export function confirmarExclusaoDeVaga() {
  cy.get(seletores.vaga.dialogo).within(() =>
    cy.contains(elementos.botao, textos.excluir).click(),
  );
}

export function conferirExclusaoDeVaga() {
  cy.wait(aliases.aguardar(aliases.arquivarVaga));
  cy.contains(textos.vagas.nenhuma).should("be.visible");
}

export function conferirModalidadesDoCatalogo() {
  cy.contains(elementos.cabecalho3, planoIndividualCdu.nome).should(
    "be.visible",
  );
  cy.contains(elementos.cabecalho3, planoInstitucionalCdu.nome).should(
    "be.visible",
  );
}

export function selecionarOfertaInstitucionalDoCatalogo() {
  cy.contains(classes.cartaoPlano, planoInstitucionalCdu.nome)
    .contains(elementos.ancora, textos.plano.criarContaInstitucional)
    .click();
}

export function conferirOfertaPreservadaNoCadastro() {
  cy.location(parametrosUrl.caminho).should("eq", rotas.cadastro);
  cy.location(parametrosUrl.busca).then((search) => {
    const parametros = new URLSearchParams(search);
    expect(parametros.get(parametrosUrl.tipo)).to.eq(
      valoresSistema.perfilCadastro.instituicao,
    );
    expect(parametros.get(parametrosUrl.redirecionamento)).to.eq(
      rotas.checkout(planoInstitucionalCdu.id),
    );
  });
  cy.contains(seletores.cadastro.perfil, textos.perfilInstituicao).should(
    "have.attr",
    "aria-checked",
    "true",
  );
}

export function prepararContratacaoInstitucional() {
  prepararSessao({
    tipoConta: valoresSistema.conta.instituicao,
    planoAtivo: false,
  });
  prepararCheckout(planoInstitucionalCdu);
  cy.intercept(metodosHttp.post, endpoints.contratacoesSimuladas, {
    statusCode: codigosHttp.criado,
    body: respostaContratacao({ plano: planoInstitucionalCdu }),
  }).as(aliases.contratarPlanoInstitucional);
}

export function abrirCheckoutInstitucional() {
  cy.visit(rotas.checkout(planoInstitucionalCdu.id));
}

export function conferirOfertaInstitucional() {
  cy.wait(aliases.aguardar(aliases.consultarOfertaCheckout));
  cy.contains(
    elementos.cabecalho1,
    textos.plano.tituloCheckoutInstitucional,
  ).should("be.visible");
  cy.contains(elementos.forte, textos.plano.alunosIncluidos).should(
    "be.visible",
  );
}

export function preencherCartaoInstitucionalAprovado() {
  cy.contains(elementos.botao, textos.plano.cartaoAprovado).click();
}

export function enviarPagamentoInstitucional() {
  cy.contains(elementos.botao, textos.plano.simularPagamento).click();
}

export function conferirContratacaoInstitucional() {
  cy.wait(aliases.aguardar(aliases.contratarPlanoInstitucional))
    .its("request.body.planoId")
    .should("eq", planoInstitucionalCdu.id);
  cy.contains(elementos.cabecalho2, textos.plano.aprovado).should("be.visible");
  cy.contains(textos.plano.planoInstitucionalAtivo).should("be.visible");
  cy.contains(textos.plano.semCobranca).should("be.visible");
  cy.contains(elementos.ancora, textos.plano.irParaArea).should(
    "have.attr",
    "href",
    rotas.instituicao,
  );
}

export function prepararPagamentoRecusado() {
  prepararCheckout(planoIndividualCdu);
  cy.intercept(metodosHttp.post, endpoints.contratacoesSimuladas, {
    statusCode: codigosHttp.criado,
    body: respostaContratacao({
      situacaoPagamento: valoresSistema.pagamento.recusado,
      plano: planoIndividualCdu,
    }),
  }).as(aliases.recusarPagamento);
}

export function abrirCheckoutIndividualSemPlano() {
  abrirComoAluno(rotas.checkout(planoIndividualCdu.id), { planoAtivo: false });
}

export function conferirOfertaDoCheckout() {
  cy.wait(aliases.aguardar(aliases.consultarOfertaCheckout));
}

export function preencherCartaoRecusado() {
  cy.contains(elementos.botao, textos.plano.cartaoRecusado).click();
}

export function enviarPagamentoRecusado() {
  cy.contains(elementos.botao, textos.plano.simularPagamento).click();
}

export function conferirPagamentoRecusado() {
  cy.wait(aliases.aguardar(aliases.recusarPagamento)).then(({ request }) => {
    expect(request.body).to.deep.equal({
      planoId: planoIndividualCdu.id,
      numeroCartao: valoresCdu.cartaoRecusado.numero,
      validade: valoresCdu.cartaoRecusado.validade,
      cvv: valoresCdu.cartaoRecusado.cvv,
    });
    expect(request.headers[cabecalhosHttp.chaveIdempotencia]).to.be.a("string")
      .and.not.be.empty;
  });
  cy.contains(elementos.cabecalho2, textos.plano.recusado).should("be.visible");
  cy.contains(textos.plano.cartaoRecusadoMensagem).should("be.visible");
  cy.contains(textos.plano.planoNaoAtivado).should("be.visible");
  cy.contains(elementos.botao, textos.plano.tentarNovamente).should(
    "be.visible",
  );
}

export function prepararCadastroEVinculoInstitucional() {
  const contratoInstitucional = valoresCdu.contratoInstitucional;
  const capacidadeLivre = {
    contratoId: contratoInstitucional,
    quantidadeVagas: valoresCdu.capacidadeInstitucional.total,
    vagasUtilizadas: valoresCdu.capacidadeInstitucionalVazia.usada,
    vagasDisponiveis: valoresCdu.capacidadeInstitucionalVazia.disponivel,
  };
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  const vinculo = {
    id: valoresCdu.vinculoAluno,
    contratoId: contratoInstitucional,
    contaAlunoId: aluno.id,
    nomeAluno: aluno.nome,
    emailAluno: aluno.email,
    vinculadoEm: datasCypress.alunoVinculado,
  };
  prepararSessao({ tipoConta: valoresSistema.conta.instituicao });
  cy.intercept(metodosHttp.get, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.capacidadeInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [capacidadeLivre],
  });
  cy.intercept(metodosHttp.post, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.criado,
    body: {
      aluno,
      vinculacao: {
        vinculo,
        capacidade: {
          ...capacidadeLivre,
          vagasUtilizadas: valoresCdu.capacidadeInstitucional.usada,
          vagasDisponiveis: valoresCdu.capacidadeInstitucional.disponivel,
        },
      },
    },
  }).as(aliases.cadastrarEVincular);
  cy.intercept(metodosHttp.delete, endpoints.vinculoInstitucional(vinculo.id), {
    statusCode: codigosHttp.semConteudo,
    body: null,
  }).as(aliases.removerVinculo);
}

export function abrirGestaoInstitucional() {
  cy.visit(rotas.instituicao);
}

export function informarNomeDoAlunoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.nomeCompleto)
      .find(elementos.input)
      .type(aluno.nome);
  });
}

export function informarEmailDoAlunoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.email)
      .find(elementos.input)
      .type(aluno.email);
  });
}

export function informarCpfDoAlunoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.cpf)
      .find(elementos.input)
      .type(aluno.cpf);
  });
}

export function informarTelefoneDoAlunoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.telefone)
      .find(elementos.input)
      .type(aluno.telefone);
  });
}

export function informarEnderecoDoAlunoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.endereco)
      .find(elementos.input)
      .type(aluno.endereco);
  });
}

export function informarSenhaDoAlunoInstitucional() {
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.senha)
      .find(elementos.input)
      .type(credenciaisCdu.senha);
  });
}

export function confirmarSenhaDoAlunoInstitucional() {
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.label, textos.instituicao.confirmarSenha)
      .find(elementos.input)
      .type(credenciaisCdu.senha);
  });
}

export function enviarCadastroDoAlunoInstitucional() {
  cy.get(seletores.instituicao.formularioAluno).within(() => {
    cy.contains(elementos.botao, textos.instituicao.cadastrarEVincular).click();
  });
}

export function conferirCadastroEVinculoInstitucional() {
  const contratoInstitucional = valoresCdu.contratoInstitucional;
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.wait(aliases.aguardar(aliases.cadastrarEVincular))
    .its("request.body")
    .should("deep.equal", {
      contratoId: contratoInstitucional,
      nome: aluno.nome,
      email: aluno.email,
      cpf: aluno.cpf,
      telefone: aluno.telefone,
      endereco: aluno.endereco,
      senha: credenciaisCdu.senha,
    });
  cy.contains(textos.instituicao.mensagemAlunoCriado).should("be.visible");
  cy.contains(elementos.celulaTabela, aluno.nome).should("be.visible");
  cy.contains(textos.instituicao.vagasDisponiveis)
    .parent()
    .should("contain", valoresCdu.capacidadeInstitucional.disponivel);
}

export function removerVinculoInstitucional() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.get(seletores.instituicao.removerAluno(aluno.nome)).click();
}

export function conferirVinculoRemovido() {
  const aluno = {
    ...valoresCdu.alunoInstitucional,
    tipoConta: valoresSistema.conta.aluno,
    objetoInstituicao: instituicaoCdu.id,
  };
  cy.wait(aliases.aguardar(aliases.removerVinculo));
  cy.contains(elementos.celulaTabela, aluno.nome).should("not.exist");
  cy.contains(textos.instituicao.removerVinculo(aluno.nome)).should(
    "be.visible",
  );
}

export function prepararVinculoDeAlunoExistente() {
  const contratoInstitucional = valoresCdu.contratoInstitucional;
  const capacidadeLivre = {
    contratoId: contratoInstitucional,
    quantidadeVagas: valoresCdu.capacidadeInstitucionalVazia.total,
    vagasUtilizadas: valoresCdu.capacidadeInstitucionalVazia.usada,
    vagasDisponiveis: valoresCdu.capacidadeInstitucionalVazia.disponivel,
  };
  prepararSessao({ tipoConta: valoresSistema.conta.instituicao });
  cy.intercept(metodosHttp.get, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.capacidadeInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [capacidadeLivre],
  });
  cy.intercept(metodosHttp.post, endpoints.vincularAlunoPorEmail, {
    statusCode: codigosHttp.criado,
    body: {
      vinculo: {
        id: valoresCdu.vinculoAlunoExistente,
        contratoId: contratoInstitucional,
        contaAlunoId: valoresCdu.alunoExistente.id,
        nomeAluno: valoresCdu.alunoExistente.nome,
        emailAluno: valoresCdu.alunoExistente.email,
        vinculadoEm: datasCypress.alunoVinculado,
      },
      capacidade: {
        ...capacidadeLivre,
        vagasUtilizadas: valoresCdu.capacidadeInstitucional.usada,
        vagasDisponiveis: valoresCdu.capacidadeInstitucional.disponivel,
      },
    },
  }).as(aliases.vincularExistente);
}

export function abrirGestaoParaVinculoExistente() {
  cy.visit(rotas.instituicao);
}

export function informarEmailDoAlunoExistente() {
  cy.get(seletores.instituicao.emailAluno).type(
    valoresCdu.alunoExistente.email,
  );
}

export function confirmarVinculoDoAlunoExistente() {
  cy.contains(elementos.botao, textos.instituicao.vincularAluno).click();
}

export function conferirVinculoDoAlunoExistente() {
  const contratoInstitucional = valoresCdu.contratoInstitucional;
  cy.wait(aliases.aguardar(aliases.vincularExistente))
    .its("request.body")
    .should("deep.equal", {
      emailAluno: valoresCdu.alunoExistente.email,
      contratoId: contratoInstitucional,
    });
  cy.contains(textos.instituicao.mensagemAlunoVinculado).should("be.visible");
  cy.contains(elementos.celulaTabela, valoresCdu.alunoExistente.nome).should(
    "be.visible",
  );
}

export function prepararInstituicaoSemVinculos() {
  cy.intercept(metodosHttp.get, endpoints.alunosInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
  cy.intercept(metodosHttp.get, endpoints.capacidadeInstituicao, {
    statusCode: codigosHttp.sucesso,
    body: [],
  });
}

export function acessarTreinoComoInstituicao() {
  abrirComoInstituicao(rotas.treinos);
}

export function conferirRedirecionamentoParaInstituicao() {
  cy.location(parametrosUrl.caminho).should("eq", rotas.instituicao);
  cy.contains(elementos.cabecalho1, instituicaoCdu.nome).should("be.visible");
}

export function prepararVerificacaoDeAuditoriaDaCompra() {
  cy.intercept(metodosHttp.post, endpoints.contratacoesSimuladas).as(
    aliases.contratarParaAuditoria,
  );
}

export function conferirAuditoriaDaCompraPersistida() {
  cy.wait(aliases.aguardar(aliases.contratarParaAuditoria))
    .then(({ response }) => {
      expect(response.statusCode).to.eq(codigosHttp.sucesso);
      expect(response.body.situacaoPagamento).to.eq(
        valoresSistema.pagamento.aprovado,
      );
      return cy.task(
        "consultarAuditoriaPorContrato",
        response.body.contratoId,
      );
    })
    .then((eventos) => {
      expect(eventos).to.have.length(1);
      expect(eventos[0]).to.include({
        acao: "CONTRATACAO_PAGAMENTO_SIMULADO",
        resultado: "SUCESSO",
        tipoRecurso: "CONTRATO",
      });
      expect(eventos[0].atorContaId).to.be.a("string").and.not.be.empty;
      expect(eventos[0].detalhes).to.include({
        modo: "SIMULADO",
        situacaoPagamento: "APROVADO",
      });
    });
}

export function prepararCatalogoParaAlunoSemAcesso() {
  prepararCatalogo();
}

export function acessarTreinoComoAlunoSemPlano() {
  abrirComoAluno(rotas.treinos, { planoAtivo: false });
}

export function conferirRedirecionamentoParaOfertas() {
  cy.location(parametrosUrl.caminho).should("eq", rotas.planos);
  cy.location(parametrosUrl.busca).should(
    "contain",
    parametrosUrl.acessoNecessario,
  );
  cy.contains(textos.plano.acessoNecessario).should("be.visible");
  cy.contains(elementos.cabecalho1, textos.plano.tituloPagina).should(
    "be.visible",
  );
}

export function abrirTreinoPorHabilidades() {
  abrirComoAluno(rotas.treinos, { habilidades: [habilidadeTesteCdu] });
}

export function digitarRespostaDaEntrevista(numero) {
  cy.get(seletores.treino.resposta)
    .clear()
    .type(textos.treino.respostaDemonstracao(numero));
}

export function enviarRespostaDaEntrevista() {
  cy.contains(elementos.botao, textos.treino.enviarResposta).click();
}

export function conferirProximaPerguntaDaEntrevista(numero) {
  cy.wait(aliases.aguardar(aliases.gerarPerguntaIA))
    .its(`request.body.${camposApi.treino.numeroPergunta}`)
    .should("eq", numero);
  cy.contains(textos.treino.perguntaIa(numero)).should("be.visible");
}

export function conferirAvaliacaoDaEntrevista() {
  cy.wait(aliases.aguardar(aliases.avaliarTreinoIA)).then(({ request }) => {
    expect(request.body.interacoes).to.have.length(
      dadosCaminhoFeliz.totalPerguntas,
    );
    expect(request.body.tipo_contexto).to.eq(
      valoresSistema.contexto.habilidades,
    );
  });
  cy.contains(elementos.botao, textos.treino.finalizar).should("be.enabled");
}

export function finalizarEntrevista() {
  cy.contains(elementos.botao, textos.treino.finalizar).click();
}

export function conferirFeedbackDaEntrevista() {
  cy.wait(aliases.aguardar(aliases.salvarTreinoConcluido)).then(
    ({ request }) => {
      expect(request.body.interacoes).to.have.length(
        dadosCaminhoFeliz.totalPerguntas,
      );
      expect(request.body.tipoContexto).to.eq(
        valoresSistema.contexto.habilidades,
      );
    },
  );
  cy.location(parametrosUrl.caminho).should("eq", rotas.feedback);
  cy.get(seletores.treino.nota).should(
    "have.text",
    String(valoresCdu.pontuacao),
  );
  cy.contains(valoresCdu.pontosFortes[0]).should("be.visible");
  cy.contains(valoresCdu.pontosMelhoria[0]).should("be.visible");
}

export function conferirTreinoListadoNoHistorico() {
  cy.contains(seletores.historico.linhaTreino, historicoCdu[0].titulo).should(
    "contain",
    historicoCdu[0].nota,
  );
}
export function abrirDetalhesDoTreinoNoHistorico() {
  cy.contains(seletores.historico.linhaTreino, historicoCdu[0].titulo).click();
}
export function conferirDetalhesDoTreinoNoHistorico() {
  cy.location(parametrosUrl.caminho).should("eq", rotas.feedback);
  cy.contains(elementos.cabecalho1, textos.feedback.titulo).should(
    "be.visible",
  );
  cy.contains(historicoCdu[0].pontosFortes[0]).should("be.visible");
  cy.contains(historicoCdu[0].pontosMelhoria[0]).should("be.visible");
}

export function informarResponsavelDaInstituicao(valor) {
  cy.get(seletores.cadastro.responsavel).type(valor);
}

export function informarNomeDaInstituicao(valor) {
  cy.get(seletores.cadastro.nome).type(valor);
}

export function informarEmailDaInstituicao(valor) {
  cy.get(seletores.cadastro.email).type(valor);
}

export function informarCnpjDaInstituicao(valor) {
  cy.get(seletores.cadastro.cnpj).type(valor);
}

export function informarTelefoneDaInstituicao(valor) {
  cy.get(seletores.cadastro.telefone).type(valor);
}

export function informarEnderecoDaInstituicao(valor) {
  cy.get(seletores.cadastro.endereco).type(valor);
}

export function conferirPagamentoIndividualAprovado() {
  const plano = planoIndividualCdu;
  cy.wait(aliases.aguardar(aliases.contratarPlanoIndividual)).then(
    ({ request, response }) => {
      expect(request.body).to.deep.equal({
        planoId: plano.id,
        numeroCartao: dadosCaminhoFeliz.cartaoAprovado.numero,
        validade: dadosCaminhoFeliz.cartaoAprovado.validade,
        cvv: dadosCaminhoFeliz.cartaoAprovado.cvv,
      });
      expect(request.headers[cabecalhosHttp.chaveIdempotencia]).to.be.a(
        "string",
      ).and.not.be.empty;
      expect(response.statusCode).to.eq(201);
      expect(response.body.situacaoPagamento).to.eq(
        valoresSistema.pagamento.aprovado,
      );
    },
  );
  cy.contains(elementos.cabecalho2, textos.plano.aprovado).should("be.visible");
  cy.contains(textos.plano.semCobranca).should("be.visible");
}
export function abrirConfirmacaoDeEmailDoPagamento() {
  cy.contains(elementos.resumo, textos.plano.verConfirmacaoEmail).click();
}
export function conferirConfirmacaoDeEmailDoPagamento() {
  cy.contains(textos.plano.corpoEmailSimulado).should("be.visible");
}
