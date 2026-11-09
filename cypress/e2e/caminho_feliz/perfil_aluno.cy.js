import { dadosCaminhoFeliz, dadosTeste } from "../funcoes/constantes.js";
import {
  abrirEdicaoDosDadosDaConta,
  abrirFormularioDeHabilidade,
  abrirFormularioDeVaga,
  abrirHistoricoPeloFeedback,
  abrirMinhasHabilidades,
  abrirTelaDeTreinos,
  abrirTreinoPeloHistorico,
  abrirVagasDeInteresse,
  alterarEnderecoDoAluno,
  alterarTelefoneDoAluno,
  conferirAreaDoAluno,
  conferirCadastroDoAlunoConcluido,
  conferirCheckoutIndividual,
  conferirContextoDaVagaNoTreino,
  conferirDadosDoAlunoSalvos,
  conferirEmailNoMailpit,
  conferirFeedbackAbertoPeloHistorico,
  conferirFeedbackDoTreino,
  conferirFormularioDeCadastroDoAluno,
  conferirHabilidadeCadastrada,
  conferirOfertasDoAluno,
  conferirPagamentoAprovado,
  conferirPerguntaDaIa,
  conferirTelaDeEdicaoDosDadosDoAluno,
  conferirTelaDeHistoricoDeTreinos,
  conferirTelaDePreparacaoDoTreino,
  conferirTelaMinhasHabilidades,
  conferirTelaDeVagasDeInteresse,
  conferirTreinoNoHistorico,
  conferirTreinoProntoParaFinalizar,
  conferirVagaCadastrada,
  confirmarCadastroDaHabilidade,
  confirmarCadastroDaVaga,
  confirmarSenhaDeCadastro,
  continuarDoCadastroParaOfertas,
  digitarRespostaDoTreino,
  expandirMenuDesenvolvimento,
  enviarCadastroDoAluno,
  enviarPagamentoSimulado,
  enviarRespostaDoTreino,
  finalizarTreino,
  habilidadeDoCaminho,
  informarCategoriaDaHabilidade,
  informarCpfDoAluno,
  informarEmailDoAluno,
  informarEnderecoDoAluno,
  informarEmpresaDaVaga,
  informarNomeDaHabilidade,
  informarNomeDoAluno,
  informarRequisitosDaVaga,
  informarSenhaDeCadastro,
  informarTelefoneDoAluno,
  informarTituloDaVaga,
  iniciarEntrevistaComIa,
  irParaAreaDoAluno,
  marcarCaixaMailpit,
  preencherCartaoAprovado,
  respostasDoTreino,
  salvarDadosDaConta,
  selecionarNivelDaHabilidade,
  selecionarNivelDaVaga,
  selecionarModoTreinoPorVaga,
  selecionarOfertaIndividual,
  selecionarVagaDoTreino,
  vagaDoCaminho,
  visitarCadastroDoAluno,
} from "../funcoes/caminho-feliz.js";

describe(
  "caminho feliz do perfil aluno",
  { testIsolation: false, retries: 0 },
  () => {
    it("cadastra a conta real do aluno e segue para as ofertas", () => {
      visitarCadastroDoAluno();
      conferirFormularioDeCadastroDoAluno();
      informarNomeDoAluno(dadosTeste.aluno.nome);
      informarEmailDoAluno(dadosTeste.aluno.email);
      informarCpfDoAluno(dadosTeste.aluno.cpf);
      informarTelefoneDoAluno(dadosTeste.aluno.telefone);
      informarEnderecoDoAluno(dadosTeste.aluno.endereco);
      informarSenhaDeCadastro(dadosTeste.senha);
      confirmarSenhaDeCadastro(dadosTeste.senha);
      marcarCaixaMailpit();
      enviarCadastroDoAluno();
      conferirCadastroDoAlunoConcluido();
      conferirEmailNoMailpit(dadosTeste.aluno.email, "Bem-vindo ao IVIS");
      continuarDoCadastroParaOfertas();
      conferirOfertasDoAluno();
    });

    it("contrata o plano individual com pagamento simulado e entra na área", () => {
      selecionarOfertaIndividual();
      conferirCheckoutIndividual();
      preencherCartaoAprovado();
      marcarCaixaMailpit();
      enviarPagamentoSimulado();
      conferirPagamentoAprovado();
      conferirEmailNoMailpit(dadosTeste.aluno.email, "Confirmação de contratação simulada — IVIS");
      irParaAreaDoAluno();
      conferirAreaDoAluno(dadosTeste.aluno.nome);
    });

    it("atualiza os dados pessoais da conta", () => {
      abrirEdicaoDosDadosDaConta();
      conferirTelaDeEdicaoDosDadosDoAluno();
      alterarTelefoneDoAluno(dadosCaminhoFeliz.contaAtualizada.telefone);
      alterarEnderecoDoAluno(dadosCaminhoFeliz.contaAtualizada.endereco);
      salvarDadosDaConta();
      conferirDadosDoAlunoSalvos(
        dadosCaminhoFeliz.contaAtualizada.telefone,
        dadosCaminhoFeliz.contaAtualizada.endereco,
      );
    });

    it("cadastra uma habilidade no perfil", () => {
      expandirMenuDesenvolvimento();
      abrirMinhasHabilidades();
      conferirTelaMinhasHabilidades();
      abrirFormularioDeHabilidade();
      informarNomeDaHabilidade();
      informarCategoriaDaHabilidade();
      selecionarNivelDaHabilidade();
      confirmarCadastroDaHabilidade();
      conferirHabilidadeCadastrada(habilidadeDoCaminho);
    });

    it("cadastra uma vaga de interesse", () => {
      expandirMenuDesenvolvimento();
      abrirVagasDeInteresse();
      conferirTelaDeVagasDeInteresse();
      abrirFormularioDeVaga();
      informarTituloDaVaga();
      informarEmpresaDaVaga();
      selecionarNivelDaVaga();
      informarRequisitosDaVaga();
      confirmarCadastroDaVaga();
      conferirVagaCadastrada(vagaDoCaminho);
    });

    it("realiza um treino com IA usando a vaga cadastrada", () => {
      abrirTelaDeTreinos();
      conferirTelaDePreparacaoDoTreino();
      selecionarModoTreinoPorVaga();
      selecionarVagaDoTreino(vagaDoCaminho);
      conferirContextoDaVagaNoTreino(vagaDoCaminho);
      iniciarEntrevistaComIa();
      conferirPerguntaDaIa(1);
      digitarRespostaDoTreino(respostasDoTreino[0]);
      enviarRespostaDoTreino();
      conferirPerguntaDaIa(2);
      digitarRespostaDoTreino(respostasDoTreino[1]);
      enviarRespostaDoTreino();
      conferirPerguntaDaIa(3);
      digitarRespostaDoTreino(respostasDoTreino[2]);
      enviarRespostaDoTreino();
      conferirTreinoProntoParaFinalizar();
      finalizarTreino();
      conferirFeedbackDoTreino(vagaDoCaminho);
    });

    it("consulta no histórico o treino realizado para a vaga", () => {
      abrirHistoricoPeloFeedback();
      conferirTelaDeHistoricoDeTreinos();
      conferirTreinoNoHistorico(vagaDoCaminho);
      abrirTreinoPeloHistorico(vagaDoCaminho);
      conferirFeedbackAbertoPeloHistorico(vagaDoCaminho);
    });
  },
);
