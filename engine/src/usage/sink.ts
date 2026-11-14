import type { EventStore } from "../storage/index.js";
import { estimateCost, type PriceTable } from "./pricing.js";

export interface UsageInput {
  sessionId: string;
  providerId: string;
  model: string;
  tokensIn: number;
  tokensOut: number;
  cacheHits: number;
  at: number;
}

export class UsageRecorder {
  constructor(
    private readonly store: EventStore,
    private table: PriceTable,
  ) {}

  setPriceTable(table: PriceTable): void {
    this.table = table;
  }

  record(input: UsageInput): void {
    const cost =
      estimateCost(
        this.table,
        input.providerId,
        input.model,
        input.tokensIn,
        input.tokensOut,
        input.cacheHits,
        input.at,
      ) ?? 0;
    this.store.recordUsage({
      sessionId: input.sessionId,
      model: input.model,
      provider: input.providerId,
      tokensIn: input.tokensIn,
      tokensOut: input.tokensOut,
      cacheHits: input.cacheHits,
      cost,
      timestamp: input.at,
    });
  }

  estimate(provider: string, model: string, tokensIn: number, tokensOut: number, cacheHits: number, at: number): number | null {
    return estimateCost(this.table, provider, model, tokensIn, tokensOut, cacheHits, at);
  }
}
