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

## Consulta de planos (Nickolas)

- `GET /api/v1/planos`: catálogo público de ofertas ativas, ordenado por modalidade,
  preço e código. Cada oferta contém `id`, `codigo`, `modalidade`, `nome`,
  `descricao`, `precoCentavos`, `moeda` e `beneficios` na ordem cadastrada.
- `GET /api/v1/planos/{id}`: detalhes públicos de uma oferta ativa; ofertas
  inativas e identificadores inexistentes retornam `404 plano_nao_encontrado`.
- `GET /api/v1/conta/plano`: exige Bearer. A conta é extraída exclusivamente do
  principal autenticado e precisa continuar ativa no banco. Retorna `situacao`
  (`COM_PLANO_VIGENTE` ou `SEM_PLANO_VIGENTE`) e a lista `planos`.

Cada item de `planos` contém `contratoId`, `planoId`, `nomePlano`, `origemAcesso`
(`PROPRIO` ou `VINCULO_INSTITUCIONAL`), `situacao`, `iniciadoEm`, `encerradoEm`,
`condicoesTitular` e `beneficios`. Somente o titular recebe `condicoesTitular`,
com `quantidadeVagas`, `precoCentavosContratado` e `moeda`. Para aluno vinculado,
esse campo é `null`; a API não retorna identificadores da conta institucional.

A consulta considera contrato `ATIVO`, já iniciado e não encerrado. Um aluno
consulta contratos individuais próprios e contratos institucionais dos seus
vínculos ativos, já iniciados e não encerrados. Instituições consultam apenas
seus contratos institucionais. Desativar uma oferta no catálogo não encerra
contratos existentes. Não se presume exclusividade de contrato enquanto essa
política permanecer pendente: a resposta admite múltiplos contratos vigentes.

**Limites desta entrega:** o schema atual tem data de início e encerramento,
mas não possui data de expiração prevista. `encerradoEm` não significa validade
comercial, e `null` não promete acesso perpétuo. Os benefícios apresentados são
os do catálogo atual; o schema ainda não tem snapshot dos benefícios contratados.
O preço, porém, vem do snapshot existente no contrato. A consulta não substitui
autorização para iniciar treinos nem implementa contratação/pagamento.

Não foram inseridos preços ou ofertas fictícias no catálogo por migration.
Um banco novo retorna `[]` no catálogo até receber ofertas aprovadas. Valores
usados nos testes são sintéticos e ficam em banco isolado pelo Testcontainers.
As rotas são propostas implementadas para revisão da equipe; não se presume
aprovação de Alisson ou Mariana.

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
