from fastapi import HTTPException
from pydantic import ValidationError

from app.config import configuracoes
from app.provedor import ProvedorIA
from app.provedor_nvidia import ProvedorNvidia
from app.provedor_stub import ProvedorStub
from app.schemas import (
    GerarAvaliacaoEntrada,
    GerarAvaliacaoResposta,
    GerarPerguntaEntrada,
    GerarPerguntaResposta,
    calcular_nota_rubrica,
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
            avaliacao = GerarAvaliacaoResposta.model_validate(resultado)
            if len(avaliacao.avaliacoes_por_pergunta) != len(entrada.interacoes):
                raise ValueError("A avaliação não corresponde às perguntas enviadas.")
            for item, interacao in zip(
                avaliacao.avaliacoes_por_pergunta, entrada.interacoes
            ):
                if item.numero != interacao.numero or item.pergunta != interacao.pergunta:
                    raise ValueError("A avaliação não corresponde às perguntas enviadas.")
                if any(
                    (
                        bool(interacao.resposta.strip())
                        and not criterio.trecho_resposta
                    )
                    or criterio.trecho_resposta not in interacao.resposta
                    for criterio in item.criterios
                ):
                    raise ValueError("A evidência não corresponde à resposta enviada.")
            if avaliacao.avaliacao_qualitativa:
                avaliacao = avaliacao.model_copy(
                    update={
                        "nota": calcular_nota_rubrica(
                            avaliacao.avaliacoes_por_pergunta
                        )
                    }
                )
            return avaliacao
        except ValidationError as erro:
            raise HTTPException(
                status_code=502,
                detail={
                    "codigo": "saida_invalida",
                    "mensagem": "O provedor retornou uma resposta inválida.",
                },
            ) from erro
        except ValueError as erro:
            raise HTTPException(
                status_code=502,
                detail={
                    "codigo": "saida_invalida",
                    "mensagem": "O provedor retornou uma resposta inválida.",
                },
            ) from erro


_servico_stub = ServicoIA(ProvedorStub())
_servico_nvidia = ServicoIA(ProvedorNvidia())


def obter_servico_ativo() -> ServicoIA:
    if configuracoes.usar_stub:
        return _servico_stub
    if configuracoes.chave_nvidia is None:
        raise HTTPException(
            status_code=503,
            detail={
                "codigo": "provedor_indisponivel",
                "mensagem": "O provedor NVIDIA não está configurado.",
            },
        )
    return _servico_nvidia
