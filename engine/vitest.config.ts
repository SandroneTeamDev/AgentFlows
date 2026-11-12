import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["tests/**/*.test.ts"],
    pool: "forks",
    minWorkers: 1,
    maxWorkers: 1,
  },
});
