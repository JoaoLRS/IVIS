# UC12 — Contratar plano institucional

**RF:** RF14  
**Ator principal:** Instituição de Ensino  
**Ator secundário:** Gateway de pagamento  
**Pré-condições:** Instituição autenticada; oferta institucional disponível.

## Fluxo principal

1. A instituição escolhe um plano institucional no UC10.
2. Informa a quantidade de vagas, quando a oferta permitir dimensionamento.
3. O sistema valida limites e calcula o preço e condições aplicáveis.
4. A instituição revisa os dados e confirma a contratação.
5. O sistema encaminha ao UC13.
6. Após confirmação de pagamento, o sistema registra plano, capacidade contratada e período de vigência.
7. O sistema registra a ação no UC16 e confirma a contratação.

## Alternativas e exceções

- **A1 — Quantidade fora dos limites:** indicar intervalo permitido e não criar cobrança.
- **A2 — Pagamento recusado/pendente:** não ativar o plano; informar estado e permitir nova tentativa ou consulta.
- **A3 — Oferta alterada antes da confirmação:** atualizar o resumo e solicitar confirmação explícita.
- **E1 — Falha de confirmação:** manter estado pendente até reconciliação com o gateway; não conceder vagas antes da aprovação.

**Pós-condições de sucesso:** Plano institucional ativo para a instituição e capacidade contratada registrada; início da gestão no UC14.  
**Implementação observada:** Não há contas institucionais, contratação, pagamento ou gestão de vagas no código atual. Regras de preço, mínimo/máximo de vagas e vigência dependem da definição comercial.
