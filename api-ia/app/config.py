from typing import Literal

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Configuracoes(BaseSettings):
    chave_servico: SecretStr = Field(
        validation_alias="IVIS_IA_CHAVE_SERVICO",
        min_length=32,
    )
    usar_stub: bool = Field(default=True, validation_alias="IVIS_IA_USAR_STUB")
    falha_stub: Literal["nenhuma", "indisponivel", "tempo_esgotado"] = Field(
        default="nenhuma",
        validation_alias="IVIS_IA_STUB_FALHA",
    )
    chave_nvidia: SecretStr | None = Field(
        default=None,
        validation_alias="NVIDIA_API_KEY",
    )
    modelo_nvidia: str | None = Field(default=None, validation_alias="NVIDIA_MODEL")
    url_base_nvidia: str = Field(
        default="https://integrate.api.nvidia.com/v1",
        validation_alias="NVIDIA_BASE_URL",
    )
    timeout_nvidia_segundos: float = Field(
        default=25,
        gt=0,
        validation_alias="NVIDIA_TIMEOUT_SEGUNDOS",
    )

    model_config = SettingsConfigDict(env_file=None, extra="ignore")


configuracoes = Configuracoes()
