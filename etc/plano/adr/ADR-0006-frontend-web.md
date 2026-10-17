# ADR-0006 — Frontend web

- **Status:** Em andamento
- **Data:**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0003](ADR-0003-comunicacao-entre-modulos.md)

## Contexto

O IVIS precisa de uma interface de navegador para alunos e instituições. O repositório já possui um frontend demonstrativo Vue/TypeScript, e a documentação registra essa tecnologia para o módulo web.

## Decisão

Manter o frontend como módulo web desenvolvido com **Vue 3 e TypeScript**. O projeto atual usa Vite, Vue Router, Pinia, Bootstrap e Axios; estes componentes descrevem a implementação existente. O frontend deve consumir a API do backend e não deve conter segredos de IA/pagamento nem ser a autoridade para regras de acesso.

O frontend não tem autoridade para decidir nem alterar regras de negócio, permissões, preços, elegibilidade ou resultados de pagamento. Validações no navegador servem apenas para orientar o usuário; o backend valida cada operação e retorna o estado oficial. O frontend atual ainda é uma demonstração local, sem autenticação, backend, persistência em servidor ou integração real de IA.

## Consequências

### Positivas

- A equipe reaproveita código e tecnologias que já existem no repositório.
- A interface permanece desacoplada da implementação interna do backend por meio de API.
- TypeScript ajuda a representar contratos e dados consumidos pela interface.

### Custos e riscos

- Contratos API e estados de carregamento/erro precisam ser integrados quando o backend existir.
- Dados mantidos apenas no navegador não atendem requisitos de persistência e isolamento por usuário.
- Dependências de UI devem permanecer no módulo frontend, sem vazar para regras de negócio.

## Alternativas consideradas

- **Servidor de páginas acoplado ao backend:** não selecionado, pois diverge do módulo web Vue já existente.
- **Outro framework frontend:** não selecionado; não há motivação atual para substituir a implementação Vue/TypeScript.
