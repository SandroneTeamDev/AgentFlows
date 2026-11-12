package sandrone.agentflows.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

data class FlowNode(
    val id: String,
    val type: String,
    val label: String,
    val timestamp: Long,
)

@Composable
fun MainPanel(modifier: Modifier = Modifier) {
    AgentFlowsTheme {
        val nodes = remember {
            listOf(
                FlowNode("1", "trigger", "GitHub PR #42 opened", 0L),
                FlowNode("2", "router_decision", "Dispatch to Worker-1", 0L),
                FlowNode("3", "worker_start", "Worker-1: Review PR #42", 0L),
                FlowNode("4", "tool_call", "git diff main..pr-42", 0L),
                FlowNode("5", "worker_done", "Worker-1: Review complete", 0L),
            )
        }
        var selectedPanel by remember { mutableStateOf("chat") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "AgentFlows") },
                )
            },
        ) { paddingValues ->
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Sidebar(
                    selected = selectedPanel,
                    onSelect = { selectedPanel = it },
                )
                Spacer(modifier = Modifier.width(1.dp))
                MainContent(selectedPanel, nodes)
            }
        }
    }
}

@Composable
private fun Sidebar(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val items = listOf(
        "chat" to "Chat",
        "files" to "Files",
        "terminal" to "Terminal",
        "git" to "Git",
        "progress" to "Progress",
        "usage" to "Usage",
        "plugins" to "Plugins",
        "settings" to "Settings",
    )
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(8.dp),
    ) {
        items.forEach { (key, label) ->
            val isSelected = selected == key
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onSelect(key) },
                    style = if (isSelected) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    panel: String,
    nodes: List<FlowNode>,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(16.dp),
    ) {
        Text(
            text = panel.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (panel == "progress" || panel == "chat") {
            nodes.forEach { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = node.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else {
            Text(
                text = "${panel} panel coming soon",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
