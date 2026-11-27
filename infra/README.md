# infra/ – Infraestrutura do IVIS na AWS (Terraform)

Infraestrutura como código do IVIS no AWS Academy Learner Lab (região `us-east-1`). A arquitetura e as justificativas estão no [ADR-0012](../etc/plano/adr/ADR-0012-hospedagem-aws.md); o procedimento completo de execução, implantação e remoção está no [Guia de Implantação](../etc/plano/Guia_Implantacao_AWS.md).

Nunca versionar `*.tfstate*`, `*.tfvars`, `*.tfplan` nem chaves. O estado remoto fica no S3 e os segredos no Infisical.

## O que esta pasta cria

- Rede: VPC, gateway de internet, sub-redes públicas e privadas, tabelas de rota e suas associações.
- Grupos de segurança da aplicação, do monitoramento e do banco.
- Duas EC2 com Amazon Linux 2023: "app" (`t3.medium`, com IP elástico) e "monitoramento" (`t3.small`). O `user_data` instala o Docker e o plugin do Docker Compose (versão fixada em `compute.tf`).
- RDS PostgreSQL 17.11 (`db.t3.micro`), privado e com armazenamento criptografado.
- Três repositórios ECR (`ivis-backend`, `ivis-api-ia` e `ivis-frontend`), com tags imutáveis e varredura no envio.

O `terraform state list` tem 32 entradas: 31 recursos gerenciados e a leitura da AMI (um `data`).

## Arquivos

| Arquivo | Conteúdo |
|---|---|
| `versions.tf` | Versão mínima do Terraform (`>= 1.11`), provedor e backend do S3 com travamento por arquivo (`use_lockfile`). |
| `network.tf` | VPC, gateway de internet, sub-redes, tabelas de rota e associações. |
| `security.tf` | Grupos de segurança e suas regras de entrada e saída. |
| `ecr.tf` | Repositórios ECR. |
| `compute.tf` | EC2 "app", EC2 "monitoramento", IP elástico, AMI e `user_data`. |
| `database.tf` | RDS PostgreSQL. |
| `variables.tf` | Variáveis de entrada. |
| `outputs.tf` | Saídas. |

O arquivo `.terraform.lock.hcl` também é versionado, para fixar as versões dos provedores.

## Estado remoto

O bucket do estado **não** é gerenciado pelo Terraform: ele precisa existir antes do `terraform init` e foi criado à mão, com versionamento, bloqueio de acesso público e criptografia (comandos no Guia, seção 4.1). O travamento usa o arquivo `terraform.tfstate.tflock` no próprio bucket, sem DynamoDB.

## Pré-requisitos

- Terraform `>= 1.11` (o grupo usa a 1.17.1), AWS CLI 2 e o Learner Lab iniciado.
- As credenciais temporárias do laboratório entram por variáveis de ambiente no terminal, nunca em arquivo (Guia, seção 3).
- As duas EC2 usam o perfil de instância `LabInstanceProfile`, que existe no laboratório.
- Execute os comandos em um terminal fora do VS Code.

## Variáveis

| Variável | Descrição | Valor padrão |
|---|---|---|
| `regiao` | Região da AWS usada pelo projeto. | `us-east-1` |
| `senha_banco` | Senha do administrador do RDS, marcada como sensível. Informe por `TF_VAR_senha_banco` e nunca a versione. Mínimo de 12 caracteres, sem barra, arroba, aspas nem espaço (validação em `variables.tf`). | sem padrão |

## Saídas

| Saída | Descrição |
|---|---|
| `vpc_id` | Identificador da VPC do IVIS. |
| `sub_redes_publicas` | Identificadores das sub-redes públicas, por sufixo de zona. |
| `sub_redes_privadas` | Identificadores das sub-redes privadas, por sufixo de zona. |
| `grupos_de_seguranca` | Identificadores dos grupos de segurança (app, monitoramento e rds). |
| `urls_ecr` | URLs dos repositórios ECR, por nome de imagem. |
| `endereco_banco` | Endereço (host) do RDS; a senha nunca é exposta como saída. |
| `ip_elastico_app` | IP elástico da EC2 app (aponta o subdomínio do DuckDNS). |
| `ip_privado_app` | IP privado da EC2 app (alvo do Prometheus na porta 9091). |
| `ip_publico_monitoramento` | IP público da EC2 de monitoramento (muda a cada reinício). |
| `ids_instancias` | Identificadores das instâncias EC2, para uso com o SSM. |

## Como alterar a infraestrutura

Cada alteração segue: ramo novo, formatação e validação, plano salvo, revisão, `apply` do plano salvo, conferência do estado, registro de alteração e solicitação de mesclagem revisada por outra pessoa.

```powershell
cd infra
terraform init
terraform fmt -check -diff
terraform validate
terraform plan "-out=alteracao.tfplan"
terraform apply "alteracao.tfplan"
terraform state list
```

Os argumentos com ponto vão entre aspas no PowerShell 5.1. Se um `apply` parar no meio, não rode `destroy` nem repita o comando: guarde a saída e avise o coordenador. Apague os arquivos `*.tfplan` depois do uso (o Git os ignora, mas eles podem conter valores sensíveis).

## Remover os recursos (a validar)

```powershell
terraform plan "-destroy" "-out=destruir.tfplan"
```

O plano de remoção foi gerado em 22/11/2026 e mostrou `0 to add, 0 to change, 31 to destroy`, sem ser aplicado; o ciclo completo `destroy` e `apply` **não foi validado**. Antes de remover, considere:

- O banco é apagado **sem instantâneo final** (`skip_final_snapshot = true`) e não há backups automáticos (`backup_retention_period = 0`). Para guardar os dados, crie um instantâneo manual antes.
- `force_delete = true` nos repositórios ECR: as imagens são perdidas e é preciso construir e enviar de novo.
- O IP elástico muda e o subdomínio do DuckDNS precisa ser atualizado; o certificado do Let's Encrypt é perdido.
- Não são removidos: o bucket do estado, instantâneos manuais do RDS e o subdomínio do DuckDNS.
- Para um plano de **remoção** qualquer valor de `TF_VAR_senha_banco` serve, mas nunca use um valor descartável em um `plan` ou `apply` normal: o Terraform tentaria trocar a senha do RDS.

## Segurança

- O RDS é privado (`publicly_accessible = false`), com armazenamento criptografado, e a porta 5432 só é aceita pelo grupo de segurança da EC2 "app".
- A porta 22 não é aberta; a administração usa o SSM. As portas 80 e 443 da EC2 "app" são públicas.
- A porta 9091 (métricas do backend) só é aceita do grupo de segurança de monitoramento, e a 3100 (Loki) só do grupo da EC2 "app". O grupo de monitoramento não tem entrada pública: o acesso ao Grafana é por túnel SSM.
- As EC2 exigem IMDSv2 (`http_tokens = required`) e têm os discos criptografados; os repositórios ECR têm `scan_on_push`.

## Limitações

- O ciclo `terraform destroy` e `apply` não foi validado nesta infraestrutura.
- As EC2 têm `ignore_changes = [ami, user_data]`: alterar a AMI ou o `user_data` no código não recria as instâncias existentes.
- A saída de rede está liberada para qualquer destino nos grupos da app e do monitoramento (imagens Docker, SSM, Loki e coleta de métricas).
- O `plan` da EC2 foi feito com `-target` em três recursos (`aws_instance.app`, `aws_eip_association.app` e `aws_instance.monitoramento`); ver o Guia, seção 4.4.
- O Terraform não copia o compose, o `Caddyfile`, o `.env` nem a pasta `observability/` para as EC2; essas etapas são manuais.