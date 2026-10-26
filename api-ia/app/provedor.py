from typing import Protocol

from app.schemas import GerarAvaliacaoEntrada, GerarPerguntaEntrada


class ProvedorIA(Protocol):
    async def gerar_pergunta(self, entrada: GerarPerguntaEntrada) -> object: ...

    async def gerar_avaliacao(self, entrada: GerarAvaliacaoEntrada) -> object: ...
