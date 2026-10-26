import argparse
import asyncio
import json
import math
import os
import sys
from collections import Counter
from datetime import UTC, datetime
from time import perf_counter
from urllib.parse import urlsplit
from uuid import uuid4

import httpx

ALVOS_P95_MS = {
    "perguntas": 10_000,
    "avaliacoes": 20_000,
}
MAXIMO_AMOSTRAS = 1000
MAXIMA_CONCORRENCIA = 10


def percentil(valores: list[float], percentual: float) -> float:
    if not valores:
        raise ValueError("Não há valores para calcular o percentil.")
    if not 0 <= percentual <= 100:
        raise ValueError("O percentil deve estar entre 0 e 100.")

    ordenados = sorted(valores)
    posicao = max(1, math.ceil(percentual / 100 * len(ordenados)))
    return ordenados[posicao - 1]


def validar_resposta(operacao: str, corpo: object) -> bool:
    if not isinstance(corpo, dict):
        return False

    if operacao == "perguntas":
        pergunta = corpo.get("pergunta")
        return (
            isinstance(pergunta, dict)
            and isinstance(pergunta.get("texto"), str)
            and bool(pergunta["texto"].strip())
            and isinstance(corpo.get("versao_modelo"), str)
            and bool(corpo["versao_modelo"].strip())
            and isinstance(corpo.get("versao_prompt"), str)
            and bool(corpo["versao_prompt"].strip())
        )

    return (
        type(corpo.get("nota")) is int
        and 0 <= corpo["nota"] <= 100
        and _lista_de_textos_valida(corpo.get("pontos_fortes"))
        and _lista_de_textos_valida(corpo.get("pontos_melhoria"))
        and isinstance(corpo.get("versao_modelo"), str)
        and bool(corpo["versao_modelo"].strip())
        and isinstance(corpo.get("versao_prompt"), str)
        and bool(corpo["versao_prompt"].strip())
    )


def _lista_de_textos_valida(valor: object) -> bool:
    return (
        isinstance(valor, list)
        and 1 <= len(valor) <= 5
        and all(isinstance(item, str) and bool(item.strip()) for item in valor)
    )


def _payload(operacao: str, camada: str) -> dict[str, object]:
    payload: dict[str, object] = {
        "tipo_contexto": "HABILIDADES",
        "habilidades": [
            {"nome": "Python", "nivel": 4},
            {"nome": "APIs HTTP", "nivel": 3},
        ],
        "vaga": None,
        "interacoes": [
            {
                "numero": 1,
                "pergunta": "Como você trataria uma falha transitória em uma API?",
                "resposta": (
                    "Aplicaria timeout e retentativas limitadas com backoff, "
                    "registrando métricas sem armazenar dados pessoais."
                ),
            }
        ],
    }
    if camada == "api-ia":
        payload["id_correlacao"] = str(uuid4())
    if operacao == "perguntas":
        payload["numero_pergunta"] = 2
        payload["total_perguntas"] = 3
    return payload


async def _medir_requisicao(
    cliente: httpx.AsyncClient,
    semaforo: asyncio.Semaphore,
    url: str,
    operacao: str,
    camada: str,
) -> dict[str, float | int | str | None]:
    prefixo = "/v1" if camada == "api-ia" else "/v1/ia"
    endpoint = f"{url.rstrip('/')}{prefixo}/{operacao}/gerar"
    async with semaforo:
        inicio = perf_counter()
        try:
            resposta = await cliente.post(
                endpoint,
                json=_payload(operacao, camada),
            )
        except httpx.TimeoutException:
            return {
                "duracao_ms": (perf_counter() - inicio) * 1000,
                "status_http": None,
                "erro": "timeout",
            }
        except httpx.RequestError:
            return {
                "duracao_ms": (perf_counter() - inicio) * 1000,
                "status_http": None,
                "erro": "falha_de_conexao",
            }

        duracao_ms = (perf_counter() - inicio) * 1000
        if resposta.status_code != 200:
            return {
                "duracao_ms": duracao_ms,
                "status_http": resposta.status_code,
                "erro": f"http_{resposta.status_code}",
            }

        try:
            corpo = resposta.json()
        except ValueError:
            return {
                "duracao_ms": duracao_ms,
                "status_http": resposta.status_code,
                "erro": "json_invalido",
            }

        if not validar_resposta(operacao, corpo):
            return {
                "duracao_ms": duracao_ms,
                "status_http": resposta.status_code,
                "erro": "resposta_invalida",
            }
        return {
            "duracao_ms": duracao_ms,
            "status_http": resposta.status_code,
            "erro": None,
            "versao_modelo": corpo["versao_modelo"],
            "versao_prompt": corpo["versao_prompt"],
        }


def _resumir(
    resultados: list[dict[str, float | int | str | None]],
    aquecimentos_com_erro: int,
    operacao: str,
) -> dict[str, object]:
    sucessos = [resultado for resultado in resultados if resultado["erro"] is None]
    erros = Counter(
        str(resultado["erro"])
        for resultado in resultados
        if resultado["erro"] is not None
    )
    duracoes = [float(resultado["duracao_ms"]) for resultado in sucessos]
    p95_ms = percentil(duracoes, 95) if duracoes else None
    alvo_ms = ALVOS_P95_MS[operacao]
    versoes_modelo = sorted(
        {str(resultado["versao_modelo"]) for resultado in sucessos}
    )
    versoes_prompt = sorted(
        {str(resultado["versao_prompt"]) for resultado in sucessos}
    )
    atingiu_alvo = (
        p95_ms is not None
        and p95_ms <= alvo_ms
        and not erros
        and aquecimentos_com_erro == 0
    )

    if erros or aquecimentos_com_erro:
        situacao = "INCOMPLETO"
    elif atingiu_alvo:
        situacao = "DENTRO_DO_ALVO_PROPOSTO"
    else:
        situacao = "ACIMA_DO_ALVO_PROPOSTO"

    return {
        "amostras_solicitadas": len(resultados),
        "sucessos": len(sucessos),
        "falhas": sum(erros.values()),
        "falhas_aquecimento": aquecimentos_com_erro,
        "erros": dict(sorted(erros.items())),
        "p50_ms": round(percentil(duracoes, 50), 2) if duracoes else None,
        "p95_ms": round(p95_ms, 2) if p95_ms is not None else None,
        "maximo_ms": round(max(duracoes), 2) if duracoes else None,
        "versoes_modelo": versoes_modelo,
        "versoes_prompt": versoes_prompt,
        "alvo_p95_proposto_ms": alvo_ms,
        "situacao": situacao,
    }


def _validar_url(base_url: str) -> tuple[str, int | None, str]:
    partes = urlsplit(base_url)
    if (
        partes.scheme not in {"http", "https"}
        or not partes.hostname
        or partes.username
        or partes.password
        or partes.query
        or partes.fragment
    ):
        raise ValueError(
            "A URL deve usar HTTP(S), conter um host e não incluir credenciais, "
            "query ou fragmento."
        )
    return partes.hostname, partes.port, partes.path.rstrip("/")


def _cabecalhos_backend(token: str | None) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"} if token else {}


def _inteiro_positivo(valor: str) -> int:
    numero = int(valor)
    if numero < 1:
        raise argparse.ArgumentTypeError("Informe um inteiro maior que zero.")
    return numero


def _argumentos() -> argparse.Namespace:
    analisador = argparse.ArgumentParser(
        description=(
            "Mede latencia e conformidade estrutural usando somente payload "
            "sintetico; nao avalia qualidade semantica."
        )
    )
    analisador.add_argument(
        "--camada",
        choices=("api-ia", "backend"),
        default="api-ia",
        help="Camada alvo: API de IA direta ou backend integrado.",
    )
    analisador.add_argument(
        "--base-url",
        default=os.environ.get(
            "IVIS_IA_URL_MEDICAO",
            "http://127.0.0.1:8000",
        ),
        help="URL base local/representativa, sem credenciais na URL.",
    )
    analisador.add_argument(
        "--amostras",
        type=_inteiro_positivo,
        default=30,
        help=f"Amostras por operacao (maximo {MAXIMO_AMOSTRAS}; padrao 30).",
    )
    analisador.add_argument(
        "--aquecimento",
        type=int,
        default=2,
        help="Chamadas descartadas por operacao antes da medicao (padrao 2).",
    )
    analisador.add_argument(
        "--concorrencia",
        type=_inteiro_positivo,
        default=1,
        help=(
            f"Chamadas simultaneas por operacao (maximo {MAXIMA_CONCORRENCIA}; "
            "padrao 1)."
        ),
    )
    analisador.add_argument(
        "--timeout-segundos",
        type=float,
        default=30,
        help="Timeout HTTP por chamada (padrao 30).",
    )
    argumentos = analisador.parse_args()
    if argumentos.amostras > MAXIMO_AMOSTRAS:
        analisador.error(f"--amostras aceita no máximo {MAXIMO_AMOSTRAS}.")
    if argumentos.concorrencia > MAXIMA_CONCORRENCIA:
        analisador.error(f"--concorrencia aceita no máximo {MAXIMA_CONCORRENCIA}.")
    if argumentos.aquecimento < 0:
        analisador.error("--aquecimento não pode ser negativo.")
    if argumentos.timeout_segundos <= 0:
        analisador.error("--timeout-segundos deve ser maior que zero.")
    try:
        argumentos.host, argumentos.porta, argumentos.caminho_base = _validar_url(
            argumentos.base_url
        )
    except ValueError as erro:
        analisador.error(str(erro))
    return argumentos


async def _executar(argumentos: argparse.Namespace) -> int:
    if argumentos.camada == "api-ia":
        chave = os.environ.get("IVIS_IA_CHAVE_SERVICO")
        if not chave:
            print(
                "Defina IVIS_IA_CHAVE_SERVICO no ambiente para medir a API de IA.",
                file=sys.stderr,
            )
            return 2
        cabecalhos = {"X-Chave-Servico": chave}
    else:
        token = os.environ.get("IVIS_TOKEN_ACESSO")
        cabecalhos = _cabecalhos_backend(token)

    url = f"{argumentos.base_url.rstrip('/')}"
    semaforo = asyncio.Semaphore(argumentos.concorrencia)
    async with httpx.AsyncClient(
        timeout=argumentos.timeout_segundos,
        headers=cabecalhos,
    ) as cliente:
        series: dict[str, dict[str, object]] = {}
        for operacao in ("perguntas", "avaliacoes"):
            aquecimentos = [
                await _medir_requisicao(
                    cliente,
                    semaforo,
                    url + argumentos.caminho_base,
                    operacao,
                    argumentos.camada,
                )
                for _ in range(argumentos.aquecimento)
            ]
            resultados = await asyncio.gather(
                *(
                    _medir_requisicao(
                        cliente,
                        semaforo,
                        url + argumentos.caminho_base,
                        operacao,
                        argumentos.camada,
                    )
                    for _ in range(argumentos.amostras)
                )
            )
            series[operacao] = _resumir(
                resultados,
                sum(resultado["erro"] is not None for resultado in aquecimentos),
                operacao,
            )

    relatorio = {
        "gerado_em_utc": datetime.now(UTC).isoformat(),
        "camada": argumentos.camada,
        "alvo": {
            "host": argumentos.host,
            "porta": argumentos.porta,
            "caminho_base": argumentos.caminho_base or "/",
        },
        "configuracao": {
            "amostras_por_operacao": argumentos.amostras,
            "aquecimento_por_operacao": argumentos.aquecimento,
            "concorrencia": argumentos.concorrencia,
            "timeout_segundos": argumentos.timeout_segundos,
        },
        "nota": (
            "Medição técnica da camada indicada. Os alvos p95 são propostas; "
            "o stub não mede qualidade semântica."
        ),
        "operacoes": series,
    }
    print(json.dumps(relatorio, ensure_ascii=True, indent=2))
    return int(
        any(
            resultado["situacao"] != "DENTRO_DO_ALVO_PROPOSTO"
            for resultado in series.values()
        )
    )


def main() -> int:
    argumentos = _argumentos()
    return asyncio.run(_executar(argumentos))


if __name__ == "__main__":
    raise SystemExit(main())
