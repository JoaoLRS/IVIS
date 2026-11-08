import { defineStore } from "pinia";
import { ref } from "vue";
import { clienteHttp, definirTokenAcesso } from "../servicos/clienteHttp";

export interface ContaAutenticada {
  id: string;
  tipoConta: "ALUNO" | "INSTITUICAO";
  email: string;
  nome: string;
  nomeResponsavel?: string | null;
  cpf?: string | null;
  cnpj?: string | null;
  telefone?: string | null;
  endereco?: string | null;
  objetoInstituicao?: string | null;
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
  const marcadorInatividade = "ivis_sessao_expirada_inatividade";
  const limiteInatividadeMs = 20 * 60 * 1000;
  const intervaloRegistroMs = 30 * 1000;
  const accessToken = ref<string | null>(null);
  const conta = ref<ContaAutenticada | null>(null);
  const temPlano = ref<boolean | null>(null);
  const restaurada = ref(false);
  const expiraEm = ref(0);
  let restauracaoPendente: Promise<boolean> | null = null;
  let ultimaAtividade = 0;
  let ultimoRegistro = 0;
  let temporizadorInatividade: ReturnType<typeof setTimeout> | null = null;
  let registroPendente = false;

  function expirarPorInatividade() {
    window.localStorage.setItem(marcadorInatividade, "1");
    limparSessao();
    restaurada.value = true;
    window.location.replace("/login");
  }

  function agendarExpiracao() {
    if (temporizadorInatividade) clearTimeout(temporizadorInatividade);
    temporizadorInatividade = setTimeout(() => {
      if (accessToken.value && Date.now() - ultimaAtividade >= limiteInatividadeMs) {
        expirarPorInatividade();
      }
    }, limiteInatividadeMs + 100);
  }

  async function registrarAtividade() {
    if (registroPendente || Date.now() - ultimoRegistro < intervaloRegistroMs) return;
    registroPendente = true;
    try {
      const csrf = await tokenCsrf();
      await clienteHttp.post("/v1/sessoes/atividade", {}, {
        withCredentials: true,
        headers: { "X-XSRF-TOKEN": csrf },
      });
      ultimoRegistro = Date.now();
    } catch {
      // A renovação no backend decide se a sessão ainda pode ser recuperada.
    } finally {
      registroPendente = false;
    }
  }

  function observarInteracao() {
    if (!accessToken.value) return;
    if (Date.now() - ultimaAtividade >= limiteInatividadeMs) {
      expirarPorInatividade();
      return;
    }
    ultimaAtividade = Date.now();
    agendarExpiracao();
    void registrarAtividade();
  }

  if (typeof window !== "undefined") {
    for (const evento of ["pointerdown", "keydown", "scroll"]) {
      window.addEventListener(evento, observarInteracao, { passive: true });
    }
  }

  function aplicarSessao(sessao: SessaoResposta) {
    accessToken.value = sessao.accessToken;
    conta.value = sessao.conta;
    expiraEm.value = Date.now() + sessao.expiraEmSegundos * 1000;
    definirTokenAcesso(sessao.accessToken);
    ultimaAtividade = Date.now();
    ultimoRegistro = ultimaAtividade;
    agendarExpiracao();
  }

  function limparSessao() {
    if (temporizadorInatividade) clearTimeout(temporizadorInatividade);
    temporizadorInatividade = null;
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
    window.localStorage.removeItem(marcadorInatividade);
    restaurada.value = true;
    await consultarPlano();
  }

  async function cadastrarAluno(dados: {
    nome: string;
    email: string;
    cpf: string;
    telefone: string;
    endereco: string;
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
    nomeResponsavel: string;
    nomeInstituicao: string;
    email: string;
    senha: string;
    cnpj: string;
    telefone: string;
    endereco: string;
  }) {
    const csrf = await tokenCsrf();
    await clienteHttp.post("/v1/contas/instituicoes", dados, {
      withCredentials: true,
      headers: { "X-XSRF-TOKEN": csrf },
    });
    await entrar(dados.email, dados.senha);
  }

  async function restaurar(): Promise<boolean> {
    if (window.localStorage.getItem(marcadorInatividade) === "1") {
      limparSessao();
      restaurada.value = true;
      return false;
    }
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
