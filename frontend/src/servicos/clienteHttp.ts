import axios from "axios";

export const clienteHttp = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8081/api",
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});
