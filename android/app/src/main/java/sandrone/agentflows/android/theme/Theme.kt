package sandrone.agentflows.android.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import android.content.Context

@Composable
fun AgentFlowsTheme(
    colorScheme: androidx.compose.material3.ColorScheme?,
    darkContent: Boolean = false,
    content: androidx.compose.runtime.ComposableScope.() -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveScheme = colorScheme
        ?: if (androidx.compose.material3.isSystemInDarkTheme(context)) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }

    val mdThemeColorScheme = androidx.compose.material3.ColorScheme(
        primary = effectiveScheme.primary,
        onPrimary = effectiveScheme.onPrimary,
        primaryContainer = effectiveScheme.primaryContainer,
        onPrimaryContainer = effectiveScheme.onPrimaryContainer,
        secondary = effectiveScheme.secondary,
        onSecondary = effectiveScheme.onSecondary,
        secondaryContainer = effectiveScheme.secondaryContainer,
        onSecondaryContainer = effectiveScheme.onSecondaryContainer,
        tertiary = effectiveScheme.tertiary,
        onTertiary = effectiveScheme.onTertiary,
        tertiaryContainer = effectiveScheme.tertiaryContainer,
        onTertiaryContainer = effectiveScheme.onTertiaryContainer,
        error = effectiveScheme.error,
        errorContainer = effectiveScheme.errorContainer,
        onErrorContainer = effectiveScheme.onErrorContainer,
        background = effectiveScheme.background,
        onBackground = effectiveScheme.onBackground,
        surface = effectiveScheme.surface,
        onSurface = effectiveScheme.onSurface,
        surfaceVariant = effectiveScheme.surfaceVariant,
        onSurfaceVariant = effectiveScheme.onSurfaceVariant,
        outline = effectiveScheme.outline,
        outlineVariant = effectiveScheme.outlineVariant,
        shadow = effectiveScheme.shadow,
        scrim = effectiveScheme.scrim,
        inverseSurface = effectiveScheme.inverseSurface,
        inverseOnSurface = effectiveScheme.inverseOnSurface,
        inversePrimary = effectiveScheme.inversePrimary,
        surfaceDisabled = effectiveScheme.surfaceDisabled,
        onSurfaceDisabled = effectiveScheme.onSurfaceDisabled,
    )

    CompositionLocalProvider(
        androidx.compose.material3.LocalColorScheme provides mdThemeColorScheme,
        androidx.compose.material3.LocalContentColor provides
            if (darkContent) mdThemeColorScheme.onBackground else mdThemeColorScheme.onSurface,
    ) {
        MaterialTheme(
            colorScheme = mdThemeColorScheme,
            typography = androidx.compose.material3.Typography,
            shapes = androidx.compose.material3.Shapes,
            content = content,
        )
    }
}
