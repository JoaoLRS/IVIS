# Observabilidade do IVIS

Este diretório reúne o que o IVIS usa para **medir** (métricas), **registrar** (logs) e **visualizar**
(painéis) o funcionamento do sistema. Tudo roda em contêineres Docker e pode ser testado
localmente, sem a AWS.

## 1. O que é coletado e por quê

| O quê | Como | Para quê |
|---|---|---|
| **Métricas** do backend (requisições, tempo de resposta, erros 5xx, memória, CPU, disponibilidade) | O backend expõe `/actuator/prometheus` na porta **9091**; o Prometheus consulta essa porta a cada 15 s | Saber se o sistema está no ar, quão rápido responde e quanto recurso consome |
| **Logs** dos contêineres (em especial do backend: operações e erros) | O Alloy lê os logs dos contêineres pelo Docker e os envia ao Loki | Investigar o que aconteceu em uma falha (por exemplo, um `401` de login ou um `503`) |
| **Visualização** | O Grafana lê o Prometheus e o Loki | Mostrar métricas e logs em um único painel |

Esses três itens atendem ao enunciado do projeto: métricas, logs e visualização.

## 2. Arquitetura

```
EC2 app                                    EC2 monitoramento
 backend :9091  <---- coleta (Prometheus) ---- Prometheus
 contêineres  --- Alloy --- envia logs --->    Loki :3100
                                               Grafana (lê Prometheus e Loki)
```

No uso local, tudo roda na mesma máquina.

## 3. Arquivos deste diretório

| Arquivo | Função |
|---|---|
| `docker-compose.monitoramento.yml` | Prometheus, Loki e Grafana |
| `docker-compose.alloy.yml` | Alloy (envia os logs dos contêineres ao Loki); roda na EC2 app |
| `prometheus/prometheus.yml` | Alvo da coleta de métricas |
| `loki/loki-config.yml` | Loki em um único nó, com retenção de 72 h |
| `alloy/config.alloy` | Descoberta dos contêineres e envio ao Loki |
| `grafana/provisioning/` | Fontes de dados (Prometheus e Loki) e carregamento do painel |
| `grafana/dashboards/ivis-visao-geral.json` | O painel "IVIS – Visão geral" |
| `.env.exemplo` | Modelo das variáveis (copie para `.env`) |

## 4. Como subir localmente

Pré-requisito: Docker Desktop aberto. Use um terminal PowerShell na raiz do projeto.

**1. Suba o IVIS** (gera os logs que o Alloy vai coletar):

```powershell
.\release.ps1 up
```

**2. Suba o monitoramento** (Prometheus, Loki e Grafana):

```powershell
cd observability
Copy-Item .env.exemplo .env
docker compose -f docker-compose.monitoramento.yml up -d
docker compose -f docker-compose.monitoramento.yml ps
curl.exe http://localhost:3100/ready
```

O `ready` pode responder `Ingester not ready` nos primeiros segundos; espere e repita até aparecer `ready`.

**3. Suba o Alloy** (envio dos logs):

```powershell
$env:LOKI_URL = "http://host.docker.internal:3100/loki/api/v1/push"
docker compose -f docker-compose.alloy.yml up -d
```

**4. Acesse:**

- Grafana: `http://localhost:3000` (usuário `admin`; a senha é a `GRAFANA_SENHA` do seu `.env`).
  Abra **Dashboards → IVIS → IVIS – Visão geral**. As duas fontes de dados já estão cadastradas.
- Prometheus: `http://localhost:9090/targets`.
- Logs: no Grafana, em **Explore**, fonte **Loki**, consulta `{container="ivis-backend"}`.

**Para gerar dados**, use o site em `http://localhost:8080` (um login com senha errada, por exemplo, gera um `401` nos logs).

### Métricas e logs locais não aparecem juntos

O backend em contêiner usa o perfil `dev`, que **não** expõe a porta 9091. Por isso, com `.\release.ps1 up`,
o alvo `ivis-backend` fica **down** no Prometheus e os painéis de métricas ficam vazios, mas os **logs** funcionam.

Para ver as **métricas** localmente, rode o backend na sua máquina com o perfil `prod` (ele expõe a 9091):

```powershell
docker compose stop ivis-backend
Get-Content .env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object { $p = $_ -split '=',2; Set-Item -Path ("Env:" + $p[0].Trim()) -Value $p[1].Trim() }
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:IVIS_BD_URL = "jdbc:postgresql://localhost:5433/ivis"
$env:PORT = "8081"
cd backend
.\gradlew.bat bootRun
```

Nesse modo, o backend não roda em contêiner e o Alloy não enxerga os logs dele. Em resumo:
**logs** funcionam com o backend em contêiner (`dev`); **métricas** funcionam com o backend em `prod`.
Na AWS (perfil `prod` em contêiner), os dois funcionam juntos.

### Para encerrar

```powershell
docker compose -f docker-compose.alloy.yml down
docker compose -f docker-compose.monitoramento.yml down
cd ..
.\release.ps1 down
```

Para apagar também os dados do Prometheus, do Loki e do Grafana, acrescente `-v` ao `down` do monitoramento.

## 5. Como acessar na AWS

O Grafana **não tem acesso público**. O acesso é por túnel do AWS Systems Manager (SSM), feito pelo Rafael,
sem portas abertas à internet. O comando final de acesso é fornecido por ele.

Na AWS, as variáveis `IP_APP` (IP privado da EC2 app, usado pelo Prometheus) e `LOKI_URL`
(endereço de envio do Loki, usado pelo Alloy) são definidas na implantação, e não neste repositório.

## 6. O que mostra cada painel

O painel **IVIS – Visão geral** tem quatro seções.

**Disponibilidade e tráfego**

- *Disponibilidade do backend*: "No ar" quando o Prometheus consegue coletar as métricas do backend.
- *Requisições por segundo*: total de requisições HTTP atendidas por segundo.

**Tempo de resposta e erros**

- *Tempo de resposta (p95)*: tempo dentro do qual 95% das requisições são respondidas.
- *Erros 5xx por segundo*: respostas com erro de servidor por segundo; fica em zero quando não há erros.

**Recursos**

- *Memória da aplicação (heap)*: memória da JVM em uso pelo backend.
- *CPU do processo*: fração de CPU consumida pelo processo do backend.

**Logs**

- *Logs de erro do backend (respostas 4xx e 5xx)*: linhas de log com resposta de erro.

## 7. Decisões e justificativas

- **Prometheus** coleta as métricas por consulta (*pull*) à porta 9091, que fica fora do Caddy e só é
  alcançável de dentro da rede (na AWS, o grupo de segurança só a abre para a EC2 de monitoramento).
- **Loki + Alloy** coletam os logs. O Alloy substitui o Promtail, que foi descontinuado.
- **Retenção curta**: 3 dias no Prometheus e 72 h no Loki, porque a EC2 de monitoramento tem pouca memória
  (2 GiB). No Loki, a retenção só apaga dados porque o compactador está habilitado, e ele só começa a
  limpar cerca de 10 minutos depois de iniciar.
- **Portas locais**: o Prometheus e o Grafana ficam presos a `127.0.0.1`. O Loki publica a 3100 para a rede,
  de propósito, porque na AWS o Alloy envia os logs de outra máquina; quem restringe a origem é o grupo de
  segurança.
- **Socket do Docker somente leitura**: o Alloy precisa dele para descobrir os contêineres. É um risco
  aceito: quem controlar o Alloy enxerga os contêineres da máquina. Ele só lê logs e não publica portas.
- **Filtro de erros por código HTTP**: o backend registra as falhas com nível `INFO`
  (por exemplo, `POST /api/v1/sessoes -> 401`), então a consulta de erros filtra pelo código (`-> 4xx/5xx`)
  em vez de pelo nível do log.

Consumo de memória medido localmente com `docker stats` (valores de uma máquina de desenvolvimento, sem
carga real): Grafana cerca de 514 MiB, Loki cerca de 75 MiB, Alloy cerca de 61 MiB e Prometheus cerca de 24 MiB.

## 8. Limitações

- Não há métricas do servidor inteiro (CPU e memória da máquina), apenas do backend.
- O gráfico de p95 depende do histograma de tempo de resposta, que o backend habilita no perfil `prod`.
- O Grafana não tem acesso público.
- Localmente, métricas e logs não aparecem ao mesmo tempo (veja a seção 4).
- Em `dev`, os logs são texto; em `prod`, JSON. Consultas que dependem de campos JSON só funcionam em `prod`.
