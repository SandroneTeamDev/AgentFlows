package sandrone.agentflows.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import sandrone.agentflows.shared.AgentFlowsApp
import sandrone.agentflows.shared.createEngineClient

fun main() = application {
    val client = remember { createEngineClient() }
    Window(
        title = "AgentFlows",
        icon = painterResource("agentflows-icon.png"),
        onCloseRequest = {
            client.close()
            exitApplication()
        },
        state = rememberWindowState(width = 1400.dp, height = 900.dp),
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(400, 620)
        }
        AgentFlowsApp(client)
    }
}
