# IVIS: Requisitos Funcionais e Não Funcionais

O quadro de planejamento aprovado contém **20 RF e 4 RNF**. Os RF03 e RF06–RF10 têm implementação, mas permanecem em validação até o aceite formal; os demais 14 RF estão na lista de requisitos. Os quatro RNF selecionados são RNF01, RNF03, RNF07 e RNF09.

| Situação no quadro | Códigos RF |
|---|---|
| Lista de requisitos (14) | RF01, RF02, RF04, RF05, RF11–RF20 |
| Implementados, em validação (6) | RF03, RF06–RF10 |

## Requisitos Funcionais (RF)

### Acesso e Perfil

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF01**: O sistema deve permitir que Aluno e Instituição de Ensino criem conta e façam login. | Alunos e Instituições de Ensino precisam de uma identidade própria na plataforma. Por isso o sistema oferece cadastro e login para os dois perfis, o que garante acesso individualizado e é a base para todas as demais funcionalidades. | Alta |
| **RF02**: O sistema deve permitir que Aluno e Instituição de Ensino gerenciem os dados da própria conta (dados pessoais/institucionais, senha). | Depois de criada a conta, o usuário deve poder mantê-la atualizada. O sistema permite editar dados pessoais ou institucionais e alterar a senha, sem depender de suporte. | Média |
| **RF03**: O sistema deve permitir que o Aluno gerencie seu perfil de habilidades técnicas. | O Aluno pode montar e atualizar seu perfil de habilidades técnicas, informando em que áreas atua ou quer se desenvolver. Esse perfil serve de referência para direcionar os treinos. | Alta |
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
| **RF13**: O sistema deve permitir que um aluno elegível contrate um plano individual diretamente pela plataforma. | Um aluno sem vínculo institucional ativo e sem contrato individual vigente pode contratar um plano individual de forma autônoma, sem intermediação de instituição. | Alta |
| **RF14**: O sistema deve permitir que a Instituição de Ensino escolha e contrate um dos três planos institucionais: até 50 alunos por R$ 900, até 100 alunos por R$ 1.750 ou alunos ilimitados por R$ 5.000, por 30 dias no simulador acadêmico. | A instituição define sua capacidade ao selecionar uma das três ofertas. Os preços são ilustrativos e nenhuma cobrança real ocorre. Não há campo para uma quantidade arbitrária nem troca de plano durante um contrato vigente. | Alta |
| **RF15**: O sistema deve processar a contratação por pagamento simulado no modo acadêmico. | O backend valida cartões fictícios, registra aprovação ou recusa simulada e ativa o contrato somente quando aprovada. Não ocorre cobrança real. Gateway externo é evolução futura, fora do aceite deste modo. | Alta |
| **RF16**: O sistema deve permitir que a Instituição de Ensino visualize, cadastre, vincule e remova alunos conforme a capacidade do plano contratado. | Os planos de 50 e 100 alunos impedem vínculos acima do limite; o plano ilimitado permite novos vínculos sem teto numérico. Remover um vínculo preserva seu histórico. | Alta |
| **RF17**: O sistema deve aplicar o acesso do aluno conforme contrato individual ou vínculo institucional vigente. | O backend determina a origem do acesso a partir do contrato próprio ou do vínculo com contrato institucional, liberando as funções cobertas pela modalidade. | Alta |
| **RF18**: O sistema deve permitir que a Instituição de Ensino visualize alunos vinculados e vagas disponíveis do plano. | Nos planos de 50 e 100 alunos, a área institucional mostra o total, o uso e as vagas restantes. No plano ilimitado, mostra o uso e indica que a capacidade é ilimitada. | Média |
| **RF19**: O sistema deve impedir a contratação de plano individual enquanto o aluno tiver vínculo institucional ativo. | A elegibilidade para o RF13 é verificada pelo backend. Um aluno com vínculo institucional ativo não pode contratar plano individual próprio até o encerramento do vínculo. | Alta |

### Auditoria

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RF20**: O sistema deve registrar um log de auditoria das ações sensíveis (contratação de plano, vínculo/remoção de aluno, pagamento), incluindo quem realizou a ação e quando. | As ações sensíveis (contratação de plano, vínculo ou remoção de aluno e pagamento) geram registros de auditoria com quem executou a ação e quando. Isso dá rastreabilidade e apoia a resolução de disputas. | Média |

> **Modo de apresentação acadêmica (RF15):** a instância publicada para apresentação deve oferecer um simulador de pagamento controlado pelo backend, com cartão de teste fictício padrão IVIS (`4242 4242 4242 4242`, validade `12/30`, CVV `123`). A compra simulada não gera cobrança nem encaminha os dados a um gateway real. O backend não persiste PAN/CVV e é quem valida e registra o resultado como simulado. Uma futura cobrança real exigirá gateway externo e configuração própria; não pode ocorrer por fallback do simulador.

## Requisitos Não Funcionais (RNF)

| Requisito | Minitexto | Prioridade |
|---|---|---|
| **RNF01**: O sistema deve ser acessível via navegador web (Aplicação Web). | O IVIS é uma aplicação web, acessada pelo navegador, sem necessidade de instalar nada. Isso facilita a adoção por alunos e instituições. | Alta |
| **RNF03**: A geração da próxima pergunta e da avaliação final pela IA deve responder em até 10 segundos no percentil 95, cada operação medida separadamente. | Medir de ponta a ponta com o provedor real e entradas representativas. A meta está definida; sua conformidade depende de medição registrada. O timeout de segurança não comprova o percentil 95. | Alta |
| **RNF07**: O sistema deve garantir que apenas a própria Instituição de Ensino visualize/gerencie os alunos vinculados ao seu plano (isolamento de dados entre instituições). | Cada Instituição de Ensino só visualiza e gerencia os alunos vinculados ao seu próprio plano. Há isolamento de dados entre instituições, o que impede acesso cruzado a informações. | Alta |
| **RNF09**: Os registros de auditoria (RF20) devem ser mantidos por no mínimo 4 anos desde o evento e protegidos contra alteração. | A proteção contra UPDATE e DELETE comum já existe. Backup, arquivamento e descarte após o prazo exigem procedimento operacional; não há exclusão automática em produção. | Média |

> **Total aprovado:** 24 requisitos — 20 RF (14 na lista e 6 implementados em validação) e 4 RNF. Outros atributos de segurança, privacidade e usabilidade podem orientar o projeto, mas não são RNF aprovados nesta lista.

## Prioridade e validação dos requisitos implementados

O RF03 passou de Média para **Alta**, pois o perfil de habilidades é o contexto mais simples para iniciar um treino (UC08, A2). A sequência planejada foi RF01 (somente Aluno, como dependência), RF03, RF06, RF07 e RF08, RF09 e RF10. O aceite de cada item depende dos resultados registrados no plano de testes.

**Carregamento da interface:** enquanto uma navegação ou operação visível aguarda conclusão, a aplicação apresenta indicador de carregamento, mostra o cursor de espera e impede novos cliques e acionamentos por teclado. Chamadas de manutenção da sessão em segundo plano não bloqueiam a interface.
