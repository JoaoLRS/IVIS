# Plano de implementação — Frontend

O frontend é a interface web Vue 3 e TypeScript já presente no repositório. Deve consumir a API do backend e apresentar os resultados e permissões que o servidor determinar. Componentes atuais ou futuros não são autoridade de negócio.

## Princípios obrigatórios

- Não calcular nem decidir elegibilidade, acesso, capacidade, preço, contratação ou resultado de pagamento. Mostrar os dados recebidos do backend e tratar suas respostas.
- Não chamar PostgreSQL, Mailpit, NVIDIA NGC ou gateways de pagamento diretamente.
- Não guardar segredos nem dados sensíveis desnecessários no navegador.
- Usar nomes próprios em português brasileiro em classes, funções e métodos novos, sem acentos nos identificadores; manter nomes definidos por bibliotecas/frameworks quando forem externos.
- Exibir dinheiro com formatação de moeda a partir do valor oficial fornecido pelo backend; não usar ponto flutuante para lógica financeira.

## Etapas

### 1. Inventariar e organizar a aplicação existente

- Mapear páginas, componentes, rotas, estado compartilhado e integrações atuais antes de substituir ou duplicar comportamentos.
- Identificar protótipos que atualmente simulam autenticação, plano ou acesso localmente e preparar sua substituição por chamadas ao backend.
- Definir estrutura de módulos de interface por fluxo/capacidade, evitando componentes que concentrem lógica de domínio.

### 2. Integrar a API do backend

- Definir cliente HTTP centralizado com URL configurável por ambiente, tratamento consistente de erros e cancelamento/timeout quando apropriado.
- Consumir contratos de API versionados; mapear estados retornados pelo backend para apresentação, sem reimplementar suas regras.
- Implementar estados de carregamento, vazio, sucesso e falha em cada fluxo.
- Aplicar estratégia de autenticação definida pelo backend sem expor tokens em locais inseguros; tratar expiração e encerramento de sessão.

### 3. Implementar os fluxos de usuário

1. Cadastro, login, perfil e recuperação de acesso.
2. Visualização e edição de habilidades e vagas de interesse.
3. Consulta de planos disponíveis e plano vigente.
4. Contratação individual ou institucional e interface de pagamento de demonstração. Exibir claramente que o pagamento é simulado; enviar somente a intenção/dados permitidos ao backend.
5. Gestão institucional de alunos vinculados, mostrando disponibilidade e resultados fornecidos pelo servidor.
6. Preparação de treinamento, experiência de entrevista, feedback e consulta ao histórico.

### 4. Acessibilidade, responsividade e estados

- Garantir navegação por teclado, rótulos acessíveis, foco visível e mensagens de erro associadas aos campos.
- Adaptar páginas a telas pequenas e grandes sem ocultar estados ou ações essenciais.
- Renderizar conteúdo de entrevista como dado não confiável; evitar interpretação de HTML e expor somente campos previstos no contrato.
- Evitar confiança em valores de query string ou estado local para autorizar ações.

### 5. Preparar validação integrada

- Verificar cada fluxo com a API do backend e cenários de sucesso, acesso negado, estado expirado e falha de dependência.
- Confirmar que refresh, navegação direta e retorno de sessão não alteram a autoridade do backend.
- Documentar como executar o frontend e apontá-lo ao ambiente local de demonstração.

## Critérios de conclusão

- Todas as operações de domínio passam pela API do backend.
- A interface apenas solicita ações e exibe estado decidido pelo servidor.
- Fluxos aprovados têm estados de carregamento, sucesso, vazio e erro compreensíveis.
- Valores monetários são exibidos corretamente a partir dos centavos retornados, sem cálculos financeiros no cliente.
- A experiência é responsiva e utilizável por teclado.

## Dependências

- Endpoints e contratos de API do backend.
- Decisão de autenticação e ciclo de vida da sessão.
- Definição final dos estados de plano, contratação, pagamento e treinamento.
