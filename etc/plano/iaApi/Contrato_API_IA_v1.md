# Contrato da API de IA — versão 1 (proposta)

Este documento propõe o contrato entre o backend e a API de IA para o [RF07](../IVIS_Requisitos.md) e o [RF08](../IVIS_Requisitos.md). Detalha o que o [plano da API de IA](Plano_Implementacao_API_IA.md) pede ("especificar operações, campos mínimos, formato de saída e escala"). Está **pendente de validação** da equipe e deve ser implementado primeiro por um **stub** determinístico, depois pela integração com a NVIDIA NGC, sem mudar o contrato.

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
  "versao_modelo": "stub-1",
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
  "nota": 78,
  "pontos_fortes": ["…"],
  "pontos_melhoria": ["…"],
  "versao_modelo": "stub-1",
  "versao_prompt": "avaliacao-1"
}
```

- `nota`: inteiro de **0 a 100** (proposta; o protótipo do frontend já usa essa escala).
- `pontos_fortes` e `pontos_melhoria`: de 1 a 5 itens de texto cada.

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
- Calcula `nota` de forma determinística a partir do tamanho das respostas (por exemplo, respostas vazias reduzem a nota), apenas para o backend e o frontend poderem ser testados com resultados que **dependam das respostas**.
- Devolve `versao_modelo: "stub-1"`. Um treino avaliado pelo stub é identificável pela versão persistida em `avaliacoes_treinamento`.
- Permite simular `503` e `504` por configuração do serviço (não por parâmetro da requisição).

## Pendências

- Modelo e endpoint específicos da NVIDIA NGC e limites de uso.
- Escala final e critérios de avaliação (rubrica).
- Política de retenção e diagnóstico de prompts e respostas (LGPD, RNF09).
- Mecanismo definitivo de autenticação serviço a serviço.
