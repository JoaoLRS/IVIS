import axios from "axios";

let tokenAcesso: string | null = null;

export const clienteHttp = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "/api",
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

clienteHttp.interceptors.request.use((config) => {
  const caminho = (config.url ?? "").split("?")[0];
  const endpointPublicoDeSessao =
    caminho === "/v1/sessoes" ||
    caminho === "/v1/sessoes/csrf" ||
    caminho === "/v1/sessoes/renovacao" ||
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

export function definirTokenAcesso(token: string | null) {
  tokenAcesso = token;
}
