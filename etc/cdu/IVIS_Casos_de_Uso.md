# Casos de uso do IVIS

Este diretório reúne 20 casos de uso: 16 especificações funcionais originais, duas jornadas ponta a ponta e dois fluxos complementares. Os fluxos foram revisados contra o frontend Vue, o backend Spring Boot, a API de IA e as migrations presentes no repositório. Os CDU-17 e CDU-18 documentam os dois caminhos felizes automatizados em Cypress; os CDU-19 e CDU-20 detalham operações reais ainda descritas apenas dentro dos CDU-07 e CDU-13. Cada CDU termina com um diagrama Mermaid das ações dos atores.

## Parecer sobre a pertinência

Os 16 temas originais cobrem identidade e acesso, política de senha, preparação para entrevistas, planos individuais e institucionais, controle de acesso e rastreabilidade. Cadastro e login estão no CDU-01; a validação visual e a política de nova senha têm fluxo próprio no CDU-16, reutilizado por cadastro, alteração, recuperação e cadastro institucional de aluno. Os CDU-17 e CDU-18 encadeiam essas operações nas jornadas do aluno pagante e da instituição com aluno vinculado. A análise das telas e endpoints mostrou dois objetivos de usuário sem especificação própria: retomar um treino persistido e vincular uma conta de aluno já existente. Os CDU-19 e CDU-20 detalham esses fluxos sem acrescentar RF. As especificações indicam os estados parciais para distinguir comportamento descrito do que está disponível no código.

Os códigos RF seguem a versão consolidada em [IVIS_Requisitos.md](../plano/IVIS_Requisitos.md). O estado de implementação não representa validação de produção nem aceite do requisito.

## Índice e rastreabilidade

| CDU | Caso de uso | RF | Situação observada | Documento | Cobertura principal |
|---|---|---|---|---|---|
| CDU-01 | Acesso IVIS | RF01 | Implementado | [CDU-01-acesso-ivis.md](CDU-01-acesso-ivis.md) | [CDU-01-acesso-ivis.cy.js](../../cypress/e2e/cdu/CDU-01-acesso-ivis.cy.js) |
| CDU-02 | Gerenciar dados da conta | RF02 | Dados básicos e troca de senha implementados; troca em página própria | [CDU-02-gerenciar-dados-da-conta.md](CDU-02-gerenciar-dados-da-conta.md) | [CDU-02-gerenciar-dados-da-conta.cy.js](../../cypress/e2e/cdu/CDU-02-gerenciar-dados-da-conta.cy.js) |
| CDU-03 | Gerenciar habilidades | RF03 | Implementado | [CDU-03-gerenciar-habilidades.md](CDU-03-gerenciar-habilidades.md) | [CDU-03-gerenciar-habilidades.cy.js](../../cypress/e2e/cdu/CDU-03-gerenciar-habilidades.cy.js) |
| CDU-04 | Recuperar acesso | RF04 | Solicitação e tela de redefinição implementadas | [CDU-04-recuperar-acesso.md](CDU-04-recuperar-acesso.md) | [CDU-04-recuperar-acesso.cy.js](../../cypress/e2e/cdu/CDU-04-recuperar-acesso.cy.js) |
| CDU-05 | Consultar plano vigente | RF05 | API e página Meu plano implementadas | [CDU-05-consultar-plano-vigente.md](CDU-05-consultar-plano-vigente.md) | [CDU-05-consultar-plano-vigente.cy.js](../../cypress/e2e/cdu/CDU-05-consultar-plano-vigente.cy.js) |
| CDU-06 | Gerenciar vaga de interesse | RF06 | Implementado | [CDU-06-gerenciar-vaga.md](CDU-06-gerenciar-vaga.md) | [CDU-06-gerenciar-vaga.cy.js](../../cypress/e2e/cdu/CDU-06-gerenciar-vaga.cy.js) |
| CDU-07 | Realizar treinamento e receber feedback | RF07, RF08, RF09 | Sessão persistida e retomável; avaliação na conclusão | [CDU-07-realizar-treinamento.md](CDU-07-realizar-treinamento.md) | [CDU-07-realizar-treinamento.cy.js](../../cypress/e2e/cdu/CDU-07-realizar-treinamento.cy.js) |
| CDU-08 | Consultar histórico de treinos | RF10 | Implementado para treinos concluídos | [CDU-08-consultar-historico.md](CDU-08-consultar-historico.md) | [CDU-08-consultar-historico.cy.js](../../cypress/e2e/cdu/CDU-08-consultar-historico.cy.js) |
| CDU-09 | Visualizar planos disponíveis | RF11, RF12 | Implementado e público | [CDU-09-visualizar-planos.md](CDU-09-visualizar-planos.md) | [CDU-09-visualizar-planos.cy.js](../../cypress/e2e/cdu/CDU-09-visualizar-planos.cy.js) |
| CDU-10 | Contratar plano individual | RF13, RF19 | Implementado no fluxo simulado | [CDU-10-contratar-plano-individual.md](CDU-10-contratar-plano-individual.md) | [CDU-10-contratar-plano-individual.cy.js](../../cypress/e2e/cdu/CDU-10-contratar-plano-individual.cy.js) |
| CDU-11 | Contratar plano institucional | RF14 | Parcial: oferta atual define capacidade fixa | [CDU-11-contratar-plano-institucional.md](CDU-11-contratar-plano-institucional.md) | [CDU-11-contratar-plano-institucional.cy.js](../../cypress/e2e/cdu/CDU-11-contratar-plano-institucional.cy.js) |
| CDU-12 | Efetuar pagamento | RF15 | Implementado no modo acadêmico simulado; sem gateway real | [CDU-12-efetuar-pagamento.md](CDU-12-efetuar-pagamento.md) | [CDU-12-efetuar-pagamento.cy.js](../../cypress/e2e/cdu/CDU-12-efetuar-pagamento.cy.js) |
| CDU-13 | Gerenciar alunos vinculados | RF16, RF18 | Implementado | [CDU-13-gerenciar-alunos-vinculados.md](CDU-13-gerenciar-alunos-vinculados.md) | [CDU-13-gerenciar-alunos-vinculados.cy.js](../../cypress/e2e/cdu/CDU-13-gerenciar-alunos-vinculados.cy.js) |
| CDU-14 | Aplicar acesso conforme modalidade | RF17 | Implementado nas rotas; APIs validam plano, perfil ou titularidade conforme operação | [CDU-14-aplicar-modalidade-de-acesso.md](CDU-14-aplicar-modalidade-de-acesso.md) | [CDU-14-aplicar-modalidade-de-acesso.cy.js](../../cypress/e2e/cdu/CDU-14-aplicar-modalidade-de-acesso.cy.js) |
| CDU-15 | Registrar auditoria de operações sensíveis | RF20 | Contratação, pagamento e vínculos auditados; retenção pendente | [CDU-15-registrar-auditoria.md](CDU-15-registrar-auditoria.md) | [CDU-15-registrar-auditoria.cy.js](../../cypress/e2e/cdu/CDU-15-registrar-auditoria.cy.js) |
| CDU-16 | Validar requisitos de senha | RF01, RF02, RF04, RF16 | Implementado nas quatro entradas de nova senha | [CDU-16-validar-requisitos-senha.md](CDU-16-validar-requisitos-senha.md) | [CDU-16-validar-requisitos-senha.cy.js](../../cypress/e2e/cdu/CDU-16-validar-requisitos-senha.cy.js) |
| CDU-17 | Jornada feliz do aluno pagante | RF01–RF03, RF06–RF11, RF13, RF15, RF17 | Jornada real de cadastro, contratação simulada, perfil, treino com IA e histórico | [CDU-17-jornada-feliz-do-aluno.md](CDU-17-jornada-feliz-do-aluno.md) | [perfil_aluno.cy.js](../../cypress/e2e/caminho_feliz/perfil_aluno.cy.js) |
| CDU-18 | Jornada feliz da instituição e do aluno vinculado | RF01, RF11, RF14–RF18 | Jornada real de contratação simulada, vínculo, acesso do aluno e liberação de vaga; capacidade fixa | [CDU-18-jornada-feliz-da-instituicao.md](CDU-18-jornada-feliz-da-instituicao.md) | [perfil_instituicao.cy.js](../../cypress/e2e/caminho_feliz/perfil_instituicao.cy.js) |
| CDU-19 | Retomar treinamento em andamento | RF07–RF10 | Implementado; retomada após recarga coberta por teste de interface com API simulada e por teste de integração do backend | [CDU-19-retomar-treinamento.md](CDU-19-retomar-treinamento.md) | [CDU-07-realizar-treinamento.cy.js](../../cypress/e2e/cdu/CDU-07-realizar-treinamento.cy.js) |
| CDU-20 | Vincular aluno com conta existente | RF16–RF18, RF20 | Implementado; vínculo por e-mail coberto por teste de interface com API simulada e testes do serviço | [CDU-20-vincular-aluno-existente.md](CDU-20-vincular-aluno-existente.md) | [CDU-13-gerenciar-alunos-vinculados.cy.js](../../cypress/e2e/cdu/CDU-13-gerenciar-alunos-vinculados.cy.js) |


## Relações entre casos de uso

- CDU-01 cobre a criação de conta e a autenticação; a recuperação de senha iniciada no acesso segue CDU-04. A política de nova senha segue CDU-16.
- CDU-07 usa habilidades do CDU-03 ou vaga do CDU-06, exige acesso conforme CDU-14 e registra resultado para consulta no CDU-08.
- CDU-09 é público. Para comprar, o visitante autentica ou cria conta pelo CDU-01 e continua pelos CDU-10 ou CDU-11.
- CDU-10 e CDU-11 incluem o pagamento simulado do CDU-12. A ativação ocorre somente quando o backend aprova a simulação.
- CDU-13 depende de contrato institucional com capacidade; os vínculos ativos dão acesso ao aluno segundo CDU-14.
- CDU-15 é transversal. A auditoria cobre contratação e pagamento simulados e criação/remoção de vínculos institucionais.
- CDU-16 é transversal aos CDUs 01, 02, 04 e 13. Ele especifica a indicação visual e a validação da nova senha, sem duplicar as operações de conta desses casos.
- CDU-17 encadeia cadastro, contratação individual, edição de conta, cadastro de habilidade e vaga, treino por vaga e consulta do feedback pelo histórico.
- CDU-18 encadeia cadastro e contratação institucional, gestão de vagas e vínculo, acesso do aluno e remoção do vínculo.
- CDU-19 detalha a retomada do treino em andamento prevista no CDU-07. Não há necessidade de iniciar outro treino para recuperar perguntas e respostas já registradas.
- CDU-20 detalha o vínculo por e-mail de uma conta existente previsto no CDU-13. O cadastro de um novo aluno permanece no CDU-13 e na jornada CDU-18.

## Critérios usados na revisão

- Atores e etapas refletem telas, rotas, endpoints e regras encontrados no repositório.
- Alternativas representam erros e limites verificáveis, sem prometer telas ou integrações ausentes.
- A contratação acadêmica é descrita como simulação controlada pelo backend, sem cobrança real.
- Lacunas de interface e de regra são indicadas como situação atual, sem retirar do escopo requisitos ainda válidos.

## Limites da automação de interface

Os specs CDU-01 a CDU-14 e CDU-16 simulam as respostas da API para exercitar as telas, validar mensagens, rotas, indicadores e payloads enviados. Eles não comprovam persistência, autorização no backend, auditoria nem consistência transacional. Essas regras são verificadas nos testes Java/Python e, quando dependem da integração completa, nas jornadas `caminho_feliz` e no CDU-15, que usam o mesmo banco `ivis` do Compose, com limpeza restrita às contas das fixtures e seus dados vinculados. A troca de senha é verificada no CDU-02 como fluxo separado de navegação, incluindo os campos visíveis e o payload; o CDU-16 verifica os indicadores visuais. Persistência e revogação de sessões pertencem aos testes do backend.

A API técnica de limpeza de dados não é um caso de uso do usuário final. Sua configuração e restrições estão no [guia do backend](../../backend/LEIA-ME.md); os testes de integração verificam autorização, isolamento por conta e ausência em produção.
