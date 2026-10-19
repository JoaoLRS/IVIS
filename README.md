# IVIS — Intelligent Virtual Interview System

<p align="center">
  <strong>Intelligent Virtual Interview System</strong><br>
  Plataforma inteligente para treinamento de entrevistas técnicas com Inteligência Artificial.
</p>

---

## 📌 Sobre o Projeto

O **IVIS (Intelligent Virtual Interview System)** é uma aplicação web baseada em **Inteligência Artificial**, desenvolvida para auxiliar estudantes e profissionais em preparação para entrevistas de emprego na área de tecnologia.

A plataforma permite que o usuário realize **simulações de entrevistas técnicas**, personalizadas de acordo com suas habilidades ou com os requisitos de uma vaga de interesse. Durante o treinamento, a IA conduz a entrevista, processa as respostas e, ao final, fornece uma avaliação de desempenho com pontos de destaque e oportunidades de melhoria.

Além do módulo de treinamento, o IVIS contempla o **gerenciamento de contas, planos de acesso e contratação**, incluindo planos individuais para alunos e planos institucionais por meio de consórcios contratados por instituições de ensino.

O projeto está sendo desenvolvido e aprimorado no contexto da disciplina de **Engenharia de Software**, aplicando conceitos de levantamento e especificação de requisitos, casos de uso, histórias de usuário, arquitetura, desenvolvimento, testes, qualidade e evolução de software.

---

## 🎯 Objetivo

O principal objetivo do IVIS é oferecer um ambiente acessível e personalizado para que alunos e profissionais possam **praticar entrevistas técnicas de maneira recorrente**, utilizando Inteligência Artificial para conduzir os treinamentos e fornecer feedback sobre o desempenho.

O sistema busca reduzir a dependência de entrevistas simuladas realizadas exclusivamente por mentores humanos, permitindo que o usuário pratique de maneira mais frequente e direcionada.

---

## 👥 Público-Alvo

O IVIS é direcionado principalmente para:

* 🎓 **Estudantes** que desejam se preparar para processos seletivos;
* 💻 **Profissionais em transição para a área de tecnologia**;
* 🚀 **Profissionais de tecnologia** que desejam aprimorar suas habilidades em entrevistas;
* 🏫 **Instituições de Ensino** que desejam oferecer treinamento de entrevistas aos seus alunos.

---

## 👤 Atores do Sistema

### Aluno

Usuário que utiliza a plataforma para realizar treinamentos de entrevistas.

O aluno pode possuir dois perfis de acesso:

* **AlunoPagante** — possui um plano individual contratado diretamente;
* **AlunoConsórcio** — possui acesso por meio de um plano institucional contratado por uma Instituição de Ensino.

### Instituição de Ensino

Responsável pela contratação de planos institucionais/consórcio e pelo gerenciamento dos alunos vinculados às vagas contratadas.

### Sistema de IA

Serviço responsável pelo processamento das respostas e pela geração das perguntas e avaliações da entrevista.

### Sistema de Pagamento

Gateway externo responsável pelo processamento dos pagamentos relacionados à contratação dos planos.

---

## 🚀 Principais Funcionalidades

### 🔐 Acesso e Perfil

* Cadastro de Aluno e Instituição de Ensino;
* Login e autenticação;
* Gerenciamento de dados pessoais ou institucionais;
* Alteração de senha;
* Recuperação de acesso por e-mail;
* Gerenciamento de foto ou logo;
* Gerenciamento do perfil de habilidades técnicas;
* Visualização do plano vigente.

### 🤖 Treinamento com IA

* Cadastro, edição e exclusão de vagas de interesse;
* Personalização do treinamento de acordo com habilidades ou vaga;
* Simulação interativa de entrevistas;
* Processamento das respostas por IA;
* Geração automática de feedback;
* Histórico de treinamentos realizados;
* Consulta dos resultados obtidos.

### 💳 Planos e Contratação

* Visualização dos planos disponíveis;
* Apresentação de preços e benefícios;
* Contratação de plano individual;
* Contratação de plano institucional/consórcio;
* Processamento de pagamentos por gateway externo;
* Visualização de informações do plano contratado.

### 🏫 Gestão Institucional

* Gerenciamento de alunos vinculados;
* Cadastro/vinculação de alunos;
* Remoção de alunos;
* Controle de vagas contratadas;
* Visualização de vagas utilizadas e disponíveis;
* Diferenciação entre alunos de consórcio e alunos pagantes.

### 🔎 Auditoria e Segurança

* Registro de ações sensíveis;
* Identificação do usuário responsável por cada ação;
* Registro de data e horário das operações;
* Proteção dos registros de auditoria;
* Isolamento dos dados entre diferentes instituições.

---

## 📋 Requisitos do Sistema

O quadro de requisitos registra **20 Requisitos Funcionais (RF)** — 14 na lista de requisitos e 6 em andamento — e **4 Requisitos Não Funcionais (RNF)**.

### Requisitos Funcionais — lista de requisitos (14)

| Código | Requisito | Descrição |
| ------ | --------- | --------- |
| RF01 | Cadastro e login de usuários | Permitir que Alunos e Instituições de Ensino criem conta e façam login com acesso correspondente ao perfil. |
| RF02 | Gerenciamento da conta | Permitir consultar e alterar os dados da própria conta, respeitando o tipo de usuário. |
| RF04 | Recuperação de senha | Permitir recuperar o acesso usando o e-mail cadastrado. Na apresentação, as mensagens são capturadas pelo Mailpit. |
| RF05 | Visualização do plano vigente | Exibir tipo, validade e benefícios do plano associado à conta. |
| RF11 | Exibição dos planos disponíveis na Home | Apresentar planos individuais e institucionais, preços e benefícios. |
| RF12 | Visualização dos planos sem autenticação | Permitir que visitantes consultem a Home e os planos antes de criar conta ou fazer login. |
| RF13 | Contratação de plano individual | Permitir que o Aluno Pagante contrate um plano individual pela plataforma. |
| RF14 | Contratação de plano institucional | Permitir que a Instituição de Ensino contrate um plano e defina a quantidade de vagas. |
| RF15 | Processamento de pagamentos via gateway | Processar a contratação. Para a apresentação acadêmica, usar o simulador de pagamento controlado pelo backend, sem cobrança real; ver [ADR-0008](etc/plano/adr/ADR-0008-pagamento-simulado-para-demonstracao.md). |
| RF16 | Gestão de alunos vinculados à instituição | Permitir visualizar, vincular e remover alunos dentro do limite do plano institucional. |
| RF17 | Controle de acesso por tipo de aluno | Aplicar acesso conforme o plano individual ou o vínculo institucional. |
| RF18 | Controle de vagas do plano institucional | Exibir vagas contratadas, utilizadas e disponíveis. |
| RF19 | Restrição de contratação para AlunoConsórcio | Impedir contratação individual enquanto o aluno possuir vínculo ativo com uma instituição. |
| RF20 | Registro de logs de auditoria | Registrar ações sensíveis com responsável e data/hora, protegendo os registros contra alteração indevida. |

### Requisitos Funcionais — em andamento (6)

| Código | Requisito | Descrição |
| ------ | --------- | --------- |
| RF03 | Perfil de habilidades técnicas | Permitir que o Aluno cadastre, edite e remova habilidades e níveis de domínio. |
| RF06 | Gerenciamento de vagas de interesse | Permitir que o Aluno cadastre, edite e remova vagas para contextualizar os treinos. |
| RF07 | Simulação de entrevista com IA | Conduzir entrevista textual usando o perfil ou uma vaga como contexto e processar perguntas/respostas pela API de IA. |
| RF08 | Feedback de desempenho do treinamento | Apresentar a avaliação e os pontos fortes e de melhoria ao fim do treino. |
| RF09 | Armazenamento do histórico de treinos | Persistir cada treino e associá-lo ao Aluno correspondente. |
| RF10 | Consulta ao histórico de treinos | Exibir data, contexto e resultado dos treinos do Aluno. |

### Requisitos Não Funcionais

| Código | Requisito | Descrição |
| ------ | --------- | --------- |
| RNF01 | Aplicação Web | O sistema deve ser acessível por navegador web. |
| RNF03 | Tempo de resposta da IA | A IA deve responder sem comprometer a fluidez do treino; o limite mensurável ainda precisa ser definido. |
| RNF07 | Isolamento de dados entre instituições | Cada Instituição de Ensino só pode visualizar e gerenciar os próprios alunos vinculados. |
| RNF09 | Proteção e retenção dos logs de auditoria | Proteger os registros contra alteração e mantê-los pelo período definido com a equipe responsável. |

> **Total:** 24 requisitos — 20 funcionais (14 na lista e 6 em andamento) e 4 não funcionais.

---

## 🧩 Casos de Uso

O sistema está estruturado em casos de uso que representam as principais interações entre os atores e o IVIS.

| ID   | Caso de Uso                       |
| ---- | --------------------------------- |
| UC01 | Criar Conta                       |
| UC02 | Fazer Login                       |
| UC03 | Gerenciar Perfil e Habilidades    |
| UC04 | Gerenciar Vaga de Interesse       |
| UC05 | Iniciar Treinamento               |
| UC06 | Responder Perguntas da Entrevista |
| UC07 | Receber Feedback da Entrevista    |
| UC08 | Consultar Histórico de Treinos    |
| UC09 | Visualizar Home com Planos        |
| UC10 | Contratar Plano                   |
| UC11 | Gerenciar Alunos Vinculados       |
| UC12 | Efetuar Pagamento                 |
| UC13 | Gerenciar Dados da Conta          |

---

## 📖 Histórias de Usuário

O desenvolvimento do IVIS também utiliza **Histórias de Usuário** para representar as necessidades dos diferentes perfis de usuários.

As histórias contemplam:

* Treinamento de entrevistas;
* Gerenciamento de habilidades;
* Treinamento direcionado a vagas;
* Feedback de desempenho;
* Histórico de treinamentos;
* Cadastro e autenticação;
* Gerenciamento de perfil;
* Contratação de planos;
* Gestão de planos institucionais;
* Gerenciamento de alunos vinculados.

Atualmente, o projeto possui **20 Histórias de Usuário**, priorizadas utilizando a técnica **MoSCoW**.

---

## 🖥️ Principais Telas

A prototipação inicial contempla as seguintes interfaces:

* **Cadastro/Login**
* **Home**
* **Contratação e Pagamento**
* **Perfil / Minha Conta**
* **Perfil de Habilidades**
* **Gerenciamento de Vagas**
* **Treinamento**
* **Feedback**
* **Histórico**
* **Gerenciamento de Alunos Vinculados**

---

## 🔒 Segurança e Privacidade

Por trabalhar com dados pessoais de alunos e instituições, o IVIS considera requisitos relacionados à **segurança da informação e proteção de dados**.

Entre as medidas previstas estão:

* Conformidade com a **LGPD**;
* Armazenamento seguro de senhas por meio de hash;
* Controle de sessão;
* Isolamento dos dados institucionais;
* Não armazenamento direto de dados sensíveis de pagamento;
* Utilização de gateway externo para pagamentos;
* Registro de ações sensíveis para auditoria.

---

## 📦 Escopo do MVP

A primeira versão do IVIS contempla:

* Cadastro e autenticação;
* Gerenciamento de perfil;
* Gerenciamento de habilidades;
* Gerenciamento de vagas;
* Entrevista simulada por texto utilizando IA;
* Feedback automático;
* Histórico de treinamentos;
* Planos individuais e institucionais;
* Contratação e pagamento;
* Gerenciamento de alunos vinculados a instituições.

### Fora do escopo inicial

Algumas funcionalidades permanecem previstas para futuras versões:

* Entrevistas por voz ou vídeo;
* Integração com plataformas externas de vagas;
* Gamificação;
* Ranking entre alunos;
* Certificações;
* Suporte a múltiplos idiomas;
* Recursos avançados de faturamento recorrente;
* Painel administrativo completo.

---

## 🛠️ Engenharia de Software

O desenvolvimento do IVIS utiliza práticas e artefatos de **Engenharia de Software** para organizar e acompanhar a evolução do sistema.

Entre os principais artefatos estão:

* Documento de Visão e Escopo;
* Stakeholders;
* Atores do sistema;
* Diagrama de Casos de Uso;
* Casos de Uso detalhados;
* Histórias de Usuário;
* Requisitos Funcionais;
* Requisitos Não Funcionais;
* Protótipos de interface;
* Critérios de aceitação;
* Planejamento e priorização de funcionalidades.

O projeto será desenvolvido de forma **incremental**, permitindo que os requisitos sejam validados, implementados, testados e aprimorados ao longo dos Sprints.

---

## 📈 Evolução do Projeto

O IVIS encontra-se em **desenvolvimento ativo**. A evolução do sistema considera tanto a implementação das funcionalidades quanto aspectos relacionados à qualidade do software, segurança, usabilidade, desempenho e manutenção.

O projeto também poderá ser utilizado como objeto de **avaliação de desempenho**, permitindo analisar o comportamento da aplicação diante de diferentes níveis de carga e utilização.

---

## 📚 Documentação

A documentação do projeto está organizada no diretório:

```text
etc/
└── plano/
    └── IVIS_documento.md
```

Esse documento contém a especificação inicial de visão, escopo, stakeholders, atores, casos de uso, histórias de usuário, requisitos e prototipação.

A sequência de implementação dos requisitos em andamento (RF03 e RF06 a RF10) está em `etc/plano/Ordem_de_Entrega_MVP.md`.

---

## 📌 Status do Projeto

🚧 **Em desenvolvimento**

O IVIS está sendo desenvolvido no contexto acadêmico da disciplina de **Engenharia de Software**, com evolução progressiva de seus requisitos, arquitetura, funcionalidades e mecanismos de qualidade.

---

## 👨‍💻 Projeto

**IVIS — Intelligent Virtual Interview System**

Projeto acadêmico desenvolvido para aplicação prática de conceitos de **Engenharia de Software, Inteligência Artificial e desenvolvimento de aplicações web**.
