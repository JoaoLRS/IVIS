# API de IA do IVIS

Serviço Python interno do IVIS. No Marco 1, oferece configuração por ambiente,
schemas do contrato v1, autenticação entre serviços, liveness e readiness. As
operações de pergunta e avaliação já exigem autenticação, mas respondem
`501 nao_implementado` até a entrega do stub no Marco 2. Nenhuma credencial da
NVIDIA é necessária ou utilizada nesta etapa.

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
`NVIDIA_API_KEY` e `NVIDIA_MODEL` são opcionais e não são usados no Marco 1.

## Testes

```powershell
Set-Location api-ia
.\.venv\Scripts\python.exe -m pytest
```

## Configuração

| Variável | Obrigatória | Uso |
|---|---|---|
| `IVIS_IA_CHAVE_SERVICO` | Sim | Chave interna do backend, com no mínimo 32 caracteres. |
| `IVIS_IA_USAR_STUB` | Não (`true`) | Seleção futura do provedor stub. No Marco 1, readiness fica indisponível se desativado. |
| `NVIDIA_API_KEY` | Não | Reservada para o adaptador NVIDIA de marco posterior. |
| `NVIDIA_MODEL` | Não | Reservada para o adaptador NVIDIA de marco posterior. |
| `NVIDIA_BASE_URL` | Não | Endpoint base reservado para o adaptador NVIDIA. |
| `NVIDIA_TIMEOUT_SEGUNDOS` | Não (`25`) | Timeout reservado para o adaptador NVIDIA. |

As respostas de saúde não incluem configuração, credenciais ou detalhes internos.
Em ambientes implantados, a comunicação deve usar TLS.
