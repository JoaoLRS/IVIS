# Glossário do IVIS

Este glossário reúne os termos funcionais, técnicos e arquiteturais usados nos requisitos, [casos de uso](../cdu/IVIS_Casos_de_Uso.md), ADRs e código atual do IVIS. A situação de implementação de cada fluxo consta nos CDU; uma definição aqui não representa aceite formal do requisito.

## Termos do domínio

| Termo | Definição no IVIS |
|---|---|
| **Conta** | Identidade de acesso com e-mail, senha protegida e tipo `ALUNO` ou `INSTITUICAO`. Cada conta possui o perfil correspondente; a modalidade de acesso do aluno deriva de contrato ou vínculo, não de um terceiro tipo de conta. |
| **Aluno** | Pessoa que utiliza o IVIS para cadastrar habilidades e vagas de interesse, realizar treinamentos e consultar o próprio histórico. |
| **Acesso individual** | Acesso ao treinamento derivado de contrato individual vigente do próprio aluno. A interface usa este nome; a API informa origem `PROPRIO`. Um aluno sem vínculo institucional ativo pode ser elegível para contratar esse plano. |
| **Acesso institucional** | Acesso ao treinamento derivado de vínculo ativo do aluno com contrato institucional vigente. A interface usa este nome; a API informa origem `VINCULO_INSTITUCIONAL`. |
| **Instituição de Ensino** | Conta institucional que contrata plano institucional e gerencia os alunos vinculados às vagas de alunos. |
| **Plano individual** | Oferta contratada diretamente por um aluno para obter os benefícios e o acesso correspondentes. |
| **Plano institucional / plano de consórcio** | Oferta contratada por uma instituição para vincular alunos. No catálogo acadêmico atual há capacidades de 50, 100 ou alunos ilimitados por 30 dias; “consórcio” é o nome de negócio usado nos requisitos. |
| **Contrato** | Registro da contratação de um plano por uma conta. Mantém estado, vigência e o preço acordado, preservando o histórico mesmo que o catálogo de planos mude. |
| **Acesso vigente** | Permissão para usar recursos de treinamento, derivada de contrato individual ativo do aluno ou vínculo ativo com contrato institucional vigente. Consultar o histórico próprio não depende desse acesso, apenas de conta de aluno ativa. |
| **Vaga de interesse** | Vaga de emprego cadastrada pelo aluno como contexto para uma simulação de entrevista. Não representa uma vaga de assento do plano institucional. |
| **Vaga institucional** | Lugar de acesso ocupado por um vínculo ativo em contrato institucional finito. Vagas usadas são contadas a partir dos vínculos ativos; nos planos de 50 e 100 não podem ultrapassar a capacidade. No plano ilimitado, a capacidade não tem teto numérico. Não é uma vaga de emprego. |
| **Vínculo institucional** | Registro que associa um aluno a um contrato institucional. A remoção encerra o vínculo sem apagar seu histórico; o aluno pode ter vários vínculos históricos, mas no máximo um ativo por vez. A instituição titular é obtida pelo contrato. |
| **Habilidade técnica** | Competência ou tecnologia registrada no perfil do aluno, com nível de domínio de 1 a 5, usada como possível contexto do treinamento. |
| **Treinamento / simulação de entrevista** | Sessão textual em que a API de IA gera perguntas a partir das habilidades ou de uma vaga do aluno. A sessão persiste o contexto, as perguntas e as respostas, pode ser retomada enquanto está em andamento e recebe avaliação ao concluir. A interface atual usa três perguntas. |
| **Treino em andamento** | Treinamento ainda não concluído nem cancelado. O backend recupera sua sessão persistida para o aluno com acesso vigente, inclusive após recarregar a página. |
| **Pergunta de entrevista** | Pergunta gerada ou selecionada para uma sessão de treinamento, registrada em sequência para compor o histórico. |
| **Resposta** | Conteúdo enviado pelo aluno em resposta a uma pergunta do treinamento. Deve ser enviado à IA somente quando necessário para processar a sessão. |
| **Feedback / avaliação** | Resultado produzido na conclusão do treino. Com o provedor real, pode incluir nota, critérios por pergunta, justificativas, pontos fortes, melhorias e limitações. O modo stub não realiza avaliação qualitativa; o feedback de IA pode conter erros. |
| **Histórico de treinamentos** | Consulta dos treinos concluídos e de seus resultados pelo aluno titular. Continua disponível à conta de aluno ativa mesmo sem plano vigente; iniciar ou continuar um treino exige acesso vigente. |
| **Pagamento simulado** | Resultado fictício produzido pelo simulador controlado pelo backend no ambiente de apresentação. Não gera cobrança nem encaminha os dados do cartão a um gateway real; PAN e CVV não são persistidos. |
| **Cartão de teste** | Dados fictícios aceitos pelo simulador acadêmico para demonstrar a contratação. Não representam um meio de pagamento real. |
| **Auditoria** | Registro protegido de ações sensíveis, incluindo ator, ação, resultado e momento. O backend grava um evento por tentativa de contratação/pagamento simulado e eventos de criação ou remoção de vínculo. A retenção mínima exigida é de 4 anos; backup, arquivamento e descarte dependem de procedimento operacional. |
| **Código de recuperação** | Código de oito dígitos enviado ao e-mail cadastrado para validar a redefinição de senha. É de uso único e tem validade limitada; no ambiente local o Mailpit captura a mensagem. |
| **Autorização de redefinição** | Credencial temporária criada após validar o código de recuperação. Fica em cookie HttpOnly por até dez minutos e autoriza definir a nova senha; o fluxo atual não usa token na URL. |
| **Autoridade do backend** | Princípio segundo o qual o servidor valida identidade, permissões, preço, modalidade, capacidade, estados e resultados de negócio. O frontend não pode alterar essas decisões. |

## Requisitos e documentação

| Sigla ou termo | Definição |
|---|---|
| **RF** | Requisito Funcional: comportamento ou capacidade que o sistema deve oferecer. O IVIS tem 20 RF registrados em `IVIS_Requisitos.md`. |
| **RNF** | Requisito Não Funcional: atributo ou restrição de qualidade do sistema. Os quatro RNF aprovados atualmente são RNF01, RNF03, RNF07 e RNF09. |
| **RF01–RF20** | Identificadores dos requisitos funcionais aprovados; o texto de cada requisito está em [`IVIS_Requisitos.md`](IVIS_Requisitos.md). |
| **RNF01 — Aplicação Web** | O sistema é acessado por navegador, sem instalação obrigatória pelo usuário. |
| **RNF03 — Tempo de resposta da IA** | A próxima pergunta e a avaliação final têm meta separada de p95 até 10 segundos, sujeita a medição com provedor real. |
| **RNF07 — Isolamento entre instituições** | Uma instituição só pode visualizar e gerenciar os alunos vinculados aos seus próprios contratos. |
| **RNF09 — Proteção e retenção dos logs** | Os registros de auditoria devem ser protegidos contra alteração e mantidos por no mínimo 4 anos desde o evento. |
| **Caso de uso (CDU)** | Descrição de como um ator interage com o sistema para alcançar um objetivo. Os documentos estão em [`../cdu/`](../cdu/). |
| **ADR** | *Architecture Decision Record*: documento que registra contexto, decisão arquitetural, consequências e alternativas. Os ADRs atuais estão em [`adr/`](adr/). |
| **Em andamento (documentação)** | Estado de revisão de uma decisão ou artefato. É diferente da situação `EM_ANDAMENTO` de uma sessão de treinamento persistida. |

## Arquitetura e componentes

| Termo | Definição no IVIS |
|---|---|
| **Frontend** | Aplicação web Vue e TypeScript que apresenta a interface e consome a API do backend. Não acessa diretamente o banco, a API de IA ou serviços de pagamento. |
| **Backend** | Serviço Java 25 LTS com Spring Boot que aplica regras de negócio, autentica e autoriza usuários, persiste dados e coordena integrações. Usa Gradle como ferramenta de build. |
| **API de IA** | Serviço Python separado que processa solicitações de geração de perguntas e avaliação. É chamado pelo backend, não pelo navegador. |
| **Provedor NVIDIA** | Integração da API de IA com endpoint NVIDIA compatível com OpenAI, chamada de “NVIDIA NGC” em documentos de planejamento anteriores. Modelo e URL base têm valores padrão configurados no serviço Python e podem ser alterados por `NVIDIA_MODEL` e `NVIDIA_BASE_URL`; a chamada real exige `NVIDIA_API_KEY`. |
| **Modo stub da IA** | Respostas locais de demonstração quando o provedor real não está em uso. Permite exercitar o fluxo, mas não comprova a qualidade da avaliação nem o desempenho exigido pelo RNF03. |
| **API** | Interface de programação que define como módulos ou serviços trocam solicitações e respostas. Os contratos do IVIS devem ser versionados e validados. |
| **Módulo** | Parte do sistema com responsabilidade delimitada e interface explícita. O IVIS separa frontend, backend e API de IA; o backend organiza internamente capacidades de domínio. |
| **Regra de negócio** | Condição que define o comportamento do domínio, como quem pode contratar, quais benefícios estão ativos, quantas vagas existem ou quando um plano permite acesso. É validada pelo backend. |
| **Integração serviço a serviço** | Comunicação entre componentes no servidor, como a chamada autenticada do backend à API de IA. Segredos não são expostos ao navegador. |
| **Sessão de autenticação** | Estado de login da conta. O frontend usa token de acesso; um cookie protegido permite renovar a sessão, que pode ser revogada ao sair ou trocar a senha. É distinta da sessão de treinamento. |

## Banco de dados e operação

| Termo | Definição no IVIS |
|---|---|
| **PostgreSQL** | Banco de dados relacional escolhido para persistir os dados de domínio do IVIS. NoSQL não será utilizado para esse propósito. |
| **Flyway** | Ferramenta de migração que aplica alterações versionadas ao esquema do banco. Migrações já aplicadas não devem ser editadas em ambientes compartilhados. |
| **Migração** | Arquivo versionado que cria ou altera tabelas, colunas, restrições ou índices de forma reproduzível. |
| **Chave primária (PK)** | Identificador único de uma linha de tabela. As tabelas de domínio do IVIS usam UUIDs como chaves primárias. |
| **Chave estrangeira (FK)** | Coluna ou conjunto de colunas que referencia uma linha relacionada em outra tabela, mantendo integridade referencial. |
| **UUID** | Identificador universalmente único usado como chave primária `objeto` em todas as tabelas do banco. |
| **`snake_case`** | Convenção de identificadores com palavras minúsculas separadas por sublinhado, como `codigo_objeto_perfis_aluno`. No banco IVIS, nomes em português brasileiro e sem acentos. |
| **Centavos / unidade monetária mínima** | Forma inteira de representar dinheiro no banco. Por exemplo, R$ 19,90 é armazenado como `1990` centavos; a formatação monetária é aplicada para exibição. |
| **Snapshot** | Cópia imutável dos dados relevantes em um momento específico, usada para preservar o preço contratado ou o contexto de uma entrevista sem depender de alterações posteriores na origem. |
| **JSONB** | Tipo do PostgreSQL para dados JSON consultáveis. No modelo IVIS é restrito a snapshots ou metadados controlados, não substitui tabelas relacionais centrais. |
| **Soft delete / arquivamento** | Marcar um registro como inativo/arquivado em vez de apagá-lo fisicamente, para preservar relações e histórico quando necessário. |
| **Índice** | Estrutura do banco que pode acelerar consultas; deve corresponder aos padrões de acesso e não substituir validações de autorização. |
| **Idempotência** | Propriedade de uma operação em que repetir a mesma solicitação não duplica seus efeitos, necessária, por exemplo, para tentativas repetidas de pagamento. |
| **Mailpit** | Servidor SMTP de captura usado no ambiente local para inspecionar e-mails de boas-vindas, recuperação de senha e confirmação de contratação simulada, sem encaminhá-los a destinatários reais. Não é provedor de e-mail de produção. |
| **Gateway de pagamento** | Serviço externo que processa cobranças reais. Não é utilizado pelo simulador acadêmico; uma integração real depende de decisão e configuração futuras. |
| **p95** | Percentil 95 dos tempos de resposta medidos: 95% das operações observadas ficam nesse tempo ou abaixo. O RNF03 estabelece meta de até 10 segundos para geração da próxima pergunta e para avaliação final, medidas separadamente com o provedor real; a meta ainda depende de medição registrada. |

## Convenções de nomes no código

Nomes próprios de tabelas, colunas, classes, funções e métodos devem estar em português brasileiro, sem acentos nos identificadores. No banco, usar `snake_case`; em classes, `PascalCase`; em métodos e funções, seguir a convenção da linguagem, mantendo as palavras em português. Símbolos e nomes impostos por bibliotecas externas podem permanecer como definidos por elas.

---

**Documentos relacionados:** [requisitos](IVIS_Requisitos.md), [tecnologias](IVIS_Tecnologias.md), [ADR](adr/README.md), [modelo de banco](BD/Modelo_Relacional_IVIS.md) e [planos de implementação](BD/Plano_Implementacao_BD.md).
