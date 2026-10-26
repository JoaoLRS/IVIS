from fastapi import HTTPException
from pydantic import ValidationError

from app.config import configuracoes
from app.provedor import ProvedorIA
from app.provedor_stub import ProvedorStub
from app.schemas import (
    GerarAvaliacaoEntrada,
    GerarAvaliacaoResposta,
    GerarPerguntaEntrada,
    GerarPerguntaResposta,
)


class ServicoIA:
    def __init__(self, provedor: ProvedorIA) -> None:
        self._provedor = provedor

    async def gerar_pergunta(
        self,
        entrada: GerarPerguntaEntrada,
    ) -> GerarPerguntaResposta:
        try:
            resultado = await self._provedor.gerar_pergunta(entrada)
            return GerarPerguntaResposta.model_validate(resultado)
        except ValidationError as erro:
            raise HTTPException(
                status_code=502,
                detail={
                    "codigo": "saida_invalida",
                    "mensagem": "O provedor retornou uma resposta inválida.",
                },
            ) from erro

    async def gerar_avaliacao(
        self,
        entrada: GerarAvaliacaoEntrada,
    ) -> GerarAvaliacaoResposta:
        try:
            resultado = await self._provedor.gerar_avaliacao(entrada)
            return GerarAvaliacaoResposta.model_validate(resultado)
        except ValidationError as erro:
            raise HTTPException(
                status_code=502,
                detail={
                    "codigo": "saida_invalida",
                    "mensagem": "O provedor retornou uma resposta inválida.",
                },
            ) from erro


_servico_stub = ServicoIA(ProvedorStub())


def obter_servico_ativo() -> ServicoIA:
    if not configuracoes.usar_stub:
        raise HTTPException(
            status_code=503,
            detail={
                "codigo": "provedor_indisponivel",
                "mensagem": "O provedor ativo não está disponível.",
            },
        )
    return _servico_stub
