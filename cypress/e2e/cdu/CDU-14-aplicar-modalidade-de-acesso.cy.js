import {
  prepararCatalogoParaAlunoSemAcesso,
  acessarTreinoComoAlunoSemPlano,
  conferirRedirecionamentoParaOfertas,
  prepararInstituicaoSemVinculos,
  acessarTreinoComoInstituicao,
  conferirRedirecionamentoParaInstituicao,
} from "../funcoes/cdu.js";

describe("CDU-14 - Aplicar acesso conforme modalidade", () => {
  it("redireciona instituição para sua área quando tenta abrir treino de aluno", () => {
    prepararInstituicaoSemVinculos();
    acessarTreinoComoInstituicao();
    conferirRedirecionamentoParaInstituicao();
  });
  it("encaminha aluno sem acesso vigente ao catálogo em vez de abrir o treino", () => {
    prepararCatalogoParaAlunoSemAcesso();
    acessarTreinoComoAlunoSemPlano();
    conferirRedirecionamentoParaOfertas();
  });
});
