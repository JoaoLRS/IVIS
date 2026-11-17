# Implantação do IVIS na AWS

Procedimento para executar, implantar, operar e **remover** o IVIS no ambiente do AWS Academy Learner Lab (região `us-east-1`). A arquitetura e as justificativas estão no ADR-0012 (`etc/plano/adr/ADR-0012-hospedagem-aws.md`).

> **Estado deste documento (rascunho v0.1, 17/11/2026).** Os comandos abaixo foram executados pelo grupo em 15 e 16/11/2026, a partir do histórico do terminal e dos arquivos de comando usados. Os pontos marcados com **[A CONFERIR]** ainda não foram confirmados e devem ser resolvidos antes da entrega. Os pontos marcados com **[A VALIDAR]** dependem do ciclo `terraform destroy` e `apply`, ainda não feito.

## 1. Visão geral

- **EC2 "app"** (`t3.medium`, Amazon Linux 2023, IP elástico): contêineres `ivis-caddy`, `ivis-frontend`, `ivis-backend` e `ivis-api-ia`, em Docker Compose, em `/opt/ivis`.
- **RDS PostgreSQL 17** (`db.t3.micro`), privado: só aceita conexões do grupo de segurança da EC2 app.
- **EC2 "monitoramento"** (`t3.small`): Prometheus, Grafana e Loki (observabilidade, ver `observability/`).
- **ECR**: repositórios `ivis-backend`, `ivis-api-ia` e `ivis-frontend`, com tags imutáveis.
- **S3**: estado remoto do Terraform, com travamento por arquivo (`use_lockfile`).
- **Acesso administrativo**: Session Manager (SSM), sem porta 22 aberta.
- **Domínio e HTTPS**: subdomínio do DuckDNS apontando para o IP elástico; certificado Let's Encrypt emitido pelo Caddy.
- **Frontend e API na mesma origem** (Caddy serve `/` e `/api`), porque o cookie `ivis_refresh` é `SameSite=Strict`.

## 2. Pré-requisitos

Na máquina de quem implanta (Windows, PowerShell 5.1):

- AWS CLI 2, Terraform (versão mínima exigida em `infra/versions.tf` **[A CONFERIR]**), Docker, Git, gitleaks e o plugin do Session Manager.
- Acesso ao Learner Lab iniciado (botão **Start Lab**) e às credenciais temporárias do painel.

**Regra de execução:** comandos de AWS, Terraform e `gradlew test` devem ser rodados em um PowerShell **fora do VS Code**, porque o terminal do VS Code injeta o `.env` do projeto no ambiente e quebra os testes.

## 3. Credenciais do laboratório

As credenciais mudam a cada sessão e a sessão dura cerca de 4 horas (conferir o cronômetro no painel). Em cada terminal novo:

```powershell
$env:AWS_ACCESS_KEY_ID = (Read-Host "AWS_ACCESS_KEY_ID").Trim()
$env:AWS_SECRET_ACCESS_KEY = (Read-Host "AWS_SECRET_ACCESS_KEY").Trim()
$env:AWS_SESSION_TOKEN = (Read-Host "AWS_SESSION_TOKEN").Trim()
$env:AWS_DEFAULT_REGION = "us-east-1"
```

Nunca colar credenciais em arquivos, no chat ou no repositório. Os valores ficam só nas variáveis de ambiente do terminal. Conferência da sessão (a saída contém o número da conta; **não publicar**):

```powershell
aws sts get-caller-identity --query "Arn" --output text
```

## 4. Infraestrutura com Terraform (pasta `infra/`)

### 4.1 Estado remoto (feito uma única vez, à mão)

O bucket do estado não é gerenciado pelo Terraform (ele precisa existir antes do `init`):

```powershell
aws s3api create-bucket --bucket ivis-tfstate-rmmojl-20261111 --region us-east-1
aws s3api put-bucket-versioning --bucket ivis-tfstate-rmmojl-20261111 --versioning-configuration Status=Enabled
aws s3api put-public-access-block --bucket ivis-tfstate-rmmojl-20261111 --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
aws s3api put-bucket-encryption --bucket ivis-tfstate-rmmojl-20261111 --server-side-encryption-configuration '{\"Rules\":[{\"ApplyServerSideEncryptionByDefault\":{\"SSEAlgorithm\":\"AES256\"}}]}'
```

Conferência: `aws s3api get-bucket-versioning --bucket ivis-tfstate-rmmojl-20261111 --query "Status" --output text` deve responder `Enabled`.

O travamento do estado foi testado criando e removendo à mão o arquivo `terraform.tfstate.tflock` no bucket, com um `terraform plan -lock-timeout=0s` entre os dois passos.

### 4.2 Variáveis

A única variável sem valor padrão é a senha do banco, que **não** entra em arquivo versionado. Mínimo de 12 caracteres, sem barra, arroba, aspas nem espaço (regra de validação em `infra/variables.tf`):

```powershell
$env:TF_VAR_senha_banco = (Read-Host "Senha do RDS").Trim()
```

A mesma senha é cadastrada no Infisical como `IVIS_BD_SENHA` (ver seção 11). **[A CONFERIR]** como a senha foi gerada e passada ao Terraform na execução de 15/11.

### 4.3 Ritual de cada alteração de infraestrutura

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

Os argumentos do Terraform que contêm ponto vão entre aspas no PowerShell 5.1 (por exemplo, `"-out=alteracao.tfplan"`). O arquivo `*.tfplan` é ignorado pelo Git.

**Se um `apply` parar no meio, não rodar `destroy` nem repetir o comando:** guardar a saída e avisar o coordenador.

### 4.4 Ordem em que o ambiente foi criado

1. `infra/base-terraform`: backend do S3 e travas de versão.
2. `infra/rede`: VPC, sub-redes públicas e privadas.
3. `infra/seguranca-ecr`: grupos de segurança e repositórios ECR.
4. `infra/ec2`: EC2 app, EC2 de monitoramento e IP elástico. O `plan` desta etapa foi feito com `-target` em três recursos (`aws_instance.app`, `aws_eip_association.app` e `aws_instance.monitoramento`). **[A CONFERIR]** o motivo.
5. `infra/rds`: RDS PostgreSQL privado. Foi aplicado numa sessão posterior do laboratório, depois das EC2.

Resultado: **32 entradas** em `terraform state list`.

### 4.5 O que o `user_data` faz (e o que não faz)

Nas duas EC2, o `user_data` instala o Docker, habilita o serviço, inclui o `ec2-user` no grupo `docker` e instala o plugin do Docker Compose (versão fixada em `infra/compute.tf`). O registro fica em `/var/log/ivis-user-data.log`. O `user_data` **não** cria `/opt/ivis`, não cria os volumes do Caddy, não copia o compose nem o `Caddyfile`, não gera o `.env` e não faz login no ECR. Essas etapas são manuais (seção 7) até existir o `deploy.yml`.

## 5. Comandos na EC2 (padrão `send-command`)

Os comandos na EC2 são enviados por `aws ssm send-command` com um arquivo JSON em `$env:USERPROFILE\ivis-ssm`. **Nunca colar blocos de várias linhas em uma sessão SSM interativa** (a colagem pode criar arquivos nos lugares errados; ver a seção 13).

Descobrir o ID da EC2 app:

```powershell
aws ec2 describe-instances --filters "Name=tag:Name,Values=ivis-app" "Name=instance-state-name,Values=running" --query "Reservations[].Instances[].InstanceId" --output text
```

Modelo completo (leitura do estado da EC2):

```powershell
$dir = Join-Path $env:USERPROFILE "ivis-ssm"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$json = '{"InstanceIds":["ID-DA-EC2-APP"],"DocumentName":"AWS-RunShellScript","Parameters":{"commands":["date","docker ps -a --format ''table {{.Names}}\t{{.Image}}\t{{.Status}}'' | sed -E ''s/[0-9]{12}/XXXXXXXXXXXX/g''","docker inspect ivis-backend --format ''Reinicios={{.RestartCount}}''","grep ''^IVIS_TAG='' /opt/ivis/.env"]}}'
$json = $json.Replace("ID-DA-EC2-APP", "<id obtido acima>")
[System.IO.File]::WriteAllText("$dir\estado.json", $json, (New-Object System.Text.UTF8Encoding($false)))
$id = aws ssm send-command --cli-input-json "file://$dir/estado.json" --query "Command.CommandId" --output text
Start-Sleep -Seconds 8
aws ssm get-command-invocation --command-id $id --instance-id <id obtido acima> --query "[Status,StandardOutputContent,StandardErrorContent]" --output text
```

Boas práticas, todas aplicadas nos comandos do grupo:

- Mascarar números de conta de 12 dígitos na saída com `sed -E 's/[0-9]{12}/XXXXXXXXXXXX/g'`.
- Nunca imprimir o `.env` inteiro: usar `grep` só nas variáveis não sigilosas (`IVIS_TAG`, `IVIS_ECR_REGISTRO`, `IVIS_DOMINIO`).
- Apagar o JSON depois de usar, se ele contiver qualquer segredo (`Remove-Item "$dir\nome.json"`).
- No PowerShell, `\"` dentro de aspas duplas não escapa: usar aspas simples externas, com `''` para aspas simples internas.
- Gravar arquivos com `[System.IO.File]::WriteAllText` e `UTF8Encoding($false)` (UTF-8 sem BOM, quebras de linha LF), sempre com **caminho completo**, porque o `cd` não afeta os métodos de `[System.IO.File]`.

## 6. Construir e enviar as imagens ao ECR

Cada implantação usa uma **tag nova**, igual ao hash curto do commit (as tags do ECR são imutáveis).

```powershell
git switch main
git pull origin main
git status --short
$tag = git rev-parse --short HEAD
$tag
```

Testes antes de construir (PowerShell fora do VS Code):

```powershell
.\gradlew.bat --stop
.\gradlew.bat test
```

Construção local das três imagens (o frontend precisa do `VITE_API_URL=/api`, que o torna same-origin):

```powershell
docker build -t "ivis-api-ia:$tag" .\api-ia
docker build -t "ivis-frontend:$tag" --build-arg VITE_API_URL=/api .\frontend
docker build -t "ivis-backend:$tag" .\backend
```

Conferir que o `/api` entrou no frontend:

```powershell
docker run --rm --entrypoint sh "ivis-frontend:$tag" -c "grep -rl '/api' /usr/share/nginx/html/assets | head -n 3"
```

Login no ECR. **Atenção:** o `docker login` com pipe direto no PowerShell 5.1 dá `400 Bad Request`; usar o `cmd /c`:

```powershell
$conta = aws sts get-caller-identity --query "Account" --output text
$registro = "$conta.dkr.ecr.us-east-1.amazonaws.com"
cmd /c "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $registro"
```

**[A CONFERIR]** a forma exata como `$conta` foi obtida em 15/11. Não publicar o valor de `$registro` em evidências.

Marcar e enviar:

```powershell
foreach ($n in "ivis-api-ia","ivis-frontend","ivis-backend") {
  docker tag "${n}:$tag" "$registro/${n}:$tag"
  docker push "$registro/${n}:$tag"
}
foreach ($r in "ivis-backend","ivis-api-ia","ivis-frontend") { Write-Host "== $r"; aws ecr describe-images --repository-name $r --query "imageDetails[].imageTags[0]" --output text }
```

## 7. Preparar a EC2 app (primeira vez ou após recriar a infraestrutura)

Conferir que o `user_data` terminou (Docker e Compose instalados), com o padrão da seção 5:

```
grep -E "^\[ivis\]" /var/log/ivis-user-data.log
docker --version
docker compose version
systemctl is-active docker
```

Depois, em `/opt/ivis` precisam existir (todos com permissão restrita ao administrador, o `.env` com `chmod 600`):

- `docker-compose.prod.yml` e `Caddyfile` (copiados de `deploy/`).
- `.env` com as variáveis do compose (abaixo).
- Os volumes externos do Caddy, que mantêm o certificado do Let's Encrypt:

```
docker volume create ivis-teste_caddy_dados
docker volume create ivis-teste_caddy_config
```

Sem esses volumes, o `docker compose up -d` falha. **Nunca** rodar `docker volume prune`, `docker system prune --volumes` nem `docker compose down -v` nessa EC2: isso apaga o certificado e consome o limite de emissões do Let's Encrypt (limite **[A CONFERIR]**).

Variáveis do `.env` (só os **nomes**; os valores vêm do Infisical):

- Obrigatórias: `IVIS_ECR_REGISTRO`, `IVIS_TAG`, `IVIS_BD_URL`, `IVIS_BD_SENHA`, `IVIS_JWT_CHAVE_ASSINATURA`, `IVIS_IA_CHAVE_SERVICO`.
- Opcionais: `IVIS_DOMINIO` (padrão `ivis-ia.duckdns.org`), `IVIS_BD_USUARIO` (padrão `ivis`) e `NVIDIA_API_KEY` (sem ela, os treinos com IA ficam indisponíveis e o `/pronto` informa).

O modelo está em `deploy/.env.example`.

**[A CONFERIR]** o comando exato usado em 15/11 para gravar esses três arquivos na EC2 (arquivo `gravar-ivis.json`, já apagado por conter segredos) e a limpeza do resíduo `/opt/ivis-teste` (ver a seção 13).

### 7.1 DuckDNS

O subdomínio (`ivis-ia.duckdns.org`) deve apontar para o IP elástico da EC2 app. Conferir:

```powershell
nslookup ivis-ia.duckdns.org
aws ec2 describe-addresses --query "Addresses[].PublicIp" --output text
```

Os dois valores devem ser iguais. Se a infraestrutura for recriada, o IP elástico muda e o subdomínio precisa ser atualizado no site do DuckDNS (o token fica no Infisical como `DUCKDNS_TOKEN`). **[A CONFERIR]** o passo a passo da atualização.

## 8. Implantar ou trocar a versão

Procedimento **manual**, usado em 15 e 16/11 (a automação é o `deploy.yml`):

1. Testar e construir as imagens com a tag nova (seção 6).
2. Se a versão traz **migração de banco** (Flyway), criar o instantâneo do RDS **antes** (seção 9).
3. Baixar as imagens na EC2 (login no ECR pelo `LabInstanceProfile`, sem credencial no host):

```powershell
$tag = "<tag nova>"
$json = '{"InstanceIds":["ID-DA-EC2-APP"],"DocumentName":"AWS-RunShellScript","Parameters":{"commands":["export HOME=/root","cd /opt/ivis","REG=$(grep ''^IVIS_ECR_REGISTRO='' .env | cut -d= -f2)","aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $REG 2>&1 | tail -n 3","IVIS_TAG=TAGNOVA docker compose -f docker-compose.prod.yml pull 2>&1 | sed -E ''s/[0-9]{12}/XXXXXXXXXXXX/g'' | tail -n 20","docker images --format ''table {{.Repository}}\t{{.Tag}}\t{{.Size}}'' | sed -E ''s/[0-9]{12}/XXXXXXXXXXXX/g''"]}}'
$json = $json.Replace("ID-DA-EC2-APP", "<id da EC2 app>").Replace("TAGNOVA", $tag)
```

4. Trocar **só** a `IVIS_TAG` do `.env` e subir (com cópia de segurança do `.env` nomeada pela tag anterior):

```powershell
$tagAnterior = "<tag em uso agora>"
$json = '{"InstanceIds":["ID-DA-EC2-APP"],"DocumentName":"AWS-RunShellScript","Parameters":{"commands":["export HOME=/root","cd /opt/ivis","cp -p .env .env.bak-TAGANTERIOR","sed -i ''s/^IVIS_TAG=.*/IVIS_TAG=TAGNOVA/'' .env","grep ''^IVIS_TAG='' .env","docker compose -f docker-compose.prod.yml up -d 2>&1 | sed -E ''s/[0-9]{12}/XXXXXXXXXXXX/g'' | tail -n 15","sleep 45","docker ps --format ''table {{.Names}}\t{{.Image}}\t{{.Status}}'' | sed -E ''s/[0-9]{12}/XXXXXXXXXXXX/g''","docker logs ivis-backend 2>&1 | grep -iE ''flyway|migrat|Started|ERROR'' | tail -n 25"]}}'
$json = $json.Replace("ID-DA-EC2-APP", "<id da EC2 app>").Replace("TAGANTERIOR", $tagAnterior).Replace("TAGNOVA", $tag)
```

Cada `$json` é gravado e enviado como na seção 5, com `Start-Sleep` de 75 segundos antes de ler o resultado (a subida inclui os 45 s do `sleep`).

5. Verificar (seção 10).

## 9. Instantâneo do banco e pontos de volta

Antes de migrações que não se desfazem apenas trocando a imagem:

```powershell
aws rds create-db-snapshot --db-instance-identifier ivis-banco --db-snapshot-identifier ivis-antes-v14-v15 --query "DBSnapshot.Status" --output text
aws rds describe-db-snapshots --db-snapshot-identifier ivis-antes-v14-v15 --query "DBSnapshots[].[Status,PercentProgress]" --output text
```

Pontos de volta existentes em 16/11/2026: `/opt/ivis/.env.bak-a7082c5`, o instantâneo `ivis-antes-v14-v15` e as imagens `a7082c5` no ECR. **Atenção:** a imagem antiga pode **não** funcionar com o banco já na migração V15; para voltar de verdade, é preciso restaurar o instantâneo. O instantâneo pode gerar custo **[A CONFERIR]**.

## 10. Verificação

```powershell
curl.exe -s -o NUL -w "HTTP %{http_code}`n" https://ivis-ia.duckdns.org/api/v1/pronto
curl.exe -s https://ivis-ia.duckdns.org/api/v1/pronto
```

Esperado: HTTP 200 e o corpo do `/pronto` informando o estado (inclusive a indisponibilidade da IA, se a `NVIDIA_API_KEY` não estiver definida). Depois, na interface: login, cadastro e uma funcionalidade com IA. Na EC2 (padrão da seção 5): os quatro contêineres `healthy`, `RestartCount` do backend sem crescer e o log do Flyway com as migrações aplicadas.

## 11. Segredos

- Os segredos ficam no Infisical (projeto `ivis`, ambiente `prod`): `IVIS_JWT_CHAVE_ASSINATURA`, `IVIS_IA_CHAVE_SERVICO`, `NVIDIA_API_KEY`, `IVIS_BD_SENHA` e `DUCKDNS_TOKEN`.
- No GitHub ficam apenas `INFISICAL_CLIENT_ID` e `INFISICAL_CLIENT_SECRET`, de uma identidade de máquina com Universal Auth (`ivis-github-actions`). O Client Secret foi rotacionado em 17/11/2026.
- As três credenciais temporárias do laboratório **ainda não estão** no Infisical **[A FAZER, item 5 do plano]**.
- O repositório é público: `.gitignore` bloqueia `.env`, `*.tfstate*`, `*.tfvars` e chaves; o `gitleaks` roda no CI e no histórico.

## 12. Operar o laboratório (parar, encerrar e retomar)

Comportamento **observado pelo grupo em 15 e 16/11**:

- **End Lab** para as EC2 e **não** para o RDS. **Start Lab** religa as EC2 sozinhas, com o IP elástico mantido.
- Se o RDS for parado **à mão**, ele **não** volta com o Start Lab: é preciso `aws rds start-db-instance` (3 a 6 minutos até `available`). Um RDS parado volta sozinho após 7 dias **[A CONFERIR]**.
- Sem o banco, o backend reinicia em ciclo (55 reinícios medidos) até o banco ficar disponível. O `/pronto` responde cerca de 2 minutos depois de o banco ficar `available`.
- Com o RDS ligado, as EC2 voltam em cerca de 1 minuto e o `/pronto` em até cerca de 10 minutos. O compose de produção sobe sozinho (`restart: unless-stopped`).

Parar (para economizar crédito):

```powershell
aws ec2 stop-instances --instance-ids <id da EC2 app> <id da EC2 de monitoramento> --query "StoppingInstances[].[InstanceId,CurrentState.Name]" --output table
aws rds stop-db-instance --db-instance-identifier ivis-banco --query "DBInstance.DBInstanceStatus" --output text
```

Retomar (nesta ordem, com o RDS **primeiro**):

```powershell
aws rds start-db-instance --db-instance-identifier ivis-banco --query "DBInstance.DBInstanceStatus" --output text
Get-Date; aws rds describe-db-instances --db-instance-identifier ivis-banco --query "DBInstances[].DBInstanceStatus" --output text
aws ec2 describe-instances --filters "Name=tag:Name,Values=ivis-app,ivis-monitoramento" --query "Reservations[].Instances[].[InstanceId,State.Name,PublicIpAddress]" --output table
aws ssm describe-instance-information --query "InstanceInformationList[].[InstanceId,PingStatus]" --output table
```

Depois, `/pronto` com HTTP 200 e o fluxo completo. O IP público da EC2 de monitoramento muda a cada ligada.

## 13. Lições aprendidas

- **`docker login` com pipe no PowerShell 5.1** dá `400 Bad Request`: usar `cmd /c "aws ecr get-login-password ... | docker login ..."`.
- **Aspas no PowerShell:** `\"` dentro de aspas duplas não escapa; usar aspas simples externas.
- **Sessão SSM interativa:** não colar blocos de várias linhas; a colagem criou arquivos em lugares errados (apareceram `Caddyfile` e `docker-compose.yml` em `/usr/bin` **[A CONFERIR: se ainda existem]**). Usar `send-command` com arquivo JSON.
- **Terminal do VS Code:** injeta o `.env` do projeto e quebra testes; rodar AWS, Terraform e `gradlew test` fora dele. Gravar arquivos com LF.
- **Nome do contêiner do backend:** `ivis-backend` (sem `-1`).
- **`grep` de logs em JSON:** as aspas quebram o filtro de `"level":"ERROR"` dentro do JSON; usar `.` no lugar das aspas (o backend registra o campo `level`).
- **Antes de migrações** que não se desfazem trocando a imagem, criar o instantâneo do RDS.
- **O teste do CI barrou uma regressão:** a regra nova de habilidades (única por nome e nível, migração V15) mudou o nome de um método do repositório e um teste unitário falhou; a correção foi a solicitação #28. É o exemplo para a seção "como a falha de teste bloqueia a implantação".
- **Correção de um roteiro anterior:** o End Lab **não** para o RDS; ele só não volta sozinho se o grupo o parar à mão.

## 14. Limitações conhecidas

- **Segredos pelo `send-command`:** na implantação manual de 15 e 16/11, os valores usados para gerar o `.env` passaram pelos parâmetros do `send-command`. A meta é gerar o `.env` na EC2 a partir do Infisical **[A FAZER]**.
- **E-mail de recuperação de senha:** não há SMTP real em produção. O backend registra o erro de envio e responde de forma neutra ao usuário. O Mailpit só existe no plano B local.
- **Credenciais do laboratório:** temporárias, renovadas a cada sessão; não há solução automática além do Infisical.
- **Ponto único de falha:** uma só EC2 app, sem balanceador (decisão registrada no ADR-0012).
- **Imagem do Caddy sem versão fixa** (`caddy:2-alpine`).
- **Volumes do Caddy** com nome herdado do ambiente de teste e marcados como externos.
- **Conta do laboratório** encerra ao fim do curso: guardar evidências e o vídeo da demonstração.

## 15. Remover os recursos **[A VALIDAR]**

O enunciado pede a remoção dos recursos. O ciclo `destroy` e `apply` ainda **não foi validado** nesta infraestrutura e não deve ser iniciado sem revisão do plano pelo coordenador.

```powershell
cd infra
terraform plan "-destroy" "-out=destruir.tfplan"
```

Revisar o plano com o coordenador e só então `terraform apply "destruir.tfplan"`. Pontos de atenção:

- **O banco será apagado**, junto com os dados. O RDS é recriado vazio e o Flyway reaplica as migrações.
- **O IP elástico muda** ao recriar: atualizar o DuckDNS (seção 7.1).
- **O certificado do Let's Encrypt é perdido** com a EC2: o Caddy pede um novo (limite de emissões **[A CONFERIR]**).
- **Imagens do ECR:** se o repositório tiver imagens, o `destroy` pode falhar sem a opção de remoção forçada **[A CONFERIR em `infra/ecr.tf`]**.
- **O que o Terraform não remove:** o bucket do estado (criado à mão), instantâneos manuais do RDS (`ivis-antes-v14-v15`) e o subdomínio do DuckDNS. Os instantâneos podem gerar custo **[A CONFERIR]**.
- Depois de recriar, repetir as seções 6, 7 e 8 (construir, preparar a EC2 e implantar) e a verificação da seção 10.

## 16. Evidências desta documentação

Guardar (mascaradas, com retângulo preto, nunca desfoque; originais fora do repositório): `/pronto` com cadeado, `docker ps` com os quatro contêineres `healthy` e a tag, trecho do log do Flyway, ECR com as tags, o instantâneo do RDS, RDS privado (`PubliclyAccessible=False`) e grupos de segurança, `terraform state list`, e o cronômetro e o crédito do painel.
