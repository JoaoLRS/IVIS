import pytest
from pydantic import ValidationError

from app.schemas import GerarAvaliacaoEntrada, GerarPerguntaEntrada


def entrada_pergunta() -> dict:
    return {
        "id_correlacao": "treino-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 3}],
        "interacoes": [],
        "numero_pergunta": 1,
        "total_perguntas": 3,
    }


@pytest.mark.parametrize("nivel", [0, 6])
def test_nivel_da_habilidade_respeita_escala_do_backend(nivel: int) -> None:
    entrada = entrada_pergunta()
    entrada["habilidades"][0]["nivel"] = nivel

    with pytest.raises(ValidationError):
        GerarPerguntaEntrada.model_validate(entrada)


def test_numero_da_pergunta_nao_supera_total() -> None:
    entrada = entrada_pergunta()
    entrada["numero_pergunta"] = 4

    with pytest.raises(ValidationError):
        GerarPerguntaEntrada.model_validate(entrada)


@pytest.mark.parametrize("mudanca", [
    {"tipo_contexto": "VAGA"},
    {"vaga": {"titulo": "Backend", "senioridade": "Pleno", "requisitos": ["SQL"]}},
    {"habilidades": []},
])
def test_contexto_de_habilidades_exige_habilidades_sem_vaga(mudanca: dict) -> None:
    entrada = entrada_pergunta() | mudanca

    with pytest.raises(ValidationError):
        GerarPerguntaEntrada.model_validate(entrada)


def test_contexto_de_vaga_aceita_apenas_vaga() -> None:
    entrada = {
        "id_correlacao": "treino-123",
        "tipo_contexto": "VAGA",
        "vaga": {"titulo": "Backend", "senioridade": "Pleno", "requisitos": ["SQL"]},
        "interacoes": [],
    }
    assert GerarAvaliacaoEntrada.model_validate(entrada).vaga.titulo == "Backend"
    with pytest.raises(ValidationError):
        GerarAvaliacaoEntrada.model_validate(entrada | {"habilidades": []})
