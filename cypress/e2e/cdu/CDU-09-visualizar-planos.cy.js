import { rotas, textos } from "../funcoes/constantes.js";
import { prepararCatalogo, prepararVisitante } from "../funcoes/apoio.js";
import { visitar } from "../funcoes/funcoes.js";
import {
  conferirModalidadesDoCatalogo,
  selecionarOfertaInstitucionalDoCatalogo,
  conferirOfertaPreservadaNoCadastro,
} from "../funcoes/cdu.js";

describe("CDU-09 - Visualizar planos disponíveis", () => {
  it("mostra as modalidades públicas e preserva a oferta no cadastro", () => {
    prepararVisitante();
    prepararCatalogo();
    visitar(rotas.planos, textos.plano.tituloPagina);
    conferirModalidadesDoCatalogo();
    selecionarOfertaInstitucionalDoCatalogo();
    conferirOfertaPreservadaNoCadastro();
  });
});
