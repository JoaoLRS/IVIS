# Plano de implementação — Backend

O backend implementará as regras de negócio, autenticação e autorização do IVIS e será o único módulo com acesso ao banco de domínio. Sua stack definida é Java 25 LTS, Spring Boot, Gradle, PostgreSQL e Flyway. O trabalho deve seguir a [arquitetura modular](../adr/ADR-0001-arquitetura-modular.md), os limites entre módulos e os RF/RNF aprovados.

## Princípios obrigatórios

- Centralizar no backend as decisões sobre acesso, planos, contratação, pagamento, vagas, treinamento e auditoria. O frontend envia intenções e apresenta respostas; não escolhe resultados nem altera regras.
- Integrar com PostgreSQL exclusivamente pelo backend e evoluir o esquema por Flyway.
- Chamar a API de IA Python/NVIDIA NGC do lado servidor. Não expor credenciais ou chamar o provedor pelo navegador.
- Usar nomes próprios em português brasileiro em pacotes, classes, funções e métodos, sem acentos nos identificadores. Classes em `PascalCase`; métodos e funções em `camelCase`.
- Nunca registrar senhas, tokens de recuperação, dados completos de cartão ou segredos em logs.

> **Primeira fase do MVP:** a sequência de entrega de RF01 (Aluno), RF03 e RF06–RF10, com endpoints e critérios por etapa, está em [`../Ordem_de_Entrega_MVP.md`](../Ordem_de_Entrega_MVP.md). A autenticação proposta está no [ADR-0011](../adr/ADR-0011-autenticacao-do-mvp.md).

## Etapas

### 1. Preparar o esqueleto executável

- Criar o projeto Gradle para Java 25 LTS com configuração reproduzível para desenvolvimento e execução.
- Organizar módulos por capacidades e responsabilidades de domínio, mantendo dependências explícitas e evitando acesso cruzado direto a entidades internas.
- Definir configuração por ambiente, gestão de segredos e perfil local para PostgreSQL e Mailpit.
- Estabelecer tratamento uniforme de erros, validação de entrada, identificador de requisição e logs estruturados sem dados sensíveis.

### 2. Definir interfaces e segurança

- Descrever endpoints e esquemas de requisição/resposta em português brasileiro, alinhados aos casos de uso e requisitos.
- Escolher e documentar estratégia de sessão/token, expiração, revogação, armazenamento e proteção contra abuso antes de fechar autenticação.
- Implementar autorização por tipo de conta e titularidade em cada operação. IDs recebidos do navegador nunca substituem autorização no backend.
- Aplicar RNF07 ao isolamento de dados por instituição e RNF09 à proteção e retenção dos logs.

### 3. Integrar persistência

- Configurar Spring Data JPA e Flyway para PostgreSQL.
- Implementar as tabelas e restrições conforme o plano de [Banco de dados](../BD/Plano_Implementacao_BD.md), após validação do modelo.
- Implementar regras entre tabelas em transações, incluindo unicidade de contratação ativa se aplicável, limite de vagas e vínculo institucional.
- Guardar valores monetários em centavos como inteiros; formatar apenas na resposta adequada à interface.

### 4. Entregar capacidades na ordem dos casos de uso

1. Cadastro, login, perfis e recuperação de senha; enviar e-mail pelo SMTP do Mailpit em desenvolvimento/apresentação.
2. Gerenciamento de habilidades e vagas de interesse.
3. Catálogo de planos, consulta do plano vigente e contratação individual/institucional.
4. Pagamento simulado controlado pelo backend, com cartão de teste fictício, idempotência e estados claramente simulados; sem fallback para cobrança real.
5. Gestão de alunos vinculados, respeitando a capacidade contratada e o isolamento institucional.
6. Criação de treinamento, chamada autenticada à API de IA, persistência de perguntas/respostas/avaliação e consulta ao histórico.
7. Registro e consulta restrita de auditoria para eventos relevantes.

### 5. Preparar publicação demonstrativa

- Manter configuração simulada explícita e segura para pagamento e e-mail.
- Não expor console de administração do Mailpit sem controle de acesso no ambiente publicado.
- Garantir que dados de demonstração sejam fictícios, que falhas externas sejam tratadas de forma previsível e que o backend continue sendo autoridade das decisões.
- Documentar inicialização, variáveis de ambiente, migrações e contas/cenários de demonstração.

## Critérios de conclusão

- Aplicação inicia com Java 25/Gradle e configurações documentadas.
- Autenticação e autorização são aplicadas no servidor em todos os casos de uso implementados.
- Backend é o único acesso de domínio ao banco e à API de IA.
- Regras transacionais de contratação, capacidade de vagas, pagamento simulado e histórico são aplicadas no servidor.
- Erros, logs e dados sensíveis seguem os RNF aprovados.

## Pendências de decisão

- Estratégia definitiva de sessão/token e política de recuperação/limitação de tentativas.
- Contrato versionado da API do módulo de IA e comportamento de timeout/indisponibilidade.
- Regras finais de renovação, cancelamento, expiração de plano e transferência de aluno.
