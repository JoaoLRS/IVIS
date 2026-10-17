# UC03 — Gerenciar dados da conta

**RF:** RF02  
**Ator principal:** Aluno ou Instituição de Ensino  
**Pré-condições:** Usuário autenticado; acesso apenas à própria conta.

## Fluxo principal

1. O usuário abre Minha Conta.
2. O sistema exibe os dados cadastrais permitidos para seu perfil.
3. O usuário altera um ou mais campos e confirma.
4. O sistema valida os dados e verifica unicidade dos identificadores alterados.
5. O sistema salva as mudanças e confirma a atualização.

## Alternativas e exceções

- **A1 — Dados inválidos:** apontar os campos e não gravar a alteração.
- **A2 — E-mail já utilizado:** rejeitar a mudança e manter o e-mail atual.
- **A3 — Alteração de credencial:** direcionar para o fluxo seguro de troca de senha, solicitando verificação da identidade; nunca exibir a senha atual.
- **E1 — Falha ao salvar:** manter os dados anteriores e informar que a atualização não foi concluída.

**Pós-condições:** Dados válidos atualizados somente na conta do ator.  
**Implementação observada:** Não há tela/serviço de conta no frontend. Foto/logo e os campos editáveis devem respeitar o tipo de perfil e ser confirmados pela equipe.
