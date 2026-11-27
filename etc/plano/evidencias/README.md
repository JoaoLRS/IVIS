# Evidências do projeto de Computação em Nuvem

Este índice lista as evidências que sustentam a implantação do IVIS na AWS e o que cada uma comprova. As imagens foram **mascaradas** (retângulo preto) antes de entrar no repositório: número da conta, usuários do laboratório, identificadores de recursos, endereços IP, ARNs e credenciais não aparecem. Os originais não são versionados.

A coluna "Situação" indica se o arquivo já está nesta pasta (`guardada`) ou ainda será acrescentado (`pendente`).

## Evidências

| Nº | Evidência | O que comprova | Arquivo | Situação |
|---|---|---|---|---|
| 1 | Execução do CI com os cinco jobs verdes | Testes e verificações automatizados | `01-ci-verde.png` | guardada |
| 2 | Execução do Deploy com os quatro jobs verdes | Construção, implantação automatizada e verificação | `02-deploy-verde.png` | guardada |
| 3 | Log do passo "Implantar a tag nova" | Implantação por SSM, sem credencial exposta | `03-deploy-implantar.png` | guardada |
| 4 | Log do passo "Testar o /pronto" com HTTP 200 | Verificação automatizada de disponibilidade | `04-deploy-verificar.png` | guardada |
| 5 | Relatórios de teste publicados como artefatos | Resultados dos testes do backend e da API de IA | `05-artefatos-testes.png` | guardada |
| 6 | Regra `proteger-main` | Cinco checks obrigatórios e uma aprovação | `06-ruleset.png` | guardada |
| 7 | Solicitação de mesclagem com checks e aprovação | A mesclagem só ocorre com testes verdes | `07-pr-checks.png` | guardada |
| 8 | `/pronto` com HTTPS | Aplicação na nuvem com certificado válido | `08-pronto-https.png` | guardada |
| 9 | Contêineres em execução | Backend, frontend, API de IA e Caddy saudáveis | `09-docker-ps.png` | guardada |
| 10 | RDS privado | Banco sem acesso público | `10-rds-privado.png` | guardada |
| 11 | Grupos de segurança | Portas restritas (sem 22; 5432, 9091 e 3100 só por grupo) | `11-grupos-seguranca.png` | guardada |
| 12 | Instâncias EC2 em execução | Ambiente de aplicação e de monitoramento | `12-ec2.png` | guardada |
| 13 | Repositórios ECR com as tags | Artefatos publicados com tag igual ao commit | `13-ecr.png` | guardada |
| 14 | `terraform state list` (32 entradas) | Infraestrutura provisionada por Terraform | `14-terraform-state.png` | guardada |
| 15 | Plano de remoção (31 recursos) | Procedimento de remoção, ainda a validar | `15-terraform-destroy-plan.png` | guardada |
| 16 | Painel do Grafana com métricas | Observabilidade: requisições, p95, erros, heap e CPU | `16-grafana-metricas.png` | guardada |
| 17 | Painel de logs | Observabilidade: logs de erro da aplicação | `17-grafana-logs.png` | guardada |
| 18 | Fontes de dados do Grafana | Prometheus e Loki conectados | `18-grafana-fontes.png` | guardada |
| 19 | `gitleaks` sem achados | Nenhum segredo no histórico do repositório | `19-gitleaks.png` | guardada |
| 20 | Segredos no Infisical (somente os nomes) | Gerenciamento de segredos fora do código | `20-infisical-nomes.png` | guardada |
| 21 | Aplicação em uso (login e treino com IA) | Funcionalidade real executando na nuvem | vídeo entregue por link, fora do repositório | guardada (vídeo) |
| 22 | Cronômetro e crédito do laboratório | Consumo acompanhado | `22-credito.png` | guardada |

## Ocorrências registradas

Estas ocorrências fazem parte do histórico do projeto e são registradas aqui para que a documentação seja fiel ao que aconteceu.

- **Solicitações #8 e #9 de `JoaoLRS/IVIS`:** foram abertas por engano no repositório da disciplina de Engenharia de Software. A `main` desse repositório não foi reescrita. Elas correspondem à #2 da entrega.
- **Solicitação #26:** foi mesclada com o CI vermelho, porque os checks obrigatórios ainda não estavam ativos. A #27 a reverteu e a #28 corrigiu o teste de integração. O fato motivou a proteção da branch `main`.
- **Registro duplicado `b53e3d1` (20/11/2026):** foi feito por engano direto na `main` local e nunca foi enviado ao repositório remoto; a `main` local foi realinhada com `git reset --hard origin/main`.
- **Credenciais temporárias do laboratório (20/11/2026):** houve um vazamento acidental de credenciais temporárias do laboratório, resolvido com End Lab e Start Lab, que as invalidou.

## Aprovações e limitações

- A aprovação das tecnologias fora da lista do enunciado (Caddy, DuckDNS, Let's Encrypt e Grafana Alloy) ocorreu no acompanhamento do projeto, em 11/11/2026, e está registrada no ADR-0012. Não há registro escrito separado.
- O ciclo `terraform destroy` e `apply` não foi validado nesta infraestrutura; o plano de remoção foi gerado, sem ser aplicado.
- O ambiente na AWS não permanece no ar: o laboratório é desligado entre os usos e a conta é encerrada ao fim do curso. As evidências acima e o vídeo substituem o acesso à aplicação.