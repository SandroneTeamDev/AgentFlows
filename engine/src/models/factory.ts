import type { ModelProvider } from "./base.js";
import type { ProviderConfig } from "../types.js";
import { OpenAICompatibleProvider } from "./openai.js";
import { AnthropicProvider } from "./anthropic.js";
import { GeminiProvider } from "./gemini.js";
import { LocalProvider } from "./local.js";

export function createProvider(config: ProviderConfig): ModelProvider {
  const headers = config.headers ?? {};
  const body = config.body ?? {};
  switch (config.kind) {
    case "anthropic":
      return new AnthropicProvider(config.id, config.baseUrl, config.apiKey, headers, body);
    case "gemini":
      return new GeminiProvider(config.id, config.baseUrl, config.apiKey, headers, body);
    case "ollama":
    case "llama_cpp":
    case "mnn":
    case "custom":
      return new LocalProvider(config.id, config.kind, config.baseUrl, config.apiKey, headers, body);
    default:
      return new OpenAICompatibleProvider(config.id, config.baseUrl, config.apiKey, headers, body);
  }
}
