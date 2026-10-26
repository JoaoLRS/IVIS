# Demonstração do Marco 6 — qualidade e latência

## Escopo e estado

Este roteiro prepara a demonstração da integração do fluxo de treino com a API
de IA. O stub atual permite demonstrar chamadas, validação de contrato e
comportamento de falhas, mas **não avalia qualidade semântica**. A avaliação do
stub depende do tamanho das respostas; ela não é evidência de qualidade de um
modelo. Ainda não há adaptador NIM nem modelo de inferência confirmado, então
qualidade do modelo e latência NIM permanecem pendentes.

Os alvos p95 de 10 s para pergunta e 20 s para avaliação são propostas a medir,
não SLAs aprovados. A execução integrada de Compose requer Docker e as medições
devem registrar camada, ambiente, provedor/modelo, tamanho da amostra,
concorrência e resultado. Não usar respostas ou dados pessoais reais.

## Preparação da demonstração local

1. Instale Docker Desktop e confirme que o daemon está ativo.
2. Na raiz do repositório, crie `.env` a partir de `.env.example` e configure
   `IVIS_IA_CHAVE_SERVICO` com uma chave local de pelo menos 32 caracteres.
   Não use nem adicione uma chave NVIDIA para demonstrar o stub.
3. Inicie a pilha:

   ```powershell
   docker compose up --build -d
   docker compose ps
   ```

4. Confirme `http://localhost:8000/saude` e
   `http://localhost:8000/pronto`. Os dois devem indicar serviço ativo/pronto,
   sem incluir segredos.
5. Abra `http://localhost:8080/treinos`, inicie uma entrevista demonstrativa,
   responda às perguntas e confirme que o feedback aparece. O navegador deve
   chamar apenas o backend (`localhost:8081/api`); não deve chamar a API de IA
   ou NVIDIA diretamente.
6. Para conferir os comportamentos de falha e preservação de respostas, rode o
   teste Cypress existente do fluxo de treinos. Esse teste simula 503/504 e não
   é uma medição do provedor real.
7. Ao concluir, encerre os containers com `docker compose down`. Não use
   `-v`: o volume do banco não faz parte da limpeza desta demonstração.

## Medição reproduzível

O script envia somente payloads sintéticos, verifica status e formato da
resposta, descarta aquecimento e imprime um relatório JSON sem prompt, resposta,
token ou corpo de erro. O padrão conservador é 30 amostras por operação,
concorrência 1 e 2 chamadas de aquecimento. Ele aceita até 1.000 amostras e
concorrência até 10; qualquer carga maior deve ser combinada e executada em
ambiente autorizado.
O p95 usa o método de posto mais próximo (`ceil(0,95 × n)`) sobre chamadas
bem-sucedidas. O relatório agrega automaticamente as versões de modelo e prompt
devolvidas pela API.

Com a API de IA local ativa, no PowerShell:

```powershell
Set-Location api-ia
$env:IVIS_IA_CHAVE_SERVICO = "chave-local-com-pelo-menos-32-caracteres"
.\.venv\Scripts\python.exe scripts\medir_desempenho.py
```

Essa execução mede somente a API de IA. Para medir a rota integrada pelo
backend, aponte o modo `backend` ao serviço Spring Boot iniciado localmente:

```powershell
.\.venv\Scripts\python.exe scripts\medir_desempenho.py `
  --camada backend `
  --base-url http://127.0.0.1:8081/api
```

Se o ambiente exigir token de usuário, forneça-o em
`IVIS_TOKEN_ACESSO` no processo local. Não o passe como argumento de linha de
comando nem o inclua no relatório. Para ambiente diferente do local, confirme
autorização, representatividade, provedor/modelo e carga com a equipe antes de
executar. O código de saída é diferente de zero se houver falhas, resposta
inválida ou p95 acima do alvo proposto.

Registre junto ao JSON, sem segredos:

- data/hora, commit, ambiente e camada medida;
- provedor/modelo e versão de prompt (quando disponíveis);
- amostras, aquecimento, concorrência e demais condições da carga;
- p50, p95, máximo, falhas e comparação com os alvos propostos;
- limitações, incluindo se o resultado é do stub ou de NIM.

Não compare medições de ambientes, modelos ou cargas diferentes como se fossem
equivalentes. Um resultado do stub não demonstra latência nem qualidade do NIM.

## Protocolo proposto para avaliação humana

Esta rubrica é uma proposta para aprovação da equipe antes de pontuar qualquer
modelo. Use casos sintéticos em português e avalie separadamente cada operação.
Como ponto de partida, recomenda-se revisão independente por duas pessoas e
registro de divergências; isso não substitui aprovação da rubrica nem validação
de viés.

Pontue cada critério de 0 a 2: `0` = inadequado/ausente, `1` = parcial, `2` =
adequado. Registre exemplos e justificativa, sem inferir atributos pessoais.

| Operação | Critério |
|---|---|
| Pergunta | Pertinência ao contexto sintético informado |
| Pergunta | Correção técnica e possibilidade de resposta |
| Pergunta | Clareza, neutralidade e ausência de ambiguidade |
| Pergunta | Capacidade de elicitar raciocínio ou evidência concreta |
| Avaliação | Feedback fundamentado nas interações fornecidas |
| Avaliação | Correção técnica dos pontos fortes e de melhoria |
| Avaliação | Especificidade e utilidade prática do feedback |
| Avaliação | Coerência entre nota, justificativas e respostas |

Anote também: código do caso sintético, versão de modelo/prompt, notas por
critério, saída estruturalmente válida (sim/não), erro crítico (sim/não) e
comentários sem copiar dados reais. Não use a nota heurística do stub como
resultado de qualidade, nem transforme uma média pequena de casos em aprovação
de produção.

## Critérios de conclusão e pendências

- [x] Instrumentação local sintética e relatório agregado em JSON.
- [x] Roteiro de demonstração do fluxo, privacidade e falhas.
- [ ] Demonstração ponta a ponta executada em Compose.
- [ ] Aprovar rubrica, conjunto de casos e carga representativa com a equipe.
- [ ] Confirmar um modelo NIM inferível e o adaptador do Marco 5.
- [ ] Medir p95 integrado e qualidade humana com resultados registrados; não
      declarar SLA até a equipe aprovar os alvos.
