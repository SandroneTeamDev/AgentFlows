import { OpenAICompatibleProvider } from "./openai.js";

const DEFAULT_BASE_URLS: Record<string, string> = {
  ollama: "http://127.0.0.1:11434/v1",
  llama_cpp: "http://127.0.0.1:8080/v1",
  mnn: "http://127.0.0.1:8080/v1",
  custom: "http://127.0.0.1:1234/v1",
};

export class LocalProvider extends OpenAICompatibleProvider {
  constructor(
    id: string,
    kind: string,
    baseUrl?: string,
    apiKey?: string,
    extraHeaders: Record<string, string> = {},
    extraBody: Record<string, unknown> = {},
  ) {
    super(id, baseUrl && baseUrl.length > 0 ? baseUrl : (DEFAULT_BASE_URLS[kind] ?? DEFAULT_BASE_URLS.custom), apiKey, extraHeaders, extraBody);
  }

  static defaultBaseUrl(kind: string): string {
    return DEFAULT_BASE_URLS[kind] ?? DEFAULT_BASE_URLS.custom;
  }
}
