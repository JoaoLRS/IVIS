# UC15 — Aplicar acesso conforme modalidade

**RF:** RF17; regra transversal usada pelos casos protegidos  
**Ator principal:** Usuário autenticado (Aluno Pagante, AlunoConsórcio ou Instituição de Ensino)  
**Pré-condições:** Sessão autenticada; conta, plano e vínculos disponíveis para autorização.

## Fluxo principal

1. O sistema identifica o tipo de conta e consulta plano individual ou vínculo institucional ativo.
2. Ao acessar uma funcionalidade protegida, o sistema avalia situação, vigência e permissões.
3. O sistema concede somente os recursos autorizados para a modalidade vigente.
4. Em cada operação institucional, o sistema verifica também que o recurso pertence à instituição autenticada (RNF07).

## Alternativas e exceções

- **A1 — AlunoConsórcio com vínculo ativo:** conceder acesso coberto pelo consórcio; impedir contratação individual conforme RF19/UC11.
- **A2 — Aluno Pagante com plano individual ativo:** conceder os recursos incluídos no plano individual.
- **A3 — Sem plano/vínculo ativo ou expirado:** negar funções pagas e apresentar orientação apropriada.
- **A4 — Vínculo removido ou plano revogado:** recalcular permissões nas próximas operações e encerrar/restringir sessões conforme política definida.
- **E1 — Estado inconsistente ou não verificável:** negar acesso protegido com segurança e registrar falha para investigação.

**Pós-condições:** Decisão de acesso aplicada e sem exposição de dados de outra instituição.  
**Implementação observada:** O frontend não tem autenticação, papéis ou autorização; menu e rotas atuais não demonstram controles por modalidade.
