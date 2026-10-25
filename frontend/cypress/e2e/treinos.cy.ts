describe("treino integrado ao backend de IA", () => {
  it("mantém a resposta quando a próxima pergunta falha e permite tentar de novo", () => {
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

    let chamadasAvaliacao = 0;
    cy.intercept("POST", "**/api/v1/ia/avaliacoes/gerar", (requisicao) => {
      chamadasAvaliacao += 1;
      if (chamadasAvaliacao === 1) {
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
          nota: 81,
          pontos_fortes: ["Respostas objetivas."],
          pontos_melhoria: ["Inclua mais exemplos."],
          versao_modelo: "stub-1",
          versao_prompt: "avaliacao-1",
        },
      });
    }).as("gerarAvaliacao");

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
    cy.wait("@gerarAvaliacao");
    cy.contains("Sua resposta foi mantida; tente novamente.").should(
      "be.visible",
    );
    cy.get('textarea[name="resposta"]').should(
      "have.value",
      "Resposta da terceira pergunta",
    );

    cy.contains("button", "Tentar novamente").click();
    cy.wait("@gerarAvaliacao").then(({ request }) => {
      expect(request.body.interacoes).to.have.length(3);
      expect(request.body.interacoes[2].resposta).to.equal(
        "Resposta da terceira pergunta",
      );
    });

    cy.contains("button", "Finalizar e ver feedback").click();
    cy.contains("strong", "81").should("be.visible");
    cy.contains("Respostas objetivas.").should("be.visible");
  });
});
