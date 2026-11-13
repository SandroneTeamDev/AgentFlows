package sandrone.agentflows.shared

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
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
    fun close()
}

expect fun createEngineClient(): EngineClient

data class FlowEntry(
    val id: String,
    val type: String,
    val label: String,
    val timestamp: Long,
    val details: String,
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
    val flows: List<FlowEntry> = emptyList(),
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
        FlowEntry(
            id = item.string("id"),
            type = item.string("type"),
            label = item.string("label"),
            timestamp = item["timestamp"]?.jsonPrimitive?.longOrNull ?: 0,
            details = item["data"]?.toString() ?: "{}",
        )
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
            tokensIn = item["tokensIn"]?.jsonPrimitive?.longOrNull ?: 0,
            tokensOut = item["tokensOut"]?.jsonPrimitive?.longOrNull ?: 0,
            cost = item["cost"]?.jsonPrimitive?.floatOrNull?.toDouble() ?: 0.0,
            timestamp = item["timestamp"]?.jsonPrimitive?.longOrNull ?: 0,
        )
    }
    val providers = (obj["providers"] as? JsonArray).orEmpty().map { node ->
        val item = node.jsonObject
        ProviderEntry(
            id = item.string("id"),
            kind = item.string("kind"),
            baseUrl = item.string("baseUrl"),
            models = ((item["models"] as? JsonArray).orEmpty()).map { it.jsonPrimitive.content },
        )
    }
    val watchdogObj = obj["watchdog"] as? JsonObject
    val watchdog = WatchdogInfo(
        healthy = watchdogObj?.get("healthy")?.jsonPrimitive?.booleanOrNull ?: true,
        alerts = ((watchdogObj?.get("alerts") as? JsonArray).orEmpty()).map { it.jsonPrimitive.content },
        lastHeartbeat = watchdogObj?.get("lastHeartbeat")?.jsonPrimitive?.longOrNull ?: 0,
    )
    val configured = obj["modelConfigured"]?.jsonPrimitive?.booleanOrNull ?: false
    return WorkspaceSnapshot(
        connected = true,
        modelConfigured = configured,
        flows = flows,
        tasks = tasks,
        usage = usage,
        providers = providers,
        watchdog = watchdog,
        uptimeMs = obj["uptimeMs"]?.jsonPrimitive?.longOrNull ?: 0,
        message = obj["message"]?.jsonPrimitive?.contentOrNull
            ?: if (configured) "Local engine online" else "Local engine online · model not configured",
    )
}

internal fun parseSimpleResult(element: JsonElement): String {
    val obj = element.jsonObject
    return obj["triggerId"]?.jsonPrimitive?.contentOrNull
        ?: obj["answer"]?.jsonPrimitive?.contentOrNull
        ?: ""
}

private fun JsonObject.string(key: String): String =
    (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
