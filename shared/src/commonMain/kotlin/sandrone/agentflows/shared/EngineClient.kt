package sandrone.agentflows.shared

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal data class EngineConnection(val port: Int, val token: String)

interface EngineClient {
    suspend fun snapshot(): WorkspaceSnapshot
    suspend fun submit(text: String): String
    suspend fun cancelTask(workerId: String)
    suspend fun askWatchdog(question: String): String
    suspend fun registerProvider(id: String, baseUrl: String, models: List<String>)
    suspend fun probeModels(): Boolean
    suspend fun usageSummary(range: UsageRange, granularity: UsageGranularity = UsageGranularity.DAY): UsageSummary
    suspend fun usageTools(range: UsageRange): List<UsageToolBucket>
    suspend fun usageExport(range: UsageRange, format: String = "csv"): String
    fun close()
}

enum class UsageRange(val wire: String, val label: String) {
    TODAY("today", "Today"),
    WEEK("7d", "7 days"),
    MONTH("30d", "30 days"),
    ALL("all", "All time"),
}

enum class UsageGranularity(val wire: String, val label: String) {
    HOUR("hour", "Hourly"),
    DAY("day", "Daily"),
    WEEK("week", "Weekly"),
}

data class UsageBucket(
    val tokens: Long = 0,
    val tokensIn: Long = 0,
    val tokensOut: Long = 0,
    val cacheHits: Long = 0,
    val cacheWriteTokens: Long = 0,
    val cost: Double = 0.0,
    val actualCost: Double = 0.0,
    val requests: Int = 0,
    val failures: Int = 0,
)

data class UsageDayBucket(
    val date: String,
    val tokens: Long,
    val cost: Double,
    val requests: Int,
)

data class UsagePoint(
    val key: String,
    val label: String,
    val tokens: Long,
    val cost: Double,
    val actualCost: Double,
    val requests: Int,
)

data class UsageSessionBucket(
    val sessionId: String,
    val role: String,
    val models: List<String>,
    val tokens: Long,
    val tokensIn: Long,
    val tokensOut: Long,
    val cost: Double,
    val actualCost: Double,
    val requests: Int,
    val failures: Int,
    val toolCalls: Int,
    val avgDurationMs: Int,
    val lastActivity: Long,
)

data class UsageModelBucket(
    val model: String,
    val provider: String,
    val tokens: Long,
    val cost: Double,
    val actualCost: Double,
    val requests: Int,
    val priced: Boolean,
)

data class UsageErrorBucket(val category: String, val requests: Int)

data class UsageToolBucket(
    val tool: String,
    val calls: Int,
    val failures: Int,
    val avgDurationMs: Int,
    val totalDurationMs: Long,
)

data class UsageTokens(
    val input: Long = 0,
    val output: Long = 0,
    val cacheRead: Long = 0,
    val cacheWrite: Long = 0,
)

data class UsageCost(
    val input: Double = 0.0,
    val output: Double = 0.0,
    val cacheRead: Double = 0.0,
    val cacheWrite: Double = 0.0,
)

data class UsageSummary(
    val range: String = "7d",
    val granularity: String = "day",
    val totals: UsageBucket = UsageBucket(),
    val priced: Boolean = true,
    val composition: UsageTokens = UsageTokens(),
    val costBreakdown: UsageCost = UsageCost(),
    val byPoint: List<UsagePoint> = emptyList(),
    val bySession: List<UsageSessionBucket> = emptyList(),
    val byModel: List<UsageModelBucket> = emptyList(),
    val byRole: List<UsagePoint> = emptyList(),
    val byError: List<UsageErrorBucket> = emptyList(),
    val heatmap: List<UsageDayBucket> = emptyList(),
    val statsUpdatedAt: Long = 0,
)

expect fun createEngineClient(): EngineClient

data class AgentFlowEntry(
    val id: String,
    val type: String,
    val label: String,
    val timestamp: Long,
    val details: String,
)

data class WorkerEntry(
    val id: String,
    val status: String,
)

data class TaskEntry(
    val id: String,
    val title: String,
    val status: String,
)

data class UsageEntry(
    val sessionId: String,
    val model: String,
    val provider: String,
    val tokensIn: Long,
    val tokensOut: Long,
    val cost: Double,
    val timestamp: Long,
)

data class ProviderEntry(
    val id: String,
    val kind: String,
    val baseUrl: String,
    val models: List<String>,
)

data class WatchdogInfo(
    val healthy: Boolean,
    val alerts: List<String>,
    val lastHeartbeat: Long,
)

data class WorkspaceSnapshot(
    val connected: Boolean = false,
    val modelConfigured: Boolean = false,
    val flows: List<AgentFlowEntry> = emptyList(),
    val workers: List<WorkerEntry> = emptyList(),
    val tasks: List<TaskEntry> = emptyList(),
    val usage: List<UsageEntry> = emptyList(),
    val providers: List<ProviderEntry> = emptyList(),
    val watchdog: WatchdogInfo = WatchdogInfo(true, emptyList(), 0),
    val uptimeMs: Long = 0,
    val message: String = "Connecting to the local engine…",
)

internal fun parseSnapshot(element: JsonElement): WorkspaceSnapshot {
    val obj = element.jsonObject
    val flows = (obj["flows"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        AgentFlowEntry(
            id = item.string("id"),
            type = item.string("type"),
            label = item.string("label"),
            timestamp = item["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
            details = item["data"]?.toString() ?: "{}",
        )
    }
    val workers = (obj["workers"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        WorkerEntry(item.string("id"), item.string("status"))
    }
    val tasks = (obj["tasks"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        TaskEntry(item.string("id"), item.string("title"), item.string("status"))
    }
    val usage = (obj["usage"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        UsageEntry(
            sessionId = item.string("sessionId"),
            model = item.string("model"),
            provider = item.string("provider"),
            tokensIn = item["tokensIn"]?.jsonPrimitive?.longOrNull ?: 0L,
            tokensOut = item["tokensOut"]?.jsonPrimitive?.longOrNull ?: 0L,
            cost = item["cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            timestamp = item["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }
    val providers = (obj["providers"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        ProviderEntry(
            id = item.string("id"),
            kind = item.string("kind"),
            baseUrl = item.string("baseUrl"),
            models = (item["models"] as? JsonArray).orEmpty().map { it.jsonPrimitive.contentOrNull.orEmpty() },
        )
    }
    val watchdogNode = obj["watchdog"] as? JsonObject
    val watchdog = WatchdogInfo(
        healthy = watchdogNode?.get("healthy")?.jsonPrimitive?.booleanOrNull ?: true,
        alerts = (watchdogNode?.get("alerts") as? JsonArray).orEmpty().map { it.jsonPrimitive.contentOrNull.orEmpty() },
        lastHeartbeat = watchdogNode?.get("lastHeartbeat")?.jsonPrimitive?.longOrNull ?: 0L,
    )
    val configured = obj["modelConfigured"]?.jsonPrimitive?.booleanOrNull ?: false
    return WorkspaceSnapshot(
        connected = true,
        modelConfigured = configured,
        flows = flows,
        workers = workers,
        tasks = tasks,
        usage = usage,
        providers = providers,
        watchdog = watchdog,
        uptimeMs = obj["uptimeMs"]?.jsonPrimitive?.longOrNull ?: 0L,
        message = obj["message"]?.jsonPrimitive?.contentOrNull
            ?: if (configured) "Local engine online" else "Local engine online · model not configured",
    )
}

internal fun parseSubmitResult(element: JsonElement): String {
    val obj = element.jsonObject
    val trigger = obj["triggerId"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val state = obj["state"]?.jsonPrimitive?.contentOrNull.orEmpty()
    return if (state.isNotEmpty()) "$trigger|$state" else trigger
}

internal fun parseWatchdogAnswer(element: JsonElement): String {
    val obj = element.jsonObject
    return obj["answer"]?.jsonPrimitive?.contentOrNull
        ?: obj["message"]?.jsonPrimitive?.contentOrNull
        ?: ""
}

internal fun parseProbeResult(element: JsonElement): Boolean {
    val obj = element.jsonObject
    return obj["healthy"]?.jsonPrimitive?.booleanOrNull ?: false
}

private fun JsonObject.string(key: String): String =
    (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun parseUsageSummary(element: JsonElement): UsageSummary {
    val obj = element.jsonObject

    fun number(node: JsonObject?, key: String): Long = node?.get(key)?.jsonPrimitive?.longOrNull ?: 0L
    fun real(node: JsonObject?, key: String): Double = node?.get(key)?.jsonPrimitive?.doubleOrNull ?: 0.0
    fun text(node: JsonObject?, key: String): String = node?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
    fun flag(node: JsonObject?, key: String, fallback: Boolean): Boolean =
        node?.get(key)?.jsonPrimitive?.booleanOrNull ?: fallback

    fun bucket(node: JsonObject?): UsageBucket = UsageBucket(
        tokens = number(node, "tokens"),
        tokensIn = number(node, "tokensIn"),
        tokensOut = number(node, "tokensOut"),
        cacheHits = number(node, "cacheHits"),
        cacheWriteTokens = number(node, "cacheWriteTokens"),
        cost = real(node, "cost"),
        actualCost = real(node, "actualCost"),
        requests = number(node, "requests").toInt(),
        failures = number(node, "failures").toInt(),
    )

    fun pointOf(node: JsonObject): UsagePoint = UsagePoint(
        key = text(node, "key"),
        label = text(node, "label").ifBlank { text(node, "key") },
        tokens = number(node, "tokens"),
        cost = real(node, "cost"),
        actualCost = real(node, "actualCost"),
        requests = number(node, "requests").toInt(),
    )

    val totalsNode = obj["totals"] as? JsonObject
    val compositionNode = obj["composition"] as? JsonObject
    val breakdownNode = obj["costBreakdown"] as? JsonObject

    val bySession = (obj["bySession"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageSessionBucket(
            sessionId = text(item, "sessionId"),
            role = text(item, "role"),
            models = (item["models"] as? JsonArray).orEmpty().map { it.jsonPrimitive.contentOrNull.orEmpty() },
            tokens = number(item, "tokens"),
            tokensIn = number(item, "tokensIn"),
            tokensOut = number(item, "tokensOut"),
            cost = real(item, "cost"),
            actualCost = real(item, "actualCost"),
            requests = number(item, "requests").toInt(),
            failures = number(item, "failures").toInt(),
            toolCalls = number(item, "toolCalls").toInt(),
            avgDurationMs = number(item, "avgDurationMs").toInt(),
            lastActivity = number(item, "lastActivity"),
        )
    }

    val byModel = (obj["byModel"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageModelBucket(
            model = text(item, "model"),
            provider = text(item, "provider"),
            tokens = number(item, "tokens"),
            cost = real(item, "cost"),
            actualCost = real(item, "actualCost"),
            requests = number(item, "requests").toInt(),
            priced = flag(item, "priced", true),
        )
    }

    val byError = (obj["byError"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageErrorBucket(text(item, "category"), number(item, "requests").toInt())
    }

    val heatmap = (obj["heatmap"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageDayBucket(
            date = text(item, "date"),
            tokens = number(item, "tokens"),
            cost = real(item, "cost"),
            requests = number(item, "requests").toInt(),
        )
    }

    return UsageSummary(
        range = text(obj, "range").ifBlank { "7d" },
        granularity = text(obj, "granularity").ifBlank { "day" },
        totals = bucket(totalsNode),
        priced = flag(totalsNode, "priced", true),
        composition = UsageTokens(
            input = number(compositionNode, "input"),
            output = number(compositionNode, "output"),
            cacheRead = number(compositionNode, "cacheRead"),
            cacheWrite = number(compositionNode, "cacheWrite"),
        ),
        costBreakdown = UsageCost(
            input = real(breakdownNode, "input"),
            output = real(breakdownNode, "output"),
            cacheRead = real(breakdownNode, "cacheRead"),
            cacheWrite = real(breakdownNode, "cacheWrite"),
        ),
        byPoint = (obj["byPoint"] as? JsonArray).orEmpty().map { pointOf(it.jsonObject) },
        bySession = bySession,
        byModel = byModel,
        byRole = (obj["byRole"] as? JsonArray).orEmpty().map { pointOf(it.jsonObject) },
        byError = byError,
        heatmap = heatmap,
        statsUpdatedAt = number(obj, "statsUpdatedAt"),
    )
}

internal fun parseToolBuckets(element: JsonElement): List<UsageToolBucket> =
    (element as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageToolBucket(
            tool = (item["tool"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            calls = (item["calls"] as? JsonPrimitive)?.intOrNull ?: 0,
            failures = (item["failures"] as? JsonPrimitive)?.intOrNull ?: 0,
            avgDurationMs = (item["avgDurationMs"] as? JsonPrimitive)?.intOrNull ?: 0,
            totalDurationMs = (item["totalDurationMs"] as? JsonPrimitive)?.longOrNull ?: 0L,
        )
    }