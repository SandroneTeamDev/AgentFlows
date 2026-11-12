import { randomUUID } from "node:crypto";
import type { AgentFlowNode, WatchdogState } from "./types.js";

type WatchdogEvent =
  | { type: "alert"; message: string; workerId?: string }
  | { type: "heartbeat"; ok: boolean }
  | { type: "fix_applied"; description: string };

export interface WatchdogCallback {
  (event: WatchdogEvent): void;
}

export class Watchdog {
  private state: WatchdogState;
  private timer: ReturnType<typeof setInterval> | undefined;
  private callback: WatchdogCallback;
  private lastWorkerTimestamps = new Map<string, number>();
  private staleThresholdMs: number;

  constructor(callback: WatchdogCallback, intervalMs: number, staleThresholdMs: number = 60_000) {
    this.callback = callback;
    this.staleThresholdMs = staleThresholdMs;
    this.state = {
      intervalMs,
      lastHeartbeat: Date.now(),
      alerts: [],
      healthy: true,
    };
  }

  start(): void {
    this.stop();
    this.timer = setInterval(() => this.tick(), this.state.intervalMs);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  recordWorkerActivity(workerId: string): void {
    this.lastWorkerTimestamps.set(workerId, Date.now());
  }

  get state(): WatchdogState {
    return this.state;
  }

  private tick(): void {
    this.state.lastHeartbeat = Date.now();
    const now = Date.now();

    for (const [workerId, lastActive] of this.lastWorkerTimestamps) {
      if (now - lastActive > this.staleThresholdMs) {
        const alert = `worker ${workerId} stalled for ${Math.round((now - lastActive) / 1000)}s`;
        this.state.alerts.push(alert);
        this.state.healthy = false;
        this.callback({ type: "alert", message: alert, workerId });
      }
    }

    if (this.state.alerts.length === 0) {
      this.state.healthy = true;
    }

    this.callback({ type: "heartbeat", ok: this.state.healthy });
  }

  applyFix(description: string): void {
    this.state.alerts = [];
    this.state.healthy = true;
    this.callback({ type: "fix_applied", description });
  }
}
