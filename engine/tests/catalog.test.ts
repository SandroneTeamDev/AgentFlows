import { describe, expect, it } from "vitest";
import { isMakeupWorkday, isOffDay, dayKeyOf, type HolidayCalendar } from "../src/usage/holidays.js";
import { estimateCostBreakdown, multiplierFor } from "../src/usage/pricing.js";
import { acceptsEffort, findModel, searchModels, supportedEfforts, type Catalog } from "../src/catalog/index.js";

const calendar: HolidayCalendar = {
  source: "test",
  years: {
    "2026": {
      off: ["2026-10-01", "2026-10-02"],
      work: ["2026-10-10"],
    },
  },
};

const rules = {
  isOffDay: (at: number) => isOffDay(calendar, at),
  isMakeupWorkday: (at: number) => isMakeupWorkday(calendar, at),
};

const peak = {
  days: ["mon", "tue", "wed", "thu", "fri"],
  windows: [["01:00", "04:00"]] as [string, string][],
  factor: 2,
};

const table = {
  "deepseek/deepseek-chat": { input: 1, output: 2, peak },
};

describe("holiday calendar", () => {
  it("derives a UTC day key", () => {
    expect(dayKeyOf(Date.UTC(2026, 9, 1, 12))).toBe("2026-10-01");
  });

  it("detects official days off", () => {
    expect(isOffDay(calendar, Date.UTC(2026, 9, 1, 12))).toBe(true);
    expect(isOffDay(calendar, Date.UTC(2026, 9, 3, 12))).toBe(false);
  });

  it("detects make-up workdays", () => {
    expect(isMakeupWorkday(calendar, Date.UTC(2026, 9, 10, 12))).toBe(true);
  });

  it("ignores years it does not know", () => {
    expect(isOffDay(calendar, Date.UTC(2030, 0, 1, 12))).toBe(false);
  });
});

describe("peak pricing with holidays", () => {
  it("charges the peak factor on a normal weekday inside the window", () => {
    const at = Date.UTC(2026, 9, 8, 2);
    expect(multiplierFor(table["deepseek/deepseek-chat"], at, rules)).toBe(2);
  });

  it("suppresses the peak factor on an official holiday", () => {
    const at = Date.UTC(2026, 9, 1, 2);
    expect(new Date(at).getUTCDay()).not.toBe(0);
    expect(multiplierFor(table["deepseek/deepseek-chat"], at, rules)).toBe(1);
  });

  it("applies the factor on a make-up workday that falls on a weekend", () => {
    const at = Date.UTC(2026, 9, 10, 2);
    expect(new Date(at).getUTCDay()).toBe(6);
    expect(multiplierFor(table["deepseek/deepseek-chat"], at, rules)).toBe(2);
  });

  it("feeds the multiplier into the recorded actual cost", () => {
    const breakdown = estimateCostBreakdown(
      table,
      "deepseek",
      "deepseek-chat",
      { input: 1_000_000, output: 0, cacheRead: 0, cacheWrite: 0 },
      Date.UTC(2026, 9, 8, 2),
      rules,
    );
    expect(breakdown.total).toBeCloseTo(1, 6);
    expect(breakdown.actual).toBeCloseTo(2, 6);
    expect(breakdown.multiplier).toBe(2);
  });
});

describe("model catalog", () => {
  const catalog: Catalog = {
    generatedAt: "now",
    providers: {
      deepseek: {
        name: "DeepSeek",
        models: {
          "deepseek-v4-pro": {
            id: "deepseek-v4-pro",
            type: "chat",
            context: 1_048_576,
            maxOutput: 393_216,
            reasoning: true,
            reasoningEfforts: ["low", "high", "max"],
            tools: true,
            input: ["text"],
            output: ["text"],
          },
          "glm-image": {
            id: "glm-image",
            type: "image",
            input: ["text"],
            output: ["image"],
          },
        },
      },
      openai: {
        name: "OpenAI",
        models: {
          "gpt-test": { id: "gpt-test", type: "chat", context: 128_000 },
        },
      },
    },
  };

  it("finds a model by provider and id", () => {
    expect(findModel(catalog, "deepseek", "deepseek-v4-pro")?.context).toBe(1_048_576);
  });

  it("falls back to a case-insensitive cross-provider lookup", () => {
    expect(findModel(catalog, "unknown", "GPT-TEST")?.type).toBe("chat");
  });

  it("returns undefined for unknown models", () => {
    expect(findModel(catalog, "deepseek", "nope")).toBeUndefined();
  });

  it("filters by type and context floor", () => {
    const images = searchModels(catalog, { type: "image" });
    expect(images.map((entry) => entry.model.id)).toEqual(["glm-image"]);

    const big = searchModels(catalog, { minContext: 200_000 });
    expect(big.map((entry) => entry.model.id)).toEqual(["deepseek-v4-pro"]);
  });

  it("filters by free-text query", () => {
    expect(searchModels(catalog, { query: "openai" }).map((entry) => entry.model.id)).toEqual(["gpt-test"]);
  });

  it("exposes reasoning efforts and validates them", () => {
    expect(supportedEfforts(catalog, "deepseek", "deepseek-v4-pro")).toEqual(["low", "high", "max"]);
    expect(acceptsEffort(catalog, "deepseek", "deepseek-v4-pro", "max")).toBe(true);
    expect(acceptsEffort(catalog, "deepseek", "deepseek-v4-pro", "medium")).toBe(false);
  });

  it("accepts any effort for models without declared levels", () => {
    expect(supportedEfforts(catalog, "openai", "gpt-test")).toEqual([]);
    expect(acceptsEffort(catalog, "openai", "gpt-test", "whatever")).toBe(true);
  });
});
