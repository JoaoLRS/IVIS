import {
  prepararContratacaoInstitucional,
  abrirCheckoutInstitucional,
  conferirOfertaInstitucional,
  preencherCartaoInstitucionalAprovado,
  enviarPagamentoInstitucional,
  conferirContratacaoInstitucional,
} from "../funcoes/cdu.js";

describe("CDU-11 - Contratar plano institucional", () => {
  it("ativa a oferta institucional e exibe a capacidade incluída", () => {
    prepararContratacaoInstitucional();
    abrirCheckoutInstitucional();
    conferirOfertaInstitucional();
    preencherCartaoInstitucionalAprovado();
    enviarPagamentoInstitucional();
    conferirContratacaoInstitucional();
  });
});
