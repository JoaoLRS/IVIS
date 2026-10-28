# IVIS — Backend

Etapa 1 da [Ordem de Entrega do MVP](../etc/plano/Ordem_de_Entrega_MVP.md): esqueleto executável com primeira integração da IA e identidade/sessão. Java 25 LTS, Spring Boot, Gradle, PostgreSQL e Flyway. O schema de identidade e domínio está nas migrations V2 e V3; controllers de conta, login e sessão estão disponíveis, enquanto os demais domínios continuam em implementação.

## Rodar com Docker (backend + banco + frontend)

Na raiz do repositório:

```bash
docker compose up --build
```

- Backend: <http://localhost:8081/api/v1/saude> e <http://localhost:8081/api/v1/pronto> (esta confere o banco).
- PostgreSQL: `127.0.0.1:5432`, banco `ivis`. O banco vazio é criado só pelas migrações Flyway.
- Copie `.env.example` para `.env` e preencha `IVIS_JWT_CHAVE_ASSINATURA` com a saída de `openssl rand -base64 32`; não reutilize nem versione essa chave.

## Rodar em desenvolvimento

Pré-requisitos: JDK 25, Docker e uma chave local de serviço (ao menos 32 caracteres).

```bash
docker compose up ivis-banco ivis-api-ia ivis-mailpit -d
cd backend
export IVIS_BD_SENHA=ivis_local
export IVIS_IA_CHAVE_SERVICO=chave-local-com-pelo-menos-32-caracteres
export IVIS_JWT_CHAVE_ASSINATURA="$(openssl rand -base64 32)"
export IVIS_COOKIE_SEGURO=false
gradle bootRun
```

O backend chama a API de IA usando `IVIS_IA_URL` e `X-Chave-Servico`; o navegador
chama somente as rotas do backend `/api/v1/ia/perguntas/gerar` e
`/api/v1/ia/avaliacoes/gerar`. Respostas da IA são validadas antes de serem
retornadas. Em falhas, o backend preserva as interações recebidas e devolve um
erro contratual sem repassar detalhes internos.

O Gradle Wrapper ainda não foi gerado. Uma vez, com Gradle instalado, rode `gradle wrapper --gradle-version 9.1.0` dentro de `backend/` e versione `gradlew`, `gradlew.bat` e `gradle/wrapper/`.

Testes (exigem Docker, usam Testcontainers): `gradle test`.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `IVIS_BD_URL` | `jdbc:postgresql://localhost:5432/ivis` | Conexão JDBC. |
| `IVIS_BD_USUARIO` | `ivis` | Usuário do banco. |
| `IVIS_BD_SENHA` | *(obrigatória)* | Senha do banco. Sem padrão no código. |
| `IVIS_PORTA` | `8081` | Porta HTTP. |
| `IVIS_CORS_ORIGENS` | `http://localhost:8080,http://localhost:5173` | Origens do frontend, separadas por vírgula. |
| `IVIS_JWT_CHAVE_ASSINATURA` | *(obrigatória, Base64 de pelo menos 32 bytes aleatórios)* | Chave HMAC-SHA256 de assinatura dos JWTs. Gerar por ambiente; nunca versionar nem reutilizar entre ambientes. |
| `IVIS_COOKIE_SEGURO` | `true` | Habilita `Secure` no cookie de refresh. Pode ser `false` somente para desenvolvimento local HTTP. |
| `IVIS_LOGIN_TENTATIVAS_MAX` | `10` | Tentativas inválidas por janela, aplicadas ao e-mail normalizado e ao IP de origem. |
| `IVIS_LOGIN_JANELA` | `15m` | Janela para os limites locais de login (duração Spring, por exemplo `15m`). |
| `IVIS_RECUPERACAO_TENTATIVAS_MAX` | `5` | Solicitações de recuperação por e-mail/IP e redefinições por IP na janela. |
| `IVIS_RECUPERACAO_JANELA` | `15m` | Janela dos limites locais de recuperação. |
| `IVIS_RECUPERACAO_VALIDADE` | `30m` | Validade do token de uso único (configuração aceita entre 5 minutos e 2 horas). |
| `IVIS_URL_REDEFINICAO_SENHA` | `http://localhost:8080/redefinir-senha` | URL do frontend incluída no e-mail de redefinição. |
| `IVIS_EMAIL_REMETENTE` | `no-reply@ivis.local` | Remetente das mensagens capturadas pelo Mailpit. |
| `IVIS_SMTP_HOST` / `IVIS_SMTP_PORTA` | `localhost` / `1025` | SMTP do Mailpit em desenvolvimento; no Compose o backend usa `ivis-mailpit:1025`. |
| `IVIS_IA_URL` | `http://localhost:8000` | URL base interna da API de IA; no Compose, usa `http://ivis-api-ia:8000`. |
| `IVIS_IA_CHAVE_SERVICO` | *(obrigatória, mínimo 32 caracteres)* | Chave serviço a serviço enviada ao serviço de IA. Nunca vai para o frontend. |
| `IVIS_IA_TIMEOUT_PERGUNTA` | `105s` | Timeout para geração de perguntas (90s da NVIDIA mais margem de rede). |
| `IVIS_IA_TIMEOUT_AVALIACAO` | `110s` | Timeout para avaliação (90s da NVIDIA mais margem de rede). |
| `SPRING_PROFILES_ACTIVE` | *(vazio)* | `estruturado` liga logs em JSON (ECS). |

## Convenções de API (Etapa 0, item 4)

- Base `/api` (servlet context path) + `/v1`: por exemplo `/api/v1/saude`. O `clienteHttp.ts` do frontend já aponta para `http://localhost:8081/api`.
- Identidade: `POST /api/v1/contas/alunos`, `POST /api/v1/contas/instituicoes`, `POST /api/v1/sessoes` (login), `POST /api/v1/sessoes/renovacao` e `DELETE /api/v1/sessoes/atual` (logout autenticado).
- Conta e senha: `GET/PATCH /api/v1/conta`, `POST /api/v1/conta/senha`, `POST /api/v1/senhas/recuperacao` e `POST /api/v1/senhas/redefinicao`. Perfil e senha autenticados derivam a conta do JWT; troca/redefinição de senha revoga todas as sessões de renovação existentes.
- Habilidades e vagas: `GET/POST /api/v1/habilidades`, `PUT/DELETE /api/v1/habilidades/{id}`, `GET/POST /api/v1/vagas` e `PUT/DELETE /api/v1/vagas/{id}`. Todas exigem JWT de uma conta com perfil de aluno; o titular vem do `sub`, nunca do payload.
- Operações institucionais: `GET /api/v1/instituicao/alunos`, `POST /api/v1/instituicao/vinculos`, `DELETE /api/v1/instituicao/vinculos/{id}` e `GET /api/v1/instituicao/resumo-vagas`. Todas exigem JWT de instituição; listagem e remoção são sempre delimitadas aos contratos pertencentes à conta autenticada.
- Criação de vínculo recebe `contratoId` e `contaAlunoId`; o servidor verifica que o contrato pertence à instituição, usa plano `INSTITUCIONAL` e está ativo, e que o destino é uma conta ativa de aluno. A capacidade é contada pelos vínculos `ATIVO`; o contrato é bloqueado durante a contagem/inserção e o índice único parcial do banco impede vínculos ativos duplicados entre instituições concorrentes.
- Remover vínculo encerra a linha histórica (`situacao=ENCERRADO`, `desvinculado_em`) em vez de apagá-la. `GET /instituicao/resumo-vagas` retorna um item por contrato institucional ativo com `quantidadeVagas`, `vagasUtilizadas` e `vagasDisponiveis`; não há contador persistido.
- Clientes tipados para esses endpoints estão em `frontend/src/servicos/instituicao.ts`; a interface institucional aguarda integração ao fluxo de sessão.
- Habilidade recebe `nome`, `categoria` (opcional) e `nivel` (1–5). Nome é aparado e normalizado para detectar duplicidade sem diferenciar maiúsculas/minúsculas; repetição retorna `409 habilidade_duplicada`.
- Vaga recebe `titulo`, `empresa` (opcional), `senioridade`, `descricao` (opcional) e `requisitos` (1 a 20 itens, cada um até 500 caracteres). Campos textuais obrigatórios não podem ficar vazios; requisitos repetidos após normalização são rejeitados. Respostas incluem o UUID, timestamps e requisitos ordenados.
- Exclusão de vaga é arquivamento: ela deixa de aparecer na listagem e não pode mais ser atualizada, mas a linha e seu UUID permanecem para satisfazer referências de treinos existentes. O JSON `contexto_registrado` do treino é snapshot e não é recalculado ao editar/arquivar a vaga; qualquer criação futura de treino deve persistir ali os campos da vaga e requisitos usados naquele momento. Exclusão de habilidade remove somente o cadastro e não altera snapshots históricos.
- O frontend tem funções tipadas em `frontend/src/servicos/perfilAluno.ts` para listar/criar/atualizar/excluir habilidades e vagas. Chamadas mutáveis recebem o access token e token CSRF; não existe mais dependência de IDs de array nos contratos da API.
- `PATCH /api/v1/conta` aceita `nome` e `email` para aluno, ou `razaoSocial` e `email` para instituição. `POST /api/v1/conta/senha` recebe `senhaAtual` e `novaSenha`; ambos exigem access token e CSRF.
- `POST /api/v1/senhas/recuperacao` recebe `{"email":"..."}` e responde `202` com a mesma mensagem para e-mail cadastrado ou não cadastrado. `POST /api/v1/senhas/redefinicao` recebe `{"token":"...","novaSenha":"..."}`; ambos exigem CSRF, mas não JWT.
- Tokens são armazenados apenas como hash, expiram em 30 minutos por padrão, são de uso único e há limites locais por e-mail/IP. O link carrega o token no fragmento (`#token=...`), que o navegador não envia na requisição HTTP. Falha de SMTP é registrada sem incluir endereço ou token no log; o token daquela tentativa é invalidado e o cliente recebe a mesma resposta neutra. Consulte a disponibilidade do Mailpit em desenvolvimento.
- Mailpit captura o SMTP em `127.0.0.1:1025`; a interface web fica em `http://localhost:8025`, publicada somente no loopback. Nenhum e-mail é encaminhado a destinatários reais.
- Cadastro exige nome ou razão social, e-mail válido e senha de 12 a 128 caracteres. A criação da conta e do perfil é atômica; e-mail repetido retorna `409 email_ja_cadastrado`. Login inválido retorna sempre `401 credenciais_invalidas`, independentemente de o e-mail existir.
- Login/renovação retornam `accessToken`, `tipoToken`, `expiraEmSegundos` e resumo da conta. O refresh token é entregue apenas pelo cookie `ivis_refresh`; não o armazenar no frontend. A renovação substitui e revoga o token anterior.
- Antes de operações mutáveis, obter `GET /api/v1/sessoes/csrf`, manter o cookie `XSRF-TOKEN` e enviar seu valor em `X-XSRF-TOKEN`. Enviar também `Authorization: Bearer <accessToken>` nas rotas autenticadas; logout exige o access token corrente.
- JSON em UTF-8; identificadores em português brasileiro, sem acentos.
- **Identificador de requisição:** cabeçalho `X-Id-Requisicao`. O cliente pode enviar um valor de 8 a 64 caracteres `[A-Za-z0-9-]`; se faltar ou for inválido, o servidor gera um UUID. O valor volta no cabeçalho da resposta, no corpo de erro e em todas as linhas de log da requisição.
- **Erro uniforme:**

```json
{
  "codigo": "dados_invalidos",
  "mensagem": "Há dados inválidos na requisição.",
  "campos": [{ "campo": "email", "mensagem": "…" }],
  "idRequisicao": "6f1c…"
}
```

  `campos` é sempre uma lista (vazia quando não se aplica). As mensagens de erro do framework são fixas e não repetem o texto da exceção. Erros de regra de negócio usam `ExcecaoNegocio`.
- **Logs:** só método, caminho, status e duração por requisição. Nada de corpo, cabeçalhos, consulta, senhas ou tokens.
- **CORS:** só as origens de `IVIS_CORS_ORIGENS`, com credenciais (cookie de renovação do ADR-0011).
- **CSRF:** buscar `GET /api/v1/sessoes/csrf` antes de chamadas mutáveis e enviar o cookie `XSRF-TOKEN` junto do cabeçalho `X-XSRF-TOKEN`. `ivis_refresh` é HttpOnly, SameSite Strict e Secure por padrão.

## Preparação de segurança

O backend inclui Spring Security e OAuth2 Resource Server com validação HS256 do JWT. A cadeia HTTP é stateless, não usa autenticação de formulário; health/readiness, consulta de CSRF, cadastro, login e renovação são públicos, e as demais rotas exigem JWT válido. As rotas de IA existentes também exigem autenticação. O JWT leva o UUID da conta em `sub` e o tipo de conta, sem incluir plano ou permissões de negócio.

CORS é aplicado pela cadeia de segurança, restringido a `IVIS_CORS_ORIGENS`, aceita `PATCH` e permite credenciais para a renovação por cookie. CSRF permanece habilitado nas operações mutáveis; os endpoints bearer da IA são excluídos por não usar autenticação por cookie. Cadastros criam as contas e perfis com transação; e-mail é normalizado em minúsculas e único. Refresh tokens aleatórios de uso único são armazenados apenas como hash SHA-256 e rotacionados a cada renovação. As sessões expiram em sete dias, os JWTs em quinze minutos, e logout revoga a sessão do refresh correspondente à conta autenticada.

Perfil próprio e alteração de senha usam o UUID do `sub` autenticado, nunca um identificador recebido do cliente. A troca de senha exige a senha atual. Tokens de recuperação têm 256 bits aleatórios, são persistidos somente como SHA-256, expiram, são consumidos uma única vez e cada pedido novo invalida tokens ativos anteriores. Troca e recuperação revogam todas as sessões de refresh da conta; access tokens emitidos anteriormente permanecem válidos somente até sua expiração natural (15 minutos). A resposta à solicitação é neutra para não revelar se o e-mail existe. O Mailpit é usado somente como captura SMTP local; sua interface e SMTP são publicados no loopback, e a implantação não deve substituir o host por um provedor real sem decisão específica.

Tentativas inválidas são limitadas por e-mail normalizado e endereço IP, com janela e quantidade configuráveis. Os contadores são limitados em tamanho e mantidos apenas na memória do processo; em implantação com mais de uma instância, trocar por armazenamento compartilhado antes de publicar para que o limite seja consistente. O IP considerado é o observado pelo servidor, sem confiar em `X-Forwarded-For`.

## Estrutura

```
src/main/java/br/ivis/
  AplicacaoIvis.java
  comum/
    erro/          RespostaErro, ErroCampo, ExcecaoNegocio, TratadorGlobalDeErros, TratadorFalhaInesperada
    requisicao/    FiltroIdentificadorRequisicao
    configuracao/  ConfiguracaoCors, ConfiguracaoSeguranca
    saude/         SaudeControlador
src/main/resources/
  application.yml
  db/migration/    V1__base_inicial.sql, V2__modelo_relacional_treinos.sql,
                   V3__modelo_relacional_contas_planos_pagamentos_auditoria.sql
```
