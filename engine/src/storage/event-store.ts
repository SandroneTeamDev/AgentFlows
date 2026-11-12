import Database from "better-sqlite3";
import type { AgentFlowNode } from "../core/types.js";

export class EventStore {
  private readonly db: Database.Database;

  constructor(path: string) {
    this.db = new Database(path);
    this.db.pragma("journal_mode = WAL");
    this.db.exec("CREATE TABLE IF NOT EXISTS flow_events (id TEXT PRIMARY KEY, timestamp INTEGER NOT NULL, payload TEXT NOT NULL)");
    this.db.exec("CREATE INDEX IF NOT EXISTS flow_events_timestamp ON flow_events(timestamp)");
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

  close(): void {
    this.db.close();
  }
}
