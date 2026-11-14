import { readFile } from "node:fs/promises";

export interface PeakRule {
  days?: string[];
  windows: [string, string][];
  factor: number;
}

export interface PriceEntry {
  input: number;
  output: number;
  cacheRead?: number;
  cacheWrite?: number;
  peak?: PeakRule;
}

export interface PriceTable {
  [key: string]: PriceEntry;
}

export interface LoadedPrices {
  table: PriceTable;
  source: "file" | "empty";
  loadedAt: number;
  error?: string;
}

export interface CostBreakdown {
  input: number;
  output: number;
  cacheRead: number;
  cacheWrite: number;
  total: number;
  actual: number;
  multiplier: number;
  priced: boolean;
}

const DAY_NAMES = ["sun", "mon", "tue", "wed", "thu", "fri", "sat"];

const toMinutes = (value: string): number => {
  const [hours, minutes] = value.split(":");
  return Number(hours) * 60 + Number(minutes ?? 0);
};

export interface CalendarRules {
  isOffDay(at: number): boolean;
  isMakeupWorkday(at: number): boolean;
}

export function multiplierFor(entry: PriceEntry, at: number, calendar?: CalendarRules): number {
  const rule = entry.peak;
  if (!rule || rule.windows.length === 0) return 1;

  const date = new Date(at);
  const day = DAY_NAMES[date.getUTCDay()];

  if (rule.days && rule.days.length > 0) {
    const namedDay = rule.days.includes(day);
    const makeup = calendar?.isMakeupWorkday(at) ?? false;
    const holiday = calendar?.isOffDay(at) ?? false;
    if (holiday) return 1;
    if (!namedDay && !makeup) return 1;
  }

  const minutes = date.getUTCHours() * 60 + date.getUTCMinutes();
  for (const [start, end] of rule.windows) {
    if (minutes >= toMinutes(start) && minutes < toMinutes(end)) return rule.factor;
  }
  return 1;
}

export function resolvePrice(
  table: PriceTable,
  provider: string,
  model: string,
): PriceEntry | undefined {
  const exact = table[`${provider}/${model}`];
  if (exact) return exact;
  const byModel = table[model];
  if (byModel) return byModel;
  const lower = model.toLowerCase();
  const match = Object.keys(table).find((key) => key.toLowerCase() === lower);
  return match ? table[match] : undefined;
}

const EMPTY_BREAKDOWN: CostBreakdown = {
  input: 0,
  output: 0,
  cacheRead: 0,
  cacheWrite: 0,
  total: 0,
  actual: 0,
  multiplier: 1,
  priced: false,
};

export function estimateCostBreakdown(
  table: PriceTable,
  provider: string,
  model: string,
  tokens: { input: number; output: number; cacheRead: number; cacheWrite: number },
  at: number,
  calendar?: CalendarRules,
): CostBreakdown {
  const entry = resolvePrice(table, provider, model);
  if (!entry) return EMPTY_BREAKDOWN;

  const inputTokens = Math.max(0, tokens.input);
  const outputTokens = Math.max(0, tokens.output);
  const cacheReadTokens = Math.max(0, tokens.cacheRead);
  const cacheWriteTokens = Math.max(0, tokens.cacheWrite);
  const cacheReadRate = entry.cacheRead ?? entry.input;
  const cacheWriteRate = entry.cacheWrite ?? entry.input;

  const input = (inputTokens / 1_000_000) * entry.input;
  const output = (outputTokens / 1_000_000) * entry.output;
  const cacheRead = (cacheReadTokens / 1_000_000) * cacheReadRate;
  const cacheWrite = (cacheWriteTokens / 1_000_000) * cacheWriteRate;
  const total = input + output + cacheRead + cacheWrite;
  const multiplier = multiplierFor(entry, at, calendar);

  return {
    input,
    output,
    cacheRead,
    cacheWrite,
    total,
    actual: total * multiplier,
    multiplier,
    priced: true,
  };
}

export function estimateCost(
  table: PriceTable,
  provider: string,
  model: string,
  tokensIn: number,
  tokensOut: number,
  cacheHits: number,
  at: number,
): number | null {
  const breakdown = estimateCostBreakdown(
    table,
    provider,
    model,
    { input: Math.max(0, tokensIn - cacheHits), output: tokensOut, cacheRead: cacheHits, cacheWrite: 0 },
    at,
  );
  return breakdown.priced ? breakdown.actual : null;
}

export function normalizePriceTable(raw: unknown): PriceTable {
  if (!raw || typeof raw !== "object") return {};
  const out: PriceTable = {};
  for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
    if (!value || typeof value !== "object") continue;
    const entry = value as Record<string, unknown>;
    const input = Number(entry.input);
    const output = Number(entry.output);
    if (!Number.isFinite(input) || !Number.isFinite(output)) continue;
    const cacheRead = Number(entry.cacheRead);
    const cacheWrite = Number(entry.cacheWrite);
    const peak = normalizePeak(entry.peak);
    out[key] = {
      input,
      output,
      ...(Number.isFinite(cacheRead) ? { cacheRead } : {}),
      ...(Number.isFinite(cacheWrite) ? { cacheWrite } : {}),
      ...(peak ? { peak } : {}),
    };
  }
  return out;
}

function normalizePeak(raw: unknown): PeakRule | undefined {
  if (!raw || typeof raw !== "object") return undefined;
  const rule = raw as Record<string, unknown>;
  const factor = Number(rule.factor);
  if (!Number.isFinite(factor)) return undefined;
  const windows = Array.isArray(rule.windows) ? rule.windows : [];
  const parsed: [string, string][] = [];
  for (const window of windows) {
    if (Array.isArray(window) && window.length === 2) {
      const [start, end] = window;
      if (typeof start === "string" && typeof end === "string") parsed.push([start, end]);
    }
  }
  if (parsed.length === 0) return undefined;
  const days = Array.isArray(rule.days)
    ? rule.days.filter((day): day is string => typeof day === "string")
    : undefined;
  return { windows: parsed, factor, ...(days && days.length > 0 ? { days } : {}) };
}

export async function loadPriceTable(path: string): Promise<LoadedPrices> {
  try {
    const text = await readFile(path, "utf8");
    const table = normalizePriceTable(JSON.parse(text));
    return { table, source: "file", loadedAt: Date.now() };
  } catch (err) {
    return {
      table: {},
      source: "empty",
      loadedAt: Date.now(),
      error: err instanceof Error ? err.message : String(err),
    };
  }
}
