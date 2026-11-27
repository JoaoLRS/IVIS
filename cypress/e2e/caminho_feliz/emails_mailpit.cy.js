import { dadosCaminhoFeliz, dadosTeste } from "../funcoes/constantes.js";
import {
  abrirRecuperacaoDeSenhaAposCheckout,
  conferirCadastroDoAlunoConcluido,
  conferirEmailNoMailpit,
  conferirLoginComNovaSenha,
  conferirPagamentoAprovado,
  conferirSenhaAntigaRecusada,
  confirmarSenhaDeCadastro,
  confirmarNovaSenha,
  continuarDoCadastroParaOfertas,
  enviarCadastroDoAluno,
  enviarPagamentoSimulado,
  guardarCodigoRecuperacaoDoMailpit,
  informarCpfDoAluno,
  informarCredenciaisDeLogin,
  informarCodigoDeRecuperacao,
  informarEmailDoAluno,
  informarEnderecoDoAluno,
  informarNomeDoAluno,
  informarNovaSenha,
  informarSenhaDeCadastro,
  informarTelefoneDoAluno,
  marcarCaixaMailpit,
  preencherCartaoAprovado,
  selecionarOfertaIndividual,
  solicitarRecuperacaoDeSenha,
  tentarAutenticacao,
  validarCodigoDeRecuperacao,
  visitarCadastroDoAluno,
  voltarAoLoginAposRedefinicao,
  voltarParaRedefinicaoDeSenha,
} from "../funcoes/caminho-feliz.js";

describe("entrega de e-mails ao Mailpit", { testIsolation: false, retries: 0 }, () => {
  it("cadastra o aluno e recebe as boas-vindas", () => {
    visitarCadastroDoAluno();
    informarNomeDoAluno(dadosTeste.aluno.nome);
    informarEmailDoAluno(dadosTeste.aluno.email);
    informarCpfDoAluno(dadosTeste.aluno.cpf);
    informarTelefoneDoAluno(dadosTeste.aluno.telefone);
    informarEnderecoDoAluno(dadosTeste.aluno.endereco);
    informarSenhaDeCadastro(dadosTeste.senha);
    confirmarSenhaDeCadastro(dadosTeste.senha);
    marcarCaixaMailpit();
    enviarCadastroDoAluno();
    conferirCadastroDoAlunoConcluido();
    conferirEmailNoMailpit(dadosTeste.aluno.email, "Bem-vindo ao IVIS", "Sua conta IVIS foi criada");
    continuarDoCadastroParaOfertas();
  });

  it("contrata o plano e recebe a confirmação", () => {
    selecionarOfertaIndividual();
    preencherCartaoAprovado();
    marcarCaixaMailpit();
    enviarPagamentoSimulado();
    conferirPagamentoAprovado();
    conferirEmailNoMailpit(dadosTeste.aluno.email, "Confirmação de contratação simulada — IVIS", "Nenhum valor foi cobrado");
  });

  it("solicita a recuperação e lê o código no Mailpit", () => {
    abrirRecuperacaoDeSenhaAposCheckout(dadosTeste.aluno.email);
    solicitarRecuperacaoDeSenha(dadosTeste.aluno.email);
    guardarCodigoRecuperacaoDoMailpit(dadosTeste.aluno.email);
  });

  it("valida o código e redefine a senha", () => {
    voltarParaRedefinicaoDeSenha(dadosTeste.aluno.email);
    informarCodigoDeRecuperacao();
    validarCodigoDeRecuperacao();
    informarNovaSenha();
    confirmarNovaSenha();
  });

  it("recusa a senha antiga após a redefinição", () => {
    voltarAoLoginAposRedefinicao();
    informarCredenciaisDeLogin(dadosTeste.aluno.email, dadosTeste.senha);
    tentarAutenticacao();
    conferirSenhaAntigaRecusada();
  });

  it("autentica o aluno com a senha nova", () => {
    informarCredenciaisDeLogin(dadosTeste.aluno.email, dadosCaminhoFeliz.senhaRedefinida);
    tentarAutenticacao();
    conferirLoginComNovaSenha(dadosTeste.aluno.nome);
  });
});
