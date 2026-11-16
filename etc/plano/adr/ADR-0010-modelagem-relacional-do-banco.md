# ADR-0010 — Modelagem relacional inicial do banco

- **Status:*Aprovado**
- **Data:*17/10/2026**
- **Decisores:** Pendente de validação da equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md), [ADR-0009](ADR-0009-backend-como-autoridade-de-negocio.md)

## Contexto

O IVIS já decidiu usar PostgreSQL e Flyway. O sistema precisa persistir contas, planos e contratos, vínculos de alunos, vagas e habilidades, entrevistas, resultados de pagamento e trilha de auditoria. É necessário propor como esses dados se relacionam, preservar histórico e isolar dados de cada instituição.

## Decisão proposta

Adotar inicialmente um **modelo relacional normalizado em PostgreSQL**, administrado exclusivamente pelo backend Java. As tabelas, chaves, restrições e índices serão criados e alterados por migrações versionadas do Flyway. Frontend e API de IA não acessam diretamente o banco.

Convenções propostas:

- nomes de tabelas e colunas em português brasileiro, usando `snake_case` sem acentos, chaves primárias UUID e datas/hora com fuso (`TIMESTAMPTZ`, gravadas em UTC);
- nomes próprios no código do backend também em português brasileiro: classes em `PascalCase` e funções/métodos em `camelCase`, sem acentos nos identificadores. Siglas técnicas consolidadas podem ser mantidas quando necessárias;
- dinheiro como inteiro em centavos (ou unidade monetária mínima) mais código ISO da moeda, nunca `FLOAT`;
- valores de plano contratados guardados como snapshot no contrato, para preservar o preço histórico quando o catálogo mudar;
- dados de domínio em tabelas relacionais. `JSONB` só pode ser usado para snapshots imutáveis ou metadados variáveis, nunca para substituir entidades e relacionamentos centrais;
- restrições e índices no banco para integridade estrutural, complementados por validações transacionais no backend;
- histórico e auditoria não são apagados em cascata. Contas e contratos devem ser desativados/encerrados segundo política a definir;
- o registro de pagamento não contém número de cartão, CVV ou credenciais. A transação de apresentação guarda apenas estado e indicação de que foi simulada.

O modelo inicial sugerido é o diagrama e catálogo de tabelas em [`../BD/Modelo_Relacional_IVIS.md`](../BD/Modelo_Relacional_IVIS.md). Ele cobre contas/perfis, catálogo e contratos, associações institucionais, treinamento/feedback, pagamentos e auditoria.

## Consequências

### Positivas

- Integridade referencial e consultas claras para as relações entre alunos, instituições, planos, vagas e treinos.
- Migrações rastreáveis e reproduzíveis em diferentes ambientes.
- A propriedade dos dados permanece no backend, alinhada à arquitetura modular e à regra de autoridade do servidor.
- Snapshots e política de não exclusão em cascata preservam resultados e rastreabilidade.

### Custos e riscos

- O backend deve controlar operações concorrentes de contratação e consumo de vagas em transações para não ultrapassar a capacidade do plano.
- Restrições que envolvem várias tabelas (por exemplo, compatibilidade de modalidade e limite de vagas) precisam de validação no backend, além das constraints possíveis no banco.
- Prazos de retenção, exclusão/anonimização de dados e regras jurídicas ainda precisam ser definidos.
- O modelo é uma proposta derivada dos RF; precisa de revisão de domínio antes da primeira migração de produção.

## Alternativas consideradas

- **Banco NoSQL:** rejeitado; o IVIS utilizará PostgreSQL relacional, conforme ADR-0004.
- **Um banco acessado por frontend, backend e API de IA:** rejeitado; somente o backend será dono do schema e da persistência do domínio.
- **Guardar toda a entrevista, conta e contrato em documentos JSON:** rejeitado; dificultaria integridade, isolamento e consultas relacionais.

## Decisões ainda em aberto

- Confirmar entidades/atributos e cardinalidades do modelo proposto.
- Definir políticas de retenção, anonimização e exclusão de contas e históricos.
- Definir se atributos de contexto da entrevista devem ser normalizados ou armazenados como snapshot `JSONB` versionado.
- Definir estratégia operacional de backup e restauração do PostgreSQL.
