# Casos de uso do IVIS

Este diretório reúne 15 casos de uso ligados aos requisitos funcionais do IVIS. Os fluxos foram revisados contra o frontend Vue, o backend Spring Boot, a API de IA e as migrations presentes no repositório. Cada CDU termina com um diagrama Mermaid das ações do ator.

## Parecer sobre a pertinência

Os 15 temas continuam fazendo sentido para o escopo: cobrem identidade e acesso, preparação para entrevistas, planos individuais e institucionais, controle de acesso e rastreabilidade. Cadastro e login foram consolidados em um único CDU, Acesso IVIS, pois ambos conduzem à entrada autenticada na plataforma. As especificações indicam os estados parciais para distinguir comportamento descrito do que está disponível no código.

Os códigos RF seguem a versão consolidada em [IVIS_Requisitos.md](../plano/IVIS_Requisitos.md). O estado de implementação não representa validação de produção nem aceite do requisito.

## Índice e rastreabilidade

| CDU | Caso de uso | RF | Situação observada | Documento |
|---|---|---|---|---|
| CDU-01 | Acesso IVIS | RF01 | Implementado | [CDU-01-acesso-ivis.md](CDU-01-acesso-ivis.md) |
| CDU-02 | Gerenciar dados da conta | RF02 | Parcial: dados básicos editáveis; senha tem endpoint, sem tela | [CDU-02-gerenciar-dados-da-conta.md](CDU-02-gerenciar-dados-da-conta.md) |
| CDU-03 | Gerenciar habilidades | RF03 | Implementado | [CDU-03-gerenciar-habilidades.md](CDU-03-gerenciar-habilidades.md) |
| CDU-04 | Recuperar acesso | RF04 | Parcial: solicitação implementada; falta tela de redefinição | [CDU-04-recuperar-acesso.md](CDU-04-recuperar-acesso.md) |
| CDU-05 | Consultar plano vigente | RF05 | Parcial: API consultável; falta tela dedicada | [CDU-05-consultar-plano-vigente.md](CDU-05-consultar-plano-vigente.md) |
| CDU-06 | Gerenciar vaga de interesse | RF06 | Implementado | [CDU-06-gerenciar-vaga.md](CDU-06-gerenciar-vaga.md) |
| CDU-07 | Realizar treinamento e receber feedback | RF07, RF08, RF09 | Implementado com provedor de IA configurável | [CDU-07-realizar-treinamento.md](CDU-07-realizar-treinamento.md) |
| CDU-08 | Consultar histórico de treinos | RF10 | Implementado para treinos concluídos | [CDU-08-consultar-historico.md](CDU-08-consultar-historico.md) |
| CDU-09 | Visualizar planos disponíveis | RF11, RF12 | Implementado e público | [CDU-09-visualizar-planos.md](CDU-09-visualizar-planos.md) |
| CDU-10 | Contratar plano individual | RF13, RF19 | Implementado no fluxo simulado | [CDU-10-contratar-plano-individual.md](CDU-10-contratar-plano-individual.md) |
| CDU-11 | Contratar plano institucional | RF14 | Parcial: oferta atual define capacidade fixa | [CDU-11-contratar-plano-institucional.md](CDU-11-contratar-plano-institucional.md) |
| CDU-12 | Efetuar pagamento | RF15 | Implementado no modo acadêmico simulado; sem gateway real | [CDU-12-efetuar-pagamento.md](CDU-12-efetuar-pagamento.md) |
| CDU-13 | Gerenciar alunos vinculados | RF16, RF18 | Implementado | [CDU-13-gerenciar-alunos-vinculados.md](CDU-13-gerenciar-alunos-vinculados.md) |
| CDU-14 | Aplicar acesso conforme modalidade | RF17 | Implementado nas rotas; APIs validam plano, perfil ou titularidade conforme operação | [CDU-14-aplicar-modalidade-de-acesso.md](CDU-14-aplicar-modalidade-de-acesso.md) |
| CDU-15 | Registrar auditoria de operações sensíveis | RF20 | Parcial: contratação simulada gera evento; cobertura geral pendente | [CDU-15-registrar-auditoria.md](CDU-15-registrar-auditoria.md) |

## Relações entre casos de uso

- CDU-01 cobre a criação de conta e a autenticação; a recuperação de senha iniciada no acesso segue CDU-04.
- CDU-07 usa habilidades do CDU-03 ou vaga do CDU-06, exige acesso conforme CDU-14 e registra resultado para consulta no CDU-08.
- CDU-09 é público. Para comprar, o visitante autentica ou cria conta pelo CDU-01 e continua pelos CDU-10 ou CDU-11.
- CDU-10 e CDU-11 incluem o pagamento simulado do CDU-12. A ativação ocorre somente quando o backend aprova a simulação.
- CDU-13 depende de contrato institucional com capacidade; os vínculos ativos dão acesso ao aluno segundo CDU-14.
- CDU-15 é transversal. Hoje a auditoria cobre contratação e pagamento simulados; vínculo e remoção ainda não geram eventos no código observado.

## Critérios usados na revisão

- Atores e etapas refletem telas, rotas, endpoints e regras encontrados no repositório.
- Alternativas representam erros e limites verificáveis, sem prometer telas ou integrações ausentes.
- A contratação acadêmica é descrita como simulação controlada pelo backend, sem cobrança real.
- Lacunas de interface e de regra são indicadas como situação atual, sem retirar do escopo requisitos ainda válidos.
