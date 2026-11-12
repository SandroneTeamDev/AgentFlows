package sandrone.agentflows.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import sandrone.agentflows.shared.AgentFlowsApp
import sandrone.agentflows.shared.AgentFlowsTheme
import sandrone.agentflows.shared.createEngineClient

fun main() = application {
    val client = remember { createEngineClient() }
    Window(
        title = "AgentFlows",
        onCloseRequest = {
            client.close()
            exitApplication()
        },
        state = rememberWindowState(width = 1280.dp, height = 840.dp),
    ) {
        AgentFlowsTheme {
            AgentFlowsApp(client)
        }
    }
}
