import {
  prepararCadastroEVinculoInstitucional,
  abrirGestaoInstitucional,
  informarNomeDoAlunoInstitucional,
  informarEmailDoAlunoInstitucional,
  informarCpfDoAlunoInstitucional,
  informarTelefoneDoAlunoInstitucional,
  informarEnderecoDoAlunoInstitucional,
  informarSenhaDoAlunoInstitucional,
  confirmarSenhaDoAlunoInstitucional,
  enviarCadastroDoAlunoInstitucional,
  conferirCadastroEVinculoInstitucional,
  removerVinculoInstitucional,
  conferirVinculoRemovido,
  prepararVinculoDeAlunoExistente,
  abrirGestaoParaVinculoExistente,
  informarEmailDoAlunoExistente,
  confirmarVinculoDoAlunoExistente,
  conferirVinculoDoAlunoExistente,
} from "../funcoes/cdu.js";

describe("CDU-13 - Gerenciar alunos vinculados", () => {
  it("cadastra um aluno dentro da capacidade e depois encerra o vínculo", () => {
    prepararCadastroEVinculoInstitucional();
    abrirGestaoInstitucional();
    informarNomeDoAlunoInstitucional();
    informarEmailDoAlunoInstitucional();
    informarCpfDoAlunoInstitucional();
    informarTelefoneDoAlunoInstitucional();
    informarEnderecoDoAlunoInstitucional();
    informarSenhaDoAlunoInstitucional();
    confirmarSenhaDoAlunoInstitucional();
    enviarCadastroDoAlunoInstitucional();
    conferirCadastroEVinculoInstitucional();
    removerVinculoInstitucional();
    conferirVinculoRemovido();
  });
  it("vincula uma conta de aluno existente pelo e-mail", () => {
    prepararVinculoDeAlunoExistente();
    abrirGestaoParaVinculoExistente();
    informarEmailDoAlunoExistente();
    confirmarVinculoDoAlunoExistente();
    conferirVinculoDoAlunoExistente();
  });
});
