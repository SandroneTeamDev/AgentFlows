package sandrone.agentflows.shared

import java.io.File
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private object DesktopEngineHost {
    private val mutex = Mutex()
    private val dataDir: File by lazy {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> File(System.getenv("LOCALAPPDATA") ?: home, "AgentFlows")
            os.contains("mac") -> File(home, "Library/Application Support/AgentFlows")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "agentflows")
        }
    }

    suspend fun connection(): EngineConnection = mutex.withLock {
        withContext(Dispatchers.IO) {
            val descriptor = File(dataDir, "connection.json")
            if (ready(descriptor)) return@withContext readConnection(descriptor)
            val resources = System.getProperty("compose.application.resources.dir")?.let(::File)
            val engineDir = listOfNotNull(
                resources?.let { File(it, "engine") },
                File(System.getProperty("user.dir"), "engine"),
                File(System.getProperty("user.dir"), "../engine"),
            ).firstOrNull { File(it, "dist/main.js").isFile }
                ?: error("The local engine has not been built or bundled")
            val windows = System.getProperty("os.name").lowercase().contains("win")
            val bundledNode = resources?.let { File(it, "runtime/" + if (windows) "node.exe" else "node") }
            val node = bundledNode?.takeIf { it.isFile }?.absolutePath ?: "node"
            dataDir.mkdirs()
            val process = ProcessBuilder(node, File(engineDir, "dist/main.js").absolutePath, "--data-dir", dataDir.absolutePath)
            listOf("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy").forEach { process.environment().remove(it) }
            process.environment()["NO_PROXY"] = "localhost,127.0.0.1"
            process.redirectErrorStream(true)
            process.redirectOutput(ProcessBuilder.Redirect.appendTo(File(dataDir, "engine.log")))
            val child = process.start()
            repeat(80) {
                if (ready(descriptor)) return@withContext readConnection(descriptor)
                check(child.isAlive) { "The local engine exited during startup" }
                delay(100)
            }
            error("The local engine did not become ready")
        }
    }

    private fun ready(descriptor: File): Boolean {
        if (!descriptor.isFile) return false
        return try {
            val connection = readConnection(descriptor)
            val http = URL("http://127.0.0.1:${connection.port}/health").openConnection(Proxy.NO_PROXY) as HttpURLConnection
            try {
                http.connectTimeout = 250
                http.readTimeout = 250
                http.setRequestProperty("Authorization", "Bearer ${connection.token}")
                http.responseCode == 200
            } finally {
                http.disconnect()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun effectStore(): EffectPrefsStore {
        val file = File(dataDir, "effect-prefs.json")
        return object : EffectPrefsStore {
            override fun load(): EffectPrefs =
                if (file.isFile) decodeEffectPrefs(file.readText()) else EffectPrefs()

            override fun save(prefs: EffectPrefs) {
                file.parentFile?.mkdirs()
                file.writeText(prefs.encode())
            }
        }
    }
}

actual fun createEngineClient(): EngineClient = HttpEngineClient { DesktopEngineHost.connection() }

actual fun createEffectPrefsStore(): EffectPrefsStore = DesktopEngineHost.effectStore()
