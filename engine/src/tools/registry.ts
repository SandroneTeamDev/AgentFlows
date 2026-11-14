import type { AccessLevel } from "../types.js";
import type { ToolSchema } from "../models/base.js";

export interface ToolContext {
  worktree: string;
}

export interface ToolDefinition extends ToolSchema {
  effect: "read" | "write" | "execute";
  run(args: Record<string, unknown>, signal?: AbortSignal, ctx?: ToolContext): Promise<unknown>;
}

export class ToolRegistry {
  private tools = new Map<string, ToolDefinition>();

  constructor(private readonly accessLevel: AccessLevel = "view") {}

  register(tool: ToolDefinition): void {
    if (this.tools.has(tool.name)) throw new Error(`Tool already registered: ${tool.name}`);
    this.tools.set(tool.name, tool);
  }

  names(): string[] {
    return this.schemas().map((tool) => tool.name);
  }

  schemas(): ToolSchema[] {
    return [...this.tools.values()]
      .filter((tool) => this.allowed(tool))
      .map(({ name, description, parameters }) => ({ name, description, parameters }));
  }

  async execute(
    name: string,
    args: Record<string, unknown>,
    signal?: AbortSignal,
    ctx?: ToolContext,
  ): Promise<unknown> {
    signal?.throwIfAborted();
    const tool = this.tools.get(name);
    if (!tool) throw new Error(`Unknown tool: ${name}`);
    if (!this.allowed(tool)) throw new Error(`Tool denied by ${this.accessLevel} access: ${name}`);
    return tool.run(args, signal, ctx);
  }

  allowed(tool: ToolDefinition): boolean {
    if (this.accessLevel === "full") return true;
    if (this.accessLevel === "plan") return false;
    if (tool.effect === "read") return true;
    return this.accessLevel === "workspace_write" && tool.effect === "write";
  }
}
