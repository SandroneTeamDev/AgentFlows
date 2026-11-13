# AgentFlows

**v0.0.1-rc1-alpha1 · Alpha development preview**

A local-first, continuously running personal Super-agent built around an event-flow timeline rather than a DAG editor. One conversational Router coordinates context-isolated Workers while the interface stays fully responsive.

## Features and direction

- **Event-flow workspace** — inspect Triggers, Router decisions, Worker activity, tool arguments, and results in an expandable timeline.
- **Native expressive interface** — Compose Multiplatform with Material Design 3, dynamic color where available, spring motion, and an adaptive workspace.
- **Single-kernel engine** — a bundled Node.js CLI owns all Agent state and execution, ZCode-style: the interface is a thin renderer that talks to it over authenticated loopback JSON-RPC, and every Agent capability lives in one process with one source of truth.
- **Two-layer orchestration** — a lightweight Router batches and dispatches incoming Triggers; Workers execute in isolated worktrees with per-agent Markdown memory.
- **Event-driven core** — two-level filtering (deterministic fast path, semantic slow path), thread subscriptions with direct dispatch, Inline & Spill context with chain-of-recall, and speculative execution.
- **Full-duplex interaction** — long-running tools run through `start` / `stop`; results stream back as new events while the conversation keeps moving.
- **Self-healing Watchdog** — an independent control loop monitors every Worker for stalls and off-track work, and can be queried in plain language.
- **Local persistence** — SQLite keeps sessions, tasks, events, and usage on the device; no hosted backend.
- **Pluggable models** — OpenAI-compatible protocols, Anthropic, Gemini, and local inference (Ollama, llama.cpp, LM Studio, MNN), with per-task model routing and custom headers.

This is an early development preview, not a feature-complete release. Capabilities are not considered complete merely because their configuration or interface is present.

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
