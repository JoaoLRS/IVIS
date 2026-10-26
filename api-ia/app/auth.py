import hmac

from fastapi import Header, HTTPException

from app.config import configuracoes


def exigir_backend(x_chave_servico: str | None = Header(default=None)) -> None:
    chave_recebida = x_chave_servico or ""
    chave_configurada = configuracoes.chave_servico.get_secret_value()
    if not hmac.compare_digest(chave_recebida, chave_configurada):
        raise HTTPException(
            status_code=401,
            detail={
                "codigo": "nao_autenticado",
                "mensagem": "Credencial de serviço inválida.",
            },
        )
