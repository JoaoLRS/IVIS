import {
  objetoInstituicaoCdu,
  rotas,
  valoresSistema,
} from "../funcoes/constantes.js";
import { alunoCdu, abrirComoAluno } from "../funcoes/apoio.js";
import { conferirPlanoVigenteInstitucional } from "../funcoes/cdu.js";
import { abrirDetalhesDoPlano, conferirDetalhesDoPlano, conferirAusenciaDePlano } from "../funcoes/acesso-cdu.js";

describe("CDU-05 - Aplicar acesso institucional vigente à sessão", () => {
  it("mostra validade e benefícios do plano vigente", () => {
    abrirDetalhesDoPlano();
    conferirDetalhesDoPlano();
  });
  it("permite consultar a situação mesmo sem plano ativo", () => {
    abrirDetalhesDoPlano({ semPlano: true });
    conferirAusenciaDePlano();
  });
  it("encaminha o aluno à área permitida pelo acesso institucional da sessão", () => {
    abrirComoAluno(rotas.inicioAluno, {
      conta: { ...alunoCdu, objetoInstituicao: objetoInstituicaoCdu },
      origemAcesso: valoresSistema.modalidade.institucional,
    });
    conferirPlanoVigenteInstitucional();
  });
});
