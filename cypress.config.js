import { limparBancoDeDados } from "./cypress/tasks/limpar-banco.js";

export default {
  video: true,
  videosFolder: "cypress/downloads",
  screenshotOnRunFailure: true,
  screenshotsFolder: "cypress/screenshots",
  viewportWidth: 1280,
  viewportHeight: 720,
  defaultCommandTimeout: 8000,
  requestTimeout: 10000,
  responseTimeout: 30000,
  retries: { runMode: 2, openMode: 0 },

  e2e: {
    baseUrl: "http://localhost:8080",
    specPattern: "cypress/e2e/**/*.cy.{js,ts}",
    supportFile: "cypress/support/e2e.js",
    setupNodeEvents(on) {
      on("task", { limparBancoDeDados });
    },
  },
};
