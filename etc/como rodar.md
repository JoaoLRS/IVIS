# Como rodar o IVIS

Este repositório contém o módulo de frontend em `frontend/`. A interface usa dados de demonstração no navegador; autenticação, persistência, IA e pagamentos ainda dependem de serviços de backend.

## Rodar com Docker

Pré-requisito: Docker Desktop instalado e em execução.

Na raiz do repositório, execute:

```bash
docker compose up --build
```

Quando o container estiver ativo, acesse [http://localhost:8080](http://localhost:8080).

Para encerrar, use `Ctrl+C`. Para remover o container e a rede criados pelo Compose, execute:

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

Abra a pasta raiz `IVIS` no IntelliJ IDEA. O diretório `frontend/` contém um `package.json`, que identifica o módulo JavaScript/Node.js. Caso o IntelliJ solicite, configure o Node.js como runtime do projeto e instale as dependências com npm.

## Estrutura do módulo

- `frontend/src/`: telas e estilos da aplicação.
- `frontend/Dockerfile`: imagem de produção do frontend.
- `frontend/nginx.conf`: configuração do servidor web.
- `docker-compose.yml`: execução do frontend a partir da raiz do repositório na porta `8080`.
