# Modelo relacional do IVIS — proposta inicial

Este documento descreve as tabelas e regras de relacionamento propostas para o banco PostgreSQL do IVIS. É o detalhamento associado ao [ADR-0010](../adr/ADR-0010-modelagem-relacional-do-banco.md); deve ser validado pela equipe antes da criação das migrações Flyway. O **backend** é o único módulo que lê e grava os dados de negócio.

## Princípios e convenções

- PostgreSQL relacional; não utilizar NoSQL.
- Uma chave primária UUID por tabela; nomes de tabelas e colunas em português brasileiro, usando `snake_case` sem acentos. Classes de entidade e funções/métodos do backend também devem usar nomes em português brasileiro (`PascalCase` para classes e `camelCase` para funções/métodos), sem acentos nos identificadores.
- Horários em `TIMESTAMPTZ`, armazenados em UTC. Valores monetários em centavos (`BIGINT` ou `INTEGER` conforme limite) e código da moeda (`CHAR(3)`), nunca ponto flutuante.
- Regras de domínio são validadas no backend em toda operação. Constraints no banco reforçam unicidade, nulabilidade, cardinalidade e limites simples.
- Proibir `ON DELETE CASCADE` em contratos, transações, treinos e auditoria. Usar estado de desativação/encerramento e política de retenção definida pela equipe.
- Armazenar somente os dados necessários. Não criar colunas para PAN/número de cartão, CVV, senha em texto puro ou segredos de integração.
- `JSONB` pode ser considerado apenas para snapshot imutável do contexto enviado à IA ou metadados técnicos controlados; dados consultados e relacionados ficam em colunas/tabelas relacionais.

## Diagrama de relacionamentos

```mermaid
erDiagram
    CONTAS ||--o| PERFIS_ALUNO : "perfil de aluno"
    CONTAS ||--o| PERFIS_INSTITUICAO : "perfil de instituicao"
    CONTAS ||--o{ TOKENS_RECUPERACAO_SENHA : recupera
    PERFIS_ALUNO ||--o{ HABILIDADES : possui
    PERFIS_ALUNO ||--o{ VAGAS_INTERESSE : cadastra
    VAGAS_INTERESSE ||--o{ REQUISITOS_VAGA : detalha
    PLANOS ||--o{ BENEFICIOS_PLANO : oferece
    CONTAS ||--o{ CONTRATOS : contrata
    PLANOS ||--o{ CONTRATOS : referencia
    CONTRATOS ||--o{ VINCULOS_INSTITUCIONAIS : disponibiliza
    PERFIS_ALUNO ||--o{ VINCULOS_INSTITUCIONAIS : vincula
    CONTRATOS ||--o{ PAGAMENTOS : recebe
    PERFIS_ALUNO ||--o{ TREINAMENTOS : realiza
    VAGAS_INTERESSE o|--o{ TREINAMENTOS : contextualiza
    TREINAMENTOS ||--o{ PERGUNTAS_ENTREVISTA : contem
    PERGUNTAS_ENTREVISTA ||--o| RESPOSTAS_ENTREVISTA : respondida
    TREINAMENTOS ||--o| AVALIACOES_TREINAMENTO : avaliada
    CONTAS o|--o{ EVENTOS_AUDITORIA : "ator (pode ser sistema)"
```

`CONTAS` distingue `ALUNO` e `INSTITUICAO`. Cada conta deve ter exatamente um perfil do subtipo correspondente. A coerência entre discriminador e perfil é assegurada pelo backend; pode ser reforçada por restrições/gatilhos se o projeto precisar.

## Catálogo proposto de tabelas

### Identidade e perfis

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `contas` | `id`, `tipo_conta`, `email`, `email_normalizado`, `hash_senha`, `situacao`, `criado_em`, `atualizado_em` | Identidade e autenticação. `email_normalizado` único; senha sempre em hash. `tipo_conta` limitado a `ALUNO` ou `INSTITUICAO`. Desativar em vez de apagar sem avaliar dependências. |
| `perfis_aluno` | `conta_id` PK/FK, `nome_exibicao`, `url_foto`, `criado_em`, `atualizado_em` | Extensão 1:1 de uma conta `ALUNO`. Só o aluno dono pode alterar o próprio perfil. |
| `perfis_instituicao` | `conta_id` PK/FK, `razao_social`, `documento_fiscal`, `url_logo`, `criado_em`, `atualizado_em` | Extensão 1:1 de uma conta `INSTITUICAO`. `documento_fiscal` único se exigido pelo produto; acesso restrito à instituição e ao backend. |
| `tokens_recuperacao_senha` | `id`, `conta_id` FK, `hash_token`, `expira_em`, `consumido_em`, `criado_em` | Tokens de recuperação de uso único (RF04). Persistir apenas hash do token, expirar e marcar consumo; limitar solicitações no backend. A mensagem/link é capturada pelo Mailpit no ambiente de apresentação. |

Não duplicar `AlunoPagante`/`AlunoConsórcio` como papel editável no cliente. A modalidade é derivada pelo backend a partir do plano individual ativo ou de vínculo institucional ativo, aplicando RF17 e RF19.

### Habilidades e vagas

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `habilidades` | `id`, `conta_aluno_id`, `nome`, `nome_normalizado`, `categoria`, `nivel`, `criado_em`, `atualizado_em` | Uma habilidade pertence a um aluno. `nivel` limitado ao intervalo definido pelo produto (proposta: 1–5). Impedir duplicata do mesmo nome normalizado por aluno. |
| `vagas_interesse` | `id`, `conta_aluno_id`, `titulo`, `empresa`, `senioridade`, `descricao`, `criado_em`, `atualizado_em`, `arquivado_em` | Uma vaga pertence a um aluno. Arquivar ou preservar o contexto quando já referenciada por um treino; não excluir histórico associado. |
| `requisitos_vaga` | `id`, `vaga_interesse_id`, `texto_requisito`, `ordem` | Requisitos/tecnologias da vaga em linhas próprias, na ordem informada. Evita lista delimitada ou array opaco na coluna da vaga. |

### Planos, contratos e vínculos institucionais

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `planos` | `id`, `codigo`, `modalidade`, `nome`, `descricao`, `preco_centavos`, `moeda`, `ativo`, `criado_em`, `atualizado_em` | Catálogo; `modalidade` limitado a individual/institucional; preço não negativo. Só backend administra ofertas e preço vigente. |
| `beneficios_plano` | `id`, `plano_id`, `texto_beneficio`, `ordem` | Benefícios de exibição do catálogo, ordenados por `ordem`. |
| `contratos` | `id`, `conta_titular_id`, `plano_id`, `situacao`, `quantidade_vagas`, `preco_centavos_contratado`, `moeda`, `iniciado_em`, `encerrado_em`, `criado_em`, `atualizado_em` | Contratação/histórico de plano. `conta_titular_id` é aluno para plano individual e instituição para plano institucional. Guardar preço contratado como registro histórico. `quantidade_vagas=1` para individual; institucional com capacidade positiva. A compatibilidade plano/conta é validada no backend. |
| `vinculos_institucionais` | `id`, `contrato_id`, `conta_aluno_id`, `situacao`, `vinculado_em`, `desvinculado_em`, `criado_por_conta_id` | Histórico de vínculo do aluno a uma contratação institucional. Cada linha referencia o contrato institucional, do qual se deriva a instituição proprietária. Índice único parcial impede mais de um vínculo institucional ativo por aluno, salvo futura regra explícita. |

A quantidade de vagas usadas é a contagem de `vinculos_institucionais` ativos para o contrato. Vagas disponíveis = `contratos.quantidade_vagas` menos essa contagem. Não armazenar a quantidade de vagas usadas como valor independente, pois poderia divergir dos vínculos.

### Pagamentos

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `pagamentos` | `id`, `contrato_id`, `modo`, `situacao`, `valor_centavos`, `moeda`, `metodo`, `referencia_provedor`, `criado_em`, `atualizado_em`, `confirmado_em` | Tentativas e resultado associado a uma contratação. `modo` distingue `SIMULADO` de futuro `REAL`; `situacao` é controlada pelo backend. Para simulação, registrar apenas resultado e referência/cenário fictício se necessário; nunca PAN/CVV. Uma compra simulada jamais deve ser confundida com pagamento real. |

Uma contratação pode ter várias tentativas; cada tentativa pertence a exatamente uma contratação. A ativação do plano é uma decisão do backend após resultado aprovado (simulado no modo acadêmico ou confirmação confiável do gateway real, se adotado futuramente). Usar chave de idempotência/identificador de operação para não duplicar efeito ao repetir requisições.

### Entrevistas e histórico

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `treinamentos` | `id`, `conta_aluno_id`, `vaga_interesse_id` nullable, `tipo_contexto`, `situacao`, `contexto_registrado`, `iniciado_em`, `finalizado_em`, `criado_em` | Uma sessão pertence a um aluno e opcionalmente a uma vaga. `tipo_contexto` distingue treino por habilidades ou vaga. O contexto registrado conserva os dados usados mesmo após edição/arquivamento da vaga ou das habilidades. RF09 é atendido pela persistência da sessão e seus dados relacionados. |
| `perguntas_entrevista` | `id`, `treinamento_id`, `numero_sequencial`, `texto_pergunta`, `criado_em` | Perguntas da sessão em ordem única por sessão. |
| `respostas_entrevista` | `id`, `pergunta_id` UNIQUE, `texto_resposta`, `criado_em` | Uma resposta registrada por pergunta no fluxo inicial; deixar a cardinalidade explícita. Não enviar conteúdo a serviços além do necessário para a operação de IA. |
| `avaliacoes_treinamento` | `id`, `treinamento_id` UNIQUE, `nota`, `pontos_fortes`, `pontos_melhoria`, `gerado_em`, `versao_modelo` | No máximo uma avaliação vigente por sessão (histórico de tentativas de geração pode ser adicionado se necessário). `nota` limitada à escala adotada. RF08 e consulta detalhada do RF10 usam estes dados. |

RF10 consulta sessões do aluno autenticado, nunca um identificador de conta de aluno escolhido livremente pelo cliente sem verificação de propriedade. Para preservar histórico, mudanças posteriores nas entidades de origem não reescrevem perguntas, respostas, avaliações ou contexto registrado do treinamento.

### Auditoria

| Tabela | Colunas principais | Regras e finalidade |
|---|---|---|
| `eventos_auditoria` | `id`, `ator_conta_id` nullable, `acao`, `resultado`, `tipo_recurso`, `recurso_id`, `ocorrido_em`, `detalhes` (JSONB restrito) | Eventos de contratação, pagamento e inclusão/remoção de vínculo (RF20). `ator_conta_id` nulo apenas para ação automática sem ator humano identificável. Papel da aplicação sem permissão de alteração/remoção; acesso de leitura restrito. `detalhes` não deve guardar senha, token, PAN/CVV ou conteúdo completo da requisição. Aplicar retenção definida pelo RNF09. |

Mailpit captura mensagens SMTP fora do banco de domínio. Não criar tabela que replique seu conteúdo ou tokens de redefinição sem necessidade funcional e política de retenção definida.

## Regras de relacionamento e integridade

1. **Conta e subtipo:** uma conta é exatamente Aluno ou Instituição; tem apenas o perfil de subtipo correspondente. O cliente não define nem altera o tipo como regra de autorização.
2. **Propriedade do aluno:** habilidades, vagas, sessões e consultas de histórico sempre são escopadas ao aluno autenticado pelo backend. FK e índice ajudam integridade/performance, mas autorização é feita no servidor.
3. **Propriedade institucional (RNF07):** a instituição proprietária de um vínculo deriva da assinatura institucional. Toda leitura/escrita de vínculo valida que a assinatura pertence à instituição autenticada; IDs informados pelo cliente não provam propriedade.
4. **Plano e contrato:** um contrato pertence a uma única conta e referencia um plano. O backend valida a modalidade do plano contra o tipo de conta antes de gravar. Situação ativa, vigência e preço vêm dos dados do servidor, nunca de campos confiados ao navegador.
5. **Vagas institucionais:** vínculo ativo só pode ser criado para um contrato institucional ativo e com capacidade livre. A checagem e inserção ocorrem em transação com bloqueio/concorrência apropriado; ao remover vínculo, encerrar linha histórica e liberar a capacidade. A soma de vínculos ativos não ultrapassa `quantidade_vagas`.
6. **Vínculo único:** um aluno não pode ter mais de um vínculo institucional ativo por vez. A regra RF19 também impede contrato individual enquanto houver vínculo institucional ativo. Casos de transferência e coexistência ainda precisam de definição da equipe.
7. **Pagamentos:** um pagamento pertence a um contrato; contrato pode ter múltiplas tentativas. Só backend cria ou atualiza estado. Notificação duplicada deve ser idempotente. Estado simulado não pode ser apresentado como cobrança real.
8. **Histórico de treino:** sessão pertence a um aluno; pergunta pertence à sessão; resposta pertence à pergunta; feedback pertence a uma sessão. Sessões preservam snapshot e não são apagadas em cascata junto com vaga/perfil.
9. **Auditoria e retenção (RNF09):** evento inclui quem/quando/o quê/resultado, é somente acrescentado, tem acesso restrito e segue prazo mínimo de retenção a definir. Não permitir edição pelo usuário ou remoção em cascata.
10. **Chaves e exclusões:** FKs usam política restritiva por padrão. `SET NULL` só quando a relação for opcional e o histórico puder continuar inteligível; soft delete/arquivamento é preferível para entidades referenciadas.

## Índices e constraints recomendados

- `UNIQUE (email_normalizado)` em `contas`; índice/restrição para documento fiscal da instituição, se obrigatório.
- `CHECK` em `tipo_conta`, situação/modalidade, nível de habilidade, valores monetários não negativos e nota dentro da escala.
- `UNIQUE (conta_aluno_id, nome_normalizado)` em `habilidades`.
- `UNIQUE (vaga_interesse_id, ordem)` e `UNIQUE (treinamento_id, numero_sequencial)`.
- índice de treinamentos por `(conta_aluno_id, iniciado_em DESC)` para RF10.
- índices em chaves estrangeiras usadas em consultas de titularidade: `contratos(conta_titular_id,situacao)`, `vinculos_institucionais(contrato_id,situacao)` e `vinculos_institucionais(conta_aluno_id,situacao)`.
- índice único parcial em `vinculos_institucionais(conta_aluno_id) WHERE situacao='ATIVO'` para vínculo ativo único.
- `UNIQUE (pergunta_id)` em respostas e `UNIQUE (treinamento_id)` em avaliações.
- índice em pagamentos por `(contrato_id, criado_em DESC)` e chave de idempotência única quando implementada.
- índice de auditoria por `(tipo_recurso, recurso_id, ocorrido_em DESC)` e `(ator_conta_id, ocorrido_em DESC)`.

Constraints que dependem de outras linhas/tabelas (por exemplo, capacidade de vagas, modalidade e transições de estado) não devem depender apenas de `CHECK`: o backend implementa a regra em transação e o banco reforça a integridade possível.

## Evolução com Flyway

- Criar cada alteração por migração versionada e revisada; não editar migrações já aplicadas em ambientes compartilhados.
- A primeira migração cria tabelas, FKs, constraints e índices em ordem de dependência.
- Evoluções de situação/colunas devem preservar dados existentes e incluir estratégia de preenchimento retroativo quando necessário.
- Usar dados de demonstração separados de migrações de schema; nunca inserir segredos, senhas reais, tokens ou cartões em massa.

## Pendências para validar antes da primeira migração

- Campos obrigatórios de aluno/instituição e necessidade de foto/logo e CNPJ.
- Moeda, ciclo de vigência, renovação e regras de preço/capacidade dos planos.
- Transferência de aluno entre instituições e efeito de remoção/vencimento do plano no acesso.
- Política de exclusão/anonimização e períodos concretos de retenção para conta, treino e auditoria.
- Formato do contexto de IA e necessidade de persistir todas as respostas ou somente registros selecionados.
- Esquema de estados e cenários do simulador de pagamento.
