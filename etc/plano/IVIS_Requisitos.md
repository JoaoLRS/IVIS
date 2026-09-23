# IVIS — Requisitos Funcionais e Não Funcionais (Revisão)

Este documento reúne os requisitos do IVIS já **ajustados** com base na validação feita em cima dos Casos de Uso, Histórias de Usuário e Escopo do projeto. Itens marcados com 🆕 são novos (surgiram da validação) e itens marcados com ✏️ foram reescritos para corrigir uma inconsistência apontada anteriormente.

---

## Requisitos Funcionais (RF)

### Acesso e Perfil

- **RF01**: O sistema deve permitir que Aluno e Instituição de Ensino criem conta e façam login.
- **RF02**: O sistema deve permitir que Aluno e Instituição de Ensino gerenciem os dados da própria conta (dados pessoais/institucionais, foto/logo, senha).
- **RF03**: O sistema deve permitir que o Aluno gerencie seu perfil de habilidades técnicas.
- **RF04** 🆕: O sistema deve permitir que o usuário recupere o acesso à conta em caso de esquecimento de senha ("esqueci minha senha"), via e-mail cadastrado.
- **RF05** 🆕: O sistema deve permitir que Aluno e Instituição de Ensino visualizem os detalhes do plano vigente (tipo de plano, validade e benefícios inclusos).

### Treinamento

- **RF06**: O sistema deve permitir que o Aluno gerencie (cadastre, edite e exclua) vagas de interesse para treinamento.
- **RF07**: O sistema deve realizar a simulação de entrevista (treinamento) processando as respostas via IA.
- **RF08**: O sistema deve gerar um feedback/avaliação de desempenho ao final de cada treino.
- **RF09**: O sistema deve armazenar o histórico de treinos realizados por aluno.
- **RF10** ✏️: O sistema deve permitir que o Aluno consulte seu histórico de treinos, exibindo data, vaga/habilidade treinada e resultado. *(antes estava junto do RF09, que fala só em "armazenar" — separado para deixar explícita a consulta, coberta pela US05/UC8)*

### Planos e Contratação

- **RF11**: O sistema deve exibir uma Home com os planos disponíveis (individual e institucional/consórcio), incluindo preço e benefícios.
- **RF12** 🆕: A Home com os planos disponíveis deve poder ser visualizada por um visitante não autenticado, antes da criação de conta. *(a equipe deve confirmar se essa é de fato a intenção comercial)*
- **RF13**: O sistema deve permitir que o Aluno Pagante contrate um plano individual diretamente pela plataforma.
- **RF14**: O sistema deve permitir que a Instituição de Ensino contrate um plano de consórcio, definindo a quantidade de vagas.
- **RF15**: O sistema deve processar pagamentos de contratação de planos por meio de um gateway de pagamento externo.
- **RF16**: O sistema deve permitir que a Instituição de Ensino cadastre/vincule e remova alunos dentro do limite de vagas do plano de consórcio contratado.
- **RF17**: O sistema deve diferenciar o acesso de AlunoConsórcio e AlunoPagante conforme o plano contratado/vinculado.
- **RF18**: O sistema deve permitir que a Instituição de Ensino visualize a quantidade de vagas do plano já utilizadas e disponíveis.
- **RF19** ✏️: A contratação de plano individual (RF13) deve ficar restrita ao AlunoPagante — um AlunoConsórcio não deve contratar um plano individual próprio enquanto estiver vinculado a uma instituição. *(corrige a inconsistência do Diagrama de Casos de Uso, em que a seta de "Contratar Plano" saía do ator genérico Aluno)*

### Auditoria

- **RF20** 🆕: O sistema deve registrar um log de auditoria das ações sensíveis (contratação de plano, vínculo/remoção de aluno, pagamento), incluindo quem realizou a ação e quando.

---

## Requisitos Não Funcionais (RNF)

- **RNF01**: O sistema deve ser acessível via navegador web (Aplicação Web).
- **RNF02**: O sistema deve estar em conformidade com a LGPD, dado o tratamento de dados pessoais de alunos e instituições (relevante frente à ANPD como stakeholder).
- **RNF03**: O tempo de resposta da IA durante a simulação de entrevista não deve comprometer a fluidez da conversa (definir SLA, ex.: resposta em até X segundos).
- **RNF04**: O sistema deve ser responsivo, funcionando em desktop e dispositivos móveis.
- **RNF05**: O sistema deve garantir disponibilidade adequada para uso educacional (definir % de uptime esperado).
- **RNF06**: O sistema não deve armazenar diretamente dados sensíveis de pagamento (ex.: número de cartão), delegando o processamento a um gateway certificado (PCI-DSS).
- **RNF07**: O sistema deve garantir que apenas a própria Instituição de Ensino visualize/gerencie os alunos vinculados ao seu plano (isolamento de dados entre instituições).
- **RNF08** 🆕: O sistema deve armazenar senhas de forma criptografada (hash) e encerrar a sessão do usuário automaticamente após um período de inatividade.
- **RNF09** 🆕: Os registros de auditoria (RF20) devem ser mantidos por um prazo mínimo, a ser definido com a equipe jurídica, e protegidos contra alteração.

> ⚠️ Todos os itens marcados como 🆕 ou ✏️ são propostas para a equipe validar, ajustar ou descartar — não substituem a definição formal do time.

---

## Resumo

| Categoria | Quantidade |
|---|---|
| Requisitos Funcionais (RF) | 20 |
| Requisitos Não Funcionais (RNF) | 9 |
| **Total** | **29** |
