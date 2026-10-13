# Frontend IVIS

Módulo web independente em Vue 3, Composition API, TypeScript, Vite, Vue Router, Pinia e Bootstrap 5. O `package.json` identifica o módulo Node.js para o IntelliJ IDEA.

## Escopo funcional

Este frontend implementa somente os requisitos das imagens fornecidas:

- **RF03:** cadastrar, editar e remover habilidades do perfil, usadas como contexto de novos treinos.
- **RF06:** cadastrar, visualizar, editar e excluir vagas de interesse; a interface impede excluir a vaga usada em um treino em andamento.
- **RF07:** simular uma entrevista técnica por texto com contexto de habilidades ou vaga e encerramento após as perguntas demonstrativas.
- **RF08:** apresentar um feedback demonstrativo com resultado, pontos fortes e pontos de melhoria ao concluir o treino.
- **RF10:** consultar o histórico local com data, contexto (vaga ou habilidades) e resultado.

Não há backend nesta entrega. Os dados são mantidos em memória e a entrevista/feedback são simulações locais, sem IA real. A privacidade entre alunos exige autenticação e autorização no backend; a interface demonstra apenas o histórico de um aluno local.

## Estrutura

- `src/componentes/`: componentes compartilhados da interface.
- `src/paginas/`: telas dos requisitos funcionais incluídos.
- `src/dados/`: modelos e dados demonstrativos.
- `src/estados/`: estado local com Pinia.
- `src/roteador/`: rotas Vue Router.
- `src/servicos/`: cliente Axios para futura integração com API.

## Executar

Na raiz, consulte [`../etc/como rodar.md`](../etc/como%20rodar.md). `node_modules/` não deve ser versionado; o `.gitignore` da raiz já o ignora.
