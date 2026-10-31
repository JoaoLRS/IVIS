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
            "nota": None,
            "pontos_fortes": [],
            "pontos_melhoria": [],
            "avaliacoes_por_pergunta": [],
            "avaliacao_qualitativa": False,
            "nivel_confianca": "BAIXA",
            "limitacoes": "Modo demonstrativo.",
            "versao_modelo": "stub-1",
            "versao_prompt": "avaliacao-3",
        },
    )
    assert not validar_resposta(
        "avaliacoes",
        {
            "nota": 80,
            "pontos_fortes": [],
            "pontos_melhoria": [],
            "avaliacoes_por_pergunta": [],
            "avaliacao_qualitativa": False,
            "nivel_confianca": "BAIXA",
            "limitacoes": "Modo demonstrativo.",
            "versao_modelo": "stub-1",
            "versao_prompt": "avaliacao-3",
        },
    )
    assert validar_resposta(
        "avaliacoes",
        {
            "nota": 78,
            "pontos_fortes": ["Resposta tecnicamente fundamentada."],
            "pontos_melhoria": ["Detalhar alternativas."],
            "avaliacoes_por_pergunta": [
                {
                    "numero": 1,
                    "pergunta": "Como tratar falhas?",
                    "criterios": [
                        {
                            "criterio": criterio,
                            "nota": nota,
                            "justificativa": "Justificativa.",
                            "trecho_resposta": "Trecho literal.",
                        }
                        for criterio, nota in (
                            ("CORRECAO_TECNICA", 4),
                            ("COMPLETUDE", 3),
                            ("CLAREZA", 5),
                        )
                    ],
                    "nivel_confianca": "MEDIA",
                    "limitacao": "Contexto limitado.",
                }
            ],
            "avaliacao_qualitativa": True,
            "nivel_confianca": "MEDIA",
            "limitacoes": "Contexto limitado.",
            "versao_modelo": "nvidia/fake",
            "versao_prompt": "avaliacao-3",
        },
    )
