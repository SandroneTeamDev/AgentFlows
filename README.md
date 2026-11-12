# AgentFlows

**v0.0.1-rc1-alpha1**

> A locally-running Super-agent that stays online, keeps context across sessions, and pushes multiple tasks in parallel without human babysitting.

## What it does

- **Event-driven, not prompt-driven** — watches GitHub PRs, file changes, cron triggers, and webhooks; acts on what matters, stays quiet on noise
- **Two-layer architecture** — a lightweight Router dispatches work to isolated Worker agents; each Worker owns its own git worktree so parallel tasks never collide
- **Full-duplex async ReAct Loop** — fire off a long-running tool call and keep talking; results stream back as events, no blocking
- **Self-healing Watchdog** — an independent session monitors every Worker; catches stalls, off-track tasks, and infrastructure faults, then fixes or re-dispatches
- **Inline & Spill context** — token budget treated like a cache: hot data inlined, cold data spilled to disk, chain-of-recall fetches on demand
- **Speculative execution** — the agent pre-drafts responses and pre-creates PR fixes in the background; useful results delivered, useless ones discarded
- **4-level access control** — full / plan / view / workspace_write, per-agent and per-path
- **OS-level sandbox** — Linux bwrap, macOS seatbelt, Windows MIC
- **Pluggable model routing** — OpenAI-compatible, Anthropic, Gemini, Ollama, LM Studio, llama.cpp, MNN; per-task model assignment; custom HTTP headers and bodies

## Architecture

Based on the DeepSeek Harness team's [Super-agent design exploration](https://mp.weixin.qq.com/s/zoS7GlReWPhfCjENGxXCdg):

WeChat: https://mp.weixin.qq.com/s/zoS7GlReWPhfCjENGxXCdg
X: https://x.com/DeepSeekHarness/status/2116857257245987236

| Layer | Tech |
|---|---|
| UI | Compose Multiplatform, MD3 M3 Expressive |
| Engine | Node.js / TypeScript, separate process, localhost IPC |
| Core | Router + Worker + Event Bus + Async ReAct Loop + Watchdog |

## Build

All distributions (APK, EXE, DMG, deb, rpm, arch) are built in parallel by [GitHub Actions](https://github.com/SandroneTeamDev/AgentFlows/actions).
Download the latest artifacts from the most recent successful **Build** run.

## License

AGPL-3.0
