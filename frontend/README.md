# IVIS Frontend

Frontend web independente do IVIS, organizado como um módulo Node.js/Vite. O IntelliJ IDEA reconhece a pasta `frontend` como um projeto JavaScript/Node ao abrir o repositório com o plugin JavaScript habilitado, usando o `package.json` como manifesto do módulo.

## Executar localmente

```bash
cd frontend
npm install
npm run dev
```

## Executar com Docker

Na raiz do repositório:

```bash
docker compose up --build
```

A aplicação ficará disponível em `http://localhost:8080`. Também é possível executar o compose dentro desta pasta.

## Escopo desta entrega

O frontend é um protótipo navegável com dados de demonstração mantidos no navegador. Ele cobre visão geral, habilidades, vagas, entrevista textual simulada, feedback, histórico, plano, conta e gestão institucional. Conexão com autenticação, persistência, serviço de IA e gateway de pagamento depende dos respectivos serviços de backend, que não existem neste repositório.
