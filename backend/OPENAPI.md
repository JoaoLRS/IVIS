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

## Planos e contratação simulada

- `GET /api/v1/planos`: catálogo público de ofertas ativas, ordenado por modalidade,
  preço e código. Cada oferta contém `id`, `codigo`, `modalidade`, `nome`,
  `descricao`, `precoCentavos`, `moeda`, `duracaoDias` e `beneficios` na ordem cadastrada.
- `GET /api/v1/planos/{id}`: detalhes públicos de uma oferta ativa; ofertas
  inativas e identificadores inexistentes retornam `404 plano_nao_encontrado`.
- `GET /api/v1/conta/plano`: exige Bearer. A conta é extraída exclusivamente do
  principal autenticado e precisa continuar ativa no banco. Retorna `situacao`
  (`COM_PLANO_VIGENTE` ou `SEM_PLANO_VIGENTE`) e a lista `planos`. O acesso
  próprio deixa de ser vigente ao atingir `expiraEm`.

Cada item de `planos` contém `contratoId`, `planoId`, `nomePlano`, `origemAcesso`
(`PROPRIO` ou `VINCULO_INSTITUCIONAL`), `situacao`, `iniciadoEm`, `expiraEm`, `encerradoEm`,
`condicoesTitular` e `beneficios`. Somente o titular recebe `condicoesTitular`,
com `quantidadeVagas`, `precoCentavosContratado` e `moeda`. Para aluno vinculado,
esse campo é `null`; a API não retorna identificadores da conta institucional.

A consulta considera contrato `ATIVO`, já iniciado e não encerrado. Um aluno
consulta contratos individuais próprios e contratos institucionais dos seus
vínculos ativos, já iniciados e não encerrados. Instituições consultam apenas
seus contratos institucionais. Desativar uma oferta no catálogo não encerra
contratos existentes. Não se presume exclusividade de contrato enquanto essa
política permanecer pendente: a resposta admite múltiplos contratos vigentes.

`POST /api/v1/contratacoes/simuladas` exige sessão autenticada com tipo de conta
compatível com a modalidade (aluno para individual, instituição para acadêmico), cabeçalho
`Idempotency-Key` e token CSRF. O corpo recebe `planoId`, `numeroCartao`,
`validade` (MM/AA ou MM/AAAA) e `cvv`. O servidor só aceita cartões de teste
documentados na tela de checkout: `4242 4242 4242 4242` para aprovação ou
`4000 0000 0000 0002` para recusa; ambos usam CVV `123` e validade futura.
O servidor calcula o preço e a duração, não confia em resultado enviado pelo
cliente e retorna a situação simulada do contrato e pagamento. A chave de
idempotência permite repetir uma requisição sem criar uma segunda cobrança
simulada.

Este fluxo é acadêmico: não conecta a adquirente, não movimenta dinheiro, não
envia e-mail real e não grava número do cartão/CVV. Em aprovações, o backend
registra uma mensagem em `emails_simulados` e devolve a prévia na resposta para
exibição autenticada no checkout. A migration V4 inclui uma oferta individual
demonstrativa de 30 dias por R$ 19,90. A migration V5 inclui o Plano Acadêmico
institucional por 30 dias, com capacidade para 10 alunos, por R$ 99,90. Ambos
os valores são ilustrativos, sem cobrança real. Uma instituição contrata o
plano acadêmico com sua conta institucional; depois, pode vincular alunos até
o limite de vagas. Preço, duração e capacidade são definidos no catálogo.

## Subir a aplicação com Docker

Na raiz do repositório, copie `.env.example` para `.env`, defina
`IVIS_JWT_CHAVE_ASSINATURA` com uma chave Base64 gerada a partir de pelo menos
32 bytes aleatórios e mantenha `IVIS_IA_CHAVE_SERVICO` configurada. Não
versione `.env` nem compartilhe os valores.

```powershell
docker compose up --build
```

O Compose sobe frontend, backend, PostgreSQL, API de IA e Mailpit. As migrations
criam o catálogo demonstrativo na migration V4. A interface
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
