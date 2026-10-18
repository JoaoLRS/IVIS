# Architecture Decision Records (ADR)

Este diretório registra decisões arquiteturais do IVIS. Cada decisão tem um identificador permanente (`ADR-NNNN`), contexto, decisão, consequências e alternativas consideradas. Decisões substituídas permanecem no histórico com status atualizado e referência à decisão sucessora.

## Decisões

| ID | Decisão | Status |
|---|---|---|
| [ADR-0001](ADR-0001-arquitetura-modular.md) | Organizar o IVIS em módulos com responsabilidades e interfaces explícitas | Em andamento |
| [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md) | Definir os limites dos módulos frontend, backend e API de IA | Em andamento |
| [ADR-0003](ADR-0003-comunicacao-entre-modulos.md) | Backend consome a API de IA Python/NVIDIA NGC; módulos integram por APIs | Em andamento |
| [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md) | Stack do backend e persistência relacional | Em andamento |
| [ADR-0005](ADR-0005-modulo-de-inteligencia-artificial.md) | Isolar o processamento de IA em módulo Python | Em andamento |
| [ADR-0006](ADR-0006-frontend-web.md) | Manter o frontend como módulo web Vue e TypeScript | Em andamento |
| [ADR-0007](ADR-0007-simulacao-de-email-com-mailpit.md) | Capturar e-mails de demonstração usando Mailpit | Em andamento |
| [ADR-0008](ADR-0008-pagamento-simulado-para-demonstracao.md) | Simular contratação com cartão de teste no backend | Em andamento |
| [ADR-0009](ADR-0009-backend-como-autoridade-de-negocio.md) | Manter regras de negócio exclusivamente sob autoridade do backend | Em andamento |
| [ADR-0010](ADR-0010-modelagem-relacional-do-banco.md) | Definir modelo relacional inicial do IVIS | Em andamento |
| [ADR-0011](ADR-0011-autenticacao-do-mvp.md) | Autenticação e sessão do MVP (JWT de acesso curto e token de renovação) | Proposto |

## Como adicionar uma decisão

1. Escolha o próximo número disponível e um nome curto em kebab-case.
2. Copie [`TEMPLATE.md`](TEMPLATE.md) e preencha contexto, decisão, consequências e alternativas.
3. Use **Em andamento** enquanto a equipe revisa a decisão; use **Aceito** após aprovação explícita; use **Substituído** quando outro ADR assumir seu lugar.
4. Atualize esta lista e faça referência cruzada quando uma decisão depender de outra.

Os ADRs registram intenção arquitetural. A implementação atual ainda contém apenas o frontend demonstrativo, conforme [`LEIA-ME.md`](../../../frontend/LEIA-ME.md).
