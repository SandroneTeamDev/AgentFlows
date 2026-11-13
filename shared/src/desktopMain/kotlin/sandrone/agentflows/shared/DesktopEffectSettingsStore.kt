package sandrone.agentflows.shared

import java.io.File

actual fun createEffectSettingsStore(): EffectSettingsStore {
    val home = System.getProperty("user.home")
    val dir = if (System.getProperty("os.name").lowercase().contains("win")) {
        File(System.getenv("LOCALAPPDATA") ?: home, "AgentFlows")
    } else if (System.getProperty("os.name").lowercase().contains("mac")) {
        File(home, "Library/Application Support/AgentFlows")
    } else {
        File(System.getenv("XDG_CONFIG_HOME") ?: File(home, ".config").absolutePath, "agentflows")
    }
    val file = File(dir, "effect.json")
    return object : EffectSettingsStore {
        override fun load(): EffectSettings {
            return if (!file.exists()) {
                EffectSettings()
            } else {
                try {
                    EffectSettings.parse(file.readText())
                } catch (ignored: Exception) {
                    EffectSettings()
                }
            }
        }

        override fun save(settings: EffectSettings) {
            dir.mkdirs()
            file.writeText(settings.wire())
        }
    }
}
