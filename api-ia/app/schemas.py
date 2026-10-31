from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class EsquemaBase(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Habilidade(EsquemaBase):
    nome: str = Field(min_length=1)
    nivel: int


class Vaga(EsquemaBase):
    titulo: str = Field(min_length=1)
    senioridade: str = Field(min_length=1)
    requisitos: list[str] = Field(max_length=20)


class Interacao(EsquemaBase):
    numero: int = Field(ge=1)
    pergunta: str = Field(min_length=1)
    resposta: str = Field(max_length=4000)


class ContextoEntrada(EsquemaBase):
    id_correlacao: str = Field(min_length=1)
    tipo_contexto: Literal["HABILIDADES", "VAGA"]
    habilidades: list[Habilidade] | None = Field(default=None, max_length=30)
    vaga: Vaga | None = None
    interacoes: list[Interacao] = Field(max_length=10)

    @model_validator(mode="after")
    def validar_contexto(self) -> "ContextoEntrada":
        if self.tipo_contexto == "HABILIDADES":
            if not self.habilidades or self.vaga is not None:
                raise ValueError(
                    "HABILIDADES exige habilidades e não aceita vaga."
                )
        elif self.vaga is None or self.habilidades is not None:
            raise ValueError("VAGA exige vaga e não aceita habilidades.")
        return self


class GerarPerguntaEntrada(ContextoEntrada):
    numero_pergunta: int = Field(ge=1)
    total_perguntas: int = Field(ge=1, le=10)


class GerarAvaliacaoEntrada(ContextoEntrada):
    pass


class CriterioAvaliacao(EsquemaBase):
    criterio: Literal["CORRECAO_TECNICA", "COMPLETUDE", "CLAREZA"]
    nota: int = Field(ge=0, le=5)
    justificativa: str = Field(min_length=1, max_length=500)
    trecho_resposta: str = Field(max_length=500)


class AvaliacaoPergunta(EsquemaBase):
    numero: int = Field(ge=1)
    pergunta: str = Field(min_length=1, max_length=1000)
    criterios: list[CriterioAvaliacao] = Field(max_length=3)
    nivel_confianca: Literal["BAIXA", "MEDIA", "ALTA"]
    limitacao: str = Field(min_length=1, max_length=500)

    @model_validator(mode="after")
    def validar_criterios(self) -> "AvaliacaoPergunta":
        nomes = [criterio.criterio for criterio in self.criterios]
        esperados = {"CORRECAO_TECNICA", "COMPLETUDE", "CLAREZA"}
        if self.criterios and set(nomes) != esperados:
            raise ValueError("A rubrica exige exatamente os três critérios.")
        if len(nomes) != len(set(nomes)):
            raise ValueError("A rubrica não aceita critérios duplicados.")
        return self


class PerguntaGerada(EsquemaBase):
    texto: str = Field(min_length=1, max_length=1000)


class GerarPerguntaResposta(EsquemaBase):
    pergunta: PerguntaGerada
    versao_modelo: str = Field(min_length=1)
    versao_prompt: str = Field(min_length=1)


class GerarAvaliacaoResposta(EsquemaBase):
    nota: int = Field(default=0, ge=0, le=100)
    pontos_fortes: list[str] = Field(min_length=1, max_length=5)
    pontos_melhoria: list[str] = Field(min_length=1, max_length=5)
    avaliacoes_por_pergunta: list[AvaliacaoPergunta] = Field(max_length=10)
    avaliacao_qualitativa: bool
    nivel_confianca: Literal["BAIXA", "MEDIA", "ALTA"]
    limitacoes: str = Field(min_length=1, max_length=1000)
    versao_modelo: str = Field(min_length=1)
    versao_prompt: str = Field(min_length=1)

    @model_validator(mode="after")
    def validar_disponibilidade_da_rubrica(self) -> "GerarAvaliacaoResposta":
        if self.avaliacao_qualitativa and not self.avaliacoes_por_pergunta:
            raise ValueError("A avaliação qualitativa exige avaliações por pergunta.")
        if not self.avaliacao_qualitativa and any(
            avaliacao.criterios for avaliacao in self.avaliacoes_por_pergunta
        ):
            raise ValueError("A rubrica não pode conter notas sem avaliação qualitativa.")
        return self


def calcular_nota_rubrica(avaliacoes: list[AvaliacaoPergunta]) -> int:
    criterios = [
        criterio
        for avaliacao in avaliacoes
        for criterio in avaliacao.criterios
    ]
    if not criterios:
        return 0
    total_maximo = len(criterios) * 5
    return (sum(criterio.nota for criterio in criterios) * 100 + total_maximo // 2) // total_maximo
