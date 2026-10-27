import asyncio

import pytest
from fastapi import HTTPException
from pydantic import SecretStr

from app import config, provedor_nvidia
from app.provedor_nvidia import ProvedorNvidia
from app.schemas import GerarPerguntaEntrada


class RespostaFalsa:
    def __init__(self, corpo: dict[str, object], status_code: int = 200) -> None:
        self._corpo = corpo
        self.status_code = status_code

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict[str, object]:
        return self._corpo


def test_provedor_nvidia_chama_modelo_e_retorna_contrato(monkeypatch) -> None:
    chamadas: list[dict[str, object]] = []

    class ClienteFalso:
        def __init__(self, **kwargs) -> None:
            chamadas.append({"timeout": kwargs["timeout"]})

        async def __aenter__(self):
            return self

        async def __aexit__(self, *args) -> None:
            return None

        async def post(self, url, *, headers, json):
            chamadas.append({"url": url, "headers": headers, "json": json})
            return RespostaFalsa(
                {
                    "choices": [
                        {
                            "message": {
                                "content": '{"pergunta":{"texto":"Como usa Python?"}}'
                            }
                        }
                    ]
                }
            )

    config.configuracoes.chave_nvidia = SecretStr("token-de-teste")
    config.configuracoes.modelo_nvidia = "nvidia/nemotron-3.5-bolt-e4b"
    monkeypatch.setattr(provedor_nvidia.httpx, "AsyncClient", ClienteFalso)

    entrada = GerarPerguntaEntrada.model_validate(
        {
            "id_correlacao": "correlacao-teste",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        }
    )
    resultado = asyncio.run(ProvedorNvidia().gerar_pergunta(entrada))

    assert resultado == {
        "pergunta": {"texto": "Como usa Python?"},
        "versao_modelo": "nvidia/nemotron-3.5-bolt-e4b",
        "versao_prompt": "perguntas-1",
    }
    requisicao = chamadas[1]
    assert requisicao["url"] == "https://integrate.api.nvidia.com/v1/chat/completions"
    assert requisicao["headers"]["Authorization"] == "Bearer token-de-teste"
    assert requisicao["json"]["model"] == "nvidia/nemotron-3.5-bolt-e4b"
    assert requisicao["json"]["stream"] is False
    assert requisicao["json"]["chat_template_kwargs"]["enable_thinking"] is False
    assert requisicao["json"]["response_format"]["type"] == "json_schema"
    assert "id_correlacao" not in requisicao["json"]["messages"][1]["content"]


def test_provedor_nvidia_rejeita_json_invalido(monkeypatch) -> None:
    class ClienteFalso:
        def __init__(self, **kwargs) -> None:
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *args) -> None:
            return None

        async def post(self, *args, **kwargs):
            return RespostaFalsa(
                {"choices": [{"message": {"content": "texto sem JSON"}}]}
            )

    config.configuracoes.chave_nvidia = SecretStr("token-de-teste")
    monkeypatch.setattr(provedor_nvidia.httpx, "AsyncClient", ClienteFalso)
    entrada = GerarPerguntaEntrada.model_validate(
        {
            "id_correlacao": "correlacao-teste",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        }
    )

    with pytest.raises(HTTPException) as erro:
        asyncio.run(ProvedorNvidia().gerar_pergunta(entrada))
    assert erro.value.status_code == 502
    assert erro.value.detail["codigo"] == "saida_invalida"


def test_provedor_nvidia_consulta_resultado_assincrono(monkeypatch) -> None:
    urls_consultadas: list[str] = []

    class ClienteFalso:
        def __init__(self, **kwargs) -> None:
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *args) -> None:
            return None

        async def post(self, *args, **kwargs):
            return RespostaFalsa({"requestId": "pedido-123"}, status_code=202)

        async def get(self, url, **kwargs):
            urls_consultadas.append(url)
            return RespostaFalsa(
                {
                    "choices": [
                        {
                            "message": {
                                "content": '{"pergunta":{"texto":"Pergunta pronta"}}'
                            }
                        }
                    ]
                }
            )

    async def dormir_sem_espera(intervalo: float) -> None:
        del intervalo

    config.configuracoes.chave_nvidia = SecretStr("token-de-teste")
    config.configuracoes.modelo_nvidia = "nvidia/nemotron-3.5-bolt-e4b"
    monkeypatch.setattr(provedor_nvidia.httpx, "AsyncClient", ClienteFalso)
    monkeypatch.setattr(provedor_nvidia.asyncio, "sleep", dormir_sem_espera)
    entrada = GerarPerguntaEntrada.model_validate(
        {
            "id_correlacao": "correlacao-teste",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        }
    )

    resultado = asyncio.run(ProvedorNvidia().gerar_pergunta(entrada))

    assert resultado["pergunta"] == {"texto": "Pergunta pronta"}
    assert urls_consultadas == [
        "https://integrate.api.nvidia.com/v1/status/pedido-123"
    ]
