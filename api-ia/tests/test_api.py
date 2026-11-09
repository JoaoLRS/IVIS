from fastapi.testclient import TestClient
from pydantic import SecretStr

from app import auth, config, rotas
from app.main import app
from app.servico import ServicoIA

CHAVE_TESTE = "chave-de-teste-com-mais-de-32-caracteres"
cliente = TestClient(app)


def setup_function() -> None:
    config.configuracoes.chave_servico = SecretStr(CHAVE_TESTE)
    config.configuracoes.chave_nvidia = None


def test_liveness_nao_expoe_configuracoes() -> None:
    resposta = cliente.get("/saude")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "ATIVO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_disponivel_com_provedor_real_configurado() -> None:
    config.configuracoes.chave_nvidia = SecretStr("chave-nvidia-de-teste")
    resposta = cliente.get("/pronto")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "PRONTO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_indisponivel_sem_provedor_configurado() -> None:

    resposta = cliente.get("/pronto")

    assert resposta.status_code == 503
    assert resposta.json() == {
        "codigo": "provedor_indisponivel",
        "mensagem": "O provedor ativo não está disponível.",
    }


def test_rota_protegida_rejeita_chave_ausente() -> None:
    resposta = cliente.post("/v1/perguntas/gerar", json={})

    assert resposta.status_code == 401
    assert resposta.json()["codigo"] == "nao_autenticado"


def test_rota_protegida_rejeita_chave_invalida() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": "chave incorreta"},
        json={},
    )

    assert resposta.status_code == 401
    assert resposta.json()["codigo"] == "nao_autenticado"


def test_rota_pergunta_usa_provedor_injetado(monkeypatch) -> None:
    entrada = {
        "id_correlacao": "correlacao-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 4}],
        "vaga": None,
        "interacoes": [],
        "numero_pergunta": 1,
        "total_perguntas": 3,
    }
    resposta_provedor = {
        "pergunta": {"texto": "Pergunta do provedor"},
        "versao_modelo": "fake-1",
        "versao_prompt": "fake-prompt-1",
    }

    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            assert recebido.id_correlacao == "correlacao-123"
            return resposta_provedor

        async def gerar_avaliacao(self, recebido):
            raise AssertionError("Não deveria gerar avaliação neste teste.")

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )

    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json=entrada,
    )

    assert resposta.status_code == 200
    assert resposta.json() == resposta_provedor


def test_rota_avaliacao_usa_provedor_injetado(monkeypatch) -> None:
    resposta_provedor = {
        "nota": None,
        "pontos_fortes": [],
        "pontos_melhoria": [],
        "avaliacoes_por_pergunta": [],
        "avaliacao_qualitativa": False,
        "nivel_confianca": "BAIXA",
        "limitacoes": "Não há interações para avaliar.",
        "versao_modelo": "fake-1",
        "versao_prompt": "fake-prompt-1",
    }

    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            raise AssertionError("Não deveria gerar pergunta neste teste.")

        async def gerar_avaliacao(self, recebido):
            assert recebido.id_correlacao == "correlacao-123"
            return resposta_provedor

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
        },
    )

    assert resposta.status_code == 200
    assert resposta.json() == resposta_provedor


def test_rota_avaliacao_rejeita_saida_invalida_do_provedor(monkeypatch) -> None:
    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            raise AssertionError("Não deveria gerar pergunta neste teste.")

        async def gerar_avaliacao(self, recebido):
            return {
                "nota": 101,
                "pontos_fortes": ["Ponto forte"],
                "pontos_melhoria": ["Melhoria"],
                "versao_modelo": "fake-1",
                "versao_prompt": "fake-prompt-1",
            }

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
        },
    )

    assert resposta.status_code == 502
    assert resposta.json() == {
        "codigo": "saida_invalida",
        "mensagem": "O provedor retornou uma resposta inválida.",
    }


def test_rota_pergunta_rejeita_texto_vazio_do_provedor(monkeypatch) -> None:
    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            return {
                "pergunta": {"texto": ""},
                "versao_modelo": "fake-1",
                "versao_prompt": "fake-prompt-1",
            }

    monkeypatch.setattr(rotas, "obter_servico_ativo", lambda: ServicoIA(ProvedorFalso()))
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        },
    )

    assert resposta.status_code == 502
    assert resposta.json()["codigo"] == "saida_invalida"


def test_rota_rejeita_numero_de_pergunta_maior_que_total_sem_chamar_provedor(monkeypatch) -> None:
    def provedor_inesperado():
        raise AssertionError("Entrada inválida não deve chamar o provedor.")

    monkeypatch.setattr(rotas, "obter_servico_ativo", provedor_inesperado)
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "interacoes": [],
            "numero_pergunta": 4,
            "total_perguntas": 3,
        },
    )

    assert resposta.status_code == 422
    assert resposta.json()["codigo"] == "entrada_invalida"


def test_operacao_indisponivel_sem_chave_nvidia() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        },
    )

    assert resposta.status_code == 503
    assert resposta.json()["codigo"] == "provedor_indisponivel"


def test_chave_e_comparada_com_hmac_compare_digest(monkeypatch) -> None:
    chamada: list[tuple[str, str]] = []

    def comparar(chave_recebida: str, chave_configurada: str) -> bool:
        chamada.append((chave_recebida, chave_configurada))
        return False

    monkeypatch.setattr(auth.hmac, "compare_digest", comparar)
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": "chave-invalida"},
        json={},
    )

    assert resposta.status_code == 401
    assert chamada == [("chave-invalida", CHAVE_TESTE)]


def test_validacao_de_entrada_usa_erro_do_contrato() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={"tipo_contexto": "DESCONHECIDO"},
    )

    assert resposta.status_code == 422
    assert resposta.json() == {
        "codigo": "entrada_invalida",
        "mensagem": "A requisição não atende ao contrato da API.",
    }


def test_validacao_rejeita_resposta_acima_do_limite() -> None:
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [
                {
                    "numero": 1,
                    "pergunta": "Pergunta",
                    "resposta": "x" * 4001,
                }
            ],
        },
    )

    assert resposta.status_code == 422
    assert resposta.json()["codigo"] == "entrada_invalida"
