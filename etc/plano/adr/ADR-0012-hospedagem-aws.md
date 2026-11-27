# ADR-0012 — Hospedagem do IVIS na AWS

- **Status:** Aprovado
- **Data:** 2026-11-09
- **Atualizado em:** 2026-11-23 (ver "Atualização de 23/11/2026" ao final)
- **Decisores:** Equipe IVIS
- **Relacionados:** [ADR-0007](ADR-0007-simulacao-de-email-com-mailpit.md), [ADR-0011](ADR-0011-autenticacao-do-mvp.md), [`Plano_Implantacao_AWS_Academy.md`](../Plano_Implantacao_AWS_Academy.md), [`Guia_Implantacao_AWS.md`](../Guia_Implantacao_AWS.md)

## Contexto

O backend do IVIS publicado na Vercel sofre partida a frio: após períodos sem uso, a primeira requisição espera a inicialização da aplicação Spring Boot. Além disso, o Projeto Integrador da disciplina de Computação em Nuvem exige implantação na AWS, com infraestrutura como código (Terraform), integração e entrega contínuas, gerenciamento de segredos, testes automatizados, observabilidade e documentação.

O [ADR-0011](ADR-0011-autenticacao-do-mvp.md) define o token de renovação em cookie `HttpOnly`, `Secure` e `SameSite=Strict`, o que exige que frontend e backend sejam servidos pelo mesmo site. O ambiente disponível é o AWS Academy (Learner Lab), região `us-east-1`, cujas credenciais temporárias mudam a cada sessão e cujos serviços permitidos precisam ser confirmados no console da turma.

O [plano de implantação anterior](../Plano_Implantacao_AWS_Academy.md) previa uma única EC2 com Docker Compose e PostgreSQL em volume EBS. Esta decisão o substitui quanto à arquitetura.

## Decisão

Migrar toda a pilha (frontend, backend, API de IA e banco de dados) para a AWS, com a seguinte arquitetura:

- **EC2 "app"** (t3.medium, com IP elástico): executa, em Docker Compose, o proxy reverso Caddy, o frontend (Nginx), o backend e a API de IA. As imagens são publicadas no **Amazon ECR**.
- **Mesma origem:** o Caddy serve `/` (frontend) e `/api` (backend) no mesmo domínio, com HTTPS por DuckDNS e Let's Encrypt, preservando o cookie `ivis_refresh`. Somente as portas 80 e 443 ficam abertas ao público; o acesso administrativo é feito pelo AWS Systems Manager (SSM), sem a porta 22 aberta.
- **Amazon RDS para PostgreSQL** (db.t3.micro), em sub-rede privada, acessível apenas pelo grupo de segurança da EC2 "app".
- **EC2 "monitoramento"** (t3.small): Prometheus (métricas), Loki (logs) e Grafana (painéis). A coleta de logs na EC2 "app" usa o **Grafana Alloy**, pois o Promtail foi descontinuado.
- **Terraform** em `infra/`, com estado remoto em **bucket S3**.
- **GitHub Actions** para verificação, testes, construção das imagens, implantação e verificação automática de disponibilidade após a implantação.
- **Infisical** como fonte dos segredos. Como as credenciais do Learner Lab mudam a cada sessão, elas ficam no Infisical e o GitHub guarda apenas uma credencial fixa de acesso a ele.

**Validação (etapa 0.3):** as premissas foram validadas em 09 e 10/11/2026 e estão registradas em [`Validacao_Learner_Lab.md`](../Validacao_Learner_Lab.md). Todas se confirmaram: RDS, ECR, S3 e SSM disponíveis; RDS db.t3.micro com PostgreSQL 17.x em sub-rede privada; uso de `LabRole` e `LabInstanceProfile`; permanência do IP elástico e retorno das EC2 ao reiniciar o laboratório. O estado remoto no S3 com travamento por arquivo (`use_lockfile`, Terraform 1.11 ou superior) dispensa o DynamoDB. O consumo de crédito foi estimado pela tabela de preços, pois o painel do laboratório não permite medi-lo. Os achados que não invalidam a decisão estão em Consequências. Por isso o status é **Aprovado**.

Esta decisão não define alta disponibilidade, política de backup do banco, domínio permanente, envio real de e-mail (o Mailpit segue como no ADR-0007) nem a forma de expor métricas no backend, que serão tratados em decisões posteriores.

## Consequências

### Positivas

- Elimina a partida a frio, pois o backend roda como processo permanente.
- A mesma origem mantém o cookie de renovação conforme o ADR-0011, sem relaxar `SameSite`.
- O banco passa a ser serviço gerenciado e a observabilidade fica separada da aplicação.
- Cobre os requisitos técnicos do enunciado (infraestrutura como código, CI/CD, segredos, testes, observabilidade e documentação).

### Custos e riscos

- Consumo de crédito do laboratório com duas EC2, um RDS e um IP elástico; é preciso acompanhar o gasto.
- O Start Lab religa toda EC2 parada, e o RDS não é parado pelo laboratório; os dois consomem crédito o tempo todo. Regra: apagar o que não estiver em uso (`terraform destroy`) em vez de parar.
- As credenciais mudam a cada sessão e o encerramento por esgotamento do tempo não foi testado. O IP elástico persistiu nos ciclos testados, mas é necessário um procedimento de retomada (Start Lab, esperar cerca de 3 minutos e verificar a saúde); o Docker e os contêineres precisam subir sozinhos no boot.
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

## Atualização de 23/11/2026

Esta seção registra o que se confirmou ou mudou na implantação. A decisão acima continua valendo; nada do texto anterior foi alterado.

### Confirmado

- Pilha inteira na AWS, mesma origem por causa do cookie `ivis_refresh`, RDS PostgreSQL privado, EC2 "app" com Caddy (em vez de balanceador de carga), ECR, Terraform com estado no S3, GitHub Actions e Infisical, tal como decidido.
- Em 11/11/2026, o professor aprovou, no acompanhamento do projeto, as tecnologias fora da lista do enunciado: Caddy, DuckDNS, Let's Encrypt e Grafana Alloy.
- O acesso administrativo e a implantação usam o SSM, sem a porta 22 aberta.
- A observabilidade (Prometheus, Loki, Grafana e Alloy) foi implantada e verificada em 22/11/2026. O Grafana não tem porta pública: o acesso é por túnel SSM.
- O Ansible e o balanceador de carga ficaram fora da entrega; as razões estão na proposta inicial.

### Mudou em relação ao texto original

- **Métricas:** o backend passou a expor métricas Prometheus na porta 9091 (Actuator e Micrometer), fora do Caddy e liberada apenas para o grupo de segurança de monitoramento. O item "o backend não expõe métricas hoje" deixou de valer.
- **Operação do laboratório:** a regra "apagar o que não estiver em uso (`terraform destroy`) em vez de parar" **não foi adotada**. O grupo parou o RDS e as EC2 à mão para poupar crédito, porque recriar a infraestrutura apagaria o banco (sem instantâneo final), perderia as imagens do ECR, mudaria o IP elástico e o certificado, e exigiria refazer à mão o `.env`, o compose, o `Caddyfile`, os volumes, as imagens e a observabilidade.
- **Comportamento medido:** o End Lab não para o RDS; um RDS parado à mão não volta com o Start Lab e exige `aws rds start-db-instance`; as EC2 voltam sozinhas e o compose de produção sobe sozinho.
- **Banco:** sem backups automáticos (`BackupRetentionPeriod = 0`) e sem instantâneo final na remoção (`skip_final_snapshot = true`); o único ponto de volta é um instantâneo manual. Isso define, na prática, a política que o texto original deixou em aberto.
- **E-mail:** o Mailpit só existe no plano local. Em produção não há SMTP real: a recuperação de senha responde de forma neutra e o e-mail não é entregue (limitação da demonstração).
- **Segredos:** o Infisical usa uma identidade de máquina com acesso de leitura; no GitHub ficam apenas o identificador e o segredo dessa identidade. As credenciais do laboratório estão no Infisical e precisam ser atualizadas a cada Start Lab. A ação usada no pipeline exporta todos os segredos do ambiente `prod` para o job (melhoria prevista: pasta própria para as credenciais da AWS).

### Riscos e limitações que permanecem

- O ciclo `terraform destroy` e `apply` **não foi validado**. Em 22/11 foi gerado o plano de remoção (31 recursos a remover, sem aplicar). A recomendação para a entrega é não executá-lo, pelos motivos acima, e entregar o plano e o procedimento documentados.
- O pipeline não gera o `.env`, não copia o compose, o `Caddyfile` nem `observability/`, e não cria os volumes do Caddy; a reversão para a versão anterior é manual.
- O Alloy monta o socket do Docker em modo somente leitura, que protege só o arquivo do socket; o risco foi aceito.
- A imagem do Caddy não tem versão fixa e há uma única EC2 "app", sem balanceador.
