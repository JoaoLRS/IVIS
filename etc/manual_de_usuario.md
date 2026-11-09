# Manual do usuário — IVIS

## 1. Sobre o IVIS

O **IVIS (Intelligent Virtual Interview System)** foi criado para ajudar estudantes a praticar entrevistas técnicas. Você pode começar um treino com base nas suas habilidades ou em uma vaga que chamou sua atenção. Depois de responder às perguntas por texto, recebe um feedback e pode rever os treinos no histórico.

Há duas formas de acesso: contratar um plano individual ou ser vinculado a uma instituição de ensino com plano ativo. As instituições têm uma área própria para acompanhar e administrar os alunos vinculados.

Neste manual, você encontra os passos para instalar, iniciar e usar o IVIS. Durante a aula, a equipe também disponibilizará a aplicação pela AWS Academy. 
Esse ambiente fica ativo por até quatro horas, então será iniciado pouco antes da aula. Se quiser rodar o projeto no seu computador, siga os passos a seguir.
Além, disso a equipe disponibilizara dois notebook já configurado para realizar os testes. 

## 2. Clonar o repositório

### Pré-requisitos

- Git instalado para clonar o repositório;
- Conexão com a internet para baixar o repositório.

### Passos

1. Abra o PowerShell ou um terminal.
2. Acesse a pasta onde deseja guardar o projeto. No PowerShell:

   ```powershell
   cd C:\projetos
   ```

   No Linux ou macOS, use o terminal e um caminho da sua pasta pessoal, por exemplo:

   ```bash
   mkdir -p ~/projetos
   cd ~/projetos
   ```

3. Clone o repositório:

   ```powershell
   git clone https://github.com/JoaoLRS/IVIS.git
   ```

4. Entre na pasta criada:

   ```powershell
   cd IVIS
   ```

Para atualizar uma cópia já clonada, execute `git pull` dentro da pasta do projeto.

### Tecnologias necessárias para executar o projeto

Depois de clonar o repositório, instale as ferramentas necessárias conforme a forma como pretende executar o IVIS. Os links abaixo levam aos sites oficiais:

| Tecnologia | Quando é necessária | Download oficial |
| --- | --- | --- |
| Git | Para clonar e atualizar o repositório. | [Windows](https://git-scm.com/install/windows), [macOS](https://git-scm.com/install/mac) ou [página oficial com opções por sistema](https://git-scm.com/downloads/) |
| Docker | Necessário para executar a aplicação completa em containers. No Windows e macOS, instale Docker Desktop; no Linux, use Docker Desktop ou Docker Engine com Docker Compose. | [Windows](https://docs.docker.com/desktop/setup/install/windows-install/), [macOS](https://docs.docker.com/desktop/setup/install/mac-install/), [Linux: Docker Desktop](https://docs.docker.com/desktop/setup/install/linux/) ou [Linux: Docker Engine](https://docs.docker.com/engine/install/) |
| Node.js 22 ou superior | Necessário para `config.ps1` no Windows e para desenvolver/compilar o frontend localmente. O npm é instalado junto com Node.js. | [Download Node.js para Windows, macOS e Linux](https://nodejs.org/en/download) |
| Python 3.12 ou 3.13 | Necessário para instalar e executar a API de IA localmente e para `config.ps1`. | [Downloads oficiais por sistema](https://www.python.org/downloads/), [Windows](https://www.python.org/downloads/windows/) ou [macOS](https://www.python.org/downloads/macos/) |
| JDK 25 | Necessário para compilar ou executar o backend fora do Docker e para `config.ps1`. O Gradle já está incluído no repositório pelo Gradle Wrapper. | [Oracle JDK 25 para Windows, macOS e Linux](https://www.oracle.com/br/java/technologies/downloads/); alternativa: [Eclipse Temurin JDK 25](https://adoptium.net/temurin/releases/?version=25) |

Para iniciar o IVIS pelo Docker, basta ter Docker disponível e em execução depois de clonar o repositório: Docker Desktop no Windows/macOS, ou Docker Desktop/Docker Engine com Compose no Linux. Git é usado para clonar ou atualizar os arquivos; Node.js, Python e JDK não precisam estar instalados localmente para essa forma de execução. Essas três ferramentas são exigidas pelo `config.ps1` no Windows, que instala dependências e compila módulos no computador.

## 3. Configurar e iniciar o IVIS no computador

Escolha o procedimento de acordo com o sistema operacional: no Windows, use os scripts PowerShell; no Linux ou macOS, use o script Bash; para desenvolver os módulos fora dos containers, instale também as dependências de cada tecnologia. Para apenas usar a aplicação, o Docker executa os serviços sem exigir Node.js, Python ou JDK instalados no computador.

### Usar os arquivos de ambiente fornecidos para a aula

Junto com o projeto, a professora e os alunos receberão dois arquivos na pasta `env/`. Antes de executar `config.ps1`, `release.ps1` ou `release.sh`, copie cada arquivo para o local em que a aplicação o procura:

| Arquivo recebido | Copiar para | Uso |
| --- | --- | --- |
| `env/env_main/.env` | `.env`, na raiz do repositório | Configurações do banco, do backend e da comunicação com a API de IA. |
| `env/env_api_ia/.env` | `api-ia/.env` | Configurações do provedor de IA, incluindo `NVIDIA_API_KEY`. |

No **Windows**, abra o PowerShell na raiz do repositório e execute:

```powershell
Copy-Item -LiteralPath 'env/env_main/.env' -Destination '.env' -Force
Copy-Item -LiteralPath 'env/env_api_ia/.env' -Destination 'api-ia/.env' -Force
```

No **Linux ou macOS**, abra o terminal na raiz do repositório e execute:

```bash
cp env/env_main/.env .env
cp env/env_api_ia/.env api-ia/.env
```

Esses comandos substituem os arquivos de destino caso já existam, por exemplo, se uma execução anterior dos scripts criou arquivos a partir dos exemplos. A chave `IVIS_IA_CHAVE_SERVICO` usada pelos dois containers é lida do `.env` da raiz pelo Docker Compose; mantenha os arquivos recebidos nos destinos indicados. Depois da cópia, siga a opção de inicialização do seu sistema operacional abaixo.

### Opção A — preparar com `config.ps1` e iniciar com `release.ps1`

Abra o PowerShell na raiz do repositório. Para instalar as dependências locais e compilar os módulos, execute:

```powershell
.\config.ps1 install
```

O comando `install` exige Node.js 22 ou superior, Python 3.12 ou 3.13 e JDK 25. Ele instala as dependências do projeto, frontend e testes E2E, cria o ambiente virtual da API de IA e compila o backend e o frontend. Não é necessário instalar o Gradle globalmente; o projeto inclui Gradle Wrapper.

Para sincronizar dependências e atualizar pacotes npm dentro das faixas definidas pelo projeto, use:

```powershell
.\config.ps1 update
```

Para reconstruir também as imagens Docker durante a configuração, acrescente `-BuildDocker`:

```powershell
.\config.ps1 install -BuildDocker
```

O `config.ps1` instala e compila os componentes, mas não inicia a aplicação. Para preparar os arquivos de ambiente, construir as imagens necessárias e iniciar a stack completa pelo Docker, execute:

```powershell
.\release.ps1 up
```

O `release.ps1 up` preserva os arquivos de ambiente já copiados. Se algum deles estiver ausente, cria-o a partir do respectivo exemplo; também gera chaves locais ausentes e inicia frontend, backend, banco de dados, API de IA e Mailpit. A `NVIDIA_API_KEY` em `api-ia/.env` habilita a geração de perguntas e avaliações. Sem essa chave, o restante da aplicação pode iniciar, mas os treinos que dependem de IA não funcionarão.

Quando os serviços estiverem prontos, abra <http://localhost:8080> no navegador. Para consultar o estado, acompanhar os logs, reiniciar ou parar os serviços, use:

```powershell
.\release.ps1 status
.\release.ps1 logs
.\release.ps1 restart
.\release.ps1 down
```

O comando `logs` acompanha a saída até você pressionar `Ctrl+C`. `down` remove os containers, mas preserva o volume de dados do banco.

Opções adicionais para iniciar a stack:

```powershell
.\release.ps1 up -NoBuild
.\release.ps1 up -RefreshImages
.\release.ps1 up -TimeoutSeconds 600
```

Use `-NoBuild` quando as imagens já estiverem construídas e você quiser apenas iniciar os containers. `-RefreshImages` atualiza as imagens-base e reconstrói as imagens do IVIS. `-TimeoutSeconds` altera o tempo máximo de espera pela prontidão (o padrão é 300 segundos). Consulte a ajuda completa com `.\release.ps1 help` ou `.\config.ps1 help`.

### Opção B — configurar manualmente

Para desenvolver os componentes fora do Docker, instale Node.js 22 ou superior, Python 3.12 ou 3.13 e JDK 25. Instale Git e Docker Desktop também se ainda não estiverem disponíveis. Na raiz do projeto, instale as dependências npm e Python e compile o backend:

```powershell
npm ci
npm --prefix frontend ci
npm --prefix cypress/e2e ci
py -3.12 -m venv api-ia/.venv
& .\api-ia\.venv\Scripts\python.exe -m pip install -r api-ia/requirements.txt
Push-Location backend
.\gradlew.bat bootJar
Pop-Location
npm --prefix frontend run build
```

Se tiver Python 3.13 em vez de 3.12, substitua `py -3.12` por `py -3.13`. O Gradle Wrapper deve ser executado com JDK 25. Configure `JAVA_HOME` para apontar à pasta do JDK quando essa versão não for a padrão do terminal.

Se você recebeu os arquivos da pasta `env/`, use a cópia indicada no início desta seção. Somente se não os recebeu, crie os arquivos de ambiente a partir dos exemplos, sem sobrescrever configurações existentes:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
if (-not (Test-Path api-ia/.env)) { Copy-Item api-ia/.env.example api-ia/.env }
```

Edite `api-ia/.env` e informe a `NVIDIA_API_KEY` para habilitar os treinos. Revise `.env` para ajustar portas e credenciais locais, se necessário. O `release.ps1 up` completa a configuração de chaves locais ausentes e sobe os serviços em Docker. Se preferir executar o frontend pelo servidor de desenvolvimento, mantenha o backend ativo e rode em outro terminal:

```powershell
npm --prefix frontend run dev
```

Abra <http://localhost:5173>. Consulte [Como rodar o IVIS](como%20rodar.md) para configurações adicionais de frontend e backend e solução de problemas.

### Linux e macOS

O `config.ps1` e o `release.ps1` são scripts PowerShell destinados ao Windows. No Linux e no macOS, use `release.sh` com Bash. Instale e inicie o Docker (Docker Desktop no macOS; Docker Desktop ou Docker Engine com o plugin Compose no Linux) e confirme que os comandos `docker`, `curl` e `openssl` estão disponíveis. Na raiz do repositório, inicie a aplicação com:

```bash
bash release.sh up
```

Se os arquivos recebidos já foram copiados, o script os preserva. Caso contrário, na primeira execução ele cria `.env` e `api-ia/.env` com base nos exemplos e gera as chaves locais necessárias. Nesse caso, preencha `NVIDIA_API_KEY` em `api-ia/.env` e execute `bash release.sh up` novamente para habilitar os treinos com IA. Abra <http://localhost:8080> quando a inicialização terminar.

Os comandos de manutenção no Linux e macOS são:

```bash
bash release.sh status
bash release.sh logs
bash release.sh restart
bash release.sh down
```

`logs` acompanha a saída até você pressionar `Ctrl+C`. `down` remove os containers e preserva o volume do banco de dados. Para ver os comandos disponíveis, use `bash release.sh help`.

Se for desenvolver os módulos fora dos containers, instale Node.js 22 ou superior, Python 3.12 ou 3.13 e JDK 25 pelos links oficiais acima. Em seguida, na raiz do repositório, instale as dependências e compile os módulos:

```bash
npm ci
npm --prefix frontend ci
npm --prefix cypress/e2e ci
python3.12 -m venv api-ia/.venv
api-ia/.venv/bin/python -m pip install -r api-ia/requirements.txt
(cd backend && ./gradlew bootJar)
npm --prefix frontend run build
```

Se estiver usando Python 3.13, troque `python3.12` por `python3.13`. O Gradle Wrapper pode baixar as dependências do backend; não instale Gradle globalmente. Para mais detalhes de desenvolvimento, consulte os guias do frontend, backend e API de IA.

## 4. Criar uma conta e entrar

1. Na página inicial, selecione **Começar agora** ou **Criar conta**.
2. Escolha **Sou aluno** ou **Sou instituição**.
3. Preencha os campos solicitados e informe um e-mail válido.
4. Crie uma senha com pelo menos 8 caracteres, contendo letra maiúscula, letra minúscula, número e caractere especial. A senha pode ter até 128 caracteres.
5. Confirme a senha e selecione **Criar conta**.
6. Após o cadastro, selecione **Continuar**. Para acessar áreas protegidas, a conta também precisa de um plano individual ou de um vínculo institucional ativo.

Para entrar em uma conta existente, selecione **Entrar**, informe e-mail e senha e pressione **Entrar**. O sistema direciona cada tipo de conta para sua área correspondente.

No ambiente local, o e-mail de boas-vindas é enviado ao Mailpit em <http://localhost:8025>. A mesma caixa recebe a confirmação de contratação simulada e as mensagens de recuperação de senha. Esses e-mails de teste não chegam à caixa externa do endereço informado.

### Recuperar a senha

Na tela de login, selecione **Esqueci minha senha**, informe o e-mail da conta e solicite as instruções. Abra o link recebido e cadastre uma nova senha seguindo os requisitos exibidos na tela. Na execução local, as mensagens de e-mail ficam no Mailpit em <http://localhost:8025>; esse ambiente não envia mensagens para uma caixa externa.

## 5. Planos e acesso

Visitantes podem consultar as modalidades na página **Planos**. Para contratar, escolha uma oferta, crie uma conta ou entre e prossiga para o checkout.

**O checkout deste projeto é uma simulação acadêmica. Não há cobrança ou movimentação real de dinheiro.** No ambiente de demonstração, os dados de teste apresentados na própria tela permitem simular aprovação ou recusa. Após uma aprovação, o plano é ativado; em caso de recusa, use **Tentar novamente**.

Depois de entrar, abra **Meu plano** no menu para consultar a modalidade, validade e benefícios do acesso. A conta da instituição também apresenta a quantidade de vagas incluídas no contrato. A capacidade disponível nas ofertas institucionais atuais é definida pela própria oferta.

## 6. Usar a área do aluno

O menu lateral reúne **Meus treinos**, **Vagas de interesse**, **Minhas habilidades**, **Histórico**, **Meu plano** e os dados da conta.

### Cadastrar habilidades

1. Abra **Minhas habilidades** e escolha a opção para adicionar uma habilidade.
2. Informe o nome, a categoria e o nível de domínio.
3. Salve. Use as opções de edição ou exclusão para manter o perfil atualizado.

Os níveis disponíveis vão de iniciante a especialista. As habilidades cadastradas podem ser selecionadas como contexto de um treino.

### Cadastrar uma vaga de interesse

1. Abra **Vagas de interesse** e escolha a opção para adicionar uma vaga.
2. Informe o cargo, a empresa, o nível de senioridade e as habilidades ou requisitos separados por vírgula.
3. Salve a vaga. Você pode editá-la ou excluí-la pela lista.
4. Para começar um treino com essa vaga, escolha **Treinar para esta vaga**.

### Realizar e retomar um treino

1. Abra **Meus treinos** ou selecione **Iniciar treinamento** no menu.
2. Escolha o contexto: habilidades do perfil ou uma vaga cadastrada. Selecione ao menos uma habilidade ou uma vaga.
3. Pressione **Iniciar entrevista** e responda às perguntas, enviando cada resposta antes de avançar.
4. O treino contém três perguntas. Depois de responder às três, selecione **Finalizar treino** para solicitar a avaliação.
5. Consulte o feedback apresentado e acesse **Histórico** para abrir resultados de treinos concluídos.

Se a página for recarregada durante uma entrevista, o IVIS permite retomar o treino em andamento. Se houver uma falha temporária na geração de uma pergunta ou avaliação, a tela informa o problema; tente novamente. **Cancelar treino** encerra a sessão sem incluí-la entre os treinos concluídos.

A nota e os comentários dependem da avaliação disponível para aquela sessão. Caso a tela informe que não há avaliação qualitativa, ative/configure o provedor de IA na execução local e faça um novo treino.

## 7. Usar a área da instituição

A área **Alunos e vagas** exibe os alunos vinculados, o total de vagas do plano e quantas ainda estão disponíveis.

Para adicionar um aluno, use uma das opções:

- **Vincular aluno:** informe o e-mail de uma conta IVIS existente do tipo aluno e selecione **Vincular aluno**.
- **Cadastrar novo aluno:** preencha os dados cadastrais, crie uma senha conforme os requisitos apresentados e selecione **Cadastrar e vincular**.

O vínculo depende de haver uma vaga disponível. A lista mostra os alunos vinculados e a data do vínculo. Selecione **Remover** ao lado de um aluno para encerrar o vínculo e liberar a vaga. O aluno removido deixa de ter o acesso fornecido por aquela instituição, a menos que possua outro acesso vigente.

## 8. Gerenciar a conta e sair

No menu, abra os dados identificados como **Aluno** ou **Instituição** para consultar ou atualizar os campos disponíveis. Para trocar a senha, acesse a opção **Alterar senha**, informe a senha atual, cadastre e confirme a nova senha e salve.

Para encerrar a sessão, use o botão de saída no menu. Em computadores e dispositivos móveis, o menu pode aparecer como uma barra lateral ou ser aberto pelo botão de navegação.

## 9. Executar todos os testes no computador

Execute os comandos **na raiz do repositório**, salvo quando o passo indicar outra pasta. Os testes de frontend usam Vitest; os de backend usam JUnit e Testcontainers; os da API de IA usam pytest; e os testes de interface usam Cypress. A suíte Cypress completa inclui as jornadas reais do aluno e da instituição, verifica e-mails no Mailpit e solicita perguntas e avaliações ao provedor de IA.

### 9.1. Preparar o ambiente

1. Instale Git, Docker, Node.js 22 ou superior, Python 3.12 ou 3.13 e JDK 25 pelos links da seção 2. Inicie o Docker.
2. Copie os dois arquivos recebidos de `env/` para `.env` e `api-ia/.env`, seguindo a seção 3. Os treinos reais exigem `NVIDIA_API_KEY` em `api-ia/.env`.
3. Instale as dependências locais e inicie a aplicação. Aguarde até que o frontend esteja acessível em <http://localhost:8080> e o Mailpit em <http://localhost:8025>. Mantenha os serviços em execução durante os testes de backend com Testcontainers, da limpeza do banco e do Cypress.

No **Windows (PowerShell)**, os comandos de preparação são:

```powershell
.\config.ps1 install
.\release.ps1 up
```

No **Linux ou macOS (Bash)**, instale as dependências e inicie a stack com:

```bash
npm ci
npm --prefix frontend ci
npm --prefix cypress/e2e ci
python3.12 -m venv api-ia/.venv
api-ia/.venv/bin/python -m pip install -r api-ia/requirements.txt
bash release.sh up
```

Se estiver usando Python 3.13, substitua `python3.12` por `python3.13`. O `config.ps1 install` já instala as dependências locais no Windows. No Linux/macOS, o Gradle Wrapper baixa as dependências Java quando o teste do backend for executado.

### 9.2. Rodar as suítes no Windows

Execute cada bloco abaixo no **PowerShell**, na ordem. Aguarde a conclusão de um bloco antes de iniciar o próximo.

**Frontend — testes unitários, tipos, estilo e compilação:**

```powershell
npm --prefix frontend run test:unit
npm --prefix frontend run typecheck
npm --prefix frontend run lint
npm --prefix frontend run build
```

**Backend — testes unitários e de integração com PostgreSQL via Testcontainers:**

```powershell
Push-Location backend
.\gradlew.bat test --rerun-tasks
Pop-Location
```

**API de IA — testes com o transporte do provedor simulado:**

```powershell
Push-Location api-ia
& .\.venv\Scripts\python.exe -m pytest tests -q
Pop-Location
```

**Limpeza seletiva dos dados usados pelo Cypress — teste de integração com o banco da aplicação:**

```powershell
node --test cypress/tasks/limpar-banco.integracao.test.mjs
```

**Interface — todos os casos de uso e jornadas reais:**

```powershell
npm run testar:e2e
```

### 9.3. Rodar as suítes no Linux ou macOS

Execute cada bloco abaixo no **Bash**, na ordem, a partir da raiz do repositório. Aguarde a conclusão de um bloco antes de iniciar o próximo.

```bash
npm --prefix frontend run test:unit
npm --prefix frontend run typecheck
npm --prefix frontend run lint
npm --prefix frontend run build
```

```bash
(cd backend && ./gradlew test --rerun-tasks)
```

```bash
(cd api-ia && .venv/bin/python -m pytest tests -q)
```

```bash
node --test cypress/tasks/limpar-banco.integracao.test.mjs
npm run testar:e2e
```

### 9.4. Executar novamente um teste Cypress específico

Se quiser investigar somente uma jornada, execute um dos exemplos abaixo com a stack ativa. Os mesmos comandos funcionam no PowerShell e no Bash:

```bash
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/perfil_aluno.cy.js" --config retries=0
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/perfil_instituicao.cy.js" --config retries=0
npm --prefix cypress/e2e run testar:e2e -- --spec "caminho_feliz/emails_mailpit.cy.js" --config retries=0
```

Para executar um caso de uso isolado, substitua o valor de `--spec` pelo arquivo correspondente em `cypress/e2e/cdu/`, por exemplo `"cdu/CDU-10-contratar-plano-individual.cy.js"`. Execute **uma suíte Cypress por vez**: os hooks das jornadas reais e do CDU-15 limpam as contas reservadas para os testes antes e depois da execução. A limpeza preserva outras contas e o catálogo de planos.

O Cypress usa a aplicação em `localhost:8080` e o Mailpit em `localhost:8025`; não é preciso iniciar o Vite em outra porta. Os vídeos são gravados em `cypress/downloads` e as capturas de falhas em `cypress/screenshots`. Se a jornada de treino falhar por indisponibilidade do provedor, confira a configuração da API de IA e os logs com `.\release.ps1 logs` no Windows ou `bash release.sh logs` no Linux/macOS.

## 10. Dúvidas e problemas comuns

| Situação | O que fazer |
| --- | --- |
| A área de treinamento pede um plano | Consulte **Planos** e contrate uma modalidade ou peça à instituição responsável para verificar seu vínculo. |
| Não há habilidades ou vagas para escolher | Cadastre ao menos uma habilidade ou vaga antes de iniciar o treino. |
| O treino não inicia ou a IA está indisponível | Na execução local, confira se `NVIDIA_API_KEY` está configurada em `api-ia/.env` e se os serviços estão em execução. Tente novamente mais tarde se o provedor estiver temporariamente indisponível. |
| O e-mail de recuperação não aparece | Na execução local, confira o Mailpit em <http://localhost:8025>. Na AWS Academy, o acesso ao Mailpit é restrito à equipe responsável pela demonstração. |
| Não é possível adicionar aluno à instituição | Confira se há vaga disponível, se o e-mail pertence a uma conta de aluno e se ela ainda não possui vínculo institucional ativo. |
| A tela não carrega informações | Atualize a página. Se o problema persistir na cópia local, execute `.\release.ps1 status` para consultar os serviços ou `.\release.ps1 logs` para consultar os logs. |
