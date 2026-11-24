# Evidências do projeto de Computação em Nuvem

Este índice lista as evidências que sustentam a implantação do IVIS na AWS e o que cada uma comprova. As imagens foram **mascaradas** (retângulo preto) antes de entrar no repositório: número da conta, usuários do laboratório, identificadores de recursos, endereços IP, ARNs e credenciais não aparecem. Os originais não são versionados.

A coluna "Situação" indica se o arquivo já está nesta pasta (`guardada`) ou ainda será acrescentado (`pendente`).

## Evidências

| Nº | Evidência | O que comprova | Arquivo previsto | Situação |
|---|---|---|---|---|
| 1 | Execução do CI com os cinco jobs verdes | Testes e verificações automatizados | `01-ci-verde.png` | pendente |
| 2 | Execução do Deploy com os quatro jobs verdes | Construção, implantação automatizada e verificação | `02-deploy-verde.png` | pendente |
| 3 | Log do passo "Implantar a tag nova" | Implantação por SSM, sem credencial exposta | `03-deploy-implantar.png` | pendente |
| 4 | Log do passo "Testar o /pronto" com HTTP 200 | Verificação automatizada de disponibilidade | `04-deploy-verificar.png` | pendente |
| 5 | Relatórios de teste publicados como artefatos | Resultados dos testes do backend e da API de IA | `05-artefatos-testes.png` | pendente |
| 6 | Regra `proteger-main` | Cinco checks obrigatórios e uma aprovação | `06-ruleset.png` | pendente |
| 7 | Solicitação de mesclagem com checks e aprovação | A mesclagem só ocorre com testes verdes | `07-pr-checks.png` | pendente |
| 8 | `/pronto` com HTTPS | Aplicação na nuvem com certificado válido | `08-pronto-https.png` | pendente |
| 9 | Contêineres em execução | Backend, frontend, API de IA e Caddy saudáveis | `09-docker-ps.png` | pendente |
| 10 | RDS privado | Banco sem acesso público | `10-rds-privado.png` | pendente |
| 11 | Grupos de segurança | Portas restritas (sem 22; 5432, 9091 e 3100 só por grupo) | `11-grupos-seguranca.png` | pendente |
| 12 | Instâncias EC2 em execução | Ambiente de aplicação e de monitoramento | `12-ec2.png` | pendente |
| 13 | Repositórios ECR com as tags | Artefatos publicados com tag igual ao commit | `13-ecr.png` | pendente |
| 14 | `terraform state list` (32 entradas) | Infraestrutura provisionada por Terraform | `14-terraform-state.png` | pendente |
| 15 | Plano de remoção (31 recursos) | Procedimento de remoção, ainda a validar | `15-terraform-destroy-plan.png` | pendente |
| 16 | Painel do Grafana com métricas | Observabilidade: requisições, p95, erros, heap e CPU | `16-grafana-metricas.png` | pendente |
| 17 | Painel de logs | Observabilidade: logs de erro da aplicação | `17-grafana-logs.png` | pendente |
| 18 | Fontes de dados do Grafana | Prometheus e Loki conectados | `18-grafana-fontes.png` | pendente |
| 19 | `gitleaks` sem achados | Nenhum segredo no histórico do repositório | `19-gitleaks.png` | pendente |
| 20 | Segredos no Infisical (somente os nomes) | Gerenciamento de segredos fora do código | `20-infisical-nomes.png` | pendente |
| 21 | Aplicação em uso (login e treino com IA) | Funcionalidade real executando na nuvem | `21-aplicacao.png` | pendente |
| 22 | Cronômetro e crédito do laboratório | Consumo acompanhado | `22-credito.png` | pendente |

## Ocorrências registradas

Estas ocorrências fazem parte do histórico do projeto e são registradas aqui para que a documentação seja fiel ao que aconteceu.

- **Solicitações #8 e #9 de `JoaoLRS/IVIS`:** foram abertas por engano no repositório da disciplina de Engenharia de Software. A `main` desse repositório não foi reescrita. Elas correspondem à #2 da entrega.
- **Solicitação #26:** foi mesclada com o CI vermelho, porque os checks obrigatórios ainda não estavam ativos. A #27 a reverteu e a #28 corrigiu o teste de integração. O fato motivou a proteção da branch `main`.
- **Registro duplicado `b53e3d1` (20/11/2026):** foi feito por engano direto na `main` local e nunca foi enviado ao repositório remoto; a `main` local foi realinhada com `git reset --hard origin/main`.
- **Credenciais temporárias do laboratório (20/11/2026):** houve um vazamento acidental de credenciais temporárias do laboratório, resolvido com End Lab e Start Lab, que as invalidou.