import {
  conferirOfertaDoCheckout,
  conferirPagamentoIndividualAprovado,
  abrirConfirmacaoDeEmailDoPagamento,
  conferirConfirmacaoDeEmailDoPagamento,
  prepararConfirmacaoDemoradaDoPlano,
  conferirAcessoAguardandoConfirmacaoDoPlano,
  aguardarConfirmacaoDoPlano,
} from "../funcoes/cdu.js";
import {
  preencherCartaoAprovado,
  enviarPagamentoSimulado,
  irParaAreaDoAluno,
  conferirAreaDoAluno,
} from "../funcoes/caminho-feliz.js";
import { alunoCdu, rotas } from "../funcoes/constantes.js";
import { abrirComoAluno, planoIndividualCdu } from "../funcoes/apoio.js";
import { prepararCheckoutPlanoIndividual } from "../funcoes/funcoes.js";

describe("CDU-10 - Contratar plano individual", () => {
  it("aguarda a confirmação do plano antes de liberar a entrada na área", () => {
    prepararCheckoutPlanoIndividual({ atualizarPlanoAposCompra: false });
    abrirComoAluno(rotas.checkout(planoIndividualCdu.id), {
      planoAtivo: false,
    });
    conferirOfertaDoCheckout();
    prepararConfirmacaoDemoradaDoPlano();
    preencherCartaoAprovado();
    enviarPagamentoSimulado();
    conferirAcessoAguardandoConfirmacaoDoPlano();
    aguardarConfirmacaoDoPlano();
    irParaAreaDoAluno();
    conferirAreaDoAluno(alunoCdu.nome);
  });
  it("aprova a oferta individual e confirma a ativação simulada", () => {
    prepararCheckoutPlanoIndividual({ atualizarPlanoAposCompra: false });
    abrirComoAluno(rotas.checkout(planoIndividualCdu.id), {
      planoAtivo: false,
    });
    conferirOfertaDoCheckout();
    preencherCartaoAprovado();
    enviarPagamentoSimulado();
    conferirPagamentoIndividualAprovado();
    abrirConfirmacaoDeEmailDoPagamento();
    conferirConfirmacaoDeEmailDoPagamento();
  });
});
