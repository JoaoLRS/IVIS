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


class PerguntaGerada(EsquemaBase):
    texto: str = Field(min_length=1, max_length=1000)


class GerarPerguntaResposta(EsquemaBase):
    pergunta: PerguntaGerada
    versao_modelo: str = Field(min_length=1)
    versao_prompt: str = Field(min_length=1)


class GerarAvaliacaoResposta(EsquemaBase):
    nota: int = Field(ge=0, le=100)
    pontos_fortes: list[str] = Field(min_length=1, max_length=5)
    pontos_melhoria: list[str] = Field(min_length=1, max_length=5)
    versao_modelo: str = Field(min_length=1)
    versao_prompt: str = Field(min_length=1)
