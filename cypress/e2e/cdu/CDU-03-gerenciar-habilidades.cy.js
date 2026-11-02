import { valoresCdu } from "../funcoes/constantes.js";
import {
  habilidadeTeste,
  prepararTelaDeHabilidades,
  prepararCadastroDeHabilidade,
  abrirCadastroDeHabilidade,
  informarNomeDaHabilidadeCdu,
  informarCategoriaDaHabilidadeCdu,
  selecionarNivelDaHabilidadeCdu,
  confirmarCadastroDaHabilidadeCdu,
  conferirCadastroDeHabilidade,
  prepararEdicaoDeHabilidade,
  abrirEdicaoDeHabilidade,
  alterarNomeDaHabilidadeCdu,
  confirmarEdicaoDaHabilidadeCdu,
  conferirEdicaoDeHabilidade,
  prepararRemocaoDeHabilidade,
  solicitarRemocaoDeHabilidade,
  confirmarRemocaoDeHabilidade,
  conferirRemocaoDeHabilidade,
} from "../funcoes/cdu.js";

describe("CDU-03 - Gerenciar habilidades", () => {
  it("cadastra uma habilidade no perfil", () => {
    prepararTelaDeHabilidades();
    prepararCadastroDeHabilidade();
    abrirCadastroDeHabilidade();
    informarNomeDaHabilidadeCdu();
    informarCategoriaDaHabilidadeCdu();
    selecionarNivelDaHabilidadeCdu();
    confirmarCadastroDaHabilidadeCdu();
    conferirCadastroDeHabilidade();
  });

  it("edita uma habilidade cadastrada", () => {
    prepararTelaDeHabilidades([habilidadeTeste]);
    prepararEdicaoDeHabilidade();
    abrirEdicaoDeHabilidade();
    alterarNomeDaHabilidadeCdu();
    confirmarEdicaoDaHabilidadeCdu();
    conferirEdicaoDeHabilidade();
  });

  it("remove uma habilidade do perfil", () => {
    prepararTelaDeHabilidades([
      { ...habilidadeTeste, nome: valoresCdu.habilidadeAtualizada },
    ]);
    prepararRemocaoDeHabilidade();
    solicitarRemocaoDeHabilidade();
    confirmarRemocaoDeHabilidade();
    conferirRemocaoDeHabilidade();
  });
});
