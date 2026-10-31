# Contrato da API de IA — versão 1 (proposta)

Este documento define o contrato v1 entre o backend e a API de IA para o [RF07](../IVIS_Requisitos.md) e o [RF08](../IVIS_Requisitos.md). O contrato foi aprovado para orientar o stub e a integração posterior com NVIDIA NGC, sem mudança das rotas ou dos formatos HTTP.

Identificadores em português brasileiro, sem acentos, conforme o [glossário](../Glossario_IVIS.md).

## Princípios

- Só o backend chama esta API; o navegador nunca a acessa.
- O backend envia o **mínimo** necessário: nada de nome, e-mail, ids de conta ou de treino. O `id_correlacao` é um identificador aleatório da chamada, usado apenas para diagnóstico.
- A resposta da IA é conteúdo não confiável: o backend valida a estrutura e os limites antes de persistir ou exibir.
- A API de IA não persiste conteúdo e não decide acesso, plano ou continuidade de sessão.

## Autenticação entre serviços

Cabeçalho `X-Chave-Servico` com chave configurada por variável de ambiente nos dois serviços. Chamadas sem chave válida recebem `401`. Mecanismo definitivo (chave ou mTLS) ainda pendente, conforme o plano da API de IA.

## Operações

Prefixo `/v1`. Corpo e resposta em JSON, UTF-8.

### `POST /v1/perguntas/gerar`

Gera a próxima pergunta da entrevista.

Requisição:

```json
{
  "id_correlacao": "9f1c...",
  "tipo_contexto": "HABILIDADES",
  "habilidades": [{ "nome": "Vue 3", "nivel": 4 }],
  "vaga": null,
  "interacoes": [
    { "numero": 1, "pergunta": "…", "resposta": "…" }
  ],
  "numero_pergunta": 2,
  "total_perguntas": 3
}
```

- `tipo_contexto`: `HABILIDADES` ou `VAGA`. Com `HABILIDADES`, `habilidades` é obrigatório e `vaga` é `null`. Com `VAGA`, `vaga` é obrigatório: `{ "titulo", "senioridade", "requisitos": ["…"] }`.
- `interacoes`: perguntas e respostas já registradas, em ordem; vazio na primeira pergunta.

Resposta `200`:

```json
{
  "pergunta": { "texto": "…" },
  "versao_modelo": "nvidia/modelo-configurado",
  "versao_prompt": "perguntas-1"
}
```

### `POST /v1/avaliacoes/gerar`

Avalia o conjunto de perguntas e respostas do treino.

Requisição:

```json
{
  "id_correlacao": "b72e...",
  "tipo_contexto": "VAGA",
  "habilidades": null,
  "vaga": { "titulo": "…", "senioridade": "Pleno", "requisitos": ["…"] },
  "interacoes": [
    { "numero": 1, "pergunta": "…", "resposta": "…" }
  ]
}
```

Resposta `200`:

```json
{
  "nota": 80,
  "pontos_fortes": ["…"],
  "pontos_melhoria": ["…"],
  "avaliacoes_por_pergunta": [
    {
      "numero": 1,
      "pergunta": "…",
      "criterios": [
        {
          "criterio": "CORRECAO_TECNICA",
          "nota": 4,
          "justificativa": "…",
          "trecho_resposta": "trecho literal da resposta"
        },
        {
          "criterio": "COMPLETUDE",
          "nota": 4,
          "justificativa": "…",
          "trecho_resposta": "trecho literal da resposta"
        },
        {
          "criterio": "CLAREZA",
          "nota": 4,
          "justificativa": "…",
          "trecho_resposta": "trecho literal da resposta"
        }
      ],
      "nivel_confianca": "MEDIA",
      "limitacao": "…"
    }
  ],
  "avaliacao_qualitativa": true,
  "nivel_confianca": "MEDIA",
  "limitacoes": "…",
  "versao_modelo": "nvidia/modelo-configurado",
  "versao_prompt": "avaliacao-3"
}
```

- `nota`: inteiro de **0 a 100**, calculado a partir da rubrica ponderada (correção técnica 60%, completude 25% e clareza 15%). O tamanho da resposta não é critério; respostas concisas podem receber nota alta se forem corretas e completas. A nota não é garantia de acerto.
- `pontos_fortes` e `pontos_melhoria`: de 1 a 5 itens de texto na avaliação qualitativa; ficam vazios no stub.
- `avaliacoes_por_pergunta`: uma avaliação por interação; a rubrica usa `CORRECAO_TECNICA`, `COMPLETUDE` e `CLAREZA`, cada uma de 0 a 5. `trecho_resposta` deve ser uma citação literal da resposta e é validado pela API.
- `nivel_confianca`: estimativa qualitativa (`BAIXA`, `MEDIA` ou `ALTA`), não calibrada. `limitacao` e `limitacoes` registram ressalvas específicas e gerais.
- `avaliacao_qualitativa`: informa se os critérios realmente foram avaliados. É `false` no stub; nesse caso, `nota` é `null`, listas de pontos fortes e melhorias ficam vazias e os critérios por pergunta não contêm notas.

### `GET /saude` e `GET /pronto`

Liveness e readiness, sem segredos nem detalhes internos.

## Limites (propostos)

| Item | Limite |
|---|---|
| Tamanho de `resposta` | 4.000 caracteres |
| Tamanho de `pergunta` retornada | 1.000 caracteres |
| Itens em `interacoes` | 10 |
| Habilidades enviadas | 30 |
| Requisitos de vaga | 20 |
| `total_perguntas` | 1 a 10 (padrão do MVP: 3) |

## Erros

Formato: `{ "codigo": "…", "mensagem": "…" }`.

| HTTP | `codigo` | Situação | Ação do backend |
|---|---|---|---|
| 401 | `nao_autenticado` | Chave de serviço ausente ou inválida | Falha de configuração; não repetir. |
| 422 | `entrada_invalida` | Contrato violado ou limite excedido | Corrigir a requisição; não repetir. |
| 502 | `saida_invalida` | O modelo devolveu algo fora do contrato | Repetir no máximo uma vez. |
| 503 | `provedor_indisponivel` | NVIDIA NGC fora do ar ou limite de uso | Preservar as respostas e permitir nova tentativa (UC08, A4/A5). |
| 504 | `tempo_esgotado` | Limite de tempo excedido | Idem. |

## Tempo de resposta (RNF03)

Proposta inicial de SLA para discussão: **pergunta em até 10 s e avaliação em até 20 s** no percentil 95. O backend usa timeout um pouco acima desses valores. Os números só podem ser confirmados com medições na Etapa 6 da [ordem de entrega](../Ordem_de_Entrega_MVP.md).

## Comportamento do stub

- Gera perguntas a partir de um conjunto fixo, variando pelo tema (habilidade ou requisito) e pelo `numero_pergunta`.
- Não avalia o conteúdo e não produz nota nem feedback resumido. A resposta declara `avaliacao_qualitativa: false`, confiança baixa e orienta configurar um provedor real.
- Devolve `versao_modelo: "stub-1"` e `versao_prompt: "avaliacao-3"`. Um treino sem avaliação qualitativa é identificável pela versão persistida em `avaliacoes_treinamento`.
- Permite simular `503` e `504` por configuração do serviço (não por parâmetro da requisição); a readiness fica indisponível quando a simulação de falha está ativa.

## Pendências

- Modelo e endpoint específicos da NVIDIA NGC e limites de uso.
- Validar a rubrica e a calibração da confiança com especialistas e respostas de referência.
- Política de retenção e diagnóstico de prompts e respostas (LGPD, RNF09).
- Mecanismo definitivo de autenticação serviço a serviço.
