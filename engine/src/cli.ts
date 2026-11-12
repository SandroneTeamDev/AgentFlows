#!/usr/bin/env node
import { readFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import type { EngineConnection } from "./ipc/server.js";
import { defaultDataDirectory, runEngine } from "./runtime.js";

const [command = "serve", ...args] = process.argv.slice(2);
if (command === "serve") {
  await runEngine(args);
} else if (["status", "stop", "send"].includes(command)) {
  const { values, positionals } = parseArgs({ args, allowPositionals: true, options: {
    "data-dir": { type: "string", default: defaultDataDirectory() },
  } });
  const connection = JSON.parse(await readFile(join(resolve(values["data-dir"]!), "connection.json"), "utf8")) as EngineConnection;
  if (connection.host !== "127.0.0.1" || !Number.isInteger(connection.port) || connection.port < 1 || connection.port > 65535) throw new Error("Invalid local engine descriptor");
  const method = command === "status" ? "snapshot" : command === "stop" ? "engine.stop" : "trigger.submit";
  const response = await fetch(`http://127.0.0.1:${connection.port}/rpc`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${connection.token}` },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method, params: { text: positionals.join(" ") } }),
  });
  if (!response.ok) throw new Error(`Local IPC request failed: ${response.status}`);
  const message = await response.json() as { result?: unknown; error?: { message: string } };
  if (message.error) throw new Error(message.error.message);
  console.log(JSON.stringify(message.result, null, 2));
} else {
  console.error("Usage: agentflows [serve|status|stop|send] [--data-dir PATH] [TEXT]");
  process.exitCode = 1;
}
