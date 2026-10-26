from fastapi import APIRouter

from app.servico import obter_servico_ativo
from app.schemas import (
    GerarAvaliacaoEntrada,
    GerarAvaliacaoResposta,
    GerarPerguntaEntrada,
    GerarPerguntaResposta,
)

roteador = APIRouter()


@roteador.post("/perguntas/gerar", response_model=GerarPerguntaResposta)
async def gerar_pergunta(entrada: GerarPerguntaEntrada) -> GerarPerguntaResposta:
    return await obter_servico_ativo().gerar_pergunta(entrada)


@roteador.post("/avaliacoes/gerar", response_model=GerarAvaliacaoResposta)
async def gerar_avaliacao(
    entrada: GerarAvaliacaoEntrada,
) -> GerarAvaliacaoResposta:
    return await obter_servico_ativo().gerar_avaliacao(entrada)
