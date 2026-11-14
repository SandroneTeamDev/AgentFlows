import type { UsageRecord } from "../types.js";

export type UsageRange = "today" | "7d" | "30d" | "all";

export interface UsageBucket {
  tokens: number;
  tokensIn: number;
  tokensOut: number;
  cacheHits: number;
  cost: number;
  requests: number;
}

export interface UsageDay extends UsageBucket {
  date: string;
}

export interface UsageSessionBucket extends UsageBucket {
  sessionId: string;
  models: string[];
  lastActivity: number;
}

export interface UsageSummary {
  range: UsageRange;
  from: number;
  to: number;
  totals: UsageBucket & { priced: boolean };
  composition: { input: number; output: number; cache: number };
  byDay: UsageDay[];
  bySession: UsageSessionBucket[];
  byModel: ({ model: string; provider: string } & UsageBucket)[];
  heatmap: UsageDay[];
}

const DAY_MS = 86_400_000;

export function rangeWindow(range: UsageRange, now: number): number {
  switch (range) {
    case "today": {
      const date = new Date(now);
      date.setHours(0, 0, 0, 0);
      return date.getTime();
    }
    case "7d":
      return startOfDay(now - 6 * DAY_MS);
    case "30d":
      return startOfDay(now - 29 * DAY_MS);
    default:
      return 0;
  }
}

export function startOfDay(at: number): number {
  const date = new Date(at);
  date.setHours(0, 0, 0, 0);
  return date.getTime();
}

export function dayKey(at: number): string {
  const date = new Date(at);
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

const emptyBucket = (): UsageBucket => ({
  tokens: 0,
  tokensIn: 0,
  tokensOut: 0,
  cacheHits: 0,
  cost: 0,
  requests: 0,
});

const addInto = (bucket: UsageBucket, tokensIn: number, tokensOut: number, cacheHits: number, cost: number) => {
  bucket.tokensIn += tokensIn;
  bucket.tokensOut += tokensOut;
  bucket.cacheHits += cacheHits;
  bucket.tokens += tokensIn + tokensOut;
  bucket.cost += cost;
  bucket.requests += 1;
};

export function summarizeUsage(
  records: UsageRecord[],
  range: UsageRange,
  now: number,
  costOf: (record: UsageRecord) => number | null,
): UsageSummary {
  const from = rangeWindow(range, now);
  const totals = { ...emptyBucket(), priced: true };
  const byDay = new Map<string, UsageDay>();
  const bySession = new Map<string, UsageSessionBucket & { modelSet: Set<string> }>();
  const byModel = new Map<string, { model: string; provider: string } & UsageBucket>();

  for (const record of records) {
    if (record.timestamp < from || record.timestamp > now) continue;
    const estimated = costOf(record);
    const cost = estimated ?? record.cost;
    if (estimated === null) totals.priced = false;

    addInto(totals, record.tokensIn, record.tokensOut, record.cacheHits, cost);

    const key = dayKey(record.timestamp);
    const day = byDay.get(key) ?? { date: key, ...emptyBucket() };
    addInto(day, record.tokensIn, record.tokensOut, record.cacheHits, cost);
    byDay.set(key, day);

    const sessionKey = record.sessionId || "unassigned";
    const session = bySession.get(sessionKey) ?? {
      sessionId: sessionKey,
      models: [],
      modelSet: new Set<string>(),
      lastActivity: 0,
      ...emptyBucket(),
    };
    addInto(session, record.tokensIn, record.tokensOut, record.cacheHits, cost);
    session.modelSet.add(record.model);
    session.lastActivity = Math.max(session.lastActivity, record.timestamp);
    bySession.set(sessionKey, session);

    const modelKey = `${record.provider}/${record.model}`;
    const model = byModel.get(modelKey) ?? { model: record.model, provider: record.provider, ...emptyBucket() };
    addInto(model, record.tokensIn, record.tokensOut, record.cacheHits, cost);
    byModel.set(modelKey, model);
  }

  const heatmapStart = startOfDay(now) - 83 * DAY_MS;
  const heatmap: UsageDay[] = [];
  for (let offset = 0; offset < 84; offset += 1) {
    const stamp = heatmapStart + offset * DAY_MS;
    const key = dayKey(stamp);
    heatmap.push({ date: key, ...(byDay.get(key) ?? emptyBucket()) });
  }

  const sortedDays = [...byDay.values()].sort((a, b) => a.date.localeCompare(b.date));

  return {
    range,
    from,
    to: now,
    totals,
    composition: {
      input: totals.tokensIn,
      output: totals.tokensOut,
      cache: totals.cacheHits,
    },
    byDay: sortedDays,
    bySession: [...bySession.values()]
      .map((session) => ({
        sessionId: session.sessionId,
        models: [...session.modelSet],
        lastActivity: session.lastActivity,
        tokens: session.tokens,
        tokensIn: session.tokensIn,
        tokensOut: session.tokensOut,
        cacheHits: session.cacheHits,
        cost: session.cost,
        requests: session.requests,
      }))
      .sort((a, b) => b.lastActivity - a.lastActivity),
    byModel: [...byModel.values()].sort((a, b) => b.tokens - a.tokens),
    heatmap,
  };
}
