package sandrone.agentflows.shared

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.Haze
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.material3.material3

enum class EffectMode { DISABLED, BLUR, GLASS }

data class EffectPrefs(
    val mode: EffectMode = EffectMode.DISABLED,
    val blurRadiusDp: Float = 12f,
    val glassTintAlpha: Float = 0.28f,
)

interface EffectPrefsStore {
    fun load(): EffectPrefs
    fun save(prefs: EffectPrefs)
}

class InMemoryEffectPrefsStore(initial: EffectPrefs = EffectPrefs()) : EffectPrefsStore {
    private var current = initial
    override fun load(): EffectPrefs = current
    override fun save(prefs: EffectPrefs) {
        current = prefs
    }
}

expect fun createEffectPrefsStore(): EffectPrefsStore

fun EffectPrefs.encode(): String = "${mode.name}|$blurRadiusDp|$glassTintAlpha"

fun decodeEffectPrefs(text: String?): EffectPrefs {
    val stored = text?.trim().orEmpty()
    if (stored.isEmpty()) return EffectPrefs()
    val parts = stored.split('|')
    val mode = parts.getOrNull(0)?.let { raw -> runCatching { EffectMode.valueOf(raw) }.getOrNull() } ?: EffectMode.DISABLED
    val radius = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 64f) ?: 12f
    val alpha = parts.getOrNull(2)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.28f
    return EffectPrefs(mode = mode, blurRadiusDp = radius, glassTintAlpha = alpha)
}

internal val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun EffectRoot(content: @Composable () -> Unit) {
    val hazeState = rememberHazeState()
    Haze(hazeState) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            content()
        }
    }
}

@Composable
fun Modifier.effectSurface(prefs: EffectPrefs, containerColor: Color): Modifier {
    val hazeState = LocalHazeState.current
    if (hazeState == null) return this.then(Modifier.background(containerColor))

    return when (prefs.mode) {
        EffectMode.DISABLED -> this.then(Modifier.background(containerColor))
        EffectMode.BLUR -> this.then(
            Modifier.hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle.Material3 {
                    blurRadius(prefs.blurRadiusDp.dp)
                },
            ),
        )
        EffectMode.GLASS -> this.then(
            Modifier.hazeGlass(
                input = HazeInput.Sources(hazeState),
                style = GlassStyle.material3(
                    containerColor = containerColor,
                    tint = containerColor.copy(alpha = prefs.glassTintAlpha),
                ),
            ),
        )
    }
}
