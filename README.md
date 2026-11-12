# AgentFlows

> 持续在线的本地 Super-agent 系统。跨会话上下文、多任务并行、事件驱动、全双工交互。

## 架构

- **UI**：Compose Multiplatform（MD3 M3 Expressive，动态取色 + motion springs）
- **引擎**：Node.js / TypeScript（独立进程，localhost IPC 桥接）
- **Super-agent 5 模块**：双层 Router/Worker · 事件驱动 · 全双工 · 存算分离 · Watchdog

## 平台

| 平台 | UI | 分发 |
|---|---|---|
| Android | Compose MP 原生 | APK |
| macOS | Compose MP (JVM) | DMG |
| Windows | Compose MP (JVM) | EXE |
| Linux | Compose MP (JVM) | deb / rpm / arch |

## 许可

AGPL-3.0
