# ADR-0001 — Arquitetura modular para o IVIS

- **Status:** Em andamento
- **Data:**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0003](ADR-0003-comunicacao-entre-modulos.md)

## Contexto

O IVIS reúne interface web, regras de negócio e processamento de IA. Esses componentes têm responsabilidades e necessidades de evolução diferentes. A documentação de tecnologias já descreve três módulos independentes que se comunicam por APIs; falta registrar essa orientação como decisão arquitetural central.

## Decisão

O IVIS será organizado como uma **arquitetura modular**, com módulos de responsabilidade explícita e dependências controladas por interfaces documentadas. No nível do produto, os módulos são:

1. **Frontend web:** interação com os usuários.
2. **Backend:** regras de negócio, autenticação, autorização e persistência do domínio.
3. **API de IA:** geração de perguntas e avaliação das respostas.

O frontend não acessa diretamente o banco de dados nem o provedor de IA. Regras do domínio e decisões de acesso pertencem ao backend. O módulo de IA não é a fonte de verdade de contas, planos, vínculos ou histórico.

Esta decisão define os limites lógicos e a direção das dependências. Ela, por si só, não determina quantidade de processos/instâncias em produção, infraestrutura de implantação, estratégia de escalabilidade, mensageria ou divisão interna do backend em submódulos.

## Consequências

### Positivas

- Responsabilidades podem evoluir sem concentrar interface, regras de negócio e prompts/modelos no mesmo componente.
- Contratos entre módulos tornam integrações explícitas e permitem substituir implementações atrás das interfaces.
- A separação da IA ajuda a isolar dependências, segredos e falhas do provedor de modelos.

### Custos e riscos

- Chamadas entre módulos acrescentam latência, falhas de rede e trabalho de observabilidade.
- Contratos precisam de versionamento, compatibilidade e documentação.
- A separação exige configuração e execução local coordenadas; não cria automaticamente alta disponibilidade ou segurança.

## Alternativas consideradas

- **Aplicação única sem fronteiras entre componentes:** mais simples no início, mas mistura ciclos de mudança e responsabilidades.
- **Microserviços por cada domínio de negócio:** não adotado como requisito. A decisão estabelece três módulos de produto; decompor o backend em serviços menores depende de evidência e de uma ADR futura.
