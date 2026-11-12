package sandrone.agentflows.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sandrone.agentflows.android.theme.AgentFlowsTheme

@Composable
fun MainPanel(modifier: Modifier = Modifier) {
    AgentFlowsTheme {
        var panel by remember { mutableStateOf("chat") }
        Scaffold(
            topBar = { TopAppBar(title = { Text("AgentFlows") }) },
        ) { pad ->
            Column(
                modifier = modifier.fillMaxSize().padding(pad),
            ) {
                listOf("Chat", "Files", "Terminal", "Git", "Progress", "Usage", "Plugins", "Settings").forEach { label ->
                    val selected = panel == label
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (selected) "$label (active)" else label,
                            modifier = Modifier
                                .padding(12.dp)
                                .clickable { panel = label },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
