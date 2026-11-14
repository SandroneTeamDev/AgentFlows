import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { EventStore } from "../src/storage/index.js";
import type { SessionState } from "../src/types.js";

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
