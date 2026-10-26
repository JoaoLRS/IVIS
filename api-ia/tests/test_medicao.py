import pytest

from scripts.medir_desempenho import (
    _cabecalhos_backend,
    _resumir,
    percentil,
    validar_resposta,
)


def test_percentil_usa_ordem_mais_proxima() -> None:
    assert percentil([5, 1, 4, 2, 3], 50) == 3
    assert percentil([5, 1, 4, 2, 3], 95) == 5


def test_percentil_rejeita_entrada_sem_medicoes() -> None:
    with pytest.raises(ValueError, match="Não há valores"):
        percentil([], 95)


def test_cabecalho_backend_usa_token_bearer() -> None:
    assert _cabecalhos_backend("token-de-teste") == {
        "Authorization": "Bearer token-de-teste"
    }
    assert _cabecalhos_backend(None) == {}


def test_resumo_inclui_versoes_e_marca_aquecimento_com_falha() -> None:
    resumo = _resumir(
        [
            {
                "duracao_ms": 12.5,
                "status_http": 200,
                "erro": None,
                "versao_modelo": "stub-1",
                "versao_prompt": "perguntas-1",
            }
        ],
        aquecimentos_com_erro=1,
        operacao="perguntas",
    )

    assert resumo["versoes_modelo"] == ["stub-1"]
    assert resumo["versoes_prompt"] == ["perguntas-1"]
    assert resumo["situacao"] == "INCOMPLETO"


def test_validar_resposta_de_pergunta_confere_contrato() -> None:
    assert validar_resposta(
        "perguntas",
        {
            "pergunta": {"texto": "Pergunta sintética"},
            "versao_modelo": "stub-1",
            "versao_prompt": "perguntas-1",
        },
    )
    assert not validar_resposta(
        "perguntas",
        {
            "pergunta": {"texto": ""},
            "versao_modelo": "stub-1",
            "versao_prompt": "perguntas-1",
        },
    )


def test_validar_resposta_de_avaliacao_confere_contrato() -> None:
    assert validar_resposta(
        "avaliacoes",
        {
            "nota": 80,
            "pontos_fortes": ["Ponto forte"],
            "pontos_melhoria": ["Ponto de melhoria"],
            "versao_modelo": "stub-1",
            "versao_prompt": "avaliacao-1",
        },
    )
    assert not validar_resposta(
        "avaliacoes",
        {
            "nota": True,
            "pontos_fortes": ["Ponto forte"],
            "pontos_melhoria": ["Ponto de melhoria"],
            "versao_modelo": "stub-1",
            "versao_prompt": "avaliacao-1",
        },
    )
