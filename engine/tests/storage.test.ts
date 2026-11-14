import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { EventStore } from "../src/storage/index.js";
import type { SessionState, UsageRecord } from "../src/types.js";

const dirs: string[] = [];

const makeStore = async () => {
  const dir = await mkdtemp(join(tmpdir(), "agentflows-store-"));
  dirs.push(dir);
  return new EventStore(join(dir, "state.sqlite"));
};

afterEach(async () => {
  for (const dir of dirs.splice(0)) await rm(dir, { recursive: true, force: true });
});

const sessionState = (id: string, updatedAt: number): SessionState => ({
  id,
  role: "worker",
  modelId: "gpt-test",
  context: [{ result: "done" }],
  toolCalls: [
    {
      id: "call-1",
      name: "read_file",
      args: { path: "a.txt" },
      status: "done",
      startedAt: 1,
      finishedAt: 2,
    },
  ],
  subscriptions: ["thread-1"],
  createdAt: 1,
  updatedAt,
});

describe("session persistence", () => {
  it("round-trips a session with its tool calls", async () => {
    const store = await makeStore();
    store.upsertSession(sessionState("worker-1", 10));

    const stored = store.getSession("worker-1");
    expect(stored?.modelId).toBe("gpt-test");
    expect(stored?.subscriptions).toEqual(["thread-1"]);
    expect(stored?.toolCalls).toHaveLength(1);
    expect(stored?.toolCalls[0].name).toBe("read_file");
    store.close();
  });

  it("overwrites a session on repeated writes", async () => {
    const store = await makeStore();
    store.upsertSession(sessionState("worker-1", 10));
    store.upsertSession({ ...sessionState("worker-1", 20), context: [{ result: "again" }] });

    const rows = store.listSessions();
    expect(rows).toHaveLength(1);
    expect(rows[0].updatedAt).toBe(20);
    expect(rows[0].context).toEqual([{ result: "again" }]);
    store.close();
  });

  it("returns undefined for an unknown session", async () => {
    const store = await makeStore();
    expect(store.getSession("missing")).toBeUndefined();
    store.close();
  });

  it("orders sessions by most recent update", async () => {
    const store = await makeStore();
    store.upsertSession(sessionState("older", 5));
    store.upsertSession(sessionState("newer", 50));

    expect(store.listSessions().map((row) => row.id)).toEqual(["newer", "older"]);
    store.close();
  });
});
const usageRecord = (patch: Partial<UsageRecord> = {}): UsageRecord => ({
  sessionId: "worker-1",
  workerId: "worker-1",
  role: "worker",
  provider: "deepseek",
  model: "deepseek-chat",
  stream: false,
  tokensIn: 800,
  tokensOut: 200,
  cacheHits: 100,
  cacheWriteTokens: 50,
  cost: { input: 0.1, output: 0.2, cacheRead: 0.01, cacheWrite: 0.02, total: 0.33, actual: 0.66 },
  priceMultiplier: 2,
  priced: true,
  durationMs: 1200,
  toolCalls: 3,
  retries: 1,
  errorCategory: "rate_limit",
  timestamp: Date.UTC(2026, 4, 11, 12),
  ...patch,
});

describe("usage log persistence", () => {
  it("round-trips every recorded dimension", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord());

    const rows = store.usageSince(0);
    expect(rows).toHaveLength(1);
    const row = rows[0];
    expect(row.role).toBe("worker");
    expect(row.tokensIn).toBe(800);
    expect(row.cacheHits).toBe(100);
    expect(row.cacheWriteTokens).toBe(50);
    expect(row.cost.total).toBeCloseTo(0.33, 6);
    expect(row.cost.actual).toBeCloseTo(0.66, 6);
    expect(row.priceMultiplier).toBe(2);
    expect(row.durationMs).toBe(1200);
    expect(row.toolCalls).toBe(3);
    expect(row.retries).toBe(1);
    expect(row.errorCategory).toBe("rate_limit");
    store.close();
  });

  it("filters by time window", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord({ timestamp: 1000 }));
    store.recordUsage(usageRecord({ timestamp: 5000 }));

    expect(store.usageSince(2000)).toHaveLength(1);
    expect(store.usageSince(0, 2000)).toHaveLength(1);
    store.close();
  });

  it("counts tool usage and failures", async () => {
    const store = await makeStore();
    store.recordToolUsage({ sessionId: "s", tool: "read_file", ok: true, durationMs: 10, timestamp: 100 });
    store.recordToolUsage({ sessionId: "s", tool: "read_file", ok: false, durationMs: 30, timestamp: 200 });
    store.recordToolUsage({ sessionId: "s", tool: "terminal", ok: true, durationMs: 50, timestamp: 300 });

    const stats = store.toolUsageStats(0);
    const readFile = stats.find((entry) => entry.tool === "read_file");
    expect(readFile?.calls).toBe(2);
    expect(readFile?.failures).toBe(1);
    expect(readFile?.avgDurationMs).toBe(20);
    store.close();
  });

  it("groups errors by category", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord({ errorCategory: "rate_limit" }));
    store.recordUsage(usageRecord({ errorCategory: "rate_limit" }));
    store.recordUsage(usageRecord({ errorCategory: undefined }));

    const errors = store.errorBreakdown(0);
    expect(errors.find((entry) => entry.category === "rate_limit")?.requests).toBe(2);
    expect(errors.find((entry) => entry.category === "none")?.requests).toBe(1);
    store.close();
  });

  it("builds a daily rollup", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord({ timestamp: Date.UTC(2026, 4, 11, 10) }));
    store.recordUsage(usageRecord({ timestamp: Date.UTC(2026, 4, 11, 14) }));
    store.refreshDailyRollup(0, Date.UTC(2026, 5, 1));

    const rollup = store.rollupSince("2026-05-01");
    const row = rollup.find((entry) => entry.day === "2026-05-11");
    expect(row?.requests).toBe(2);
    expect(row?.tokens_in).toBe(1600);
    store.close();
  });

  it("deletes records older than the cutoff", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord({ timestamp: 1000 }));
    store.recordUsage(usageRecord({ timestamp: 9000 }));

    const deleted = store.cleanupUsage(5000);
    expect(deleted).toBe(1);
    expect(store.usageSince(0)).toHaveLength(1);
    store.close();
  });

  it("exports the requested window", async () => {
    const store = await makeStore();
    store.recordUsage(usageRecord({ timestamp: 1000 }));
    store.recordUsage(usageRecord({ timestamp: 9000 }));

    expect(store.exportUsage(0, 5000)).toHaveLength(1);
    store.close();
  });
});
