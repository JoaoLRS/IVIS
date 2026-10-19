# Como rodar o IVIS

Este repositório contém o módulo frontend em `frontend/` (Vue 3, Composition API, TypeScript, Vite e Bootstrap) e o esqueleto do backend em `backend/` (Java 25, Spring Boot, Gradle, PostgreSQL e Flyway; veja `backend/LEIA-ME.md`). O frontend ainda não consome o backend. O protótipo cobre RF03 (perfil de habilidades), RF06 (vagas de interesse), RF07 (simulação textual), RF08 (feedback) e RF10 (histórico), conforme as imagens fornecidas.

Os dados ficam em memória no navegador. Perguntas, resultado e feedback são demonstrativos: não há conexão com uma IA real nem persistência no servidor. A restrição de acesso ao histórico por aluno precisa de autenticação e autorização em um backend futuro.

## Rodar com Docker

Pré-requisito: Docker Desktop instalado e em execução.

Na raiz do repositório, execute:

```bash
docker compose up --build
```

Isso sobe o banco, o backend e o frontend. Quando os containers estiverem ativos, acesse o frontend em [http://localhost:8080](http://localhost:8080) e confira o backend em [http://localhost:8081/api/v1/saude](http://localhost:8081/api/v1/saude).

Para encerrar, use `Ctrl+C`. Para remover os containers e a rede criados pelo Compose, execute (acrescente `-v` para apagar também os dados do banco):

```bash
docker compose down
```

## Rodar em desenvolvimento

Pré-requisitos: Node.js 22 ou superior e npm.

No terminal, entre na pasta do frontend e instale as dependências:

```bash
cd frontend
npm install
```

Inicie o servidor de desenvolvimento:

```bash
npm run dev
```

Abra o endereço exibido pelo Vite no terminal, normalmente [http://localhost:5173](http://localhost:5173).

## Abrir no IntelliJ IDEA

Abra a pasta raiz `IVIS` no IntelliJ IDEA. O diretório `frontend/` contém um `package.json`, que identifica o módulo JavaScript/Node.js. Caso o IntelliJ solicite, configure o Node.js como runtime do projeto e instale as dependências com npm. O código usa Vue 3, Composition API e TypeScript.

## Estrutura do módulo

- `frontend/src/`: páginas, componentes e serviços da aplicação em português.
- `frontend/Dockerfile`: imagem de produção do frontend.
- `frontend/nginx.conf`: configuração do servidor web.
- `backend/`: API Spring Boot (porta `8081`, base `/api/v1`).
- `docker-compose.yml`: execução de PostgreSQL, backend (`8081`) e frontend (`8080`) a partir da raiz do repositório.
