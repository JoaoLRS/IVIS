import { defineStore } from "pinia";
import { ref } from "vue";
import { clienteHttp, definirTokenAcesso } from "../servicos/clienteHttp";

export interface ContaAutenticada {
  id: string;
  tipoConta: "ALUNO" | "INSTITUICAO";
  email: string;
  nome: string;
  nomeAdministrador?: string | null;
}

interface SessaoResposta {
  accessToken: string;
  tipoToken: string;
  expiraEmSegundos: number;
  conta: ContaAutenticada;
}

interface RespostaPlano {
  situacao: "COM_PLANO_VIGENTE" | "SEM_PLANO_VIGENTE";
  planos: Array<{ situacao: string; origemAcesso: string }>;
}

async function tokenCsrf(): Promise<string> {
  const resposta = await clienteHttp.get<{ token: string }>(
    "/v1/sessoes/csrf",
    {
      withCredentials: true,
    },
  );
  return resposta.data.token;
}

export const useEstadoSessao = defineStore("sessao", () => {
  const accessToken = ref<string | null>(null);
  const conta = ref<ContaAutenticada | null>(null);
  const temPlano = ref<boolean | null>(null);
  const restaurada = ref(false);
  const expiraEm = ref(0);
  let restauracaoPendente: Promise<boolean> | null = null;

  function aplicarSessao(sessao: SessaoResposta) {
    accessToken.value = sessao.accessToken;
    conta.value = sessao.conta;
    expiraEm.value = Date.now() + sessao.expiraEmSegundos * 1000;
    definirTokenAcesso(sessao.accessToken);
  }

  function limparSessao() {
    accessToken.value = null;
    conta.value = null;
    temPlano.value = null;
    expiraEm.value = 0;
    definirTokenAcesso(null);
  }

  async function consultarPlano(): Promise<boolean> {
    if (!accessToken.value) return false;
    const resposta = await clienteHttp.get<RespostaPlano>("/v1/conta/plano");
    temPlano.value =
      resposta.data.situacao === "COM_PLANO_VIGENTE" &&
      resposta.data.planos.some((plano) => plano.situacao === "ATIVO");
    return temPlano.value;
  }

  async function entrar(email: string, senha: string) {
    const csrf = await tokenCsrf();
    const resposta = await clienteHttp.post<SessaoResposta>(
      "/v1/sessoes",
      {
        email,
        senha,
      },
      {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": csrf },
      },
    );
    aplicarSessao(resposta.data);
    restaurada.value = true;
    await consultarPlano();
  }

  async function cadastrarAluno(dados: {
    nome: string;
    email: string;
    senha: string;
  }) {
    const csrf = await tokenCsrf();
    await clienteHttp.post("/v1/contas/alunos", dados, {
      withCredentials: true,
      headers: { "X-XSRF-TOKEN": csrf },
    });
    await entrar(dados.email, dados.senha);
  }

  async function cadastrarInstituicao(dados: {
    nomeAdministrador: string;
    razaoSocial: string;
    email: string;
    senha: string;
  }) {
    const csrf = await tokenCsrf();
    await clienteHttp.post("/v1/contas/instituicoes", dados, {
      withCredentials: true,
      headers: { "X-XSRF-TOKEN": csrf },
    });
    await entrar(dados.email, dados.senha);
  }

  async function restaurar(): Promise<boolean> {
    if (accessToken.value && Date.now() < expiraEm.value - 30_000) return true;
    if (accessToken.value) {
      accessToken.value = null;
      definirTokenAcesso(null);
      restaurada.value = false;
    }
    if (restaurada.value) return false;
    if (restauracaoPendente) return restauracaoPendente;

    restauracaoPendente = (async () => {
      try {
        const csrf = await tokenCsrf();
        const resposta = await clienteHttp.post<SessaoResposta>(
          "/v1/sessoes/renovacao",
          {},
          {
            withCredentials: true,
            headers: { "X-XSRF-TOKEN": csrf },
          },
        );
        aplicarSessao(resposta.data);
        await consultarPlano();
        return true;
      } catch {
        limparSessao();
        return false;
      } finally {
        restaurada.value = true;
        restauracaoPendente = null;
      }
    })();
    return restauracaoPendente;
  }

  async function sair() {
    try {
      if (accessToken.value) {
        const csrf = await tokenCsrf();
        await clienteHttp.delete("/v1/sessoes/atual", {
          withCredentials: true,
          headers: { "X-XSRF-TOKEN": csrf },
        });
      }
    } finally {
      limparSessao();
      restaurada.value = true;
    }
  }

  return {
    accessToken,
    conta,
    temPlano,
    restaurada,
    entrar,
    cadastrarAluno,
    cadastrarInstituicao,
    restaurar,
    consultarPlano,
    sair,
  };
});
