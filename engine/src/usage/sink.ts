import type { EventStore } from "../storage/index.js";
import { estimateCostBreakdown, type CalendarRules, type PriceTable, type CostBreakdown } from "./pricing.js";
import type { AgentRoleName, ErrorCategory } from "../types.js";

export interface UsageInput {
  requestId?: string;
  sessionId: string;
  workerId?: string;
  taskId?: string;
  role: AgentRoleName;
  providerId: string;
  model: string;
  requestedModel?: string;
  reasoningEffort?: string;
  stream?: boolean;
  stopReason?: string;
  endpoint?: string;
  tokensIn: number;
  tokensOut: number;
  cacheHits: number;
  cacheWriteTokens?: number;
  durationMs?: number;
  toolCalls?: number;
  retries?: number;
  errorCategory?: ErrorCategory;
  at: number;
}

export class UsageRecorder {
  constructor(
    private readonly store: EventStore,
    private table: PriceTable,
    private calendar?: CalendarRules,
  ) {}

  setPriceTable(table: PriceTable): void {
    this.table = table;
  }

  setCalendar(calendar: CalendarRules): void {
    this.calendar = calendar;
  }

  get priceTable(): PriceTable {
    return this.table;
  }

  breakdown(input: {
    providerId: string;
    model: string;
    tokensIn: number;
    tokensOut: number;
    cacheHits: number;
    cacheWriteTokens?: number;
    at: number;
  }): CostBreakdown {
    return estimateCostBreakdown(
      this.table,
      input.providerId,
      input.model,
      {
        input: Math.max(0, input.tokensIn - input.cacheHits),
        output: input.tokensOut,
        cacheRead: input.cacheHits,
        cacheWrite: input.cacheWriteTokens ?? 0,
      },
      input.at,
      this.calendar,
    );
  }

  record(input: UsageInput): void {
    const cost = this.breakdown(input);
    this.store.recordUsage({
      requestId: input.requestId,
      sessionId: input.sessionId,
      workerId: input.workerId,
      taskId: input.taskId,
      role: input.role,
      provider: input.providerId,
      model: input.model,
      requestedModel: input.requestedModel,
      reasoningEffort: input.reasoningEffort,
      stream: input.stream ?? false,
      stopReason: input.stopReason,
      endpoint: input.endpoint,
      tokensIn: input.tokensIn,
      tokensOut: input.tokensOut,
      cacheHits: input.cacheHits,
      cacheWriteTokens: input.cacheWriteTokens ?? 0,
      cost: {
        input: cost.input,
        output: cost.output,
        cacheRead: cost.cacheRead,
        cacheWrite: cost.cacheWrite,
        total: cost.total,
        actual: cost.actual,
      },
      priceMultiplier: cost.multiplier,
      priced: cost.priced,
      durationMs: input.durationMs ?? 0,
      toolCalls: input.toolCalls ?? 0,
      retries: input.retries ?? 0,
      errorCategory: input.errorCategory,
      timestamp: input.at,
    });
  }

  estimate(provider: string, model: string, tokensIn: number, tokensOut: number, cacheHits: number, at: number): number | null {
    const cost = this.breakdown({ providerId: provider, model, tokensIn, tokensOut, cacheHits, at });
    return cost.priced ? cost.actual : null;
  }
}
