import {
  informarResponsavelDaInstituicao,
  informarNomeDaInstituicao,
  informarEmailDaInstituicao,
  informarCnpjDaInstituicao,
  informarTelefoneDaInstituicao,
  informarEnderecoDaInstituicao,
} from "../funcoes/cdu.js";
import {
  informarNomeDoAluno,
  informarEmailDoAluno,
  informarCpfDoAluno,
  informarTelefoneDoAluno,
  informarEnderecoDoAluno,
  informarSenhaDeCadastro,
  confirmarSenhaDeCadastro,
} from "../funcoes/caminho-feliz.js";
import { dadosTeste, rotas, textos } from "../funcoes/constantes.js";
import {
  cadastrarAluno,
  cadastrarInstituicao,
  continuarParaPlanos,
  prepararCaminhoFelizPerfilAluno,
  prepararCaminhoFelizPerfilInstituicao,
  prepararLoginPerfilAluno,
  prepararVisitanteSemSessao,
  prepararLogout,
  selecionarPerfil,
  visitar,
  verificarLoginAutomaticoAluno,
  verificarLoginAutomaticoInstituicao,
  login,
  sairDaConta,
} from "../funcoes/funcoes.js";
import {
  conferirHistoricoPrivadoDaConta,
  conferirRedirecionamentoAoLoginDoHistorico,
  contasIsolamentoCdu,
  prepararAcessoEntreContas,
  visitarHistoricoSemSessao,
} from "../funcoes/acesso-cdu.js";

describe("CDU-01 - Acesso IVIS", () => {
  it("cadastra aluno com os dados obrigatórios e apresenta as ofertas", () => {
    const aluno = dadosTeste.aluno;
    prepararCaminhoFelizPerfilAluno();
    visitar(
      rotas.cadastroAlunoComDestino(rotas.inicioAluno),
      textos.tituloCadastro,
    );
    informarNomeDoAluno(aluno.nome);
    informarEmailDoAluno(aluno.email);
    informarCpfDoAluno(aluno.cpf);
    informarTelefoneDoAluno(aluno.telefone);
    informarEnderecoDoAluno(aluno.endereco);
    informarSenhaDeCadastro(dadosTeste.senha);
    confirmarSenhaDeCadastro(dadosTeste.senha);
    cadastrarAluno(aluno);
    verificarLoginAutomaticoAluno(aluno);
    continuarParaPlanos();
  });

  it("cadastra uma instituição com responsável e dados de contato", () => {
    const instituicao = dadosTeste.instituicao;
    prepararCaminhoFelizPerfilInstituicao();
    visitar(
      rotas.cadastroInstituicaoComDestino(rotas.instituicao),
      textos.tituloCadastro,
    );
    selecionarPerfil(textos.perfilInstituicao);
    informarResponsavelDaInstituicao(instituicao.nomeResponsavel);
    informarNomeDaInstituicao(instituicao.nomeInstituicao);
    informarEmailDaInstituicao(instituicao.email);
    informarCnpjDaInstituicao(instituicao.cnpj);
    informarTelefoneDaInstituicao(instituicao.telefone);
    informarEnderecoDaInstituicao(instituicao.endereco);
    informarSenhaDeCadastro(dadosTeste.senha);
    confirmarSenhaDeCadastro(dadosTeste.senha);
    cadastrarInstituicao(instituicao);
    verificarLoginAutomaticoInstituicao(instituicao);
  });

  it("autentica uma conta existente e encerra a sessão", () => {
    const aluno = dadosTeste.aluno;
    prepararLoginPerfilAluno();
    prepararLogout();
    visitar(rotas.login, textos.tituloLogin);
    login(
      aluno.email,
      dadosTeste.senha,
      rotas.inicioAluno,
      textos.areaAluno.saudacao(aluno.nome),
    );
    sairDaConta();
  });

  it("não exibe os dados da primeira conta após outra conta entrar no mesmo navegador", () => {
    prepararAcessoEntreContas();

    visitar(rotas.login, textos.tituloLogin);
    login(
      contasIsolamentoCdu.alunoA.email,
      dadosTeste.senha,
      rotas.inicioAluno,
      textos.areaAluno.saudacao(contasIsolamentoCdu.alunoA.nome),
    );
    conferirHistoricoPrivadoDaConta(contasIsolamentoCdu.alunoA, "Treino privado da conta A");
    sairDaConta();

    login(
      contasIsolamentoCdu.alunoB.email,
      dadosTeste.senha,
      rotas.inicioAluno,
      textos.areaAluno.saudacao(contasIsolamentoCdu.alunoB.nome),
    );
    conferirHistoricoPrivadoDaConta(
      contasIsolamentoCdu.alunoB,
      "Treino privado da conta B",
      "Treino privado da conta A",
    );
  });

  it("redireciona para o login ao abrir o histórico sem sessão", () => {
    prepararVisitanteSemSessao();
    visitarHistoricoSemSessao();
    conferirRedirecionamentoAoLoginDoHistorico();
  });
});
