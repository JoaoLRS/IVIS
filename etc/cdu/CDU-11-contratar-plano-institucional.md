# CDU-11 - Contratar plano institucional

## Atores

- Instituição de Ensino

## Pré-condições

- A instituição está autenticada.
- Existe oferta institucional ativa.
- Não há contrato institucional vigente que impeça nova contratação.

## Fluxo principal

1. A instituição seleciona uma oferta institucional conforme CDU-09.
2. O sistema verifica o tipo da conta e apresenta preço, duração, benefícios e capacidade incluída na oferta (50, 100 ou ilimitada).
3. A instituição revisa as condições e segue ao checkout simulado conforme CDU-12.
4. O backend valida o resultado do pagamento simulado.
5. Se aprovado, o sistema registra contrato, preço e capacidade definidos pelo catálogo.
6. O sistema registra evento de contratação e confirma a ativação.
7. A instituição pode seguir para gerenciar alunos e vagas conforme CDU-13.

## Fluxos alternativos e exceções

- **A1 — Conta de aluno tenta contratar a modalidade institucional:** o sistema rejeita a operação.
- **A2 — Já existe contrato institucional vigente:** o sistema impede duplicidade e informa a situação.
- **A3 — Pagamento simulado recusado:** o contrato não é ativado nem libera vagas.
- **A4 — Oferta indisponível ou inativa:** o sistema solicita outra seleção.
- **E1 — Falha ao registrar ou confirmar:** o sistema não apresenta o contrato como ativo.

## Pós-condições

- **Aprovado:** contrato institucional ativo, com validade e capacidade definida na oferta escolhida.
- **Recusado:** contrato não ativado e nenhuma vaga liberada.

## Situação atual no sistema

Implementado para as três capacidades predefinidas: 50 alunos (R$ 900), 100 alunos (R$ 1.750) e alunos ilimitados (R$ 5.000), cada uma por 30 dias no simulador. A instituição escolhe a capacidade ao selecionar a oferta. Não há quantidade arbitrária nem troca de plano durante contrato vigente.

## Diagrama de ações do ator

```mermaid
flowchart LR
    ator(["🧍 Instituição de Ensino"])
    ator --> a1["Selecionar oferta institucional"]
    ator --> a2["Revisar preço, duração e vagas incluídas"]
    ator --> a3["Ir ao checkout simulado"]
    ator --> a4["Enviar dados do cartão de teste"]
    ator --> a5["Consultar resultado da contratação"]
```
