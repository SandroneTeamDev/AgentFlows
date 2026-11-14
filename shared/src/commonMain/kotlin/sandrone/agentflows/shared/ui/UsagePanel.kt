package sandrone.agentflows.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sandrone.agentflows.shared.UsageEntry
import sandrone.agentflows.shared.nowMillis

private data class UsageTotals(
    val tokensIn: Long,
    val tokensOut: Long,
    val cost: Double,
    val sessions: Int,
) {
    val tokens: Long get() = tokensIn + tokensOut
}

private data class ModelShare(
    val model: String,
    val tokens: Long,
    val cost: Double,
    val calls: Int,
)

@Composable
fun UsagePanel(
    usage: List<UsageEntry>,
    modifier: Modifier = Modifier,
) {
    if (usage.isEmpty()) {
        EmptyState(
            icon = AgentIcons.Chart,
            title = "No usage recorded",
            detail = "Token and cost accounting is local-only. Entries appear here as soon as the engine records a model call.",
            modifier = modifier,
        )
        return
    }

    val totals = remember(usage) {
        UsageTotals(
            tokensIn = usage.sumOf { it.tokensIn },
            tokensOut = usage.sumOf { it.tokensOut },
            cost = usage.sumOf { it.cost },
            sessions = usage.map { it.sessionId }.toSet().size,
        )
    }
    val shares = remember(usage) {
        usage.groupBy { it.model }.map { (model, entries) ->
            ModelShare(
                model = model.ifBlank { "unassigned" },
                tokens = entries.sumOf { it.tokensIn + it.tokensOut },
                cost = entries.sumOf { it.cost },
                calls = entries.size,
            )
        }.sortedByDescending { it.tokens }
    }
    val daily = remember(usage) {
        usage.groupBy { it.timestamp / MILLIS_PER_DAY }.mapValues { (_, entries) -> entries.sumOf { it.tokensIn + it.tokensOut } }
    }
    val requestCounts = remember(usage) {
        usage.groupBy { it.timestamp / MILLIS_PER_DAY }.mapValues { (_, entries) -> entries.size }
    }
    val lastSeven = remember(usage) {
        val today = nowMillis() / MILLIS_PER_DAY
        (6 downTo 0).map { offset ->
            val day = today - offset
            day to (daily[day] ?: 0L)
        }
    }

    LazyColumn(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Tokens", formatCount(totals.tokens), Modifier.weight(1f), caption = "${formatCount(totals.tokensIn)} in · ${formatCount(totals.tokensOut)} out")
                MetricTile("Cost", formatCost(totals.cost), Modifier.weight(1f), caption = "estimated locally")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Calls", usage.size.toString(), Modifier.weight(1f))
                MetricTile("Sessions", totals.sessions.toString(), Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "Last 7 days", subtitle = "tokens per day", icon = AgentIcons.Chart) {
                BarChart(
                    values = lastSeven.map { it.second.toFloat() },
                    labels = lastSeven.map { formatShortDay(it.first * MILLIS_PER_DAY) },
                )
            }
        }
        item {
            SectionCard(title = "Activity", subtitle = "requests per day, last 12 weeks", icon = AgentIcons.Wave) {
                ActivityGrid(requestCounts, weeks = 12)
            }
        }
        item {
            SectionCard(title = "By model", subtitle = "share of recorded tokens", icon = AgentIcons.Bolt) {
                shares.forEach { share ->
                    val fraction = if (totals.tokens > 0) share.tokens.toFloat() / totals.tokens.toFloat() else 0f
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(share.model, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                "${formatCount(share.tokens)} · ${formatCost(share.cost)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                        )
                        Text(
                            "${share.calls} calls",
                            Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = "Recent entries", subtitle = "newest first", icon = AgentIcons.Chart) {
                if (usage.isEmpty()) {
                    Text("Nothing recorded yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(usage.takeLast(20).asReversed(), key = { "${it.sessionId}-${it.timestamp}-${it.model}" }) { entry ->
            UsageRow(entry)
        }
    }
}

@Composable
private fun UsageRow(entry: UsageEntry) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.model.ifBlank { "unassigned" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "${formatCount(entry.tokensIn)} in · ${formatCount(entry.tokensOut)} out · ${formatRelative(entry.timestamp, nowMillis())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatCost(entry.cost),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

