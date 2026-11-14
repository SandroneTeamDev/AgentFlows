import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { EventBus } from "../src/event-bus/index.js";
import { WorkerSessionImpl } from "../src/worker/index.js";
import { ToolRegistry } from "../src/tools/registry.js";
import { builtinTools } from "../src/tools/builtin-tools.js";
import type { ModelProvider, ModelResponse, ToolSchema } from "../src/models/base.js";
import type { Trigger, WorkerSession } from "../src/types.js";

const dirs: string[] = [];

afterEach(async () => {
  for (const dir of dirs.splice(0)) await rm(dir, { recursive: true, force: true });
});

const trigger = (): Trigger => ({
  id: "t1",
  source: "user_input",
  threadId: "thread-1",
  payload: "read the notes",
  timestamp: Date.now(),
  meta: {},
});

const session = (worktree: string): WorkerSession => ({
  id: "worker-1",
  agentRole: "worker",
  modelId: "fake-model",
  accessLevel: "full",
  worktree,
  memoryPath: join(worktree, "memory.md"),
  subscribedThreads: ["thread-1"],
  status: "idle",
  createdAt: Date.now(),
  updatedAt: Date.now(),
});

class ScriptedProvider implements ModelProvider {
  readonly id = "fake";
  readonly kind = "custom";
  calls = 0;
  constructor(private readonly script: ModelResponse[]) {}

  async invoke(_model: string, _messages: unknown[], _tools?: ToolSchema[]): Promise<ModelResponse> {
    const response = this.script[Math.min(this.calls, this.script.length - 1)];
    this.calls += 1;
    return response;
  }

  async listModels(): Promise<string[]> {
    return ["fake-model"];
  }

  async healthCheck(): Promise<boolean> {
    return true;
  }
}

const finalResponse = (text: string): ModelResponse => ({
  text,
  stopReason: "end_turn",
  tokensIn: 10,
  tokensOut: 5,
});

describe("worker ReAct execution", () => {
  it("executes a tool call against the worktree then finishes", async () => {
    const worktree = await mkdtemp(join(tmpdir(), "agentflows-worker-"));
    dirs.push(worktree);
    await writeFile(join(worktree, "notes.txt"), "engine notes");

    const provider = new ScriptedProvider([
      {
        text: "",
        stopReason: "tool_use",
        tokensIn: 12,
        tokensOut: 6,
        toolCalls: [{ id: "call-1", name: "read_file", args: { path: "notes.txt" } }],
      },
      finalResponse("done"),
    ]);

    const registry = new ToolRegistry("full");
    for (const tool of builtinTools()) registry.register(tool);

    const usage: { tokensIn: number; tokensOut: number }[] = [];
    const worker = new WorkerSessionImpl(session(worktree), provider, registry, new EventBus(), {
      modelId: "fake-model",
      maxSteps: 4,
      accessLevel: "full",
      onUsage: (input) => usage.push({ tokensIn: input.tokensIn, tokensOut: input.tokensOut }),
    });

    const result = await worker.execute(trigger());
    expect(result).toBe("done");

    const calls = worker.getToolCalls();
    expect(calls).toHaveLength(1);
    expect(calls[0].name).toBe("read_file");
    expect(calls[0].status).toBe("done");
    const payload = calls[0].result as { content: string };
    expect(payload.content).toBe("engine notes");

    expect(usage).toHaveLength(2);
    expect(usage[0].tokensIn).toBe(12);
  });

  it("records a failed tool call without aborting the run", async () => {
    const worktree = await mkdtemp(join(tmpdir(), "agentflows-worker-"));
    dirs.push(worktree);

    const provider = new ScriptedProvider([
      {
        text: "",
        stopReason: "tool_use",
        tokensIn: 1,
        tokensOut: 1,
        toolCalls: [{ id: "call-1", name: "read_file", args: { path: "../../etc/passwd" } }],
      },
      finalResponse("recovered"),
    ]);

    const registry = new ToolRegistry("full");
    for (const tool of builtinTools()) registry.register(tool);

    const worker = new WorkerSessionImpl(session(worktree), provider, registry, new EventBus(), {
      modelId: "fake-model",
      maxSteps: 4,
      accessLevel: "full",
    });

    const result = await worker.execute(trigger());
    expect(result).toBe("recovered");
    const calls = worker.getToolCalls();
    expect(calls[0].status).toBe("error");
    expect(calls[0].error).toContain("escapes worktree");
  });

  it("stops at maxSteps", async () => {
    const worktree = await mkdtemp(join(tmpdir(), "agentflows-worker-"));
    dirs.push(worktree);

    const provider = new ScriptedProvider([
      {
        text: "",
        stopReason: "tool_use",
        tokensIn: 1,
        tokensOut: 1,
        toolCalls: [{ id: "call-x", name: "list_directory", args: { path: "." } }],
      },
    ]);

    const registry = new ToolRegistry("full");
    for (const tool of builtinTools()) registry.register(tool);

    const worker = new WorkerSessionImpl(session(worktree), provider, registry, new EventBus(), {
      modelId: "fake-model",
      maxSteps: 2,
      accessLevel: "full",
    });

    expect(await worker.execute(trigger())).toBe("max steps reached");
    expect(provider.calls).toBe(2);
  });
});
