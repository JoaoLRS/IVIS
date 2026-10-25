from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


async def tratar_erro_http(
    requisicao: Request,
    excecao: StarletteHTTPException,
) -> JSONResponse:
    del requisicao
    detalhe = excecao.detail
    if isinstance(detalhe, dict) and {"codigo", "mensagem"} <= detalhe.keys():
        corpo = detalhe
    else:
        corpo = {
            "codigo": "erro_requisicao",
            "mensagem": "Não foi possível processar a requisição.",
        }
    return JSONResponse(status_code=excecao.status_code, content=corpo)


async def tratar_entrada_invalida(
    requisicao: Request,
    excecao: RequestValidationError,
) -> JSONResponse:
    del requisicao, excecao
    return JSONResponse(
        status_code=422,
        content={
            "codigo": "entrada_invalida",
            "mensagem": "A requisição não atende ao contrato da API.",
        },
    )
