import type { ModelProvider, ModelResponse, ToolSchema, ToolCallRequest } from "./base.js";

type ChatMessage = { role: string; content: unknown; tool_calls?: unknown[]; tool_call_id?: string };

interface Part {
  text?: string;
  functionCall?: { name?: string; args?: Record<string, unknown> };
  functionResponse?: { name?: string; response?: Record<string, unknown> };
}

export class GeminiProvider implements ModelProvider {
  readonly id: string;
  readonly kind = "gemini";
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
    messages: ChatMessage[],
    tools?: ToolSchema[],
    signal?: AbortSignal,
  ): Promise<ModelResponse> {
    const system = messages
      .filter((message) => message.role === "system")
      .map((message) => this.asText(message.content))
      .filter(Boolean)
      .join("\n\n");

    const body: Record<string, unknown> = {
      contents: this.convert(messages.filter((message) => message.role !== "system")),
      ...this.extraBody,
    };
    if (system) body.systemInstruction = { parts: [{ text: system }] };
    if (tools && tools.length > 0) {
      body.tools = [
        {
          functionDeclarations: tools.map((tool) => ({
            name: tool.name,
            description: tool.description,
            parameters: tool.parameters,
          })),
        },
      ];
    }

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...this.extraHeaders,
    };
    if (this.apiKey) headers["x-goog-api-key"] = this.apiKey;

    const res = await fetch(`${this.baseUrl}/v1beta/models/${model}:generateContent`, {
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
      candidates?: {
        content?: { parts?: Part[] };
        finishReason?: string;
      }[];
      usageMetadata?: {
        promptTokenCount?: number;
        candidatesTokenCount?: number;
        cachedContentTokenCount?: number;
      };
    };

    const candidate = data.candidates?.[0];
    const parts = candidate?.content?.parts ?? [];
    const text = parts
      .filter((part) => typeof part.text === "string")
      .map((part) => part.text ?? "")
      .join("");
    const toolCalls: ToolCallRequest[] = parts
      .filter((part) => part.functionCall)
      .map((part, index) => ({
        id: `${part.functionCall?.name ?? "call"}-${index}-${Date.now()}`,
        name: part.functionCall?.name ?? "",
        args: part.functionCall?.args ?? {},
      }));

    return {
      text,
      toolCalls: toolCalls.length > 0 ? toolCalls : undefined,
      stopReason: this.mapStopReason(candidate?.finishReason),
      tokensIn: data.usageMetadata?.promptTokenCount ?? 0,
      tokensOut: data.usageMetadata?.candidatesTokenCount ?? 0,
      cacheReadTokens: data.usageMetadata?.cachedContentTokenCount,
      raw: data,
    };
  }

  async listModels(): Promise<string[]> {
    const headers: Record<string, string> = { ...this.extraHeaders };
    if (this.apiKey) headers["x-goog-api-key"] = this.apiKey;
    const res = await fetch(`${this.baseUrl}/v1beta/models`, { headers });
    if (!res.ok) return [];
    const data = (await res.json()) as { models?: { name?: string }[] };
    return (data.models ?? [])
      .map((entry) => (entry.name ?? "").replace(/^models\//, ""))
      .filter(Boolean);
  }

  async healthCheck(): Promise<boolean> {
    try {
      await this.listModels();
      return true;
    } catch {
      return false;
    }
  }

  private convert(messages: ChatMessage[]): { role: string; parts: Part[] }[] {
    const out: { role: string; parts: Part[] }[] = [];
    const pendingCalls = new Map<string, string>();

    for (const message of messages) {
      const role = message.role === "assistant" ? "model" : "user";
      const parts: Part[] = [];

      if (role === "model") {
        const text = this.asText(message.content);
        if (text) parts.push({ text });
        for (const call of (message.tool_calls ?? []) as {
          id?: string;
          function?: { name?: string; arguments?: string };
        }[]) {
          const name = call.function?.name ?? "";
          if (call.id) pendingCalls.set(call.id, name);
          parts.push({ functionCall: { name, args: this.parseArgs(call.function?.arguments) } });
        }
      } else if (message.role === "tool") {
        const name = pendingCalls.get(message.tool_call_id ?? "") ?? "tool";
        parts.push({
          functionResponse: {
            name,
            response: { result: this.asText(message.content) },
          },
        });
      } else {
        const text = this.asText(message.content);
        if (text) parts.push({ text });
      }

      if (parts.length === 0) continue;
      const last = out[out.length - 1];
      if (last && last.role === role) {
        last.parts.push(...parts);
      } else {
        out.push({ role, parts });
      }
    }
    return out;
  }

  private asText(content: unknown): string {
    if (typeof content === "string") return content;
    if (content == null) return "";
    if (Array.isArray(content)) {
      return content
        .map((part) => {
          if (typeof part === "string") return part;
          if (part && typeof part === "object" && "text" in part) {
            return String((part as { text?: unknown }).text ?? "");
          }
          return "";
        })
        .join("");
    }
    return JSON.stringify(content);
  }

  private parseArgs(raw: string | undefined): Record<string, unknown> {
    if (!raw) return {};
    try {
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === "object" ? (parsed as Record<string, unknown>) : {};
    } catch {
      return {};
    }
  }

  private mapStopReason(reason: string | undefined): ModelResponse["stopReason"] {
    switch (reason) {
      case "STOP":
        return "end_turn";
      case "MAX_TOKENS":
        return "max_tokens";
      default:
        return "stop";
    }
  }
}
