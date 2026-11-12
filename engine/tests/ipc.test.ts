import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { startEngine } from "../src/ipc/server.js";

const cleanups: (() => Promise<unknown>)[] = [];
afterEach(async () => {
  for (const cleanup of cleanups.splice(0).reverse()) await cleanup();
});

async function fixture() {
  const dir = await mkdtemp(join(tmpdir(), "agentflows-ipc-"));
  cleanups.push(() => rm(dir, { recursive: true, force: true }));
  const engine = await startEngine(dir);
  cleanups.push(() => engine.app.close());
  const rpc = (method: string, params: Record<string, unknown> = {}) => engine.app.inject({
    method: "POST",
    url: "/rpc",
    headers: { authorization: `Bearer ${engine.connection.token}` },
    payload: { jsonrpc: "2.0", id: 1, method, params },
  });
  return { dir, engine, rpc };
}

describe("local IPC", () => {
  it("binds only to loopback and writes a private connection descriptor", async () => {
    const { dir, engine } = await fixture();
    expect(engine.connection.host).toBe("127.0.0.1");
    const descriptor = JSON.parse(await readFile(join(dir, "connection.json"), "utf8"));
    expect(descriptor.token).toMatch(/^[a-f0-9]{64}$/);
  });

  it("rejects missing authentication and browser origins", async () => {
    const { engine } = await fixture();
    expect((await engine.app.inject({ method: "GET", url: "/health" })).statusCode).toBe(403);
    expect((await engine.app.inject({ method: "GET", url: "/health", headers: {
      authorization: `Bearer ${engine.connection.token}`, origin: "https://example.com",
    } })).statusCode).toBe(403);
  });

  it("stores genuine Triggers without claiming a Worker has executed them", async () => {
    const { rpc } = await fixture();
    const accepted = (await rpc("trigger.submit", { text: "Inspect this project" })).json();
    expect(accepted.result.state).toBe("pending_configuration");
    const snapshot = (await rpc("snapshot")).json().result;
    expect(snapshot.status.modelConfigured).toBe(false);
    expect(snapshot.tasks).toEqual([]);
    expect(snapshot.flows[0].data.text).toBe("Inspect this project");
  });

  it("returns protocol errors for unknown methods and invalid tasks", async () => {
    const { rpc } = await fixture();
    expect((await rpc("unknown")).json().error.code).toBe(-32601);
    expect((await rpc("trigger.submit", { text: " " })).json().error.code).toBe(-32602);
  });
});
