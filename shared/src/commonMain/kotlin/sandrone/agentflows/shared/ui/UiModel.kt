package sandrone.agentflows.shared.ui

enum class AgentTab(val label: String) {
    Chat("Chat"),
    Flows("Flows"),
    Usage("Usage"),
    Settings("Settings"),
}

enum class MessageAuthor { USER, ENGINE }

data class ChatMessage(
    val id: String,
    val author: MessageAuthor,
    val text: String,
    val status: String? = null,
    val detail: String? = null,
    val timestamp: Long = 0L,
)

enum class FlowTone {
    TRIGGER,
    ROUTER,
    WORKER,
    TOOL,
    RESULT,
    ALERT,
    NEUTRAL,
}

const val MILLIS_PER_DAY = 86_400_000L

fun flowToneOf(type: String): FlowTone = when (type) {
    "trigger" -> FlowTone.TRIGGER
    "router_decision" -> FlowTone.ROUTER
    "worker_start", "worker_done" -> FlowTone.WORKER
    "tool_call" -> FlowTone.TOOL
    "speculative" -> FlowTone.RESULT
    "error", "watchdog_alert" -> FlowTone.ALERT
    else -> FlowTone.NEUTRAL
}

fun flowLabel(type: String): String = type.replace('_', ' ')

private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = if (month <= 2) y + 1 else y
    return Triple(year.toInt(), month.toInt(), day.toInt())
}

fun formatClock(millis: Long): String {
    val seconds = (millis / 1000) % 86_400
    val hours = (seconds / 3600).toString().padStart(2, '0')
    val minutes = ((seconds % 3600) / 60).toString().padStart(2, '0')
    return "$hours:$minutes"
}

fun formatDate(millis: Long): String {
    val (year, month, day) = civilFromDays(millis / MILLIS_PER_DAY)
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

fun formatShortDay(millis: Long): String {
    val (_, month, day) = civilFromDays(millis / MILLIS_PER_DAY)
    return "${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

fun formatRelative(millis: Long, now: Long): String {
    val delta = (now - millis).coerceAtLeast(0)
    return when {
        millis <= 0L -> "unknown"
        delta < 60_000L -> "just now"
        delta < 3_600_000L -> "${delta / 60_000L}m ago"
        delta < MILLIS_PER_DAY -> "${delta / 3_600_000L}h ago"
        else -> "${delta / MILLIS_PER_DAY}d ago"
    }
}

fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0s"
    val totalSeconds = millis / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun formatCount(value: Long): String = when {
    value >= 1_000_000L -> "${(value / 100_000L) / 10.0}M"
    value >= 1_000L -> "${(value / 100L) / 10.0}K"
    else -> value.toString()
}

fun formatCost(value: Double): String {
    if (value <= 0.0) return "$0.00"
    if (value < 0.01) return "<$0.01"
    val cents = (value * 100.0).toLong()
    return "$" + (cents / 100).toString() + "." + (cents % 100).toString().padStart(2, '0')
}
