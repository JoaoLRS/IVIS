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

O comando prepara os arquivos locais `.env` e `api-ia/.env` a partir dos exemplos, compila e sobe PostgreSQL, Mailpit, API de IA, backend e frontend. A API de IA usa o stub por padrão, então não é necessária uma chave NVIDIA para testar a aplicação. O script espera os serviços de IA, backend/banco e frontend responderem antes de encerrar.

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
