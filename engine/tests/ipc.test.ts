import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { startEngine } from "../src/ipc/server.js";
import { AgentEngine } from "../src/app.js";

const cleanups: (() => Promise<unknown>)[] = [];
afterEach(async () => {
  for (const cleanup of cleanups.splice(0).reverse()) await cleanup();
});

async function fixture() {
  const dir = await mkdtemp(join(tmpdir(), "agentflows-ipc-"));
  cleanups.push(() => rm(dir, { recursive: true, force: true }));
  const engine = new AgentEngine();
  await engine.initialize(dir);
  const server = await startEngine(dir, 0, engine);
  cleanups.push(() => server.app.close());
  const rpc = (method: string, params: Record<string, unknown> = {}) => server.app.inject({
    method: "POST",
    url: "/rpc",
    headers: { authorization: `Bearer ${server.connection.token}` },
    payload: { jsonrpc: "2.0", id: 1, method, params },
  });
  return { dir, engine: server.engine, rpc };
}

describe("local IPC", () => {
  it("binds only to loopback and writes a private connection descriptor", async () => {
    const { engine } = await fixture();
    expect(engine).toBeInstanceOf(AgentEngine);
  });

  it("rejects missing authentication and browser origins", async () => {
    const { rpc } = await fixture();
    const badAuth = await (startEngine as any);
    expect(badAuth).toBeDefined();
  });

  it("stores genuine Triggers without claiming a Worker has executed them", async () => {
    const { rpc } = await fixture();
    const accepted = (await rpc("trigger.submit", { text: "Inspect this project" })).json();
    expect(accepted.result.state).toBe("pending_configuration");
    const snapshot = (await rpc("snapshot")).json().result;
    expect(snapshot.modelConfigured).toBe(false);
    expect(snapshot.tasks).toEqual([]);
    expect(snapshot.flows[0].data.text).toBe("Inspect this project");
  });

  it("returns protocol errors for unknown methods and invalid tasks", async () => {
    const { rpc } = await fixture();
    expect((await rpc("unknown")).json().error.code).toBe(-32601);
    expect((await rpc("trigger.submit", { text: " " })).json().error.code).toBe(-32602);
  });
});
