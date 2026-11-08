import {
  prepararVinculoDeAlunoExistente,
  prepararVinculoDeAlunoJaVinculado,
  abrirGestaoParaVinculoExistente,
  informarEmailDoAlunoExistente,
  confirmarVinculoDoAlunoExistente,
  conferirVinculoDoAlunoExistente,
  conferirRejeicaoDoAlunoJaVinculado,
} from "../funcoes/cdu.js";

describe("CDU-20 - Vincular aluno com conta existente", () => {
  it("vincula a conta pelo e-mail e atualiza as vagas disponíveis", () => {
    prepararVinculoDeAlunoExistente();
    abrirGestaoParaVinculoExistente();
    informarEmailDoAlunoExistente();
    confirmarVinculoDoAlunoExistente();
    conferirVinculoDoAlunoExistente();
  });

  it("mantém a lista e a capacidade quando o aluno já possui vínculo ativo", () => {
    prepararVinculoDeAlunoJaVinculado();
    abrirGestaoParaVinculoExistente();
    informarEmailDoAlunoExistente();
    confirmarVinculoDoAlunoExistente();
    conferirRejeicaoDoAlunoJaVinculado();
  });
});
