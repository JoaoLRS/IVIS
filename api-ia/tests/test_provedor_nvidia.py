import asyncio

import httpx
import pytest
from fastapi import HTTPException
from pydantic import SecretStr

from app import config, provedor_nvidia
from app.provedor_nvidia import ProvedorNvidia
from app.schemas import GerarAvaliacaoEntrada, GerarPerguntaEntrada


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

    monkeypatch.setattr(config.configuracoes, "chave_nvidia", SecretStr("token-de-teste"))
    monkeypatch.setattr(config.configuracoes, "modelo_nvidia", "nvidia/nemotron-3.5-bolt-e4b")
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
    assert "nível 1 é iniciante" in requisicao["json"]["messages"][0]["content"]


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

    monkeypatch.setattr(config.configuracoes, "chave_nvidia", SecretStr("token-de-teste"))
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

    monkeypatch.setattr(config.configuracoes, "chave_nvidia", SecretStr("token-de-teste"))
    monkeypatch.setattr(config.configuracoes, "modelo_nvidia", "nvidia/nemotron-3.5-bolt-e4b")
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


def entrada_pergunta() -> GerarPerguntaEntrada:
    return GerarPerguntaEntrada.model_validate(
        {
            "id_correlacao": "correlacao-teste",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        }
    )


def simular_http(monkeypatch, responder) -> None:
    cliente_original = httpx.AsyncClient
    monkeypatch.setattr(config.configuracoes, "chave_nvidia", SecretStr("token-de-teste"))
    monkeypatch.setattr(
        provedor_nvidia.httpx,
        "AsyncClient",
        lambda **opcoes: cliente_original(transport=httpx.MockTransport(responder), **opcoes),
    )


@pytest.mark.parametrize("status", [401, 429, 500])
def test_erro_http_do_provedor_vira_503_sem_vazar_detalhes(monkeypatch, status) -> None:
    def responder(requisicao: httpx.Request) -> httpx.Response:
        assert requisicao.headers["Authorization"] == "Bearer token-de-teste"
        return httpx.Response(status, json={"segredo": "mensagem interna da NVIDIA"})

    simular_http(monkeypatch, responder)
    with pytest.raises(HTTPException) as erro:
        asyncio.run(ProvedorNvidia().gerar_pergunta(entrada_pergunta()))

    assert erro.value.status_code == 503
    assert erro.value.detail["codigo"] == "provedor_indisponivel"
    assert "mensagem interna" not in str(erro.value.detail)


def test_timeout_do_provedor_vira_504(monkeypatch) -> None:
    def responder(requisicao: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("tempo interno de rede", request=requisicao)

    simular_http(monkeypatch, responder)
    with pytest.raises(HTTPException) as erro:
        asyncio.run(ProvedorNvidia().gerar_pergunta(entrada_pergunta()))

    assert erro.value.status_code == 504
    assert erro.value.detail["codigo"] == "tempo_esgotado"
    assert "tempo interno" not in str(erro.value.detail)


def test_falha_de_conexao_do_provedor_vira_503(monkeypatch) -> None:
    def responder(requisicao: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("endereço interno indisponível", request=requisicao)

    simular_http(monkeypatch, responder)
    with pytest.raises(HTTPException) as erro:
        asyncio.run(ProvedorNvidia().gerar_pergunta(entrada_pergunta()))

    assert erro.value.status_code == 503
    assert erro.value.detail["codigo"] == "provedor_indisponivel"
    assert "endereço interno" not in str(erro.value.detail)


def test_resposta_assincrona_sem_id_nao_inicia_consultas(monkeypatch) -> None:
    chamadas: list[str] = []

    def responder(requisicao: httpx.Request) -> httpx.Response:
        chamadas.append(requisicao.url.path)
        return httpx.Response(202, json={"requestId": ""})

    simular_http(monkeypatch, responder)
    with pytest.raises(HTTPException) as erro:
        asyncio.run(ProvedorNvidia().gerar_pergunta(entrada_pergunta()))

    assert erro.value.status_code == 502
    assert erro.value.detail["codigo"] == "saida_invalida"
    assert chamadas == ["/v1/chat/completions"]


def test_avaliacao_envia_interacoes_e_retorna_versoes(monkeypatch) -> None:
    import json

    def responder(requisicao: httpx.Request) -> httpx.Response:
        corpo = json.loads(requisicao.content)
        dados = json.loads(corpo["messages"][1]["content"])
        assert dados["interacoes"][0]["resposta"] == "Usei índices e expliquei o custo."
        assert "id_correlacao" not in dados
        assert corpo["response_format"]["json_schema"]["name"] == "avaliacao_gerada"
        return httpx.Response(200, json={
            "choices": [{"message": {"content": json.dumps({
                "nota": 82,
                "pontos_fortes": ["Explicou o custo da solução."],
                "pontos_melhoria": ["Comparar alternativas."],
            })}}]
        })

    simular_http(monkeypatch, responder)
    entrada = GerarAvaliacaoEntrada.model_validate({
        "id_correlacao": "correlacao-teste",
        "tipo_contexto": "VAGA",
        "vaga": {"titulo": "Backend", "senioridade": "Pleno", "requisitos": ["SQL"]},
        "interacoes": [{
            "numero": 1, "pergunta": "Como otimizar uma consulta?",
            "resposta": "Usei índices e expliquei o custo.",
        }],
    })

    resultado = asyncio.run(ProvedorNvidia().gerar_avaliacao(entrada))

    assert resultado["nota"] == 82
    assert resultado["pontos_fortes"] == ["Explicou o custo da solução."]
    assert resultado["versao_modelo"] == config.configuracoes.modelo_nvidia
    assert resultado["versao_prompt"] == "avaliacao-1"


def test_pergunta_seguinte_inclui_nivel_e_historico_para_evitar_repeticao(monkeypatch) -> None:
    import json

    def responder(requisicao: httpx.Request) -> httpx.Response:
        corpo = json.loads(requisicao.content)
        dados = json.loads(corpo["messages"][1]["content"])
        assert dados["habilidades"] == [{"nome": "Python", "nivel": 5}]
        assert dados["interacoes"] == [{
            "numero": 1, "pergunta": "O que é uma lista?",
            "resposta": "É uma coleção ordenada.",
        }]
        assert dados["numero_pergunta"] == 2
        instrucao = corpo["messages"][0]["content"]
        assert "não repetir perguntas" in instrucao
        assert "nível 1 é iniciante e o 5 é avançado" in instrucao
        return httpx.Response(200, json={
            "choices": [{"message": {"content": '{"pergunta":{"texto":"Como dimensionar memória?"}}'}}]
        })

    simular_http(monkeypatch, responder)
    entrada = GerarPerguntaEntrada.model_validate(entrada_pergunta().model_dump() | {
        "habilidades": [{"nome": "Python", "nivel": 5}],
        "interacoes": [{
            "numero": 1, "pergunta": "O que é uma lista?",
            "resposta": "É uma coleção ordenada.",
        }],
        "numero_pergunta": 2,
    })

    resultado = asyncio.run(ProvedorNvidia().gerar_pergunta(entrada))

    assert resultado["pergunta"]["texto"] == "Como dimensionar memória?"
