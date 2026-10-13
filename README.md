# IVIS — Frontend

Módulo frontend independente do IVIS, desenvolvido com Vue 3, TypeScript, Composition API, Vite, Vue Router, Pinia e Bootstrap. O escopo desta entrega está limitado aos requisitos funcionais RF03, RF06, RF07, RF08 e RF10 descritos em `etc/` e nas imagens fornecidas.

O protótipo cobre edição do perfil de habilidades, gerenciamento de vagas de interesse, simulação textual de entrevista, visualização do feedback demonstrativo e consulta ao histórico local. Não contém backend, autenticação, persistência remota nem integração com serviço de IA. Por isso, o isolamento real do histórico por aluno depende da implementação futura de autenticação e autorização no backend.

## Iniciar com Docker

Na raiz do repositório:

```bash
docker compose up --build
```

Acesse `http://localhost:8080`. Para instruções de execução local, IntelliJ IDEA e encerramento do container, consulte [`etc/como rodar.md`](etc/como%20rodar.md).

## Módulo

O projeto IntelliJ reconhece `frontend/package.json` como módulo Node.js. Os arquivos da aplicação em `frontend/src/` usam nomes em português. O diretório `node_modules/` é ignorado pelo Git e não deve ser commitado.
