import { createRequire } from "node:module";
import type { DatabaseSync as DatabaseSyncType } from "node:sqlite";

const require = createRequire(import.meta.url);
const { DatabaseSync } = require("node:sqlite") as typeof import("node:sqlite");
import type { AgentFlowNode, SessionState, TaskRecord, UsageRecord, WorkerSession } from "../types.js";

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
      CREATE TABLE IF NOT EXISTS usage (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id TEXT NOT NULL,
        model TEXT NOT NULL,
        provider TEXT NOT NULL,
        tokens_in INTEGER NOT NULL,
        tokens_out INTEGER NOT NULL,
        cache_hits INTEGER NOT NULL,
        cost REAL NOT NULL,
        timestamp INTEGER NOT NULL
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
        "INSERT INTO usage (session_id, model, provider, tokens_in, tokens_out, cache_hits, cost, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
      )
      .run(
        record.sessionId,
        record.model,
        record.provider,
        record.tokensIn,
        record.tokensOut,
        record.cacheHits,
        record.cost,
        record.timestamp,
      );
  }

  listUsage(limit = 100): UsageRecord[] {
    const count = Math.min(1000, Math.max(1, Math.trunc(limit)));
    const rows = this.db
      .prepare("SELECT * FROM usage ORDER BY timestamp DESC LIMIT ?")
      .all(count) as {
        session_id: string;
        model: string;
        provider: string;
        tokens_in: number;
        tokens_out: number;
        cache_hits: number;
        cost: number;
        timestamp: number;
      }[];
    return rows.map((row) => ({
      sessionId: row.session_id,
      model: row.model,
      provider: row.provider,
      tokensIn: row.tokens_in,
      tokensOut: row.tokens_out,
      cacheHits: row.cache_hits,
      cost: row.cost,
      timestamp: row.timestamp,
    }));
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
