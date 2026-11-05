describe("treino integrado ao backend de IA", () => {
  it("mantém a resposta quando a próxima pergunta falha e permite tentar de novo", () => {
    cy.intercept("GET", "**/api/v1/sessoes/csrf", {
      statusCode: 200,
      body: { token: "csrf-de-teste" },
    });
    cy.intercept("POST", "**/api/v1/sessoes/renovacao", {
      statusCode: 200,
      body: {
        accessToken: "token-de-teste",
        tipoToken: "Bearer",
        expiraEmSegundos: 3600,
        conta: {
          id: "conta-de-teste",
          tipoConta: "ALUNO",
          email: "aluno@exemplo.com",
          nome: "Aluno de teste",
        },
      },
    });
    cy.intercept("GET", "**/api/v1/conta/plano", {
      statusCode: 200,
      body: {
        situacao: "COM_PLANO_VIGENTE",
        planos: [{ situacao: "ATIVO", origemAcesso: "INDIVIDUAL" }],
      },
    });
    cy.intercept("GET", "**/api/v1/habilidades", {
      statusCode: 200,
      body: [
        {
          id: "habilidade-de-teste",
          nome: "JavaScript",
          categoria: null,
          nivel: 3,
          criadoEm: "2025-01-15T12:00:00Z",
          atualizadoEm: "2025-01-15T12:00:00Z",
        },
      ],
    });
    cy.intercept("GET", "**/api/v1/vagas", {
      statusCode: 200,
      body: [],
    });
    cy.intercept("GET", "**/api/v1/treinos", {
      statusCode: 200,
      body: [],
    });

    let chamadasPergunta = 0;
    cy.intercept("POST", "**/api/v1/ia/perguntas/gerar", (requisicao) => {
      chamadasPergunta += 1;
      if (chamadasPergunta === 2) {
        requisicao.reply({
          statusCode: 503,
          body: {
            codigo: "provedor_indisponivel",
            mensagem: "Serviço indisponível.",
          },
        });
        return;
      }

      requisicao.reply({
        statusCode: 200,
        body: {
          pergunta: { texto: `Pergunta ${chamadasPergunta}` },
          versao_modelo: "stub-1",
          versao_prompt: "perguntas-1",
        },
      });
    }).as("gerarPergunta");

    let chamadasSalvamento = 0;
    cy.intercept("POST", "**/api/v1/treinos/concluidos", (requisicao) => {
      chamadasSalvamento += 1;
      if (chamadasSalvamento === 1) {
        requisicao.reply({
          statusCode: 504,
          body: {
            codigo: "tempo_esgotado",
            mensagem: "A solicitação demorou mais que o limite.",
          },
        });
        return;
      }

      requisicao.reply({
        statusCode: 200,
        body: {
          id: "treino-de-teste",
          titulo: "Entrevista por habilidades",
          contexto: "Habilidades: JavaScript",
          nota: 80,
          data: "2025-01-15T12:00:00Z",
          pontosFortes: ["Respostas objetivas."],
          pontosMelhoria: ["Inclua mais exemplos."],
          avaliacoesPorPergunta: [1, 2, 3].map((numero) => ({
            numero,
            pergunta: `Pergunta ${numero}`,
            criterios: [
              {
                criterio: "CORRECAO_TECNICA",
                nota: 4,
                justificativa: "Resposta objetiva, mas precisa de mais detalhe.",
                trecho_resposta: "Resposta",
              },
              {
                criterio: "COMPLETUDE",
                nota: 4,
                justificativa: "Inclua mais exemplos.",
                trecho_resposta: "Resposta",
              },
              {
                criterio: "CLAREZA",
                nota: 4,
                justificativa: "A ideia principal está compreensível.",
                trecho_resposta: "Resposta",
              },
            ],
            nivel_confianca: "BAIXA",
            limitacao: "As respostas do teste são curtas.",
          })),
          avaliacaoQualitativa: true,
          nivelConfianca: "BAIXA",
          limitacoes: "As respostas do teste são curtas.",
        },
      });
    }).as("salvarTreino");

    cy.visit("/treinos");
    cy.contains("button", "Iniciar entrevista").click();
    cy.wait("@gerarPergunta");
    cy.contains("p", "Pergunta 1").should("be.visible");

    cy.get('textarea[name="resposta"]').type("Resposta que deve sobreviver");
    cy.contains("button", "Enviar resposta").click();
    cy.wait("@gerarPergunta");
    cy.contains("Sua resposta foi mantida; tente novamente.").should(
      "be.visible",
    );
    cy.get('textarea[name="resposta"]').should(
      "have.value",
      "Resposta que deve sobreviver",
    );

    cy.contains("button", "Tentar novamente").click();
    cy.wait("@gerarPergunta").then(({ request }) => {
      expect(request.body.interacoes[0].resposta).to.equal(
        "Resposta que deve sobreviver",
      );
    });
    cy.contains("p", "Pergunta 3").should("be.visible");

    cy.get('textarea[name="resposta"]').type("Resposta da segunda pergunta");
    cy.contains("button", "Enviar resposta").click();
    cy.wait("@gerarPergunta");
    cy.get('textarea[name="resposta"]').type("Resposta da terceira pergunta");
    cy.contains("button", "Enviar resposta").click();
    cy.contains("Você respondeu às três perguntas.").should("be.visible");
    cy.contains("button", "Finalizar e ver feedback").click();
    cy.wait("@salvarTreino");
    cy.contains("A avaliação está temporariamente indisponível.").should(
      "be.visible",
    );
    cy.contains("button", "Finalizar e ver feedback").click();
    cy.wait("@salvarTreino").then(({ request }) => {
      expect(request.body.interacoes).to.have.length(3);
      expect(request.body.interacoes[2].resposta).to.equal(
        "Resposta da terceira pergunta",
      );
    });
    cy.contains("strong", "80").should("be.visible");
    cy.contains("Respostas objetivas.").should("be.visible");
    cy.contains("RUBRICA POR PERGUNTA").should("be.visible");
    cy.contains("Correção técnica").should("be.visible");
    cy.contains("Os trechos mostram o que a IA considerou").should("be.visible");
    cy.contains("button", "Ver histórico").click();
    cy.contains("80/100").should("be.visible");
    cy.get(".history-list-row").click();
    cy.contains("strong", "80").should("be.visible");
  });
});
