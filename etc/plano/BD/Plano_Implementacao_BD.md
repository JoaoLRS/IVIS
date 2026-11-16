# Plano de implementação — Banco de dados

Este plano transforma o [modelo relacional proposto](Modelo_Relacional_IVIS.md) em entregas de banco PostgreSQL e migrações Flyway. O backend Java é o único módulo autorizado a acessar e alterar os dados de negócio. O modelo e os ADRs continuam em revisão pela equipe.

## Princípios obrigatórios

- Usar PostgreSQL relacional; não introduzir NoSQL.
- Escrever nomes de tabelas, colunas, restrições e objetos do banco em português brasileiro, em `snake_case` sem acentos.
- Representar dinheiro como inteiro em centavos mais código de moeda; nunca usar ponto flutuante. O backend formata o valor para exibição.
- Gravar datas com fuso em UTC, usar UUIDs como identificadores e armazenar senhas/tokens somente como hashes.
- Não persistir número completo de cartão nem código de segurança. Pagamentos de apresentação são simulados pelo backend.
- Preservar histórico de contratos, pagamentos, treinamentos e auditoria; evitar exclusão em cascata.

## Etapas

### 1. Revisar o modelo de domínio

- Validar com a equipe as tabelas, cardinalidades, campos obrigatórios e estados descritos em `Modelo_Relacional_IVIS.md`.
- Fechar regras ainda pendentes: exclusão/anonimização de conta e treino, operação da retenção de auditoria por 4 anos, renovação/troca de planos, transferência entre instituições e ciclo de cobrança real.
- Confirmar quais dados de perfil são necessários e se documentos fiscais serão coletados.
- Registrar alterações aprovadas no ADR-0010 e neste modelo antes da primeira migração.

### 2. Preparar a persistência local

- Disponibilizar PostgreSQL para desenvolvimento por configuração reproduzível do ambiente local.
- Configurar conexão do backend por variáveis de ambiente; manter credenciais fora do repositório.
- Integrar Flyway ao backend e definir como migrações são executadas localmente e em cada ambiente.
- Definir usuários/perfis do banco com privilégios mínimos; a aplicação não deve executar DDL em ambiente publicado.

### 3. Implementar migrações incrementais

- **Concluída para o schema central do produto:** `V2__modelo_relacional_treinos.sql` e `V3__modelo_relacional_contas_planos_pagamentos_auditoria.sql` criam identidade, segurança, perfil, habilidades, vagas, treinos, Q&A, avaliação, planos, contratos, vínculos institucionais, pagamentos e auditoria.
- Evoluções futuras continuam entrando por novas migrações Flyway, sem editar versões já aplicadas.

- Manter as migrações versionadas em ordem de dependência; as tabelas centrais atuais estão distribuídas entre V2 e V3.
- Declarar PKs, FKs, `NOT NULL`, unicidade, índices e `CHECK` para regras estruturais.
- Usar nomes em português brasileiro também para índices e restrições.
- Não editar migrações já aplicadas em ambientes compartilhados; corrigir com nova migração.
- Separar dados fictícios de demonstração das migrações de estrutura.

### 4. Implementar repositórios e regras transacionais no backend

- **Implementado:** entidades, repositórios e operações de domínio iniciam treino, persistem perguntas e respostas, concluem com avaliação e consultam histórico. Manter testes de regressão para essas operações.

- Criar entidades e repositórios com nomes em português brasileiro; classes em `PascalCase`, métodos em `camelCase`, identificadores sem acentos.
- Derivar a conta autenticada no servidor; não aceitar do frontend IDs como prova de titularidade.
- Fazer contratação e reserva de vaga em transação, protegendo contra concorrência e excesso de vagas.
- Garantir idempotência no processamento de pagamento simulado e nas operações que possam ser repetidas.
- Persistir snapshots necessários para conservar o contexto histórico de treinamentos e preços contratados.

### 5. Validar a operação do banco

- Conferir que um banco vazio pode ser criado integralmente pelas migrações Flyway.
- Verificar integridade referencial, limites de vagas, unicidade de vínculo ativo e consultas isoladas por instituição/aluno.
- Definir rotina de backup e restauração antes de qualquer publicação com dados persistentes.
- Documentar dados fictícios de demonstração e procedimento de limpeza compatível com retenção.

## Critérios de conclusão

- Modelo e ADR-0010 revisados pela equipe.
- Migrações reproduzíveis, versionadas e em português brasileiro.
- Backend é o único proprietário da persistência de domínio.
- Valores financeiros são exatos em centavos e pagamentos de apresentação não cobram dinheiro.
- Regras de integridade, isolamento e retenção aprovadas e documentadas.

## Dependências e pendências

- Aprovação do modelo relacional e dos procedimentos operacionais de retenção; o mínimo de 4 anos para auditoria já foi definido.
- Implementação do módulo backend e definição do ambiente de desenvolvimento.
- Definição final das regras de ciclo de vida de planos, vínculos e pagamentos.
