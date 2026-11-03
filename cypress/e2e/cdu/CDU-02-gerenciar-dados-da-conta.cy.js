import { abrirTrocaDeSenha, informarSenhasDaConta, confirmarTrocaDeSenha, conferirSenhaAlterada } from "../funcoes/acesso-cdu.js";
import {
  abrirContaDoAlunoVinculado,
  prepararEdicaoDaContaDoAluno,
  alterarNomeDaContaAluno,
  alterarEmailDaContaAluno,
  alterarCpfDaContaAluno,
  alterarTelefoneDaContaAluno,
  alterarEnderecoDaContaAluno,
  conferirInstituicaoDaContaPreservada,
  salvarDadosDaContaEditada,
  conferirContaAlunoAtualizada,
  abrirContaDaInstituicao,
  prepararEdicaoDaContaDaInstituicao,
  alterarResponsavelDaContaInstituicao,
  alterarNomeDaContaInstituicao,
  alterarEmailDaContaInstituicao,
  alterarCnpjDaContaInstituicao,
  alterarTelefoneDaContaInstituicao,
  alterarEnderecoDaContaInstituicao,
  conferirContaInstituicaoAtualizada,
} from "../funcoes/conta-cdu.js";

describe("CDU-02 - Gerenciar dados da conta", () => {
  it("abre a página de senha, altera a credencial e limpa os campos sensíveis", () => {
    abrirTrocaDeSenha();
    informarSenhasDaConta();
    confirmarTrocaDeSenha();
    conferirSenhaAlterada();
  });
  it("edita os dados pessoais do aluno e preserva o código da instituição", () => {
    abrirContaDoAlunoVinculado();
    prepararEdicaoDaContaDoAluno();
    alterarNomeDaContaAluno();
    alterarEmailDaContaAluno();
    alterarCpfDaContaAluno();
    alterarTelefoneDaContaAluno();
    alterarEnderecoDaContaAluno();
    conferirInstituicaoDaContaPreservada();
    salvarDadosDaContaEditada();
    conferirContaAlunoAtualizada();
  });

  it("edita nome do responsável, instituição, CNPJ e dados de contato", () => {
    abrirContaDaInstituicao();
    prepararEdicaoDaContaDaInstituicao();
    alterarResponsavelDaContaInstituicao();
    alterarNomeDaContaInstituicao();
    alterarEmailDaContaInstituicao();
    alterarCnpjDaContaInstituicao();
    alterarTelefoneDaContaInstituicao();
    alterarEnderecoDaContaInstituicao();
    salvarDadosDaContaEditada();
    conferirContaInstituicaoAtualizada();
  });
});
