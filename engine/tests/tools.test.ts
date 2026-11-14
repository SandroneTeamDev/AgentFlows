import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { builtinTools } from "../src/tools/builtin-tools.js";
import { ToolRegistry } from "../src/tools/registry.js";

let worktree = "";

const registryFor = (accessLevel: "full" | "plan" | "view" | "workspace_write") => {
  const registry = new ToolRegistry(accessLevel);
  for (const tool of builtinTools()) registry.register(tool);
  return registry;
};

beforeEach(async () => {
  worktree = await mkdtemp(join(tmpdir(), "agentflows-tools-"));
});

afterEach(async () => {
  await rm(worktree, { recursive: true, force: true });
});

describe("worktree containment", () => {
  it("rejects parent traversal on read", async () => {
    const registry = registryFor("full");
    await expect(
      registry.execute("read_file", { path: "../../etc/passwd" }, undefined, { worktree }),
    ).rejects.toThrow("escapes worktree");
  });

  it("rejects absolute paths outside the worktree", async () => {
    const registry = registryFor("full");
    await expect(
      registry.execute("read_file", { path: "/etc/passwd" }, undefined, { worktree }),
    ).rejects.toThrow("escapes worktree");
  });

  it("rejects traversal on write without touching the parent directory", async () => {
    const registry = registryFor("full");
    await expect(
      registry.execute("write_file", { path: "../escape.txt", content: "x" }, undefined, { worktree }),
    ).rejects.toThrow("escapes worktree");
  });

  it("rejects traversal on delete", async () => {
    const registry = registryFor("full");
    await expect(
      registry.execute("delete_file", { path: "../outside" }, undefined, { worktree }),
    ).rejects.toThrow("escapes worktree");
  });

  it("refuses to delete the worktree root", async () => {
    const registry = registryFor("full");
    await expect(registry.execute("delete_file", { path: "." }, undefined, { worktree })).rejects.toThrow(
      "worktree root",
    );
  });

  it("requires a bound worktree", async () => {
    const registry = registryFor("full");
    await expect(registry.execute("read_file", { path: "a.txt" })).rejects.toThrow("no worktree");
  });
});

describe("real file operations", () => {
  it("writes then reads the same content", async () => {
    const registry = registryFor("full");
    await registry.execute("write_file", { path: "notes/a.txt", content: "hello" }, undefined, { worktree });
    const read = (await registry.execute("read_file", { path: "notes/a.txt" }, undefined, { worktree })) as {
      content: string;
      bytes: number;
    };
    expect(read.content).toBe("hello");
    expect(read.bytes).toBe(5);
    expect(await readFile(join(worktree, "notes/a.txt"), "utf8")).toBe("hello");
  });

  it("lists directory entries with types", async () => {
    const registry = registryFor("full");
    await writeFile(join(worktree, "file.txt"), "x");
    const listed = (await registry.execute("list_directory", { path: "." }, undefined, { worktree })) as {
      entries: { name: string; type: string }[];
    };
    expect(listed.entries).toContainEqual({ name: "file.txt", type: "file" });
  });

  it("deletes a file inside the worktree", async () => {
    const registry = registryFor("full");
    await writeFile(join(worktree, "gone.txt"), "x");
    await registry.execute("delete_file", { path: "gone.txt" }, undefined, { worktree });
    await expect(readFile(join(worktree, "gone.txt"))).rejects.toThrow();
  });
});

describe("access level enforcement", () => {
  it("denies writes to a view-only Worker", async () => {
    const registry = registryFor("view");
    expect(registry.names()).not.toContain("write_file");
    await expect(
      registry.execute("write_file", { path: "x.txt", content: "x" }, undefined, { worktree }),
    ).rejects.toThrow("denied");
  });

  it("denies terminal to a workspace-write Worker", async () => {
    const registry = registryFor("workspace_write");
    await expect(
      registry.execute("terminal", { command: "echo hi" }, undefined, { worktree }),
    ).rejects.toThrow("denied");
  });

  it("denies every mutating tool in plan mode", async () => {
    const registry = registryFor("plan");
    expect(registry.names()).toEqual([]);
  });

  it("allows workspace-write to create files", async () => {
    const registry = registryFor("workspace_write");
    await registry.execute("write_file", { path: "ok.txt", content: "y" }, undefined, { worktree });
    expect(await readFile(join(worktree, "ok.txt"), "utf8")).toBe("y");
  });
});

describe("terminal execution", () => {
  it("runs a command inside the worktree", async () => {
    const registry = registryFor("full");
    const result = (await registry.execute(
      "terminal",
      { command: "pwd && echo marker" },
      undefined,
      { worktree },
    )) as { exitCode: number; stdout: string };
    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain("agentflows-tools-");
    expect(result.stdout).toContain("marker");
  });

  it("reports a non-zero exit code instead of throwing", async () => {
    const registry = registryFor("full");
    const result = (await registry.execute("terminal", { command: "exit 3" }, undefined, { worktree })) as {
      exitCode: number;
    };
    expect(result.exitCode).toBe(3);
  });
});
