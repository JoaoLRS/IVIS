import { existsSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { spawnSync } from "node:child_process";
import process from "node:process";
import { fileURLToPath } from "node:url";

const raiz = dirname(fileURLToPath(import.meta.url));
const windows = process.platform === "win32";
const problemas = [];
const verificacoes = [];

function executar(comando, argumentos, opcoes = {}) {
  return spawnSync(comando, argumentos, {
    cwd: opcoes.cwd ?? raiz,
    encoding: "utf8",
    shell: opcoes.shell ?? false,
    timeout: opcoes.timeout ?? 60_000,
    maxBuffer: 8 * 1024 * 1024,
  });
}

function comandoDisponivel(comando) {
  const localizador = windows ? "where.exe" : "which";
  const resultado = executar(localizador, [comando]);
  return resultado.status === 0;
}

function registrar(ok, nome, detalhe, instrucao) {
  verificacoes.push({ ok, nome, detalhe });
  if (!ok) problemas.push({ nome, detalhe, instrucao });
}

function verificarComando(comando, nome, instrucao) {
  const disponivel = comandoDisponivel(comando);
  registrar(
    disponivel,
    nome,
    disponivel ? "encontrado no PATH" : `comando '${comando}' não encontrado`,
    instrucao,
  );
  return disponivel;
}

function verificarNode() {
  if (!verificarComando("node", "Node.js", "Instale Node.js LTS e execute este verificador novamente.")) {
    return false;
  }
  const resultado = executar("node", ["--version"]);
  const versao = resultado.stdout.trim().replace(/^v/, "");
  const major = Number.parseInt(versao.split(".")[0], 10);
  const ok = Number.isFinite(major) && major >= 20;
  registrar(
    ok,
    "Versão do Node.js",
    ok ? `Node.js ${versao}` : `Node.js ${versao || "desconhecido"}; Cypress 15 requer Node.js 20 ou superior`,
    "Instale uma versão LTS atual do Node.js.",
  );
  return ok;
}

function verificarDependenciasNpm(pacote, nome, npmDisponivel, instrucao) {
  const pastaPacote = resolve(raiz, pacote);
  const manifesto = resolve(pastaPacote, "package.json");
  if (!existsSync(manifesto)) {
    registrar(false, `Manifesto ${nome}`, `${pacote}/package.json não encontrado`, null);
    return;
  }

  if (!npmDisponivel) {
    registrar(false, `Dependências ${nome}`, "não foi possível consultar os pacotes sem npm", "Instale Node.js LTS, que inclui o npm, e execute este verificador novamente.");
    return;
  }

  const resultado = executar("npm", ["ls", "--depth=0", "--json"], {
    cwd: pastaPacote,
    shell: windows,
  });
  let dados;
  try {
    dados = JSON.parse(resultado.stdout || "{}");
  } catch {
    dados = null;
  }

  const falhas = dados?.problems ?? [];
  const ok = resultado.status === 0 && falhas.length === 0;
  const detalhe = ok
    ? `pacotes declarados em ${pacote}/package.json instalados`
    : falhas.length
      ? falhas.join("; ")
      : (resultado.stderr || resultado.stdout || "npm não conseguiu verificar os pacotes").trim();
  registrar(
    ok,
    `Dependências ${nome}`,
    detalhe,
    instrucao,
  );
}

function verificarBinarioCypress(npmDisponivel) {
  const pastaE2e = resolve(raiz, "cypress", "e2e");
  if (!npmDisponivel || !existsSync(resolve(pastaE2e, "node_modules"))) return;
  const cypress = executar("npx", ["--no-install", "cypress", "verify"], {
    cwd: pastaE2e,
    timeout: 120_000,
    shell: windows,
  });
  registrar(
    cypress.status === 0,
    "Binário do Cypress",
    cypress.status === 0
      ? "instalado e disponível"
      : (cypress.stderr || cypress.stdout || "binário do Cypress não encontrado").trim(),
    "No diretório cypress/e2e, execute: npx cypress install.",
  );
}

const codigoVerificadorPython = String.raw`
import importlib.metadata as metadata
import pathlib
import re
import sys

requirements = pathlib.Path(sys.argv[1])
falhas = []
for linha in requirements.read_text(encoding="utf-8").splitlines():
    linha = linha.split("#", 1)[0].strip()
    if not linha or linha.startswith(("-", "git+", "http://", "https://")):
        continue
    correspondencia = re.match(r"^([A-Za-z0-9][A-Za-z0-9._-]*)(?:\[[^]]+\])?(.*)$", linha)
    if not correspondencia:
        continue
    nome, restricao = correspondencia.groups()
    try:
        instalada = metadata.version(nome)
    except metadata.PackageNotFoundError:
        falhas.append(f"{nome} (ausente; requerido: {linha})")
        continue
    versao_exata = re.search(r"==\s*([A-Za-z0-9.*+_-]+)", restricao)
    if versao_exata and versao_exata.group(1) != "*" and instalada != versao_exata.group(1):
        falhas.append(f"{nome} {instalada} (requerido: {versao_exata.group(1)})")
if falhas:
    print("\n".join(falhas))
    sys.exit(1)
print("Todas as dependências fixadas estão instaladas.")
`;

function verificarDependenciasPython(pythonDisponivel) {
  const pasta = resolve(raiz, "api-ia");
  const requirements = resolve(pasta, "requirements.txt");
  const pythonVenv = windows
    ? resolve(pasta, ".venv", "Scripts", "python.exe")
    : resolve(pasta, ".venv", "bin", "python");

  if (!existsSync(requirements)) {
    registrar(false, "API de IA (Python)", "api-ia/requirements.txt não encontrado", null);
    return;
  }

  if (!existsSync(pythonVenv)) {
    const instrucao = windows
      ? "No PowerShell: Set-Location api-ia; py -3.12 -m venv .venv; .\\.venv\\Scripts\\python.exe -m pip install -r requirements.txt."
      : "No terminal: cd api-ia && python3.12 -m venv .venv && .venv/bin/python -m pip install -r requirements.txt.";
    registrar(
      false,
      "Ambiente virtual da API de IA",
      "api-ia/.venv não encontrado",
      pythonDisponivel ? instrucao : `Instale Python 3.12 ou 3.13 e depois execute: ${instrucao}`,
    );
    return;
  }

  const versaoPython = executar(pythonVenv, ["--version"]);
  const versao = `${versaoPython.stdout}\n${versaoPython.stderr}`.match(/Python\s+(\d+)\.(\d+)/)?.slice(1).map(Number);
  const versaoCompativel = versao?.[0] === 3 && (versao?.[1] === 12 || versao?.[1] === 13);
  registrar(
    versaoPython.status === 0 && versaoCompativel,
    "Versão do Python da API de IA",
    versaoCompativel ? `Python ${versao[0]}.${versao[1]}` : "api-ia/.venv deve usar Python 3.12 ou 3.13",
    windows
      ? "Recrie o ambiente com: Set-Location api-ia; py -3.12 -m venv .venv."
      : "Recrie o ambiente com: cd api-ia && python3.12 -m venv .venv.",
  );

  const resultado = executar(pythonVenv, ["-c", codigoVerificadorPython, requirements]);
  registrar(
    resultado.status === 0,
    "Dependências da API de IA",
    (resultado.stdout || resultado.stderr || "não foi possível verificar o ambiente Python").trim(),
    windows
      ? "No PowerShell: Set-Location api-ia; .\\.venv\\Scripts\\python.exe -m pip install -r requirements.txt."
      : "No terminal: cd api-ia && .venv/bin/python -m pip install -r requirements.txt.",
  );
}

function verificarJava() {
  const disponivel = comandoDisponivel("java");
  if (!disponivel) {
    registrar(false, "JDK do backend", "comando 'java' não encontrado", "Instale o JDK 25 para compilar ou executar o backend localmente. Com Docker, o JDK é fornecido pela imagem de build.");
    return false;
  }
  const resultado = executar("java", ["-version"]);
  const versao = `${resultado.stdout}\n${resultado.stderr}`.match(/version\s+"?(\d+)/i)?.[1];
  const ok = versao === "25";
  registrar(
    ok,
    "JDK do backend",
    versao ? `Java ${versao}${ok ? "" : " encontrado; o projeto requer Java 25"}` : "não foi possível identificar a versão do Java",
    "Instale o JDK 25 e confirme que java está no PATH. Com Docker, o JDK é fornecido pela imagem de build.",
  );
  return ok;
}

function verificarGradle(javaDisponivel) {
  const disponivel = comandoDisponivel("gradle");
  if (!disponivel) {
    registrar(false, "Gradle do backend", "comando 'gradle' não encontrado", "Para executar o backend localmente, instale Gradle 9.1 ou use Docker Compose, que já compila o backend dentro da imagem.");
    return false;
  }
  if (!javaDisponivel) {
    registrar(false, "Dependências do backend (Gradle)", "não é possível verificar as dependências sem o JDK 25", "Instale o JDK 25. Depois, execute 'gradle -p backend test' para resolver as dependências do backend.");
    return false;
  }

  const resultado = executar("gradle", [
    "-p", "backend", "dependencies", "--configuration", "testRuntimeClasspath",
    "--offline", "--console=plain", "--no-daemon",
  ], { timeout: 120_000, shell: windows });
  const saida = `${resultado.stdout}\n${resultado.stderr}`;
  const falhas = saida.split(/\r?\n/).filter((linha) => /\bFAILED\b|Could not resolve|not found in/i.test(linha));
  const ok = resultado.status === 0 && falhas.length === 0;
  registrar(
    ok,
    "Dependências do backend (Gradle)",
    ok
      ? "dependências de execução e teste disponíveis no cache local"
      : falhas.length
        ? falhas.join("; ")
        : (resultado.error?.message || saida.trim() || "não foi possível verificar as dependências do Gradle em modo offline"),
    "No diretório do projeto, execute 'gradle -p backend test' com acesso ao Maven Central para baixar as dependências ausentes.",
  );
  return ok;
}

console.log("Verificação de dependências do IVIS\n");

const nodeDisponivel = verificarNode();
const npmDisponivel = verificarComando("npm", "npm", "Instale Node.js LTS, que inclui o npm.");
if (nodeDisponivel) {
  verificarDependenciasNpm(".", "da raiz", npmDisponivel, "Na raiz do projeto, execute: npm install.");
  verificarDependenciasNpm("frontend", "do frontend", npmDisponivel, "No PowerShell: npm --prefix frontend install. Em Bash: npm --prefix frontend install.");
  verificarDependenciasNpm("cypress/e2e", "de testes E2E", npmDisponivel, "No PowerShell ou Bash, execute: npm --prefix cypress/e2e install.");
  verificarBinarioCypress(npmDisponivel);
}

const pythonVenvPath = windows
  ? resolve(raiz, "api-ia", ".venv", "Scripts", "python.exe")
  : resolve(raiz, "api-ia", ".venv", "bin", "python");
const pythonDisponivel = existsSync(pythonVenvPath) || (windows
  ? comandoDisponivel("py") || comandoDisponivel("python")
  : comandoDisponivel("python3") || comandoDisponivel("python"));
registrar(
  pythonDisponivel,
  "Python",
  pythonDisponivel ? "interpretador disponível no PATH" : "interpretador Python não encontrado",
  "Instale Python 3.12 ou 3.13 para desenvolver ou testar a API de IA localmente.",
);
verificarDependenciasPython(pythonDisponivel);

const javaDisponivel = verificarJava();
verificarGradle(javaDisponivel);

const dockerDisponivel = comandoDisponivel("docker");
if (!dockerDisponivel) {
  registrar(false, "Docker Compose", "comando 'docker' não encontrado", "Instale o Docker Desktop (Windows/macOS) ou Docker Engine com o plugin Compose (Linux) para iniciar o sistema completo.");
} else {
  const compose = executar("docker", ["compose", "version"]);
  registrar(
    compose.status === 0,
    "Docker Compose",
    compose.status === 0 ? compose.stdout.trim() : (compose.stderr || "plugin Docker Compose não encontrado").trim(),
    "Instale ou atualize o Docker Desktop/Engine para incluir Docker Compose v2.",
  );
}

for (const verificacao of verificacoes) {
  const marca = verificacao.ok ? "OK" : "FALTA";
  console.log(`[${marca}] ${verificacao.nome}: ${verificacao.detalhe}`);
}

if (problemas.length) {
  console.log("\nAções necessárias:");
  for (const problema of problemas) {
    console.log(`- ${problema.nome}: ${problema.instrucao ?? problema.detalhe}`);
  }
  process.exitCode = 1;
} else {
  console.log("\nDependências verificadas. Nenhuma instalação necessária.");
}
