import { randomUUID } from "node:crypto";
import type { EventBus } from "./event-bus.js";
import type {
  Trigger,
  RouterSession,
  WorkerSession,
  TaskRecord,
  AgentFlowNode,
  InlineRef,
} from "./types.js";
import type { ModelProvider } from "../model/provider.js";
import type { ModelRouter } from "../model/router.js";

interface RouterConfig {
  modelId: string;
  batchSize: number;
  maxConcurrency: number;
}

export class RouterLayer {
  private session: RouterSession;
  private bus: EventBus;
  private modelRouter: ModelRouter;
  private pendingTriggers: Trigger[] = [];
  private tasks: Map<string, TaskRecord> = new Map();
  private flowNodes: AgentFlowNode[] = [];
  private recallCache = new Map<string, InlineRef>();
  private config: RouterConfig;

  constructor(bus: EventBus, modelRouter: ModelRouter, config: RouterConfig) {
    this.bus = bus;
    this.modelRouter = modelRouter;
    this.config = config;
    this.session = {
      id: randomUUID(),
      modelId: config.modelId,
      activeWorkers: [],
    };
  }

  get sessionId(): string {
    return this.session.id;
  }

  handleTrigger(trigger: Trigger): void {
    this.pendingTriggers.push(trigger);
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
      .map(
        (t) =>
          `[${t.source}] ${JSON.stringify(t.payload, null, 0)}`,
      )
      .join("\n---\n");

    const response = await provider.invoke(this.config.modelId, [
      {
        role: "system",
        content:
          "You are the router layer of a multi-agent system. " +
          "Decide which triggers require action and how to group them into tasks. " +
          "Return JSON: {\"tasks\": [{\"threadId\": \"...\", \"title\": \"...\", \"description\": \"...\"}]} " +
          "or {\"tasks\": []} if no action needed.",
      },
      {
        role: "user",
        content: payload,
      },
    ]);

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
    };

    this.bus.registerWorker(worker);
    this.session.activeWorkers.push(worker.id);

    const taskRecord: TaskRecord = {
      id: worker.id,
      title: task.title,
      status: "assigned",
      assignedWorker: worker.id,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    this.tasks.set(worker.id, taskRecord);

    this.flowNodes.push({
      id: randomUUID(),
      type: "worker_start",
      label: task.title,
      data: { workerId: worker.id, threadId: task.threadId },
      timestamp: Date.now(),
    });

    return worker;
  }

  recall(objectId: string): InlineRef | undefined {
    return this.recallCache.get(objectId);
  }

  chainRecall(
    startId: string,
    depth: number = 3,
  ): InlineRef[] {
    const results: InlineRef[] = [];
    const visited = new Set<string>();
    const queue: { ref: InlineRef; d: number }[] = [
      { ref: this.recallCache.get(startId)!, d: 0 },
    ];

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
  }

  spill(objectId: string, truncatedSummary: string): void {
    this.recallCache.set(objectId, {
      id: objectId,
      summary: `${truncatedSummary} [spilled - call read to get full content]`,
    });
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
    this.session.activeWorkers = this.session.activeWorkers.filter(
      (w) => w !== workerId,
    );
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
}
