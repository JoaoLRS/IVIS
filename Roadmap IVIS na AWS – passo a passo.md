# Roadmap IVIS na AWS – passo a passo

Projeto integrador de Computação em Nuvem (UFRPE) integrado ao projeto de Engenharia de Software · versão consolidada de 05/11/2026

## 1. O que mudou desde a versão anterior

- **Mantemos o repositório atual** (https://github.com/lucifer-son/IVIS ou https://github.com/JoaoLRS/IVIS). Não criamos outro: o enunciado exige histórico Git, IaC, CI/CD e documentação no mesmo repo, e o IVIS já tem código, testes e docs em `etc/`. A Fase 0 foi criada para preparar o repo.
- **Front e back no mesmo domínio.** O cookie de renovação `ivis_refresh` é `SameSite=Strict` e o front usa CSRF por cookie. Front na Vercel + back em `duckdns.org` seria cross-site e quebraria a sessão. Por isso a stack inteira vai para a AWS, com o Caddy servindo `/` (front) e `/api` (back). Isso também elimina CORS e o cold start.
- **Banco no RDS PostgreSQL** (em vez do Neon), para entrar na nota de integração com serviços AWS e para isolar o banco por Security Group.
- **Credenciais do Learner Lab mudam a cada sessão.** Elas ficam no Infisical, e o GitHub guarda só uma credencial fixa (Fase 5).
- **Logs com Grafana Alloy**, não Promtail, que foi descontinuado.
- **Estado do Terraform no S3 com lock nativo** (`use_lockfile`), sem depender de DynamoDB.
- **Checklist de validação do lab no início**, para descobrir cedo o que a sua conta bloqueia.
- **Fase de divisão de tarefas por integrante**, porque o enunciado exige participação e conhecimento de todos.

## 2. Arquitetura-alvo

- **EC2 "app" (t3.medium, Elastic IP)**: Caddy (HTTPS automático via DuckDNS) → container `frontend` (Nginx) em `/` e container `backend` (Spring Boot) em `/api`. O backend fala com o container `api-ia` na rede interna e com o RDS.
- **RDS PostgreSQL (db.t3.micro)**: sem acesso público; o Security Group só aceita a EC2 app.
- **EC2 "monitoramento" (t3.small)**: Prometheus, Grafana e Loki. A EC2 app roda o Grafana Alloy para enviar logs.
- **ECR**: imagens `ivis-backend`, `ivis-api-ia`, `ivis-frontend`. A EC2 puxa pelo `LabInstanceProfile`, sem credencial no host.
- **S3**: estado do Terraform.
- **Infisical**: segredos. **GitHub Actions**: CI/CD.

## 3. Pesos da avaliação e onde cada fase pontua

- Aplicação 25%: Fases 1 e 3
- Serviços AWS 20%: Fases 2 e 3
- Terraform 15%: Fase 2
- Pipeline CI/CD 15%: Fase 4
- Testes e qualidade 10%: Fase 4
- Segurança e segredos 5%: Fase 5
- Observabilidade 5%: Fase 6
- Documentação 5%: Fase 7

## 4. Fase 0 – Repositório, proposta e validação do lab

### 0.1 Preparar o repositório

1. Adicione todos os integrantes e o professor como colaboradores (ou deixe o repo público depois do passo 4).
2. Crie a tag do estado atual: `git tag v1.0-vercel && git push --tags`. É o ponto de retorno se a AWS der problema.
3. Proteja a `main`: exigir PR, exigir 1 aprovação e exigir checks de CI (depois da Fase 4).
4. Rode `gitleaks detect` no histórico completo. Se achar segredo real, troque a credencial; apagar o arquivo não limpa o histórico.
5. Confira o `.gitignore`: `.idea/`, `.vscode/`, `.env`, `*.tfstate*`, `.terraform/`, `*.pem` de chave privada.
6. Crie as pastas `infra/` e `.github/workflows/`.
7. Trabalhem em branches curtas (`infra/terraform`, `ci/pipeline`, `obs/prometheus`) com PR revisado por outra pessoa. Isso gera o histórico que o professor avalia.

### 0.2 Proposta e decisões

1. Escreva a proposta inicial: problema (cold start na Vercel, latência), funcionalidades (CDUs já implementados), arquitetura e tecnologias.
2. Crie `etc/plano/adr/ADR-0012-hospedagem-aws.md` com as decisões: stack inteira na AWS, mesmo domínio por causa do cookie, RDS, EC2 + Caddy em vez de ALB, ECR, Infisical.
3. Leve a proposta à aula de acompanhamento e peça aprovação de qualquer tecnologia fora da lista do enunciado.

### 0.3 Validar o Learner Lab (faça logo)

1. Inicie o lab, abra o console e anote a região (use `us-east-1`).
2. Confirme que existem `LabRole` e `LabInstanceProfile`.
3. Teste se RDS, ECR, S3 e SSM estão disponíveis e quais classes de instância o RDS aceita.
4. Pare e reinicie o lab uma vez e observe: as EC2 voltam sozinhas? O Elastic IP permanece? Isso define o checklist da demo.
5. Verifique o crédito disponível (Vocareum) e anote o consumo diário.

## 5. Fase 1 – Preparar a aplicação

1. **Métricas no backend**: adicione `spring-boot-starter-actuator` e `micrometer-registry-prometheus` ao `build.gradle.kts`. Em `application-prod.yml`, use `management.server.port=9091` e exponha só `health,prometheus`. A porta fica fora do Caddy e liberada só para a EC2 de monitoramento.
2. **Logs**: padronize a saída (JSON ou linha com `idRequisicao`, que o filtro atual já gera) e registre eventos relevantes e erros.
3. **CORS**: tire as URLs da Vercel dos valores padrão do `application.yml`. Em produção o acesso é same-origin.
4. **Perfil de produção**: revise o pool Hikari (hoje com 2 conexões, pensado para serverless) e o `lazy-initialization`. Em EC2 persistente, inicializar tudo no start detecta erros cedo.
5. **Cookie**: `IVIS_COOKIE_SEGURO=true` em produção.
6. **Frontend**: build com `VITE_API_URL=/api` para chamadas same-origin.
7. **E-mail de recuperação de senha**: o Mailpit não serve em produção. Decida entre documentar como limitação da demo ou usar um SMTP real.
8. **`docker-compose.prod.yml`** (em `deploy/`): sem Mailpit e sem Postgres local; imagens do ECR; `restart: unless-stopped`; `IVIS_BD_URL` apontando para o RDS; segredos vindos de `.env` gerado no host; rede interna entre backend e api-ia.
9. Teste o conjunto localmente antes de ir para a AWS.

## 6. Fase 2 – Terraform (pasta `infra/`)

1. Estrutura: `versions.tf`, `main.tf`, `network.tf`, `security.tf`, `compute.tf`, `database.tf`, `ecr.tf`, `variables.tf`, `outputs.tf`, `README.md`.
2. Crie à mão (uma vez) o bucket S3 do estado e configure o backend:

```
terraform {
  required_version = ">= 1.10"
  backend "s3" {
    bucket       = "ivis-tfstate-<sufixo-unico>"
    key          = "ivis/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true
  }
}
```

3. **Rede e Security Groups**:
   - app: 80 e 443 de qualquer origem; 22 só dos IPs do grupo;
   - RDS: 5432 só do SG da app;
   - monitoramento: 3000 (Grafana) só dos IPs do grupo; 9091 da app liberado para o SG de monitoramento.
4. **EC2 app e monitoramento**: AMI Ubuntu ou Amazon Linux 2023, `iam_instance_profile = "LabInstanceProfile"`, `user_data` que instala Docker, Compose e o agente SSM.
5. **Elastic IP** associado à EC2 app.
6. **RDS**: `db.t3.micro`, PostgreSQL, `publicly_accessible = false`, senha vinda de variável sensível (nunca em arquivo versionado).
7. **ECR**: um repositório por imagem, com `scan_on_push`.
8. **Outputs**: IP público, endpoint do RDS, URLs do ECR.
9. Credenciais da AWS entram por variáveis de ambiente (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) copiadas do painel do lab. Nunca em arquivo.
10. Valide o ciclo `init` → `plan` → `apply` → `destroy` → `apply`. O enunciado pede remoção dos recursos.

## 7. Fase 3 – Deploy manual de validação

Faça uma vez à mão, para só depois automatizar.

1. Crie o subdomínio no DuckDNS apontando para o Elastic IP.
2. Na EC2 app, crie o `Caddyfile`:

```
ivis.duckdns.org {
    handle /api/* { reverse_proxy backend:8081 }
    handle { reverse_proxy frontend:80 }
}
```

3. Faça login no ECR da EC2, baixe as imagens e suba o compose de produção.
4. Teste `https://ivis.duckdns.org/api/v1/pronto`, depois login, cadastro, uma funcionalidade com IA e a renovação de sessão (espere o token expirar ou force).
5. Corrija o que falhar e **anote cada comando em `docs/deploy.md`**; ele vira a base do pipeline.

## 8. Fase 4 – CI/CD (GitHub Actions)

### 8.1 `ci.yml` (em todo PR e push)

1. Frontend: instalar, ESLint e Vitest.
2. api-ia: instalar dependências e pytest.
3. Backend: `./gradlew test` (o runner tem Docker, então Testcontainers funciona).
4. Terraform: `fmt -check` e `validate`.
5. Segurança: gitleaks.
6. Publique relatórios de teste como artefato do job.

### 8.2 `deploy.yml` (push na `main`)

Fluxo em jobs encadeados, cada um com `needs` do anterior:

```
test  →  build-push (ECR)  →  deploy (SSM)  →  verify
```

1. **test**: reaproveita o `ci.yml` (`workflow_call`). Se falhar, nada segue. Documente isso, pois o enunciado pede explicar como os testes influenciam a implantação.
2. **build-push**: obtém as credenciais da AWS no Infisical, faz login no ECR e envia as 3 imagens com tag igual ao SHA do commit.
3. **deploy**: usa SSM Run Command na EC2 app para atualizar a tag, rodar `docker compose pull` e `up -d`. Se o SSM não funcionar no seu lab, use SSH com chave guardada no Infisical.
4. **verify**: `curl --retry 10 --retry-delay 6 --fail https://ivis.duckdns.org/api/v1/pronto`. Se falhar, o job falha e o pipeline volta para a tag anterior.
5. Terraform fica em workflow separado e manual (`workflow_dispatch`: plan, revisão, apply).
6. Depois de o CI funcionar, ative nas regras da `main` os checks obrigatórios.

Observação: o Cypress contra produção é arriscado porque as tarefas de limpeza do projeto acessam o banco diretamente. Use-o localmente e mantenha um smoke test de API no pipeline.

## 9. Fase 5 – Segurança e segredos

1. Crie um projeto no Infisical com o ambiente `prod`.
2. Cadastre: `IVIS_JWT_CHAVE_ASSINATURA`, `IVIS_IA_CHAVE_SERVICO`, `NVIDIA_API_KEY`, senha do RDS e as 3 credenciais temporárias do Learner Lab.
3. No GitHub, guarde **apenas** o `client id` e o `client secret` de uma machine identity do Infisical.
4. A cada sessão do lab, atualize as 3 credenciais da AWS **só no Infisical**. O pipeline as busca em tempo de execução.
5. Na EC2, um script no boot ou no deploy busca os segredos e gera o `.env` com `chmod 600`.
6. Evidências para a avaliação: `.gitignore`, gitleaks passando, segredos mascarados nos logs do Actions, SG do RDS fechado e porta 22 restrita.

## 10. Fase 6 – Observabilidade

1. Na EC2 de monitoramento, suba Prometheus, Grafana e Loki por compose.
2. Prometheus: `scrape_config` para `<ip-da-app>:9091/actuator/prometheus`. Para CPU/memória do host, adicione node\_exporter ou Telegraf.
3. Na EC2 app, rode o Grafana Alloy coletando logs dos containers e enviando ao Loki.
4. No Grafana, configure Prometheus e Loki como data sources e monte um dashboard com: requisições por segundo, latência p95, taxa de erros 5xx, disponibilidade (`up`), CPU/memória e painel de logs de erro.
5. Salve os dashboards como JSON em `observability/` no repo.
6. Opcional: alerta simples para `up == 0`.
7. Garanta que o Grafana seja acessível só pelos IPs do grupo.

## 11. Fase 7 – Documentação e demonstração

1. README principal: visão geral, diagrama da arquitetura, tecnologias e **justificativa de cada uma**.
2. `docs/deploy.md`: como executar, implantar e **remover** (`terraform destroy`).
3. Seção de segurança: segredos, Security Groups, cookies, HTTPS.
4. Seção de testes: o que roda, quando, e como a falha bloqueia o deploy.
5. Seção de observabilidade: como acessar os dashboards e o que cada painel mostra.
6. Tabela de divisão de tarefas por integrante.
7. Evidências: prints de pipeline verde, `terraform apply`, dashboards e a aplicação em HTTPS.
8. Ensaie a demo completa: alteração pequena → PR → CI verde → merge → deploy → `/pronto` → app funcionando → métricas e logs no Grafana.

## 12. Checklist antes de qualquer demonstração

1. Iniciar o lab e atualizar as credenciais no Infisical.
2. Confirmar que EC2 app, EC2 de monitoramento e RDS estão de pé.
3. Abrir `https://ivis.duckdns.org/api/v1/pronto`.
4. Fazer login e uma operação completa no sistema.
5. Abrir o Grafana e conferir métricas e logs recentes.
6. Ter o último pipeline verde à mão.
7. Plano B: gravação ou prints da demonstração.

## 13. Divisão sugerida entre 4 integrantes

Todos precisam conhecer a arquitetura completa; a divisão abaixo é só a responsabilidade principal.

- **Pessoa A – Infraestrutura**: Fases 0.3, 2 e 3.
- **Pessoa B – CI/CD e segredos**: Fases 4 e 5.
- **Pessoa C – Aplicação**: Fase 1 (backend, front, compose de produção) e testes.
- **Pessoa D – Observabilidade e documentação**: Fases 6 e 7.

**Aqui precisamos decidir para saber como ficará as questões de commits**

## 14. Riscos e mitigação

- **Reinício do lab não religa as instâncias**: documentar o checklist da seção 12 e usar `restart: unless-stopped`.
- **Serviço bloqueado no seu lab (RDS, ECR, SSM)**: descobrir na Fase 0.3. Alternativas: Postgres em container com volume EBS, GHCR no lugar do ECR, SSH no lugar do SSM. Justifique no ADR.
- **Crédito acabando**: desligar monitoramento e RDS quando não estiver em uso e acompanhar o consumo semanalmente.
- **Memória apertada na t3.medium**: acompanhar o uso e ajustar `JAVA_OPTS` e o tamanho da instância.
- **Credencial do lab expirada no meio do pipeline**: sempre atualizar o Infisical antes de rodar workflows.
- **Segredo vazado no histórico**: gitleaks na Fase 0 e no CI.
- **Conta do lab encerrada ao fim do curso**: guardar prints e vídeo da demonstração, e manter `docs/` no repo.
