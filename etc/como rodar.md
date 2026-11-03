# Como rodar o IVIS

Este guia cobre duas formas de executar o IVIS localmente: iniciar toda a stack em containers Docker ou rodar o frontend com o servidor de desenvolvimento do Vite. Os comandos pressupõem que o terminal está na raiz do repositório.

## Requisitos

- Docker Desktop iniciado, com Docker Compose disponível.
- Node.js 22 ou superior e npm para executar o frontend fora do Docker.
- Bash e OpenSSL para usar `release.sh` (Linux, macOS ou Git Bash no Windows). No PowerShell, use `release.ps1`.
- Acesso à internet na primeira execução para baixar imagens e dependências.

O backend é compilado dentro da imagem Docker. Não é necessário instalar Java ou Gradle para executar o conjunto.

## Subir o sistema completo com Docker

Na raiz do repositório, execute um dos comandos:

```bash
bash release.sh up
```

No PowerShell:

```powershell
.\release.ps1 up
```

O comando prepara os arquivos locais `.env` e `api-ia/.env` a partir dos exemplos, compila e sobe PostgreSQL, Mailpit, API de IA, backend e frontend. O Compose usa o stub da API de IA por padrão, tornando as jornadas locais repetíveis sem depender do tempo de resposta da NVIDIA. Para ativar a NVIDIA, defina `IVIS_IA_USAR_STUB=false` no `.env` da raiz e informe a chave em `api-ia/.env`. O script espera os serviços de IA, backend/banco e frontend responderem antes de encerrar.

Endereços padrão:

| Serviço | Endereço |
| --- | --- |
| Frontend em container | `http://localhost:8080` |
| Backend | `http://localhost:8081/api` |
| API de IA | `http://localhost:8000` |
| Mailpit (caixa de e-mail local) | `http://localhost:8025` |
| PostgreSQL | `localhost:5433` |

Os endpoints de prontidão são `http://localhost:8081/api/v1/pronto` para backend e banco e `http://localhost:8000/pronto` para a API de IA. O serviço de banco não é exposto fora da máquina local.

### Comandos para administrar os containers

```bash
bash release.sh status
bash release.sh logs
bash release.sh restart
bash release.sh down
```

Os mesmos comandos podem ser usados com `.\release.ps1` no PowerShell. `down` remove os containers e preserva os dados do volume `ivis-banco-dados`. Para apagar os dados também, execute conscientemente `docker compose down -v`.

Para acompanhar por mais tempo a inicialização no Bash, defina `IVIS_RELEASE_TIMEOUT_SECONDS` antes de usar `up` (padrão: 300 segundos).

## Rodar o frontend com `npm run dev`

Também é possível executar o frontend diretamente pelo Vite. Primeiro, inicie backend, banco e API de IA com Docker:

```powershell
.\release.ps1 up
```

Abra outro terminal na raiz do repositório e execute:

```powershell
Set-Location .\frontend
npm ci
npm run dev
```

No Bash, os comandos equivalentes são:

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. O frontend usa `http://localhost:8081/api` como URL padrão do backend. Para apontar a outro backend durante o desenvolvimento, crie `frontend/.env.local` com:

```dotenv
VITE_API_URL=http://localhost:8081/api
```

`VITE_API_URL` é incorporada ao código entregue ao navegador. Nunca coloque senhas, tokens ou chaves privadas em variáveis com prefixo `VITE_`.

Para compilar o frontend localmente:

```bash
cd frontend
npm ci
npm run build
```

O resultado é gerado em `frontend/dist/`.

## Instalar dependências para os testes

Execute na raiz do repositório. Estes comandos instalam as dependências JavaScript e criam um ambiente Python isolado para os testes da API de IA:

```powershell
npm ci
npm --prefix frontend ci
npm --prefix cypress/e2e ci
py -m venv api-ia/.venv
& .\api-ia\.venv\Scripts\python.exe -m pip install -r api-ia/requirements.txt
```

Também é necessário ter Java 25, Gradle disponível no terminal e Docker Desktop ativo para os testes de integração do backend. Os testes Python e os testes de interface com mocks não precisam do banco; as jornadas reais Cypress usam a stack Docker.

## Executar os testes e verificações

Os comandos abaixo partem da raiz do repositório e usam PowerShell.

### Testes da API de IA (Python/pytest)

```powershell
Push-Location .\api-ia
& .\.venv\Scripts\python.exe -m pytest tests -q
Pop-Location
```

Para executar um arquivo ou caso específico:

```powershell
Push-Location .\api-ia
python -m pytest tests/test_api.py -q
python -m pytest tests/test_provedor_nvidia.py -q
python -m pytest tests/test_medicao.py -q
python -m pytest tests/test_api.py::test_gerar_pergunta_retorna_resposta_deterministica -q
Pop-Location
```

Esses testes usam o stub e provedores HTTP falsos; não chamam a NVIDIA nem precisam de uma chave real.

### Testes do backend (JUnit/Gradle)

Com Docker Desktop iniciado, execute a suíte unitária e de integração:

```powershell
gradle -p backend test
```

Os testes de integração usam Testcontainers e iniciam PostgreSQL. Para filtrar uma classe ou um método JUnit:

```powershell
gradle -p backend test --tests "br.ivis.EsqueletoIntegracaoTest"
gradle -p backend test --tests "br.ivis.identidade.ServicoPerfilAlunoTest"
gradle -p backend test --tests "br.ivis.EsqueletoIntegracaoTest.trocaDeSenhaExigeSenhaAtualERevogaRenovacoesAnteriores"
```

### Verificações do frontend

```powershell
npm --prefix frontend run test:unit
npm --prefix frontend run build
npm --prefix frontend run verificar-estilo
```

`build` também executa a verificação de tipos TypeScript/Vue. Para verificar dependências e arquivos JavaScript/TypeScript não utilizados pelo projeto:

```powershell
npm run deps:check
npm run deps:audit
```

Os testes unitários usam Vitest e exercitam componentes Vue e o fluxo de treinamento com respostas da API simuladas. Para acompanhar os testes enquanto edita:

```powershell
npm --prefix frontend run test:unit:watch
```

### Testes Cypress de CDU com APIs simuladas

Inicie o backend, o banco e a API de IA com a stack local:

```powershell
.\release.ps1 up
```

Em outro terminal, sirva o frontend atual pelo Vite em `http://localhost:5173`:

```powershell
npm --prefix frontend run dev
```

O Cypress usa `http://localhost:5173` por padrão. Use `localhost` exatamente; o backend permite essa origem CORS e não permite `127.0.0.1:5173`.

Os CDUs 01–14, exceto o 15, usam respostas simuladas e não limpam o banco. Para executar todos esses cenários:

```powershell
npm run testar:e2e -- --spec "cdu/CDU-[0-1][0-4]-*.cy.js"
```

Para executar um CDU isolado ou o grupo inteiro, use o mesmo comando com outro padrão:

```powershell
npm run testar:e2e -- --spec "cdu/CDU-04-*.cy.js"
npm run testar:e2e -- --spec "cdu/CDU-07-*.cy.js"
npm run testar:e2e -- --spec "cdu/CDU-*.cy.js"
```

O último comando inclui o CDU-15 real. As jornadas de `caminho_feliz/` também usam backend e banco reais. **Atenção:** antes e depois de cada spec real, os hooks executam `TRUNCATE` nas tabelas transacionais do banco `ivis`. Rode esses cenários somente quando os dados do banco local puderem ser apagados; não use um banco compartilhado ou com dados que queira preservar.

### Jornadas Cypress reais e CDU-15

Confirme que a stack está ativa e que o banco local `ivis` contém apenas dados descartáveis. Os hooks limpam automaticamente as tabelas transacionais antes e depois das jornadas reais; não é necessária variável de ambiente:

```powershell
npm run testar:e2e -- --spec "caminho_feliz/*.cy.js"
```

Para testar a gravação da auditoria da contratação real:

```powershell
npm run testar:e2e -- --spec "cdu/CDU-15-*.cy.js"
```

Para executar a suíte Cypress completa, incluindo CDUs simulados, CDU-15 e jornadas reais:

```powershell
npm run testar:e2e
```

O Cypress recusa a limpeza se o nome do banco não for `ivis`, mas essa verificação não confirma se os dados são descartáveis. Execute as jornadas reais somente contra o banco local do Compose sem dados que queira preservar.

Para abrir o Cypress em modo interativo, use a pasta do pacote:

```powershell
Set-Location .\cypress\e2e
npx cypress open
```

No painel do Cypress, selecione o spec desejado. Para executar sem interface, os vídeos ficam em `cypress/downloads` e as capturas de falhas em `cypress/screenshots`.

### Resumo para rodar todas as verificações automatizadas

Depois de instalar as dependências, com Java 25 e Docker Desktop disponíveis e a stack IVIS iniciada, a sequência completa é:

```powershell
Push-Location .\api-ia
python -m pytest tests -q
Pop-Location
gradle -p backend test
npm --prefix frontend run test:unit
npm --prefix frontend run build
npm --prefix frontend run verificar-estilo
npm run deps:check
npm run deps:audit
npm run testar:e2e
```

O último comando Cypress pode apagar os dados transacionais do banco local `ivis`; faça a execução completa somente com dados descartáveis.

## Primeiro acesso e planos demonstrativos

- A raiz do site abre a apresentação pública; o acesso à área do aluno/instituição exige sessão e plano individual ativo ou vínculo institucional ativo.
- Cadastro e login são processados pelo backend. O access token fica em memória no frontend; a renovação usa cookie HttpOnly.
- As migrations do Flyway criam e atualizam as tabelas e os planos demonstrativos.
- Na página `/planos`, o aluno pode contratar o plano individual e uma instituição pode contratar o plano acadêmico.
- O checkout usa cartões fictícios. Para aprovação use `4242 4242 4242 4242`, validade futura (por exemplo `12/30`) e CVV `123`. Para recusa use `4000 0000 0000 0002`, validade futura e CVV `123`.

Os valores são ilustrativos para a atividade acadêmica. Não há cobrança financeira. O fluxo de compra não envia uma mensagem real: a confirmação simulada fica registrada no sistema e não armazena PAN ou CVV.

## Usar a NVIDIA em vez do stub (opcional)

Edite `api-ia/.env`, que é local e não deve ser enviado ao Git:

```dotenv
IVIS_IA_USAR_STUB=false
NVIDIA_API_KEY=sua-chave
NVIDIA_MODEL=nvidia/nemotron-3.5-bolt-e4b
```

Recrie a API de IA:

```bash
docker compose up --build -d ivis-api-ia
```

Use a mesma `IVIS_IA_CHAVE_SERVICO` em `.env` e `api-ia/.env`. Se o provedor NVIDIA falhar, a aplicação não troca silenciosamente para o stub. As notas do stub são demonstrativas e não representam uma avaliação semântica real.

## Publicar o frontend na Vercel

A Vercel publica o frontend Vue/Vite deste repositório. Ela não executa a stack Docker inteira: PostgreSQL, backend Spring Boot e API FastAPI precisam estar publicados em serviços próprios que aceitem esses componentes. Veja o guia [Como publicar o IVIS na Vercel](vercel.md) para os passos, configurações e variáveis necessárias.

O script Bash também oferece `bash release.sh deploy-vercel` para iniciar uma implantação de produção do frontend já vinculado à Vercel. Esse comando não publica backend, API de IA nem banco.

## Solução de problemas

### Docker não está disponível

Se aparecer erro sobre `docker_engine`, abra o Docker Desktop e aguarde o mecanismo ficar pronto. Depois, tente novamente:

```powershell
.\release.ps1 up
```

### Porta ocupada

O PostgreSQL usa a porta local `5433`, configurável por `IVIS_BD_PORTA` no `.env`. Frontend, backend, API de IA e Mailpit usam as portas mostradas acima, definidas em `docker-compose.yml`. Libere a porta em uso ou altere o mapeamento correspondente.

### Backend ou banco não ficam prontos

Confira o estado e os logs:

```powershell
.\release.ps1 status
.\release.ps1 logs
```

Confira se `.env` contém `IVIS_JWT_CHAVE_ASSINATURA` Base64 válida com pelo menos 32 bytes e se `IVIS_IA_CHAVE_SERVICO` coincide com o valor de `api-ia/.env`.

### Frontend não consegue acessar o backend

Confirme que backend está pronto em `http://localhost:8081/api/v1/pronto`, que `VITE_API_URL` aponta para `http://localhost:8081/api` e que a origem do frontend (`http://localhost:5173` para Vite ou `http://localhost:8080` para o container) está em `IVIS_CORS_ORIGENS` no `.env`. Reinicie o backend após alterar o arquivo.

### Serviço de IA retorna indisponível

Com o stub ativo, confirme `IVIS_IA_USAR_STUB=true` em `api-ia/.env`. Se o stub estiver desativado, confira a chave NVIDIA e o acesso ao provedor.

## Estrutura dos módulos

- `frontend/`: Vue 3, TypeScript, Vite e Bootstrap.
- `backend/`: API Spring Boot, autenticação, planos, perfis, habilidades, vagas e vínculos institucionais.
- `api-ia/`: API FastAPI com stub e adaptador NVIDIA.
- `docker-compose.yml`: PostgreSQL, Mailpit, backend, API de IA e frontend.
- `release.sh`: inicia e administra a stack local e pode publicar o frontend na Vercel.
- `release.ps1`: inicia e administra a stack local no Windows/PowerShell.
