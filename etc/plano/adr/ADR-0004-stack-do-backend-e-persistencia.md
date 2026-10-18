# ADR-0004 — Stack do backend e persistência

- **Status:*Aprovado**
- **Data:*17/10/2026**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0010](ADR-0010-modelagem-relacional-do-banco.md)

## Contexto

O documento de tecnologias propõe Java com Spring Boot para o backend e PostgreSQL para os dados do produto. A equipe confirmou Java 25 LTS e Gradle. O documento também lista bibliotecas Spring e JWT como tecnologias necessárias/sugeridas. Nenhum backend ou schema de banco foi encontrado no código atual.

## Decisão

Registra-se a seguinte stack do backend:

- backend em **Java 25 LTS** com Spring Boot;
- **Gradle** como ferramenta de build;
- PostgreSQL como banco relacional do domínio IVIS, administrado pelo backend;
- Spring Data JPA para persistência, Spring Security para autenticação/autorização e **Flyway** para migração de schema;
- mecanismo de sessão/token e política de renovação/revogação ainda a especificar. A menção a JWT no documento de tecnologias não basta para decidir formato, duração, armazenamento ou ciclo de vida de credenciais.

O banco é autoridade dos dados de negócio do IVIS. Esta decisão não determina se módulos internos do backend compartilham uma instância/schema, nem autoriza os módulos frontend ou IA a acessarem o banco. A estratégia de autenticação continua pendente.

## Consequências

### Positivas

- Stack alinhada à proposta já documentada e compatível com validação, segurança e persistência estruturadas.
- Banco relacional atende relações entre contas, planos, vínculos, treinos e pagamentos.
- Migrações versionadas com Flyway tornam mudanças de schema rastreáveis.

### Custos e riscos

- A equipe precisa definir a estratégia de autenticação.
- Persistência de dados pessoais exige controles de acesso, retenção e proteção compatíveis com LGPD.
- PostgreSQL e backend ainda precisam ser adicionados e operados; esta decisão não descreve backup, disponibilidade ou implantação.

## Alternativas consideradas

- **Persistência em memória/localStorage no frontend:** adequada apenas à demonstração existente; não atende isolamento entre contas e persistência durável do sistema.
- **Banco NoSQL:** rejeitado; o IVIS utilizará PostgreSQL como banco de dados relacional.
