# ADR-0012 — Hospedagem do IVIS na AWS

- **Status:** Proposto
- **Data:** 2026-11-09
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0007](ADR-0007-simulacao-de-email-com-mailpit.md), [ADR-0011](ADR-0011-autenticacao-do-mvp.md), [`Plano_Implantacao_AWS_Academy.md`](../Plano_Implantacao_AWS_Academy.md)

## Contexto

O backend do IVIS publicado na Vercel sofre partida a frio: após períodos sem uso, a primeira requisição espera a inicialização da aplicação Spring Boot. Além disso, o Projeto Integrador da disciplina de Computação em Nuvem exige implantação na AWS, com infraestrutura como código (Terraform), integração e entrega contínuas, gerenciamento de segredos, testes automatizados, observabilidade e documentação.

O [ADR-0011](ADR-0011-autenticacao-do-mvp.md) define o token de renovação em cookie `HttpOnly`, `Secure` e `SameSite=Strict`, o que exige que frontend e backend sejam servidos pelo mesmo site. O ambiente disponível é o AWS Academy (Learner Lab), região `us-east-1`, cujas credenciais temporárias mudam a cada sessão e cujos serviços permitidos precisam ser confirmados no console da turma.

O [plano de implantação anterior](../Plano_Implantacao_AWS_Academy.md) previa uma única EC2 com Docker Compose e PostgreSQL em volume EBS. Esta decisão o substitui quanto à arquitetura.

## Decisão

Migrar toda a pilha (frontend, backend, API de IA e banco de dados) para a AWS, com a seguinte arquitetura:

- **EC2 "app"** (t3.medium, com IP elástico): executa, em Docker Compose, o proxy reverso Caddy, o frontend (Nginx), o backend e a API de IA. As imagens são publicadas no **Amazon ECR**.
- **Mesma origem:** o Caddy serve `/` (frontend) e `/api` (backend) no mesmo domínio, com HTTPS por DuckDNS e Let's Encrypt, preservando o cookie `ivis_refresh`. Somente as portas 80 e 443 ficam abertas ao público; o acesso administrativo é restrito.
- **Amazon RDS para PostgreSQL** (db.t3.micro), em sub-rede privada, acessível apenas pelo grupo de segurança da EC2 "app".
- **EC2 "monitoramento"** (t3.small): Prometheus (métricas), Loki (logs) e Grafana (painéis). A coleta de logs na EC2 "app" usa o **Grafana Alloy**, pois o Promtail foi descontinuado.
- **Terraform** em `infra/`, com estado remoto em **bucket S3**.
- **GitHub Actions** para verificação, testes, construção das imagens, implantação e verificação automática de disponibilidade após a implantação.
- **Infisical** como fonte dos segredos. Como as credenciais do Learner Lab mudam a cada sessão, elas ficam no Infisical e o GitHub guarda apenas uma credencial fixa de acesso a ele.

**Premissas a validar na etapa 0.3** (ver plano da Fase 0): disponibilidade de RDS, ECR, S3 e SSM no laboratório; classes de instância aceitas pelo RDS; uso de `LabRole` e `LabInstanceProfile`; permanência do IP elástico e retorno das instâncias ao reiniciar o laboratório; consumo de crédito. Se alguma premissa falhar, este ADR será revisto antes da implantação. O status passa a **Aceito** depois dessa validação.

Esta decisão não define alta disponibilidade, política de backup do banco, domínio permanente, envio real de e-mail (o Mailpit segue como no ADR-0007) nem a forma de expor métricas no backend, que serão tratados em decisões posteriores.

## Consequências

### Positivas

- Elimina a partida a frio, pois o backend roda como processo permanente.
- A mesma origem mantém o cookie de renovação conforme o ADR-0011, sem relaxar `SameSite`.
- O banco passa a ser serviço gerenciado e a observabilidade fica separada da aplicação.
- Cobre os requisitos técnicos do enunciado (infraestrutura como código, CI/CD, segredos, testes, observabilidade e documentação).

### Custos e riscos

- Consumo de crédito do laboratório com duas EC2, um RDS e um IP elástico; é preciso acompanhar o gasto.
- O laboratório pode encerrar a sessão e parar recursos; credenciais e IPs podem mudar, e é necessário procedimento de retomada.
- Ponto único de falha na EC2 "app": aceitável para demonstração acadêmica, não para produção.
- DuckDNS é serviço externo gratuito, sem garantia de disponibilidade.
- O backend não expõe métricas hoje; será preciso adicionar suporte a elas antes da observabilidade.
- Mais componentes para configurar e documentar do que no plano anterior.

## Alternativas consideradas

- **Manter a Vercel:** rejeitada, pois a partida a frio é o problema que motivou a migração e a AWS é exigida pela disciplina.
- **EC2 única com PostgreSQL em volume EBS (plano anterior):** mais simples, porém sem serviço gerenciado para os dados e sem separar o monitoramento da aplicação.
- **Frontend em S3 com CDN e backend em outra origem:** rejeitada, pois origens diferentes quebrariam o cookie `SameSite=Strict` do ADR-0011.
- **Balanceador de carga com certificado gerenciado e domínio próprio:** exige domínio e permissões de DNS que o laboratório não oferece, e acrescenta custo; o Caddy com DuckDNS atende à demonstração.
- **Contêineres gerenciados ou funções sem servidor:** fora da lista de serviços do enunciado, com permissões do laboratório não validadas; exigiriam justificativa e aprovação.