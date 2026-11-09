import { dadosTeste } from "../funcoes/constantes.js";
import {
  abrirRecuperacaoDeSenhaAposCheckout,
  conferirCadastroDoAlunoConcluido,
  conferirEmailNoMailpit,
  conferirPagamentoAprovado,
  confirmarSenhaDeCadastro,
  continuarDoCadastroParaOfertas,
  enviarCadastroDoAluno,
  enviarPagamentoSimulado,
  informarCpfDoAluno,
  informarEmailDoAluno,
  informarEnderecoDoAluno,
  informarNomeDoAluno,
  informarSenhaDeCadastro,
  informarTelefoneDoAluno,
  marcarCaixaMailpit,
  preencherCartaoAprovado,
  selecionarOfertaIndividual,
  solicitarRecuperacaoDeSenha,
  visitarCadastroDoAluno,
} from "../funcoes/caminho-feliz.js";

describe("entrega de e-mails ao Mailpit", { retries: 0 }, () => {
  it("recebe boas-vindas, confirmação de contratação e recuperação de senha", () => {
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
    selecionarOfertaIndividual();
    preencherCartaoAprovado();
    marcarCaixaMailpit();
    enviarPagamentoSimulado();
    conferirPagamentoAprovado();
    conferirEmailNoMailpit(dadosTeste.aluno.email, "Confirmação de contratação simulada — IVIS", "Nenhum valor foi cobrado");

    abrirRecuperacaoDeSenhaAposCheckout(dadosTeste.aluno.email);
    marcarCaixaMailpit();
    solicitarRecuperacaoDeSenha();
    conferirEmailNoMailpit(dadosTeste.aluno.email, "Redefinição de senha — IVIS", "Use o link a seguir");
  });
});
