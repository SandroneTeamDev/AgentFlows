import { randomUUID } from "node:crypto";
import { mkdir, readFile } from "node:fs/promises";
import { EventBus } from "./event-bus/index.js";
import { TwoLevelFilter } from "./filter/index.js";
import { InlineSpillManager } from "./memory/index.js";
import { SpeculativeExecutor } from "./specul/index.js";
import { Watchdog, type WatchdogEvent } from "./watchdog/index.js";
import type { WatchdogState } from "./types.js";
import { EventStore } from "./storage/index.js";
import { OpenAICompatibleProvider } from "./models/openai.js";
import { ModelRouter } from "./models/router.js";
import { RouterLayer } from "./router/index.js";
import { ToolRegistry } from "./tools/registry.js";
import { controlTools, readOnlyTools, writeTools } from "./tools/builtin-tools.js";
import { AsyncReActLoop } from "./loop.js";
import { WorkerSessionImpl } from "./worker/index.js";
import type { ProviderConfig, Trigger, UsageRecord, WorkerSession } from "./types.js";

export interface SubmitResult {
  triggerId: string;
  state: "pending_configuration" | "queued";
}

export interface WatchdogQueryResult {
  question: string;
  healthy: boolean;
  alerts: string[];
  answer: string;
  checkedAt: number;
}

const DEFAULT_PROVIDER: ProviderConfig = {
  id: "openai",
  kind: "openai",
  baseUrl: "http://127.0.0.1:11434/v1",
  models: [],
};

export class AgentEngine {
  private bus = new EventBus();
  private filter = new TwoLevelFilter();
  private memory = new InlineSpillManager();
  private specul = new SpeculativeExecutor();
  private store?: EventStore;
  private modelRouter = new ModelRouter();
  private providers = new Map<string, ProviderConfig>();
  private router?: RouterLayer;
  private registry = new ToolRegistry("full");
  private workers = new Map<string, WorkerSessionImpl>();
  private loops = new Map<string, AsyncReActLoop>();
  private watchdog?: Watchdog;
  private watchdogEvents: WatchdogEvent[] = [];
  private dataDir = "";
  private startedAt = Date.now();

  readonly ready = false;

  async initialize(dataDir: string): Promise<void> {
    this.dataDir = dataDir;
    await mkdir(dataDir, { recursive: true });
    this.store = new EventStore(`${dataDir}/state.sqlite`);

    this.providers.set(DEFAULT_PROVIDER.id, DEFAULT_PROVIDER);
    const openai = new OpenAICompatibleProvider(DEFAULT_PROVIDER.id, DEFAULT_PROVIDER.baseUrl);
    this.modelRouter.register({ provider: openai, config: DEFAULT_PROVIDER });
    this.modelRouter.setFallbackChain([...DEFAULT_PROVIDER.models]);

    this.router = new RouterLayer(
      this.bus,
      this.modelRouter,
      { modelId: DEFAULT_PROVIDER.models[0] ?? "", batchSize: 8, maxConcurrency: 4 },
      new InlineSpillManager(),
    );

    this.registry = new ToolRegistry("full");
    for (const tool of [...readOnlyTools(), ...writeTools(), ...controlTools()]) {
      this.registry.register(tool);
    }

    this.watchdog = new Watchdog((event) => {
      this.watchdogEvents.push(event);
      if (this.watchdogEvents.length > 200) this.watchdogEvents.shift();
      if (event.type === "alert") {
        this.store?.append({
          id: randomUUID(),
          type: "watchdog_alert",
          label: event.message,
          data: { workerId: event.workerId ?? null },
          timestamp: Date.now(),
        });
      }
    }, 5000, 60_000);
    this.watchdog.start();

    this.bus.onGlobal(async (trigger) => {
      if (!this.router) return;
      this.router.handleTrigger(trigger);
      await this.router.flush();
    });

    (this as { ready: boolean }).ready = true;
  }

  get modelConfigured(): boolean {
    return [...this.providers.values()].some((p) => p.models.length > 0);
  }

  registerProvider(config: ProviderConfig): { registered: boolean; models: string[] } {
    if (!config.id || !config.baseUrl) throw new Error("id and baseUrl are required");
    this.providers.set(config.id, config);
    const provider = new OpenAICompatibleProvider(
      config.id,
      config.baseUrl,
      config.apiKey,
      config.headers,
      config.body,
    );
    this.modelRouter.register({ provider, config });
if (config.models.length > 0) this.modelRouter.setFallbackChain([...config.models]);
    return { registered: true, models: config.models };
  }

  async probeModels(): Promise<boolean> {
    const results = await Promise.all(
      [...this.providers.values()].map(async (cfg) => {
        const provider = this.modelRouter.resolve(cfg.models[0] ?? cfg.id);
        return provider.healthCheck();
      }),
    );
    return results.some(Boolean);
  }

  async submit(text: string): Promise<SubmitResult> {
    const triggerId = randomUUID();
    const trigger: Trigger = {
      id: triggerId,
      source: "custom_webhook",
      threadId: triggerId,
      payload: text.trim(),
      timestamp: Date.now(),
      meta: {},
    };

    const verdict = await this.filter.filter(trigger);
    if (!verdict.shouldAct) {
      this.store?.append({
        id: randomUUID(),
        type: "trigger",
        label: "Trigger filtered out before routing",
        data: { triggerId, text: text.trim(), action: verdict.action, state: "filtered" },
        timestamp: Date.now(),
      });
      return { triggerId, state: "pending_configuration" };
    }

    this.store?.append({
      id: randomUUID(),
      type: "trigger",
      label: "User task received",
      data: { text: text.trim(), source: "custom_webhook", triggerId, action: verdict.action },
      timestamp: Date.now(),
    });

    if (!this.modelConfigured) return { triggerId, state: "pending_configuration" };

    await this.bus.emit(trigger);
    return { triggerId, state: "queued" };
  }

  cancel(workerId: string): { cancelled: boolean } {
    const worker = this.workers.get(workerId);
    if (!worker) return { cancelled: false };
    worker.abort();
    this.router?.onWorkerError(workerId, "cancelled by user");
    return { cancelled: true };
  }

  async askWatchdog(question: string): Promise<WatchdogQueryResult> {
    const state = this.watchdogState();
    const recent = this.watchdogEvents.slice(-5).map((event) => JSON.stringify(event));
    const answer = [
      `Watchdog has been running for ${Math.round((Date.now() - this.startedAt) / 1000)}s.`,
      `Heartbeat at ${new Date(state.lastHeartbeat).toISOString()}, healthy=${state.healthy}.`,
      state.alerts.length ? `Active alerts: ${state.alerts.join("; ")}` : "No active alerts.",
      recent.length ? `Recent events: ${recent.join(" | ")}` : "No recent events.",
      `Question acknowledged: ${question}`,
    ].join(" ");
    return { question, healthy: state.healthy, alerts: state.alerts, answer, checkedAt: Date.now() };
  }

  watchdogState(): WatchdogState {
    return this.watchdog?.state ?? { intervalMs: 5000, lastHeartbeat: Date.now(), alerts: [], healthy: true };
  }

  listUsage(): UsageRecord[] {
    return this.store?.listUsage() ?? [];
  }

  async readMemory(path: string): Promise<{ path: string; content: string }> {
    if (!path || path.includes("\0")) throw new Error("path is required");
    try {
      const content = await readFile(path, "utf8");
      return { path, content };
    } catch {
      return { path, content: "" };
    }
  }

  spawnWorker(session: WorkerSession): WorkerSessionImpl {
    const provider = this.modelRouter.resolve(session.modelId);
    const worker = new WorkerSessionImpl(session, provider, this.registry, this.bus, {
      modelId: session.modelId,
      maxSteps: 12,
      accessLevel: session.accessLevel,
    });
    this.workers.set(session.id, worker);
    return worker;
  }

  trackLoop(workerId: string, loop: AsyncReActLoop): void {
    this.loops.set(workerId, loop);
  }

  getLoop(workerId: string): AsyncReActLoop | undefined {
    return this.loops.get(workerId);
  }

  snapshot() {
    return {
      connected: true,
      modelConfigured: this.modelConfigured,
      router: this.router?.getSession() ?? {
        id: "", modelId: "", activeWorkers: [], pendingTriggers: [], createdAt: 0, updatedAt: 0,
      },
      workers: [...this.workers.values()].map((w) => ({ id: w.id, status: w.status })),
      tasks: this.store?.listTasks() ?? [],
      flows: this.store?.list() ?? [],
      watchdog: this.watchdogState(),
      usage: this.listUsage(),
      providers: [...this.providers.values()].map((p) => ({ id: p.id, kind: p.kind, baseUrl: p.baseUrl, models: p.models })),
      uptimeMs: Date.now() - this.startedAt,
      message: this.modelConfigured
        ? "Local engine online · model configured"
        : "Local engine online · model not configured",
    };
  }

  close(): void {
    this.watchdog?.stop();
    for (const loop of this.loops.values()) loop.stop();
    this.loops.clear();
    for (const worker of this.workers.values()) worker.abort();
    this.workers.clear();
    this.store?.close();
    (this as { ready: boolean }).ready = false;
  }
}

export function createEngine(): AgentEngine {
  return new AgentEngine();
}
