# API de IA do IVIS

Serviço Python interno do IVIS. Implementa o contrato v1 com uma interface de
provedor, um stub determinístico para gerar perguntas e avaliações, validação
das saídas antes de responder, autenticação entre serviços, liveness e readiness.
O stub não usa nem exige credenciais NVIDIA.

## Executar localmente

Requer Python 3.12 ou 3.13. Crie um ambiente virtual e instale as versões
fixadas:

```powershell
Set-Location api-ia
py -3.13 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:IVIS_IA_CHAVE_SERVICO = "substitua-por-uma-chave-local-com-pelo-menos-32-caracteres"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --app-dir . --host 127.0.0.1 --port 8000
```

Ou, a partir deste diretório, carregue as variáveis de um `.env` local criado
com base em `.env.example` antes de iniciar o Uvicorn. O serviço não carrega
arquivos de ambiente automaticamente: as variáveis devem ser fornecidas pelo
ambiente de execução.

- Liveness: `GET http://localhost:8000/saude`
- Readiness: `GET http://localhost:8000/pronto`
- Rotas de negócio v1: `POST /v1/perguntas/gerar` e `POST /v1/avaliacoes/gerar`

As rotas de negócio exigem `X-Chave-Servico`. A variável
`IVIS_IA_CHAVE_SERVICO` é obrigatória e deve ser diferente da chave NVIDIA.
As avaliações do stub são demonstrativas: a nota depende somente do tamanho das
respostas, não da sua qualidade técnica.

As rotas delegam a geração ao contrato interno `ProvedorIA`. O adaptador stub é
o único provedor ativo; desativar `IVIS_IA_USAR_STUB` retorna
`503/provedor_indisponivel`. Saídas dos provedores são validadas contra os
schemas v1 e uma saída incompatível resulta em `502/saida_invalida`. O adaptador
NVIDIA ainda não foi implementado e nenhuma chamada externa é feita pelo serviço.

O backend acessa estas rotas pela rede interna do Compose (`http://ivis-api-ia:8000`).
O frontend não deve chamar este serviço diretamente; para iniciar e conduzir um
treino, use as rotas do backend `/api/v1/ia/perguntas/gerar` e
`/api/v1/ia/avaliacoes/gerar`.

## Testes

```powershell
Set-Location api-ia
$env:IVIS_IA_CHAVE_SERVICO = "chave-local-com-pelo-menos-32-caracteres"
.\.venv\Scripts\python.exe -m pytest
```

## Marco 6: medição e demonstração

O roteiro de integração local, protocolo de avaliação humana e limitações das
medições estão em [DEMONSTRACAO_MARCO_6.md](DEMONSTRACAO_MARCO_6.md). Para medir
latência com payloads sintéticos, inicie primeiro o serviço e execute:

```powershell
Set-Location api-ia
$env:IVIS_IA_CHAVE_SERVICO = "chave-local-com-pelo-menos-32-caracteres"
.\.venv\Scripts\python.exe scripts\medir_desempenho.py
```

O script mede a API de IA por padrão. Use `--camada backend
--base-url http://127.0.0.1:8081/api` para medir a rota integrada. A medição
não avalia qualidade semântica; o stub não representa desempenho ou qualidade
do NIM. Os alvos p95 do relatório são propostas, não um SLA aprovado.

## Configuração

| Variável | Obrigatória | Uso |
|---|---|---|
| `IVIS_IA_CHAVE_SERVICO` | Sim | Chave interna do backend, com no mínimo 32 caracteres. |
| `IVIS_IA_USAR_STUB` | Não (`true`) | Ativa o provedor stub; desativado, as operações retornam `503/provedor_indisponivel`. |
| `IVIS_IA_STUB_FALHA` | Não (`nenhuma`) | Simula falha do stub: `nenhuma`, `indisponivel` (`503`) ou `tempo_esgotado` (`504`). Não é um parâmetro das requisições. |
| `NVIDIA_API_KEY` | Não | Reservada para o adaptador NVIDIA de marco posterior. |
| `NVIDIA_MODEL` | Não | Reservada para o adaptador NVIDIA de marco posterior. |
| `NVIDIA_BASE_URL` | Não | Endpoint base reservado para o adaptador NVIDIA. |
| `NVIDIA_TIMEOUT_SEGUNDOS` | Não (`25`) | Timeout reservado para o adaptador NVIDIA. |

As respostas de saúde não incluem configuração, credenciais ou detalhes internos.
Com `IVIS_IA_STUB_FALHA` diferente de `nenhuma`, `/pronto` retorna
`503/provedor_indisponivel` para indicar que o stub configurado não está pronto.
Em ambientes implantados, a comunicação deve usar TLS.
