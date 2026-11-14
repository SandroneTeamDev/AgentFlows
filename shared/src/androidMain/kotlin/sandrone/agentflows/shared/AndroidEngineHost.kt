package sandrone.agentflows.shared

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object AndroidEngineHost {
    private var directory: File? = null
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        val app = context.applicationContext
        directory = File(app.filesDir, "engine")
        prefs = app.getSharedPreferences("agentflows", Context.MODE_PRIVATE)
    }

    internal fun connection(): EngineConnection =
        readConnection(File(checkNotNull(directory) { "Android engine host is not initialized" }, "connection.json"))

    internal fun effectStore(): EffectPrefsStore {
        val store = prefs ?: return InMemoryEffectPrefsStore()
        return object : EffectPrefsStore {
            override fun load(): EffectPrefs = decodeEffectPrefs(store.getString(KEY, null))

            override fun save(prefs: EffectPrefs) {
                store.edit().putString(KEY, prefs.encode()).apply()
            }
        }
    }

    private const val KEY = "effect_prefs"
}

actual fun createEngineClient(): EngineClient = HttpEngineClient { AndroidEngineHost.connection() }

actual fun createEffectPrefsStore(): EffectPrefsStore = AndroidEngineHost.effectStore()
