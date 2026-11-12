package sandrone.agentflows.shared

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal data class EngineConnection(val port: Int, val token: String)

interface EngineClient {
    suspend fun snapshot(): WorkspaceSnapshot
    suspend fun submit(text: String)
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

data class TaskEntry(val id: String, val title: String, val status: String)

data class WorkspaceSnapshot(
    val connected: Boolean = false,
    val modelConfigured: Boolean = false,
    val flows: List<FlowEntry> = emptyList(),
    val tasks: List<TaskEntry> = emptyList(),
    val message: String = "Connecting to the local engine…",
)

internal fun parseSnapshot(element: JsonElement): WorkspaceSnapshot {
    val obj = element.jsonObject
    val status = obj["status"] as? JsonObject
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
    val configured = status?.get("modelConfigured")?.jsonPrimitive?.booleanOrNull ?: false
    return WorkspaceSnapshot(
        connected = true,
        modelConfigured = configured,
        flows = flows,
        tasks = tasks,
        message = if (configured) "Local engine online" else "Local engine online · model not configured",
    )
}

private fun JsonObject.string(key: String): String =
    (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
