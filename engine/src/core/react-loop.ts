import type { WorkerSessionImpl } from "./worker.js";

type LoopCallback = (event: {
  type: "message" | "tool_start" | "tool_done" | "tool_error" | "done" | "error";
  payload: unknown;
}) => void;

export class AsyncReActLoop {
  private worker: WorkerSessionImpl;
  private callback: LoopCallback;
  private active = false;

  constructor(worker: WorkerSessionImpl, callback: LoopCallback) {
    this.worker = worker;
    this.callback = callback;
  }

  start(trigger: unknown): void {
    this.active = true;
    this.callback({ type: "message", payload: { status: "started" } });
    this.worker.execute(trigger as never).then((result) => {
      this.active = false;
      this.callback({ type: "done", payload: result });
    });
  }

  stop(): void {
    this.active = false;
    this.worker.abort();
    this.callback({ type: "message", payload: { status: "stopped" } });
  }

  isActive(): boolean {
    return this.active;
  }

  getToolCalls() {
    return this.worker.getToolCalls();
  }
}
