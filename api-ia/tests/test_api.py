from fastapi.testclient import TestClient
from pydantic import SecretStr

from app import auth, config, rotas
from app.main import app
from app.schemas import AvaliacaoPergunta, calcular_nota_rubrica
from app.servico import ServicoIA

CHAVE_TESTE = "chave-de-teste-com-mais-de-32-caracteres"
cliente = TestClient(app)


def setup_function() -> None:
    config.configuracoes.chave_servico = SecretStr(CHAVE_TESTE)
    config.configuracoes.chave_nvidia = None
    config.configuracoes.usar_stub = True
    config.configuracoes.falha_stub = "nenhuma"


def test_liveness_nao_expoe_configuracoes() -> None:
    resposta = cliente.get("/saude")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "ATIVO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_do_stub_nao_expoe_configuracoes() -> None:
    resposta = cliente.get("/pronto")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "PRONTO"}
    assert CHAVE_TESTE not in resposta.text


def test_readiness_indisponivel_quando_stub_e_provedor_real_estao_desativados(
    monkeypatch,
) -> None:
    config.configuracoes.usar_stub = False
    monkeypatch.setattr(config.configuracoes, "chave_nvidia", None)

    resposta = cliente.get("/pronto")

    assert resposta.status_code == 503
    assert resposta.json() == {
        "codigo": "provedor_indisponivel",
        "mensagem": "O provedor ativo não está disponível.",
    }


def test_readiness_disponivel_com_provedor_real_configurado(monkeypatch) -> None:
    config.configuracoes.usar_stub = False
    monkeypatch.setattr(
        config.configuracoes,
        "chave_nvidia",
        SecretStr("chave-nvidia-de-teste"),
    )

    resposta = cliente.get("/pronto")

    assert resposta.status_code == 200
    assert resposta.json() == {"situacao": "PRONTO"}
    assert "chave-nvidia-de-teste" not in resposta.text


def test_readiness_indisponivel_quando_falha_stub_configurada() -> None:
    config.configuracoes.falha_stub = "indisponivel"

    resposta = cliente.get("/pronto")

    assert resposta.status_code == 503
    assert resposta.json()["codigo"] == "provedor_indisponivel"


def test_rota_protegida_rejeita_chave_ausente() -> None:
    resposta = cliente.post("/v1/perguntas/gerar", json={})

    assert resposta.status_code == 401
    assert resposta.json()["codigo"] == "nao_autenticado"


def test_rota_protegida_rejeita_chave_invalida() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": "chave incorreta"},
        json={},
    )

    assert resposta.status_code == 401
    assert resposta.json()["codigo"] == "nao_autenticado"


def test_gerar_pergunta_retorna_resposta_deterministica() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        },
    )

    assert resposta.status_code == 200
    assert resposta.json() == {
        "pergunta": {
            "texto": "Como você aplicaria Python em um projeto real e quais decisões técnicas tomaria?"
        },
        "versao_modelo": "stub-1",
        "versao_prompt": "perguntas-1",
    }


def test_rota_pergunta_usa_provedor_injetado(monkeypatch) -> None:
    entrada = {
        "id_correlacao": "correlacao-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 4}],
        "vaga": None,
        "interacoes": [],
        "numero_pergunta": 1,
        "total_perguntas": 3,
    }
    resposta_provedor = {
        "pergunta": {"texto": "Pergunta do provedor"},
        "versao_modelo": "fake-1",
        "versao_prompt": "fake-prompt-1",
    }

    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            assert recebido.id_correlacao == "correlacao-123"
            return resposta_provedor

        async def gerar_avaliacao(self, recebido):
            raise AssertionError("Não deveria gerar avaliação neste teste.")

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )

    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json=entrada,
    )

    assert resposta.status_code == 200
    assert resposta.json() == resposta_provedor


def test_rota_avaliacao_usa_provedor_injetado(monkeypatch) -> None:
    resposta_provedor = {
        "nota": 82,
        "pontos_fortes": ["Explicou a solução com clareza."],
        "pontos_melhoria": ["Detalhar as alternativas consideradas."],
        "avaliacoes_por_pergunta": [
            {
                "numero": 1,
                "pergunta": "Como usa Python?",
                "criterios": [
                    {
                        "criterio": "CORRECAO_TECNICA",
                        "nota": 4,
                        "justificativa": "A resposta menciona experiência.",
                        "trecho_resposta": "Tenho experiência.",
                    },
                    {
                        "criterio": "COMPLETUDE",
                        "nota": 3,
                        "justificativa": "Não detalha exemplos.",
                        "trecho_resposta": "Tenho experiência.",
                    },
                    {
                        "criterio": "CLAREZA",
                        "nota": 5,
                        "justificativa": "A frase é direta.",
                        "trecho_resposta": "Tenho experiência.",
                    },
                ],
                "nivel_confianca": "MEDIA",
                "limitacao": "A resposta é curta para confirmar domínio técnico.",
            }
        ],
        "avaliacao_qualitativa": True,
        "nivel_confianca": "MEDIA",
        "limitacoes": "Uma resposta curta pode omitir conhecimentos relevantes.",
        "versao_modelo": "fake-1",
        "versao_prompt": "fake-prompt-2",
    }

    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            raise AssertionError("Não deveria gerar pergunta neste teste.")

        async def gerar_avaliacao(self, recebido):
            assert recebido.id_correlacao == "correlacao-123"
            return resposta_provedor

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [
                {
                    "numero": 1,
                    "pergunta": "Como usa Python?",
                    "resposta": "Tenho experiência.",
                }
            ],
        },
    )

    assert resposta.status_code == 200
    assert resposta.json() == {**resposta_provedor, "nota": 78}


def test_rota_avaliacao_rejeita_saida_invalida_do_provedor(monkeypatch) -> None:
    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            raise AssertionError("Não deveria gerar pergunta neste teste.")

        async def gerar_avaliacao(self, recebido):
            return {
                "nota": 101,
                "pontos_fortes": ["Ponto forte"],
                "pontos_melhoria": ["Melhoria"],
                "versao_modelo": "fake-1",
                "versao_prompt": "fake-prompt-1",
            }

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
        },
    )

    assert resposta.status_code == 502
    assert resposta.json() == {
        "codigo": "saida_invalida",
        "mensagem": "O provedor retornou uma resposta inválida.",
    }


def test_rota_avaliacao_rejeita_evidencia_que_nao_existe_na_resposta(monkeypatch) -> None:
    class ProvedorFalso:
        async def gerar_pergunta(self, recebido):
            raise AssertionError("Não deveria gerar pergunta neste teste.")

        async def gerar_avaliacao(self, recebido):
            criterios = [
                {
                    "criterio": criterio,
                    "nota": 4,
                    "justificativa": "Justificativa de teste.",
                    "trecho_resposta": "Trecho inventado.",
                }
                for criterio in (
                    "CORRECAO_TECNICA",
                    "COMPLETUDE",
                    "CLAREZA",
                )
            ]
            return {
                "nota": 80,
                "pontos_fortes": ["Resposta objetiva."],
                "pontos_melhoria": ["Inclua detalhes."],
                "avaliacoes_por_pergunta": [
                    {
                        "numero": 1,
                        "pergunta": "Como usa Python?",
                        "criterios": criterios,
                        "nivel_confianca": "MEDIA",
                        "limitacao": "Resposta curta.",
                    }
                ],
                "avaliacao_qualitativa": True,
                "nivel_confianca": "MEDIA",
                "limitacoes": "Resposta curta.",
                "versao_modelo": "fake-1",
                "versao_prompt": "fake-prompt-2",
            }

    monkeypatch.setattr(
        rotas,
        "obter_servico_ativo",
        lambda: ServicoIA(ProvedorFalso()),
    )
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [
                {
                    "numero": 1,
                    "pergunta": "Como usa Python?",
                    "resposta": "Tenho experiência.",
                }
            ],
        },
    )

    assert resposta.status_code == 502
    assert resposta.json()["codigo"] == "saida_invalida"


def test_gerar_pergunta_varia_com_numero_e_contexto() -> None:
    payload = {
        "id_correlacao": "correlacao-123",
        "tipo_contexto": "VAGA",
        "habilidades": None,
        "vaga": {
            "titulo": "Desenvolvedor",
            "senioridade": "Pleno",
            "requisitos": ["Python", "PostgreSQL"],
        },
        "interacoes": [],
        "numero_pergunta": 2,
        "total_perguntas": 3,
    }

    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json=payload,
    )

    assert resposta.status_code == 200
    assert resposta.json()["pergunta"]["texto"] == (
        "Descreva um desafio envolvendo Python e como avaliaria as alternativas para resolvê-lo."
    )


def test_stub_nao_avalia_a_qualidade_nem_o_tamanho_das_respostas() -> None:
    payload = {
        "id_correlacao": "correlacao-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 4}],
        "vaga": None,
        "interacoes": [
            {"numero": 1, "pergunta": "Pergunta", "resposta": "x" * 250},
            {"numero": 2, "pergunta": "Pergunta", "resposta": ""},
        ],
    }

    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json=payload,
    )

    assert resposta.status_code == 200
    resposta_curta = resposta.json()
    payload["interacoes"][0]["resposta"] = "x"
    payload["interacoes"][1]["resposta"] = "y" * 4000
    resposta_longa = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json=payload,
    )

    assert resposta.status_code == 200
    assert resposta_longa.status_code == 200
    assert resposta_curta == resposta_longa.json()
    assert resposta_curta == {
        "nota": None,
        "pontos_fortes": [],
        "pontos_melhoria": [],
        "avaliacoes_por_pergunta": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "criterios": [],
                "nivel_confianca": "BAIXA",
                "limitacao": (
                    "Modo demonstrativo: a avaliação qualitativa está desativada. "
                    "Configure um provedor de IA para receber feedback sobre a "
                    "qualidade das respostas."
                ),
            },
            {
                "numero": 2,
                "pergunta": "Pergunta",
                "criterios": [],
                "nivel_confianca": "BAIXA",
                "limitacao": (
                    "Modo demonstrativo: a avaliação qualitativa está desativada. "
                    "Configure um provedor de IA para receber feedback sobre a "
                    "qualidade das respostas."
                ),
            },
        ],
        "avaliacao_qualitativa": False,
        "nivel_confianca": "BAIXA",
        "limitacoes": (
            "Modo demonstrativo: a avaliação qualitativa está desativada. Configure "
            "um provedor de IA para receber feedback sobre a qualidade das respostas."
        ),
        "versao_modelo": "stub-1",
        "versao_prompt": "avaliacao-3",
    }


def test_nota_pondera_correcao_tecnica_acima_de_estilo() -> None:
    avaliacao = AvaliacaoPergunta.model_validate(
        {
            "numero": 1,
            "pergunta": "Como aplica Python?",
            "criterios": [
                {
                    "criterio": "CORRECAO_TECNICA",
                    "nota": 5,
                    "justificativa": "Resposta tecnicamente correta.",
                    "trecho_resposta": "Trecho literal.",
                },
                {
                    "criterio": "COMPLETUDE",
                    "nota": 0,
                    "justificativa": "Faltam detalhes.",
                    "trecho_resposta": "Trecho literal.",
                },
                {
                    "criterio": "CLAREZA",
                    "nota": 0,
                    "justificativa": "Pouco claro.",
                    "trecho_resposta": "Trecho literal.",
                },
            ],
            "nivel_confianca": "MEDIA",
            "limitacao": "Exemplo para teste.",
        }
    )

    assert calcular_nota_rubrica([avaliacao]) == 60


def test_stub_mapeia_indisponibilidade_e_timeout() -> None:
    payload = {
        "id_correlacao": "correlacao-123",
        "tipo_contexto": "HABILIDADES",
        "habilidades": [{"nome": "Python", "nivel": 4}],
        "vaga": None,
        "interacoes": [],
        "numero_pergunta": 1,
        "total_perguntas": 3,
    }
    headers = {"X-Chave-Servico": CHAVE_TESTE}

    config.configuracoes.falha_stub = "indisponivel"
    indisponivel = cliente.post("/v1/perguntas/gerar", headers=headers, json=payload)
    assert indisponivel.status_code == 503
    assert indisponivel.json()["codigo"] == "provedor_indisponivel"

    config.configuracoes.falha_stub = "tempo_esgotado"
    timeout = cliente.post("/v1/perguntas/gerar", headers=headers, json=payload)
    assert timeout.status_code == 504
    assert timeout.json()["codigo"] == "tempo_esgotado"


def test_operacao_indisponivel_quando_stub_desativado() -> None:
    config.configuracoes.usar_stub = False
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [],
            "numero_pergunta": 1,
            "total_perguntas": 3,
        },
    )

    assert resposta.status_code == 503
    assert resposta.json()["codigo"] == "provedor_indisponivel"


def test_chave_e_comparada_com_hmac_compare_digest(monkeypatch) -> None:
    chamada: list[tuple[str, str]] = []

    def comparar(chave_recebida: str, chave_configurada: str) -> bool:
        chamada.append((chave_recebida, chave_configurada))
        return False

    monkeypatch.setattr(auth.hmac, "compare_digest", comparar)
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": "chave-invalida"},
        json={},
    )

    assert resposta.status_code == 401
    assert chamada == [("chave-invalida", CHAVE_TESTE)]


def test_validacao_de_entrada_usa_erro_do_contrato() -> None:
    resposta = cliente.post(
        "/v1/perguntas/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={"tipo_contexto": "DESCONHECIDO"},
    )

    assert resposta.status_code == 422
    assert resposta.json() == {
        "codigo": "entrada_invalida",
        "mensagem": "A requisição não atende ao contrato da API.",
    }


def test_validacao_rejeita_resposta_acima_do_limite() -> None:
    resposta = cliente.post(
        "/v1/avaliacoes/gerar",
        headers={"X-Chave-Servico": CHAVE_TESTE},
        json={
            "id_correlacao": "correlacao-123",
            "tipo_contexto": "HABILIDADES",
            "habilidades": [{"nome": "Python", "nivel": 4}],
            "vaga": None,
            "interacoes": [
                {
                    "numero": 1,
                    "pergunta": "Pergunta",
                    "resposta": "x" * 4001,
                }
            ],
        },
    )

    assert resposta.status_code == 422
    assert resposta.json()["codigo"] == "entrada_invalida"
