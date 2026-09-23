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

O projeto possui atualmente **20 Requisitos Funcionais (RF)** e **9 Requisitos Não Funcionais (RNF)**.

### Requisitos Funcionais

| Código | Categoria     | Descrição                                     |
| ------ | ------------- | --------------------------------------------- |
| RF01   | Acesso        | Cadastro e login de Aluno e Instituição       |
| RF02   | Perfil        | Gerenciamento dos dados da conta              |
| RF03   | Perfil        | Gerenciamento das habilidades técnicas        |
| RF04   | Segurança     | Recuperação de acesso por e-mail              |
| RF05   | Planos        | Visualização dos detalhes do plano vigente    |
| RF06   | Treinamento   | Gerenciamento de vagas de interesse           |
| RF07   | Treinamento   | Simulação de entrevista com IA                |
| RF08   | Treinamento   | Geração de feedback de desempenho             |
| RF09   | Treinamento   | Armazenamento do histórico de treinos         |
| RF10   | Treinamento   | Consulta ao histórico de treinos              |
| RF11   | Planos        | Exibição dos planos disponíveis               |
| RF12   | Planos        | Acesso público à Home de planos               |
| RF13   | Contratação   | Contratação de plano individual               |
| RF14   | Contratação   | Contratação de plano institucional            |
| RF15   | Pagamento     | Processamento via gateway externo             |
| RF16   | Institucional | Gerenciamento de alunos vinculados            |
| RF17   | Acesso        | Diferenciação dos tipos de aluno              |
| RF18   | Institucional | Controle de vagas utilizadas/disponíveis      |
| RF19   | Contratação   | Restrição do plano individual ao AlunoPagante |
| RF20   | Auditoria     | Registro de ações sensíveis                   |

### Requisitos Não Funcionais

| Código | Categoria       | Requisito                                                |
| ------ | --------------- | -------------------------------------------------------- |
| RNF01  | Acessibilidade  | Aplicação acessível via navegador web                    |
| RNF02  | Segurança       | Conformidade com a LGPD                                  |
| RNF03  | Desempenho      | Tempo de resposta adequado durante a entrevista com IA   |
| RNF04  | Usabilidade     | Interface responsiva para desktop e dispositivos móveis  |
| RNF05  | Disponibilidade | Disponibilidade adequada para uso educacional            |
| RNF06  | Segurança       | Não armazenamento direto de dados sensíveis de pagamento |
| RNF07  | Segurança       | Isolamento de dados entre instituições                   |
| RNF08  | Segurança       | Hash de senhas e encerramento automático de sessão       |
| RNF09  | Auditoria       | Proteção e retenção dos registros de auditoria           |

> **Total:** 29 requisitos — 20 funcionais e 9 não funcionais.

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

---

## 📌 Status do Projeto

🚧 **Em desenvolvimento**

O IVIS está sendo desenvolvido no contexto acadêmico da disciplina de **Engenharia de Software**, com evolução progressiva de seus requisitos, arquitetura, funcionalidades e mecanismos de qualidade.

---

## 👨‍💻 Projeto

**IVIS — Intelligent Virtual Interview System**

Projeto acadêmico desenvolvido para aplicação prática de conceitos de **Engenharia de Software, Inteligência Artificial e desenvolvimento de aplicações web**.
