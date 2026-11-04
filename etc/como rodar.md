# Como rodar o IVIS

Este guia parte da **raiz do repositório**. O caminho mais simples usa Docker para subir frontend, backend, API de IA, PostgreSQL e Mailpit. Para editar a interface com atualização automática, rode o frontend pelo Vite.

## Antes de começar

| Para quê | Ferramentas necessárias |
| --- | --- |
| Executar o sistema completo | Docker Desktop iniciado, com Docker Compose |
| Desenvolver ou testar o frontend | Node.js 22 ou superior e npm |
| Testar o backend fora do container | JDK 25 e Docker Desktop; o Gradle Wrapper já está no projeto |
| Testar a API de IA fora do container | Python 3.12 ou 3.13 e `pip` |

Na primeira execução, o Docker e os gerenciadores de pacotes precisam baixar imagens e dependências. **Java e Gradle locais não são necessários para subir o sistema pelo Docker.**

## 1. Subir o sistema completo

No **PowerShell**, na raiz do projeto:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
if (-not (Test-Path api-ia/.env)) { Copy-Item api-ia/.env.example api-ia/.env }
```

Edite `api-ia/.env` e preencha `NVIDIA_API_KEY` para gerar perguntas e avaliações reais. Sem a chave, o sistema inicia com um aviso e os treinos ficam indisponíveis; cadastro, planos e os demais módulos podem ser utilizados. O healthcheck do Compose verifica `/saude` (processo ativo), enquanto `/pronto` continua retornando 503 sem o provedor configurado. O `.env` da raiz contém a configuração local dos demais serviços. Não envie esses arquivos ao Git. O script gera uma chave JWT local caso `IVIS_JWT_CHAVE_ASSINATURA` esteja vazia.

```powershell
.\release.ps1 up
```

No **Bash, Git Bash ou WSL**, use `bash release.sh up` após preparar os mesmos arquivos. O script Bash precisa de `curl` e `openssl` disponíveis. Ambos os scripts constroem as imagens e aguardam a prontidão dos serviços.

| Serviço | Endereço local |
| --- | --- |
| Aplicação | <http://localhost:8080> |
| Backend | <http://localhost:8081/api> |
| API de IA | <http://localhost:8000> |
| Mailpit, para ver e-mails de recuperação | <http://localhost:8025> |
| PostgreSQL | `localhost:5433`, banco `ivis` |

O backend pode ser conferido em <http://localhost:8081/api/v1/pronto> e a API de IA em <http://localhost:8000/pronto>. Para abrir o banco em uma IDE, use o usuário e a senha de `IVIS_BD_USUARIO` e `IVIS_BD_SENHA` no `.env`. O exemplo usa `ivis` e `ivis_local`. A porta é definida por `IVIS_BD_PORTA` (padrão `5433`); `5432` é a porta **interna** do container.

### Parar, consultar estado e acompanhar logs

```powershell
.\release.ps1 status
.\release.ps1 logs
.\release.ps1 down
```

`logs` acompanha a saída até você pressionar `Ctrl+C`. `down` remove os containers e preserva o volume do banco. `restart` reinicia os serviços sem reconstruir as imagens; depois de alterar código ou variáveis de ambiente, use `up` novamente. No Bash, substitua `.\release.ps1` por `bash release.sh`.

## 2. Desenvolver o frontend com Vite

Deixe a stack Docker ativa. Em **outro terminal**, na raiz do projeto:

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Abra <http://localhost:5173>. O Vite encaminha `/api` ao backend em `localhost:8081`. Para usar outro backend, crie `frontend/.env.local` com `VITE_API_URL` apontando para a URL completa da API, por exemplo `http://localhost:8081/api`, e reinicie o Vite. Variáveis `VITE_` ficam visíveis no navegador; não coloque segredos nelas.

Para trabalhar também no backend fora do Docker, consulte [o guia do backend](../backend/LEIA-ME.md). Nesse caso, pare o container `ivis-backend` antes de iniciar o `bootRun` na mesma porta.

## 3. Primeiro acesso

Abra a aplicação, crie uma conta de aluno ou instituição e consulte os planos. O checkout é **simulado**, sem cobrança real:

| Resultado | Cartão de teste | Validade | CVV |
| --- | --- | --- | --- |
| Aprovado | `4242 4242 4242 4242` | `12/30` | `123` |
| Recusado | `4000 0000 0000 0002` | `12/30` | `123` |

Use uma validade futura quando executar o teste. Os e-mails de recuperação ficam no Mailpit; não são entregues a uma caixa externa. Sem `NVIDIA_API_KEY`, os treinos que precisam gerar perguntas ou avaliação não funcionam.

## 4. Rodar os testes

As verificações abaixo são independentes. Execute as que correspondem ao módulo alterado.

### Frontend

```powershell
npm --prefix frontend ci
npm --prefix frontend run test:unit
npm --prefix frontend run typecheck
npm --prefix frontend run lint
npm --prefix frontend run build
```

`build` também faz a verificação de tipos e grava o resultado em `frontend/dist/`. Os testes unitários usam Vitest e não precisam de Docker.

### Backend

Com **JDK 25** e Docker Desktop ativos, use o Gradle Wrapper do projeto. Não é preciso instalar Gradle globalmente:

```powershell
Push-Location backend
.\gradlew.bat test
Pop-Location
```

Os testes de integração usam Testcontainers para iniciar PostgreSQL. No Bash, execute `./gradlew test` dentro de `backend/`.

### API de IA

No PowerShell, crie o ambiente virtual e instale as dependências uma vez:

```powershell
py -3.12 -m venv api-ia/.venv
& .\api-ia\.venv\Scripts\python.exe -m pip install -r api-ia/requirements.txt
Push-Location api-ia
& .\.venv\Scripts\python.exe -m pytest tests -q
Pop-Location
```

Esses testes usam provedores simulados e não exigem uma chave NVIDIA real.

### Cypress: testes de interface

Instale as dependências do pacote e inicie o Vite em outro terminal:

```powershell
npm --prefix cypress/e2e ci
npm --prefix frontend run dev
```

Os cenários de CDU com `cy.intercept` usam respostas simuladas e podem rodar apenas com o Vite. Para executar um arquivo:

```powershell
npm --prefix cypress/e2e run testar:e2e -- --spec "cdu/CDU-16-*.cy.js"
```

**A suíte completa inclui jornadas reais** (`caminho_feliz/` e CDU-15). Elas usam o mesmo backend, API de IA e banco `ivis` do Compose. Os hooks removem apenas as contas das fixtures reais e seus dados vinculados, preservando outras contas e o catálogo. Siga a [preparação do Cypress](../cypress/e2e/README.md) antes de executar `npm run testar:e2e`. As jornadas de treino precisam da chave NVIDIA.

### Dependências do projeto

Para conferir ferramentas instaladas e dependências declaradas:

```powershell
npm ci
npm run deps:check
npm run deps:audit
```

## Problemas comuns

| Sintoma | O que conferir |
| --- | --- |
| Docker Engine indisponível | Abra o Docker Desktop, espere o Engine iniciar e repita `.\release.ps1 up`. |
| Porta ocupada | Verifique as portas `8080`, `8081`, `8000`, `8025` e `5433`. A porta do banco pode ser alterada em `IVIS_BD_PORTA` no `.env`. |
| Banco recusa a senha | Confira a porta e as credenciais do `.env`. Se o volume já existia, mudar `IVIS_BD_SENHA` no arquivo não altera a senha gravada no PostgreSQL. |
| Frontend não acessa o backend | Confira <http://localhost:8081/api/v1/pronto>, a URL de `VITE_API_URL` (se definida) e `IVIS_CORS_ORIGENS` no `.env`. |
| IA indisponível no treino | Confira `NVIDIA_API_KEY` em `api-ia/.env` e os logs com `.\release.ps1 logs`. |
| `java` não é reconhecido nos testes do backend | Instale/configure JDK 25 e `JAVA_HOME`; para apenas subir a stack Docker, Java local não é necessário. |

Para detalhes de publicação, veja [Como publicar na Vercel](vercel.md). Para investigar cada módulo, consulte os guias do [frontend](../frontend/LEIA-ME.md), [backend](../backend/LEIA-ME.md) e [API de IA](../api-ia/LEIA-ME.md).
