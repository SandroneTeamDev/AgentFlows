import { createRequire } from "node:module";
import type { DatabaseSync as DatabaseSyncType } from "node:sqlite";

const require = createRequire(import.meta.url);
const { DatabaseSync } = require("node:sqlite") as typeof import("node:sqlite");
import type {
  AgentFlowNode,
  SessionState,
  TaskRecord,
  ToolUsageRecord,
  UsageRecord,
  WorkerSession,
} from "../types.js";

export class EventStore {
  private readonly db: DatabaseSyncType;

  constructor(path: string) {
    this.db = new DatabaseSync(path);
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS flow_events (
        id TEXT PRIMARY KEY,
        timestamp INTEGER NOT NULL,
        payload TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS flow_events_timestamp ON flow_events(timestamp);
      CREATE TABLE IF NOT EXISTS tasks (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        status TEXT NOT NULL,
        assigned_worker TEXT,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,
        result TEXT
      );
      CREATE TABLE IF NOT EXISTS usage_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        request_id TEXT,
        session_id TEXT NOT NULL,
        worker_id TEXT,
        task_id TEXT,
        role TEXT NOT NULL,
        provider TEXT NOT NULL,
        model TEXT NOT NULL,
        requested_model TEXT,
        reasoning_effort TEXT,
        stream INTEGER NOT NULL DEFAULT 0,
        stop_reason TEXT,
        endpoint TEXT,
        tokens_in INTEGER NOT NULL,
        tokens_out INTEGER NOT NULL,
        cache_read INTEGER NOT NULL,
        cache_write INTEGER NOT NULL,
        cost_input REAL NOT NULL,
        cost_output REAL NOT NULL,
        cost_cache_read REAL NOT NULL,
        cost_cache_write REAL NOT NULL,
        cost_total REAL NOT NULL,
        cost_actual REAL NOT NULL,
        price_multiplier REAL NOT NULL DEFAULT 1,
        priced INTEGER NOT NULL DEFAULT 1,
        duration_ms INTEGER NOT NULL DEFAULT 0,
        tool_calls INTEGER NOT NULL DEFAULT 0,
        retries INTEGER NOT NULL DEFAULT 0,
        error_category TEXT,
        created_at INTEGER NOT NULL
      );
      CREATE INDEX IF NOT EXISTS usage_logs_created ON usage_logs(created_at);
      CREATE INDEX IF NOT EXISTS usage_logs_session ON usage_logs(session_id, created_at);
      CREATE INDEX IF NOT EXISTS usage_logs_model ON usage_logs(model, created_at);
      CREATE TABLE IF NOT EXISTS tool_usage (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id TEXT NOT NULL,
        worker_id TEXT,
        tool TEXT NOT NULL,
        ok INTEGER NOT NULL,
        duration_ms INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL
      );
      CREATE INDEX IF NOT EXISTS tool_usage_created ON tool_usage(created_at);
      CREATE TABLE IF NOT EXISTS usage_daily (
        day TEXT NOT NULL,
        provider TEXT NOT NULL,
        model TEXT NOT NULL,
        role TEXT NOT NULL,
        requests INTEGER NOT NULL,
        tokens_in INTEGER NOT NULL,
        tokens_out INTEGER NOT NULL,
        cache_read INTEGER NOT NULL,
        cache_write INTEGER NOT NULL,
        cost_total REAL NOT NULL,
        cost_actual REAL NOT NULL,
        PRIMARY KEY (day, provider, model, role)
      );
      CREATE TABLE IF NOT EXISTS sessions (
        id TEXT PRIMARY KEY,
        role TEXT NOT NULL,
        model_id TEXT NOT NULL,
        context TEXT NOT NULL,
        tool_calls TEXT NOT NULL,
        subscriptions TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS workers (
        id TEXT PRIMARY KEY,
        model_id TEXT NOT NULL,
        access_level TEXT NOT NULL,
        worktree TEXT NOT NULL,
        memory_path TEXT NOT NULL,
        subscribed_threads TEXT NOT NULL,
        status TEXT NOT NULL,
        task_id TEXT,
        parent_worker_id TEXT,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );
    `);
  }

  append(node: AgentFlowNode): void {
    this.db.prepare("INSERT INTO flow_events (id, timestamp, payload) VALUES (?, ?, ?)")
      .run(node.id, node.timestamp, JSON.stringify(node));
  }

  list(limit = 200): AgentFlowNode[] {
    const count = Math.min(1000, Math.max(1, Math.trunc(limit)));
    const rows = this.db.prepare("SELECT payload FROM flow_events ORDER BY timestamp DESC, rowid DESC LIMIT ?")
      .all(count) as { payload: string }[];
    return rows.reverse().map((row) => JSON.parse(row.payload) as AgentFlowNode);
  }

  upsertTask(task: TaskRecord): void {
    this.db
      .prepare(
        "INSERT INTO tasks (id, title, status, assigned_worker, created_at, updated_at, result) VALUES (?, ?, ?, ?, ?, ?, ?) " +
          "ON CONFLICT(id) DO UPDATE SET title=excluded.title, status=excluded.status, assigned_worker=excluded.assigned_worker, updated_at=excluded.updated_at, result=excluded.result",
      )
      .run(
        task.id,
        task.title,
        task.status,
        task.assignedWorker ?? null,
        task.createdAt,
        task.updatedAt,
        task.result ? JSON.stringify(task.result) : null,
      );
  }

  listTasks(): TaskRecord[] {
    const rows = this.db
      .prepare("SELECT * FROM tasks ORDER BY created_at DESC")
      .all() as {
        id: string;
        title: string;
        status: string;
        assigned_worker: string | null;
        created_at: number;
        updated_at: number;
        result: string | null;
      }[];
    return rows.map((row) => ({
      id: row.id,
      title: row.title,
      status: row.status as TaskRecord["status"],
      assignedWorker: row.assigned_worker ?? undefined,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      result: row.result ? JSON.parse(row.result) : undefined,
    }));
  }

  recordUsage(record: UsageRecord): void {
    this.db
      .prepare(
        "INSERT INTO usage_logs (request_id, session_id, worker_id, task_id, role, provider, model, requested_model, reasoning_effort, stream, stop_reason, endpoint, tokens_in, tokens_out, cache_read, cache_write, cost_input, cost_output, cost_cache_read, cost_cache_write, cost_total, cost_actual, price_multiplier, priced, duration_ms, tool_calls, retries, error_category, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      )
      .run(
        record.requestId ?? null,
        record.sessionId,
        record.workerId ?? null,
        record.taskId ?? null,
        record.role,
        record.provider,
        record.model,
        record.requestedModel ?? null,
        record.reasoningEffort ?? null,
        record.stream ? 1 : 0,
        record.stopReason ?? null,
        record.endpoint ?? null,
        record.tokensIn,
        record.tokensOut,
        record.cacheHits,
        record.cacheWriteTokens,
        record.cost.input,
        record.cost.output,
        record.cost.cacheRead,
        record.cost.cacheWrite,
        record.cost.total,
        record.cost.actual,
        record.priceMultiplier,
        record.priced ? 1 : 0,
        record.durationMs,
        record.toolCalls,
        record.retries,
        record.errorCategory ?? null,
        record.timestamp,
      );
  }

  listUsage(limit = 200, from = 0, to = Number.MAX_SAFE_INTEGER): UsageRecord[] {
    const count = Math.min(5000, Math.max(1, Math.trunc(limit)));
    const rows = this.db
      .prepare("SELECT * FROM usage_logs WHERE created_at >= ? AND created_at <= ? ORDER BY created_at DESC, id DESC LIMIT ?")
      .all(from, to, count) as unknown as UsageRow[];
    return rows.map(toUsageRecord);
  }

  usageSince(from: number, to = Number.MAX_SAFE_INTEGER, limit = 50_000): UsageRecord[] {
    const rows = this.db
      .prepare("SELECT * FROM usage_logs WHERE created_at >= ? AND created_at <= ? ORDER BY created_at ASC, id ASC LIMIT ?")
      .all(from, to, limit) as unknown as UsageRow[];
    return rows.map(toUsageRecord);
  }

  recordToolUsage(record: ToolUsageRecord): void {
    this.db
      .prepare("INSERT INTO tool_usage (session_id, worker_id, tool, ok, duration_ms, created_at) VALUES (?, ?, ?, ?, ?, ?)")
      .run(record.sessionId, record.workerId ?? null, record.tool, record.ok ? 1 : 0, record.durationMs, record.timestamp);
  }

  toolUsageStats(from: number, to = Number.MAX_SAFE_INTEGER) {
    const rows = this.db
      .prepare(
        "SELECT tool, COUNT(*) AS calls, SUM(CASE WHEN ok = 0 THEN 1 ELSE 0 END) AS failures, " +
          "AVG(duration_ms) AS avg_ms, SUM(duration_ms) AS total_ms FROM tool_usage " +
          "WHERE created_at >= ? AND created_at <= ? GROUP BY tool ORDER BY calls DESC",
      )
      .all(from, to) as { tool: string; calls: number; failures: number; avg_ms: number; total_ms: number }[];
    return rows.map((row) => ({
      tool: row.tool,
      calls: row.calls,
      failures: row.failures ?? 0,
      avgDurationMs: Math.round(row.avg_ms ?? 0),
      totalDurationMs: row.total_ms ?? 0,
    }));
  }

  errorBreakdown(from: number, to = Number.MAX_SAFE_INTEGER) {
    const rows = this.db
      .prepare(
        "SELECT COALESCE(error_category, 'none') AS category, COUNT(*) AS requests FROM usage_logs " +
          "WHERE created_at >= ? AND created_at <= ? GROUP BY category ORDER BY requests DESC",
      )
      .all(from, to) as { category: string; requests: number }[];
    return rows;
  }

  refreshDailyRollup(from: number, to: number): void {
    const rows = this.db
      .prepare("SELECT * FROM usage_logs WHERE created_at >= ? AND created_at <= ?")
      .all(from, to) as unknown as UsageRow[];
    const buckets = new Map<string, { requests: number; tokensIn: number; tokensOut: number; cacheRead: number; cacheWrite: number; costTotal: number; costActual: number }>();
    for (const row of rows) {
      const day = new Date(row.created_at).toISOString().slice(0, 10);
      const key = `${day}|${row.provider}|${row.model}|${row.role}`;
      const bucket = buckets.get(key) ?? { requests: 0, tokensIn: 0, tokensOut: 0, cacheRead: 0, cacheWrite: 0, costTotal: 0, costActual: 0 };
      bucket.requests += 1;
      bucket.tokensIn += row.tokens_in;
      bucket.tokensOut += row.tokens_out;
      bucket.cacheRead += row.cache_read;
      bucket.cacheWrite += row.cache_write;
      bucket.costTotal += row.cost_total;
      bucket.costActual += row.cost_actual;
      buckets.set(key, bucket);
    }
    const insert = this.db.prepare(
      "INSERT INTO usage_daily (day, provider, model, role, requests, tokens_in, tokens_out, cache_read, cache_write, cost_total, cost_actual) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(day, provider, model, role) DO UPDATE SET requests=excluded.requests, tokens_in=excluded.tokens_in, tokens_out=excluded.tokens_out, cache_read=excluded.cache_read, cache_write=excluded.cache_write, cost_total=excluded.cost_total, cost_actual=excluded.cost_actual",
    );
    for (const [key, bucket] of buckets) {
      const [day, provider, model, role] = key.split("|");
      insert.run(day, provider, model, role, bucket.requests, bucket.tokensIn, bucket.tokensOut, bucket.cacheRead, bucket.cacheWrite, bucket.costTotal, bucket.costActual);
    }
  }

  rollupSince(fromDay: string) {
    return this.db
      .prepare("SELECT * FROM usage_daily WHERE day >= ? ORDER BY day ASC")
      .all(fromDay) as {
      day: string;
      provider: string;
      model: string;
      role: string;
      requests: number;
      tokens_in: number;
      tokens_out: number;
      cache_read: number;
      cache_write: number;
      cost_total: number;
      cost_actual: number;
    }[];
  }

  cleanupUsage(beforeTimestamp: number): number {
    const result = this.db.prepare("DELETE FROM usage_logs WHERE created_at < ?").run(beforeTimestamp);
    this.db.prepare("DELETE FROM tool_usage WHERE created_at < ?").run(beforeTimestamp);
    return Number(result.changes ?? 0);
  }

  exportUsage(from: number, to: number, limit = 50_000): UsageRecord[] {
    return this.usageSince(from, to, limit);
  }

  upsertSession(state: SessionState): void {
    this.db
      .prepare(
        "INSERT INTO sessions (id, role, model_id, context, tool_calls, subscriptions, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
          "ON CONFLICT(id) DO UPDATE SET role=excluded.role, model_id=excluded.model_id, context=excluded.context, tool_calls=excluded.tool_calls, subscriptions=excluded.subscriptions, updated_at=excluded.updated_at",
      )
      .run(
        state.id,
        state.role,
        state.modelId,
        JSON.stringify(state.context),
        JSON.stringify(state.toolCalls),
        JSON.stringify(state.subscriptions),
        state.createdAt,
        state.updatedAt,
      );
  }

  getSession(id: string): SessionState | undefined {
    const row = this.db.prepare("SELECT * FROM sessions WHERE id = ?").get(id) as
      | {
          id: string;
          role: string;
          model_id: string;
          context: string;
          tool_calls: string;
          subscriptions: string;
          created_at: number;
          updated_at: number;
        }
      | undefined;
    if (!row) return undefined;
    return {
      id: row.id,
      role: row.role as SessionState["role"],
      modelId: row.model_id,
      context: JSON.parse(row.context),
      toolCalls: JSON.parse(row.tool_calls),
      subscriptions: JSON.parse(row.subscriptions),
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  listSessions(limit = 50): SessionState[] {
    const count = Math.min(500, Math.max(1, Math.trunc(limit)));
    const rows = this.db
      .prepare("SELECT * FROM sessions ORDER BY updated_at DESC LIMIT ?")
      .all(count) as {
      id: string;
      role: string;
      model_id: string;
      context: string;
      tool_calls: string;
      subscriptions: string;
      created_at: number;
      updated_at: number;
    }[];
    return rows.map((row) => ({
      id: row.id,
      role: row.role as SessionState["role"],
      modelId: row.model_id,
      context: JSON.parse(row.context),
      toolCalls: JSON.parse(row.tool_calls),
      subscriptions: JSON.parse(row.subscriptions),
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    }));
  }

  upsertWorker(session: WorkerSession): void {
    this.db
      .prepare(
        "INSERT INTO workers (id, model_id, access_level, worktree, memory_path, subscribed_threads, status, task_id, parent_worker_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
          "ON CONFLICT(id) DO UPDATE SET model_id=excluded.model_id, access_level=excluded.access_level, worktree=excluded.worktree, memory_path=excluded.memory_path, subscribed_threads=excluded.subscribed_threads, status=excluded.status, task_id=excluded.task_id, parent_worker_id=excluded.parent_worker_id, updated_at=excluded.updated_at",
      )
      .run(
        session.id,
        session.modelId,
        session.accessLevel,
        session.worktree,
        session.memoryPath,
        JSON.stringify(session.subscribedThreads),
        session.status,
        session.taskId ?? null,
        session.parentWorkerId ?? null,
        session.createdAt,
        session.updatedAt,
      );
  }

  close(): void {
    this.db.close();
  }
}
interface UsageRow {
  id: number;
  request_id: string | null;
  session_id: string;
  worker_id: string | null;
  task_id: string | null;
  role: string;
  provider: string;
  model: string;
  requested_model: string | null;
  reasoning_effort: string | null;
  stream: number;
  stop_reason: string | null;
  endpoint: string | null;
  tokens_in: number;
  tokens_out: number;
  cache_read: number;
  cache_write: number;
  cost_input: number;
  cost_output: number;
  cost_cache_read: number;
  cost_cache_write: number;
  cost_total: number;
  cost_actual: number;
  price_multiplier: number;
  priced: number;
  duration_ms: number;
  tool_calls: number;
  retries: number;
  error_category: string | null;
  created_at: number;
}

function toUsageRecord(row: UsageRow): UsageRecord {
  return {
    id: row.id,
    requestId: row.request_id ?? undefined,
    sessionId: row.session_id,
    workerId: row.worker_id ?? undefined,
    taskId: row.task_id ?? undefined,
    role: row.role as UsageRecord["role"],
    provider: row.provider,
    model: row.model,
    requestedModel: row.requested_model ?? undefined,
    reasoningEffort: row.reasoning_effort ?? undefined,
    stream: row.stream === 1,
    stopReason: row.stop_reason ?? undefined,
    endpoint: row.endpoint ?? undefined,
    tokensIn: row.tokens_in,
    tokensOut: row.tokens_out,
    cacheHits: row.cache_read,
    cacheWriteTokens: row.cache_write,
    cost: {
      input: row.cost_input,
      output: row.cost_output,
      cacheRead: row.cost_cache_read,
      cacheWrite: row.cost_cache_write,
      total: row.cost_total,
      actual: row.cost_actual,
    },
    priceMultiplier: row.price_multiplier,
    priced: row.priced === 1,
    durationMs: row.duration_ms,
    toolCalls: row.tool_calls,
    retries: row.retries,
    errorCategory: (row.error_category ?? undefined) as UsageRecord["errorCategory"],
    timestamp: row.created_at,
  };
}
