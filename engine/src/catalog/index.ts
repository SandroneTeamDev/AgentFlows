import { readFile } from "node:fs/promises";

export type ModelType = "chat" | "image" | "embedding" | "unknown";

export interface CatalogCost {
  input: number;
  output: number;
  cacheRead?: number;
  cacheWrite?: number;
}

export interface CatalogModel {
  id: string;
  name?: string;
  type: ModelType;
  context?: number;
  maxOutput?: number;
  input?: string[];
  output?: string[];
  reasoning?: boolean;
  reasoningEfforts?: string[];
  tools?: boolean;
  structuredOutput?: boolean;
  streamUsage?: boolean;
  cost?: CatalogCost;
}

export interface CatalogProvider {
  name: string;
  models: Record<string, CatalogModel>;
}

export interface Catalog {
  generatedAt: string;
  providers: Record<string, CatalogProvider>;
}

export interface LoadedCatalog {
  catalog: Catalog;
  source: "file" | "empty";
  loadedAt: number;
  error?: string;
}

const EMPTY: Catalog = { generatedAt: "", providers: {} };

export async function loadCatalog(path: string): Promise<LoadedCatalog> {
  try {
    const raw = JSON.parse(await readFile(path, "utf8")) as Catalog;
    if (!raw || typeof raw !== "object" || !raw.providers) throw new Error("catalog has no providers");
    return { catalog: raw, source: "file", loadedAt: Date.now() };
  } catch (err) {
    return {
      catalog: EMPTY,
      source: "empty",
      loadedAt: Date.now(),
      error: err instanceof Error ? err.message : String(err),
    };
  }
}

export function findModel(
  catalog: Catalog,
  provider: string,
  modelId: string,
): CatalogModel | undefined {
  const direct = catalog.providers[provider]?.models[modelId];
  if (direct) return direct;

  const lowerProvider = provider.toLowerCase();
  const lowerModel = modelId.toLowerCase();

  for (const [providerId, entry] of Object.entries(catalog.providers)) {
    if (providerId.toLowerCase() !== lowerProvider) continue;
    const match = Object.keys(entry.models).find((key) => key.toLowerCase() === lowerModel);
    if (match) return entry.models[match];
  }

  for (const entry of Object.values(catalog.providers)) {
    const match = Object.keys(entry.models).find((key) => key.toLowerCase() === lowerModel);
    if (match) return entry.models[match];
  }

  return undefined;
}

export interface CatalogQuery {
  type?: ModelType;
  provider?: string;
  query?: string;
  reasoning?: boolean;
  minContext?: number;
  limit?: number;
}

export function searchModels(catalog: Catalog, options: CatalogQuery = {}) {
  const needle = options.query?.trim().toLowerCase() ?? "";
  const results: { provider: string; providerName: string; model: CatalogModel }[] = [];
  for (const [providerId, entry] of Object.entries(catalog.providers)) {
    if (options.provider && providerId !== options.provider) continue;
    for (const model of Object.values(entry.models)) {
      if (options.type && model.type !== options.type) continue;
      if (options.reasoning === true && !model.reasoning) continue;
      if (options.minContext !== undefined && (model.context ?? 0) < options.minContext) continue;
      if (needle) {
        const haystack = [providerId, entry.name, model.id, model.name ?? ""].join(" ").toLowerCase();
        if (!haystack.includes(needle)) continue;
      }
      results.push({ provider: providerId, providerName: entry.name, model });
    }
  }
  results.sort((a, b) => (b.model.context ?? 0) - (a.model.context ?? 0));
  const limit = Math.min(500, Math.max(1, options.limit ?? 100));
  return results.slice(0, limit);
}

export function supportedEfforts(catalog: Catalog, provider: string, modelId: string): string[] {
  return findModel(catalog, provider, modelId)?.reasoningEfforts ?? [];
}

export function acceptsEffort(catalog: Catalog, provider: string, modelId: string, effort: string): boolean {
  const efforts = supportedEfforts(catalog, provider, modelId);
  if (efforts.length === 0) return true;
  return efforts.includes(effort);
}

export function acceptsInput(catalog: Catalog, provider: string, modelId: string, modality: string): boolean {
  const model = findModel(catalog, provider, modelId);
  if (!model?.input) return true;
  return model.input.includes(modality);
}
