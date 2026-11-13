export interface SpeculativeTask {
  id: string;
  signal: string;
  description: string;
  value: unknown;
  executed: boolean;
}

export class SpeculativeExecutor {
  private tasks = new Map<string, SpeculativeTask>();
  private readonly maxTasks: number;

  constructor(maxTasks = 64) {
    this.maxTasks = maxTasks;
  }

  schedule(id: string, signal: string, description: string, run: () => unknown): void {
    if (this.tasks.size >= this.maxTasks) return;
    let value: unknown;
    try {
      value = run();
    } catch {
      value = { skipped: true, reason: "speculative execution failed" };
    }
    this.tasks.set(id, { id, signal, description, value, executed: true });
  }

  collect(id: string): SpeculativeTask | undefined {
    return this.tasks.get(id);
  }

  discard(id: string): void {
    this.tasks.delete(id);
  }

  list(): SpeculativeTask[] {
    return [...this.tasks.values()];
  }

  clear(): void {
    this.tasks.clear();
  }
}
