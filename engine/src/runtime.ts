import { homedir } from "node:os";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import { startEngine } from "./ipc/server.js";

export function defaultDataDirectory(): string {
  if (process.platform === "win32") return join(process.env.LOCALAPPDATA ?? homedir(), "AgentFlows");
  if (process.platform === "darwin") return join(homedir(), "Library", "Application Support", "AgentFlows");
  return join(process.env.XDG_DATA_HOME ?? join(homedir(), ".local", "share"), "agentflows");
}

export async function runEngine(args: string[]): Promise<void> {
  const { values } = parseArgs({ args, options: {
    "data-dir": { type: "string", default: defaultDataDirectory() },
    port: { type: "string", default: "0" },
  } });
  const engine = await startEngine(resolve(values["data-dir"]!), Number(values.port));
  console.log(`AgentFlows local engine listening on 127.0.0.1:${engine.connection.port}`);
  for (const signal of ["SIGINT", "SIGTERM"] as const) {
    process.once(signal, () => { void engine.app.close(); });
  }
}
