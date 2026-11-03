import {
  aliases,
  codigosHttp,
  credenciaisCdu,
  datasCypress,
  endpoints,
  instituicaoCdu,
  alunoCdu,
  metodosHttp,
  planoIndividualCdu,
  planoInstitucionalCdu,
  valoresCdu,
  textosCdu,
  valoresSistema,
} from "./constantes.js";

export {
  alunoCdu,
  credenciaisCdu,
  historicoCdu,
  instituicaoCdu,
  planoIndividualCdu,
  planoInstitucionalCdu,
} from "./constantes.js";

export function prepararVisitante() {
  cy.intercept(metodosHttp.get, endpoints.csrf, {
    statusCode: codigosHttp.sucesso,
    body: { token: credenciaisCdu.csrf },
  });
  cy.intercept(metodosHttp.post, endpoints.renovarSessao, {
    statusCode: codigosHttp.naoAutorizado,
    body: { mensagem: textosCdu.erroSessaoAusente },
  });
}

export function prepararCatalogo() {
  cy.intercept(metodosHttp.get, endpoints.planos, {
    statusCode: codigosHttp.sucesso,
    body: [planoIndividualCdu, planoInstitucionalCdu],
  }).as(aliases.consultarCatalogo);
}

export function prepararSessao({
  tipoConta = valoresSistema.conta.aluno,
  planoAtivo = true,
  origemAcesso,
  conta = {},
  habilidades = [],
  vagas = [],
  historico = [],
} = {}) {
  const contaPadrao = tipoConta === valoresSistema.conta.instituicao ? instituicaoCdu : alunoCdu;
  const contaSessao = { ...contaPadrao, ...conta, tipoConta };
  const accessToken =
    tipoConta === valoresSistema.conta.instituicao
      ? valoresCdu.tokenInstituicao
      : valoresCdu.tokenAluno;
  const origem =
    origemAcesso ??
    (tipoConta === valoresSistema.conta.instituicao
      ? valoresSistema.modalidade.institucional
      : valoresSistema.modalidade.individual);

  cy.intercept(metodosHttp.get, endpoints.csrf, {
    statusCode: codigosHttp.sucesso,
    body: { token: credenciaisCdu.csrf },
  });
  cy.intercept(metodosHttp.post, endpoints.renovarSessao, {
    statusCode: codigosHttp.sucesso,
    body: {
      accessToken,
      tipoToken: valoresSistema.tipoToken,
      expiraEmSegundos: valoresCdu.expiraEmSegundos,
      conta: contaSessao,
    },
  }).as(aliases.restaurarSessao);
  cy.intercept(metodosHttp.get, endpoints.plano, {
    statusCode: codigosHttp.sucesso,
    body: planoAtivo
      ? {
          situacao: valoresSistema.plano.comVigente,
          planos: [{ situacao: valoresSistema.plano.ativo, origemAcesso: origem }],
        }
      : { situacao: valoresSistema.plano.semVigente, planos: [] },
  }).as(aliases.consultarPlanoVigente);
  cy.intercept(metodosHttp.get, endpoints.habilidades, {
    statusCode: codigosHttp.sucesso,
    body: habilidades,
  });
  cy.intercept(metodosHttp.get, endpoints.vagas, { statusCode: codigosHttp.sucesso, body: vagas });
  cy.intercept(metodosHttp.get, endpoints.treinos, { statusCode: codigosHttp.sucesso, body: historico });
  return contaSessao;
}

export function abrirComoAluno(caminho, opcoes = {}) {
  prepararSessao({ ...opcoes, tipoConta: valoresSistema.conta.aluno });
  cy.visit(caminho);
}

export function abrirComoInstituicao(caminho, opcoes = {}) {
  prepararSessao({ ...opcoes, tipoConta: valoresSistema.conta.instituicao });
  cy.visit(caminho);
}

export function prepararCheckout(plano) {
  cy.intercept(metodosHttp.get, endpoints.ofertaCheckout(plano.id), {
    statusCode: codigosHttp.sucesso,
    body: plano,
  }).as(aliases.consultarOfertaCheckout);
}

export function respostaContratacao({
  situacaoPagamento = valoresSistema.pagamento.aprovado,
  plano,
  emailConfirmacao = null,
}) {
  const aprovado = situacaoPagamento === valoresSistema.pagamento.aprovado;
  return {
    contratoId: valoresCdu.contrato,
    pagamentoId: valoresCdu.pagamento,
    planoId: plano.id,
    nomePlano: plano.nome,
    situacaoContrato: aprovado ? valoresSistema.pagamento.ativo : valoresSistema.pagamento.encerrado,
    situacaoPagamento,
    modoPagamento: valoresSistema.pagamento.simulado,
    valorCentavos: plano.precoCentavos,
    moeda: plano.moeda,
    expiraEm: aprovado ? datasCypress.expiracaoContrato : null,
    reprocessada: false,
    emailConfirmacao,
  };
}
