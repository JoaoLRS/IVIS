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
          nota: 80,
          pontos_fortes: ["Respostas objetivas."],
          pontos_melhoria: ["Inclua mais exemplos."],
          avaliacoes_por_pergunta: [1, 2, 3].map((numero) => ({
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
          avaliacao_qualitativa: true,
          nivel_confianca: "BAIXA",
          limitacoes: "As respostas do teste são curtas.",
          versao_modelo: "nvidia/fake-1",
          versao_prompt: "avaliacao-2",
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
    cy.contains("strong", "80").should("be.visible");
    cy.contains("Respostas objetivas.").should("be.visible");
    cy.contains("RUBRICA POR PERGUNTA").should("be.visible");
    cy.contains("Correção técnica").should("be.visible");
    cy.contains("Os trechos mostram o que a IA considerou").should("be.visible");
  });
});
