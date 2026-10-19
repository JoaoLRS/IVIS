# IVIS — Backend

Etapa 1 da [Ordem de Entrega do MVP](../etc/plano/Ordem_de_Entrega_MVP.md): esqueleto executável. Java 25 LTS, Spring Boot, Gradle, PostgreSQL e Flyway. Ainda não há regra de negócio nem tabelas de domínio; isso começa na Etapa 2.

## Rodar com Docker (backend + banco + frontend)

Na raiz do repositório:

```bash
docker compose up --build
```

- Backend: <http://localhost:8081/api/v1/saude> e <http://localhost:8081/api/v1/pronto> (esta confere o banco).
- PostgreSQL: `127.0.0.1:5432`, banco `ivis`. O banco vazio é criado só pelas migrações Flyway.
- Para trocar usuário/senha/origens, copie `.env.example` para `.env`.

## Rodar em desenvolvimento

Pré-requisitos: JDK 25 e Docker.

```bash
docker compose up ivis-banco -d
cd backend
export IVIS_BD_SENHA=ivis_local
gradle bootRun
```

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
| `SPRING_PROFILES_ACTIVE` | *(vazio)* | `estruturado` liga logs em JSON (ECS). |

## Convenções de API (Etapa 0, item 4)

- Base `/api` (servlet context path) + `/v1`: por exemplo `/api/v1/saude`. O `clienteHttp.ts` do frontend já aponta para `http://localhost:8081/api`.
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

## Estrutura

```
src/main/java/br/ivis/
  AplicacaoIvis.java
  comum/
    erro/          RespostaErro, ErroCampo, ExcecaoNegocio, TratadorGlobalDeErros, TratadorFalhaInesperada
    requisicao/    FiltroIdentificadorRequisicao
    configuracao/  ConfiguracaoCors
    saude/         SaudeControlador
src/main/resources/
  application.yml
  db/migration/    V1__base_inicial.sql
```
