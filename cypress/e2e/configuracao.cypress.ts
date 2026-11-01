import { defineConfig } from "cypress";

export default defineConfig({
  video: true,
  videosFolder: "../downloads",
  screenshotOnRunFailure: true,
  screenshotsFolder: "../screenshots",
  viewportWidth: 1280,
  viewportHeight: 720,
  defaultCommandTimeout: 8000,
  requestTimeout: 10000,
  responseTimeout: 30000,
  retries: { runMode: 2, openMode: 0 },
  e2e: {
    baseUrl: "http://localhost:8080",
    specPattern: "**/*.cy.{js,ts}",
    supportFile: "../support/e2e.js",
  },
});
