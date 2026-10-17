# UC11 — Contratar plano individual

**RF:** RF13 e RF19  
**Ator principal:** Aluno Pagante  
**Atores secundários:** Gateway de pagamento  
**Pré-condições:** Usuário autenticado; não possui vínculo institucional ativo que o classifique como AlunoConsórcio; oferta individual disponível.

## Fluxo principal

1. O Aluno Pagante seleciona uma oferta individual no UC10.
2. O sistema confirma elegibilidade e apresenta preço, período e benefícios.
3. O aluno confirma a contratação e segue para pagamento (UC13).
4. Após confirmação verificável do gateway, o sistema registra o contrato e ativa o plano na conta correta.
5. O sistema registra a ação sensível no UC16 e confirma o resultado ao aluno.

## Alternativas e exceções

- **A1 — AlunoConsórcio com vínculo ativo:** bloquear a contratação individual e explicar que o acesso decorre do consórcio (RF19).
- **A2 — Já possui contratação individual incompatível/ativa:** informar estado e aplicar a política de contratação definida.
- **A3 — Pagamento recusado ou pendente:** não ativar como plano pago; exibir estado e permitir acompanhar/re tentar conforme retorno do gateway.
- **E1 — Retorno duplicado ou tardio:** processar idempotentemente e não criar ativações/cobranças duplicadas.

**Pós-condições de sucesso:** Plano individual ativo e associado ao Aluno Pagante; auditoria registrada.  
**Implementação observada:** Não há fluxo de contratação ou controle de modalidade no frontend.
