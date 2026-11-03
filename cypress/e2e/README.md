# Convenções dos testes Cypress

Estas convenções registram o padrão encontrado na suíte e a organização das jornadas concluídas.

- Escrever nomes de cenários e funções em português, descrevendo ações e resultados observáveis.
- Centralizar dados, rotas, seletores e textos em `funcoes/constantes.js`; reutilizar funções de apoio em `funcoes/`.
- Organizar os testes de aceitação por caso de uso em `cdu/`, conforme `etc/cdu/`. Em geral, esses cenários usam `cy.intercept` para controlar respostas e verificar o comportamento do frontend e os dados enviados. Respostas simuladas não comprovam persistência, auditoria ou regras do backend. O CDU-15 é uma exceção: percorre cadastro e contratação reais e consulta o evento persistido no banco local.
- Executar as jornadas em `caminho_feliz/` com frontend, backend, banco e API de IA reais, sem interceptações que substituam respostas. O provedor de IA pode ser o stub configurado no serviço local.
- As jornadas sequenciais usam `testIsolation: false` e `retries: 0`: as etapas compartilham a sessão e dependem das anteriores. Os cenários de CDU mantêm o isolamento padrão.
- Verificar resultados visíveis e, nas jornadas reais, recarregar a página quando necessário para comprovar persistência. Evitar esperas fixas; usar asserções com repetição automática ou aliases de requisições.
- Os hooks em `../support/e2e.js` limpam as tabelas transacionais antes e depois apenas das jornadas reais em `caminho_feliz/` e do CDU-15; os cenários com mocks não alteram o banco. A limpeza ocorre automaticamente, sem variável de confirmação, e a tarefa recusa bancos cujo nome não seja `ivis`. Usar somente o banco local descartável do Compose e executar uma suíte por vez: a limpeza apaga contas, contratos e demais dados transacionais.

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

O Cypress usa `http://localhost:5173`, origem permitida pelo backend e servida pelo Vite com o código-fonte atual. Inicie o frontend e mantenha os serviços reais ativos quando executar jornadas reais:

```powershell
.\release.ps1 up
npm --prefix frontend run dev
```

Para executar todos os specs no PowerShell da raiz:

```powershell
npm run testar:e2e
```

Use `localhost` exatamente como configurado. O backend tem CORS restrito a `http://localhost:5173`; acessar o Vite por `127.0.0.1` ou em outra porta faz as jornadas reais falharem na criação de sessão/conta.

Para repassar opções do Cypress, executar diretamente no pacote:

```powershell
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/*.cy.js" --config retries=0
```

Os vídeos ficam em `cypress/downloads` e as capturas de falhas em `cypress/screenshots`.
