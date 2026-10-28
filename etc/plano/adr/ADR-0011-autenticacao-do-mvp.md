# ADR-0011 — Autenticação e sessão do MVP

- **Status:** Aprovado
- **Data:** 2026-10-18
- **Aprovado em:** 2026-10-27
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md), [ADR-0009](ADR-0009-backend-como-autoridade-de-negocio.md), [ADR-0010](ADR-0010-modelagem-relacional-do-banco.md)

## Contexto

O [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md) adota Spring Security e cita JWT, mas deixa em aberto formato, duração, armazenamento e revogação de credenciais. A primeira fatia do MVP (RF01 para Aluno, RF03, RF06 a RF10; ver [`Ordem_de_Entrega_MVP.md`](../Ordem_de_Entrega_MVP.md)) precisa de identidade para associar habilidades, vagas e histórico a cada aluno.

## Decisão

- **Senha:** armazenada apenas como hash com algoritmo adaptativo fornecido pelo Spring Security (Argon2id ou BCrypt com custo adequado). Nunca registrada em log.
- **Token de acesso:** JWT assinado pelo backend, de **curta duração** (15 minutos), enviado no cabeçalho `Authorization: Bearer`. Contém apenas o identificador da conta e o tipo de conta; plano, modalidade e permissões de negócio **não** são confiados ao token e são consultados no servidor (ADR-0009).
- **Token de renovação:** valor opaco e aleatório, de uso único com rotação, entregue em cookie `HttpOnly`, `Secure` e `SameSite=Strict`. O backend guarda somente o **hash** do token em tabela própria de sessões, com validade de 7 dias e marcação de revogação. O token de acesso fica apenas em memória no frontend, nunca em `localStorage`.
- **Encerrar sessão:** `sair` revoga o token de renovação. O token de acesso expira sozinho.
- **Segredo de assinatura:** injetado por variável de ambiente; fora do repositório.
- **Proteção contra abuso:** limitação de tentativas de login por conta e por origem; resposta de falha idêntica para e-mail inexistente e senha incorreta.
- **Identidade nas operações:** o identificador do aluno é sempre extraído do token. Ids de conta recebidos no caminho, na consulta ou no corpo não substituem essa identidade.

Esta decisão não define recuperação de senha (RF04), confirmação de e-mail, revogação de todas as sessões após troca de senha nem login de Instituição de Ensino além do reuso do mesmo mecanismo.

## Consequências

### Positivas

- Backend sem estado de sessão no acesso normal, mas com revogação possível pelo token de renovação.
- Token de acesso exposto a pouco tempo de abuso; o de renovação não é legível por scripts da página.
- Mantém a autoridade de negócio no servidor, conforme o ADR-0009.

### Custos e riscos

- Exige persistência de sessões; a tabela `sessoes_autenticacao` já consta no schema da migration V3 e no [`Modelo_Relacional_IVIS.md`](../BD/Modelo_Relacional_IVIS.md).
- O cookie de renovação exige configurar CORS com credenciais e o domínio das requisições; frontend e backend precisam ser servidos de forma compatível (mesmo site ou proxy).
- O frontend precisa renovar o token e repetir a requisição quando receber resposta de token expirado.
- Os prazos de token e limites de tentativa podem ser revistos após validação operacional; o ADR fixa os prazos iniciais, mas não os valores numéricos dos limites de tentativa.

## Alternativas consideradas

- **Sessão no servidor com cookie:** mais simples de revogar, mas introduz estado de sessão no servidor e foge da direção já registrada no ADR-0004 (JWT).
- **Somente JWT de acesso longo, sem renovação:** mais simples, porém sem revogação e com janela de abuso maior.
- **Guardar o token em `localStorage`:** rejeitado por ficar legível a qualquer script injetado na página.
