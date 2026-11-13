package sandrone.agentflows.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.material3.material3

@Composable
fun Modifier.effectSurface(
    hazeState: HazeState,
    settings: EffectSettings,
    shape: RoundedCornerShape,
    containerColor: Color,
): Modifier {
    if (!settings.enabled) return background(containerColor, shape)
    val input = HazeInput.Sources(hazeState)
    return when (settings.mode) {
        EffectMode.Blur -> hazeBlur(
            input = input,
            style = HazeBlurStyle.Material3 {
                blurRadius(settings.blurRadiusDp.dp)
            },
        )

        EffectMode.Glass -> hazeGlass(
            input = input,
            style = GlassStyle.material3(
                containerColor = containerColor,
                tint = containerColor.copy(alpha = 0.3f),
            ).then {
                optics {
                    copy(
                        blurRadius = OpticalSizeValue.Fixed(16.dp),
                        depth = OpticalSizeValue.Fixed(0.5f),
                    )
                }
                shape(shape)
            },
        )
    }
}
