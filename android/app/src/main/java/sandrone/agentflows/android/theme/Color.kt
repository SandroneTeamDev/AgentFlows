package sandrone.agentflows.android.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DynamicColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import android.content.Context

private val PurpleKeyColor = Color(0xFF6750A4)
private val PurpleKeyColorDark = Color(0xFFD0BCFF)

private val SeedColorDark = Color(0xFF6750A4)

private val LightColorSchemeLight = ColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1F1B31),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF2EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    surfaceDisabled = Color(0xFFF9F5F9),
    onSurfaceDisabled = Color(0xFF8C8790),
)

private val DarkColorSchemeDark = ColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFF690005),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF6750A4),
    surfaceDisabled = Color(0xFF49454F),
    onSurfaceDisabled = Color(0xFF8C8790),
)

@Stable
val LightColorScheme: ColorScheme = LightColorSchemeLight

@Stable
val DarkColorScheme: ColorScheme = DarkColorSchemeDark

@Composable
fun MaterialThemeColorScheme(context: Context, dark: Boolean): ColorScheme {
    return if (dark) {
        dynamicDarkColorScheme(context, SeedColorDark)
    } else {
        dynamicLightColorScheme(context, SeedColorDark)
    }
}

fun MD3ColorTokens(colorScheme: ColorScheme): Map<String, String> {
    return mapOf(
        "primary" to colorScheme.primary.toArgb().toString(),
        "on_primary" to colorScheme.onPrimary.toArgb().toString(),
        "primary_container" to colorScheme.primaryContainer.toArgb().toString(),
        "secondary" to colorScheme.secondary.toArgb().toString(),
        "on_secondary" to colorScheme.onSecondary.toArgb().toString(),
        "background" to colorScheme.background.toArgb().toString(),
        "on_background" to colorScheme.onBackground.toArgb().toString(),
        "surface" to colorScheme.surface.toArgb().toString(),
        "on_surface" to colorScheme.onSurface.toArgb().toString(),
        "surface_variant" to colorScheme.surfaceVariant.toArgb().toString(),
        "outline" to colorScheme.outline.toArgb().toString(),
    )
}
