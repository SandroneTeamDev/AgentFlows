import type { UsageRecord } from "../types.js";

export type UsageRange = "today" | "7d" | "30d" | "all";
export type UsageGranularity = "hour" | "day" | "week";

export interface UsageBucket {
  tokens: number;
  tokensIn: number;
  tokensOut: number;
  cacheHits: number;
  cacheWriteTokens: number;
  cost: number;
  actualCost: number;
  requests: number;
  failures: number;
}

export interface UsagePoint extends UsageBucket {
  key: string;
  label: string;
}

export interface UsageSessionBucket extends UsageBucket {
  sessionId: string;
  workerId?: string;
  role: string;
  models: string[];
  lastActivity: number;
  avgDurationMs: number;
  toolCalls: number;
}

export interface UsageModelBucket extends UsageBucket {
  model: string;
  provider: string;
  priced: boolean;
}

export interface UsageSummary {
  range: UsageRange;
  granularity: UsageGranularity;
  from: number;
  to: number;
  totals: UsageBucket & { priced: boolean };
  composition: { input: number; output: number; cacheRead: number; cacheWrite: number };
  costBreakdown: { input: number; output: number; cacheRead: number; cacheWrite: number };
  byPoint: UsagePoint[];
  bySession: UsageSessionBucket[];
  byModel: UsageModelBucket[];
  byRole: UsagePoint[];
  byError: { category: string; requests: number }[];
  heatmap: { date: string; requests: number; tokens: number; cost: number }[];
  statsUpdatedAt: number;
}

const DAY_MS = 86_400_000;

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

export function rangeWindow(range: UsageRange, now: number): number {
  switch (range) {
    case "today":
      return startOfDay(now);
    case "7d":
      return startOfDay(now - 6 * DAY_MS);
    case "30d":
      return startOfDay(now - 29 * DAY_MS);
    default:
      return 0;
  }
}

function pointKey(at: number, granularity: UsageGranularity): string {
  const date = new Date(at);
  if (granularity === "hour") {
    const hour = `${date.getHours()}`.padStart(2, "0");
    return `${dayKey(at)} ${hour}:00`;
  }
  if (granularity === "week") {
    const monday = new Date(startOfDay(at));
    const weekday = (monday.getDay() + 6) % 7;
    monday.setDate(monday.getDate() - weekday);
    return dayKey(monday.getTime());
  }
  return dayKey(at);
}

const emptyBucket = (): UsageBucket => ({
  tokens: 0,
  tokensIn: 0,
  tokensOut: 0,
  cacheHits: 0,
  cacheWriteTokens: 0,
  cost: 0,
  actualCost: 0,
  requests: 0,
  failures: 0,
});

const addInto = (bucket: UsageBucket, record: UsageRecord) => {
  bucket.tokensIn += record.tokensIn;
  bucket.tokensOut += record.tokensOut;
  bucket.cacheHits += record.cacheHits;
  bucket.cacheWriteTokens += record.cacheWriteTokens;
  bucket.tokens += record.tokensIn + record.tokensOut + record.cacheHits + record.cacheWriteTokens;
  bucket.cost += record.cost.total;
  bucket.actualCost += record.cost.actual;
  bucket.requests += 1;
  if (record.errorCategory) bucket.failures += 1;
};

export interface SummaryOptions {
  range: UsageRange;
  granularity?: UsageGranularity;
  now?: number;
  costOf?: (record: UsageRecord) => number | null;
}

export function summarizeUsage(records: UsageRecord[], options: SummaryOptions): UsageSummary {
  const now = options.now ?? Date.now();
  const range = options.range;
  const granularity = options.granularity ?? "day";
  const from = rangeWindow(range, now);

  const totals = { ...emptyBucket(), priced: true };
  const points = new Map<string, UsagePoint>();
  const sessions = new Map<string, UsageSessionBucket & { modelSet: Set<string>; durationTotal: number }>();
  const models = new Map<string, UsageModelBucket>();
  const roles = new Map<string, UsagePoint>();
  const errors = new Map<string, number>();
  const days = new Map<string, { requests: number; tokens: number; cost: number }>();
  const costBreakdown = { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 };

  for (const record of records) {
    if (record.timestamp < from || record.timestamp > now) continue;

    if (options.costOf) {
      const estimated = options.costOf(record);
      if (estimated === null) totals.priced = false;
    } else if (!record.priced) {
      totals.priced = false;
    }

    addInto(totals, record);
    costBreakdown.input += record.cost.input;
    costBreakdown.output += record.cost.output;
    costBreakdown.cacheRead += record.cost.cacheRead;
    costBreakdown.cacheWrite += record.cost.cacheWrite;

    const pk = pointKey(record.timestamp, granularity);
    const point = points.get(pk) ?? { key: pk, label: pk, ...emptyBucket() };
    addInto(point, record);
    points.set(pk, point);

    const sessionKey = record.sessionId || "unassigned";
    const session = sessions.get(sessionKey) ?? {
      sessionId: sessionKey,
      workerId: record.workerId,
      role: record.role,
      models: [],
      modelSet: new Set<string>(),
      lastActivity: 0,
      durationTotal: 0,
      avgDurationMs: 0,
      toolCalls: 0,
      ...emptyBucket(),
    };
    addInto(session, record);
    session.modelSet.add(record.model);
    session.lastActivity = Math.max(session.lastActivity, record.timestamp);
    session.durationTotal += record.durationMs;
    session.toolCalls += record.toolCalls;
    sessions.set(sessionKey, session);

    const modelKey = `${record.provider}/${record.model}`;
    const model = models.get(modelKey) ?? { model: record.model, provider: record.provider, priced: record.priced, ...emptyBucket() };
    addInto(model, record);
    if (!record.priced) model.priced = false;
    models.set(modelKey, model);

    const rolePoint = roles.get(record.role) ?? { key: record.role, label: record.role, ...emptyBucket() };
    addInto(rolePoint, record);
    roles.set(record.role, rolePoint);

    if (record.errorCategory) {
      errors.set(record.errorCategory, (errors.get(record.errorCategory) ?? 0) + 1);
    }

    const dk = dayKey(record.timestamp);
    const day = days.get(dk) ?? { requests: 0, tokens: 0, cost: 0 };
    day.requests += 1;
    day.tokens += record.tokensIn + record.tokensOut + record.cacheHits + record.cacheWriteTokens;
    day.cost += record.cost.actual;
    days.set(dk, day);
  }

  const heatmap: { date: string; requests: number; tokens: number; cost: number }[] = [];
  const heatmapStart = startOfDay(now) - 83 * DAY_MS;
  for (let offset = 0; offset < 84; offset += 1) {
    const stamp = heatmapStart + offset * DAY_MS;
    const key = dayKey(stamp);
    const day = days.get(key) ?? { requests: 0, tokens: 0, cost: 0 };
    heatmap.push({ date: key, ...day });
  }

  return {
    range,
    granularity,
    from,
    to: now,
    totals,
    composition: {
      input: totals.tokensIn,
      output: totals.tokensOut,
      cacheRead: totals.cacheHits,
      cacheWrite: totals.cacheWriteTokens,
    },
    costBreakdown,
    byPoint: [...points.values()].sort((a, b) => a.key.localeCompare(b.key)),
    bySession: [...sessions.values()]
      .map((session) => ({
        sessionId: session.sessionId,
        workerId: session.workerId,
        role: session.role,
        models: [...session.modelSet],
        lastActivity: session.lastActivity,
        avgDurationMs: session.requests > 0 ? Math.round(session.durationTotal / session.requests) : 0,
        toolCalls: session.toolCalls,
        tokens: session.tokens,
        tokensIn: session.tokensIn,
        tokensOut: session.tokensOut,
        cacheHits: session.cacheHits,
        cacheWriteTokens: session.cacheWriteTokens,
        cost: session.cost,
        actualCost: session.actualCost,
        requests: session.requests,
        failures: session.failures,
      }))
      .sort((a, b) => b.lastActivity - a.lastActivity),
    byModel: [...models.values()].sort((a, b) => b.tokens - a.tokens),
    byRole: [...roles.values()].sort((a, b) => b.tokens - a.tokens),
    byError: [...errors.entries()].map(([category, requests]) => ({ category, requests })).sort((a, b) => b.requests - a.requests),
    heatmap,
    statsUpdatedAt: now,
  };
}
