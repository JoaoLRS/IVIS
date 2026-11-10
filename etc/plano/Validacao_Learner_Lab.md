# Validação do AWS Academy Learner Lab

- **Período:** 09 e 10/11/2026
- **Região:** us-east-1
- **Objetivo:** confirmar, na prática, as premissas do [ADR-0012](adr/ADR-0012-hospedagem-aws.md) antes de escrever o Terraform da Fase 1.
- **Resultado:** todas as premissas se confirmaram. Os achados da seção 4 não invalidam o ADR-0012, mas passam a ser regras de operação.

## 1. Método

Os recursos de teste foram criados por AWS CLI, observados e apagados no mesmo dia. Os identificadores abaixo não contêm segredos. O número da conta e os nomes de usuário foram omitidos ou substituídos por `XXXXXXXXXXXX`, `[usuario]` e `[sistema]`.

## 2. Premissas validadas

| Premissa | Como foi testada | Resultado |
|---|---|---|
| Perfil de execução disponível | Consulta da identidade e do `LabInstanceProfile` | Perfil `voclabs`; `LabRole` e `LabInstanceProfile` existem, e o perfil contém a `LabRole` |
| Estado do Terraform no S3 com travamento por arquivo | Bucket com versionamento e bloqueio de acesso público; duas gravações do mesmo objeto com `--if-none-match "*"` | A segunda gravação falhou com `PreconditionFailed`. O mecanismo de `use_lockfile` funciona; a confirmação com o próprio Terraform fica para a Fase 1 |
| VPC própria | Criação e remoção de VPC e sub-rede de teste | Possível. A VPC padrão tem sub-redes públicas em 6 zonas |
| ECR | Repositório com varredura ao enviar e obtenção de token | Funciona |
| EC2 com IMDSv2 e perfil de instância | t3.small com `LabInstanceProfile`, IMDSv2 obrigatório, grupo de segurança só com as portas 80 e 443 | Funciona |
| Administração sem SSH | Instância registrada no Systems Manager (SSM) | Aparece como `Online`; não é necessária a porta 22 |
| IP elástico | Associação a uma EC2 de teste | Funciona |
| RDS PostgreSQL privado | db.t3.micro, PostgreSQL 17.6, `PubliclyAccessible=false`, sem Multi-AZ, sem retenção de cópias. Versões 17.5 a 17.11 disponíveis | Funciona |
| Isolamento do banco | Grupo de segurança do banco aceita a porta 5432 só do grupo da EC2; teste de conexão de TCP feito pela EC2 via SSM | `PORTA_ABERTA` |

## 3. Ciclos de End Lab e Start Lab

Os horários estão em -03:00. As paradas de EC2 foram feitas pelo próprio integrante (Rafael Melo) que executou os testes; o laboratório só religou as instâncias.

### 3.1 Ciclo curto

| Horário | Evento | Origem |
|---|---|---|
| 23:37 | End Lab | Integrante |
| 23:41:30 | `StopInstances` da EC2 "app" | Navegador, sessão do integrante |
| 23:45 | Start Lab | Integrante |
| 23:45:15 | `StartInstances` | `[sistema]` |

Resultado: a EC2 "app" voltou a `Online` no SSM em menos de 3 minutos. Os IPs elásticos mantiveram os mesmos endereços e as mesmas associações. O RDS permaneceu `available`, no mesmo endereço, sem evento de parada ou início.

### 3.2 Ciclo longo (cerca de 10 horas)

| Horário | Evento | Origem |
|---|---|---|
| ~00:04 | End Lab (hora aproximada) | Integrante |
| 00:01:58 e 00:03:56 | `StopInstances` da t3.micro antiga | AWS CLI, sessão do integrante |
| 00:02:47 | `StopInstances` da EC2 "app" | Navegador, sessão do integrante |
| 09:57:50 | `StartInstances` | `[sistema]` |
| 09:58 | Start Lab | Integrante |
| 10:01:20 | Captura imediata | EC2 "app" e t3.micro `running`; "app" `Online` no SSM |
| 10:06:30 | Captura aos 5 minutos | Sem alteração |

Resultado: o RDS ficou `available` o tempo todo, no mesmo endereço. Não há `StopDBInstance` nem `StartDBInstance` no CloudTrail no período. O único evento `restarted` do banco é da criação dele.

### 3.3 Correção de um registro anterior

Na primeira análise, a parada das 23:41 foi atribuída ao laboratório. O CloudTrail mostrou que todas as paradas de EC2 vieram da sessão do integrante. **O laboratório só religa instâncias; nesta validação, não parou nenhuma EC2 nem o RDS.**

## 4. Achados e regras de operação

1. **O Start Lab religa toda EC2 parada,** inclusive as que foram paradas de propósito. Regra: apagar o que não estiver em uso, em vez de parar.
2. **O RDS não é parado pelo laboratório** (nem após cerca de 10 horas) e consome crédito o tempo todo. Regra: o banco só existe quando o ambiente existe. Fora do trabalho, desligar com `terraform destroy`.
3. **Retomada após o Start Lab:** esperar cerca de 3 minutos e executar a verificação de saúde. Como o IP elástico persiste, o DuckDNS não precisa mudar. O Docker e os contêineres da EC2 precisam subir sozinhos no boot.
4. **Acesso administrativo por SSM,** sem a porta 22 aberta. O ADR-0012 deve registrar isso.
5. **O backend não tem Actuator nem Micrometer.** As métricas da aplicação ficam para a Fase 2. Endpoints de saúde existentes: `/v1/saude` e `/v1/pronto` (backend) e `/saude` (api-ia).
6. **As credenciais do laboratório são temporárias** (a sessão dura cerca de 4 horas) e precisam ser renovadas a cada Start Lab. Isso é uma restrição do ambiente, e não um resultado medido. Conforme o ADR-0012, elas ficam no Infisical e o GitHub guarda só uma credencial fixa de acesso a ele; o procedimento de renovação será implementado na Fase 1.

## 5. Consumo de crédito

- Leitura do painel às 10:19 de 10/11/2026: **US$ 11,2 de US$ 50**. O painel arredonda para US$ 0,1 e atualiza com atraso, então **não serve para medir o consumo** desta validação. A estimativa vem da tabela de preços.
- Preços sob demanda em us-east-1, **a conferir nas páginas de preço da AWS antes de usar em decisões**:

| Recurso | Preço aproximado por hora (US$) |
|---|---|
| EC2 t3.small | 0,0208 |
| EC2 t3.micro | 0,0104 |
| EC2 t3.medium | 0,0416 |
| RDS db.t3.micro (PostgreSQL, Single-AZ) | 0,018 |
| Endereço IPv4 público (cada) | 0,005 |

- Arquitetura final (t3.medium + t3.small + db.t3.micro + 2 IPv4): cerca de **US$ 0,09 por hora**, ou US$ 2,15 por dia ligada. Com cerca de US$ 38,8 de saldo, isso dá aproximadamente 18 dias ligada sem interrupção. O armazenamento (discos, S3, ECR) custa centavos nessa escala.

## 6. Limitações

- O encerramento do laboratório por **esgotamento do tempo da sessão** não foi testado. Os dois ciclos terminaram por End Lab manual.
- O `use_lockfile` foi validado só por gravação condicional no S3, e não com o Terraform.

## 7. Limpeza

Foram apagados: RDS (sem cópia final), EC2 de teste, IP elástico de teste, dois grupos de segurança, grupo de sub-redes do RDS, repositório ECR e bucket S3 (com todas as versões). A VPC de teste já havia sido apagada. A captura final de estado mostra que nenhum recurso da validação restou. Ficaram apenas recursos anteriores à validação, que não fazem parte do projeto.