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
    suspend fun usageSummary(range: UsageRange): UsageSummary
    fun close()
}

enum class UsageRange(val wire: String, val label: String) {
    TODAY("today", "Today"),
    WEEK("7d", "7 days"),
    MONTH("30d", "30 days"),
    ALL("all", "All time"),
}

data class UsageBucket(
    val tokens: Long = 0,
    val tokensIn: Long = 0,
    val tokensOut: Long = 0,
    val cacheHits: Long = 0,
    val cost: Double = 0.0,
    val requests: Int = 0,
)

data class UsageDayBucket(
    val date: String,
    val tokens: Long,
    val cost: Double,
    val requests: Int,
)

data class UsageSessionBucket(
    val sessionId: String,
    val models: List<String>,
    val tokens: Long,
    val tokensIn: Long,
    val tokensOut: Long,
    val cost: Double,
    val requests: Int,
    val lastActivity: Long,
)

data class UsageModelBucket(
    val model: String,
    val provider: String,
    val tokens: Long,
    val cost: Double,
    val requests: Int,
)

data class UsageSummary(
    val range: String = "7d",
    val totals: UsageBucket = UsageBucket(),
    val priced: Boolean = true,
    val byDay: List<UsageDayBucket> = emptyList(),
    val bySession: List<UsageSessionBucket> = emptyList(),
    val byModel: List<UsageModelBucket> = emptyList(),
    val heatmap: List<UsageDayBucket> = emptyList(),
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
    val totalsNode = obj["totals"] as? JsonObject

    fun bucket(node: JsonObject?): UsageBucket = UsageBucket(
        tokens = node?.get("tokens")?.jsonPrimitive?.longOrNull ?: 0L,
        tokensIn = node?.get("tokensIn")?.jsonPrimitive?.longOrNull ?: 0L,
        tokensOut = node?.get("tokensOut")?.jsonPrimitive?.longOrNull ?: 0L,
        cacheHits = node?.get("cacheHits")?.jsonPrimitive?.longOrNull ?: 0L,
        cost = node?.get("cost")?.jsonPrimitive?.doubleOrNull ?: 0.0,
        requests = node?.get("requests")?.jsonPrimitive?.intOrNull ?: 0,
    )

    fun days(key: String): List<UsageDayBucket> = (obj[key] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageDayBucket(
            date = item["date"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            tokens = item["tokens"]?.jsonPrimitive?.longOrNull ?: 0L,
            cost = item["cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            requests = item["requests"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    val bySession = (obj["bySession"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        val modelList = (item["models"] as? JsonArray).orEmpty().map { it.jsonPrimitive.contentOrNull.orEmpty() }
        UsageSessionBucket(
            sessionId = item["sessionId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            models = modelList,
            tokens = item["tokens"]?.jsonPrimitive?.longOrNull ?: 0L,
            tokensIn = item["tokensIn"]?.jsonPrimitive?.longOrNull ?: 0L,
            tokensOut = item["tokensOut"]?.jsonPrimitive?.longOrNull ?: 0L,
            cost = item["cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            requests = item["requests"]?.jsonPrimitive?.intOrNull ?: 0,
            lastActivity = item["lastActivity"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    val byModel = (obj["byModel"] as? JsonArray).orEmpty().map { entry ->
        val item = entry.jsonObject
        UsageModelBucket(
            model = item["model"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            provider = item["provider"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            tokens = item["tokens"]?.jsonPrimitive?.longOrNull ?: 0L,
            cost = item["cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            requests = item["requests"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    return UsageSummary(
        range = obj["range"]?.jsonPrimitive?.contentOrNull ?: "7d",
        totals = bucket(totalsNode),
        priced = totalsNode?.get("priced")?.jsonPrimitive?.booleanOrNull ?: true,
        byDay = days("byDay"),
        bySession = bySession,
        byModel = byModel,
        heatmap = days("heatmap"),
    )
}
