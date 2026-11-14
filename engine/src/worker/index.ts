import { randomUUID } from "node:crypto";
import type { EventBus } from "../event-bus/index.js";
import type { Trigger, WorkerSession, ToolCall, AgentFlowNode } from "../types.js";
import type { ModelProvider } from "../models/base.js";
import type { ToolRegistry } from "../tools/registry.js";

interface WorkerConfig {
  modelId: string;
  maxSteps: number;
  accessLevel: "full" | "plan" | "view" | "workspace_write";
  onUsage?: (input: {
    sessionId: string;
    providerId: string;
    model: string;
    tokensIn: number;
    tokensOut: number;
    cacheHits: number;
    at: number;
  }) => void;
}

interface ModelMessage {
  role: string;
  content: unknown;
  tool_calls?: { id: string; type: string; function: { name: string; arguments: string } }[];
  tool_call_id?: string;
}

export class WorkerSessionImpl {
  private session: WorkerSession;
  private provider: ModelProvider;
  private toolRegistry: ToolRegistry;
  private bus: EventBus;
  private config: WorkerConfig;
  private toolCalls: ToolCall[] = [];
  private flowNodes: AgentFlowNode[] = [];
  private abortController: AbortController;
  private running = false;
  private activeTools = new Map<string, AbortController>();

  constructor(
    session: WorkerSession,
    provider: ModelProvider,
    toolRegistry: ToolRegistry,
    bus: EventBus,
    config: WorkerConfig,
  ) {
    this.session = session;
    this.provider = provider;
    this.toolRegistry = toolRegistry;
    this.bus = bus;
    this.config = config;
    this.abortController = new AbortController();
    this.bus.registerWorker(session);
  }

  get id(): string {
    return this.session.id;
  }

  get status(): string {
    return this.session.status;
  }

  async execute(trigger: Trigger): Promise<unknown> {
    this.session.status = "running";
    this.running = true;
    let step = 0;

    const systemPrompt = this.buildSystemPrompt();
    const userPrompt = this.buildUserPrompt(trigger);
    const messages: ModelMessage[] = [
      { role: "system", content: systemPrompt },
      { role: "user", content: userPrompt },
    ];

    while (step < this.config.maxSteps && this.running) {
      const response = await this.provider.invoke(
        this.config.modelId,
        messages,
        this.toolRegistry.schemas(),
        this.abortController.signal,
      );

      this.config.onUsage?.({
        sessionId: this.session.id,
        providerId: this.provider.id,
        model: this.config.modelId,
        tokensIn: response.tokensIn,
        tokensOut: response.tokensOut,
        cacheHits: response.cacheReadTokens ?? 0,
        at: Date.now(),
      });

      if (response.toolCalls && response.toolCalls.length > 0) {
        messages.push({
          role: "assistant",
          content: response.text,
          tool_calls: response.toolCalls.map((call) => ({
            id: call.id,
            type: "function",
            function: { name: call.name, arguments: JSON.stringify(call.args) },
          })),
        });

        for (const tc of response.toolCalls) {
          const toolCall: ToolCall = {
            id: tc.id,
            name: tc.name,
            args: tc.args,
            status: "pending",
            startedAt: Date.now(),
          };
          this.toolCalls.push(toolCall);

          this.flowNodes.push({
            id: randomUUID(),
            type: "tool_call",
            label: `${tc.name}(${JSON.stringify(tc.args).slice(0, 80)})`,
            data: { toolCallId: toolCall.id },
            timestamp: Date.now(),
          });

          const result = await this.executeTool(tc, this.abortController.signal);
          toolCall.status = result.ok ? "done" : "error";
          toolCall.result = result.ok ? result.value : undefined;
          toolCall.error = result.ok ? undefined : String(result.value);
          toolCall.finishedAt = Date.now();

          this.flowNodes.push({
            id: randomUUID(),
            type: result.ok ? "worker_start" : "error",
            label: result.ok ? `${tc.name} ok` : `${tc.name} failed`,
            data: { toolCallId: toolCall.id, duration: toolCall.finishedAt - toolCall.startedAt },
            timestamp: Date.now(),
          });

          messages.push({
            role: "tool",
            tool_call_id: tc.id,
            content: result.ok ? JSON.stringify(result.value) : `Error: ${result.value}`,
          } as ModelMessage);
        }
        step++;
        continue;
      }

      if (response.stopReason === "end_turn") {
        this.session.status = "done";
        this.running = false;
        return response.text;
      }

      step++;
    }

    this.session.status = "done";
    this.running = false;
    return "max steps reached";
  }

  startBackground(toolCallId: string, toolName: string, args: Record<string, unknown>): string {
    const ctrl = new AbortController();
    this.activeTools.set(toolCallId, ctrl);
    const runId = randomUUID();
    this.executeToolAsync(toolCallId, toolName, args, ctrl.signal).catch(() => {});
    return runId;
  }

  stopBackground(toolCallId: string): void {
    this.activeTools.get(toolCallId)?.abort();
    this.activeTools.delete(toolCallId);
  }

  abort(): void {
    this.running = false;
    this.abortController.abort();
    for (const ctrl of this.activeTools.values()) {
      ctrl.abort();
    }
    this.activeTools.clear();
  }

  getToolCalls(): ToolCall[] {
    return this.toolCalls;
  }

  getFlowNodes(): AgentFlowNode[] {
    return this.flowNodes;
  }

  private async executeTool(
    tc: { id: string; name: string; args: Record<string, unknown> },
    signal: AbortSignal,
  ): Promise<{ ok: boolean; value: unknown }> {
    if (tc.name === "start") {
      const runId = this.startBackground(tc.id, String(tc.args.tool), tc.args.args as Record<string, unknown>);
      return { ok: true, value: { started: true, runId } };
    }
    if (tc.name === "stop") {
      this.stopBackground(String(tc.args.id));
      return { ok: true, value: { stopped: true } };
    }
    try {
      const result = await this.toolRegistry.execute(tc.name, tc.args, signal, {
        worktree: this.session.worktree,
      });
      return { ok: true, value: result };
    } catch (err) {
      return { ok: false, value: err instanceof Error ? err.message : String(err) };
    }
  }

  private async executeToolAsync(
    toolCallId: string,
    toolName: string,
    args: Record<string, unknown>,
    signal: AbortSignal,
  ): Promise<void> {
    try {
      const result = await this.toolRegistry.execute(toolName, args, signal, {
        worktree: this.session.worktree,
      });
      const tc = this.toolCalls.find((t) => t.id === toolCallId);
      if (tc) {
        tc.status = "done";
        tc.result = result;
        tc.finishedAt = Date.now();
      }
      this.bus.emit({
        id: randomUUID(),
        source: "local_terminal",
        threadId: this.session.subscribedThreads[0] ?? "",
        payload: { type: "tool_completed", toolCallId, result },
        timestamp: Date.now(),
        meta: { workerId: this.session.id },
      });
    } catch (err) {
      const tc = this.toolCalls.find((t) => t.id === toolCallId);
      if (tc) {
        tc.status = "error";
        tc.error = err instanceof Error ? err.message : String(err);
        tc.finishedAt = Date.now();
      }
    }
  }

  private buildSystemPrompt(): string {
    const toolNames = this.toolRegistry.names();
    return [
      `You are a worker agent with "${this.config.accessLevel}" access level.`,
      `Available tools: ${toolNames.join(", ")}.`,
      `Working directory: ${this.session.worktree}.`,
      `Memory file: ${this.session.memoryPath}.`,
      `You report results to the router, not directly to the user.`,
    ].join("\n");
  }

  private buildUserPrompt(trigger: Trigger): string {
    return JSON.stringify(
      {
        task: trigger.payload,
        source: trigger.source,
        thread: trigger.threadId,
      },
      null,
      2,
    );
  }
}
