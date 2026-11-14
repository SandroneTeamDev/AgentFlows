import type { ModelProvider, ModelResponse, ToolSchema, ToolCallRequest } from "./base.js";

type ChatMessage = { role: string; content: unknown; tool_calls?: unknown[]; tool_call_id?: string };

interface Block {
  type: string;
  text?: string;
  id?: string;
  name?: string;
  input?: Record<string, unknown>;
  tool_use_id?: string;
  content?: unknown;
}

export class AnthropicProvider implements ModelProvider {
  readonly id: string;
  readonly kind = "anthropic";
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
      .filter((m) => m.role === "system")
      .map((m) => this.asText(m.content))
      .filter(Boolean)
      .join("\n\n");

    const body: Record<string, unknown> = {
      model,
      max_tokens: 8192,
      messages: this.convert(messages.filter((m) => m.role !== "system")),
      ...this.extraBody,
    };
    if (system) body.system = system;
    if (tools && tools.length > 0) {
      body.tools = tools.map((tool) => ({
        name: tool.name,
        description: tool.description,
        input_schema: tool.parameters,
      }));
    }

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "anthropic-version": "2023-06-01",
      ...this.extraHeaders,
    };
    if (this.apiKey) headers["x-api-key"] = this.apiKey;

    const res = await fetch(`${this.baseUrl}/v1/messages`, {
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
      content?: Block[];
      stop_reason?: string;
      usage?: {
        input_tokens?: number;
        output_tokens?: number;
        cache_read_input_tokens?: number;
      };
    };

    const blocks = data.content ?? [];
    const text = blocks
      .filter((block) => block.type === "text")
      .map((block) => block.text ?? "")
      .join("");
    const toolCalls: ToolCallRequest[] = blocks
      .filter((block) => block.type === "tool_use")
      .map((block) => ({
        id: block.id ?? "",
        name: block.name ?? "",
        args: (block.input ?? {}) as Record<string, unknown>,
      }));

    return {
      text,
      toolCalls: toolCalls.length > 0 ? toolCalls : undefined,
      stopReason: this.mapStopReason(data.stop_reason),
      tokensIn: data.usage?.input_tokens ?? 0,
      tokensOut: data.usage?.output_tokens ?? 0,
      cacheReadTokens: data.usage?.cache_read_input_tokens,
      raw: data,
    };
  }

  async listModels(): Promise<string[]> {
    const headers: Record<string, string> = {
      "anthropic-version": "2023-06-01",
      ...this.extraHeaders,
    };
    if (this.apiKey) headers["x-api-key"] = this.apiKey;
    const res = await fetch(`${this.baseUrl}/v1/models`, { headers });
    if (!res.ok) return [];
    const data = (await res.json()) as { data?: { id: string }[] };
    return (data.data ?? []).map((entry) => entry.id);
  }

  async healthCheck(): Promise<boolean> {
    try {
      await this.listModels();
      return true;
    } catch {
      return false;
    }
  }

  private convert(messages: ChatMessage[]): { role: string; content: Block[] }[] {
    const out: { role: string; content: Block[] }[] = [];
    for (const message of messages) {
      const role = message.role === "assistant" ? "assistant" : "user";
      const blocks: Block[] = [];

      if (role === "assistant") {
        const text = this.asText(message.content);
        if (text) blocks.push({ type: "text", text });
        for (const call of (message.tool_calls ?? []) as {
          id?: string;
          function?: { name?: string; arguments?: string };
        }[]) {
          blocks.push({
            type: "tool_use",
            id: call.id ?? "",
            name: call.function?.name ?? "",
            input: this.parseArgs(call.function?.arguments),
          });
        }
      } else if (message.role === "tool") {
        blocks.push({
          type: "tool_result",
          tool_use_id: message.tool_call_id ?? "",
          content: this.asText(message.content),
        });
      } else {
        const text = this.asText(message.content);
        if (text) blocks.push({ type: "text", text });
      }

      if (blocks.length === 0) continue;
      const last = out[out.length - 1];
      if (last && last.role === role) {
        last.content.push(...blocks);
      } else {
        out.push({ role, content: blocks });
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
      case "tool_use":
        return "tool_use";
      case "max_tokens":
        return "max_tokens";
      case "end_turn":
        return "end_turn";
      default:
        return "stop";
    }
  }
}
