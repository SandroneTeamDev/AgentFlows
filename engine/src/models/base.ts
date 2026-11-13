export interface ToolCallRequest {
  id: string;
  name: string;
  args: Record<string, unknown>;
}

export interface ModelResponse {
  text: string;
  toolCalls?: ToolCallRequest[];
  stopReason: "end_turn" | "tool_use" | "max_tokens" | "stop";
  tokensIn: number;
  tokensOut: number;
  cacheReadTokens?: number;
  raw?: unknown;
}

export interface ToolSchema {
  name: string;
  description: string;
  parameters: Record<string, unknown>;
}

export interface ModelProvider {
  readonly id: string;
  readonly kind: string;
  invoke(
    model: string,
    messages: { role: string; content: unknown; tool_calls?: unknown[]; tool_call_id?: string }[],
    tools?: ToolSchema[],
    signal?: AbortSignal,
  ): Promise<ModelResponse>;
  listModels(): Promise<string[]>;
  healthCheck(): Promise<boolean>;
}
