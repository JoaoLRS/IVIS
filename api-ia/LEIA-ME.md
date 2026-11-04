# API de IA do IVIS

Serviço Python interno do IVIS. Implementa o contrato v1 com uma interface de
provedor NVIDIA para gerar perguntas e avaliações, validação das saídas antes
de responder, autenticação entre serviços, liveness e readiness.

## Executar localmente

Requer Python 3.12 ou 3.13. Copie `.env.example` para `.env` dentro de `api-ia/`
e preencha as configurações locais. O arquivo `.env` é carregado automaticamente
e não deve ser enviado ao Git. Para usar o Nemotron pela NVIDIA, configure
preencha `NVIDIA_API_KEY` com uma chave válida. O
modelo padrão é `nvidia/nemotron-3.5-bolt-e4b`.

Crie um ambiente virtual e instale as versões fixadas:

```powershell
Set-Location api-ia
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app.main:app --app-dir . --host 127.0.0.1 --port 8000
```

Variáveis exportadas no ambiente do processo têm precedência sobre o `.env`.
Para integrar o backend executado fora do Compose, configure o mesmo valor de
`IVIS_IA_CHAVE_SERVICO` no ambiente do backend. No Compose, a chave definida no
`.env` da raiz é compartilhada automaticamente pelos dois serviços. O serviço
não imprime a chave nem os prompts/respostas nos logs.

- Liveness: `GET http://localhost:8000/saude`
- Readiness: `GET http://localhost:8000/pronto`
- Rotas de negócio v1: `POST /v1/perguntas/gerar` e `POST /v1/avaliacoes/gerar`

As rotas de negócio exigem `X-Chave-Servico`. A variável
`IVIS_IA_CHAVE_SERVICO` é obrigatória e deve ser diferente da chave NVIDIA.
As rotas delegam a geração ao contrato interno `ProvedorIA` e usam o endpoint
NVIDIA compatível com OpenAI. As
respostas estruturadas do modelo são validadas contra os schemas v1; uma saída
incompatível resulta em `502/saida_invalida`. Falhas de rede retornam
`503/provedor_indisponivel` e timeout retorna `504/tempo_esgotado`.

O backend acessa estas rotas pela rede interna do Compose (`http://ivis-api-ia:8000`).
O frontend não chama este serviço diretamente. Para conduzir um treino, usa as
rotas de sessão do backend: `POST /api/v1/treinos`,
`POST /api/v1/treinos/{id}/respostas` e `POST /api/v1/treinos/{id}/concluir`.

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
não avalia qualidade semântica. Os alvos p95 do relatório são propostas, não um SLA aprovado.

## Configuração

| Variável | Obrigatória | Uso |
|---|---|---|
| `IVIS_IA_CHAVE_SERVICO` | Sim | Chave interna do backend, com no mínimo 32 caracteres. |
| `NVIDIA_API_KEY` | Sim | Chave privada do NVIDIA API Catalog. |
| `NVIDIA_MODEL` | Não (`nvidia/nemotron-3.5-bolt-e4b`) | Modelo NVIDIA selecionado. |
| `NVIDIA_BASE_URL` | Não (`https://integrate.api.nvidia.com/v1`) | URL base do endpoint NVIDIA. |
| `NVIDIA_TIMEOUT_SEGUNDOS` | Não (`90`) | Timeout total da chamada externa, incluindo eventual consulta de operação assíncrona. |

As respostas de saúde não incluem configuração, credenciais ou detalhes internos.
Sem `NVIDIA_API_KEY`, `/pronto` retorna `503/provedor_indisponivel`.
Em ambientes implantados, a comunicação deve usar TLS.

O endpoint do Nemotron pode responder `202` enquanto processa uma chamada; o
adaptador consulta `/status/{requestId}` até obter o resultado ou atingir o
timeout configurado. A documentação do modelo não lista português entre os
idiomas oficialmente suportados, então valide a qualidade das perguntas e do
feedback em português com casos sintéticos antes de integrar ao fluxo real.
