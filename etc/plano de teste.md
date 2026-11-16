# Plano de teste do IVIS

**Ciclo:** 2026-11, de 02/11/2026 a 06/11/2026  
**Versão deste plano:** 0.3, revisão técnica de 11/11/2026
**Situação:** em revisão; aceite do ciclo ainda não registrado  
**Código verificado na revisão atual:** árvore local de 11/11/2026, ainda sem commit de referência; registrar o commit antes da execução formal.
**Revisão exploratória anterior:** commit `a0635f9`, ambiente Docker Compose local, em 09/11/2026; os achados estão no relatório externo de revisão. Esta referência não representa aprovação do ciclo nem reteste visual de todas as correções.
**Base:** plano de teste em PDF fornecido pela equipe (versão 0.1, de 28/10/2026), [requisitos](plano/IVIS_Requisitos.md), [casos de uso](cdu/IVIS_Casos_de_Uso.md) e código do repositório. Antes de executar um ciclo formal, registrar commit, ambiente, executor e data.

## 1. Objetivo e limites

Verificar os requisitos implementados do IVIS com cenários válidos, inválidos, de autorização, persistência e isolamento. Um teste unitário ou um Cypress com API simulada comprova apenas o comportamento da camada exercitada. A aprovação de um requisito exige evidência da camada responsável pela regra e, quando necessário, da jornada integrada.

O escopo inclui os 20 requisitos funcionais RF01–RF20 e os quatro não funcionais aprovados RNF01, RNF03, RNF07 e RNF09. O [CDU-16](cdu/CDU-16-validar-requisitos-senha.md) especifica a política transversal de senha. A limpeza de dados restrita a desenvolvimento/homologação é uma verificação técnica, fora dos casos de uso do usuário final.

Este plano **não declara todos os RFs aprovados**. O RF14 agora oferece três capacidades institucionais predefinidas (50, 100 e ilimitada) no simulador; a troca de plano durante um contrato ativo ainda não existe. Um gateway de pagamento real permanece fora do modo acadêmico. As metas de RNF03 (p95 até 10 s para pergunta e avaliação, separadamente) e RNF09 (guarda mínima de 4 anos) estão definidas, mas faltam, respectivamente, medição com provedor real e política operacional de backup, arquivamento e descarte. A qualidade semântica de perguntas e feedback da NVIDIA requer avaliação própria.

## 2. Ambiente, dados e segurança

| Camada | Execução e referência |
| --- | --- |
| Frontend | Vue 3/Vite em `http://localhost:5173` no desenvolvimento ou `http://localhost:8080` no Compose. |
| Backend | Java 25/Spring Boot em `http://localhost:8081/api`; prontidão em `/v1/pronto`. Testes de integração usam PostgreSQL temporário via Testcontainers. |
| API de IA | Python 3.12/3.13 e FastAPI; prontidão em `http://localhost:8000/pronto`. Testes pytest simulam o transporte NVIDIA; jornadas reais exigem `NVIDIA_API_KEY`. |
| Banco do Compose e Cypress | PostgreSQL 17, banco `ivis`, porta local `5433` por padrão. As jornadas reais usam o serviço `ivis-banco`; a limpeza é limitada às contas das fixtures e seus dados vinculados. |
| E-mail | Mailpit em `http://localhost:8025`; nenhuma entrega externa no ambiente local. |
| Cypress | Configuração em [`cypress/e2e/cypress.config.ts`](../cypress/e2e/cypress.config.ts); vídeos em `cypress/downloads` e capturas de falha em `cypress/screenshots`. |

Usar contas, CPF/CNPJ, respostas e cartões **fictícios**. Não anexar `.env`, senhas, tokens, cookies, chaves, números de cartão reais, dumps ou logs com dados pessoais às evidências. Para isolamento, preparar pelo menos dois alunos e duas instituições com dados distintos. Registrar IDs fictícios e o procedimento de limpeza. A [preparação do Cypress](../cypress/e2e/README.md) deve ser seguida antes das jornadas reais; os e-mails das fixtures ficam reservados para testes e a limpeza remove somente essas contas e seus dados vinculados no banco `ivis`.

## 3. Estratégia e níveis

| Nível | O que comprova | Implementação atual |
| --- | --- | --- |
| Unitário frontend | Regras de senha, estado da sessão/treino, guarda de rotas e decisões das páginas. | [`frontend/tests/unit`](../frontend/tests/unit/), Vitest. |
| Unitário e integração backend | Regras de identidade, planos, vínculos, sessão, treino, auditoria, migrations e autorização com PostgreSQL. | [`backend/src/test/java`](../backend/src/test/java/), JUnit/Spring Test/Testcontainers. |
| API de IA | Contratos FastAPI, autenticação de serviço, adaptação NVIDIA, timeout e falhas. | [`api-ia/tests`](../api-ia/tests/), pytest e transporte HTTP simulado. |
| Interface com API simulada | Formulários, navegação, mensagens e payloads por caso de uso. | [`cypress/e2e/cdu`](../cypress/e2e/cdu/), CDU-01 a CDU-14 e CDU-16. |
| Ponta a ponta real | Persistência e integração entre UI, backend, banco e API de IA; auditoria do CDU-15. | [`cypress/e2e/caminho_feliz`](../cypress/e2e/caminho_feliz/) e [CDU-15](../cypress/e2e/cdu/CDU-15-registrar-auditoria.cy.js), com o banco `ivis` do Compose. |
| Exploratória e não funcional | Responsividade, teclado, clareza, latência real e qualidade das respostas de IA. | Registrar roteiro, medições, avaliadores e resultados separadamente. O [script de medição](../api-ia/scripts/medir_desempenho.py) mede latência, não qualidade semântica. |

Prioridade **alta** para autenticação, titularidade/isolamento, contratação, capacidade institucional, persistência de treino e auditoria. Prioridade **média** para CRUD de perfil, plano, histórico e apresentação de feedback. Ajustes visuais sem impacto em regra são revisados conforme a alteração.

## 4. Rastreabilidade dos requisitos

Esta tabela aponta a evidência automatizada principal. A presença de um teste indica **cobertura localizada**, não aceite formal do requisito.

| Requisito | Foco | Evidência principal e situação |
| --- | --- | --- |
| RF01 | Cadastro e login | CDU-01; `IdentidadeServiceTest` e `FluxosAplicacaoIntegracaoTest`; sessão/roteador no frontend. Cobertura automatizada. |
| RF02 | Dados da conta e senha | CDU-02/16; `IdentidadeServiceTest`, `PoliticaSenhaTest` e testes das páginas Conta/AlterarSenha. Cobertura automatizada. |
| RF03 | Habilidades | CDU-03; `PerfilAlunoServiceTest` e página Habilidades. Cobertura automatizada. |
| RF04 | Recuperação | CDU-04/16; `RecuperacaoSenhaServiceTest` e páginas Login/RedefinirSenha. Cobertura automatizada; entrega externa de e-mail fora do escopo local. |
| RF05 | Plano vigente | CDU-05; `ConsultaPlanosIntegracaoTest` e página MeuPlano. Cobertura automatizada. |
| RF06 | Vagas de interesse | CDU-06; `PerfilAlunoServiceTest` e página Vagas. Cobertura automatizada. |
| RF07 | Entrevista com IA | CDU-07; `FluxosAplicacaoIntegracaoTest`, estado/tela Treinos e `api-ia/tests`. Contrato e persistência cobertos; qualidade real da IA pendente. |
| RF08 | Avaliação | CDU-07; conclusão do treino no backend e contrato da API de IA. Qualidade do feedback real pendente. |
| RF09 | Persistência do treino | CDU-07; `FluxosAplicacaoIntegracaoTest` e jornada real do aluno. Integração automatizada no backend. |
| RF10 | Consulta do histórico | CDU-08; `FluxosAplicacaoIntegracaoTest` e spec CDU-08. Isolamento coberto no backend. |
| RF11–RF12 | Catálogo público | CDU-09; `ConsultaPlanosIntegracaoTest` e spec CDU-09. Cobertura automatizada. |
| RF13 | Contratação individual | CDU-10/12; `FluxosAplicacaoIntegracaoTest` e checkout. Cobertura do simulador acadêmico. |
| RF14 | Contratação institucional | CDU-11/12; três ofertas selecionáveis de 50, 100 e alunos ilimitados com pagamento simulado. Não há quantidade arbitrária nem troca de plano ativo. |
| RF15 | Pagamento | CDU-12; aprovação/recusa, idempotência e auditoria do simulador cobertas. Gateway real não implementado neste modo. |
| RF16 | Gerenciar alunos vinculados | CDU-13; `VinculoInstitucionalServiceTest` e página Instituicao. Cobertura automatizada. |
| RF17 | Acesso por modalidade | CDU-14; guarda do frontend e integração de autorização no backend. Cobertura automatizada. |
| RF18 | Capacidade institucional | CDU-13; `VinculoInstitucionalServiceTest` cobre limite finito, concorrência e ausência de teto no plano ilimitado. |
| RF19 | Bloqueio da compra individual para aluno vinculado | CDU-10/14; `FluxosAplicacaoIntegracaoTest`. Cobertura automatizada. |
| RF20 | Auditoria | CDU-15; gravação, idempotência e proteção contra alteração em `FluxosAplicacaoIntegracaoTest`. Retenção mínima de 4 anos definida; operação de arquivo e descarte ainda precisa ser formalizada. |

| RNF | Evidência e limite |
| --- | --- |
| RNF01 | Aplicação web validada nos testes de interface; registrar também navegadores, resoluções, teclado e resultado exploratório do ciclo. |
| RNF03 | Meta aprovada: p95 até 10 s para geração da próxima pergunta e p95 até 10 s para avaliação final, medidos separadamente. Registrar amostra, ambiente e medição com o provedor real; timeout configurado não comprova a meta. |
| RNF07 | Isolamento entre instituições testado em `VinculoInstitucionalServiceTest` e `FluxosAplicacaoIntegracaoTest`; repetir na jornada real com duas instituições. |
| RNF09 | Guarda mínima aprovada de 4 anos desde o evento. Triggers V8/V10 e tentativa de alteração verificadas na integração. Definir e testar procedimento operacional de backup, arquivamento e descarte; a aplicação não apaga automaticamente eventos em produção. |

## 5. Casos prioritários do ciclo

Os IDs abaixo preservam e corrigem os casos do PDF. Cada execução formal precisa de pré-condição, massa, passos, esperado, observado e evidência registrados. **Situação abaixo é de planejamento**, não resultado de execução.

| ID | RF/RNF | Cenário e resultado esperado | Automação relacionada |
| --- | --- | --- | --- |
| CT-IDENT-01 | RF01 | Aluno cria conta e entra; conta/perfil persistem e a sessão corresponde ao aluno. | CDU-01; integração de identidade. |
| CT-IDENT-02 | RF01/RF17 | Instituição entra; área institucional abre e rotas de treino de aluno recusam a conta. | CDU-01/14; integração de treino. |
| CT-CONTA-03 | RF01 | Instituição informa responsável e nome institucional; ambos ficam associados à conta correta. | CDU-01; integração de cadastro. |
| CT-ISOL-04 | RF09/RF10 | Alunos A/B alternam no mesmo navegador; histórico e estado local de A não aparecem para B. | CDU-08; frontend estadoAplicacao; integração de histórico. |
| CT-ISOL-05 | RF16/RNF07 | Instituições A/B consultam vínculos; A não vê nem altera vínculo de B. | CDU-13; integração de vínculos. |
| CT-TREINO-06 | RF07/RF08/RF09 | Aluno com acesso conclui entrevista; perguntas, respostas, nota e feedback pertencem ao aluno. | CDU-07; integração de treino; jornada real. |
| CT-TREINO-07 | RF07/RF08 | Falha de IA durante resposta/avaliação mantém sessão recuperável e impede conclusão sem avaliação. | `FluxosAplicacaoIntegracaoTest`: falha na próxima pergunta e na avaliação final; preservação das respostas, retomada após novo login e conclusão idempotente na retentativa. |
| CT-PLANO-08 | RF16/RF18 | Com capacidade N definida pela oferta, N vínculos ativos são permitidos; o excedente falha sem criar vínculo. | CDU-13; `VinculoInstitucionalServiceTest`. |
| CT-PLANO-09 | RF16/RF18 | Remover aluno encerra vínculo ativo e libera uma vaga. | CDU-13; integração de vínculos. |
| CT-COMPRA-10 | RF13/RF14/RF15 | Cartão fictício aprovado ativa acesso; recusado não concede acesso nem realiza cobrança real. | CDU-10/11/12; integração de contratação. |
| CT-COMPRA-11 | RF19 | Aluno com vínculo institucional ativo não contrata plano individual. | CDU-10/14; integração de contratação. |
| CT-SEG-12 | RF01/RF02/RNF07 | Sem sessão, token inválido ou perfil incompatível: API recusa sem expor dados; frontend não reutiliza estado antigo. | CDU-14; integração e testes do estado de sessão. |
| CT-AUD-13 | RF20/RNF09 | Ação coberta gera evento com ator/data; alteração comum é recusada. Confirmar que nenhuma rotina de produção descarta eventos antes de 4 anos e registrar política de backup. | CDU-15; integração de auditoria e verificação operacional. |
| CT-SENHA-14 | RF01/RF02/RF04 | Nova senha exige 8–128 caracteres, maiúscula, minúscula, número e especial; feedback visual acompanha digitação. | CDU-16; `PoliticaSenhaTest`; testes Vue. |
| CT-REC-15 | RF04 | Solicitação de recuperação não revela existência do e-mail; token é de uso único e expira. | CDU-04; `RecuperacaoSenhaServiceTest`. |
| CT-ISOL-16 | RF07/RF09/RF17 | Aluno B com plano ativo tenta responder, concluir e cancelar treino de A; API retorna 404, sem alterar o treino nem acionar a IA. | `FluxosAplicacaoIntegracaoTest.outroAlunoNaoRespondeConcluiNemCancelaTreinoDoTitular`. |
| CT-VINC-17 | RF16/RF17/RF18 | Remover vínculo bloqueia início, retomada, resposta e conclusão de treino com o token já emitido; respostas e avaliações permanecem intactas e a vaga permite vincular outro aluno que inicia seu próprio treino. | `FluxosAplicacaoIntegracaoTest.removerVinculoBloqueiaTreinoComTokenExistenteELiberaVagaParaOutroAluno`. |
| CT-PERFIL-18 | RF03 | Aluno cria, edita e remove habilidade; categoria longa permanece legível no desktop e no celular. | CDU-03; `PerfilAlunoServiceTest`; revisão visual. |
| CT-PLANO-19 | RF05 | Aluno e instituição consultam plano, validade e benefícios; vagas institucionais aparecem com rótulo correto. | CDU-05; `ConsultaPlanosIntegracaoTest`; revisão visual. |
| CT-VAGA-20 | RF06 | Aluno cria, edita e remove vaga; requisito longo permanece dentro do card em 360 px e 1920 px; contador usa singular com uma vaga. | CDU-06; `PerfilAlunoServiceTest`; revisão visual. |
| CT-CATALOGO-21 | RF11/RF12 | Visitante consulta preços e benefícios sem login, inclusive com API de sessão lenta; página de planos não apresenta rolagem horizontal em 360 px. | CDU-09; `ConsultaPlanosIntegracaoTest`; revisão visual. |
| CT-PLANO-22 | RF14/RF16/RF18 | Catálogo oferece planos de 50, 100 e ilimitado por R$ 900, R$ 1.750 e R$ 5.000; a compra simulada grava capacidade e preço escolhidos. O limite finito bloqueia excedentes e o ilimitado aceita novos vínculos. | CDU-11/13; integração do catálogo e compra; `VinculoInstitucionalServiceTest`; jornada real a executar. |
| CT-CARGA-23 | Usabilidade transversal | Durante navegação ou chamada visível, cursor de espera e indicador aparecem; cliques e teclado ficam inativos até a conclusão, inclusive em erro. Atividade de sessão em segundo plano não bloqueia. | `clienteHttp.test.ts`; revisão de interface a executar. |

A limpeza de dados por CPF/CNPJ é verificada separadamente em [`LimpezaDadosIntegracaoTest.java`](../backend/src/test/java/br/ivis/LimpezaDadosIntegracaoTest.java): a rota só existe em dev/homologação e deve apagar apenas a conta identificada e seus dados vinculados.

**Casos a especificar para fechamento:** escolha e compra das três ofertas institucionais; limites de 50 e 100 e vínculo sem teto no plano ilimitado; eventual troca de plano institucional vigente conforme regra comercial futura; medição de RNF03 com a meta aprovada de 10 s no p95 para cada operação; procedimento de guarda de auditoria por no mínimo 4 anos; avaliação humana da adequação das perguntas por nível, da não repetição e da qualidade do feedback. Registrar as verificações não executadas como ressalva ou bloqueio, nunca como aprovadas por inferência.

## 6. Entrada, execução e saída

**Entrada:** commit e ambiente identificados; dependências instaladas; dados sintéticos preparados; Docker disponível para Testcontainers/jornadas reais; chave NVIDIA configurada apenas quando o caso exige provedor real. Conferir o [guia de execução](como%20rodar.md).

**Comandos de regressão, a partir da raiz (PowerShell):**

```powershell
npm --prefix frontend run test:unit
npm --prefix frontend run typecheck
npm --prefix frontend run lint
Push-Location backend
.\gradlew.bat test --rerun-tasks
Pop-Location
Push-Location api-ia
& .\.venv\Scripts\python.exe -m pytest tests -q
Pop-Location
```

Para Cypress, a suíte usa o frontend do Compose em `http://localhost:8080`. A suíte completa inclui as jornadas reais e o CDU-15: preparar a stack Compose conforme o [README do Cypress](../cypress/e2e/README.md), então executar `npm run testar:e2e`. Registrar specs, ambiente, resultado, screenshots/vídeos e referência de defeito. Não converter a existência de um spec em resultado aprovado.

**Saída proposta para o ciclo:** nenhuma falha aberta de severidade bloqueadora ou crítica nos fluxos prioritários; todos os casos executados com observado e evidência; falhas altas com decisão explícita; requisitos parciais e RNFs ainda sem evidência de conformidade listados como ressalva; aceite registrado por responsável. Esta regra é proposta para revisão da equipe, não um aceite já concedido.

### Registro de execução

| Data | Commit/ambiente | Suíte | Resultado verificado | Evidência ou pendência |
| --- | --- | --- | --- | --- |
| 04/11/2026 | Local, commit `215a183` | Vitest frontend | 63 testes passaram em 17 arquivos | Saída do comando `npm run test:unit`; não cobre backend real. |
| 04/11/2026 | Local, commit `215a183` | pytest API de IA | 38 testes passaram | Um aviso de depreciação em Starlette/AnyIO; provedor NVIDIA simulado. |
| 04/11/2026 | Local, commit `215a183` | JUnit backend | 61 testes passaram, 0 falhas | `gradle test --rerun-tasks --console=plain`; integração executada com Testcontainers. |
| 04/11/2026 | Não executado nesta revisão | Cypress | Sem resultado registrado | Preparar Vite e, para jornadas reais, stack Compose e NVIDIA. |
| 04/11/2026 | Local, stack Compose em `localhost:8080`, banco único `ivis` | Cypress CDU-15 e perfil instituição | 6 testes passaram, incluindo hooks de limpeza | Execução com `--config baseUrl=http://localhost:8080,retries=0`; vídeos em `cypress/downloads`. Limpeza seletiva também validada por `node --test cypress/tasks/limpar-banco.integracao.test.mjs`, com transação revertida e preservação de outra conta, contrato e auditoria. |
| 04/11/2026 | Local, base `f3ab890` com novos testes cruzados ainda não commitados | JUnit backend | Compilação passou; 32 testes passaram; 3 classes falharam na inicialização por Docker indisponível | `gradlew.bat test --console=plain`; os novos cenários em `FluxosAplicacaoIntegracaoTest` não chegaram a executar. Reexecutar com o motor Docker disponível. |

### Modelo de evidência e defeito

Para cada caso: **ID, RF/RNF, commit, ambiente, executor, data, pré-condições, dados sintéticos, passos, esperado, observado, status** (`aprovado`, `reprovado`, `bloqueado` ou `não executado`) e caminho da evidência. Se houver defeito, registrar ID, frequência, severidade (`bloqueador`, `crítica`, `alta`, `normal`, `baixa`), prioridade de correção (P0–P3), responsável, versão da correção e resultado do reteste. Mascarar dados sensíveis antes de anexar evidências.

### Decisão do ciclo

| Campo | Registro |
| --- | --- |
| Decisão | Pendente: aprovado / aprovado com ressalvas / reprovado / inconclusivo. |
| Responsável pelo aceite | A definir pela equipe. |
| Data e commit aceitos | A registrar. |
| Defeitos e riscos aceitos | A registrar com justificativa e responsável. |
