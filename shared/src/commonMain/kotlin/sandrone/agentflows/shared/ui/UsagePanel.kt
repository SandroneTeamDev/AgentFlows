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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import sandrone.agentflows.shared.EngineClient
import sandrone.agentflows.shared.UsageBucket
import sandrone.agentflows.shared.UsageCost
import sandrone.agentflows.shared.UsageDayBucket
import sandrone.agentflows.shared.UsageGranularity
import sandrone.agentflows.shared.UsageRange
import sandrone.agentflows.shared.UsageSessionBucket
import sandrone.agentflows.shared.UsageSummary
import sandrone.agentflows.shared.UsageToolBucket
import sandrone.agentflows.shared.nowMillis

@Composable
fun UsagePanel(
    client: EngineClient,
    modifier: Modifier = Modifier,
) {
    var range by remember { mutableStateOf(UsageRange.WEEK) }
    var granularity by remember { mutableStateOf(UsageGranularity.DAY) }
    var summary by remember { mutableStateOf(UsageSummary()) }
    var tools by remember { mutableStateOf<List<UsageToolBucket>>(emptyList()) }
    var failure by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    var exportNotice by remember { mutableStateOf<String?>(null) }
    var exportRequest by remember { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(exportRequest) {
        if (exportRequest == 0) return@LaunchedEffect
        val format = if (exportRequest == 1 || exportRequest == 3) "csv" else "json"
        try {
            val payload = client.usageExport(range, format)
            clipboard.setText(AnnotatedString(payload))
            exportNotice = "Copied ${payload.lines().size} rows as ${format.uppercase()}."
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            exportNotice = "Export failed: ${error.message ?: "unknown error"}"
        }
    }

    LaunchedEffect(range, granularity, tick) {
        try {
            summary = client.usageSummary(range, granularity)
            tools = client.usageTools(range)
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
                    session.role.lowercase().contains(needle) ||
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UsageGranularity.entries.forEach { option ->
                    SelectablePill(option.label, granularity == option, onClick = { granularity = option })
                }
            }
        }

        if (failure != null) {
            item {
                SectionCard(title = "Usage unavailable", subtitle = "the local engine did not answer", icon = AgentIcons.Chart) {
                    Text(failure.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                        formatCost(summary.totals.actualCost),
                        Modifier.weight(1f),
                        caption = if (summary.priced) "local estimate" else "unpriced models present",
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Calls", summary.totals.requests.toString(), Modifier.weight(1f))
                    MetricTile("Sessions", summary.bySession.size.toString(), Modifier.weight(1f))
                    MetricTile(
                        "Failures",
                        summary.totals.failures.toString(),
                        Modifier.weight(1f),
                        caption = if (summary.totals.failures == 0) "none" else "see breakdown",
                    )
                }
            }
            item {
                SectionCard(title = "Trend", subtitle = granularity.label.lowercase() + " buckets", icon = AgentIcons.Chart) {
                    if (summary.byPoint.isEmpty()) {
                        Text("Nothing recorded in this range.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        BarChart(
                            values = summary.byPoint.map { it.tokens.toFloat() },
                            labels = summary.byPoint.map { it.key.takeLast(5) },
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Context composition", subtitle = "prompt · completion · cache read · cache write", icon = AgentIcons.Wave) {
                    CompositionRing(
                        input = summary.composition.input,
                        output = summary.composition.output,
                        cacheRead = summary.composition.cacheRead,
                        cacheWrite = summary.composition.cacheWrite,
                    )
                }
            }
            item {
                SectionCard(title = "Cost breakdown", subtitle = "standard vs billed", icon = AgentIcons.Bolt) {
                    CostRows(summary.costBreakdown, summary.totals)
                }
            }
            item {
                SectionCard(title = "By role", subtitle = "router · worker · watchdog · subagent", icon = AgentIcons.Settings) {
                    val total = summary.totals.tokens.coerceAtLeast(1)
                    summary.byRole.forEach { entry ->
                        val fraction = entry.tokens.toFloat() / total.toFloat()
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.label.ifBlank { entry.key }, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${formatCount(entry.tokens)} · ${formatCost(entry.actualCost)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp),
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(title = "By model", subtitle = "share of recorded tokens", icon = AgentIcons.Bolt) {
                    val total = summary.totals.tokens.coerceAtLeast(1)
                    summary.byModel.forEach { entry ->
                        val fraction = entry.tokens.toFloat() / total.toFloat()
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.model.ifBlank { "unassigned" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(
                                    "${formatCount(entry.tokens)} · ${formatCost(entry.actualCost)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                            )
                            Text(
                                "${entry.requests} calls · ${entry.provider}${if (entry.priced) "" else " · unpriced"}",
                                Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (tools.isNotEmpty()) {
                item {
                    SectionCard(title = "Tool usage", subtitle = "calls, failures and latency", icon = AgentIcons.Wave) {
                        tools.forEach { tool ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(tool.tool, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(
                                    "${tool.calls} calls · ${tool.failures} failed · ${tool.avgDurationMs}ms avg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (tool.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (summary.byError.isNotEmpty()) {
                item {
                    SectionCard(title = "Failures", subtitle = "by category", icon = AgentIcons.Bolt) {
                        summary.byError.forEach { entry ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.category, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    entry.requests.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
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
                SectionCard(title = "Export", subtitle = "copy the ledger for this range", icon = AgentIcons.Settings) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { exportRequest += 1 }) { Text("Copy CSV") }
                        TextButton(onClick = { exportRequest += 2 }) { Text("Copy JSON") }
                    }
                    Text(
                        exportNotice ?: "Copies every recorded row for this range to the clipboard. Nothing leaves the device.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item {
                SectionCard(title = "By session", subtitle = "newest activity first", icon = AgentIcons.Settings) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Filter by session, role or model") },
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
private fun CostRows(breakdown: UsageCost, totals: UsageBucket) {
    val rows = listOf(
        "Prompt" to breakdown.input,
        "Completion" to breakdown.output,
        "Cache read" to breakdown.cacheRead,
        "Cache write" to breakdown.cacheWrite,
    )
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { (label, value) ->
            KeyValueRow(label, formatCost(value))
        }
        Spacer(Modifier.height(8.dp))
        KeyValueRow("Standard", formatCost(totals.cost))
        KeyValueRow("Billed", formatCost(totals.actualCost))
    }
}

@Composable
private fun SessionRow(session: UsageSessionBucket) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(session.sessionId.take(20).ifBlank { "unassigned" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "${session.role.ifBlank { "worker" }} · ${session.models.joinToString(", ").ifBlank { "unknown model" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                "${formatCount(session.tokens)} tokens · ${session.requests} calls · ${session.toolCalls} tools · ${session.avgDurationMs}ms avg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatRelative(session.lastActivity, nowMillis()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCost(session.actualCost),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            )
            if (session.failures > 0) {
                Text(
                    "${session.failures} failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CompositionRing(input: Long, output: Long, cacheRead: Long, cacheWrite: Long) {
    val total = (input + output + cacheRead + cacheWrite).coerceAtLeast(1L)
    val slices = listOf(
        Triple("Prompt", input, MaterialTheme.colorScheme.primary),
        Triple("Completion", output, MaterialTheme.colorScheme.tertiary),
        Triple("Cache read", cacheRead, MaterialTheme.colorScheme.secondary),
        Triple("Cache write", cacheWrite, MaterialTheme.colorScheme.error),
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
    return era * 146097L + doe - 719468L
}