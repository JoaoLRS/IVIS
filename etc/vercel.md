# Como publicar o IVIS na Vercel

Este guia publica o frontend Vue/Vite e o backend Spring Boot em um projeto Vercel. A API FastAPI de IA é publicada em um projeto Vercel separado, com URL própria. O PostgreSQL é gerenciado separadamente; este guia usa Neon como exemplo.

## Arquitetura de publicação

```text
Navegador
  └── Projeto Vercel do IVIS (domínio único)
        ├── /api/* → Backend Spring Boot ──> Neon PostgreSQL
        └── demais caminhos → Frontend Vue/Vite
                              Backend ── HTTPS + X-Chave-Servico ──> Projeto Vercel da API de IA
```

O navegador chama somente o backend para operações da API. A API de IA tem uma URL pública, mas as rotas de negócio exigem `X-Chave-Servico`; não exponha essa chave no frontend. Como os serviços estão em projetos separados, configure `IVIS_IA_URL` manualmente no projeto do backend. Cadastre a mesma chave de serviço nos dois projetos.

## 1. Prepare o banco Neon

Crie um projeto Neon de produção e um banco/role exclusivos para o IVIS. No Neon Console, use a opção de conexão PostgreSQL/JDBC e anote o host, o nome do banco, o usuário e a senha. Exija TLS. O backend já inclui o driver PostgreSQL JDBC; não instale `@neondatabase/serverless`, que é um driver Node.js e não é usado pelo backend Java.

O Flyway executa as migrations do backend ao iniciar. Use um banco de produção vazio ou faça backup e revise as migrations antes de apontar para um banco que já contenha dados.

## 2. Configure as variáveis de ambiente na Vercel

Configure as variáveis do ambiente **Production** nos projetos indicados abaixo. Variáveis de um projeto Vercel não são compartilhadas com outro; cadastre a chave de serviço em ambos. Copie os valores do Neon Console; não inclua credenciais em arquivos versionados nem em variáveis `VITE_*`.

| Variável | Onde | Valor/observação |
| --- | --- | --- |
| `IVIS_BD_URL` | Projeto do IVIS (backend) | URL JDBC com o host, a porta e o banco do Neon, por exemplo `jdbc:postgresql://HOST.neon.tech/neondb?sslmode=require`. Não use a URI `postgresql://` do driver Node sem convertê-la para o formato JDBC. |
| `IVIS_BD_USUARIO` | Projeto do IVIS (backend) | Usuário/role de banco do Neon. |
| `IVIS_BD_SENHA` | Projeto do IVIS (backend) | Senha do usuário/role; cadastrar como secret. |
| `IVIS_JWT_CHAVE_ASSINATURA` | Projeto do IVIS (backend) | Chave exclusiva de produção, Base64 e com ao menos 32 bytes aleatórios. Não reutilize a chave local. |
| `IVIS_COOKIE_SEGURO` | Projeto do IVIS (backend) | `true` em HTTPS. |
| `IVIS_CORS_ORIGENS` | Projeto do IVIS (backend) | Origens exatas dos frontends autorizados, separadas por vírgula e sem barra final. Para Production, use `https://ivis-ia.vercel.app`; para um deployment Preview, inclua a origem exata do Preview, como `https://ivis-b4s65t63k-lucifer-son.vercel.app`, no ambiente correspondente. |
| `IVIS_URL_REDEFINICAO_SENHA` | Projeto do IVIS (backend) | URL completa da página do frontend, por exemplo `https://ivis-ia.vercel.app/redefinir-senha`. |
| `IVIS_IA_URL` | Projeto do IVIS (backend) | URL base HTTPS pública do projeto da API de IA, sem `/v1/...`, por exemplo `https://ivis-api-ia.vercel.app`. |
| `IVIS_IA_CHAVE_SERVICO` | Projetos do IVIS (backend) e da API de IA | Mesmo segredo aleatório, forte e exclusivo nos dois projetos. Use ao menos 32 caracteres e cadastre-o como secret. |
| `IVIS_IA_USAR_STUB` | Projeto da API de IA | `true` para a demonstração acadêmica sem chave de modelo; `false` somente se configurar um provedor de IA real. |

Se instalar a integração Neon na Vercel, ela pode fornecer uma `DATABASE_URL` no formato URI padrão `postgresql://`. O backend Spring não consome essa variável diretamente: configure `IVIS_BD_URL`, `IVIS_BD_USUARIO` e `IVIS_BD_SENHA` separadamente com host, banco e credenciais do Neon. Prefira o endpoint com pooler mostrado no Neon Console para a aplicação hospedada.

Para habilitar NVIDIA, configure também `NVIDIA_API_KEY` e `NVIDIA_MODEL` como secrets no projeto da API de IA. Não use a chave em uma variável `VITE_*`.

A chave de serviço é obrigatória ao iniciar a API de IA. Configure-a no ambiente **Production** (e **Preview**, se usado) do projeto da API de IA e no ambiente correspondente do projeto do IVIS. Depois de alterar variáveis, faça novo deploy dos projetos afetados; os deployments existentes não recebem as alterações retroativamente.

O fluxo de checkout é acadêmico e simulado: não integra uma adquirente nem movimenta dinheiro; o cartão fictício não deve ser tratado como um pagamento real. A confirmação da contratação é registrada pelo sistema. O envio de e-mail para recuperação de senha é separado do e-mail simulado de contratação. Hoje o backend permite configurar `IVIS_SMTP_HOST`, `IVIS_SMTP_PORTA` e `IVIS_EMAIL_REMETENTE`; autenticação SMTP e TLS não estão expostas como variáveis de configuração, então valide a compatibilidade do provedor antes de depender do envio real em produção.

## 3. Configure o domínio e os cookies de sessão

O backend define o cookie de renovação como `HttpOnly`, `Secure` em produção e `SameSite=Strict`. Como frontend e backend são roteados no mesmo domínio, configure `IVIS_CORS_ORIGENS` com a origem exata desse domínio. Em produção com domínio próprio, use o domínio de produção também em `IVIS_URL_REDEFINICAO_SENHA`.

## 4. Publique

Conecte o repositório Git ao projeto do IVIS e mantenha o Root Directory na raiz do repositório. A configuração [vercel.json](../vercel.json) publica `backend` em `/api/*` e direciona os demais caminhos para o frontend; ela não declara nem publica a API de IA. Configure o projeto da API de IA separadamente para implantar o serviço Python e disponibilizar sua URL HTTPS.

O container do backend usa `PORT` e define `80` como padrão na imagem, conforme o [contrato do runtime de container da Vercel](https://vercel.com/docs/functions/runtimes/container). Se configurar `PORT` nas variáveis do projeto, use a porta em que o serviço deve escutar. No Docker Compose local, essa variável é sobrescrita para `8081`, preservando o mapeamento usado pelo frontend e pelos testes locais.

Cadastre as variáveis de Production nos projetos correspondentes e faça o deploy de ambos. Para testar com segurança, configure também as variáveis de Preview e use um banco Neon de teste separado; não use o banco de produção em previews.

## 5. Verifique a aplicação publicada

1. Abra a URL de produção e confira se a apresentação carrega.
2. Abra diretamente `/planos` e `/login` em uma nova aba para validar os rewrites da SPA.
3. No navegador, confirme que as chamadas usam o mesmo domínio em `/api` e não `localhost`.
4. Cadastre uma conta de teste e confira login, renovação da sessão, logout e acesso aos planos.
5. Faça um checkout com os dados fictícios aprovados e recusados descritos em [como rodar](como%20rodar.md). Nenhuma transação real deve ocorrer.
6. Abra `https://<URL-DA-IA>/pronto` e confirme que a API de IA está pronta.
7. Envie `POST https://<URL-DA-IA>/v1/perguntas/gerar` com `X-Chave-Servico` e corpo `{}`: `422` indica que a aplicação respondeu e a chave passou, enquanto `401` indica chave ausente ou diferente. Não coloque a chave em logs, no frontend ou em chamados públicos.
8. Confira os logs do backend na Vercel e valide que o Flyway aplicou as migrations no banco Neon correto. Se a chamada do backend à IA falhar, confirme se `IVIS_IA_URL` aponta para a URL base correta e se `IVIS_IA_CHAVE_SERVICO` coincide nos dois projetos.

## Problemas comuns

### A página abre, mas as rotas internas dão 404

Confirme que o Root Directory da Vercel está na raiz e que o rewrite de fallback para o serviço `frontend` está presente em [vercel.json](../vercel.json).

### CORS bloqueia a API

Defina `IVIS_CORS_ORIGENS` no serviço `backend` com cada origem exata que deve acessar aquele ambiente, separadas por vírgula e sem curingas ou barra final. Para Production, inclua `https://ivis-ia.vercel.app`. Em Preview, inclua a URL exata do frontend Preview que originou a chamada. Reimplante o backend após alterar a variável.

### A sessão não renova ou o cookie não é enviado

Confirme HTTPS, `IVIS_COOKIE_SEGURO=true` e que frontend e backend usam o mesmo domínio público do projeto Vercel. O IVIS usa `SameSite=Strict`.

### Backend falha ao iniciar

Confira os logs do backend, `IVIS_BD_URL`, `IVIS_BD_USUARIO`, `IVIS_BD_SENHA`, a chave JWT e a configuração da API de IA. Para Neon, confirme o host e nome do banco, que `IVIS_BD_URL` começa com `jdbc:postgresql://` e inclui `?sslmode=require`, e que as conexões do ambiente Vercel são aceitas. Para a IA, confirme que `IVIS_IA_URL` está definido no projeto do IVIS e que a chave de serviço obrigatória é igual nos dois projetos. O health check do backend é `/api/v1/pronto`.

## Referências oficiais

- [Vercel Services](https://vercel.com/docs/services)
- [Runtime de container da Vercel](https://vercel.com/docs/functions/runtimes/container)
- [Variáveis de ambiente na Vercel](https://vercel.com/docs/environment-variables)
- [Integrar Neon com Vercel](https://neon.com/docs/guides/vercel-overview)
- [Conectar aplicações ao Neon](https://neon.com/docs/connect/connect-from-any-app)
