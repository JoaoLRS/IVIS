# CDU-05 - Consultar plano vigente

## Atores

- Aluno
- Instituição de Ensino
- Sistema IVIS

## Pré-condições

- O usuário está autenticado.
- O sistema identifica a conta pela sessão autenticada.

## Fluxo principal

1. O usuário solicita consultar seu plano ou situação de acesso.
2. O sistema consulta os contratos vigentes da conta.
3. Para o aluno, o sistema considera plano individual próprio e eventual vínculo institucional ativo.
4. Para a instituição, o sistema considera somente contratos cujo titular é a própria instituição.
5. O sistema apresenta modalidade, plano, origem do acesso, situação, validade, benefícios e limites disponíveis para aquele perfil.
6. Se não houver acesso vigente, o sistema informa a situação e permite consultar as ofertas públicas conforme CDU-09.

## Fluxos alternativos e exceções

- **A1 — Sem plano ou vínculo vigente:** o sistema informa que não há acesso ativo.
- **A2 — Contrato vencido ou encerrado:** o sistema não o apresenta como vigente.
- **A3 — Aluno com vínculo institucional:** o sistema informa a origem do acesso sem expor dados da instituição nem permitir administrar seu contrato.
- **E1 — Falha na consulta:** o sistema informa que não foi possível obter os dados do plano.

## Pós-condições

- Nenhuma alteração é feita; são consultados somente dados de acesso associados à conta.

## Situação atual no sistema

Implementado. A API retorna contratos próprios ou por vínculo. A página `/meu-plano`, disponível no menu para aluno e instituição mesmo sem plano ativo, apresenta plano, origem, validade, benefícios e capacidade quando o usuário é titular. Essas informações ficam em um card com status vigente, blocos de validade/modalidade/capacidade e benefícios. A aplicação usa Roboto Mono em todas as páginas. Falhas permitem repetir a consulta; ausência de plano oferece acesso ao catálogo.

## Cobertura automatizada

- `cypress/e2e/cdu/CDU-05-consultar-plano-vigente.cy.js` verifica plano e benefícios, estado sem plano e direcionamento segundo o acesso institucional. O primeiro cenário também confere a estrutura do card e a fonte global Roboto Mono.
- `frontend/tests/unit/paginas/MeuPlano.test.ts` cobre renderização do card, capacidade/benefícios, estado vazio e nova tentativa após falha. As respostas são simuladas; regras de titularidade e origem são verificadas no backend.

## Diagrama de ações do ator

```mermaid
flowchart LR
    ator(["🧍 Aluno ou Instituição"])
    ator --> a1["Solicitar consulta do plano"]
    ator --> a2["Consultar situação e validade"]
    ator --> a3["Consultar origem, benefícios e limites"]
```
