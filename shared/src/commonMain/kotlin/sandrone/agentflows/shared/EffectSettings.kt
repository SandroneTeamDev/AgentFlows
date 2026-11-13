package sandrone.agentflows.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class EffectMode { Blur, Glass }

data class EffectSettings(
    val enabled: Boolean = true,
    val mode: EffectMode = EffectMode.Blur,
    val blurRadiusDp: Int = 12,
) {
    fun wire(): String =
        """{"enabled":$enabled,"mode":"${mode.name}","blurRadiusDp":$blurRadiusDp}"""

    companion object {
        fun parse(raw: String?): EffectSettings {
            if (raw.isNullOrBlank()) return EffectSettings()
            if (raw.startsWith('{')) {
                return try {
                    val obj = Json.parseToJsonElement(raw).jsonObject
                    EffectSettings(
                        enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                        mode = when (obj["mode"]?.jsonPrimitive?.contentOrNull) {
                            "Glass" -> EffectMode.Glass
                            else -> EffectMode.Blur
                        },
                        blurRadiusDp = obj["blurRadiusDp"]?.jsonPrimitive?.intOrNull?.coerceIn(4, 40) ?: 12,
                    )
                } catch (ignored: Exception) {
                    EffectSettings()
                }
            }
            val parts = raw.split('/')
            if (parts.size == 3) {
                return EffectSettings(
                    enabled = parts[0].toBoolean(),
                    mode = when (parts[1]) {
                        "Glass" -> EffectMode.Glass
                        else -> EffectMode.Blur
                    },
                    blurRadiusDp = parts[2].toIntOrNull()?.coerceIn(4, 40) ?: 12,
                )
            }
            return EffectSettings()
        }
    }
}

interface EffectSettingsStore {
    fun load(): EffectSettings
    fun save(settings: EffectSettings)
}

expect fun createEffectSettingsStore(): EffectSettingsStore

class EffectSettingsController(private val store: EffectSettingsStore) {
    val state: MutableState<EffectSettings> = mutableStateOf(store.load())

    fun update(transform: (EffectSettings) -> EffectSettings) {
        state.value = transform(state.value)
        store.save(state.value)
    }
}

@Composable
fun rememberEffectSettings(): EffectSettingsController =
    remember { EffectSettingsController(createEffectSettingsStore()) }
