import type { z } from "zod";

export type AgentRole = "router" | "worker" | "watchdog" | "subagent";

export type AccessLevel = "full" | "plan" | "view" | "workspace_write";

export type TriggerSource =
  | "local_fs"
  | "local_terminal"
  | "local_cron"
  | "local_git_hook"
  | "github_webhook"
  | "email"
  | "calendar"
  | "custom_webhook"
  | "user_input";

export interface Trigger {
  id: string;
  source: TriggerSource;
  threadId: string;
  payload: unknown;
  timestamp: number;
  meta: Record<string, unknown>;
}

export interface Thread {
  id: string;
  label: string;
  triggers: Trigger[];
  subscribers: string[];
  active: boolean;
}

export interface WorkerSession {
  id: string;
  agentRole: "worker" | "subagent";
  modelId: string;
  accessLevel: AccessLevel;
  worktree: string;
  memoryPath: string;
  subscribedThreads: string[];
  status: "idle" | "running" | "blocked" | "done";
  taskId?: string;
  parentWorkerId?: string;
  createdAt: number;
  updatedAt: number;
}

export interface RouterSession {
  id: string;
  modelId: string;
  activeWorkers: string[];
  pendingTriggers: string[];
  createdAt: number;
  updatedAt: number;
}

export interface ToolCall {
  id: string;
  name: string;
  args: Record<string, unknown>;
  status: "pending" | "running" | "done" | "error";
  result?: unknown;
  error?: string;
  startedAt: number;
  finishedAt?: number;
}

export interface ModelCall {
  id: string;
  provider: string;
  model: string;
  role: AgentRole;
  messages: Record<string, unknown>[];
  tokensIn: number;
  tokensOut: number;
  cost: number;
  startedAt: number;
  finishedAt?: number;
}

export interface TaskRecord {
  id: string;
  title: string;
  status: "queued" | "assigned" | "running" | "review" | "done" | "failed";
  assignedWorker?: string;
  createdAt: number;
  updatedAt: number;
  result?: unknown;
}

export interface SessionState {
  id: string;
  role: AgentRole;
  modelId: string;
  context: unknown[];
  toolCalls: ToolCall[];
  task?: TaskRecord;
  subscriptions: string[];
  createdAt: number;
  updatedAt: number;
}

export interface InlineRef {
  id: string;
  summary: string;
  next?: InlineRef[];
}

export interface WatchdogState {
  intervalMs: number;
  lastHeartbeat: number;
  alerts: string[];
  healthy: boolean;
}

export interface ProviderConfig {
  id: string;
  kind: "openai" | "anthropic" | "gemini" | "ollama" | "llama_cpp" | "mnn" | "custom";
  baseUrl: string;
  apiKey?: string;
  models: string[];
  headers?: Record<string, string>;
  body?: Record<string, unknown>;
}

export interface ModelAssignment {
  chatModel: string;
  memoryModel: string;
  summaryModel: string;
  uiModel: string;
}

export interface UsageRecord {
  sessionId: string;
  model: string;
  provider: string;
  tokensIn: number;
  tokensOut: number;
  cacheHits: number;
  cost: number;
  timestamp: number;
}

export interface AgentFlowNode {
  id: string;
  type: "trigger" | "router_decision" | "worker_start" | "tool_call" | "worker_done" | "error" | "watchdog_alert" | "speculative";
  label: string;
  data: Record<string, unknown>;
  children?: AgentFlowNode[];
  timestamp: number;
}

export interface FilterRule {
  id: string;
  source: TriggerSource | "*";
  conditions: Record<string, unknown>;
  action: "pass" | "drop" | "candidate";
}

export interface PluginManifest {
  id: string;
  name: string;
  version: string;
  kind: "ui_panel" | "tool" | "agent" | "automation" | "mcp_server" | "skill";
  entry: string;
  permissions: string[];
  icon?: string;
}

export interface EngineSnapshot {
  connected: boolean;
  modelConfigured: boolean;
  router: RouterSession;
  workers: WorkerSession[];
  tasks: TaskRecord[];
  flows: AgentFlowNode[];
  watchdog: WatchdogState;
  usage: UsageRecord[];
  message: string;
}

export interface RpcRequest {
  jsonrpc: "2.0";
  id: string | number;
  method: string;
  params?: Record<string, unknown>;
}

export interface RpcResponse {
  jsonrpc: "2.0";
  id: string | number | null;
  result?: unknown;
  error?: { code: number; message: string };
}
