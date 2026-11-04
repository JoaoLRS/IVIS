# Convenções dos testes Cypress

Estas convenções registram o padrão encontrado na suíte e a organização das jornadas concluídas.

- Escrever nomes de cenários e funções em português, descrevendo ações e resultados observáveis.
- Centralizar dados, rotas, seletores e textos em `funcoes/constantes.js`; reutilizar funções de apoio em `funcoes/`.
- Organizar os testes de aceitação de interface por caso de uso em `cdu/`, conforme `etc/cdu/`, inclusive o CDU-16 de requisitos de senha. O CDU-17 é uma API operacional sem tela e tem teste de integração no backend. Em geral, os cenários de interface usam `cy.intercept` para controlar respostas e verificar o comportamento do frontend e os dados enviados. Respostas simuladas não comprovam persistência, auditoria ou regras do backend. O CDU-15 é uma exceção: percorre cadastro e contratação reais e consulta o evento persistido no banco descartável `ivis_e2e`.
- Executar as jornadas em `caminho_feliz/` com frontend, backend, banco e API de IA reais, sem interceptações que substituam respostas. Essas jornadas exigem acesso ao provedor NVIDIA.
- As jornadas sequenciais usam `testIsolation: false` e `retries: 0`: as etapas compartilham a sessão e dependem das anteriores. Os cenários de CDU mantêm o isolamento padrão.
- Verificar resultados visíveis e, nas jornadas reais, recarregar a página quando necessário para comprovar persistência. Evitar esperas fixas; usar asserções com repetição automática ou aliases de requisições.
- Os hooks em `../support/e2e.js` limpam as tabelas transacionais antes e depois apenas das jornadas reais em `caminho_feliz/` e do CDU-15; os cenários com mocks não alteram o banco. A tarefa acessa exclusivamente o contêiner `ivis-e2e-banco` e recusa um banco cujo nome não seja `ivis_e2e`. Execute uma suíte por vez: a limpeza apaga contas, contratos e demais dados transacionais **desse banco descartável**.

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

O Cypress usa `http://localhost:5173`. Para os cenários simulados, basta iniciar o Vite. Para as jornadas reais, inicie um PostgreSQL isolado e um backend apontado para ele; mantenha a API de IA acessível. Exemplo no PowerShell, em terminais separados:

```powershell
docker run --rm -d --name ivis-e2e-banco -e POSTGRES_DB=ivis_e2e -e POSTGRES_USER=ivis -e POSTGRES_PASSWORD=ivis_e2e_local -p 127.0.0.1:15433:5432 postgres:17-alpine

# Terminal do backend, com IVIS_JWT_CHAVE_ASSINATURA e IVIS_IA_CHAVE_SERVICO definidos:
$env:IVIS_BD_URL = 'jdbc:postgresql://localhost:15433/ivis_e2e'
$env:IVIS_BD_USUARIO = 'ivis'
$env:IVIS_BD_SENHA = 'ivis_e2e_local'
$env:IVIS_IA_URL = 'http://localhost:8000'
$env:IVIS_CORS_ORIGENS = 'http://localhost:5173'
$env:IVIS_COOKIE_SEGURO = 'false'
$env:PORT = '18081'
cd backend
.\gradlew.bat bootRun

# Terminal do frontend:
$env:VITE_API_URL = 'http://localhost:18081/api'
npm --prefix frontend run dev
```

Para executar todos os specs no PowerShell da raiz:

```powershell
npm run testar:e2e
```

Use `localhost` exatamente como configurado. Após as jornadas, pare o backend e execute `docker stop ivis-e2e-banco` para remover o banco descartável. O Compose normal e seu banco `ivis` não participam da limpeza Cypress.

Para repassar opções do Cypress, executar diretamente no pacote:

```powershell
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/*.cy.js" --config retries=0
```

Os vídeos ficam em `cypress/downloads` e as capturas de falhas em `cypress/screenshots`.
