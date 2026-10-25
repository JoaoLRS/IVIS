from fastapi import APIRouter

from app.provedor_stub import gerar_avaliacao as gerar_avaliacao_stub
from app.provedor_stub import gerar_pergunta as gerar_pergunta_stub
from app.schemas import (
    GerarAvaliacaoEntrada,
    GerarAvaliacaoResposta,
    GerarPerguntaEntrada,
    GerarPerguntaResposta,
)

roteador = APIRouter()


@roteador.post("/perguntas/gerar", response_model=GerarPerguntaResposta)
async def gerar_pergunta(entrada: GerarPerguntaEntrada) -> GerarPerguntaResposta:
    return gerar_pergunta_stub(entrada)


@roteador.post("/avaliacoes/gerar", response_model=GerarAvaliacaoResposta)
async def gerar_avaliacao(
    entrada: GerarAvaliacaoEntrada,
) -> GerarAvaliacaoResposta:
    return gerar_avaliacao_stub(entrada)
