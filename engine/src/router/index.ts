import { randomUUID } from "node:crypto";
import type { EventBus } from "../event-bus/index.js";
import type { InlineRef } from "../types.js";
import type {
  AgentFlowNode,
  ModelAssignment,
  ProviderConfig,
  RouterSession,
  TaskRecord,
  Trigger,
  WorkerSession,
} from "../types.js";
import type { ModelProvider } from "../models/base.js";
import type { ModelRouter } from "../models/router.js";
import { InlineSpillManager } from "../memory/index.js";

interface RouterConfig {
  modelId: string;
  batchSize: number;
  maxConcurrency: number;
  onUsage?: (input: {
    sessionId: string;
    role: "router";
    providerId: string;
    model: string;
    stopReason?: string;
    tokensIn: number;
    tokensOut: number;
    cacheHits: number;
    durationMs?: number;
    at: number;
  }) => void;
  onWorkerSpawned?: (session: WorkerSession) => void;
}

export class RouterLayer {
  private session: RouterSession;
  private bus: EventBus;
  private modelRouter: ModelRouter;
  private pendingTriggers: Trigger[] = [];
  private tasks = new Map<string, TaskRecord>();
  private flowNodes: AgentFlowNode[] = [];
  private recallCache = new Map<string, InlineRef>();
  private config: RouterConfig;
  private readonly memory: InlineSpillManager;

  constructor(
    bus: EventBus,
    modelRouter: ModelRouter,
    config: RouterConfig,
    memory: InlineSpillManager,
  ) {
    this.bus = bus;
    this.modelRouter = modelRouter;
    this.config = config;
    this.memory = memory;
    this.session = {
      id: randomUUID(),
      modelId: config.modelId,
      activeWorkers: [],
      pendingTriggers: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
  }

  get sessionId(): string {
    return this.session.id;
  }

  handleTrigger(trigger: Trigger): void {
    this.pendingTriggers.push(trigger);
    this.session.pendingTriggers.push(trigger.id);
    this.session.updatedAt = Date.now();
    this.flowNodes.push({
      id: randomUUID(),
      type: "trigger",
      label: trigger.source,
      data: { triggerId: trigger.id },
      timestamp: Date.now(),
    });
  }

  async flush(): Promise<void> {
    if (this.pendingTriggers.length === 0) return;

    const batch = this.pendingTriggers.slice(0, this.config.batchSize);
    this.pendingTriggers.splice(0, batch.length);
    this.session.pendingTriggers = this.session.pendingTriggers.filter(
      (id) => !batch.some((t) => t.id === id),
    );
    this.session.updatedAt = Date.now();

    const provider = this.modelRouter.resolve(this.config.modelId);
    const decision = await this.routeBatch(batch, provider);

    for (const task of decision.tasks) {
      const worker = this.spawnWorker(task);
      this.bus.subscribe(task.threadId, worker.id);
    }

    this.flowNodes.push({
      id: randomUUID(),
      type: "router_decision",
      label: `dispatched ${decision.tasks.length} tasks`,
      data: { batch: batch.map((t) => t.id) },
      timestamp: Date.now(),
    });
  }

  private async routeBatch(
    triggers: Trigger[],
    provider: ModelProvider,
  ): Promise<{ tasks: { threadId: string; title: string; description: string }[] }> {
    const payload = triggers
      .map((t) => `[${t.source}] ${JSON.stringify(t.payload, null, 0)}`)
      .join("\n---\n");

    const startedAt = Date.now();
    const response = await provider.invoke(this.config.modelId, [
      {
        role: "system",
        content:
          "You are the router layer of a multi-agent system. " +
          "Decide which triggers require action and how to group them into tasks. " +
          'Return JSON: {"tasks": [{"threadId": "...", "title": "...", "description": "..."}]} ' +
          'or {"tasks": []} if no action needed.',
      },
      { role: "user", content: payload },
    ]);

    this.config.onUsage?.({
      sessionId: this.session.id,
      role: "router",
      providerId: provider.id,
      model: this.config.modelId,
      stopReason: response.stopReason,
      tokensIn: response.tokensIn,
      tokensOut: response.tokensOut,
      cacheHits: response.cacheReadTokens ?? 0,
      durationMs: Date.now() - startedAt,
      at: startedAt,
    });

    const parsed = JSON.parse(response.text) as {
      tasks: { threadId: string; title: string; description: string }[];
    };
    return { tasks: parsed.tasks ?? [] };
  }

  private spawnWorker(task: {
    threadId: string;
    title: string;
    description: string;
  }): WorkerSession {
    const workerId = randomUUID();
    const worker: WorkerSession = {
      id: workerId,
      agentRole: "worker",
      modelId: this.modelRouter.resolveStrongest(),
      accessLevel: "workspace_write",
      worktree: `./.agentflows/workers/${workerId}`,
      memoryPath: `./.agentflows/memory/${workerId}.md`,
      subscribedThreads: [task.threadId],
      status: "idle",
      taskId: task.title,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    this.bus.registerWorker(worker);
    this.config.onWorkerSpawned?.(worker);
    this.session.activeWorkers.push(workerId);

    const taskRecord: TaskRecord = {
      id: workerId,
      title: task.title,
      status: "assigned",
      assignedWorker: workerId,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    this.tasks.set(workerId, taskRecord);

    this.flowNodes.push({
      id: randomUUID(),
      type: "worker_start",
      label: task.title,
      data: { workerId, threadId: task.threadId },
      timestamp: Date.now(),
    });

    return worker;
  }

  recall(objectId: string): InlineRef | undefined {
    return this.recallCache.get(objectId);
  }

  chainRecall(startId: string, depth = 3): InlineRef[] {
    const results: InlineRef[] = [];
    const visited = new Set<string>();
    const start = this.recallCache.get(startId);
    const queue: { ref: InlineRef; d: number }[] = start ? [{ ref: start, d: 0 }] : [];

    while (queue.length > 0) {
      const { ref, d } = queue.shift()!;
      if (!ref || visited.has(ref.id)) continue;
      visited.add(ref.id);
      results.push(ref);
      if (d < depth && ref.next) {
        for (const n of ref.next) {
          queue.push({ ref: this.recallCache.get(n.id) ?? n, d: d + 1 });
        }
      }
    }
    return results;
  }

  inline(objectId: string, summary: string, next: string[] = []): void {
    this.recallCache.set(objectId, {
      id: objectId,
      summary,
      next: next.length > 0 ? next.map((id) => ({ id, summary: "" })) : undefined,
    });
    this.memory.inline(objectId, summary, next);
  }

  spill(objectId: string, truncatedSummary: string): void {
    this.recallCache.set(objectId, {
      id: objectId,
      summary: `${truncatedSummary} [spilled - call read to get full content]`,
    });
    this.memory.spill(objectId, truncatedSummary);
  }

  getFlowNodes(): AgentFlowNode[] {
    return this.flowNodes;
  }

  getTasks(): TaskRecord[] {
    return [...this.tasks.values()];
  }

  getActiveWorkers(): string[] {
    return this.session.activeWorkers;
  }

  onWorkerComplete(workerId: string, result: unknown): void {
    const task = this.tasks.get(workerId);
    if (task) {
      task.status = "done";
      task.updatedAt = Date.now();
      task.result = result;
    }
    this.session.activeWorkers = this.session.activeWorkers.filter((w) => w !== workerId);
    this.session.updatedAt = Date.now();
    this.flowNodes.push({
      id: randomUUID(),
      type: "worker_done",
      label: task?.title ?? workerId,
      data: { workerId },
      timestamp: Date.now(),
    });
  }

  onWorkerError(workerId: string, error: string): void {
    const task = this.tasks.get(workerId);
    if (task) {
      task.status = "failed";
      task.updatedAt = Date.now();
    }
    this.flowNodes.push({
      id: randomUUID(),
      type: "error",
      label: error,
      data: { workerId },
      timestamp: Date.now(),
    });
  }

  getSession(): RouterSession {
    return this.session;
  }

  getAssignment(): ModelAssignment {
    return {
      chatModel: this.config.modelId,
      memoryModel: this.config.modelId,
      summaryModel: this.config.modelId,
      uiModel: this.config.modelId,
    };
  }

  registerProviderConfig(config: ProviderConfig): void {
    this.modelRouter.register({ provider: this.modelRouter.resolve(config.models[0] ?? config.id), config });
  }
}
