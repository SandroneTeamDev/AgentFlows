package sandrone.agentflows.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MotionTokens {
    const val ShellColorMillis = 250
    const val ContentMillis = 220
}

object SpringTokens {
    fun enter() = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun settle() = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF62528A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF211238),
    inversePrimary = Color(0xFFD1BCFF),
    secondary = Color(0xFF635B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1F1929),
    tertiary = Color(0xFF7E5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF31101D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceTint = Color(0xFF62528A),
    inverseSurface = Color(0xFF313036),
    inverseOnSurface = Color(0xFFF3EFF7),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFCAC4CF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFDF7FF),
    surfaceDim = Color(0xFFDED8E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F1FA),
    surfaceContainer = Color(0xFFF2ECF4),
    surfaceContainerHigh = Color(0xFFECE6EE),
    surfaceContainerHighest = Color(0xFFE6E0E9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD1BCFF),
    onPrimary = Color(0xFF33234F),
    primaryContainer = Color(0xFF493B70),
    onPrimaryContainer = Color(0xFFE9DDFF),
    inversePrimary = Color(0xFF62528A),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF342D41),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFFF0B8C8),
    onTertiary = Color(0xFF482532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD9E2),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    surfaceTint = Color(0xFFD1BCFF),
    inverseSurface = Color(0xFFE6E1E9),
    inverseOnSurface = Color(0xFF313036),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF48454E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3B383E),
    surfaceDim = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0E0D11),
    surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainer = Color(0xFF201F24),
    surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A),
)

private val AgentShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AgentTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        bodySmall = base.bodySmall.copy(fontSize = 12.sp),
    )
}

val MonoFamily: FontFamily = FontFamily.Monospace

@Composable
fun AgentFlowsTheme(
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val target = colorScheme ?: if (isSystemInDarkTheme()) DarkColors else LightColors
    val animated = animate(target)
    MaterialTheme(
        colorScheme = animated,
        shapes = AgentShapes,
        typography = AgentTypography,
        content = content,
    )
}

@Composable
private fun animate(target: ColorScheme): ColorScheme {
    @Composable
    fun role(value: Color): Color {
        val state by animateColorAsState(value, tween(MotionTokens.ShellColorMillis))
        return state
    }
    return target.copy(
        primary = role(target.primary),
        onPrimary = role(target.onPrimary),
        primaryContainer = role(target.primaryContainer),
        onPrimaryContainer = role(target.onPrimaryContainer),
        secondary = role(target.secondary),
        onSecondary = role(target.onSecondary),
        secondaryContainer = role(target.secondaryContainer),
        onSecondaryContainer = role(target.onSecondaryContainer),
        tertiary = role(target.tertiary),
        onTertiary = role(target.onTertiary),
        tertiaryContainer = role(target.tertiaryContainer),
        onTertiaryContainer = role(target.onTertiaryContainer),
        background = role(target.background),
        onBackground = role(target.onBackground),
        surface = role(target.surface),
        onSurface = role(target.onSurface),
        surfaceVariant = role(target.surfaceVariant),
        onSurfaceVariant = role(target.onSurfaceVariant),
        surfaceContainerLowest = role(target.surfaceContainerLowest),
        surfaceContainerLow = role(target.surfaceContainerLow),
        surfaceContainer = role(target.surfaceContainer),
        surfaceContainerHigh = role(target.surfaceContainerHigh),
        surfaceContainerHighest = role(target.surfaceContainerHighest),
        outline = role(target.outline),
        outlineVariant = role(target.outlineVariant),
        error = role(target.error),
        onError = role(target.onError),
        errorContainer = role(target.errorContainer),
        onErrorContainer = role(target.onErrorContainer),
    )
}
