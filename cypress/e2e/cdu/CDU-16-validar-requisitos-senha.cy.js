import { senhasRequisitosCdu, textos } from "../funcoes/constantes.js";
import {
  abrirCadastroParaConferirSenha,
  informarSenhaParaConferirRequisitos,
  conferirQuantidadeDeRequisitosAtendidos,
  conferirRequisitoEmVerde,
  conferirRequisitoPendente,
} from "../funcoes/requisitos-senha.js";

describe("CDU-16 - Validar requisitos de senha", () => {
  it("inicia o cadastro com todos os requisitos pendentes", () => {
    abrirCadastroParaConferirSenha();
    conferirQuantidadeDeRequisitosAtendidos(0);
  });

  it("marca a letra maiúscula em verde ao digitá-la", () => {
    abrirCadastroParaConferirSenha();
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.maiuscula);
    conferirQuantidadeDeRequisitosAtendidos(1);
    conferirRequisitoEmVerde(textos.requisitosSenha.maiuscula);
    conferirRequisitoPendente(textos.requisitosSenha.tamanho);
  });

  it("marca a letra minúscula em verde ao digitá-la", () => {
    abrirCadastroParaConferirSenha();
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.maiusculaEMinuscula);
    conferirQuantidadeDeRequisitosAtendidos(2);
    conferirRequisitoEmVerde(textos.requisitosSenha.maiuscula);
    conferirRequisitoEmVerde(textos.requisitosSenha.minuscula);
  });

  it("marca número e caractere especial em verde", () => {
    abrirCadastroParaConferirSenha();
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.categoriasCompletas);
    conferirQuantidadeDeRequisitosAtendidos(4);
    conferirRequisitoEmVerde(textos.requisitosSenha.numero);
    conferirRequisitoEmVerde(textos.requisitosSenha.especial);
    conferirRequisitoPendente(textos.requisitosSenha.tamanho);
  });

  it("marca o tamanho em verde ao chegar a oito caracteres", () => {
    abrirCadastroParaConferirSenha();
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.valida);
    conferirQuantidadeDeRequisitosAtendidos(5);
    conferirRequisitoEmVerde(textos.requisitosSenha.tamanho);
  });

  it("retira a marcação da maiúscula quando ela é removida", () => {
    abrirCadastroParaConferirSenha();
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.valida);
    conferirQuantidadeDeRequisitosAtendidos(5);
    informarSenhaParaConferirRequisitos(senhasRequisitosCdu.semMaiuscula);
    conferirQuantidadeDeRequisitosAtendidos(3);
    conferirRequisitoPendente(textos.requisitosSenha.maiuscula);
  });
});
