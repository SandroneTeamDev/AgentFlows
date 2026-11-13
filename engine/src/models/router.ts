import type { ModelProvider } from "./base.js";
import type { ProviderConfig } from "../types.js";

interface RouterEntry {
  provider: ModelProvider;
  config: ProviderConfig;
}

export class ModelRouter {
  private providers = new Map<string, RouterEntry>();
  private fallbackChain: string[] = [];
  private usageCounter = new Map<string, number>();

  register(entry: RouterEntry): void {
    this.providers.set(entry.provider.id, entry);
  }

  unregister(providerId: string): void {
    this.providers.delete(providerId);
  }

  resolve(modelId: string): ModelProvider {
    for (const entry of this.providers.values()) {
      if (entry.config.models.includes(modelId)) {
        return entry.provider;
      }
    }
    if (this.providers.size === 1) {
      return this.providers.values().next().value!.provider;
    }
    throw new Error(`no provider found for model ${modelId}`);
  }

  resolveStrongest(): string {
    for (const entry of this.providers.values()) {
      if (entry.config.models.length > 0) {
        return entry.config.models[entry.config.models.length - 1];
      }
    }
    return "unknown";
  }

  setFallbackChain(modelIds: string[]): void {
    this.fallbackChain = modelIds;
  }

  recordUsage(providerId: string, cost: number): void {
    this.usageCounter.set(
      providerId,
      (this.usageCounter.get(providerId) ?? 0) + cost,
    );
  }

  getUsage(providerId?: string): number | number[] {
    if (providerId) return this.usageCounter.get(providerId) ?? 0;
    return [...this.usageCounter.values()];
  }

  async routeWithFallback(
    modelId: string,
    messages: { role: string; content: unknown }[],
  ): Promise<{ provider: ModelProvider; response: import("./base.js").ModelResponse }> {
    const chain = this.fallbackChain.length > 0 ? [modelId, ...this.fallbackChain] : [modelId];
    let lastError: unknown;
    for (const mid of chain) {
      try {
        const provider = this.resolve(mid);
        const response = await provider.invoke(mid, messages);
        return { provider, response };
      } catch (err) {
        lastError = err;
      }
    }
    throw lastError;
  }

  listProviders(): ProviderConfig[] {
    return [...this.providers.values()].map((e) => e.config);
  }
}
