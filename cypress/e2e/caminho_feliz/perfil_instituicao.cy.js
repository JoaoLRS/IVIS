import { dadosTeste, rotas } from "../funcoes/constantes.js";
import { preencherCadastroInstituicao } from "../funcoes/funcoes.js";
import {
  conferirCadastroDoAlunoConcluido,
  conferirEmailNoMailpit,
  continuarDoCadastroParaOfertas,
  preencherCartaoAprovado,
  enviarPagamentoSimulado,
  conferirPagamentoAprovado,
  irParaAreaDoAluno,
  marcarCaixaMailpit,
} from "../funcoes/caminho-feliz.js";
import {
  autenticarNaJornadaDaInstituicao,
  cadastrarAlunoDaInstituicao,
  conferirAlunoDaInstituicao,
  conferirAreaDaInstituicao,
  conferirOfertasDaInstituicao,
  conferirSaudacaoDoAlunoVinculado,
  conferirVinculoRemovido,
  enviarCadastroDaInstituicao,
  recarregarAreaDaInstituicao,
  removerAlunoDaInstituicao,
  selecionarOfertaInstitucional,
  visitarCadastroDaInstituicao,
} from "../funcoes/instituicao.js";

describe("jornada da instituição e do aluno vinculado", { testIsolation: false, retries: 0 }, () => {
  it("cadastra a conta real da instituição e segue para as ofertas", () => {
    visitarCadastroDaInstituicao();
    preencherCadastroInstituicao({ ...dadosTeste.instituicao, senha: dadosTeste.senha });
    marcarCaixaMailpit();
    enviarCadastroDaInstituicao();
    conferirCadastroDoAlunoConcluido();
    conferirEmailNoMailpit(dadosTeste.instituicao.email, "Bem-vindo ao IVIS");
    continuarDoCadastroParaOfertas();
    conferirOfertasDaInstituicao();
  });

  it("contrata o plano institucional e confere as vagas disponíveis", () => {
    selecionarOfertaInstitucional();
    preencherCartaoAprovado();
    marcarCaixaMailpit();
    enviarPagamentoSimulado();
    conferirPagamentoAprovado();
    conferirEmailNoMailpit(dadosTeste.instituicao.email, "Confirmação de contratação simulada — IVIS");
    irParaAreaDoAluno();
    conferirAreaDaInstituicao();
  });

  it("cadastra e vincula um aluno, preservando o vínculo ao recarregar", () => {
    marcarCaixaMailpit();
    cadastrarAlunoDaInstituicao();
    conferirEmailNoMailpit(dadosTeste.aluno.email, "Bem-vindo ao IVIS");
    conferirAlunoDaInstituicao();
    recarregarAreaDaInstituicao();
    conferirAlunoDaInstituicao();
  });

  it("encerra a sessão e autentica o aluno com acesso institucional", () => {
    autenticarNaJornadaDaInstituicao(dadosTeste.aluno.email, rotas.inicioAluno);
    conferirSaudacaoDoAlunoVinculado();
  });

  it("no fluxo complementar, retorna à instituição, remove o vínculo e libera a vaga", () => {
    autenticarNaJornadaDaInstituicao(dadosTeste.instituicao.email, rotas.instituicao);
    removerAlunoDaInstituicao();
    recarregarAreaDaInstituicao();
    conferirVinculoRemovido();
  });
});
