# ADR-0003 — Comunicação entre módulos

- **Status:** Em andamento
- **Data:**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md)

## Contexto

A documentação existente e as decisões da equipe definem três módulos comunicados por APIs. Em particular, o backend Java deve consumir pelo lado servidor a API de IA em Python, que utiliza NVIDIA NGC. Ainda não existe backend implementado nem contrato de API versionado.

## Decisão

Fica decidido que:

- o frontend consuma uma API publicada pelo backend;
- o backend chame a API de IA em Python/NVIDIA NGC por uma interface do lado servidor;
- o backend coordene a integração com gateway externo caso venha a ser adotado; o simulador de pagamento acadêmico permanece sob controle do backend;
- contratos de fronteira sejam explícitos, versionados e validados; nenhuma integração dependa de acessar diretamente o banco de outro módulo;
- falhas de timeout, indisponibilidade, repetição e respostas duplicadas sejam tratadas pelo módulo que inicia a chamada;
- segredos de IA e pagamento permaneçam fora do frontend e do controle de versão.

REST/HTTP com JSON é a opção inicial sugerida pelos documentos do stack, mas o protocolo, convenções de versionamento, autenticação entre serviços e especificação formal (por exemplo, OpenAPI) permanecem pendentes de decisão detalhada.

## Consequências

### Positivas

- Dependências entre módulos são observáveis e substituíveis por trás de contratos.
- O backend pode aplicar autorização e validação antes de chamar integrações externas.
- Contratos versionados reduzem acoplamento entre ciclos de desenvolvimento.

### Custos e riscos

- Comunicação remota pode falhar mesmo quando cada módulo está saudável isoladamente.
- Mudanças de contrato precisam de compatibilidade durante a evolução dos módulos.
- A equipe precisa decidir autenticação serviço a serviço, timeouts e política de retentativa antes da produção.

## Alternativas consideradas

- **Acesso compartilhado ao mesmo banco por todos os módulos:** não recomendado; mistura propriedade dos dados e acopla esquemas.
- **Mensageria como integração padrão desde o MVP:** não definida; adicionaria infraestrutura e consistência eventual sem requisito confirmado.
