# UC10 — Visualizar planos disponíveis

**RF:** RF11 e RF12  
**Ator principal:** Visitante não autenticado, Aluno ou Instituição de Ensino  
**Pré-condições:** Nenhuma para consulta pública.

## Fluxo principal

1. O visitante acessa a Home de planos.
2. O sistema apresenta ofertas individuais e institucionais, preços, benefícios, limites e condições vigentes.
3. O usuário compara opções e seleciona uma para conhecer/contratar.
4. Se não estiver autenticado, o sistema solicita login ou cadastro e preserva a oferta escolhida quando possível.

## Alternativas e exceções

- **A1 — Usuário autenticado com plano vigente:** destacar o plano atual; permitir consultar detalhes (UC06).
- **A2 — Oferta indisponível ou alterada:** apresentar informação atual e pedir nova seleção antes de contratar.
- **E1 — Falha ao carregar planos:** informar indisponibilidade e não exibir preço desatualizado como garantido.

**Pós-condições:** Nenhuma alteração ou autenticação obrigatória para visualizar.  
**Implementação observada:** Não há Home comercial ou catálogo de planos no frontend.
