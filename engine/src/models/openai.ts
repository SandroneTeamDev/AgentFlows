import type { ModelProvider, ModelResponse, ToolSchema, ToolCallRequest } from "./base.js";

export class OpenAICompatibleProvider implements ModelProvider {
  readonly id: string;
  readonly kind = "openai";
  private baseUrl: string;
  private apiKey?: string;
  private extraHeaders: Record<string, string>;
  private extraBody: Record<string, unknown>;

  constructor(
    id: string,
    baseUrl: string,
    apiKey?: string,
    extraHeaders: Record<string, string> = {},
    extraBody: Record<string, unknown> = {},
  ) {
    this.id = id;
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.apiKey = apiKey;
    this.extraHeaders = extraHeaders;
    this.extraBody = extraBody;
  }

  async invoke(
    model: string,
    messages: { role: string; content: unknown; tool_calls?: unknown[]; tool_call_id?: string }[],
    tools?: ToolSchema[],
    signal?: AbortSignal,
  ): Promise<ModelResponse> {
    const body: Record<string, unknown> = {
      model,
      messages,
      ...this.extraBody,
    };
    if (tools && tools.length > 0) {
      body.tools = tools.map((t) => ({
        type: "function",
        function: {
          name: t.name,
          description: t.description,
          parameters: t.parameters,
        },
      }));
    }

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...this.extraHeaders,
    };
    if (this.apiKey) {
      headers["Authorization"] = `Bearer ${this.apiKey}`;
    }

    const res = await fetch(`${this.baseUrl}/chat/completions`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
      signal,
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`provider ${this.id} error ${res.status}: ${text}`);
    }

    const data = (await res.json()) as {
      choices: {
        message: {
          content: string | null;
          tool_calls?: { id: string; function: { name: string; arguments: string } }[];
        };
        finish_reason: string;
      }[];
      usage: {
        prompt_tokens: number;
        completion_tokens: number;
        prompt_tokens_details?: { cached_tokens?: number };
      };
    };

    const msg = data.choices[0]?.message;
    const toolCalls: ToolCallRequest[] | undefined =
      msg?.tool_calls?.map((tc) => ({
        id: tc.id,
        name: tc.function.name,
        args: JSON.parse(tc.function.arguments || "{}"),
      }));

    return {
      text: msg?.content ?? "",
      toolCalls: toolCalls && toolCalls.length > 0 ? toolCalls : undefined,
      stopReason: this.mapStopReason(data.choices[0]?.finish_reason),
      tokensIn: data.usage?.prompt_tokens ?? 0,
      tokensOut: data.usage?.completion_tokens ?? 0,
      cacheReadTokens: data.usage?.prompt_tokens_details?.cached_tokens,
      raw: data,
    };
  }

  async listModels(): Promise<string[]> {
    const headers: Record<string, string> = { ...this.extraHeaders };
    if (this.apiKey) {
      headers["Authorization"] = `Bearer ${this.apiKey}`;
    }
    const res = await fetch(`${this.baseUrl}/models`, { headers });
    if (!res.ok) return [];
    const data = (await res.json()) as { data: { id: string }[] };
    return data.data.map((m) => m.id);
  }

  async healthCheck(): Promise<boolean> {
    try {
      await this.listModels();
      return true;
    } catch {
      return false;
    }
  }

  private mapStopReason(reason: string | undefined): ModelResponse["stopReason"] {
    switch (reason) {
      case "tool_calls":
      case "function_call":
        return "tool_use";
      case "length":
        return "max_tokens";
      default:
        return "end_turn";
    }
  }
}
