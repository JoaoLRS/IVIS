from fastapi.testclient import TestClient
from pydantic import SecretStr

from app import auth, config
from app.main import app

CHAVE_TESTE = "chave-de-teste-com-mais-de-32-caracteres"
cliente = TestClient(app)


def setup_function() -> None:
    config.configuracoes.chave_servico = SecretStr(CHAVE_TESTE)
    config.configuracoes.usar_stub = True


def test_liveness_nao_expoe_configuracoes() -> None:
    resposta = cliente.get("/saude")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "ATIVO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_do_stub_nao_expoe_configuracoes() -> None:
    resposta = cliente.get("/pronto")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "PRONTO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_indisponivel_quando_stub_desativado() -> None:
    config.configuracoes.usar_stub = False

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


def test_rota_protegida_autentica_antes_do_marco_2() -> None:
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

    assert resposta.status_code == 501
    assert resposta.json()["codigo"] == "nao_implementado"


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
