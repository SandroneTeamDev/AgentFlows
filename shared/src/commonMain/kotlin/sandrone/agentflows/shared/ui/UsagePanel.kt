package sandrone.agentflows.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import sandrone.agentflows.shared.EngineClient
import sandrone.agentflows.shared.UsageDayBucket
import sandrone.agentflows.shared.UsageRange
import sandrone.agentflows.shared.UsageSessionBucket
import sandrone.agentflows.shared.UsageSummary
import sandrone.agentflows.shared.nowMillis

@Composable
fun UsagePanel(
    client: EngineClient,
    modifier: Modifier = Modifier,
) {
    var range by remember { mutableStateOf(UsageRange.WEEK) }
    var summary by remember { mutableStateOf(UsageSummary()) }
    var failure by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(range, tick) {
        try {
            summary = client.usageSummary(range)
            failure = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failure = error.message ?: "usage service unreachable"
        }
        delay(10_000)
        tick += 1
    }

    val sessions = remember(summary, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) {
            summary.bySession
        } else {
            summary.bySession.filter { session ->
                session.sessionId.lowercase().contains(needle) ||
                    session.models.any { it.lowercase().contains(needle) }
            }
        }
    }

    LazyColumn(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UsageRange.entries.forEach { option ->
                    SelectablePill(option.label, range == option, onClick = { range = option })
                }
            }
        }

        if (failure != null) {
            item {
                SectionCard(title = "Usage unavailable", subtitle = "the local engine did not answer", icon = AgentIcons.Chart) {
                    Text(
                        failure.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { tick += 1 }) { Text("Retry") }
                }
            }
        }

        if (summary.totals.requests == 0) {
            item {
                EmptyState(
                    icon = AgentIcons.Chart,
                    title = "No usage recorded",
                    detail = "Token and cost accounting stays on this device. Entries appear as soon as the engine records a model call for the selected range.",
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        "Tokens",
                        formatCount(summary.totals.tokens),
                        Modifier.weight(1f),
                        caption = "${formatCount(summary.totals.tokensIn)} in · ${formatCount(summary.totals.tokensOut)} out",
                    )
                    MetricTile(
                        "Cost",
                        formatCost(summary.totals.cost),
                        Modifier.weight(1f),
                        caption = if (summary.priced) "estimated locally" else "unpriced models present",
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Calls", summary.totals.requests.toString(), Modifier.weight(1f))
                    MetricTile("Sessions", summary.bySession.size.toString(), Modifier.weight(1f))
                    MetricTile("Cached", formatCount(summary.totals.cacheHits), Modifier.weight(1f))
                }
            }
            item {
                SectionCard(title = "Daily tokens", subtitle = "selected range", icon = AgentIcons.Chart) {
                    val days = summary.byDay
                    if (days.isEmpty()) {
                        Text("Nothing recorded in this range.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        BarChart(
                            values = days.map { it.tokens.toFloat() },
                            labels = days.map { it.date.takeLast(5) },
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Context composition", subtitle = "prompt vs completion vs cache", icon = AgentIcons.Wave) {
                    CompositionRing(
                        input = summary.totals.tokensIn,
                        output = summary.totals.tokensOut,
                        cache = summary.totals.cacheHits,
                    )
                }
            }
            item {
                SectionCard(title = "Activity", subtitle = "requests per day", icon = AgentIcons.Wave) {
                    ActivityGrid(
                        dailyCounts = summary.heatmap.associate { day -> dayToEpochDay(day) to day.requests },
                        weeks = 12,
                    )
                }
            }
            item {
                SectionCard(title = "By model", subtitle = "share of recorded tokens", icon = AgentIcons.Bolt) {
                    val total = summary.totals.tokens.coerceAtLeast(1)
                    summary.byModel.forEach { entry ->
                        val fraction = entry.tokens.toFloat() / total.toFloat()
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    entry.model.ifBlank { "unassigned" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                )
                                Text(
                                    "${formatCount(entry.tokens)} · ${formatCost(entry.cost)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                            )
                            Text(
                                "${entry.requests} calls · ${entry.provider}",
                                Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(title = "By session", subtitle = "newest activity first", icon = AgentIcons.Settings) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Filter by session or model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (sessions.isEmpty()) {
                        Text(
                            "No session matches that filter.",
                            Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(sessions, key = { it.sessionId }) { session ->
                SessionRow(session)
            }
        }
    }
}

@Composable
private fun SessionRow(session: UsageSessionBucket) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                session.sessionId.take(18).ifBlank { "unassigned" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                "${session.models.joinToString(", ").ifBlank { "unknown model" }} · ${formatRelative(session.lastActivity, nowMillis())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                "${formatCount(session.tokensIn)} in · ${formatCount(session.tokensOut)} out · ${session.requests} calls",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatCost(session.cost),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun CompositionRing(input: Long, output: Long, cache: Long) {
    val total = (input + output + cache).coerceAtLeast(1L)
    val slices = listOf(
        Triple("Prompt", input, MaterialTheme.colorScheme.primary),
        Triple("Completion", output, MaterialTheme.colorScheme.tertiary),
        Triple("Cache", cache, MaterialTheme.colorScheme.secondary),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(112.dp)) {
            val stroke = size.minDimension * 0.18f
            var startAngle = -90f
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            slices.forEach { (_, value, color) ->
                val sweep = 360f * (value.toFloat() / total.toFloat())
                if (sweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                }
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(18.dp))
        Column {
            slices.forEach { (label, value, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(Modifier.size(10.dp).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${formatCount(value)} · ${(value * 100.0 / total).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun dayToEpochDay(day: UsageDayBucket): Long {
    val parts = day.date.split('-')
    if (parts.size != 3) return 0L
    val year = parts[0].toIntOrNull() ?: return 0L
    val month = parts[1].toIntOrNull() ?: return 0L
    val dayOfMonth = parts[2].toIntOrNull() ?: return 0L
    return epochDay(year, month, dayOfMonth)
}

private fun epochDay(year: Int, month: Int, day: Int): Long {
    var y = year
    var m = month
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return (era * 146097L + doe - 719468L)
}
