import type { ToolDefinition } from "./registry.js";

export function readOnlyTools(): ToolDefinition[] {
  return [
    {
      name: "read_file",
      effect: "read",
      description: "Read a file from the worker worktree",
      parameters: { type: "object", properties: { path: { type: "string" } }, required: ["path"] },
      run: async ({ path }: { path: string }) => ({ path, content: `[read-only stub] ${path}`, length: 0 }),
    },
    {
      name: "list_directory",
      effect: "read",
      description: "List directory entries",
      parameters: { type: "object", properties: { path: { type: "string" } }, required: ["path"] },
      run: async ({ path }: { path: string }) => ({ path, entries: [] }),
    },
  ];
}

export function writeTools(): ToolDefinition[] {
  return [
    {
      name: "write_file",
      effect: "write",
      description: "Write a file to the worker worktree",
      parameters: { type: "object", properties: { path: { type: "string" }, content: { type: "string" } }, required: ["path", "content"] },
      run: async ({ path, content }: { path: string; content: string }) => ({ path, bytes: content.length }),
    },
    {
      name: "delete_file",
      effect: "write",
      description: "Delete a file from the worker worktree",
      parameters: { type: "object", properties: { path: { type: "string" } }, required: ["path"] },
      run: async ({ path }: { path: string }) => ({ path, deleted: true }),
    },
  ];
}

export function controlTools(): ToolDefinition[] {
  return [
    {
      name: "start",
      effect: "execute",
      description: "Start a background tool without blocking the foreground conversation",
      parameters: { type: "object", properties: { tool: { type: "string" }, args: { type: "object" } }, required: ["tool", "args"] },
      run: async ({ tool }: { tool: string }) => ({ started: true, tool }),
    },
    {
      name: "stop",
      effect: "execute",
      description: "Stop a background tool by id",
      parameters: { type: "object", properties: { id: { type: "string" } }, required: ["id"] },
      run: async ({ id }: { id: string }) => ({ stopped: true, id }),
    },
  ];
}
