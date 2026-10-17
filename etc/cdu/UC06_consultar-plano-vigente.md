# UC06 — Consultar plano vigente

**RF:** RF05  
**Ator principal:** Aluno ou Instituição de Ensino  
**Pré-condições:** Usuário autenticado.

## Fluxo principal

1. O usuário abre Minha Conta/Meu Plano.
2. O sistema obtém o plano individual ou institucional associado à conta/vínculo.
3. O sistema apresenta modalidade, situação, período de validade e benefícios/limites aplicáveis.
4. Se houver plano institucional, o aluno visualiza a origem do acesso sem poder administrar o contrato da instituição.

## Alternativas e exceções

- **A1 — Sem plano ativo:** informar a situação e, quando aplicável, oferecer os planos disponíveis (UC10).
- **A2 — Plano vencido, cancelado ou pendente:** exibir o estado e as datas disponíveis sem apresentá-lo como ativo.
- **E1 — Falha de consulta:** informar que os dados não puderam ser carregados.

**Pós-condições:** Nenhuma alteração; apenas consulta autorizada.  
**Implementação observada:** Não há conta nem tela de plano no frontend.
