package sandrone.agentflows.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import sandrone.agentflows.android.theme.AgentFlowsTheme
import sandrone.agentflows.android.theme.LightColorScheme
import sandrone.agentflows.android.theme.DarkColorScheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = androidx.compose.material3.isSystemInDarkTheme(LocalContext.current)
            val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

            AgentFlowsTheme(colorScheme = colorScheme, darkContent = isDark) {
                MainPanel(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun androidx.compose.runtime.CompositionScope.MainPanel(
    modifier: Modifier = Modifier,
) {
    sandrone.agentflows.android.ui.MainPanel(
        modifier = modifier,
    )
}
