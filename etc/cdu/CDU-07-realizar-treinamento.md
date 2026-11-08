# CDU-07 - Realizar treinamento e receber feedback

## Atores

- Aluno
- Serviço de IA, acessado pelo backend
- Sistema IVIS

## Pré-condições

- O aluno está autenticado e possui plano individual ou vínculo institucional vigente.
- O aluno tem ao menos uma habilidade ou vaga para usar como contexto.
- A vaga escolhida pertence ao aluno e não está arquivada.

## Fluxo principal

1. O aluno abre Treinamentos e escolhe habilidades ou uma vaga como contexto.
2. O sistema verifica o acesso e apresenta o contexto escolhido.
3. O aluno inicia o treino.
4. O backend cria a sessão do treino, registra o contexto e a primeira pergunta gerada pelo serviço de IA e devolve seus identificadores.
5. O aluno escreve e envia uma resposta vinculada ao identificador da pergunta; o backend registra a resposta e gera a pergunta seguinte considerando as interações anteriores.
6. O passo 5 se repete até o aluno responder às três perguntas configuradas.
7. O aluno escolhe finalizar. O backend solicita a avaliação ao serviço de IA com as perguntas e respostas registradas, grava a nota e os pontos fortes e de melhoria e conclui o treino.
8. O sistema apresenta o feedback e disponibiliza o resultado no histórico conforme CDU-08.

## Fluxos alternativos e exceções

- **A1 — Sem acesso ativo:** o sistema não inicia o treino e direciona o aluno às opções de acesso.
- **A2 — Sem contexto válido:** o sistema orienta cadastrar habilidades ou vaga.
- **A3 — Falha ao gerar pergunta ou avaliação:** o sistema preserva a resposta pendente para nova tentativa e não apresenta resultado inexistente.
- **A4 — Treino cancelado antes de finalizar:** o backend marca a sessão como cancelada e ela não aparece no histórico de treinos concluídos.
- **A5 — Página recarregada ou nova sessão de acesso:** o sistema recupera a sessão em andamento do backend e permite continuar com as perguntas e respostas já registradas, conforme CDU-19.
- **E1 — Falha ao salvar:** o sistema informa que o resultado não foi registrado.

## Pós-condições

- **Sucesso:** treino concluído e avaliação armazenada para o aluno autenticado.
- O resultado não inclui treinos de outros alunos.

## Situação atual no sistema

Implementado com geração de perguntas e avaliação pelo backend/API de IA com provedor configurável. A interface usa três perguntas. Sessões em andamento, perguntas e respostas são persistidas para retomada; o histórico apresenta apenas treinos concluídos e avaliados. A avaliação é solicitada na conclusão.

## Diagrama de ações do ator

```mermaid
flowchart LR
    ator(["🧍 Aluno"])
    ator --> a1["Escolher habilidades ou vaga como contexto"]
    ator --> a2["Iniciar entrevista"]
    ator --> a3["Responder às perguntas"]
    ator --> a4["Finalizar o treino"]
    ator --> a5["Consultar feedback"]
```
