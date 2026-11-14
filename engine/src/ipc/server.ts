import { randomBytes, randomUUID, timingSafeEqual } from "node:crypto";
import { chmod, mkdir, rename, unlink, writeFile } from "node:fs/promises";
import { join } from "node:path";
import Fastify from "fastify";
import { AgentEngine, createEngine } from "../app.js";

export interface EngineConnection {
  host: "127.0.0.1";
  port: number;
  token: string;
  pid: number;
}

export async function startEngine(dataDir: string, port = 0, engine?: AgentEngine) {
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error("Invalid IPC port");
  await mkdir(dataDir, { recursive: true, mode: 0o700 });
  await chmod(dataDir, 0o700);
  const target = engine ?? createEngine();
  if (!target.ready) await target.initialize(dataDir);
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
    const params = (body.params ?? {}) as Record<string, unknown>;

    switch (body.method) {
      case "snapshot":
        return { jsonrpc: "2.0", id, result: target.snapshot() };
      case "trigger.submit": {
        const text = params.text;
        if (typeof text !== "string" || text.trim().length === 0 || text.length > 64_000) {
          return fail(-32602, "A non-empty task of at most 64000 characters is required");
        }
        return { jsonrpc: "2.0", id, result: await target.submit(text) };
      }
      case "task.cancel": {
        const workerId = params.workerId;
        if (typeof workerId !== "string" || !workerId) return fail(-32602, "workerId is required");
        return { jsonrpc: "2.0", id, result: target.cancel(workerId) };
      }
      case "watchdog.query": {
        const question = params.question;
        if (typeof question !== "string" || !question.trim()) return fail(-32602, "question is required");
        return { jsonrpc: "2.0", id, result: await target.askWatchdog(question.trim()) };
      }
      case "watchdog.state":
        return { jsonrpc: "2.0", id, result: target.watchdogState() };
      case "usage.list":
        return { jsonrpc: "2.0", id, result: target.listUsage() };
      case "usage.summary": {
        const range = params.range;
        const allowed = ["today", "7d", "30d", "all"];
        const selected = typeof range === "string" && allowed.includes(range) ? range : "7d";
        return {
          jsonrpc: "2.0",
          id,
          result: target.usageSummary(selected as "today" | "7d" | "30d" | "all"),
        };
      }
      case "usage.prices":
        return { jsonrpc: "2.0", id, result: target.priceStatus() };
      case "memory.read":
        return { jsonrpc: "2.0", id, result: target.readMemory(String(params.path ?? "")) };
      case "model.register": {
        const cfg = params.config as Record<string, unknown> | undefined;
        if (!cfg || typeof cfg.baseUrl !== "string" || !Array.isArray(cfg.models)) {
          return fail(-32602, "config.baseUrl and config.models are required");
        }
        return { jsonrpc: "2.0", id, result: target.registerProvider(cfg as never) };
      }
      case "model.probe": {
        const healthy = await target.probeModels();
        return { jsonrpc: "2.0", id, result: { healthy, configured: target.modelConfigured } };
      }
      case "engine.stop":
        setTimeout(() => { void app.close(); }, 50).unref();
        return { jsonrpc: "2.0", id, result: { stopping: true } };
      default:
        return fail(-32601, "Method not found");
    }
  });

  app.addHook("onClose", async () => {
    target.close();
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
    return { app, connection, engine: target };
  } catch (error) {
    await app.close();
    throw error;
  }
}
