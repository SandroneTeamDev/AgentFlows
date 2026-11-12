# AgentFlows

> A locally-running Super-agent system with continuous context, parallel multi-task execution, event-driven proactivity, and full-duplex async interaction.

**Version**: `0.0.1-rc1-alpha`

> Alpha builds are produced via [GitHub Actions](https://github.com/SandroneTeamDev/AgentFlows/actions).
> Download the latest APK from the **Actions → Build APK** tab (artifact: `agentflows-debug.apk`).

## Architecture

| Layer | Tech |
|---|---|
| UI | Compose Multiplatform (MD3 M3 Expressive, dynamic color + motion springs) |
| Engine | Node.js / TypeScript (separate process, localhost IPC) |
| Core | Super-agent: Router + Worker + Event Bus + Async ReAct Loop + Watchdog |

- UI is a pure display layer; all agent logic lives in the engine
- Router uses a lightweight model; Workers use the strongest model (user-configured)
- 4 access levels: full / plan / view / workspace_write
- OS-level sandbox: Linux bwrap / macOS seatbelt / Windows MIC
- Per-agent git worktree isolation

## Platforms

| Platform | Distribution |
|---|---|
| Android | APK (minSdk 29) |
| macOS | DMG |
| Windows | EXE |
| Linux | deb / rpm / arch |

## Roadmap

- [x] Project skeleton
- [x] Android app: MD3 theme + multi-panel UI + event timeline
- [x] Engine: Router / Worker / Event Bus / Filter / Watchdog / ReAct Loop
- [ ] Engine: model providers (OpenAI / Anthropic / Gemini / local)
- [ ] Engine: tool registry + plugin system
- [ ] Engine: IPC server
- [ ] Full Android: PRoot workspace, MNN local inference, accessibility service
- [ ] Desktop (macOS / Windows / Linux)

## License

AGPL-3.0
