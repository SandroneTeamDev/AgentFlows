package sandrone.agentflows.shared

import java.io.File
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal class HttpEngineClient(private val connection: suspend () -> EngineConnection) : EngineClient {
    private val counter = AtomicLong()

    override suspend fun snapshot(): WorkspaceSnapshot =
        parseSnapshot(rpc("snapshot", JsonObject(emptyMap())))

    override suspend fun submit(text: String): String =
        parseSubmitResult(rpc("trigger.submit", buildJsonObject { put("text", text) }))

    override suspend fun cancelTask(workerId: String) {
        rpc("task.cancel", buildJsonObject { put("workerId", workerId) })
    }

    override suspend fun askWatchdog(question: String): String =
        parseWatchdogAnswer(rpc("watchdog.query", buildJsonObject { put("question", question) }))

    override suspend fun registerProvider(id: String, baseUrl: String, models: List<String>) {
        rpc(
            "model.register",
            buildJsonObject {
                putJsonObject("config") {
                    put("id", id)
                    put("kind", "openai")
                    put("baseUrl", baseUrl)
                    put("models", buildJsonArray { models.forEach { add(JsonPrimitive(it)) } })
                }
            },
        )
    }

    override suspend fun probeModels(): Boolean =
        parseProbeResult(rpc("model.probe", JsonObject(emptyMap())))

    override fun close() = Unit

    private suspend fun rpc(method: String, params: JsonObject): JsonElement = withContext(Dispatchers.IO) {
        val descriptor = connection()
        require(descriptor.port in 1..65535 && descriptor.token.matches(Regex("[a-f0-9]{64}")))
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", counter.incrementAndGet())
            put("method", method)
            put("params", params)
        }.toString().toByteArray(Charsets.UTF_8)
        val http = URL("http://127.0.0.1:${descriptor.port}/rpc").openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            http.requestMethod = "POST"
            http.connectTimeout = 2500
            http.readTimeout = 15000
            http.doOutput = true
            http.setRequestProperty("Content-Type", "application/json")
            http.setRequestProperty("Authorization", "Bearer ${descriptor.token}")
            http.setFixedLengthStreamingMode(request.size)
            http.outputStream.use { it.write(request) }
            check(http.responseCode == 200) { "Local engine request failed" }
            val response = http.inputStream.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
            check(response["error"] == null) { "Local engine RPC failed" }
            response["result"] ?: error("Local engine returned no result")
        } finally {
            http.disconnect()
        }
    }
}

internal fun readConnection(file: File): EngineConnection {
    val obj = Json.parseToJsonElement(file.readText()).jsonObject
    require(obj["host"]?.jsonPrimitive?.content == "127.0.0.1")
    return EngineConnection(obj.getValue("port").jsonPrimitive.int, obj.getValue("token").jsonPrimitive.content)
}
