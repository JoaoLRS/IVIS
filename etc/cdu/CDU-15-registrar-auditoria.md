# CDU-15 - Registrar auditoria de operações sensíveis

## Atores

- Aluno ou Instituição de Ensino que inicia uma operação sensível
- Sistema IVIS

## Pré-condições

- O usuário está autenticado.
- Uma operação sensível coberta pela política de auditoria está sendo executada.
- O sistema consegue identificar o responsável e a operação associada.

## Fluxo principal

1. O usuário inicia uma contratação, pagamento, inclusão ou remoção de vínculo institucional.
2. O sistema executa as validações de acesso e de negócio.
3. O sistema registra ator, ação, resultado, recurso afetado e data/hora, sem incluir dados sensíveis desnecessários.
4. O sistema associa o evento à operação para permitir rastreabilidade.
5. O sistema conclui a operação e apresenta o resultado ao usuário.

## Fluxos alternativos e exceções

- **A1 — Operação recusada:** quando coberto pela política, o sistema registra o resultado como recusado, distinguindo-o de uma alteração concluída.
- **A2 — Operação repetida:** a chave de idempotência evita eventos de sucesso duplicados para a mesma contratação.
- **E1 — Falha ao registrar auditoria:** a política transacional de auditoria deve definir se a operação é interrompida ou encaminhada para recuperação; a regra completa está pendente.
- **A3 — Ação automatizada:** o sistema identifica a origem técnica e preserva o ator que iniciou a operação quando disponível.

## Pós-condições

- **Sucesso:** evento de auditoria associado à operação sensível.
- Consulta, proteção e retenção seguem perfis autorizados e política definida.

## Situação atual no sistema

Parcial. A tabela de auditoria existe e o backend registra aprovações e recusas de contratação/pagamento simulados. Inclusão, remoção e outras operações sensíveis ainda não são cobertas. Não há visualizador; a política de retenção e proteção contra alteração exigida pelo RNF09 também está pendente.

## Diagrama de ações do ator

```mermaid
flowchart LR
    ator(["🧍 Usuário de operação sensível"])
    ator --> a1["Iniciar contratação ou gestão de vínculo"]
    a1 --> s1["Sistema valida e registra o evento"]
    s1 --> a2["Consultar resultado da operação"]
```
