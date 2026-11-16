import axios from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { ref } from "vue";

let tokenAcesso: string | null = null;
export const requisicoesEmAndamento = ref(0);
const requisicoesVisiveis = new WeakSet<InternalAxiosRequestConfig>();

function concluirCarregamento(config?: InternalAxiosRequestConfig) {
  if (config && requisicoesVisiveis.delete(config)) {
    requisicoesEmAndamento.value = Math.max(0, requisicoesEmAndamento.value - 1);
  }
}

export const clienteHttp = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "/api",
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

clienteHttp.interceptors.request.use((config) => {
  const caminho = (config.url ?? "").split("?")[0];
  if (!["/v1/sessoes/csrf", "/v1/sessoes/renovacao", "/v1/sessoes/atividade", "/v1/senhas/redefinicao/autorizacao"].includes(caminho)) {
    requisicoesVisiveis.add(config);
    requisicoesEmAndamento.value += 1;
  }
  const endpointPublicoDeSessao =
    caminho === "/v1/sessoes" ||
    caminho === "/v1/sessoes/csrf" ||
    caminho === "/v1/sessoes/renovacao" ||
    caminho === "/v1/sessoes/atividade" ||
    caminho === "/v1/senhas/recuperacao" ||
    caminho === "/v1/senhas/validacao-codigo" ||
    caminho === "/v1/senhas/redefinicao" ||
    caminho === "/v1/senhas/redefinicao/autorizacao" ||
    caminho === "/v1/contas/alunos" ||
    caminho === "/v1/contas/instituicoes";

  // Um access token expirado não deve bloquear endpoints que iniciam ou
  // renovam a sessão, nem a emissão do cookie/token CSRF.
  if (endpointPublicoDeSessao) {
    config.headers.delete("Authorization");
    return config;
  }

  if (tokenAcesso) {
    config.headers.Authorization = `Bearer ${tokenAcesso}`;
  }
  return config;
});

clienteHttp.interceptors.response.use(
  (resposta) => {
    concluirCarregamento(resposta.config);
    return resposta;
  },
  (erro) => {
    concluirCarregamento(erro?.config);
    return Promise.reject(erro);
  },
);

export function definirTokenAcesso(token: string | null) {
  tokenAcesso = token;
}
