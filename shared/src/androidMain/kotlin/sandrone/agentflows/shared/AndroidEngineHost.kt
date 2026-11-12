package sandrone.agentflows.shared

import android.content.Context
import java.io.File

object AndroidEngineHost {
    private var directory: File? = null

    fun initialize(context: Context) {
        directory = File(context.applicationContext.filesDir, "engine")
    }

    internal fun connection(): EngineConnection =
        readConnection(File(checkNotNull(directory) { "Android engine host is not initialized" }, "connection.json"))
}

actual fun createEngineClient(): EngineClient = HttpEngineClient { AndroidEngineHost.connection() }
