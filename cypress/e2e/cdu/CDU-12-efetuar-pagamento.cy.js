import {
  prepararPagamentoRecusado,
  abrirCheckoutIndividualSemPlano,
  conferirOfertaDoCheckout,
  preencherCartaoRecusado,
  enviarPagamentoRecusado,
  conferirPagamentoRecusado,
} from "../funcoes/cdu.js";

describe("CDU-12 - Efetuar pagamento simulado", () => {
  it("processa o cenário recusado sem ativar o plano ou cobrar o usuário", () => {
    prepararPagamentoRecusado();
    abrirCheckoutIndividualSemPlano();
    conferirOfertaDoCheckout();
    preencherCartaoRecusado();
    enviarPagamentoRecusado();
    conferirPagamentoRecusado();
  });
});
