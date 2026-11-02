# Casos de uso do IVIS

Este diretório reúne 16 casos de uso ligados aos requisitos funcionais do IVIS. Os fluxos foram revisados contra o frontend Vue, o backend Spring Boot, a API de IA e as migrations presentes no repositório. Cada CDU termina com um diagrama Mermaid das ações do ator.

## Parecer sobre a pertinência

Os 16 temas continuam fazendo sentido para o escopo: cobrem identidade, preparação para entrevistas, planos individuais e institucionais, controle de acesso e rastreabilidade. Alguns descrevem funções parciais ou regras cuja interface ainda não existe. Cada especificação informa o estado atual para distinguir comportamento descrito do que está disponível no código.

Os códigos RF seguem a versão consolidada em [IVIS_Requisitos.md](../plano/IVIS_Requisitos.md). O estado de implementação não representa validação de produção nem aceite do requisito.

## Índice e rastreabilidade

| CDU | Caso de uso | RF | Situação observada | Documento |
|---|---|---|---|---|
| CDU-01 | Criar conta | RF01 | Implementado | [CDU-01-criar-conta.md](CDU-01-criar-conta.md) |
| CDU-02 | Fazer login | RF01 | Implementado | [CDU-02-fazer-login.md](CDU-02-fazer-login.md) |
| CDU-03 | Gerenciar dados da conta | RF02 | Parcial: dados básicos editáveis; senha tem endpoint, sem tela | [CDU-03-gerenciar-dados-da-conta.md](CDU-03-gerenciar-dados-da-conta.md) |
| CDU-04 | Gerenciar habilidades | RF03 | Implementado | [CDU-04-gerenciar-habilidades.md](CDU-04-gerenciar-habilidades.md) |
| CDU-05 | Recuperar acesso | RF04 | Parcial: solicitação implementada; falta tela de redefinição | [CDU-05-recuperar-acesso.md](CDU-05-recuperar-acesso.md) |
| CDU-06 | Consultar plano vigente | RF05 | Parcial: API consultável; falta tela dedicada | [CDU-06-consultar-plano-vigente.md](CDU-06-consultar-plano-vigente.md) |
| CDU-07 | Gerenciar vaga de interesse | RF06 | Implementado | [CDU-07-gerenciar-vaga.md](CDU-07-gerenciar-vaga.md) |
| CDU-08 | Realizar treinamento e receber feedback | RF07, RF08, RF09 | Implementado com provedor de IA configurável | [CDU-08-realizar-treinamento.md](CDU-08-realizar-treinamento.md) |
| CDU-09 | Consultar histórico de treinos | RF10 | Implementado para treinos concluídos | [CDU-09-consultar-historico.md](CDU-09-consultar-historico.md) |
| CDU-10 | Visualizar planos disponíveis | RF11, RF12 | Implementado e público | [CDU-10-visualizar-planos.md](CDU-10-visualizar-planos.md) |
| CDU-11 | Contratar plano individual | RF13, RF19 | Implementado no fluxo simulado | [CDU-11-contratar-plano-individual.md](CDU-11-contratar-plano-individual.md) |
| CDU-12 | Contratar plano institucional | RF14 | Parcial: oferta atual define capacidade fixa | [CDU-12-contratar-plano-institucional.md](CDU-12-contratar-plano-institucional.md) |
| CDU-13 | Efetuar pagamento | RF15 | Implementado no modo acadêmico simulado; sem gateway real | [CDU-13-efetuar-pagamento.md](CDU-13-efetuar-pagamento.md) |
| CDU-14 | Gerenciar alunos vinculados | RF16, RF18 | Implementado | [CDU-14-gerenciar-alunos-vinculados.md](CDU-14-gerenciar-alunos-vinculados.md) |
| CDU-15 | Aplicar acesso conforme modalidade | RF17 | Implementado nas rotas e APIs protegidas | [CDU-15-aplicar-modalidade-de-acesso.md](CDU-15-aplicar-modalidade-de-acesso.md) |
| CDU-16 | Registrar auditoria de operações sensíveis | RF20 | Parcial: contratação simulada gera evento; cobertura geral pendente | [CDU-16-registrar-auditoria.md](CDU-16-registrar-auditoria.md) |

## Relações entre casos de uso

- CDU-02 depende de conta criada no CDU-01; a recuperação iniciada no login segue CDU-05.
- CDU-08 usa habilidades do CDU-04 ou vaga do CDU-07, exige acesso conforme CDU-15 e registra resultado para consulta no CDU-09.
- CDU-10 é público. Para comprar, o visitante autentica ou cria conta e continua pelos CDU-11 ou CDU-12.
- CDU-11 e CDU-12 incluem o pagamento simulado do CDU-13. A ativação ocorre somente quando o backend aprova a simulação.
- CDU-14 depende de contrato institucional com capacidade; os vínculos ativos dão acesso ao aluno segundo CDU-15.
- CDU-16 é transversal. Hoje a auditoria cobre contratação e pagamento simulados; vínculo e remoção ainda não geram eventos no código observado.

## Critérios usados na revisão

- Atores e etapas refletem telas, rotas, endpoints e regras encontrados no repositório.
- Alternativas representam erros e limites verificáveis, sem prometer telas ou integrações ausentes.
- A contratação acadêmica é descrita como simulação controlada pelo backend, sem cobrança real.
- Lacunas de interface e de regra são indicadas como situação atual, sem retirar do escopo requisitos ainda válidos.
