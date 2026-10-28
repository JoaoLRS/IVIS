# Como publicar o IVIS na Vercel

Este guia publica o frontend Vue/Vite na Vercel e conecta a aplicação aos serviços publicados separadamente. O IVIS é uma aplicação composta por vários processos: a Vercel recebe o frontend estático, mas o PostgreSQL, o backend Spring Boot e a API FastAPI de IA precisam continuar executando em serviços compatíveis com containers.

## Arquitetura de publicação

```text
Navegador
  └── Frontend Vue/Vite (Vercel)
        └── Backend Spring Boot (serviço público HTTPS)
              ├── PostgreSQL gerenciado
              └── API FastAPI de IA (acessível pelo backend)
```

O navegador chama somente o backend. O endereço da API de IA e as credenciais do banco ficam no ambiente do servidor e não devem ser configurados no frontend.

## 1. Publique os serviços de servidor

Antes de publicar o frontend, tenha disponíveis:

1. Um PostgreSQL acessível pelo backend. Crie um banco e um usuário próprios para o IVIS e habilite TLS conforme exigido pelo provedor.
2. A API de IA, construída a partir de `api-ia/Dockerfile`, com endpoint de prontidão `/pronto`.
3. O backend, construído a partir de `backend/Dockerfile`, com endpoint público HTTPS e endpoint de prontidão `/api/v1/pronto`.

Use uma plataforma que mantenha processos Java e Python em execução como serviços de aplicação/container. Configure health check do backend para `/api/v1/pronto`. Ao iniciar, o Flyway aplica as migrations do backend ao banco; faça backup antes de atualizar um banco com dados importantes.

O backend deve conseguir acessar a API FastAPI pela rede do provedor. Prefira um endereço interno entre serviços para `IVIS_IA_URL`; não exponha a API de IA diretamente ao navegador.

## 2. Configure as variáveis do backend e da API de IA

Configure os valores no gerenciador de secrets da plataforma de backend. Não os grave no repositório.

| Variável | Onde | Valor/observação |
| --- | --- | --- |
| `IVIS_BD_URL` | Backend | URL JDBC, por exemplo `jdbc:postgresql://HOST:5432/ivis?sslmode=require`. Use o host, porta e banco fornecidos pelo PostgreSQL. |
| `IVIS_BD_USUARIO` | Backend | Usuário de banco dedicado ao IVIS. |
| `IVIS_BD_SENHA` | Backend | Senha desse usuário. |
| `IVIS_JWT_CHAVE_ASSINATURA` | Backend | Chave exclusiva de produção, Base64 e com ao menos 32 bytes aleatórios. Não reutilize a chave local. |
| `IVIS_COOKIE_SEGURO` | Backend | `true` em HTTPS. |
| `IVIS_CORS_ORIGENS` | Backend | Origem exata do frontend, por exemplo `https://app.seudominio.com`, sem barra final. Para múltiplas origens, separe com vírgulas. |
| `IVIS_URL_REDEFINICAO_SENHA` | Backend | URL completa da página do frontend, por exemplo `https://app.seudominio.com/redefinir-senha`. |
| `IVIS_IA_URL` | Backend | URL interna/pública HTTPS da API de IA, sem path adicional se o provedor expõe a raiz do FastAPI. |
| `IVIS_IA_CHAVE_SERVICO` | Backend e API de IA | Mesmo segredo forte e exclusivo nos dois serviços. |
| `IVIS_IA_USAR_STUB` | API de IA | `true` para a demonstração acadêmica sem chave de modelo; `false` somente se for configurar um provedor de IA real. |

Para habilitar NVIDIA, configure também `NVIDIA_API_KEY` e `NVIDIA_MODEL` como secrets da API de IA. Não use a chave em uma variável `VITE_*`.

O fluxo de checkout é acadêmico e simulado: não integra uma adquirente nem movimenta dinheiro; o cartão fictício não deve ser tratado como um pagamento real. A confirmação da contratação é registrada pelo sistema. O envio de e-mail para recuperação de senha é separado do e-mail simulado de contratação. Hoje o backend permite configurar `IVIS_SMTP_HOST`, `IVIS_SMTP_PORTA` e `IVIS_EMAIL_REMETENTE`; autenticação SMTP e TLS não estão expostas como variáveis de configuração, então valide a compatibilidade do provedor antes de depender do envio real em produção.

## 3. Configure o domínio e os cookies de sessão

O backend define o cookie de renovação como `HttpOnly`, `Secure` em produção e `SameSite=Strict`. Para o navegador enviá-lo nas chamadas autenticadas, publique frontend e backend sob o mesmo domínio registrável, por exemplo:

- Frontend: `https://app.seudominio.com`
- Backend: `https://api.seudominio.com`

Configure um domínio próprio para o projeto Vercel e um subdomínio para o backend. O frontend deve usar o endereço HTTPS do backend em `VITE_API_URL`. Evite combinar o domínio padrão `*.vercel.app` com uma API em outro domínio registrável: o cookie `SameSite=Strict` pode não acompanhar as chamadas e a renovação de sessão não funcionará.

Em `IVIS_CORS_ORIGENS`, informe a origem completa do frontend, sem curingas e sem barra no final. Se usar um domínio de preview para validar autenticação, inclua essa origem exata no CORS e confirme que frontend e API permanecem no mesmo site para o cookie.

## 4. Crie o projeto Vercel para o frontend

Conecte o repositório Git à Vercel e configure o projeto:

- **Root Directory:** `frontend`
- **Framework Preset:** Vite
- **Install Command:** `npm ci`
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Environment Variable:** `VITE_API_URL=https://api.seudominio.com/api`

Cadastre `VITE_API_URL` nos ambientes Preview e Production conforme necessário. Variáveis Vite são incorporadas aos arquivos do navegador: esse endereço é público e não pode conter segredo. Depois de alterar uma variável, gere um novo deployment para que o build use o novo valor.

O arquivo `frontend/vercel.json` já redireciona caminhos da SPA para `index.html`, permitindo abrir diretamente rotas como `/login`, `/planos` e `/checkout/...`.

## 5. Publique

### Pela integração Git

Faça push da branch conectada. A Vercel instala as dependências, executa `npm run build` e publica `frontend/dist`. Use um deployment Preview para conferir o build antes de promover a versão de produção.

### Pelo Vercel CLI

Instale e autentique o CLI, vincule o projeto a partir de `frontend/` uma vez e publique:

```bash
npm install --global vercel
vercel login
cd frontend
vercel link
cd ..
bash release.sh deploy-vercel
```

O comando `deploy-vercel` do script publica somente `frontend/` em produção. Backend, API de IA e banco devem ser atualizados na plataforma em que foram hospedados. Para uma implantação de preview manual, entre em `frontend/` e execute `vercel` sem `--prod`.

## 6. Verifique a aplicação publicada

1. Abra a URL de produção e confira se a apresentação carrega.
2. Abra diretamente `/planos` e `/login` em uma nova aba para validar os rewrites da SPA.
3. No navegador, confirme que as chamadas usam `https://api.seudominio.com/api` e não `localhost`.
4. Cadastre uma conta, confira login, renovação da sessão, logout e acesso aos planos.
5. Faça um checkout com os dados fictícios aprovados e recusados descritos em [como rodar](como%20rodar.md). Nenhuma transação real deve ocorrer.
6. Se a chamada falhar, confira primeiro o CORS do backend, `VITE_API_URL`, HTTPS, domínio dos cookies e logs do serviço backend.

## Problemas comuns

### A página abre, mas as rotas internas dão 404

Confirme que `frontend/vercel.json` foi incluído no repositório e que o Root Directory do projeto é `frontend`.

### O navegador tenta acessar localhost

`VITE_API_URL` não foi definida para o ambiente do deployment ou o deployment não foi reconstruído após sua alteração. Configure `https://api.seudominio.com/api` na Vercel e publique novamente.

### CORS bloqueia a API

Defina `IVIS_CORS_ORIGENS` no backend com a origem exata, como `https://app.seudominio.com`, sem curingas. Reinicie/reimplante o backend após mudar a configuração.

### A sessão não renova ou o cookie não é enviado

Confirme HTTPS, `IVIS_COOKIE_SEGURO=true`, chamadas do frontend com credenciais e os domínios. O IVIS usa `SameSite=Strict`; frontend e API devem estar sob o mesmo domínio registrável para que o cookie acompanhe as chamadas.

### Backend falha ao iniciar

Confira os logs do serviço, a conectividade e os dados JDBC, a chave JWT, as secrets compartilhadas com a API de IA e o health check `/api/v1/pronto`. Confirme também que o banco permite conexões TLS do host do backend.

## Referências oficiais

- [Vite na Vercel](https://vercel.com/docs/frameworks/frontend/vite)
- [Configurar build e Root Directory](https://vercel.com/docs/builds/configure-a-build)
- [Variáveis de ambiente na Vercel](https://vercel.com/docs/environment-variables)
- [Comando Vercel CLI deploy](https://vercel.com/docs/cli/deploy)
