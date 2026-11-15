package sandrone.agentflows.shared.i18n

fun flowLabel(raw: String): String = when {
    raw == "User task received" -> pick("收到用户任务", raw)
    raw == "Trigger filtered out before routing" -> pick("触发在路由前被过滤", raw)
    raw == "Worker finished" -> pick("执行体已完成", raw)
    raw.startsWith("dispatched ") && raw.endsWith(" tasks") -> {
        val count = raw.removePrefix("dispatched ").removeSuffix(" tasks")
        pick("分派了 ${count} 个任务", raw)
    }
    else -> raw
}

fun flowTypeLabel(raw: String): String = when (raw) {
    "trigger" -> pick("触发", "trigger")
    "router_decision" -> pick("路由", "router decision")
    "worker_start" -> pick("执行体启动", "worker start")
    "worker_done" -> pick("执行体完成", "worker done")
    "tool_call" -> pick("工具调用", "tool call")
    "error" -> pick("告警", "error")
    "watchdog_alert" -> pick("看门狗告警", "watchdog alert")
    "speculative" -> pick("推测", "speculative")
    "router" -> pick("路由", "router")
    "worker" -> pick("执行体", "worker")
    "tool" -> pick("工具", "tool")
    "alert" -> pick("告警", "alert")
    "system" -> pick("系统", "system")
    "usage" -> pick("用量", "usage")
    else -> raw.replace('_', ' ')
}

fun statusLabel(raw: String): String = when (raw) {
    "pending_configuration" -> pick("待配置", raw)
    "assigned" -> pick("已分派", "assigned")
    "review" -> pick("审查中", "review")
    "done" -> pick("已完成", "done")
    "blocked" -> pick("受阻", "blocked")
    "idle" -> pick("空闲", "idle")
    "running" -> pick("运行中", "running")
    "completed" -> pick("已完成", "completed")
    "failed" -> pick("失败", "failed")
    "cancelled" -> pick("已取消", "cancelled")
    "queued" -> pick("排队中", "queued")
    "accepted" -> pick("已接受", "accepted")
    "healthy" -> pick("健康", "healthy")
    "attention" -> pick("需关注", "attention")
    "online" -> pick("在线", "online")
    "offline" -> pick("离线", "offline")
    "error" -> pick("错误", "error")
    else -> raw
}

fun roleLabel(raw: String): String = when (raw) {
    "router" -> pick("路由", "router")
    "worker" -> pick("执行体", "worker")
    "reviewer" -> pick("审查", "reviewer")
    "planner" -> pick("规划", "planner")
    "watchdog" -> pick("看门狗", "watchdog")
    "subagent" -> pick("子代理", "subagent")
    "user" -> pick("用户", "user")
    else -> raw
}

fun errorLabel(raw: String): String = when (raw) {
    "none" -> pick("无", "none")
    "network" -> pick("网络", "network")
    "rate_limit" -> pick("限流", "rate limit")
    "context_limit" -> pick("上下文超限", "context limit")
    "tool_failure" -> pick("工具失败", "tool failure")
    "aborted" -> pick("已中止", "aborted")
    "provider_error" -> pick("服务商错误", "provider error")
    "unknown" -> pick("未知", "unknown")
    else -> raw
}

fun engineMessage(connected: Boolean, modelConfigured: Boolean): String = when {
    !connected -> pick("本地引擎不可用", "Local engine unavailable")
    modelConfigured -> pick("本地引擎在线 · 模型已就绪", "Local engine online · model configured")
    else -> pick("本地引擎在线 · 模型未配置", "Local engine online · model not configured")
}
