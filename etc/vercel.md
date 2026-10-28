# Como publicar o IVIS na Vercel

Este guia publica o frontend Vue/Vite, o backend Spring Boot e a API FastAPI de IA como serviços do mesmo projeto Vercel. O PostgreSQL é gerenciado separadamente; este guia usa Neon como exemplo.

## Arquitetura de publicação

```text
Navegador
  └── Projeto Vercel (domínio único)
        ├── /api/* → Backend Spring Boot
        │             ├── Neon PostgreSQL
        │             └── API FastAPI de IA (binding interno)
        └── demais caminhos → Frontend Vue/Vite
```

O navegador chama somente o backend para operações da API. A API de IA não tem rota pública: o binding do backend injeta sua URL em `IVIS_IA_URL`. As credenciais ficam em variáveis de ambiente do projeto e são consumidas pelo backend, nunca pelo frontend.

## 1. Prepare o banco Neon

Crie um projeto Neon de produção e um banco/role exclusivos para o IVIS. No Neon Console, use a opção de conexão PostgreSQL/JDBC e anote o host, o nome do banco, o usuário e a senha. Exija TLS. O backend já inclui o driver PostgreSQL JDBC; não instale `@neondatabase/serverless`, que é um driver Node.js e não é usado pelo backend Java.

O Flyway executa as migrations do backend ao iniciar. Use um banco de produção vazio ou faça backup e revise as migrations antes de apontar para um banco que já contenha dados.

## 2. Configure as variáveis de ambiente na Vercel

No projeto Vercel, configure as variáveis do ambiente **Production**. As variáveis são configuradas no projeto e compartilhadas pelos serviços; os itens marcados como backend são consumidos pelo Spring Boot. Copie os valores do Neon Console; não inclua credenciais em arquivos versionados nem em variáveis `VITE_*`.

| Variável | Onde | Valor/observação |
| --- | --- | --- |
| `IVIS_BD_URL` | Serviço `backend` | URL JDBC com o host, a porta e o banco do Neon, por exemplo `jdbc:postgresql://HOST.neon.tech/neondb?sslmode=require`. Não use a URI `postgresql://` do driver Node sem convertê-la para o formato JDBC. |
| `IVIS_BD_USUARIO` | Serviço `backend` | Usuário/role de banco do Neon. |
| `IVIS_BD_SENHA` | Serviço `backend` | Senha do usuário/role; cadastrar como secret. |
| `IVIS_JWT_CHAVE_ASSINATURA` | Serviço `backend` | Chave exclusiva de produção, Base64 e com ao menos 32 bytes aleatórios. Não reutilize a chave local. |
| `IVIS_COOKIE_SEGURO` | Serviço `backend` | `true` em HTTPS. |
| `IVIS_CORS_ORIGENS` | Serviço `backend` | Origem exata do domínio do projeto Vercel, sem barra final, por exemplo `https://ivis-ia.vercel.app`. |
| `IVIS_URL_REDEFINICAO_SENHA` | Serviço `backend` | URL completa da página do frontend, por exemplo `https://ivis-ia.vercel.app/redefinir-senha`. |
| `IVIS_IA_CHAVE_SERVICO` | Serviços `backend` e `api-ia` | Mesmo segredo forte e exclusivo nos dois serviços. |
| `IVIS_IA_USAR_STUB` | Serviço `api-ia` | `true` para a demonstração acadêmica sem chave de modelo; `false` somente se configurar um provedor de IA real. |

Se instalar a integração Neon na Vercel, ela pode fornecer uma `DATABASE_URL` no formato URI padrão `postgresql://`. O backend Spring não consome essa variável diretamente: configure `IVIS_BD_URL`, `IVIS_BD_USUARIO` e `IVIS_BD_SENHA` separadamente com host, banco e credenciais do Neon. Prefira o endpoint com pooler mostrado no Neon Console para a aplicação hospedada.

Para habilitar NVIDIA, configure também `NVIDIA_API_KEY` e `NVIDIA_MODEL` como secrets da API de IA. Não use a chave em uma variável `VITE_*`.

Não defina `IVIS_IA_URL` manualmente na Vercel: o binding declarado no serviço `backend` injeta essa variável com a URL interna de `api-ia`. A variável não está disponível durante o build do frontend.

O fluxo de checkout é acadêmico e simulado: não integra uma adquirente nem movimenta dinheiro; o cartão fictício não deve ser tratado como um pagamento real. A confirmação da contratação é registrada pelo sistema. O envio de e-mail para recuperação de senha é separado do e-mail simulado de contratação. Hoje o backend permite configurar `IVIS_SMTP_HOST`, `IVIS_SMTP_PORTA` e `IVIS_EMAIL_REMETENTE`; autenticação SMTP e TLS não estão expostas como variáveis de configuração, então valide a compatibilidade do provedor antes de depender do envio real em produção.

## 3. Configure o domínio e os cookies de sessão

O backend define o cookie de renovação como `HttpOnly`, `Secure` em produção e `SameSite=Strict`. Como frontend e backend são roteados no mesmo domínio, configure `IVIS_CORS_ORIGENS` com a origem exata desse domínio. Em produção com domínio próprio, use o domínio de produção também em `IVIS_URL_REDEFINICAO_SENHA`.

## 4. Publique

Conecte o repositório Git à Vercel e mantenha o Root Directory na raiz do repositório para que a configuração [vercel.json](../vercel.json) descubra os três serviços. Ela publica `backend` em `/api/*`, direciona os demais caminhos para o frontend e mantém `api-ia` interna. Cadastre as variáveis de Production descritas acima e faça o deploy. Para testar com segurança, configure também as variáveis de Preview para um banco Neon de teste separado; não use o banco de produção em previews.

## 5. Verifique a aplicação publicada

1. Abra a URL de produção e confira se a apresentação carrega.
2. Abra diretamente `/planos` e `/login` em uma nova aba para validar os rewrites da SPA.
3. No navegador, confirme que as chamadas usam o mesmo domínio em `/api` e não `localhost`.
4. Cadastre uma conta de teste e confira login, renovação da sessão, logout e acesso aos planos.
5. Faça um checkout com os dados fictícios aprovados e recusados descritos em [como rodar](como%20rodar.md). Nenhuma transação real deve ocorrer.
6. Confira os logs do serviço `backend` na Vercel e valide que o Flyway aplicou as migrations no banco Neon correto.

## Problemas comuns

### A página abre, mas as rotas internas dão 404

Confirme que o Root Directory da Vercel está na raiz e que o rewrite de fallback para o serviço `frontend` está presente em [vercel.json](../vercel.json).

### CORS bloqueia a API

Defina `IVIS_CORS_ORIGENS` no serviço `backend` com a origem exata do projeto, sem curingas. Reimplante após alterar a variável.

### A sessão não renova ou o cookie não é enviado

Confirme HTTPS, `IVIS_COOKIE_SEGURO=true` e que frontend e backend usam o mesmo domínio público do projeto Vercel. O IVIS usa `SameSite=Strict`.

### Backend falha ao iniciar

Confira os logs do serviço `backend`, `IVIS_BD_URL`, `IVIS_BD_USUARIO`, `IVIS_BD_SENHA`, a chave JWT e as secrets compartilhadas com a API de IA. Para Neon, confirme o host e nome do banco, que `IVIS_BD_URL` começa com `jdbc:postgresql://` e inclui `?sslmode=require`, e que as conexões do ambiente Vercel são aceitas. O health check do backend é `/api/v1/pronto`.

## Referências oficiais

- [Vercel Services](https://vercel.com/docs/services)
- [Variáveis de ambiente na Vercel](https://vercel.com/docs/environment-variables)
- [Integrar Neon com Vercel](https://neon.com/docs/guides/vercel-overview)
- [Conectar aplicações ao Neon](https://neon.com/docs/connect/connect-from-any-app)
