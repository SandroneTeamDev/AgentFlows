import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import {
  estimateCost,
  loadPriceTable,
  multiplierFor,
  normalizePriceTable,
  resolvePrice,
} from "../src/usage/pricing.js";
import { dayKey, summarizeUsage } from "../src/usage/aggregate.js";
import type { UsageRecord } from "../src/types.js";

const peakRule = {
  days: ["mon", "tue", "wed", "thu", "fri"],
  windows: [["01:00", "04:00"], ["06:00", "10:00"]] as [string, string][],
  factor: 2,
};

const table = {
  "deepseek/deepseek-chat": { input: 0.27, output: 1.1, cacheRead: 0.07, peak: peakRule },
  "gpt-4o": { input: 2.5, output: 10 },
};

const record = (patch: Partial<UsageRecord> = {}): UsageRecord => ({
  sessionId: "s1",
  model: "deepseek-chat",
  provider: "deepseek",
  tokensIn: 1_000_000,
  tokensOut: 1_000_000,
  cacheHits: 0,
  cost: 0,
  timestamp: Date.UTC(2026, 4, 10, 12, 0, 0),
  ...patch,
});

const dirs: string[] = [];

afterEach(async () => {
  for (const dir of dirs.splice(0)) await rm(dir, { recursive: true, force: true });
});

describe("price resolution", () => {
  it("prefers a provider-scoped entry", () => {
    expect(resolvePrice(table, "deepseek", "deepseek-chat")).toEqual(table["deepseek/deepseek-chat"]);
  });

  it("falls back to a bare model id", () => {
    expect(resolvePrice(table, "openai", "gpt-4o")).toEqual(table["gpt-4o"]);
  });

  it("matches case-insensitively", () => {
    expect(resolvePrice(table, "openai", "GPT-4o")).toEqual(table["gpt-4o"]);
  });

  it("returns undefined for an unknown model", () => {
    expect(resolvePrice(table, "openai", "unknown-model")).toBeUndefined();
  });
});

describe("cost estimation", () => {
  it("charges input and output rates per million tokens", () => {
    const cost = estimateCost(table, "openai", "gpt-4o", 1_000_000, 1_000_000, 0, Date.UTC(2026, 4, 10, 12));
    expect(cost).toBeCloseTo(12.5, 6);
  });

  it("charges cached input at the cache rate", () => {
    const peak = Date.UTC(2026, 4, 10, 12);
    const cost = estimateCost(table, "deepseek", "deepseek-chat", 1_000_000, 0, 1_000_000, peak);
    expect(cost).toBeCloseTo(0.07, 6);
  });

  it("returns null when the model has no price", () => {
    expect(estimateCost(table, "openai", "unknown-model", 10, 10, 0, Date.now())).toBeNull();
  });

  it("doubles DeepSeek cost inside a peak window", () => {
    const cost = estimateCost(table, "deepseek", "deepseek-chat", 1_000_000, 1_000_000, 0, Date.UTC(2026, 4, 11, 2));
    expect(cost).toBeCloseTo((0.27 + 1.1) * 2, 6);
  });

  it("keeps the base price outside peak windows", () => {
    const cost = estimateCost(table, "deepseek", "deepseek-chat", 1_000_000, 1_000_000, 0, Date.UTC(2026, 4, 11, 12));
    expect(cost).toBeCloseTo(0.27 + 1.1, 6);
  });

  it("keeps the base price for models without a peak rule", () => {
    const cost = estimateCost(table, "openai", "gpt-4o", 1_000_000, 0, 0, Date.UTC(2026, 4, 11, 2));
    expect(cost).toBeCloseTo(2.5, 6);
  });
});

describe("peak multiplier", () => {
  const entry = { input: 1, output: 2, peak: peakRule };

  it("applies the factor inside a weekday window", () => {
    expect(multiplierFor(entry, Date.UTC(2026, 4, 11, 2))).toBe(2);
    expect(multiplierFor(entry, Date.UTC(2026, 4, 11, 7))).toBe(2);
  });

  it("ignores hours outside every window", () => {
    expect(multiplierFor(entry, Date.UTC(2026, 4, 11, 12))).toBe(1);
    expect(multiplierFor(entry, Date.UTC(2026, 4, 11, 0, 30))).toBe(1);
  });

  it("ignores weekend days when the rule names weekdays", () => {
    expect(multiplierFor(entry, Date.UTC(2026, 4, 10, 2))).toBe(1);
  });

  it("returns 1 without a rule", () => {
    expect(multiplierFor({ input: 1, output: 1 }, Date.UTC(2026, 4, 11, 2))).toBe(1);
  });
});

describe("price table loading", () => {
  it("degrades to an empty table with an error when the file is missing", async () => {
    const loaded = await loadPriceTable("/nonexistent/models-dev.json");
    expect(loaded.source).toBe("empty");
    expect(loaded.table).toEqual({});
    expect(loaded.error).toBeTruthy();
  });

  it("loads a valid table from disk", async () => {
    const dir = await mkdtemp(join(tmpdir(), "agentflows-prices-"));
    dirs.push(dir);
    const file = join(dir, "prices.json");
    await writeFile(file, JSON.stringify(table));
    const loaded = await loadPriceTable(file);
    expect(loaded.source).toBe("file");
    expect(Object.keys(loaded.table).sort()).toEqual(["deepseek/deepseek-chat", "gpt-4o"]);
  });

  it("drops entries without numeric input and output", () => {
    const normalized = normalizePriceTable({
      good: { input: 1, output: 2 },
      bad: { input: "x", output: 2 },
      worse: null,
    });
    expect(Object.keys(normalized)).toEqual(["good"]);
  });
});

describe("usage aggregation", () => {
  const now = Date.UTC(2026, 4, 10, 12, 0, 0);
  const costOf = (entry: UsageRecord) => entry.cost;

  it("sums tokens and cost inside the window", () => {
    const summary = summarizeUsage([record({ cost: 1.5 }), record({ sessionId: "s2", cost: 2.5 })], "7d", now, costOf);
    expect(summary.totals.tokens).toBe(4_000_000);
    expect(summary.totals.cost).toBeCloseTo(4, 6);
    expect(summary.totals.requests).toBe(2);
    expect(summary.composition.input).toBe(2_000_000);
    expect(summary.composition.output).toBe(2_000_000);
  });

  it("excludes records outside the window", () => {
    const old = record({ timestamp: now - 40 * 86_400_000, cost: 99 });
    const summary = summarizeUsage([old, record({ cost: 1 })], "7d", now, costOf);
    expect(summary.totals.cost).toBeCloseTo(1, 6);
  });

  it("groups by session and keeps last activity", () => {
    const summary = summarizeUsage(
      [
        record({ sessionId: "a", cost: 1, timestamp: now - 1000 }),
        record({ sessionId: "a", cost: 2, timestamp: now - 500 }),
        record({ sessionId: "b", cost: 3, timestamp: now - 2000 }),
      ],
      "7d",
      now,
      costOf,
    );
    expect(summary.bySession[0].sessionId).toBe("a");
    expect(summary.bySession[0].cost).toBeCloseTo(3, 6);
    expect(summary.bySession[0].requests).toBe(2);
  });

  it("flags unpriced models instead of inventing cost", () => {
    const summary = summarizeUsage([record()], "7d", now, () => null);
    expect(summary.totals.priced).toBe(false);
    expect(summary.totals.cost).toBeCloseTo(0, 6);
  });

  it("produces an 84-day heatmap ending today", () => {
    const summary = summarizeUsage([record({ cost: 1 })], "7d", now, costOf);
    expect(summary.heatmap).toHaveLength(84);
    expect(summary.heatmap[83].date).toBe(dayKey(now));
    expect(summary.heatmap[83].tokens).toBe(2_000_000);
  });

  it("honours the all range", () => {
    const ancient = record({ timestamp: Date.UTC(2020, 0, 1), cost: 5 });
    const summary = summarizeUsage([ancient], "all", now, costOf);
    expect(summary.totals.cost).toBeCloseTo(5, 6);
  });
});
