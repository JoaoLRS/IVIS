# OpenAPI e execução local

## Especificação da API

O backend publica a especificação OpenAPI gerada dos controllers e DTOs da aplicação:

- JSON: `http://localhost:8081/api/v3/api-docs`
- Swagger UI: `http://localhost:8081/api/swagger-ui/index.html`

No Docker Compose, use `http://localhost:8081/api/v3/api-docs` e
`http://localhost:8081/api/swagger-ui/index.html`. A especificação acompanha os
endpoints implementados; consulte-a para os esquemas, parâmetros e respostas
atualizados. Operações protegidas usam `Authorization: Bearer <access-token>`.
O refresh token é mantido pelo backend em cookie HttpOnly e não deve ser
copiado para o frontend ou incluído em logs.

## Subir a aplicação com Docker

Na raiz do repositório, copie `.env.example` para `.env`, defina
`IVIS_JWT_CHAVE_ASSINATURA` com uma chave Base64 gerada a partir de pelo menos
32 bytes aleatórios e mantenha `IVIS_IA_CHAVE_SERVICO` configurada. Não
versione `.env` nem compartilhe os valores.

```powershell
docker compose up --build
```

O Compose sobe frontend, backend, PostgreSQL, API de IA e Mailpit. A interface
do frontend fica em `http://localhost:8080`; a caixa local de e-mail fica em
`http://localhost:8025` e captura mensagens de recuperação sem encaminhá-las a
destinatários reais. Para encerrar, use `Ctrl+C`; para remover containers,
execute `docker compose down`. Os dados do PostgreSQL permanecem no volume
nomeado.

## Rodar o backend fora do Compose

Com PostgreSQL e Mailpit locais disponíveis, configure no ambiente do processo
as variáveis usadas pelo backend, especialmente `IVIS_BD_URL`,
`IVIS_BD_USUARIO`, `IVIS_BD_SENHA`, `IVIS_JWT_CHAVE_ASSINATURA` e
`IVIS_IA_CHAVE_SERVICO`. Em seguida:

```powershell
Set-Location backend
gradle bootRun
```

O frontend espera o backend em `http://localhost:8081/api`. Para executar e
validar os testes:

```powershell
Set-Location backend
gradle test
Set-Location ..\frontend
npm run build
npm run verificar-estilo
```

Os testes de integração que usam Testcontainers precisam de um daemon Docker
ativo. Para os cenários Cypress, mantenha o frontend acessível em
`http://localhost:5173` antes de executar `npm run testar:e2e`.
