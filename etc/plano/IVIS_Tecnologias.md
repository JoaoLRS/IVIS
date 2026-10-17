# IVIS — Tecnologias e Ferramentas de Desenvolvimento

Este documento define o stack tecnológico do projeto, organizado pelos 3 módulos definidos pela equipe, mais a infraestrutura e as ferramentas de desenvolvimento compartilhadas entre eles.

---

## Visão Geral da Arquitetura

O IVIS será desenvolvido como **3 módulos independentes**, que se comunicam entre si via API. A API de IA será implementada em Python e utilizará NVIDIA NGC; o backend Java consumirá essa API pelo lado servidor. O frontend consome somente a API do backend.

| Módulo | Responsabilidade | Linguagem principal |
|---|---|---|
| 1. API de IA | Processa as respostas do candidato e gera perguntas/feedback usando NVIDIA NGC | Python |
| 2. Backend | Regras de negócio, autenticação, planos, vagas, histórico | Java + Spring Boot |
| 3. Frontend | Interface web (Home, Perfil, Treinamento, etc.) | Vue + TypeScript |

```mermaid
flowchart LR
    FE["🖥️ Frontend (Vue + TS)"] --> BE["⚙️ Backend (Spring Boot)"]
    BE --> IA["🤖 API de IA (NVIDIA NGC)"]
    BE --> DB[("🗄️ PostgreSQL")]
    BE --> PAG["💳 Simulador de pagamento (apresentação)"]
    BE -. integração futura .-> PAGEXT["Gateway de pagamento real"]
    BE --> MAIL["✉️ Mailpit (e-mails capturados)"]
```

---

## Módulo 1 — API de IA

### Decisão: Python

A equipe decidiu utilizar **Python** neste módulo (registro da comparação que embasou a decisão logo abaixo, para referência futura). Como há intenção de colocar esse módulo em **produção**, esta seção também traz considerações de arquitetura voltadas a isso, além do stack de desenvolvimento.

<details>
<summary>Comparação que embasou a decisão (Python x Java)</summary>

| Critério | Python | Java |
|---|---|---|
| Suporte oficial da NVIDIA (SDKs, exemplos, NIM client) | ✅ Nativo e prioritário | ⚠️ Limitado/indireto (via REST/gRPC genérico) |
| Frameworks de orquestração de IA (LangChain, LlamaIndex, etc.) | ✅ Maduro | ❌ Praticamente inexistente |
| Velocidade de prototipação para prompts/perguntas de entrevista | ✅ Alta | ⚠️ Mais verboso |
| Consistência com o restante do stack (que já é Java no Backend) | ❌ Introduz uma segunda linguagem | ✅ Time só precisa dominar uma linguagem |
| Performance/tipagem forte em produção | ⚠️ Suficiente para I/O-bound (chamadas de API) | ✅ Melhor para regras de negócio complexas |
| Facilidade de contratar/treinar a equipe em IA | ✅ Mercado de IA é majoritariamente Python | ⚠️ Menos comum |

</details>

### Tecnologias do módulo
- **Linguagem**: Python 3.11+
- **Framework de API**: FastAPI (assíncrono, tipagem com Pydantic, documentação automática via Swagger/OpenAPI)
- **Servidor ASGI**: Uvicorn
- **Provedor de IA**: NVIDIA NGC
- **Cliente da API do provedor**: integrar pelo serviço Python; SDK/cliente HTTP específico ainda a selecionar
- **Validação de dados**: Pydantic
- **Testes**: Pytest
- **Gerenciador de dependências**: Poetry ou pip + `requirements.txt`

### Considerações para Produção 🆕

Como este módulo tem plano de ir para produção, alguns pontos que valem entrar no planejamento desde já (a maioria não muda o código da aplicação, mas sim como ela é empacotada/operada):

- **Servidor de aplicação**: em produção, não rodar o Uvicorn sozinho — usar **Gunicorn como process manager com workers Uvicorn** (`gunicorn -k uvicorn.workers.UvicornWorker`), para aproveitar múltiplos processos/núcleos.
- **Containerização otimizada**: imagem Docker com **multi-stage build**, partindo de uma base enxuta (ex.: `python:3.11-slim`), para reduzir tamanho e superfície de ataque.
- **Health checks**: expor endpoints como `/health` (liveness) e `/ready` (readiness), usados pelo orquestrador (Docker/Kubernetes) para saber se a instância está apta a receber tráfego.
- **Observabilidade**: logging estruturado (ex.: JSON) e métricas (ex.: Prometheus + Grafana) para acompanhar latência e taxa de erro nas chamadas à NVIDIA NGC — importante porque esse módulo depende de um serviço externo.
- **Segurança na comunicação interna**: autenticar a comunicação entre Backend e API de IA (ex.: API key ou mTLS); o backend é o consumidor dessa API e o frontend não chama NVIDIA NGC diretamente.
- **Gestão de segredos**: chave de acesso à NVIDIA NGC gerenciada via variável de ambiente/secrets manager, nunca versionada no código.
- **Rate limiting / retry / timeout**: proteção contra estouro de custo ou indisponibilidade momentânea da NVIDIA NGC (ex.: usando `tenacity` para retries com backoff).
- **Versionamento de prompt/modelo**: manter controle de qual versão de prompt e de modelo (NGC) está em uso, permitindo rollback caso uma mudança piore a qualidade das entrevistas.
- **Escalabilidade**: planejar desde já se a orquestração em produção será via Docker Compose (mais simples, times pequenos) ou Kubernetes (maior necessidade de autoscaling).
- **CI/CD**: pipeline automatizado de build, testes (Pytest) e deploy para este módulo, com possibilidade de rollback rápido.

> ⚠️ Esses pontos não precisam ser resolvidos agora, no início do projeto — mas é importante já sinalizar no backlog para não serem esquecidos quando o módulo estiver pronto para ir ao ar.

---

## Módulo 2 — Backend

- **Linguagem**: Java 25 LTS
- **Framework principal**: Spring Boot
- **Build tool**: Gradle

### Frameworks/bibliotecas do ecossistema Spring necessários
- **Spring Web (MVC)** — construção dos endpoints REST.
- **Spring Data JPA** — persistência e integração com o PostgreSQL.
- **Spring Security** — autenticação e autorização (login de Aluno/Instituição, controle de acesso por tipo de usuário).
- **Spring Validation** — validação de dados de entrada (DTOs).
- **Spring WebClient** (ou OpenFeign) — comunicação HTTP com o Módulo de IA e, se adotado futuramente, com um gateway real; o simulador de apresentação é controlado pelo backend.
- **JWT (jjwt ou Spring Security OAuth2 Resource Server)** — emissão e validação de token de autenticação.
- **Flyway** — versionamento e migração do schema do banco de dados.
- **Lombok** — redução de boilerplate (getters/setters/construtores).
- **MapStruct** *(sugestão)* — conversão entre Entidades e DTOs.

### Testes de Backend
- **JUnit 5** — testes unitários.
- **Mockito** — mocks de dependências nos testes unitários.
- **Spring Boot Test** — testes de integração/contexto Spring.
- *(sugestão)* **Testcontainers** — subir um PostgreSQL real em container durante os testes de integração, evitando divergência entre ambiente de teste e produção.

---

## Módulo 3 — Frontend

- **Framework**: Vue (recomenda-se Vue 3, com Composition API)
- **Linguagem**: TypeScript
- **Runtime/gerenciador de pacotes**: Node.js (+ npm ou pnpm)
- **Build tool**: Vite *(sugestão — é o padrão atual para projetos Vue 3, com build e hot-reload muito mais rápidos que alternativas antigas)*
- **Roteamento**: Vue Router *(sugestão, necessário para navegar entre Home, Login, Perfil, Treinamento etc.)*
- **Gerenciamento de estado**: Pinia *(sugestão — sucessor oficial do Vuex para Vue 3)*
- **Cliente HTTP**: Axios *(sugestão, para consumir a API do Backend)*
- **Testes E2E**: Cypress
- **Testes unitários de componentes** *(sugestão)*: Vitest, que já vem integrado ao Vite
- **Padronização de código** *(sugestão)*: ESLint + Prettier

---

## Infraestrutura

- **Containerização**: Docker
- **Orquestração local**: Docker Compose *(sugestão — para subir Backend, API de IA, PostgreSQL e Frontend juntos com um único comando em ambiente de desenvolvimento)*
- **Banco de dados**: PostgreSQL (relacional; NoSQL não será utilizado). O modelo inicial está em [`BD/Modelo_Relacional_IVIS.md`](BD/Modelo_Relacional_IVIS.md), associado ao [ADR-0010](adr/ADR-0010-modelagem-relacional-do-banco.md).
- **E-mail de demonstração**: Mailpit, para capturar mensagens SMTP sem enviá-las a destinatários reais. A interface deve ser protegida no ambiente publicado.
- **Pagamento de demonstração**: simulador controlado pelo backend, disponível também na publicação usada para apresentação acadêmica. O cartão padrão é fictício; não há cobrança nem integração com gateway real nesse modo.
- **Administração do banco** *(sugestão)*: pgAdmin ou DBeaver

---

## Ferramentas de Desenvolvimento (IDEs)

| Ferramenta | Uso recomendado |
|---|---|
| IntelliJ IDEA | Desenvolvimento do Backend (Java/Spring Boot) |
| VS Code | Desenvolvimento do Frontend (Vue/TypeScript) e do Módulo de IA (Python) |

---

## Resumo Consolidado por Módulo

| Módulo | Linguagem | Framework | Testes | Build/Deploy |
|---|---|---|---|---|
| API de IA ✅ decidido | Python | FastAPI (+ Gunicorn/Uvicorn em produção) | Pytest | Docker |
| Backend | Java 25 LTS | Spring Boot (+ Spring Data JPA, Spring Security, Flyway) | JUnit 5 + Mockito | Docker + Gradle |
| Frontend | TypeScript | Vue 3 (+ Vite, Vue Router, Pinia) | Cypress (E2E) | Docker + Node.js |
| Banco de Dados | — | PostgreSQL | — | Docker |

---

## Pontos em Aberto para Decisão da Equipe

1. Confirmar se o gerenciamento de estado do Frontend será Pinia, Vuex ou nenhum (dependendo da complexidade real de estado global da aplicação).
2. Definir se haverá testes unitários no Frontend (Vitest) além dos testes E2E com Cypress, já que o documento só mencionou Cypress.
3. Definir a ferramenta de CI/CD (ex.: GitHub Actions, GitLab CI) para rodar os testes e builds automaticamente — não foi mencionada até agora.
4. 🆕 Definir a orquestração de produção da API de IA (Docker Compose x Kubernetes), já que o módulo tem plano de ir ao ar.
5. 🆕 Definir a ferramenta de observabilidade/monitoramento (ex.: Prometheus + Grafana, Datadog) a ser usada em produção, ao menos para o módulo de IA.
6. Definir provedor de e-mail real apenas se futuramente forem necessários envios externos; Mailpit atende à demonstração.
7. Decidir e registrar em ADR próprio uma eventual integração com gateway de pagamento real. O simulador de apresentação não pode fazer fallback para cobrança real.
