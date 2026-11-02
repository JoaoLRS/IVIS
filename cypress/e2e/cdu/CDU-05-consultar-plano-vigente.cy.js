import {
  objetoInstituicaoCdu,
  rotas,
  valoresSistema,
} from "../funcoes/constantes.js";
import { alunoCdu, abrirComoAluno } from "../funcoes/apoio.js";
import { conferirPlanoVigenteInstitucional } from "../funcoes/cdu.js";

describe("CDU-05 - Aplicar acesso institucional vigente à sessão", () => {
  it("encaminha o aluno à área permitida pelo acesso institucional da sessão", () => {
    abrirComoAluno(rotas.inicioAluno, {
      conta: { ...alunoCdu, objetoInstituicao: objetoInstituicaoCdu },
      origemAcesso: valoresSistema.modalidade.institucional,
    });
    conferirPlanoVigenteInstitucional();
  });
});
