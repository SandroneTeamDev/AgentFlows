package sandrone.agentflows.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sandrone.agentflows.shared.AgentFlowEntry
import sandrone.agentflows.shared.nowMillis

@Composable
fun FlowsPanel(
    flows: List<AgentFlowEntry>,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf<String>("all") }
    val filters = remember {
        listOf(
            "all" to "All",
            "trigger" to "Trigger",
            "router_decision" to "Router",
            "worker_start" to "Worker",
            "tool_call" to "Tool",
            "error" to "Alert",
        )
    }
    val visible = remember(flows, filter) {
        if (filter == "all") flows else flows.filter { it.type == filter }
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { (key, label) ->
                SelectablePill(label = label, selected = filter == key, onClick = { filter = key })
            }
        }
        if (visible.isEmpty()) {
            EmptyState(
                icon = AgentIcons.Bolt,
                title = "No events yet",
                detail = "Triggers, Router decisions, Worker starts, tool calls and alerts appear here as they happen.",
            )
            return
        }
        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(visible.asReversed(), key = { it.id }) { entry -> FlowRow(entry) }
        }
    }
}

@Composable
private fun FlowRow(entry: AgentFlowEntry) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val tone = flowToneOf(entry.type)
    Row(Modifier.fillMaxWidth()) {
        Column(
            Modifier.width(26.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .size(10.dp)
                    .background(nodeColor(tone), CircleShape),
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .height(72.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )
        }
        Spacer(Modifier.width(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f).clickable { expanded = !expanded },
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TonePill(tone, flowLabel(entry.type))
                    Spacer(Modifier.width(8.dp))
                    TimestampLabel(entry.timestamp)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (expanded) AgentIcons.ChevronUp else AgentIcons.ChevronDown,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(entry.label, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall)
                ExpandableArea(expanded) {
                    Box(Modifier.padding(top = 12.dp)) { MonospaceBlock(entry.details) }
                }
            }
        }
    }
}

@Composable
private fun nodeColor(tone: FlowTone) = when (tone) {
    FlowTone.ALERT -> MaterialTheme.colorScheme.error
    FlowTone.ROUTER -> MaterialTheme.colorScheme.primary
    FlowTone.WORKER -> MaterialTheme.colorScheme.secondary
    FlowTone.TRIGGER -> MaterialTheme.colorScheme.tertiary
    FlowTone.TOOL -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.outline
}

@Composable
fun FlowSummaryStrip(flows: List<AgentFlowEntry>, modifier: Modifier = Modifier) {
    val triggers = flows.count { it.type == "trigger" }
    val workers = flows.count { it.type == "worker_start" }
    val tools = flows.count { it.type == "tool_call" }
    val alerts = flows.count { it.type == "error" || it.type == "watchdog_alert" }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile("Triggers", triggers.toString(), Modifier.weight(1f))
        MetricTile("Workers", workers.toString(), Modifier.weight(1f))
        MetricTile("Tool calls", tools.toString(), Modifier.weight(1f))
        MetricTile("Alerts", alerts.toString(), Modifier.weight(1f), accent = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun RecentFlowPreview(flows: List<AgentFlowEntry>, modifier: Modifier = Modifier) {
    val latest = flows.lastOrNull()
    if (latest == null) {
        Text(
            "No flow events recorded in this session.",
            modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TonePill(flowToneOf(latest.type), flowLabel(latest.type))
        Spacer(Modifier.width(8.dp))
        Text(latest.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(
            formatRelative(latest.timestamp, nowMillis()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
