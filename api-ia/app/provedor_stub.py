from fastapi import HTTPException

from app.config import configuracoes
from app.schemas import (
    GerarAvaliacaoEntrada,
    GerarAvaliacaoResposta,
    GerarPerguntaEntrada,
    GerarPerguntaResposta,
    PerguntaGerada,
)


class ProvedorStub:
    async def gerar_pergunta(
        self,
        entrada: GerarPerguntaEntrada,
    ) -> GerarPerguntaResposta:
        return gerar_pergunta(entrada)

    async def gerar_avaliacao(
        self,
        entrada: GerarAvaliacaoEntrada,
    ) -> GerarAvaliacaoResposta:
        return gerar_avaliacao(entrada)


_PERGUNTAS = (
    "Como você aplicaria {tema} em um projeto real e quais decisões técnicas tomaria?",
    "Descreva um desafio envolvendo {tema} e como avaliaria as alternativas para resolvê-lo.",
    "Quais cuidados e boas práticas são essenciais ao trabalhar com {tema}?",
)
_PONTO_FORTE = (
    "Esta avaliação demonstrativa considera o volume de texto das respostas."
)
_PONTO_MELHORIA = (
    "A nota do stub não avalia a qualidade ou a correção técnica das respostas."
)


def _verificar_disponibilidade() -> None:
    if not configuracoes.usar_stub or configuracoes.falha_stub == "indisponivel":
        raise HTTPException(
            status_code=503,
            detail={
                "codigo": "provedor_indisponivel",
                "mensagem": "O provedor ativo não está disponível.",
            },
        )
    if configuracoes.falha_stub == "tempo_esgotado":
        raise HTTPException(
            status_code=504,
            detail={
                "codigo": "tempo_esgotado",
                "mensagem": "O provedor excedeu o tempo limite.",
            },
        )


def _tema(entrada: GerarPerguntaEntrada | GerarAvaliacaoEntrada) -> str:
    if entrada.tipo_contexto == "HABILIDADES":
        assert entrada.habilidades is not None
        habilidade = entrada.habilidades[len(entrada.interacoes) % len(entrada.habilidades)]
        return habilidade.nome.strip()[:300] or "a habilidade informada"

    assert entrada.vaga is not None
    if entrada.vaga.requisitos:
        requisito = entrada.vaga.requisitos[
            len(entrada.interacoes) % len(entrada.vaga.requisitos)
        ]
        return requisito.strip()[:300] or entrada.vaga.titulo.strip()[:300]
    return entrada.vaga.titulo.strip()[:300]


def gerar_pergunta(entrada: GerarPerguntaEntrada) -> GerarPerguntaResposta:
    _verificar_disponibilidade()
    indice = (entrada.numero_pergunta - 1) % len(_PERGUNTAS)
    texto = _PERGUNTAS[indice].format(tema=_tema(entrada))
    return GerarPerguntaResposta(
        pergunta=PerguntaGerada(texto=texto),
        versao_modelo="stub-1",
        versao_prompt="perguntas-1",
    )


def gerar_avaliacao(entrada: GerarAvaliacaoEntrada) -> GerarAvaliacaoResposta:
    _verificar_disponibilidade()
    if entrada.interacoes:
        caracteres_por_resposta = sum(
            min(len(interacao.resposta), 500) for interacao in entrada.interacoes
        ) / len(entrada.interacoes)
        nota = int(caracteres_por_resposta / 5)
    else:
        nota = 0

    pontos_fortes = [_PONTO_FORTE]
    if nota > 0:
        pontos_fortes.append("As respostas enviadas contêm texto para análise.")

    pontos_melhoria = [_PONTO_MELHORIA]
    if nota == 0:
        pontos_melhoria.append("Envie respostas para que o stub possa calcular uma nota.")

    return GerarAvaliacaoResposta(
        nota=nota,
        pontos_fortes=pontos_fortes,
        pontos_melhoria=pontos_melhoria,
        versao_modelo="stub-1",
        versao_prompt="avaliacao-1",
    )
