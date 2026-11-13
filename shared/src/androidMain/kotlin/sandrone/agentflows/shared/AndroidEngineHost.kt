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

    internal fun settingsStore(): EffectSettingsStore {
        val store = prefs
            ?: return MemoryEffectSettingsStore
        return object : EffectSettingsStore {
            override fun load(): EffectSettings =
                EffectSettings.parse(store.getString("effect", null))

            override fun save(settings: EffectSettings) {
                val editor = store.edit()
                editor.putString("effect", settings.wire())
                editor.apply()
            }
        }
    }
}

internal val MemoryEffectSettingsStore = object : EffectSettingsStore {
    private var current = EffectSettings()
    override fun load(): EffectSettings = current
    override fun save(settings: EffectSettings) {
        current = settings
    }
}

actual fun createEngineClient(): EngineClient = HttpEngineClient { AndroidEngineHost.connection() }

actual fun createEffectSettingsStore(): EffectSettingsStore = AndroidEngineHost.settingsStore()
