package sandrone.agentflows.android.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AgentFlowsTheme(
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = colorScheme
        ?: if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme) { content() }
}
