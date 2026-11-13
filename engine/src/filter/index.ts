import type { Trigger, FilterRule } from "../types.js";
import type { ModelProvider } from "../models/base.js";

type FastPathResult = "pass" | "drop" | "candidate";

export class TwoLevelFilter {
  private programRules: FilterRule[] = [];
  private semanticProvider?: ModelProvider;
  private semanticModelId?: string;

  addProgramRule(rule: FilterRule): void {
    this.programRules.push(rule);
  }

  setSemanticLayer(provider: ModelProvider, modelId: string): void {
    this.semanticProvider = provider;
    this.semanticModelId = modelId;
  }

  fastPath(trigger: Trigger): FastPathResult {
    for (const rule of this.programRules) {
      if (rule.source !== "*" && rule.source !== trigger.source) continue;
      if (this.matchConditions(rule.conditions, trigger)) return rule.action;
    }
    return "candidate";
  }

  async slowPath(trigger: Trigger): Promise<boolean> {
    if (!this.semanticProvider || !this.semanticModelId) {
      return this.fastPath(trigger) !== "drop";
    }
    const response = await this.semanticProvider.invoke(this.semanticModelId, [
      {
        role: "system",
        content:
          "You are a filter. Given an event, decide if the agent should act on it. " +
          'Reply "act" or "ignore" followed by a one-line reason.',
      },
      { role: "user", content: JSON.stringify(trigger) },
    ]);
    return response.text.trim().toLowerCase().startsWith("act");
  }

  async filter(trigger: Trigger): Promise<{ action: FastPathResult; shouldAct: boolean }> {
    const fast = this.fastPath(trigger);
    if (fast === "drop") return { action: fast, shouldAct: false };
    if (fast === "pass") return { action: fast, shouldAct: true };
    const shouldAct = await this.slowPath(trigger);
    return { action: fast, shouldAct };
  }

  private matchConditions(
    conditions: Record<string, unknown>,
    trigger: Trigger,
  ): boolean {
    for (const [key, expected] of Object.entries(conditions)) {
      const actual = (trigger.meta as Record<string, unknown>)[key];
      if (typeof expected === "string" && typeof actual === "string") {
        if (!actual.includes(expected)) return false;
      } else if (Array.isArray(expected)) {
        if (!expected.includes(actual)) return false;
      } else if (actual !== expected) {
        return false;
      }
    }
    return true;
  }
}
