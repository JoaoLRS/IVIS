# Implementação do escopo de Nickolas

## Base verificada

Antes das alterações, o projeto foi compilado e iniciado com Docker Desktop.
O PostgreSQL recebeu V1–V3 pelo Flyway, o backend respondeu `PRONTO` e o frontend
respondeu HTTP 200. Cadastro e login de uma conta fictícia e geração de pergunta
pelo backend foram exercitados; o provedor retornou `stub-1`. A interface inicial
foi aberta e ainda apresenta dados demonstrativos.

Foram revisados os Markdown do repositório e os planos de equipe fornecidos.
O código atual tem identidade, sessão, habilidades, vagas, vínculos institucionais
e cliente Java de IA; referências antigas a “nenhum backend” estão desatualizadas.
Esses módulos foram reaproveitados, sem assumir as entregas de outros integrantes.

## Entrega atual

- Catálogo público persistido, detalhes e benefícios ordenados (parte backend de RF11/RF12).
- Consulta autenticada de contratos vigentes, próprios ou por vínculo (fatia de RF05).
- Organização em `br.ivis.planos`, nomes em português, Spring Boot e Spring Data JPA.
- Contratos HTTP documentados e incluídos no OpenAPI gerado.
- Testes de integração HTTP/PostgreSQL de catálogo, isolamento, preço histórico,
  conta inativa, contrato suspenso e vínculo encerrado.

Nenhuma migration nova é necessária para essa fatia: ela lê as tabelas de V3.
As entidades de leitura são imutáveis e os repositórios não expõem gravação.

## Definições encontradas nos documentos

- `etc/plano/IVIS_documento.md`, seção 1: proposta de treinos ilimitados.
- Mesmo documento, US16: preço institucional por aluno e quantidade de vagas.
- `etc/cdu/UC12_contratar-plano-institucional.md`: preços, mínimo/máximo de vagas
  e vigência dependem de definição comercial.
- `etc/plano/BD/Modelo_Relacional_IVIS.md`: ciclo de vigência, renovação e regras
  de preço/capacidade continuam pendentes.
- `etc/cdu/UC16_registrar-auditoria.md` e RNF09: prazo de retenção e perfis de
  consulta ainda não definidos. Não habilitar descarte automático nem inventar prazo.
- ADR-0008: somente pagamento simulado no backend, sem guardar PAN/CVV.

Não interpretar o exemplo monetário do glossário como preço aprovado. Não
interpretar “ilimitado” como dispensa de autenticação ou de plano válido.

## Trabalho ainda necessário no escopo

1. Contratação e pagamento simulado idempotentes, estados transacionais e auditoria.
2. Representação explícita de validade e condições contratadas após definição
   comercial; a data de encerramento existente não é data prevista de expiração.
3. Orquestração persistente de treinos, perguntas, respostas, avaliação e histórico,
   com autorização por titular e recuperação de falhas da IA.
4. Auditoria imutável e política parametrizada de retenção; leitura/exportação
   dependem de perfis autorizados definidos com identidade.
5. Revisão dos contratos com a equipe e integração das telas por Alisson.

Esta entrega não declara RF05 completo nem implementa RF07–RF10, RF13–RF15,
RF20 ou RNF09 integralmente.

## Execução local nesta máquina

Docker instalado em `C:\Users\nicko\AppData\Local\Programs\DockerDesktop`.
Um terminal aberto antes da instalação pode não ter `docker` no `PATH`.
O processo do Docker precisa estar aberto e o mecanismo Linux pronto.

A porta 8000 já estava ocupada; o ajuste local `.env.compose-local.yaml`
(ignorado pelo Git) remove apenas a publicação da IA no host. O backend continua
acessando `http://ivis-api-ia:8000` pela rede interna. Com esse arquivo presente:

```powershell
$env:Path = 'C:\Users\nicko\AppData\Local\Programs\DockerDesktop\resources\bin;' + $env:Path
docker compose -f docker-compose.yml -f .env.compose-local.yaml up --build -d
```

Frontend: `http://localhost:8080`. Prontidão do backend e banco:
`http://localhost:8081/api/v1/pronto`. Nesse ajuste, não usar `release.ps1 up`,
pois ele usa o Compose padrão e verifica a IA na porta 8000 do host.
As chaves foram geradas em `.env` local e não devem ser versionadas.

## Validação da entrega

`gradle test bootJar --no-daemon`, executado com Java 25/Gradle 9.1.0 em
container e PostgreSQL 17 via Testcontainers: **40 testes, zero falhas/erros**.
Inclui os testes existentes de identidade/IA e os três testes de integração
novos de consulta de planos. `git diff --check` também passou.

Para a atualização local foi reutilizado o JAR dessa execução bem-sucedida,
empacotado com a mesma etapa de runtime do Dockerfile do projeto. O arquivo
temporário `backend/.env.runtime.Dockerfile` é ignorado pelo Git; o Dockerfile
versionado continua compilando a aplicação a partir do código-fonte.
