# Glossário do IVIS

Este glossário reúne os termos funcionais, técnicos e arquiteturais usados nos requisitos, casos de uso, ADRs e planos de implementação. Quando um termo estiver ligado a uma decisão ainda em revisão, a definição descreve a proposta atual e pode ser ajustada pela equipe.

## Termos do domínio

| Termo | Definição no IVIS |
|---|---|
| **Aluno** | Pessoa que utiliza o IVIS para cadastrar habilidades e vagas de interesse, realizar treinamentos e consultar o próprio histórico. |
| **Aluno Pagante** | Modalidade de acesso do aluno que possui um plano individual contratado diretamente. A modalidade e as permissões são determinadas pelo backend. |
| **Aluno de Consórcio** | Modalidade de acesso do aluno que possui vínculo ativo a um contrato institucional. A instituição que administra o vínculo deriva do contrato; o aluno não define essa condição no cliente. Nos requisitos atuais também aparece o nome `AlunoConsórcio`. |
| **Instituição de Ensino** | Conta institucional que contrata plano institucional e gerencia os alunos vinculados às vagas contratadas. |
| **Plano individual** | Oferta contratada diretamente por um aluno para obter os benefícios e o acesso correspondentes. |
| **Plano institucional / plano de consórcio** | Oferta contratada por uma instituição, com capacidade de vagas para vincular alunos. “Consórcio” é o nome de negócio usado nos requisitos para essa modalidade. |
| **Contrato** | Registro da contratação de um plano por uma conta. Mantém estado, vigência e o preço acordado, preservando o histórico mesmo que o catálogo de planos mude. |
| **Vaga de interesse** | Vaga de emprego cadastrada pelo aluno como contexto para uma simulação de entrevista. Não representa uma vaga de assento do plano institucional. |
| **Vaga institucional** | Capacidade de um contrato institucional para conceder acesso a um aluno. O total contratado limita quantos vínculos podem estar ativos simultaneamente. |
| **Vínculo institucional** | Relação registrada entre um aluno e um contrato institucional ativo. A instituição titular é obtida por meio do contrato. |
| **Habilidade técnica** | Competência ou tecnologia registrada no perfil do aluno e usada como contexto possível para o treinamento. |
| **Treinamento / simulação de entrevista** | Sessão em que a IA apresenta perguntas ao aluno, recebe as respostas e gera uma avaliação. Cada sessão pertence a um aluno e pode estar associada a uma vaga de interesse. |
| **Pergunta de entrevista** | Pergunta gerada ou selecionada para uma sessão de treinamento, registrada em sequência para compor o histórico. |
| **Resposta** | Conteúdo enviado pelo aluno em resposta a uma pergunta do treinamento. Deve ser enviado à IA somente quando necessário para processar a sessão. |
| **Feedback / avaliação** | Resultado do treinamento, que pode incluir nota, pontos fortes e oportunidades de melhoria. É gerado com apoio da IA e validado pelo backend antes de ser persistido ou apresentado. |
| **Histórico de treinamentos** | Registros persistidos das sessões, perguntas, respostas, contexto e avaliações, consultáveis pelo aluno titular. |
| **Pagamento simulado** | Resultado fictício produzido pelo simulador controlado pelo backend no ambiente de apresentação. Não gera cobrança nem encaminha os dados do cartão a um gateway real; PAN e CVV não são persistidos. |
| **Cartão de teste** | Dados fictícios aceitos pelo simulador acadêmico para demonstrar a contratação. Não representam um meio de pagamento real. |
| **Auditoria** | Registro protegido e rastreável de ações sensíveis, incluindo ator, ação, resultado e momento. A retenção mínima ainda precisa ser definida pela equipe. |
| **Autoridade do backend** | Princípio segundo o qual o servidor valida identidade, permissões, preço, modalidade, capacidade, estados e resultados de negócio. O frontend não pode alterar essas decisões. |

## Requisitos e documentação

| Sigla ou termo | Definição |
|---|---|
| **RF** | Requisito Funcional: comportamento ou capacidade que o sistema deve oferecer. O IVIS tem 20 RF registrados em `IVIS_Requisitos.md`. |
| **RNF** | Requisito Não Funcional: atributo ou restrição de qualidade do sistema. Os quatro RNF aprovados atualmente são RNF01, RNF03, RNF07 e RNF09. |
| **RF01–RF20** | Identificadores dos requisitos funcionais aprovados; o texto de cada requisito está em [`IVIS_Requisitos.md`](IVIS_Requisitos.md). |
| **RNF01 — Aplicação Web** | O sistema é acessado por navegador, sem instalação obrigatória pelo usuário. |
| **RNF03 — Tempo de resposta da IA** | A resposta durante a entrevista deve manter a fluidez da conversa; o limite/SLA ainda está pendente de definição. |
| **RNF07 — Isolamento entre instituições** | Uma instituição só pode visualizar e gerenciar os alunos vinculados aos seus próprios contratos. |
| **RNF09 — Proteção e retenção dos logs** | Os registros de auditoria devem ser protegidos contra alteração e mantidos pelo período mínimo a definir com a equipe jurídica. |
| **Caso de uso (CDU)** | Descrição de como um ator interage com o sistema para alcançar um objetivo. Os documentos estão em [`../cdu/`](../cdu/). |
| **ADR** | *Architecture Decision Record*: documento que registra contexto, decisão arquitetural, consequências e alternativas. Os ADRs atuais estão em [`adr/`](adr/). |
| **Em andamento** | Estado documental usado para indicar que uma decisão ou artefato está sendo revisado pela equipe; não significa necessariamente que a implementação já começou. |

## Arquitetura e componentes

| Termo | Definição no IVIS |
|---|---|
| **Frontend** | Aplicação web Vue e TypeScript que apresenta a interface e consome a API do backend. Não acessa diretamente o banco, a API de IA ou serviços de pagamento. |
| **Backend** | Serviço Java 25 LTS com Spring Boot que aplica regras de negócio, autentica e autoriza usuários, persiste dados e coordena integrações. Usa Gradle como ferramenta de build. |
| **API de IA** | Serviço Python separado que processa solicitações de geração de perguntas e avaliação. É chamado pelo backend, não pelo navegador. |
| **NVIDIA NGC** | Plataforma/provedor NVIDIA escolhido para disponibilizar a capacidade de IA. O modelo e o endpoint específicos ainda precisam ser definidos. |
| **API** | Interface de programação que define como módulos ou serviços trocam solicitações e respostas. Os contratos do IVIS devem ser versionados e validados. |
| **Módulo** | Parte do sistema com responsabilidade delimitada e interface explícita. O IVIS separa frontend, backend e API de IA; o backend organiza internamente capacidades de domínio. |
| **Regra de negócio** | Condição que define o comportamento do domínio, como quem pode contratar, quais benefícios estão ativos, quantas vagas existem ou quando um plano permite acesso. É validada pelo backend. |
| **Integração serviço a serviço** | Comunicação entre componentes no servidor, como a chamada autenticada do backend à API de IA. Segredos não são expostos ao navegador. |

## Banco de dados e operação

| Termo | Definição no IVIS |
|---|---|
| **PostgreSQL** | Banco de dados relacional escolhido para persistir os dados de domínio do IVIS. NoSQL não será utilizado para esse propósito. |
| **Flyway** | Ferramenta de migração que aplica alterações versionadas ao esquema do banco. Migrações já aplicadas não devem ser editadas em ambientes compartilhados. |
| **Migração** | Arquivo versionado que cria ou altera tabelas, colunas, restrições ou índices de forma reproduzível. |
| **Chave primária (PK)** | Identificador único de uma linha de tabela. O modelo propõe UUIDs como chaves primárias. |
| **Chave estrangeira (FK)** | Coluna ou conjunto de colunas que referencia uma linha relacionada em outra tabela, mantendo integridade referencial. |
| **UUID** | Identificador universalmente único usado no modelo proposto como chave primária. |
| **`snake_case`** | Convenção de identificadores com palavras minúsculas separadas por sublinhado, como `conta_aluno_id`. No banco IVIS, nomes em português brasileiro e sem acentos. |
| **Centavos / unidade monetária mínima** | Forma inteira de representar dinheiro no banco. Por exemplo, R$ 19,90 é armazenado como `1990` centavos; a formatação monetária é aplicada para exibição. |
| **Snapshot** | Cópia imutável dos dados relevantes em um momento específico, usada para preservar o preço contratado ou o contexto de uma entrevista sem depender de alterações posteriores na origem. |
| **JSONB** | Tipo do PostgreSQL para dados JSON consultáveis. No modelo IVIS é restrito a snapshots ou metadados controlados, não substitui tabelas relacionais centrais. |
| **Soft delete / arquivamento** | Marcar um registro como inativo/arquivado em vez de apagá-lo fisicamente, para preservar relações e histórico quando necessário. |
| **Índice** | Estrutura do banco que pode acelerar consultas; deve corresponder aos padrões de acesso e não substituir validações de autorização. |
| **Idempotência** | Propriedade de uma operação em que repetir a mesma solicitação não duplica seus efeitos, necessária, por exemplo, para tentativas repetidas de pagamento. |
| **Mailpit** | Servidor SMTP de captura usado para simular e inspecionar e-mails de desenvolvimento/apresentação sem encaminhá-los a destinatários reais. Não é provedor de e-mail de produção. |
| **Gateway de pagamento** | Serviço externo que processa cobranças reais. Não é utilizado pelo simulador acadêmico; uma integração real depende de decisão e configuração futuras. |
| **SLA** | Acordo/limite mensurável de nível de serviço. O tempo máximo de resposta da IA previsto no RNF03 ainda está pendente de definição. |

## Convenções de nomes no código

Nomes próprios de tabelas, colunas, classes, funções e métodos devem estar em português brasileiro, sem acentos nos identificadores. No banco, usar `snake_case`; em classes, `PascalCase`; em métodos e funções, seguir a convenção da linguagem, mantendo as palavras em português. Símbolos e nomes impostos por bibliotecas externas podem permanecer como definidos por elas.

---

**Documentos relacionados:** [requisitos](IVIS_Requisitos.md), [tecnologias](IVIS_Tecnologias.md), [ADR](adr/README.md), [modelo de banco](BD/Modelo_Relacional_IVIS.md) e [planos de implementação](BD/Plano_Implementacao_BD.md).
