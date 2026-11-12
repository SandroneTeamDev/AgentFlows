# AgentFlows

**v0.0.1-rc1-alpha1 · Alpha development preview**

A local-first, continuously running personal Super-agent built around an event-flow timeline rather than a DAG editor. One conversational Router coordinates isolated Workers while the interface remains responsive.

## Features and direction

- **Event-flow workspace** — inspect Triggers, Router decisions, Worker activity, tool arguments, and results in an expandable timeline.
- **Native expressive interface** — Compose Multiplatform with Material Design 3, dynamic color where available, spring motion, and an adaptive workspace.
- **Independent engine** — TypeScript and Node.js own orchestration and execution; the interface communicates over authenticated loopback IPC.
- **Orchestration foundation** — Router, Worker, event-bus, and asynchronous tool interfaces are present; end-to-end task execution is still being connected.
- **Inspectable runtime** — incoming tasks are recorded as real Triggers. Tasks waiting for model configuration are not displayed as completed Worker results.
- **Local persistence** — SQLite persists event history without a hosted application backend.

This is an early development preview, not a feature-complete release. The broader roadmap includes concurrent Workers, isolated worktrees, persistent Markdown memory, additional model protocols and local inference, OS-enforced sandboxes, system-backed secret storage, Git tooling, document editing, Chromium integration, plugin and skill markets, usage analytics, and an independent recovery Watchdog. Capabilities are not considered complete merely because their configuration or interface is present.

Remote model providers are optional integrations. When selected, requests go to the configured provider; application state, orchestration, and execution remain on the user's device.

## Super-agent architecture

The design follows the DeepSeek Harness team's **Super-agent design exploration**, published through its official WeChat account on **October 31, 2026**: two-layer orchestration, event-driven routing, full-duplex interaction, separation of state and computation, and an independent Watchdog.

AgentFlows deliberately moves the original cloud-hosted Agent Loop onto the user's device. Continuous operation therefore depends on the local process and the operating system's background-execution limits.

WeChat: https://mp.weixin.qq.com/s/zoS7GlReWPhfCjENGxXCdg
X: https://x.com/DeepSeekHarness/status/2116857257245987236

## Build and downloads

Build status, diagnostic logs, and downloadable artifacts are available in [GitHub Actions](https://github.com/SandroneTeamDev/AgentFlows/actions). Open a successful **Build** run and use its artifacts. A failed run is not a release.

## License

[AGPL-3.0](LICENSE)
