# UC02 — Fazer login

**RF:** RF01  
**Ator principal:** Aluno ou Instituição de Ensino  
**Pré-condições:** Conta cadastrada e habilitada (UC01); usuário não autenticado.

## Fluxo principal

1. O usuário abre a tela de login e informa e-mail e senha.
2. O sistema valida as credenciais no serviço de autenticação.
3. O sistema inicia uma sessão autenticada protegida, associada à conta e ao seu perfil.
4. O sistema aplica as permissões correspondentes e encaminha o usuário à área autorizada.

## Alternativas e exceções

- **A1 — Credenciais incorretas:** informar falha sem indicar se foi e-mail ou senha; manter o usuário na tela.
- **A2 — Conta pendente, bloqueada ou desativada:** não iniciar sessão e apresentar a orientação compatível com o estado.
- **A3 — Esqueceu a senha:** encaminhar ao UC05.
- **E1 — Serviço indisponível:** informar indisponibilidade sem criar sessão parcial.

**Pós-condições de sucesso:** Sessão ativa e identidade/perfil carregados sob a política de sessão definida pelo backend.  
**Implementação observada:** Não há autenticação ou controle de sessão no frontend atual.
