from fastapi import Depends, FastAPI, HTTPException
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.auth import exigir_backend
from app.config import configuracoes
from app.erros import tratar_entrada_invalida, tratar_erro_http
from app.rotas import roteador

app = FastAPI(title="IVIS API de IA", version="1.0.0")
app.add_exception_handler(StarletteHTTPException, tratar_erro_http)
app.add_exception_handler(RequestValidationError, tratar_entrada_invalida)
app.include_router(roteador, prefix="/v1", dependencies=[Depends(exigir_backend)])


@app.get("/saude")
async def saude() -> dict[str, str]:
    return {"situacao": "ATIVO"}


@app.get("/pronto")
async def pronto() -> dict[str, str]:
    if (
        not configuracoes.usar_stub
        or configuracoes.falha_stub != "nenhuma"
    ):
        raise HTTPException(
            status_code=503,
            detail={
                "codigo": "provedor_indisponivel",
                "mensagem": "O provedor ativo não está disponível.",
            },
        )
    return {"situacao": "PRONTO"}
