import { dadosTeste } from "../funcoes/constantes.js";
import {
  prepararVerificacaoDeAuditoriaDaCompra,
  conferirAuditoriaDaCompraPersistida,
} from "../funcoes/cdu.js";
import {
  conferirCadastroDoAlunoConcluido,
  conferirCheckoutIndividual,
  conferirOfertasDoAluno,
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
  preencherCartaoAprovado,
  selecionarOfertaIndividual,
  visitarCadastroDoAluno,
} from "../funcoes/caminho-feliz.js";

describe("CDU-15 - Registrar auditoria de operações sensíveis", () => {
  it("registra no banco a contratação simulada aprovada", () => {
    visitarCadastroDoAluno();
    informarNomeDoAluno(dadosTeste.aluno.nome);
    informarEmailDoAluno(dadosTeste.aluno.email);
    informarCpfDoAluno(dadosTeste.aluno.cpf);
    informarTelefoneDoAluno(dadosTeste.aluno.telefone);
    informarEnderecoDoAluno(dadosTeste.aluno.endereco);
    informarSenhaDeCadastro(dadosTeste.senha);
    confirmarSenhaDeCadastro(dadosTeste.senha);
    enviarCadastroDoAluno();
    conferirCadastroDoAlunoConcluido();
    continuarDoCadastroParaOfertas();
    conferirOfertasDoAluno();
    selecionarOfertaIndividual();
    conferirCheckoutIndividual();

    prepararVerificacaoDeAuditoriaDaCompra();
    preencherCartaoAprovado();
    enviarPagamentoSimulado();
    conferirPagamentoAprovado();
    conferirAuditoriaDaCompraPersistida();
  });
});
