from fastapi import APIRouter, HTTPException

from app.schemas import GerarAvaliacaoEntrada, GerarPerguntaEntrada

roteador = APIRouter()


@roteador.post("/perguntas/gerar")
async def gerar_pergunta(entrada: GerarPerguntaEntrada) -> None:
    del entrada
    raise HTTPException(
        status_code=501,
        detail={
            "codigo": "nao_implementado",
            "mensagem": "Operação disponível a partir do Marco 2.",
        },
    )


@roteador.post("/avaliacoes/gerar")
async def gerar_avaliacao(entrada: GerarAvaliacaoEntrada) -> None:
    del entrada
    raise HTTPException(
        status_code=501,
        detail={
            "codigo": "nao_implementado",
            "mensagem": "Operação disponível a partir do Marco 2.",
        },
    )
