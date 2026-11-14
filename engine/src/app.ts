import { randomUUID } from "node:crypto";
import { mkdir, readFile } from "node:fs/promises";
import { EventBus } from "./event-bus/index.js";
import { TwoLevelFilter } from "./filter/index.js";
import { InlineSpillManager } from "./memory/index.js";
import { SpeculativeExecutor } from "./specul/index.js";
import { Watchdog, type WatchdogEvent } from "./watchdog/index.js";
import type { WatchdogState } from "./types.js";
import { EventStore } from "./storage/index.js";
import { createProvider } from "./models/factory.js";
import { ModelRouter } from "./models/router.js";
import { RouterLayer } from "./router/index.js";
import { ToolRegistry } from "./tools/registry.js";
import { builtinTools } from "./tools/builtin-tools.js";
import { AsyncReActLoop } from "./loop.js";
import { WorkerSessionImpl } from "./worker/index.js";
import { loadPriceTable, type LoadedPrices } from "./usage/pricing.js";
import { loadCatalog, searchModels, findModel, type Catalog, type ModelType } from "./catalog/index.js";
import { isMakeupWorkday, isOffDay, loadHolidays, type HolidayCalendar } from "./usage/holidays.js";
import { summarizeUsage, type UsageRange, type UsageSummary } from "./usage/aggregate.js";
import { UsageRecorder } from "./usage/sink.js";
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
  source: "model" | "heuristic";
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
  private recorder?: UsageRecorder;
  private prices?: LoadedPrices;
  private catalog: Catalog = { generatedAt: "", providers: {} };
  private catalogSource: "file" | "empty" = "empty";
  private catalogError?: string;
  private holidays: HolidayCalendar = { source: "", years: {} };

  readonly ready = false;

  async initialize(dataDir: string): Promise<void> {
    this.dataDir = dataDir;
    await mkdir(dataDir, { recursive: true });
    this.store = new EventStore(`${dataDir}/state.sqlite`);

    const pricePath = process.env.AGENTFLOWS_PRICES
      ? new URL(`file://${process.env.AGENTFLOWS_PRICES}`)
      : new URL("../pricing/models-dev.json", import.meta.url);
    this.prices = await loadPriceTable(pricePath.pathname);

    const holidayPath = process.env.AGENTFLOWS_HOLIDAYS
      ? new URL(`file://${process.env.AGENTFLOWS_HOLIDAYS}`)
      : new URL("../holidays/cn.json", import.meta.url);
    this.holidays = await loadHolidays(holidayPath.pathname);

    this.recorder = new UsageRecorder(this.store, this.prices.table, {
      isOffDay: (at) => isOffDay(this.holidays, at),
      isMakeupWorkday: (at) => isMakeupWorkday(this.holidays, at),
    });

    const catalogPath = process.env.AGENTFLOWS_CATALOG
      ? new URL(`file://${process.env.AGENTFLOWS_CATALOG}`)
      : new URL("../catalog/models-dev.json", import.meta.url);
    const loadedCatalog = await loadCatalog(catalogPath.pathname);
    this.catalog = loadedCatalog.catalog;
    this.catalogSource = loadedCatalog.source;
    this.catalogError = loadedCatalog.error;

    this.providers.set(DEFAULT_PROVIDER.id, DEFAULT_PROVIDER);
    const openai = createProvider(DEFAULT_PROVIDER);
    this.modelRouter.register({ provider: openai, config: DEFAULT_PROVIDER });
    this.modelRouter.setFallbackChain([...DEFAULT_PROVIDER.models]);

    this.router = new RouterLayer(
      this.bus,
      this.modelRouter,
      {
        modelId: DEFAULT_PROVIDER.models[0] ?? "",
        batchSize: 8,
        maxConcurrency: 4,
        onUsage: (input) => this.recorder?.record(input),
        onWorkerSpawned: (session) => {
          this.spawnWorker(session);
        },
      },
      new InlineSpillManager(),
    );

    this.registry = new ToolRegistry("full");
    for (const tool of builtinTools()) {
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
      this.dispatchToWorkers(trigger);
    });

    (this as { ready: boolean }).ready = true;
  }

  get modelConfigured(): boolean {
    return [...this.providers.values()].some((p) => p.models.length > 0);
  }

  registerProvider(config: ProviderConfig): { registered: boolean; models: string[] } {
    if (!config.id || !config.baseUrl) throw new Error("id and baseUrl are required");
    if (config.models.length === 0) {
      const known = this.catalog.providers[config.id];
      if (known) config = { ...config, models: Object.keys(known.models) };
    }
    this.providers.set(config.id, config);
    const provider = createProvider(config);
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
    const heuristic = [
      `Watchdog has been running for ${Math.round((Date.now() - this.startedAt) / 1000)}s.`,
      `Heartbeat at ${new Date(state.lastHeartbeat).toISOString()}, healthy=${state.healthy}.`,
      state.alerts.length ? `Active alerts: ${state.alerts.join("; ")}` : "No active alerts.",
      recent.length ? `Recent events: ${recent.join(" | ")}` : "No recent events.",
      `Question acknowledged: ${question}`,
    ].join(" ");

    const modelId = this.router?.getSession().modelId || DEFAULT_PROVIDER.models[0];
    if (!modelId) {
      return { question, healthy: state.healthy, alerts: state.alerts, answer: heuristic, source: "heuristic", checkedAt: Date.now() };
    }

    try {
      const provider = this.modelRouter.resolve(modelId);
      const workers = [...this.workers.values()].map((worker) => ({
        id: worker.id,
        status: worker.status,
        toolCalls: worker.getToolCalls().length,
      }));
      const response = await provider.invoke(modelId, [
        {
          role: "system",
          content:
            "You are the watchdog control loop of a local multi-agent runtime. " +
            "Judge whether running workers still make real progress and answer the operator. " +
            "Be concrete and short. Never invent worker ids or numbers that are not in the context.",
        },
        {
          role: "user",
          content: JSON.stringify({ question, healthy: state.healthy, alerts: state.alerts, workers, recent }, null, 2),
        },
      ]);

      this.recorder?.record({
        sessionId: "watchdog",
        providerId: provider.id,
        model: modelId,
        tokensIn: response.tokensIn,
        tokensOut: response.tokensOut,
        cacheHits: response.cacheReadTokens ?? 0,
        at: Date.now(),
      });

      const answer = response.text.trim();
      if (!answer) {
        return { question, healthy: state.healthy, alerts: state.alerts, answer: heuristic, source: "heuristic", checkedAt: Date.now() };
      }
      return { question, healthy: state.healthy, alerts: state.alerts, answer, source: "model", checkedAt: Date.now() };
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      return {
        question,
        healthy: state.healthy,
        alerts: state.alerts,
        answer: `${heuristic} (model judgement unavailable: ${reason})`,
        source: "heuristic",
        checkedAt: Date.now(),
      };
    }
  }

  watchdogState(): WatchdogState {
    return this.watchdog?.state ?? { intervalMs: 5000, lastHeartbeat: Date.now(), alerts: [], healthy: true };
  }

  listUsage(): UsageRecord[] {
    return this.store?.listUsage() ?? [];
  }

  listSessions() {
    return this.store?.listSessions() ?? [];
  }

  getSession(id: string) {
    return this.store?.getSession(id);
  }

  usageSummary(range: UsageRange = "7d"): UsageSummary {
    const records = this.store?.listUsage(1000) ?? [];
    return summarizeUsage(records, range, Date.now(), (record) =>
      this.recorder
        ? this.recorder.estimate(
            record.provider,
            record.model,
            record.tokensIn,
            record.tokensOut,
            record.cacheHits,
            record.timestamp,
          )
        : record.cost,
    );
  }

  searchCatalog(query: {
    type?: ModelType;
    provider?: string;
    query?: string;
    reasoning?: boolean;
    minContext?: number;
    limit?: number;
  }) {
    return searchModels(this.catalog, query);
  }

  modelCapabilities(provider: string, modelId: string) {
    const model = findModel(this.catalog, provider, modelId);
    if (!model) return { known: false as const, provider, model: modelId };
    return { known: true as const, provider, model: modelId, capabilities: model };
  }

  catalogStatus() {
    return {
      source: this.catalogSource,
      generatedAt: this.catalog.generatedAt,
      providers: Object.keys(this.catalog.providers).length,
      models: Object.values(this.catalog.providers).reduce(
        (total, entry) => total + Object.keys(entry.models).length,
        0,
      ),
      error: this.catalogError,
    };
  }

  priceStatus(): { source: string; models: number; loadedAt: number; error?: string } {
    return {
      source: this.prices?.source ?? "empty",
      models: Object.keys(this.prices?.table ?? {}).length,
      loadedAt: this.prices?.loadedAt ?? 0,
      error: this.prices?.error,
    };
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
    const registry = new ToolRegistry(session.accessLevel);
    for (const tool of builtinTools()) registry.register(tool);
    const worker = new WorkerSessionImpl(session, provider, registry, this.bus, {
      modelId: session.modelId,
      maxSteps: 12,
      accessLevel: session.accessLevel,
      onUsage: (input) => this.recorder?.record(input),
    });
    this.workers.set(session.id, worker);
    return worker;
  }

  private dispatchToWorkers(trigger: Trigger): void {
    for (const worker of this.workers.values()) {
      if (this.loops.get(worker.id)?.isActive()) continue;
      const session = this.bus.getWorker(worker.id);
      if (!session) continue;
      if (!session.subscribedThreads.includes(trigger.threadId)) continue;
      const loop = new AsyncReActLoop(worker, (event) => this.onLoopEvent(worker.id, event));
      this.trackLoop(worker.id, loop);
      loop.start(trigger);
    }
  }

  private onLoopEvent(
    workerId: string,
    event: { type: string; payload: unknown },
  ): void {
    if (event.type === "done") {
      const worker = this.workers.get(workerId);
      const session = this.bus.getWorker(workerId);
      if (worker && session) {
        this.store?.upsertSession({
          id: workerId,
          role: "worker",
          modelId: session.modelId,
          context: [{ result: event.payload }],
          toolCalls: worker.getToolCalls(),
          subscriptions: session.subscribedThreads,
          createdAt: session.createdAt,
          updatedAt: Date.now(),
        });
      }
      this.router?.onWorkerComplete(workerId, event.payload);
      this.store?.append({
        id: randomUUID(),
        type: "worker_done",
        label: "Worker finished",
        data: { workerId },
        timestamp: Date.now(),
      });
      return;
    }
    if (event.type === "error") {
      const message = String(event.payload);
      this.router?.onWorkerError(workerId, message);
      this.store?.append({
        id: randomUUID(),
        type: "error",
        label: message,
        data: { workerId },
        timestamp: Date.now(),
      });
    }
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
