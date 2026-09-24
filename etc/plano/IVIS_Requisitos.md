# IVIS: Requisitos Funcionais e Não Funcionais


## Requisitos Funcionais (RF)

### Acesso e Perfil

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF01**: O sistema deve permitir que Aluno e Instituição de Ensino criem conta e façam login. | Alunos e Instituições de Ensino precisam de uma identidade própria na plataforma. Por isso o sistema oferece cadastro e login para os dois perfis, o que garante acesso individualizado e é a base para todas as demais funcionalidades. | Alta |
| **RF02**: O sistema deve permitir que Aluno e Instituição de Ensino gerenciem os dados da própria conta (dados pessoais/institucionais, senha). | Depois de criada a conta, o usuário deve poder mantê-la atualizada. O sistema permite editar dados pessoais ou institucionais e alterar a senha, sem depender de suporte. | Média |
| **RF03**: O sistema deve permitir que o Aluno gerencie seu perfil de habilidades técnicas. | O Aluno pode montar e atualizar seu perfil de habilidades técnicas, informando em que áreas atua ou quer se desenvolver. Esse perfil serve de referência para direcionar os treinos. | Média |
| **RF04**: O sistema deve permitir que o usuário recupere o acesso à conta em caso de esquecimento de senha ("esqueci minha senha"), via e-mail cadastrado. | Se o usuário esquecer a senha, pode recuperar o acesso pela opção "esqueci minha senha", com um procedimento de redefinição enviado ao e-mail cadastrado. Isso evita bloqueio permanente da conta. | Média |
| **RF05**: O sistema deve permitir que Aluno e Instituição de Ensino visualizem os detalhes do plano vigente (tipo de plano, validade e benefícios inclusos). | Aluno e Instituição de Ensino podem consultar os detalhes do plano vigente: tipo de plano, validade e benefícios inclusos. Assim sabem o que estão usando e quando precisam renovar. | Baixa |

### Treinamento

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF06**: O sistema deve permitir que o Aluno gerencie (cadastre, edite e exclua) vagas de interesse para treinamento. | O Aluno gerencia as vagas de interesse para as quais quer treinar, com cadastro, edição e exclusão. Essas vagas dão o contexto de cada simulação. | Alta |
| **RF07**: O sistema deve realizar a simulação de entrevista (treinamento) processando as respostas via IA. | O núcleo do produto é a simulação de entrevista. O sistema conduz o treino e processa as respostas do Aluno por meio de IA, reproduzindo uma conversa de entrevista real. | Alta |
| **RF08**: O sistema deve gerar um feedback/avaliação de desempenho ao final de cada treino. | Ao final de cada treino, o sistema gera um feedback com a avaliação do desempenho. Com ele o Aluno identifica pontos fortes e o que precisa melhorar. | Alta |
| **RF09**: O sistema deve armazenar o histórico de treinos realizados por aluno. | Cada treino realizado é armazenado e associado ao Aluno, formando um histórico persistente que permite acompanhar a evolução ao longo do tempo. | Alta |
| **RF10**: O sistema deve permitir que o Aluno consulte seu histórico de treinos, exibindo data, vaga/habilidade treinada e resultado. | O Aluno pode consultar o histórico de treinos, vendo data, vaga ou habilidade treinada e resultado de cada um. É a tela que torna a evolução visível. | Média |

### Planos e Contratação

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF11**: O sistema deve exibir uma Home com os planos disponíveis (individual e institucional/consórcio), incluindo preço e benefícios. | A Home apresenta os planos disponíveis, individual e institucional (consórcio), com preço e benefícios. Serve de vitrine para o usuário comparar as opções e escolher. | Alta |
| **RF12**: A Home com os planos disponíveis deve poder ser visualizada por um visitante não autenticado, antes da criação de conta. | A Home de planos é pública. Um visitante não autenticado pode ver as opções e os preços antes de criar conta, o que reduz a barreira de entrada. | Média |
| **RF13**: O sistema deve permitir que o Aluno Pagante contrate um plano individual diretamente pela plataforma. | O Aluno Pagante contrata um plano individual diretamente pela plataforma, de forma autônoma, sem intermediação de instituição. | Alta |
| **RF14**: O sistema deve permitir que a Instituição de Ensino contrate um plano de consórcio, definindo a quantidade de vagas. | A Instituição de Ensino contrata um plano de consórcio e define a quantidade de vagas, dimensionando o serviço conforme o número de alunos que pretende atender. | Alta |
| **RF15**: O sistema deve processar pagamentos de contratação de planos por meio de um gateway de pagamento externo. | Os pagamentos de contratação são processados por um gateway de pagamento externo. O sistema delega a transação a um serviço especializado, em vez de tratá-la internamente. | Alta |
| **RF16**: O sistema deve permitir que a Instituição de Ensino gerencie (visualize, cadastre/vincule e remova) alunos dentro do limite de vagas do plano de consórcio contratado. | A Instituição de Ensino administra seus alunos dentro do limite de vagas contratado: visualiza, cadastra ou vincula e remove. Isso permite reaproveitar vagas ao longo do tempo. | Alta |
| **RF17**: O sistema deve diferenciar o acesso de AlunoConsórcio e AlunoPagante conforme o plano contratado/vinculado. | O sistema distingue AlunoConsórcio de AlunoPagante e libera o acesso conforme o plano contratado ou vinculado. Cada perfil enxerga o que seu plano permite. | Alta |
| **RF18**: O sistema deve permitir que a Instituição de Ensino visualize a quantidade de vagas do plano já utilizadas e disponíveis. | A Instituição de Ensino acompanha quantas vagas do plano já foram usadas e quantas ainda estão disponíveis, para planejar novos vínculos ou a ampliação do plano. | Média |
| **RF19**: A contratação de plano individual (RF13) deve ficar restrita ao AlunoPagante — um AlunoConsórcio não deve contratar um plano individual próprio enquanto estiver vinculado a uma instituição. (corrige a inconsistência do Diagrama de Casos de Uso, em que a seta de "Contratar Plano" saía do ator genérico Aluno) | A contratação de plano individual é exclusiva do AlunoPagante. Um AlunoConsórcio, enquanto vinculado a uma instituição, não contrata plano individual próprio. Esse requisito corrige a inconsistência do Diagrama de Casos de Uso, em que a seta de "Contratar Plano" partia do ator genérico Aluno. | Alta |

### Auditoria

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF20**: O sistema deve registrar um log de auditoria das ações sensíveis (contratação de plano, vínculo/remoção de aluno, pagamento), incluindo quem realizou a ação e quando. | As ações sensíveis (contratação de plano, vínculo ou remoção de aluno e pagamento) geram registros de auditoria com quem executou a ação e quando. Isso dá rastreabilidade e apoia a resolução de disputas. | Média |

## Requisitos Não Funcionais (RNF)

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RNF01**: O sistema deve ser acessível via navegador web (Aplicação Web). | O IVIS é uma aplicação web, acessada pelo navegador, sem necessidade de instalar nada. Isso facilita a adoção por alunos e instituições. | Alta |
| **RNF02**: O sistema deve estar em conformidade com a LGPD, dado o tratamento de dados pessoais de alunos e instituições (relevante frente à ANPD como stakeholder). | Como o sistema trata dados pessoais de alunos e instituições, deve estar em conformidade com a LGPD. A ANPD, como stakeholder, reforça a importância de tratar esses dados com base legal, transparência e segurança. | Alta |
| **RNF03**: O tempo de resposta da IA durante a simulação de entrevista não deve comprometer a fluidez da conversa (definir SLA, ex.: resposta em até X segundos). | A IA precisa responder rápido o bastante para que a simulação pareça uma conversa natural. O tempo máximo de resposta (X segundos) ainda precisa ser definido como SLA. | Alta |
| **RNF04**: O sistema deve ser responsivo, funcionando em desktop e dispositivos móveis. | A interface é responsiva e funciona bem tanto em desktop quanto em dispositivos móveis, para que o aluno possa treinar de onde estiver. | Média |
| **RNF05**: O sistema deve garantir disponibilidade adequada para uso educacional (definir % de uptime esperado). | O sistema deve ter disponibilidade adequada ao uso educacional, sem indisponibilidades frequentes em horários de aula ou de treino. O percentual de uptime esperado ainda precisa ser definido. | Média |
| **RNF06**: O sistema não deve armazenar diretamente dados sensíveis de pagamento (ex.: número de cartão), delegando o processamento a um gateway certificado (PCI-DSS). | O IVIS não armazena diretamente dados sensíveis de pagamento, como número de cartão. O processamento fica com um gateway certificado PCI-DSS, o que reduz o risco e a responsabilidade do sistema. | Alta |
| **RNF07**: O sistema deve garantir que apenas a própria Instituição de Ensino visualize/gerencie os alunos vinculados ao seu plano (isolamento de dados entre instituições). | Cada Instituição de Ensino só visualiza e gerencia os alunos vinculados ao seu próprio plano. Há isolamento de dados entre instituições, o que impede acesso cruzado a informações. | Alta |
| **RNF08**: O sistema deve armazenar senhas de forma criptografada (hash) e encerrar a sessão do usuário automaticamente após um período de inatividade. | As senhas são armazenadas de forma criptografada (hash), nunca em texto puro. A sessão é encerrada automaticamente após um período de inatividade, o que protege contas em dispositivos compartilhados ou esquecidos abertos. | Alta |
| **RNF09**: Os registros de auditoria (RF20) devem ser mantidos por um prazo mínimo, a ser definido com a equipe jurídica, e protegidos contra alteração. | Os registros de auditoria (RF20) são mantidos por um prazo mínimo, a definir com a equipe jurídica, e protegidos contra alteração. Assim continuam confiáveis como evidência. | Média |
