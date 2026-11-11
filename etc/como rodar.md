# Como rodar o IVIS

Siga os passos abaixo na **pasta raiz do projeto IVIS**. Se você ainda não clonou o repositório, veja [como clonar e instalar as ferramentas](manual_de_usuario.md#2-clonar-o-repositório).

## Windows: iniciar a aplicação e abrir os testes

Você precisa de **Docker Desktop**, **Node.js 22 ou superior**, **Python 3.12 ou 3.13** e **JDK 25**. Os links de instalação estão no [manual do usuário](manual_de_usuario.md#tecnologias-necessárias-para-executar-o-projeto).

### 1. Abra o Docker Desktop

Espere o Docker terminar de iniciar. Deixe-o aberto enquanto usa a aplicação ou executa os testes.

### 2. Abra o PowerShell na pasta do IVIS

No exemplo abaixo, o projeto está em `C:\projetos\IVIS`. Se você o clonou em outro lugar, use o caminho correspondente:

~~~powershell
cd C:\projetos\IVIS
~~~

Se recebeu os arquivos da pasta `env/` para a aula, copie-os **antes** de executar os scripts:

~~~powershell
Copy-Item -LiteralPath 'env/env_main/.env' -Destination '.env' -Force
Copy-Item -LiteralPath 'env/env_api_ia/.env' -Destination 'api-ia/.env' -Force
~~~

Esses comandos substituem arquivos `.env` já existentes nos destinos. Se você não recebeu a pasta `env/`, siga para o passo 3: os scripts criarão arquivos a partir dos exemplos. Depois do passo 3, preencha `NVIDIA_API_KEY` em `api-ia/.env` para usar os treinos com IA.

### 3. Prepare o projeto

~~~powershell
.\config.ps1
~~~

Espere o comando terminar. Ele instala as dependências locais, inclusive as do Cypress, e compila os módulos. `.\config.ps1 install` faz a mesma coisa.

### 4. Inicie todos os serviços

~~~powershell
.\release.ps1 up
~~~

Espere a mensagem de prontidão. O script sobe frontend, backend, API de IA, PostgreSQL e Mailpit. Você não precisa iniciar cada serviço separadamente.

### 5. Abra a aplicação

- **IVIS:** <http://localhost:8080>
- **Mailpit:** <http://localhost:8025> — recebe os e-mails de boas-vindas, contratação simulada e recuperação de senha.

### 6. Abra os testes no Cypress

Em **outro PowerShell**, também na pasta raiz do IVIS, execute:

~~~powershell
npx cypress open
~~~

Na janela do Cypress, clique em **E2E Testing**, escolha **Electron** e clique em **Start E2E Testing in Electron**. Os testes aparecem na aba **Specs**. Clique no arquivo que deseja executar, por exemplo `CDU-15-registrar-auditoria.cy.js` ou `perfil_aluno.cy.js`. Execute uma suíte por vez.

## Linux ou macOS

Inicie o Docker (Docker Desktop no macOS; Docker Desktop ou Docker Engine com Compose no Linux). Tenha `curl` e `openssl` disponíveis. Na pasta raiz do IVIS, se recebeu os arquivos da aula, copie-os:

~~~bash
cp env/env_main/.env .env
cp env/env_api_ia/.env api-ia/.env
~~~

Os comandos `cp` substituem arquivos de destino existentes. Depois, inicie a aplicação:

~~~bash
bash release.sh up
~~~

Se não recebeu os arquivos da aula, o script cria os `.env` a partir dos exemplos. Para habilitar os treinos com IA, preencha `NVIDIA_API_KEY` em `api-ia/.env` e execute `bash release.sh up` novamente.

Abra <http://localhost:8080>. Para abrir os testes no Cypress, instale Node.js 22 ou superior e execute:

~~~bash
npm --prefix cypress/e2e ci
cd cypress/e2e
npx cypress open
~~~

No Cypress, escolha **E2E Testing**, **Electron** e **Start E2E Testing in Electron**; os arquivos ficam em **Specs**. Para executar os comandos seguintes a partir da raiz, volte com `cd ../..`.

## Rodar todos os testes pelo terminal

Deixe o Docker e a aplicação ativos. Comece **na pasta raiz do IVIS** e siga as etapas na ordem. Em cada etapa, `cd` entra na pasta do módulo e `cd ..` volta à raiz. Espere um teste terminar antes de começar o próximo. A suíte Cypress inclui casos de uso e jornadas reais; o caminho feliz do aluno precisa de `NVIDIA_API_KEY` em `api-ia/.env`. Os testes de e-mail consultam o Mailpit.

### No Windows (PowerShell)

Se você seguiu os passos 1 a 4 acima, `config.ps1` já instalou as dependências. Rode nesta ordem:

**1. Frontend — testes unitários, tipos, estilo e compilação**

~~~powershell
cd frontend
npm run test:unit
npm run typecheck
npm run lint
npm run build
cd ..
~~~

**2. Backend — testes unitários e de integração**

~~~powershell
cd backend
.\gradlew.bat test
cd ..
~~~

**3. API de IA — testes Python**

~~~powershell
cd api-ia
.\.venv\Scripts\python.exe -m pytest tests -q
cd ..
~~~

**4. Banco — teste da limpeza usada pelo Cypress**

~~~powershell
node --test cypress/tasks/limpar-banco.integracao.test.mjs
~~~

**5. Interface — todos os testes Cypress**

~~~powershell
npm run testar:e2e
~~~

### No Linux ou macOS (Bash)

Para testar também o backend e a API de IA fora dos containers, instale **JDK 25** e **Python 3.12 ou 3.13**, além de Node.js. Prepare as dependências locais uma vez:

~~~bash
npm --prefix frontend ci
npm --prefix cypress/e2e ci
python3.12 -m venv api-ia/.venv
api-ia/.venv/bin/python -m pip install -r api-ia/requirements.txt
~~~

Se usar Python 3.13, troque `python3.12` por `python3.13`. Depois, na raiz do IVIS, rode um bloco por vez:

**1. Frontend**

~~~bash
cd frontend
npm run test:unit
npm run typecheck
npm run lint
npm run build
cd ..
~~~

**2. Backend**

~~~bash
cd backend
./gradlew test
cd ..
~~~

**3. API de IA**

~~~bash
cd api-ia
.venv/bin/python -m pytest tests -q
cd ..
~~~

**4. Banco**

~~~bash
node --test cypress/tasks/limpar-banco.integracao.test.mjs
~~~

**5. Interface**

~~~bash
npm run testar:e2e
~~~

Os testes do backend usam o Docker para iniciar um PostgreSQL de teste. A suíte Cypress usa o banco da aplicação; execute **uma suíte Cypress por vez**. Vídeos e capturas de falhas ficam em `cypress/downloads` e `cypress/screenshots`.

Para executar só um arquivo Cypress, por exemplo o CDU-15, use na raiz do projeto:

~~~powershell
npm --prefix cypress/e2e run testar:e2e -- --spec "cdu/CDU-15-registrar-auditoria.cy.js"
~~~

## Consultar ou parar os serviços

No Windows:

~~~powershell
.\release.ps1 status
.\release.ps1 logs
.\release.ps1 down
~~~

No Linux ou macOS, substitua `.\release.ps1` por `bash release.sh`. `logs` acompanha a saída até você pressionar `Ctrl+C`. `down` para os containers e preserva os dados do banco.

## Se algo não abrir

| O que aconteceu | Confira |
| --- | --- |
| O Docker não inicia | Abra o Docker Desktop e espere o motor ficar disponível. |
| `localhost:8080` não abre | Execute `.\release.ps1 status` e depois `.\release.ps1 logs`. No Linux/macOS, use `bash release.sh`. |
| O treino com IA não funciona | Confira `NVIDIA_API_KEY` em `api-ia/.env` e os logs da API de IA. |
| O e-mail não aparece | Abra o Mailpit em <http://localhost:8025>. |
| Alguma porta já está em uso | Verifique as portas `8080`, `8081`, `8000`, `8025` e `5433`. |

Para configurar cada módulo separadamente ou desenvolver com o Vite, consulte os guias do [frontend](../frontend/LEIA-ME.md), [backend](../backend/LEIA-ME.md) e [API de IA](../api-ia/LEIA-ME.md).
