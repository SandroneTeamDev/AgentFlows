export interface InlineRef {
  id: string;
  summary: string;
  next?: InlineRef[];
}

export class InlineSpillManager {
  private refs = new Map<string, InlineRef>();
  private readonly maxInlineChars: number;

  constructor(maxInlineChars = 500) {
    this.maxInlineChars = maxInlineChars;
  }

  inline(objectId: string, summary: string, next: string[] = []): void {
    this.refs.set(objectId, {
      id: objectId,
      summary: summary.slice(0, this.maxInlineChars),
      next: next.length > 0 ? next.map((id) => ({ id, summary: "" })) : undefined,
    });
  }

  spill(objectId: string, truncatedSummary: string): void {
    this.refs.set(objectId, {
      id: objectId,
      summary: `${truncatedSummary} [spilled - call read to get full content]`,
    });
  }

  recall(objectId: string): InlineRef | undefined {
    return this.refs.get(objectId);
  }

  chainRecall(startId: string, depth = 3): InlineRef[] {
    const results: InlineRef[] = [];
    const visited = new Set<string>();
    const start = this.recall(startId);
    const queue: { ref: InlineRef; d: number }[] = start ? [{ ref: start, d: 0 }] : [];

    while (queue.length > 0) {
      const { ref, d } = queue.shift()!;
      if (!ref || visited.has(ref.id)) continue;
      visited.add(ref.id);
      results.push(ref);
      if (d < depth && ref.next) {
        for (const n of ref.next) {
          queue.push({ ref: this.recall(n.id) ?? n, d: d + 1 });
        }
      }
    }
    return results;
  }

  list(): InlineRef[] {
    return [...this.refs.values()];
  }
}
