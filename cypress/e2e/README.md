# Convenções dos testes Cypress

Estas convenções registram o padrão encontrado na suíte e a organização das jornadas concluídas.

- Escrever nomes de cenários e funções em português, descrevendo ações e resultados observáveis.
- Centralizar dados, rotas, seletores e textos em `funcoes/constantes.js`; reutilizar funções de apoio em `funcoes/`.
- Organizar os testes de aceitação de interface por caso de uso em `cdu/`, conforme `etc/cdu/`, inclusive o CDU-16 de requisitos de senha. A API técnica de limpeza de dados é verificada no backend e não pertence aos casos de uso do usuário final. Em geral, os cenários de interface usam `cy.intercept` para controlar respostas e verificar o comportamento do frontend e os dados enviados. Respostas simuladas não comprovam persistência, auditoria ou regras do backend. O CDU-15 é uma exceção: percorre cadastro e contratação reais e consulta o evento persistido no banco `ivis` do Compose.
- Executar as jornadas em `caminho_feliz/` com frontend, backend, banco e API de IA reais, sem interceptações que substituam respostas. Essas jornadas exigem acesso ao provedor NVIDIA.
- As jornadas sequenciais usam `testIsolation: false` e `retries: 0`: as etapas compartilham a sessão e dependem das anteriores. Os cenários de CDU mantêm o isolamento padrão.
- Verificar resultados visíveis e, nas jornadas reais, recarregar a página quando necessário para comprovar persistência. Evitar esperas fixas; usar asserções com repetição automática ou aliases de requisições.
- Os hooks em `../support/e2e.js` limpam apenas as contas das fixtures reais e seus dados vinculados, antes e depois das jornadas em `caminho_feliz/` e do CDU-15. A tarefa usa `docker compose exec -T ivis-banco` no mesmo banco da aplicação, sem TRUNCATE. Os e-mails `dadosTeste.aluno.email`, `alunoAtualizadoCaminhoFeliz.email` e `dadosTeste.instituicao.email` são reservados para esses testes. Execute uma suíte por vez; outras contas e o catálogo de planos são preservados.

## Estrutura dos cenários de CDU

O arquivo `caminho_feliz/perfil_aluno.cy.js` é a referência de legibilidade:
cada `it` apresenta a sequência de ações e verificações em português. Os comandos
Cypress, seletores, interceptações e asserções ficam nas funções de apoio.
Evite uma única função que esconda todo o caso de uso.

```javascript
it("cadastra uma habilidade no perfil", () => {
  prepararTelaDeHabilidades();
  prepararCadastroDeHabilidade();
  abrirCadastroDeHabilidade();
  informarNomeDaHabilidadeCdu();
  informarCategoriaDaHabilidadeCdu();
  selecionarNivelDaHabilidadeCdu();
  confirmarCadastroDaHabilidadeCdu();
  conferirCadastroDeHabilidade();
});
```

Use funções com uma responsabilidade clara: preparar respostas, abrir telas,
informar um campo, confirmar uma ação ou conferir seu resultado. Dados de cenário
ficam em `funcoes/constantes.js`; requisições e seus contratos ficam nos helpers.
As verificações de payload devem continuar presentes durante refatorações.
O padrão de escrita não exige compartilhar estado entre os testes: os CDUs
continuam isolados e usam respostas simuladas.

## Executar a suíte

O Cypress usa `http://localhost:8080`, servido pelo container do frontend. Use a mesma stack e o mesmo banco da aplicação. Não é necessário iniciar o Vite nem outro PostgreSQL ou backend.

```powershell
# Subir os serviços (incluindo o frontend):
.\release.ps1 up

# Executar a suíte da raiz do repositório:
npm run testar:e2e
```

Remova do terminal eventuais sobrescritas antigas de `VITE_API_URL` para outro backend. As jornadas de treino exigem a chave NVIDIA configurada em `api-ia/.env`. Use `localhost` como configurado e mantenha os serviços ativos durante toda a execução.

Para validar a limpeza seletiva no mesmo banco, sem deixar dados de verificação (a transação é revertida):

```powershell
node --test cypress/tasks/limpar-banco.integracao.test.mjs
```

Para repassar opções do Cypress, executar diretamente no pacote:

```powershell
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/*.cy.js" --config retries=0
```

Os vídeos ficam em `cypress/downloads` e as capturas de falhas em `cypress/screenshots`.
