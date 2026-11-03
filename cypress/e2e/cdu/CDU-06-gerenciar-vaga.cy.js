import { valoresCdu } from "../funcoes/constantes.js";
import {
  vagaTeste,
  prepararTelaDeVagas,
  prepararCadastroDeVaga,
  abrirCadastroDeVaga,
  informarTituloDaVagaCdu,
  informarEmpresaDaVagaCdu,
  selecionarNivelDaVagaCdu,
  informarRequisitosDaVagaCdu,
  confirmarCadastroDaVagaCdu,
  conferirCadastroDeVaga,
  prepararEdicaoDeVaga,
  abrirEdicaoDeVaga,
  alterarTituloDaVagaCdu,
  confirmarEdicaoDaVagaCdu,
  conferirEdicaoDeVaga,
  prepararExclusaoDeVaga,
  solicitarExclusaoDeVaga,
  confirmarExclusaoDeVaga,
  conferirExclusaoDeVaga,
} from "../funcoes/cdu.js";

describe("CDU-06 - Gerenciar vaga de interesse", () => {
  it("cadastra uma vaga com seus requisitos", () => {
    prepararTelaDeVagas();
    prepararCadastroDeVaga();
    abrirCadastroDeVaga();
    informarTituloDaVagaCdu();
    informarEmpresaDaVagaCdu();
    selecionarNivelDaVagaCdu();
    informarRequisitosDaVagaCdu();
    confirmarCadastroDaVagaCdu();
    conferirCadastroDeVaga();
  });

  it("edita uma vaga cadastrada", () => {
    prepararTelaDeVagas([vagaTeste]);
    prepararEdicaoDeVaga();
    abrirEdicaoDeVaga();
    alterarTituloDaVagaCdu();
    confirmarEdicaoDaVagaCdu();
    conferirEdicaoDeVaga();
  });

  it("arquiva uma vaga", () => {
    prepararTelaDeVagas([{ ...vagaTeste, titulo: valoresCdu.vagaAtualizada }]);
    prepararExclusaoDeVaga();
    solicitarExclusaoDeVaga();
    confirmarExclusaoDeVaga();
    conferirExclusaoDeVaga();
  });
});
