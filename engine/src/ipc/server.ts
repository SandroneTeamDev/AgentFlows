import { randomBytes, randomUUID, timingSafeEqual } from "node:crypto";
import { chmod, mkdir, rename, writeFile, unlink } from "node:fs/promises";
import { join } from "node:path";
import Fastify from "fastify";
import { EventStore } from "../storage/event-store.js";

export interface EngineConnection {
  host: "127.0.0.1";
  port: number;
  token: string;
  pid: number;
}

export async function startEngine(dataDir: string, port = 0) {
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error("Invalid IPC port");
  await mkdir(dataDir, { recursive: true, mode: 0o700 });
  await chmod(dataDir, 0o700);
  const store = new EventStore(join(dataDir, "state.sqlite"));
  const token = randomBytes(32).toString("hex");
  const app = Fastify({ logger: false, bodyLimit: 256 * 1024 });
  let descriptorWritten = false;

  app.addHook("preHandler", async (request, reply) => {
    const provided = request.headers.authorization ?? "";
    const expected = `Bearer ${token}`;
    const valid = Buffer.byteLength(provided) === Buffer.byteLength(expected)
      && timingSafeEqual(Buffer.from(provided), Buffer.from(expected));
    if (request.headers.origin || !valid) return reply.code(403).send({ error: "Local IPC authentication required" });
  });

  app.get("/health", async () => ({ status: "ready", application: "AgentFlows" }));
  app.post("/rpc", async (request) => {
    const body = request.body as Record<string, unknown> | null;
    const id = body?.id ?? null;
    const fail = (code: number, message: string) => ({ jsonrpc: "2.0", id, error: { code, message } });
    if (!body || body.jsonrpc !== "2.0" || typeof body.method !== "string") return fail(-32600, "Invalid Request");
    const params = body.params as Record<string, unknown> | undefined;
    switch (body.method) {
      case "snapshot":
        return { jsonrpc: "2.0", id, result: {
          status: { running: true, modelConfigured: false },
          flows: store.list(),
          tasks: [],
        } };
      case "trigger.submit": {
        const text = params?.text;
        if (typeof text !== "string" || text.trim().length === 0 || text.length > 64_000) return fail(-32602, "A non-empty task of at most 64000 characters is required");
        const triggerId = randomUUID();
        store.append({
          id: triggerId,
          type: "trigger",
          label: "User task received · Router configuration pending",
          data: { text: text.trim(), source: "custom_webhook", state: "pending_configuration" },
          timestamp: Date.now(),
        });
        return { jsonrpc: "2.0", id, result: { triggerId, state: "pending_configuration" } };
      }
      case "engine.stop":
        setTimeout(() => { void app.close(); }, 50).unref();
        return { jsonrpc: "2.0", id, result: { stopping: true } };
      default:
        return fail(-32601, "Method not found");
    }
  });
  app.addHook("onClose", async () => {
    store.close();
    if (descriptorWritten) await unlink(join(dataDir, "connection.json")).catch(() => {});
  });

  try {
    await app.listen({ host: "127.0.0.1", port });
    const address = app.server.address();
    if (!address || typeof address === "string") throw new Error("IPC listener has no TCP address");
    const connection: EngineConnection = { host: "127.0.0.1", port: address.port, token, pid: process.pid };
    const tempPath = join(dataDir, `connection.${randomUUID()}.tmp`);
    await writeFile(tempPath, JSON.stringify(connection), { mode: 0o600 });
    await rename(tempPath, join(dataDir, "connection.json"));
    descriptorWritten = true;
    return { app, connection };
  } catch (error) {
    await app.close();
    throw error;
  }
}
