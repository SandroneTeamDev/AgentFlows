package sandrone.agentflows.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import sandrone.agentflows.android.theme.AgentFlowsTheme
import sandrone.agentflows.android.ui.MainPanel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentFlowsTheme {
                MainPanel(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
