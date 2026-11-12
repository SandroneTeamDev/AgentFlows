package sandrone.agentflows.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import sandrone.agentflows.android.theme.AgentFlowsTheme
import sandrone.agentflows.android.theme.LightColorScheme
import sandrone.agentflows.android.theme.DarkColorScheme
import sandrone.agentflows.android.ui.MainPanel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = isSystemInDarkTheme()
            val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

            AgentFlowsTheme(colorScheme = colorScheme, darkContent = isDark) {
                MainPanel(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
