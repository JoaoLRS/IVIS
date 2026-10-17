# ADR-0002 — Limites e responsabilidades dos módulos

- **Status:** Em andamento
- **Data:**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0003](ADR-0003-comunicacao-entre-modulos.md), [ADR-0004](ADR-0004-stack-do-backend-e-persistencia.md), [ADR-0005](ADR-0005-modulo-de-inteligencia-artificial.md), [ADR-0006](ADR-0006-frontend-web.md)

## Contexto

“Arquitetura modular” só orienta a implementação se os módulos tiverem responsabilidades e limites reconhecíveis. O documento de tecnologias do IVIS já distribui as funções entre interface, backend e IA.

## Decisão

Adotar estes limites de responsabilidade:

| Módulo | Responsabilidades | Não é responsável por |
|---|---|---|
| **Frontend web** | Apresentar telas, coletar entradas, orientar navegação e consumir a API do backend. | Aplicar sozinho regras de autorização, ser fonte oficial dos dados ou acessar banco/IA diretamente. |
| **Backend** | Ser a API de negócio; autenticar e autorizar usuários; aplicar regras de conta, habilidades, vagas, treinos, planos, pagamentos e vínculos institucionais; persistir dados e coordenar integrações. | Implementar internamente inferência/modelagem de IA ou armazenar dados sensíveis de cartão. |
| **API de IA** | Receber contexto necessário ao treino, conduzir a geração de perguntas e produzir avaliação/feedback por meio do provedor configurado. | Manter contas, permissões, planos, pagamentos, vínculos institucionais ou o histórico oficial do aluno. |

O gateway de pagamento e o provedor de modelos são sistemas externos. O backend coordena essas integrações; credenciais e dados de pagamento seguem as decisões de segurança, incluindo o simulador fictício do ADR-0008. No modo de apresentação, o backend também controla o simulador de pagamento e o fluxo de e-mail capturado pelo Mailpit. Os RNF aprovados pertinentes a estas fronteiras são RNF07 (isolamento entre instituições) e RNF09 (proteção e retenção dos logs).

O backend é a única autoridade para regras de negócio e decisões de segurança. O frontend pode validar entradas para melhorar a experiência, mas qualquer regra, permissão, preço, elegibilidade, estado de plano ou resultado de operação precisa ser validado e decidido novamente no backend. Valores ou sinalizações enviados pelo navegador nunca são prova de autorização ou pagamento.

Os limites internos do backend (por exemplo, módulos de identidade, treinamento, contratação e instituições) ainda devem ser detalhados junto ao desenho do domínio. Eles não devem criar dependências circulares nem permitir que um módulo altere diretamente os dados privados de outro sem uma interface definida.

## Consequências

### Positivas

- Há um dono explícito para regras de negócio e dados persistidos.
- A interface e o serviço de IA podem ser substituídos sem transferir responsabilidades do domínio.
- As fronteiras orientam organização de código, contratos, permissões e revisão de mudanças.

### Custos e riscos

- O backend concentra coordenação de fluxos, exigindo separação interna e testes de integração.
- O contexto enviado à IA deve ser minimizado e tratado conforme LGPD.
- Operações que atravessam pagamento, backend e IA exigem tratamento de timeout, repetição e estados parciais.

## Alternativas consideradas

- **Frontend acessando diretamente o banco/IA:** rejeitada porque contornaria autorização e exporia credenciais e contratos internos.
- **IA como dona do histórico ou do acesso:** rejeitada porque a IA é uma capacidade de processamento, não a autoridade do domínio IVIS.
- **Frontend como autoridade de regras/estados de negócio:** rejeitada porque o navegador é controlado pelo cliente e seus dados podem ser alterados; o backend deve validar toda operação.
