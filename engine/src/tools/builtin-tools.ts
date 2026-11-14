import { mkdir, readFile, readdir, rm, stat, writeFile } from "node:fs/promises";
import { dirname, isAbsolute, relative, resolve, sep } from "node:path";
import { execa } from "execa";
import type { ToolContext, ToolDefinition } from "./registry.js";

const MAX_READ_BYTES = 256 * 1024;
const MAX_OUTPUT_CHARS = 64 * 1024;
const DEFAULT_TIMEOUT_MS = 120_000;
const MAX_TIMEOUT_MS = 600_000;

function requireRoot(ctx?: ToolContext): string {
  if (!ctx || !ctx.worktree) throw new Error("no worktree bound to this tool call");
  return resolve(ctx.worktree);
}

function resolveInside(root: string, target: unknown): string {
  const raw = typeof target === "string" ? target : "";
  if (!raw || raw.includes("\0")) throw new Error("path is required");
  const candidate = isAbsolute(raw) ? resolve(raw) : resolve(root, raw);
  if (candidate !== root && !candidate.startsWith(root + sep)) {
    throw new Error(`path escapes worktree: ${raw}`);
  }
  return candidate;
}

function truncate(value: string): string {
  return value.length > MAX_OUTPUT_CHARS ? `${value.slice(0, MAX_OUTPUT_CHARS)}\n[truncated ${value.length - MAX_OUTPUT_CHARS} chars]` : value;
}

export function readOnlyTools(): ToolDefinition[] {
  return [
    {
      name: "read_file",
      effect: "read",
      description: "Read a UTF-8 text file inside the worker worktree",
      parameters: {
        type: "object",
        properties: { path: { type: "string", description: "Path relative to the worktree" } },
        required: ["path"],
      },
      run: async (args, _signal, ctx) => {
        const root = requireRoot(ctx);
        const file = resolveInside(root, args.path);
        const info = await stat(file);
        if (!info.isFile()) throw new Error(`not a file: ${String(args.path)}`);
        const buffer = await readFile(file);
        const slice = buffer.subarray(0, MAX_READ_BYTES);
        return {
          path: relative(root, file),
          content: slice.toString("utf8"),
          bytes: buffer.length,
          truncated: buffer.length > MAX_READ_BYTES,
        };
      },
    },
    {
      name: "list_directory",
      effect: "read",
      description: "List entries of a directory inside the worker worktree",
      parameters: {
        type: "object",
        properties: { path: { type: "string", description: "Directory relative to the worktree" } },
        required: ["path"],
      },
      run: async (args, _signal, ctx) => {
        const root = requireRoot(ctx);
        const dir = resolveInside(root, typeof args.path === "string" && args.path ? args.path : ".");
        const info = await stat(dir);
        if (!info.isDirectory()) throw new Error(`not a directory: ${String(args.path)}`);
        const entries = await readdir(dir, { withFileTypes: true });
        return {
          path: relative(root, dir) || ".",
          entries: entries
            .map((entry) => ({
              name: entry.name,
              type: entry.isDirectory() ? "dir" : entry.isFile() ? "file" : "other",
            }))
            .sort((a, b) => a.name.localeCompare(b.name)),
        };
      },
    },
  ];
}

export function writeTools(): ToolDefinition[] {
  return [
    {
      name: "write_file",
      effect: "write",
      description: "Write UTF-8 text to a file inside the worker worktree",
      parameters: {
        type: "object",
        properties: {
          path: { type: "string", description: "Path relative to the worktree" },
          content: { type: "string", description: "Full file content" },
        },
        required: ["path", "content"],
      },
      run: async (args, _signal, ctx) => {
        const root = requireRoot(ctx);
        const file = resolveInside(root, args.path);
        const content = typeof args.content === "string" ? args.content : "";
        await mkdir(dirname(file), { recursive: true });
        await writeFile(file, content, "utf8");
        return { path: relative(root, file), bytes: Buffer.byteLength(content) };
      },
    },
    {
      name: "delete_file",
      effect: "write",
      description: "Delete a file or directory inside the worker worktree",
      parameters: {
        type: "object",
        properties: { path: { type: "string", description: "Path relative to the worktree" } },
        required: ["path"],
      },
      run: async (args, _signal, ctx) => {
        const root = requireRoot(ctx);
        const target = resolveInside(root, args.path);
        if (target === root) throw new Error("refusing to delete the worktree root");
        await rm(target, { recursive: true, force: false });
        return { path: relative(root, target), deleted: true };
      },
    },
  ];
}

export function executeTools(): ToolDefinition[] {
  return [
    {
      name: "terminal",
      effect: "execute",
      description: "Run a shell command inside the worker worktree",
      parameters: {
        type: "object",
        properties: {
          command: { type: "string", description: "Shell command to run" },
          timeoutMs: { type: "number", description: "Timeout in milliseconds" },
        },
        required: ["command"],
      },
      run: async (args, signal, ctx) => {
        const root = requireRoot(ctx);
        const command = typeof args.command === "string" ? args.command : "";
        if (!command.trim()) throw new Error("command is required");
        const requested = typeof args.timeoutMs === "number" && args.timeoutMs > 0 ? args.timeoutMs : DEFAULT_TIMEOUT_MS;
        const timeout = Math.min(requested, MAX_TIMEOUT_MS);
        const result = await execa("sh", ["-lc", command], {
          cwd: root,
          timeout,
          reject: false,
          signal,
          maxBuffer: 16 * 1024 * 1024,
        });
        return {
          command,
          cwd: root,
          exitCode: result.exitCode ?? null,
          stdout: truncate(result.stdout ?? ""),
          stderr: truncate(result.stderr ?? ""),
        };
      },
    },
  ];
}

export function controlTools(): ToolDefinition[] {
  return [
    {
      name: "start",
      effect: "execute",
      description: "Start a background tool without blocking the foreground conversation",
      parameters: {
        type: "object",
        properties: { tool: { type: "string" }, args: { type: "object" } },
        required: ["tool", "args"],
      },
      run: async (args) => ({ started: true, tool: String(args.tool ?? "") }),
    },
    {
      name: "stop",
      effect: "execute",
      description: "Stop a background tool by id",
      parameters: { type: "object", properties: { id: { type: "string" } }, required: ["id"] },
      run: async (args) => ({ stopped: true, id: String(args.id ?? "") }),
    },
  ];
}

export function builtinTools(): ToolDefinition[] {
  return [...readOnlyTools(), ...writeTools(), ...executeTools(), ...controlTools()];
}
