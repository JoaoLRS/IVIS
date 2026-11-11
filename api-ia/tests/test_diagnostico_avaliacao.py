import asyncio
import logging

import pytest
from fastapi import HTTPException

from app.schemas import GerarAvaliacaoEntrada
from app.servico import ServicoIA


def entrada_avaliacao() -> GerarAvaliacaoEntrada:
    return GerarAvaliacaoEntrada.model_validate({
        "id_correlacao": "treino-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 3}],
        "interacoes": [{"numero": 1, "pergunta": "Pergunta privada", "resposta": "Resposta privada"}],
    })


def avaliacao_base() -> dict:
    return {
        "nota": None,
        "pontos_fortes": [],
        "pontos_melhoria": [],
        "avaliacoes_por_pergunta": [{
            "numero": 1,
            "pergunta": "Pergunta privada",
            "criterios": [],
            "nivel_confianca": "BAIXA",
            "limitacao": "Sem avaliação qualitativa.",
        }],
        "avaliacao_qualitativa": False,
        "nivel_confianca": "BAIXA",
        "limitacoes": "Sem avaliação qualitativa.",
        "versao_modelo": "fake-1",
        "versao_prompt": "fake-1",
    }


@pytest.mark.parametrize("categoria,alterar", [
    ("quantidade_perguntas", lambda saida: saida.update(avaliacoes_por_pergunta=[])),
    ("numero_pergunta", lambda saida: saida["avaliacoes_por_pergunta"][0].update(numero=2)),
    ("texto_pergunta", lambda saida: saida["avaliacoes_por_pergunta"][0].update(pergunta="Outra pergunta")),
    ("esquema", lambda saida: saida.update(nivel_confianca="INVALIDO")),
])
def test_diagnostico_de_saida_invalida_sem_conteudo(caplog, categoria, alterar) -> None:
    saida = avaliacao_base()
    alterar(saida)

    class ProvedorFalso:
        async def gerar_avaliacao(self, entrada):
            return saida

    with caplog.at_level(logging.WARNING):
        with pytest.raises(HTTPException) as erro:
            asyncio.run(ServicoIA(ProvedorFalso()).gerar_avaliacao(entrada_avaliacao()))

    assert erro.value.status_code == 502
    assert erro.value.detail["codigo"] == "saida_invalida"
    assert f"categoria={categoria}" in caplog.text
    assert "id_correlacao='treino-123'" in caplog.text
    assert "Pergunta privada" not in caplog.text
    assert "Resposta privada" not in caplog.text


def test_diagnostico_de_evidencia_invalida_sem_expor_resposta(caplog) -> None:
    saida = avaliacao_base()
    saida.update(
        avaliacao_qualitativa=True,
        pontos_fortes=["Ponto forte"],
        pontos_melhoria=["Melhoria"],
    )
    saida["avaliacoes_por_pergunta"][0]["criterios"] = [
        {"criterio": nome, "nota": 3, "justificativa": "Justificativa", "trecho_resposta": "Citação incorreta"}
        for nome in ("CORRECAO_TECNICA", "COMPLETUDE", "CLAREZA")
    ]

    class ProvedorFalso:
        async def gerar_avaliacao(self, entrada):
            return saida

    with caplog.at_level(logging.WARNING):
        with pytest.raises(HTTPException) as erro:
            asyncio.run(ServicoIA(ProvedorFalso()).gerar_avaliacao(entrada_avaliacao()))

    assert erro.value.status_code == 502
    assert "categoria=evidencia_resposta" in caplog.text
    assert "Citação incorreta" not in caplog.text
    assert "Resposta privada" not in caplog.text
