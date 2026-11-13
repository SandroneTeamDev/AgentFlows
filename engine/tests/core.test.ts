import { describe, expect, it, vi } from "vitest";
import { ToolRegistry } from "../src/tools/registry.js";
import { Watchdog } from "../src/watchdog/index.js";
import { EventBus } from "../src/event-bus/index.js";
import type { WorkerSession } from "../src/types.js";

const definition = (name: string, effect: ToolDefinition["effect"]): ToolDefinition => ({
  name, effect, description: name, parameters: { type: "object" }, run: async () => name,
});

describe("access control", () => {
  it("does not expose modifying tools to a view-only Worker", async () => {
    const registry = new ToolRegistry("view");
    registry.register(definition("read", "read"));
    registry.register(definition("write", "write"));
    expect(registry.names()).toEqual(["read"]);
    await expect(registry.execute("write", {})).rejects.toThrow("denied");
  });

  it("does not permit arbitrary execution in workspace-write mode", async () => {
    const registry = new ToolRegistry("workspace_write");
    registry.register(definition("terminal", "execute"));
    await expect(registry.execute("terminal", {})).rejects.toThrow("denied");
  });

  it("honors cancellation before invoking a tool", async () => {
    const registry = new ToolRegistry("full");
    const run = vi.fn(async () => "done");
    registry.register({ ...definition("read", "read"), run });
    const controller = new AbortController();
    controller.abort();
    await expect(registry.execute("read", {}, controller.signal)).rejects.toThrow();
    expect(run).not.toHaveBeenCalled();
  });
});

it("exposes Watchdog state without recursive getters", () => {
  const watchdog = new Watchdog(() => {}, 1000);
  expect(watchdog.state.healthy).toBe(true);
  watchdog.applyFix("reset");
  expect(watchdog.state.alerts).toEqual([]);
});

it("delivers subscribed Thread events directly to a Worker", async () => {
  const bus = new EventBus();
  const thread = bus.createThread("task");
  const worker: WorkerSession = {
    id: "worker", agentRole: "worker", modelId: "model", accessLevel: "view", worktree: "workspace",
    memoryPath: "memory.md", subscribedThreads: [thread.id], status: "idle", createdAt: 1, updatedAt: 1,
  };
  const direct = vi.fn();
  const router = vi.fn();
  bus.registerWorker(worker);
  bus.on(worker.id, direct);
  bus.onGlobal(router);
  bus.subscribe(thread.id, worker.id);
  await bus.emit({ id: "trigger", source: "local_fs", threadId: thread.id, payload: {}, timestamp: 1, meta: {} });
  expect(direct).toHaveBeenCalledOnce();
  expect(router).not.toHaveBeenCalled();
});

interface ToolDefinition {
  name: string;
  effect: "read" | "write" | "execute";
  description: string;
  parameters: { type: string; properties?: Record<string, unknown>; required?: string[] };
  run(args: Record<string, unknown>, signal?: AbortSignal): Promise<unknown>;
}
