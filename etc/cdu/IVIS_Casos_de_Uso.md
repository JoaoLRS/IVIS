# Casos de uso do IVIS

Este diretório contém a especificação detalhada dos casos de uso e seu vínculo com os RF. Os códigos RF são os da versão consolidada em [`IVIS_Requisitos.md`](../plano/IVIS_Requisitos.md); o PDF `rf.pdf` foi usado como fonte complementar para RF01, RF02, RF04, RF05 e RF11–RF20. RF03 e RF06–RF10 não constam no PDF, mas permanecem no escopo por estarem documentados nos requisitos do projeto e, em parte, já iniciados no frontend.

## Situação observada no sistema

O repositório contém um frontend demonstrativo Vue, sem backend. Habilidades (RF03), vagas (RF06), simulação local (RF07), feedback demonstrativo (RF08) e histórico mantido em memória (RF09/RF10) têm telas ou lógica iniciada. Isso não comprova persistência em servidor nem integração com IA. Cadastro/login, recuperação de senha, plano vigente, planos públicos, contratação, pagamento, gestão institucional, autorização por modalidade e auditoria ainda não estão implementados no código inspecionado. Veja [`LEIA-ME.md`](../../frontend/LEIA-ME.md) para o escopo declarado do frontend.

Os fluxos abaixo especificam o comportamento esperado. Regras marcadas como pendentes precisam de decisão do produto, em particular confirmação de e-mail, expiração de recuperação, ciclo de vida/validade de planos e reatribuição de vagas após remoção de aluno.

## Índice e rastreabilidade

| Caso de uso | RF cobertos | Documento |
|---|---|---|
| UC01 — Criar conta | RF01 | [UC01_criar-conta.md](UC01_criar-conta.md) |
| UC02 — Fazer login | RF01 | [UC02_fazer-login.md](UC02_fazer-login.md) |
| UC03 — Gerenciar dados da conta | RF02 | [UC03_gerenciar-dados-da-conta.md](UC03_gerenciar-dados-da-conta.md) |
| UC04 — Gerenciar habilidades | RF03 (fora do PDF) | [UC04_gerenciar-habilidades.md](UC04_gerenciar-habilidades.md) |
| UC05 — Recuperar acesso | RF04 | [UC05_recuperar-acesso.md](UC05_recuperar-acesso.md) |
| UC06 — Consultar plano vigente | RF05 | [UC06_consultar-plano-vigente.md](UC06_consultar-plano-vigente.md) |
| UC07 — Gerenciar vaga de interesse | RF06 (fora do PDF) | [UC07_gerenciar-vaga.md](UC07_gerenciar-vaga.md) |
| UC08 — Realizar treinamento e receber feedback | RF07, RF08, RF09 (fora do PDF) | [UC08_realizar-treinamento.md](UC08_realizar-treinamento.md) |
| UC09 — Consultar histórico de treinos | RF10 (fora do PDF) | [UC09_consultar-historico.md](UC09_consultar-historico.md) |
| UC10 — Visualizar planos | RF11, RF12 | [UC10_visualizar-planos.md](UC10_visualizar-planos.md) |
| UC11 — Contratar plano individual | RF13, RF19 | [UC11_contratar-plano-individual.md](UC11_contratar-plano-individual.md) |
| UC12 — Contratar plano institucional | RF14 | [UC12_contratar-plano-institucional.md](UC12_contratar-plano-institucional.md) |
| UC13 — Efetuar pagamento | RF15 | [UC13_efetuar-pagamento.md](UC13_efetuar-pagamento.md) |
| UC14 — Gerenciar alunos e vagas vinculadas | RF16, RF18 | [UC14_gerenciar-alunos-vinculados.md](UC14_gerenciar-alunos-vinculados.md) |
| UC15 — Aplicar acesso conforme modalidade | RF17 | [UC15_aplicar-modalidade-de-acesso.md](UC15_aplicar-modalidade-de-acesso.md) |
| UC16 — Registrar ações de auditoria | RF20 | [UC16_registrar-auditoria.md](UC16_registrar-auditoria.md) |

## Relações entre casos de uso

- UC02 e UC03 exigem conta; UC05 pode ser iniciado pelo fluxo de login.
- UC08 inclui a persistência do resultado (RF09); UC09 consulta esses registros. A geração de feedback (RF08) acontece ao concluir a entrevista.
- UC10 é público (RF12); para contratar, o visitante precisa autenticar-se e seguir UC11 ou UC12.
- UC11 e UC12 incluem UC13. A ativação só acontece após confirmação de pagamento aprovado pelo gateway.
- UC14 permite vincular aluno somente dentro da capacidade contratada; os totais de RF18 refletem vínculos ativos.
- UC15 é uma regra transversal de autorização para recursos restritos. UC16 é incluído nas operações sensíveis indicadas no RF20.

## Convenções

**Ator principal** inicia o objetivo. **Atores secundários** são serviços externos ou outros participantes. **Pré-condições** devem ser verdadeiras antes do fluxo. Fluxos alternativos indicam erros, desvios ou decisões. O estado da implementação é informativo e se refere ao código encontrado, não representa aceite do requisito.
