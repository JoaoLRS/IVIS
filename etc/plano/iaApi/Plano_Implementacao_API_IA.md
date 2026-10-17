# Plano de implementação — API de IA

A API de IA será um módulo Python separado, integrado à NVIDIA NGC e consumido pelo backend. Deve produzir perguntas e avaliações para os treinamentos sem assumir autenticação, autorização, contratação ou persistência de domínio.

## Princípios obrigatórios

- O backend é o único consumidor interno da API de IA; o navegador nunca chama este módulo nem NVIDIA NGC diretamente.
- Receber apenas o contexto necessário à tarefa e devolver saída estruturada validável pelo backend.
- Não acessar o banco de dados do IVIS nem persistir dados pessoais ou respostas sem decisão explícita de retenção.
- Gerenciar credenciais NVIDIA por configuração segura no serviço, nunca no frontend ou repositório.
- Usar nomes próprios de classes, funções e métodos em português brasileiro, sem acentos nos identificadores; classes em `PascalCase` e funções/métodos em `snake_case` conforme convenção Python.

## Etapas

### 1. Definir contrato e limites de dados

- Especificar operações para gerar perguntas e avaliar respostas, com versão explícita do contrato.
- Definir campos mínimos de entrada, tamanhos máximos, formato de saída, escala de avaliação e mensagens de erro.
- Alinhar identificadores e campos do contrato em português brasileiro com backend e frontend.
- Definir quais dados de perfil/vaga podem ser enviados, como minimizar ou remover identificadores pessoais e quais limites de conteúdo serão aplicados.

### 2. Preparar o serviço Python

- Criar aplicação Python com framework de API a confirmar pela equipe (FastAPI é a opção sugerida atualmente).
- Separar rotas, validação de esquemas, serviço de aplicação, integração com NVIDIA NGC e configuração.
- Exigir autenticação de serviço nas chamadas do backend e restringir exposição de rede conforme ambiente.
- Disponibilizar verificações de saúde/prontidão sem expor segredos ou detalhes internos.

### 3. Integrar NVIDIA NGC

- Confirmar modelo, endpoint, SDK/cliente HTTP, licenças, limites de uso e configuração disponível no NGC.
- Implementar timeout, limite de tamanho, controle de concorrência e tratamento de indisponibilidade.
- Validar estruturalmente a saída do modelo; rejeitar ou recuperar respostas inválidas de forma limitada e observável.
- Versionar configurações de modelo e instruções para que os resultados usados em cada treinamento possam ser identificados.
- Não registrar prompts/respostas completos por padrão; definir política de auditoria/diagnóstico compatível com RNF09.

### 4. Entregar operações de IA

1. Receber contexto autorizado do backend e gerar perguntas para o tipo de treinamento solicitado.
2. Receber pergunta e resposta, avaliar segundo uma escala definida e devolver pontos fortes, oportunidades de melhoria e nota estruturados.
3. Não decidir se o usuário pode treinar, acessar um plano, continuar uma sessão ou receber determinado benefício; o backend valida isso antes e depois da chamada.
4. Garantir que uma saída de IA seja tratada como conteúdo não confiável e validada pelo backend antes de persistir ou exibir.

### 5. Preparar operação e observabilidade

- Empacotar o serviço em container e documentar configuração por ambiente.
- Registrar métricas e logs técnicos mínimos (latência, falhas, versão de modelo), sem dados pessoais ou conteúdo completo por padrão.
- Definir limites de requisição e alertas para falhas/custos do provedor.
- Documentar comportamento em timeout, erro do NGC e indisponibilidade para permitir resposta apropriada do backend.

## Critérios de conclusão

- Backend autentica-se e consome contrato versionado da API.
- Saídas de geração e avaliação são estruturadas e validadas.
- Segredos não aparecem no código, imagem, logs ou navegador.
- O módulo não acessa o banco e não decide regras de negócio do IVIS.
- Limites de dados, falhas, observabilidade e versão do modelo estão documentados.

## Pendências de decisão

- Framework Python e ferramenta de dependências.
- Modelo/endpoint específico NVIDIA NGC e limites de uso.
- Contrato de API, autenticação serviço a serviço e política de timeout/retentativa.
- Política de privacidade, retenção e diagnóstico de prompts/respostas.
