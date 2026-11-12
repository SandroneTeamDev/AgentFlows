package sandrone.agentflows.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import sandrone.agentflows.shared.AgentFlowsApp
import sandrone.agentflows.shared.AgentFlowsTheme
import sandrone.agentflows.shared.AndroidEngineHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidEngineHost.initialize(applicationContext)
        setContent {
            val dynamicColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemInDarkTheme()) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            } else null
            AgentFlowsTheme(dynamicColors) {
                AgentFlowsApp()
            }
        }
    }
}
