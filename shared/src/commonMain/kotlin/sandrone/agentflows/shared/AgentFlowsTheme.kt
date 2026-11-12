package sandrone.agentflows.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MotionTokens {
    const val ColorTransitionMillis = 250
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF62528A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDDFF),
    onPrimaryContainer = Color(0xFF211238),
    secondary = Color(0xFF635B71),
    secondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFF7E5260),
    tertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFFFFF7FF),
    surface = Color(0xFFFFF7FF),
    surfaceContainer = Color(0xFFF2ECF7),
    surfaceContainerHigh = Color(0xFFECE6F1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD1BCFF),
    primaryContainer = Color(0xFF493B70),
    secondary = Color(0xFFCCC2DC),
    secondaryContainer = Color(0xFF4B435A),
    tertiary = Color(0xFFF0B8C8),
    tertiaryContainer = Color(0xFF633B48),
    background = Color(0xFF151218),
    surface = Color(0xFF151218),
    surfaceContainer = Color(0xFF211E26),
    surfaceContainerHigh = Color(0xFF2B2830),
)

@Composable
fun AgentFlowsTheme(colorScheme: ColorScheme? = null, content: @Composable () -> Unit) {
    val target = colorScheme ?: if (isSystemInDarkTheme()) DarkColors else LightColors
    @Composable
    fun color(value: Color): Color {
        val animated by animateColorAsState(value, tween(MotionTokens.ColorTransitionMillis))
        return animated
    }
    val scheme = target.copy(
        primary = color(target.primary),
        onPrimary = color(target.onPrimary),
        primaryContainer = color(target.primaryContainer),
        onPrimaryContainer = color(target.onPrimaryContainer),
        secondary = color(target.secondary),
        secondaryContainer = color(target.secondaryContainer),
        tertiary = color(target.tertiary),
        tertiaryContainer = color(target.tertiaryContainer),
        background = color(target.background),
        onBackground = color(target.onBackground),
        surface = color(target.surface),
        onSurface = color(target.onSurface),
        surfaceContainer = color(target.surfaceContainer),
        surfaceContainerHigh = color(target.surfaceContainerHigh),
        outline = color(target.outline),
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        content = content,
    )
}
