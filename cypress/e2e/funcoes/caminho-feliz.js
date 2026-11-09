import {
  classes,
  dadosCaminhoFeliz,
  rotas,
  seletores,
  textos,
} from "./constantes.js";

export const habilidadeDoCaminho = dadosCaminhoFeliz.habilidade;
export const vagaDoCaminho = dadosCaminhoFeliz.vaga;
export const respostasDoTreino = dadosCaminhoFeliz.respostasTreino;
let mensagensAnteriores = new Set();

export function marcarCaixaMailpit() {
  cy.request("http://127.0.0.1:8025/api/v1/messages").then(({ body }) => {
    mensagensAnteriores = new Set(body.messages.map((item) => item.ID));
  });
}

export function conferirEmailNoMailpit(destinatario, assunto, trechoEsperado) {
  cy.request("http://127.0.0.1:8025/api/v1/messages").then(({ body }) => {
    const mensagem = body.messages.find((item) =>
      !mensagensAnteriores.has(item.ID) &&
      item.Subject === assunto &&
      item.To.some((alvo) => alvo.Address === destinatario)
    );
    expect(mensagem, `e-mail ${assunto} para ${destinatario} no Mailpit`).to.exist;
    cy.request(`http://127.0.0.1:8025/api/v1/message/${mensagem.ID}`).then(({ body: email }) => {
      expect(email.Subject).to.eq(assunto);
      expect(email.To.some((alvo) => alvo.Address === destinatario)).to.be.true;
      expect(email.Text).to.be.a("string").and.not.be.empty;
      if (trechoEsperado) expect(email.Text).to.include(trechoEsperado);
    });
  });
}

export function visitarCadastroDoAluno() {
  cy.visit(rotas.cadastroAluno);
}

export function abrirRecuperacaoDeSenhaAposCheckout(email) {
  cy.get(".checkout-logout").click();
  cy.visit(rotas.login);
  cy.contains("button", "Esqueci minha senha").click();
  cy.get(seletores.login.email).type(email);
}

export function solicitarRecuperacaoDeSenha() {
  cy.contains("button", "Enviar instruções").click();
  cy.contains("Se este e-mail estiver cadastrado").should("be.visible");
}

export function conferirFormularioDeCadastroDoAluno() {
  cy.contains("h2", textos.cadastro.titulo).should("be.visible");
}

export function informarNomeDoAluno(nome) {
  cy.get(seletores.cadastro.nome).type(nome);
}

export function informarEmailDoAluno(email) {
  cy.get(seletores.cadastro.email).type(email);
}

export function informarCpfDoAluno(cpf) {
  cy.get(seletores.cadastro.cpf).type(cpf);
}

export function informarTelefoneDoAluno(telefone) {
  cy.get(seletores.cadastro.telefone).type(telefone);
}

export function informarEnderecoDoAluno(endereco) {
  cy.get(seletores.cadastro.endereco).type(endereco);
}

export function informarSenhaDeCadastro(senha) {
  cy.get(seletores.cadastro.senha).type(senha);
}

export function confirmarSenhaDeCadastro(senha) {
  cy.get(seletores.cadastro.confirmarSenha).type(senha);
}

export function enviarCadastroDoAluno() {
  cy.contains("button", textos.cadastro.botaoCriar).click();
}

export function conferirCadastroDoAlunoConcluido() {
  cy.contains(`${classes.confirmacaoCadastro} h2`, textos.cadastro.sucesso, {
    timeout: 30000,
  }).should("be.visible");
}

export function continuarDoCadastroParaOfertas() {
  cy.contains("button", textos.cadastro.continuar).click();
}

export function conferirOfertasDoAluno() {
  cy.location("pathname").should("eq", rotas.planos);
  cy.contains("a", textos.plano.ativarAcesso).should("be.visible");
  cy.contains(classes.cartaoPlano, textos.plano.acessoIndividual)
    .contains("a", textos.plano.continuarPagamento)
    .should("be.visible");
}

export function selecionarOfertaIndividual() {
  cy.contains(classes.cartaoPlano, textos.plano.acessoIndividual)
    .contains("a", textos.plano.continuarPagamento)
    .click();
}

export function conferirCheckoutIndividual() {
  cy.location("pathname").should("match", rotas.checkoutPadrao);
  cy.contains("h1", textos.plano.tituloCheckout).should("be.visible");
}

export function preencherCartaoAprovado() {
  cy.contains("button", textos.plano.cartaoAprovado).click();
}

export function enviarPagamentoSimulado() {
  cy.contains("button", textos.plano.simularPagamento).click();
}

export function conferirPagamentoAprovado() {
  cy.contains("h2", textos.plano.aprovado, { timeout: 30000 }).should(
    "be.visible",
  );
  cy.contains(textos.plano.semCobranca).should("be.visible");
}

export function irParaAreaDoAluno() {
  cy.contains("a", textos.plano.irParaArea).click();
}

export function conferirAreaDoAluno(nome) {
  cy.location("pathname").should("eq", rotas.inicioAluno);
  cy.contains(textos.areaAluno.saudacao(nome)).should("be.visible");
}

export function abrirEdicaoDosDadosDaConta() {
  cy.get(seletores.navegacao.perfilConta).click();
}

export function conferirTelaDeEdicaoDosDadosDoAluno() {
  cy.location("pathname").should("eq", rotas.conta);
  cy.contains("h1", textos.conta.tituloAluno).should("be.visible");
}

export function alterarTelefoneDoAluno(telefone) {
  cy.get(seletores.conta.telefone).clear().type(telefone);
}

export function alterarEnderecoDoAluno(endereco) {
  cy.get(seletores.conta.endereco).clear().type(endereco);
}

export function salvarDadosDaConta() {
  cy.contains("button", textos.conta.salvar).click();
}

export function conferirDadosDoAlunoSalvos(telefone, endereco) {
  cy.contains(textos.conta.atualizada, { timeout: 30000 }).should("be.visible");
  cy.get(seletores.conta.telefone).should("have.value", telefone);
  cy.get(seletores.conta.endereco).should("have.value", endereco);
}

export function expandirMenuDesenvolvimento() {
  cy.contains("summary", textos.navegacao.desenvolvimento).then(($summary) => {
    if (!$summary.parent("details").prop("open")) {
      cy.wrap($summary).click();
    }
  });
}

export function abrirMinhasHabilidades() {
  cy.contains(seletores.navegacao.linksDesenvolvimento, textos.habilidades.tituloPagina).click();
}

export function conferirTelaMinhasHabilidades() {
  cy.contains("h1", textos.habilidades.tituloPagina).should("be.visible");
}

export function abrirFormularioDeHabilidade() {
  cy.contains("button", textos.habilidades.adicionar).click();
}

export function informarNomeDaHabilidade(nome = habilidadeDoCaminho.nome) {
  cy.get(seletores.habilidade.nome).type(nome);
}

export function informarCategoriaDaHabilidade(
  categoria = habilidadeDoCaminho.categoria,
) {
  cy.get(seletores.habilidade.categoria).type(categoria);
}

export function selecionarNivelDaHabilidade(
  nivel = habilidadeDoCaminho.nivel,
) {
  cy.get(seletores.habilidade.nivel).select(nivel);
}

export function confirmarCadastroDaHabilidade() {
  cy.get(seletores.habilidade.dialogo)
    .contains("button", textos.habilidades.salvar)
    .click();
}

export function conferirHabilidadeCadastrada(
  habilidade = habilidadeDoCaminho,
) {
  cy.contains(classes.cartaoHabilidade, habilidade.nome).should("be.visible");
  cy.contains(classes.cartaoHabilidade, habilidade.categoria).should("be.visible");
}

export function abrirVagasDeInteresse() {
  cy.contains(seletores.navegacao.linksDesenvolvimento, textos.vagas.tituloPagina).click();
}

export function conferirTelaDeVagasDeInteresse() {
  cy.contains("h1", textos.vagas.tituloPagina).should("be.visible");
}

export function abrirFormularioDeVaga() {
  cy.contains("button", textos.vagas.adicionar).click();
}

export function informarTituloDaVaga(titulo = vagaDoCaminho.titulo) {
  cy.get(seletores.vaga.titulo).type(titulo);
}

export function informarEmpresaDaVaga(empresa = vagaDoCaminho.empresa) {
  cy.get(seletores.vaga.empresa).type(empresa);
}

export function selecionarNivelDaVaga(nivel = vagaDoCaminho.nivel) {
  cy.get(seletores.vaga.nivel).select(nivel);
}

export function informarRequisitosDaVaga(
  requisitos = vagaDoCaminho.requisitos,
) {
  cy.get(seletores.vaga.requisitos).type(requisitos);
}

export function confirmarCadastroDaVaga() {
  cy.get(seletores.vaga.dialogo)
    .contains("button", textos.vagas.salvar)
    .click();
}

export function conferirVagaCadastrada(vaga = vagaDoCaminho) {
  cy.contains(classes.cartaoVaga, vaga.titulo)
    .should("contain", vaga.empresa)
    .and("be.visible");
}

export function abrirTelaDeTreinos() {
  cy.contains(seletores.navegacao.iniciarTreinamento, textos.treino.iniciar).click();
}

export function conferirTelaDePreparacaoDoTreino() {
  cy.location("pathname").should("eq", rotas.treinos);
  cy.contains("h1", textos.treino.tituloPreparacao).should("be.visible");
}

export function selecionarModoTreinoPorVaga() {
  cy.contains(seletores.treino.cartaoContexto, textos.treino.modoVaga)
    .find(seletores.treino.modoVaga)
    .check({ force: true })
    .should("be.checked");
}

export function selecionarVagaDoTreino(vaga = vagaDoCaminho) {
  cy.contains(seletores.vaga.opcaoTreino, vaga.titulo)
    .find(seletores.treino.radioVaga)
    .check({ force: true })
    .should("be.checked");
}

export function conferirContextoDaVagaNoTreino(vaga = vagaDoCaminho) {
  cy.contains(
    classes.resumoTreino,
    textos.historico.contextoResumo(vaga.titulo, vaga.empresa),
  ).should("be.visible");
}

export function iniciarEntrevistaComIa() {
  cy.contains("button", textos.treino.iniciarEntrevista).click();
}

export function conferirPerguntaDaIa(numero) {
  cy.contains(seletores.treino.progressoPergunta, textos.treino.pergunta(numero), {
    timeout: 120000,
  }).should("be.visible");
  cy.get(seletores.treino.mensagem).last().find("p").should(($pergunta) => {
    expect($pergunta.text().trim()).not.to.be.empty;
  });
}

export function digitarRespostaDoTreino(resposta) {
  cy.get(seletores.treino.resposta).should("be.visible").type(resposta);
}

export function enviarRespostaDoTreino() {
  cy.contains("button", textos.treino.enviarResposta).click();
}

export function conferirTreinoProntoParaFinalizar() {
  cy.contains(seletores.treino.finalizar, textos.treino.finalizar, {
    timeout: 120000,
  }).should("be.enabled");
}

export function finalizarTreino() {
  cy.contains(seletores.treino.finalizar, textos.treino.finalizar).click();
}

export function conferirFeedbackDoTreino(vaga = vagaDoCaminho) {
  cy.location("pathname", { timeout: 30000 }).should("eq", rotas.feedback);
  cy.contains("h1", textos.feedback.titulo).should("be.visible");
  cy.contains(
    seletores.treino.metaFeedback,
    textos.historico.contextoResumo(vaga.titulo, vaga.empresa),
  ).should("be.visible");
  cy.get(seletores.treino.nota).invoke("text").should("match", /^\d{1,3}$/);
}

export function abrirHistoricoPeloFeedback() {
  cy.contains("button", textos.treino.historico).click();
}

export function conferirTelaDeHistoricoDeTreinos() {
  cy.location("pathname").should("eq", rotas.historico);
  cy.contains("h1", textos.historico.tituloPagina).should("be.visible");
}

export function conferirTreinoNoHistorico(vaga = vagaDoCaminho) {
  const tituloTreino = textos.historico.tituloEntrevistaVaga(vaga.titulo);
  const contextoTreino = textos.historico.contextoVaga(vaga.titulo, vaga.empresa);
  cy.contains(seletores.historico.linhaTreino, tituloTreino)
    .should("contain", contextoTreino)
    .find(seletores.historico.notaTreino)
    .invoke("text")
    .should("match", /\d+/);
}

export function abrirTreinoPeloHistorico(vaga = vagaDoCaminho) {
  cy.contains(
    seletores.historico.linhaTreino,
    textos.historico.tituloEntrevistaVaga(vaga.titulo),
  ).click();
}

export function conferirFeedbackAbertoPeloHistorico(vaga = vagaDoCaminho) {
  cy.location("pathname").should("eq", rotas.feedback);
  cy.contains("h1", textos.feedback.titulo).should("be.visible");
  cy.contains(
    seletores.treino.metaFeedback,
    textos.historico.contextoVaga(vaga.titulo, vaga.empresa),
  ).should("be.visible");
}
