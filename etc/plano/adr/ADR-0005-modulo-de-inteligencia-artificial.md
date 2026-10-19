# ADR-0005 — Módulo de inteligência artificial

- **Status:*Aprovado**
- **Data:*17/10/2026**
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0001](ADR-0001-arquitetura-modular.md), [ADR-0002](ADR-0002-limites-e-responsabilidades-dos-modulos.md), [ADR-0003](ADR-0003-comunicacao-entre-modulos.md)

## Contexto

O treinamento é uma capacidade especializada do produto. A equipe definiu Python para o módulo de IA e NVIDIA NGC como provedor/plataforma de IA. A integração ainda não está implementada; FastAPI e Uvicorn são sugeridos no documento de tecnologias, sem aprovação específica neste ADR.

## Decisão

Manter a capacidade de IA em um módulo próprio, separado do backend de domínio. A linguagem do módulo é **Python** e o provedor/plataforma escolhida é **NVIDIA NGC**. O módulo recebe apenas o contexto necessário para a sessão de entrevista e devolve perguntas e avaliação ao backend por uma interface de API. O **backend consome essa API** e coordena o envio do contexto e a recepção dos resultados; o navegador nunca chama NVIDIA NGC diretamente.

FastAPI e Uvicorn são a base sugerida para a API Python, não uma decisão fechada neste ADR. Modelo específico, formatos de prompt, limites de dados, autenticação entre backend e IA, timeouts e política de retentativa permanecem por definir antes de produção. Segredos do provedor devem ser injetados no serviço por configuração segura, nunca incluídos no frontend ou repositório.

## Consequências

### Positivas

- Dependências e ritmo de experimentação de IA ficam isolados do ciclo de regras de negócio.
- A implementação do provedor pode mudar sem transferir para a IA a propriedade do domínio IVIS.
- É possível versionar comportamento de prompts/modelos dentro do módulo.

### Custos e riscos

- A chamada de IA acrescenta latência, custo e dependência de disponibilidade externa.
- Contrato, autenticação, minimização de dados e observabilidade precisam estar definidos.
- Python adiciona uma stack operacional além do backend Java e frontend TypeScript.

## Alternativas consideradas

- **Implementar inferência e orquestração dentro do backend Java:** não escolhida, pois a documentação já definiu Python e separação do módulo.
- **Chamar o provedor de IA diretamente do navegador:** rejeitada por expor segredos e retirar do backend a coordenação/autorização do treino.
