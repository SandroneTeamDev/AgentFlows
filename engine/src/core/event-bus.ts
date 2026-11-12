import { randomUUID } from "node:crypto";
import type { Trigger, Thread, WorkerSession } from "./types.js";

type Handler = (trigger: Trigger) => void | Promise<void>;

export class EventBus {
  private handlers = new Map<string, Set<Handler>>();
  private threads = new Map<string, Thread>();
  private workers = new Map<string, WorkerSession>();
  private globalHandler?: Handler;

  on(workerId: string, handler: Handler): void {
    let set = this.handlers.get(workerId);
    if (!set) {
      set = new Set();
      this.handlers.set(workerId, set);
    }
    set.add(handler);
  }

  off(workerId: string, handler: Handler): void {
    this.handlers.get(workerId)?.delete(handler);
  }

  onGlobal(handler: Handler): void {
    this.globalHandler = handler;
  }

  offGlobal(): void {
    this.globalHandler = undefined;
  }

  createThread(label: string): Thread {
    const thread: Thread = {
      id: randomUUID(),
      label,
      triggers: [],
      subscribers: [],
      active: true,
    };
    this.threads.set(thread.id, thread);
    return thread;
  }

  subscribe(threadId: string, workerId: string): void {
    const thread = this.threads.get(threadId);
    if (!thread) return;
    if (!thread.subscribers.includes(workerId)) {
      thread.subscribers.push(workerId);
    }
  }

  unsubscribe(threadId: string, workerId: string): void {
    const thread = this.threads.get(threadId);
    if (!thread) return;
    thread.subscribers = thread.subscribers.filter((s) => s !== workerId);
  }

  registerWorker(session: WorkerSession): void {
    this.workers.set(session.id, session);
  }

  unregisterWorker(workerId: string): void {
    this.workers.delete(workerId);
  }

  async emit(trigger: Trigger): Promise<void> {
    const thread = this.threads.get(trigger.threadId);
    if (thread) {
      thread.triggers.push(trigger);
    }

    const subscriberSet = new Set<string>();
    if (thread) {
      for (const sub of thread.subscribers) {
        if (this.workers.has(sub)) {
          subscriberSet.add(sub);
        }
      }
    }

    if (subscriberSet.size > 0) {
      await Promise.all(
        [...subscriberSet].map(async (wid) => {
          const set = this.handlers.get(wid);
          if (set) {
            for (const h of set) {
              await h(trigger);
            }
          }
        }),
      );
    }

    if (subscriberSet.size === 0 && this.globalHandler) {
      await this.globalHandler(trigger);
    }
  }

  getThread(threadId: string): Thread | undefined {
    return this.threads.get(threadId);
  }

  listThreads(): Thread[] {
    return [...this.threads.values()];
  }

  getWorker(workerId: string): WorkerSession | undefined {
    return this.workers.get(workerId);
  }

  listWorkers(): WorkerSession[] {
    return [...this.workers.values()];
  }
}
