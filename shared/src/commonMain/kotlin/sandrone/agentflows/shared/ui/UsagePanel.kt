package sandrone.agentflows.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import sandrone.agentflows.shared.EngineClient
import sandrone.agentflows.shared.UsageBucket
import sandrone.agentflows.shared.UsageCost
import sandrone.agentflows.shared.UsageDayBucket
import sandrone.agentflows.shared.UsageErrorBucket
import sandrone.agentflows.shared.UsageGranularity
import sandrone.agentflows.shared.UsageModelBucket
import sandrone.agentflows.shared.UsagePoint
import sandrone.agentflows.shared.UsageRange
import sandrone.agentflows.shared.UsageSessionBucket
import sandrone.agentflows.shared.UsageSummary
import sandrone.agentflows.shared.UsageTokens
import sandrone.agentflows.shared.UsageToolBucket
import sandrone.agentflows.shared.design.AvatarTile
import sandrone.agentflows.shared.design.BarStrip
import sandrone.agentflows.shared.design.BusyIndicator
import sandrone.agentflows.shared.design.CaptionText
import sandrone.agentflows.shared.design.Donut
import sandrone.agentflows.shared.design.EmptyHint
import sandrone.agentflows.shared.design.GhostIconButton
import sandrone.agentflows.shared.design.HeatGrid
import sandrone.agentflows.shared.design.KVRow
import sandrone.agentflows.shared.design.MetaChip
import sandrone.agentflows.shared.design.Meter
import sandrone.agentflows.shared.design.MonoText
import sandrone.agentflows.shared.design.Motion
import sandrone.agentflows.shared.design.NumberCell
import sandrone.agentflows.shared.design.PanelCard
import sandrone.agentflows.shared.design.PanelHeader
import sandrone.agentflows.shared.design.PulsingDot
import sandrone.agentflows.shared.design.Radius
import sandrone.agentflows.shared.design.SectionLabel
import sandrone.agentflows.shared.design.StatBlock
import sandrone.agentflows.shared.design.ThinRule
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.design.isDesktop
import sandrone.agentflows.shared.design.metrics
import sandrone.agentflows.shared.nowMillis

private val WidthTokens = 60.dp
private val WidthCost = 62.dp
private val WidthCalls = 40.dp
private val WidthFail = 38.dp
private val WidthAvg = 48.dp
private val WidthProvider = 76.dp
private val WidthRole = 58.dp
private val WidthLast = 58.dp
private val WidthTools = 40.dp

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
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }
    var exportNotice by remember { mutableStateOf<String?>(null) }
    var exportTicket by remember { mutableStateOf(0) }
    var exportFormat by remember { mutableStateOf("csv") }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(exportTicket) {
        if (exportTicket == 0) return@LaunchedEffect
        val format = exportFormat
        try {
            val payload = client.usageExport(range, format)
            clipboard.setText(AnnotatedString(payload))
            exportNotice = "Copied " + payload.lines().size + " rows as " + format.uppercase() + "."
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            exportNotice = "Export failed: " + (error.message ?: "unknown error")
        }
    }

    LaunchedEffect(range, granularity, tick) {
        loading = true
        try {
            summary = client.usageSummary(range, granularity)
            tools = client.usageTools(range)
            failure = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failure = error.message ?: "usage service unreachable"
        }
        loading = false
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

    if (isDesktop()) {
        DesktopUsage(
            range = range,
            granularity = granularity,
            summary = summary,
            tools = tools,
            sessions = sessions,
            query = query,
            failure = failure,
            loading = loading,
            exportNotice = exportNotice,
            onRange = { range = it },
            onGranularity = { granularity = it },
            onQuery = { query = it },
            onRetry = { tick += 1 },
            onExportCsv = {
                exportFormat = "csv"
                exportTicket += 1
            },
            onExportJson = {
                exportFormat = "json"
                exportTicket += 1
            },
            onDismissNotice = { exportNotice = null },
            modifier = modifier,
        )
    } else {
        MobileUsage(
            range = range,
            granularity = granularity,
            summary = summary,
            tools = tools,
            sessions = sessions,
            query = query,
            failure = failure,
            loading = loading,
            exportNotice = exportNotice,
            onRange = { range = it },
            onGranularity = { granularity = it },
            onQuery = { query = it },
            onRetry = { tick += 1 },
            onExportCsv = {
                exportFormat = "csv"
                exportTicket += 1
            },
            onExportJson = {
                exportFormat = "json"
                exportTicket += 1
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun DesktopUsage(
    range: UsageRange,
    granularity: UsageGranularity,
    summary: UsageSummary,
    tools: List<UsageToolBucket>,
    sessions: List<UsageSessionBucket>,
    query: String,
    failure: String?,
    loading: Boolean,
    exportNotice: String?,
    onRange: (UsageRange) -> Unit,
    onGranularity: (UsageGranularity) -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = nowMillis()
    val failureText = failure
    val notice = exportNotice
    val hasData = summary.totals.requests > 0
    val initialLoading = loading && !hasData && failureText == null
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UsageRange.entries.forEach { option ->
                    MetaChip(
                        text = option.label,
                        selected = option == range,
                        onClick = { onRange(option) },
                        mono = true,
                    )
                }
                VerticalRule(14.dp)
                UsageGranularity.entries.forEach { option ->
                    MetaChip(
                        text = option.label,
                        selected = option == granularity,
                        onClick = { onGranularity(option) },
                        mono = true,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (loading) {
                BusyIndicator()
            } else {
                PulsingDot(size = 5.dp)
            }
            if (failureText != null) {
                Spacer(Modifier.width(6.dp))
                MonoText("offline", color = MaterialTheme.colorScheme.error)
            }
            VerticalRule(14.dp)
            GhostIconButton(AgentIcons.Copy, onClick = onExportCsv)
            GhostIconButton(AgentIcons.Download, onClick = onExportJson)
            GhostIconButton(AgentIcons.Refresh, onClick = onRetry, enabled = !loading)
        }
        ThinRule(alpha = 0.7f)
        if (notice != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(start = 16.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AgentIcons.Info, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                MonoText(notice, Modifier.weight(1f), size = 10.sp)
                GhostIconButton(AgentIcons.Close, onClick = onDismissNotice, size = 20.dp, iconSize = 11.dp)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            if (failureText != null) {
                item { DesktopFailureBanner(failureText, onRetry) }
            }
            if (initialLoading) {
                item { PanelCard { LoadingBlock("Collecting usage for this range") } }
            } else if (!hasData && failureText == null) {
                item {
                    PanelCard {
                        EmptyHint(
                            icon = AgentIcons.Chart,
                            title = "No usage recorded",
                            detail = "Token and cost accounting stays on this device. Entries appear as soon as the engine records a model call for the selected range.",
                            dense = true,
                        )
                    }
                }
            }
            if (hasData) {
                item { DesktopKpiStrip(summary) }
                item { DesktopBody(summary, tools, granularity) }
                item { DesktopSessionsSection(sessions, summary.bySession.size, query, onQuery, now) }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun MobileUsage(
    range: UsageRange,
    granularity: UsageGranularity,
    summary: UsageSummary,
    tools: List<UsageToolBucket>,
    sessions: List<UsageSessionBucket>,
    query: String,
    failure: String?,
    loading: Boolean,
    exportNotice: String?,
    onRange: (UsageRange) -> Unit,
    onGranularity: (UsageGranularity) -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = nowMillis()
    val failureText = failure
    val hasData = summary.totals.requests > 0
    val initialLoading = loading && !hasData && failureText == null
    val subtitle = when {
        failureText != null -> "Engine unreachable, last snapshot kept"
        initialLoading -> "Collecting the first snapshot"
        summary.statsUpdatedAt > 0 -> "Updated " + formatRelative(summary.statsUpdatedAt, now)
        else -> "Waiting for the first snapshot"
    }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(AgentIcons.Chart, size = 40.dp, shape = RoundedCornerShape(Radius.md))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Usage ledger",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = metrics().titleSize,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    CaptionText(subtitle, Modifier.padding(top = 1.dp), maxLines = 1)
                }
                GhostIconButton(
                    AgentIcons.Refresh,
                    onClick = onRetry,
                    enabled = !loading,
                    size = 44.dp,
                    iconSize = 20.dp,
                )
            }
            Spacer(Modifier.height(12.dp))
            SegmentedControl(
                options = UsageRange.entries.map { it.name to it.label },
                selected = range.name,
                onSelect = { key ->
                    UsageRange.entries.firstOrNull { it.name == key }?.let { onRange(it) }
                },
            )
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = UsageGranularity.entries.map { it.name to it.label },
                selected = granularity.name,
                onSelect = { key ->
                    UsageGranularity.entries.firstOrNull { it.name == key }?.let { onGranularity(it) }
                },
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (failureText != null) {
                item { MobileFailureBanner(failureText, onRetry) }
            }
            if (initialLoading) {
                item { PanelCard { LoadingBlock("Collecting usage for this range") } }
            } else if (!hasData && failureText == null) {
                item {
                    PanelCard {
                        EmptyHint(
                            icon = AgentIcons.Chart,
                            title = "No usage recorded",
                            detail = "Token and cost accounting stays on this device. Entries appear as soon as the engine records a model call for the selected range.",
                            dense = false,
                        )
                    }
                }
            }
            if (hasData) {
                item { MobileMetricTiles(summary) }
                item { MobileTrendCard(summary, range, granularity) }
                item { MobileCompositionCard(summary.composition) }
                item { MobileCostCard(summary.costBreakdown, summary.totals, summary.priced) }
                item { MobileRoleCard(summary.byRole, summary.totals.tokens) }
                item { MobileModelCard(summary.byModel, summary.totals.tokens) }
                if (tools.isNotEmpty()) {
                    item { MobileToolCard(tools) }
                }
                if (summary.byError.isNotEmpty()) {
                    item { MobileErrorCard(summary.byError) }
                }
                item { MobileActivityCard(summary.heatmap) }
                item { MobileExportCard(exportNotice, onExportCsv, onExportJson) }
                item { MobileSessionsHeader(total = summary.bySession.size, filtered = sessions.size, query = query, onQuery = onQuery) }
                if (sessions.isEmpty()) {
                    item { CaptionText("No session matches that filter.", Modifier.padding(horizontal = 4.dp), size = 13.sp) }
                } else {
                    items(sessions) { session -> MobileSessionCard(session, now) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DesktopKpiStrip(summary: UsageSummary) {
    val scheme = MaterialTheme.colorScheme
    val totals = summary.totals
    PanelCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatBlock(
                "Tokens",
                formatCount(totals.tokens),
                Modifier.weight(1f),
                caption = formatCount(totals.tokensIn) + " in / " + formatCount(totals.tokensOut) + " out",
            )
            VerticalRule(28.dp, inset = 10.dp)
            StatBlock(
                "Cost",
                formatCost(totals.actualCost),
                Modifier.weight(1f),
                caption = if (summary.priced) "billed estimate" else "unpriced models",
                accent = if (summary.priced) null else scheme.tertiary,
            )
            VerticalRule(28.dp, inset = 10.dp)
            StatBlock("Calls", totals.requests.toString(), Modifier.weight(1f), caption = "model calls")
            VerticalRule(28.dp, inset = 10.dp)
            StatBlock("Sessions", summary.bySession.size.toString(), Modifier.weight(1f), caption = "with activity")
            VerticalRule(28.dp, inset = 10.dp)
            StatBlock(
                "Failures",
                totals.failures.toString(),
                Modifier.weight(1f),
                caption = if (totals.failures == 0) "none recorded" else "see breakdown",
                accent = if (totals.failures > 0) scheme.error else null,
            )
        }
    }
}

@Composable
private fun DesktopBody(summary: UsageSummary, tools: List<UsageToolBucket>, granularity: UsageGranularity) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopTrendCard(summary, granularity)
            DesktopModelTable(summary.byModel, summary.totals.tokens)
            DesktopRoleTable(summary.byRole, summary.totals.tokens)
            DesktopActivityCard(summary.heatmap)
        }
        Column(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopCompositionCard(summary.composition)
            DesktopCostCard(summary.costBreakdown, summary.totals, summary.priced)
            if (tools.isNotEmpty()) {
                DesktopToolTable(tools)
            }
            if (summary.byError.isNotEmpty()) {
                DesktopErrorTable(summary.byError)
            }
        }
    }
}

@Composable
private fun DesktopTrendCard(summary: UsageSummary, granularity: UsageGranularity) {
    val points = summary.byPoint
    val peak = points.maxOfOrNull { it.tokens } ?: 0L
    val showLabels = points.size <= 24
    PanelCard {
        PanelHeader(
            title = "Trend",
            subtitle = countOf(points.size, granularity.label.lowercase() + " bucket", granularity.label.lowercase() + " buckets"),
            icon = AgentIcons.Chart,
            trailing = { MonoText("tokens") },
        )
        Spacer(Modifier.height(10.dp))
        if (points.isEmpty()) {
            CaptionText("Nothing recorded in this range.", size = 11.sp)
        } else {
            BarStrip(
                values = points.map { it.tokens.toFloat() },
                labels = points.map { it.key.takeLast(5) },
                height = 72.dp,
                showLabels = showLabels,
            )
        }
        Spacer(Modifier.height(8.dp))
        ThinRule(alpha = 0.3f)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText("peak " + formatCount(peak))
            Spacer(Modifier.weight(1f))
            MonoText("range " + summary.range + " / " + summary.granularity)
        }
    }
}

@Composable
private fun DesktopModelTable(models: List<UsageModelBucket>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    PanelCard {
        PanelHeader("By model", subtitle = "share of recorded tokens", icon = AgentIcons.Bolt)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Model", Modifier.weight(1f))
            SectionLabel("Share", Modifier.weight(0.9f))
            HeaderCell("Provider", WidthProvider)
            HeaderCell("Calls", WidthCalls)
            HeaderCell("Tokens", WidthTokens)
            HeaderCell("Cost", WidthCost)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        if (models.isEmpty()) {
            CaptionText("No model calls recorded.", Modifier.padding(vertical = 8.dp), size = 11.sp)
        } else {
            models.forEachIndexed { index, model ->
                if (index > 0) {
                    ThinRule(alpha = 0.3f)
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 26.dp).padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            model.model.ifBlank { "unassigned" },
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        )
                        if (!model.priced) {
                            Spacer(Modifier.width(6.dp))
                            MetaChip("unpriced", tone = Tone.WARNING, mono = true)
                        }
                    }
                    Box(Modifier.weight(0.9f).padding(end = 10.dp)) {
                        Meter(model.tokens.toFloat() / total.toFloat(), height = 4.dp)
                    }
                    MonoText(model.provider.ifBlank { "local" }, Modifier.width(WidthProvider), size = 10.5.sp)
                    NumberCell(model.requests.toString(), width = WidthCalls)
                    NumberCell(formatCount(model.tokens), width = WidthTokens)
                    NumberCell(formatCost(model.actualCost), width = WidthCost)
                }
            }
        }
    }
}

@Composable
private fun DesktopRoleTable(roles: List<UsagePoint>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    PanelCard {
        PanelHeader("By role", subtitle = "router, worker, watchdog, subagent", icon = AgentIcons.Settings)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Role", Modifier.weight(1f))
            SectionLabel("Share", Modifier.weight(0.9f))
            HeaderCell("Tokens", WidthTokens)
            HeaderCell("Cost", WidthCost)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        if (roles.isEmpty()) {
            CaptionText("No role activity recorded.", Modifier.padding(vertical = 8.dp), size = 11.sp)
        } else {
            roles.forEachIndexed { index, role ->
                if (index > 0) {
                    ThinRule(alpha = 0.3f)
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 26.dp).padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText(
                        role.label.ifBlank { role.key }.ifBlank { "unknown" },
                        Modifier.weight(1f),
                        size = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(Modifier.weight(0.9f).padding(end = 10.dp)) {
                        Meter(role.tokens.toFloat() / total.toFloat(), height = 4.dp)
                    }
                    NumberCell(formatCount(role.tokens), width = WidthTokens)
                    NumberCell(formatCost(role.actualCost), width = WidthCost)
                }
            }
        }
    }
}

@Composable
private fun DesktopCompositionCard(tokens: UsageTokens) {
    val scheme = MaterialTheme.colorScheme
    val parts = compositionParts(tokens)
    val total = parts.sumOf { it.second }.coerceAtLeast(1L)
    val colors = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.outline)
    PanelCard {
        PanelHeader(
            title = "Context composition",
            subtitle = "prompt, completion, cache read, cache write",
            icon = AgentIcons.Wave,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Donut(
                slices = parts.map { it.first to it.second.toFloat() },
                colors = colors,
                size = 84.dp,
                strokeRatio = 0.22f,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                parts.forEachIndexed { index, part ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(colors[index]))
                        Spacer(Modifier.width(6.dp))
                        CaptionText(part.first, Modifier.weight(1f), maxLines = 1, size = 10.5.sp)
                        MonoText(
                            formatCount(part.second) + " / " + percentOf(part.second, total) + "%",
                            size = 10.sp,
                            color = scheme.onSurface,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ThinRule(alpha = 0.3f)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText("context tokens")
            Spacer(Modifier.weight(1f))
            MonoText(formatCount(total), size = 10.sp, color = scheme.onSurface)
        }
    }
}

@Composable
private fun DesktopCostCard(breakdown: UsageCost, totals: UsageBucket, priced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader("Cost breakdown", subtitle = "standard vs billed", icon = AgentIcons.Bolt)
        Spacer(Modifier.height(6.dp))
        KVRow("Prompt", formatCost(breakdown.input))
        KVRow("Completion", formatCost(breakdown.output))
        KVRow("Cache read", formatCost(breakdown.cacheRead))
        KVRow("Cache write", formatCost(breakdown.cacheWrite))
        Spacer(Modifier.height(2.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(2.dp))
        KVRow("Standard", formatCost(totals.cost))
        KVRow("Billed", formatCost(totals.actualCost), valueColor = scheme.primary)
        if (totals.cost > totals.actualCost) {
            KVRow("Saved", formatCost(totals.cost - totals.actualCost), valueColor = scheme.secondary)
        }
        if (!priced) {
            Spacer(Modifier.height(6.dp))
            MetaChip("unpriced models present", tone = Tone.WARNING, mono = true)
        }
    }
}

@Composable
private fun DesktopToolTable(tools: List<UsageToolBucket>) {
    PanelCard {
        PanelHeader(
            title = "Tool usage",
            subtitle = countOf(tools.sumOf { it.calls }, "call", "calls") + " / " + countOf(tools.sumOf { it.failures }, "failure", "failures"),
            icon = AgentIcons.Terminal,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Tool", Modifier.weight(1f))
            HeaderCell("Calls", WidthCalls)
            HeaderCell("Fail", WidthFail)
            HeaderCell("Avg", WidthAvg)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        tools.forEachIndexed { index, tool ->
            if (index > 0) {
                ThinRule(alpha = 0.3f)
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 26.dp).padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoText(
                    tool.tool.ifBlank { "unknown" },
                    Modifier.weight(1f),
                    size = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                NumberCell(tool.calls.toString(), width = WidthCalls)
                NumberCell(
                    tool.failures.toString(),
                    width = WidthFail,
                    color = if (tool.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberCell(tool.avgDurationMs.toString() + "ms", width = WidthAvg)
            }
        }
    }
}

@Composable
private fun DesktopErrorTable(errors: List<UsageErrorBucket>) {
    PanelCard {
        PanelHeader("Failures", subtitle = "by category", icon = AgentIcons.AlertTriangle)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Category", Modifier.weight(1f))
            HeaderCell("Count", WidthCalls)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        errors.forEachIndexed { index, entry ->
            if (index > 0) {
                ThinRule(alpha = 0.3f)
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 26.dp).padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoText(
                    entry.category.ifBlank { "uncategorised" },
                    Modifier.weight(1f),
                    size = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                NumberCell(entry.requests.toString(), width = WidthCalls, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DesktopActivityCard(heatmap: List<UsageDayBucket>) {
    val daily = heatmap.associate { dayToEpochDay(it) to it.requests }
    val total = heatmap.sumOf { it.requests.toLong() }
    val peak = heatmap.maxOfOrNull { it.requests } ?: 0
    PanelCard {
        PanelHeader("Activity", subtitle = "requests per day, last 12 weeks", icon = AgentIcons.Grid)
        Spacer(Modifier.height(10.dp))
        HeatGrid(dailyCounts = daily, weeks = 12)
        Spacer(Modifier.height(8.dp))
        ThinRule(alpha = 0.3f)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText(formatCount(total) + " requests")
            Spacer(Modifier.weight(1f))
            MonoText("peak " + peak + " / day")
        }
    }
}

@Composable
private fun DesktopSessionsSection(
    sessions: List<UsageSessionBucket>,
    total: Int,
    query: String,
    onQuery: (String) -> Unit,
    now: Long,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Sessions")
            Spacer(Modifier.width(8.dp))
            MonoText(sessions.size.toString() + " / " + total)
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.width(260.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                placeholder = {
                    Text(
                        "Filter session, role or model",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    )
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        PanelCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Session", Modifier.weight(1.3f))
                SectionLabel("Role", Modifier.width(WidthRole))
                SectionLabel("Models", Modifier.weight(1.2f))
                HeaderCell("Tokens", WidthTokens)
                HeaderCell("Calls", WidthCalls)
                HeaderCell("Tools", WidthTools)
                HeaderCell("Avg", WidthAvg)
                HeaderCell("Last", WidthLast)
                HeaderCell("Cost", WidthCost)
                HeaderCell("Fail", WidthFail)
            }
            Spacer(Modifier.height(4.dp))
            ThinRule(alpha = 0.4f)
            if (sessions.isEmpty()) {
                CaptionText("No session matches that filter.", Modifier.padding(vertical = 10.dp), size = 11.sp)
            } else {
                sessions.forEachIndexed { index, session ->
                    if (index > 0) {
                        ThinRule(alpha = 0.25f)
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(
                            session.sessionId.take(20).ifBlank { "unassigned" },
                            Modifier.weight(1.3f),
                            size = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        MonoText(session.role.ifBlank { "worker" }, Modifier.width(WidthRole), size = 10.5.sp)
                        MonoText(
                            session.models.joinToString(", ").ifBlank { "unknown model" },
                            Modifier.weight(1.2f),
                            size = 10.5.sp,
                        )
                        NumberCell(formatCount(session.tokens), width = WidthTokens)
                        NumberCell(session.requests.toString(), width = WidthCalls)
                        NumberCell(session.toolCalls.toString(), width = WidthTools)
                        NumberCell(session.avgDurationMs.toString() + "ms", width = WidthAvg)
                        NumberCell(formatRelative(session.lastActivity, now), width = WidthLast)
                        NumberCell(formatCost(session.actualCost), width = WidthCost)
                        NumberCell(
                            session.failures.toString(),
                            width = WidthFail,
                            color = if (session.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopFailureBanner(message: String, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sm)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .border(1.dp, scheme.error.copy(alpha = 0.55f), shape)
            .clickable(onClick = onRetry)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AgentIcons.AlertTriangle, null, Modifier.size(13.dp), tint = scheme.error)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            SectionLabel("Usage unavailable", color = scheme.error)
            CaptionText(message, Modifier.padding(top = 1.dp), maxLines = 2, size = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        MonoText("retry", size = 10.sp, color = scheme.error)
        Spacer(Modifier.width(6.dp))
        GhostIconButton(AgentIcons.Refresh, onClick = onRetry, size = 22.dp, iconSize = 12.dp, tint = scheme.error)
    }
}

@Composable
private fun MobileFailureBanner(message: String, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = scheme.errorContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AgentIcons.AlertTriangle, null, Modifier.size(18.dp), tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Usage unavailable",
                    Modifier.weight(1f),
                    maxLines = 1,
                    color = scheme.onErrorContainer,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            Text(
                message,
                Modifier.padding(top = 6.dp),
                color = scheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = scheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun MobileMetricTiles(summary: UsageSummary) {
    val scheme = MaterialTheme.colorScheme
    val totals = summary.totals
    val gap = metrics().cardGap
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                StatBlock(
                    "Tokens",
                    formatCount(totals.tokens),
                    caption = formatCount(totals.tokensIn) + " in / " + formatCount(totals.tokensOut) + " out",
                )
            }
            PanelCard(Modifier.weight(1f)) {
                StatBlock(
                    "Cost",
                    formatCost(totals.actualCost),
                    caption = if (summary.priced) "billed estimate" else "unpriced models",
                    accent = if (summary.priced) null else scheme.tertiary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                StatBlock("Calls", totals.requests.toString(), caption = "model calls")
            }
            PanelCard(Modifier.weight(1f)) {
                StatBlock("Sessions", summary.bySession.size.toString(), caption = "with activity")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                StatBlock(
                    "Failures",
                    totals.failures.toString(),
                    caption = if (totals.failures == 0) "none recorded" else "see breakdown",
                    accent = if (totals.failures > 0) scheme.error else null,
                )
            }
            PanelCard(Modifier.weight(1f)) {
                StatBlock("Cache hits", formatCount(totals.cacheHits), caption = "prompt cache")
            }
        }
    }
}

@Composable
private fun MobileTrendCard(summary: UsageSummary, range: UsageRange, granularity: UsageGranularity) {
    val points = summary.byPoint
    val peak = points.maxOfOrNull { it.tokens } ?: 0L
    val showLabels = points.size <= 12
    PanelCard {
        PanelHeader(
            title = "Trend",
            subtitle = countOf(points.size, granularity.label.lowercase() + " bucket", granularity.label.lowercase() + " buckets"),
            icon = AgentIcons.Chart,
        )
        Spacer(Modifier.height(12.dp))
        if (points.isEmpty()) {
            CaptionText("Nothing recorded in this range.", size = 13.sp)
        } else {
            BarStrip(
                values = points.map { it.tokens.toFloat() },
                labels = points.map { it.key.takeLast(5) },
                height = 96.dp,
                showLabels = showLabels,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText("peak " + formatCount(peak), size = 11.sp)
            Spacer(Modifier.weight(1f))
            MonoText(range.label + " / " + granularity.label.lowercase(), size = 11.sp)
        }
    }
}

@Composable
private fun MobileCompositionCard(tokens: UsageTokens) {
    val scheme = MaterialTheme.colorScheme
    val parts = compositionParts(tokens)
    val total = parts.sumOf { it.second }.coerceAtLeast(1L)
    val colors = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.outline)
    PanelCard {
        PanelHeader(
            title = "Context composition",
            subtitle = "prompt, completion, cache read, cache write",
            icon = AgentIcons.Wave,
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(
                slices = parts.map { it.first to it.second.toFloat() },
                colors = colors,
                size = 132.dp,
                strokeRatio = 0.2f,
            )
        }
        Spacer(Modifier.height(16.dp))
        parts.forEachIndexed { index, part ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[index]))
                Spacer(Modifier.width(10.dp))
                CaptionText(part.first, Modifier.weight(1f), maxLines = 1, size = 13.sp)
                MonoText(
                    formatCount(part.second) + " / " + percentOf(part.second, total) + "%",
                    size = 12.sp,
                    color = scheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(4.dp))
        KVRow("Context tokens", formatCount(total))
    }
}

@Composable
private fun MobileCostCard(breakdown: UsageCost, totals: UsageBucket, priced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader("Cost breakdown", subtitle = "standard vs billed", icon = AgentIcons.Bolt)
        Spacer(Modifier.height(10.dp))
        KVRow("Prompt", formatCost(breakdown.input))
        KVRow("Completion", formatCost(breakdown.output))
        KVRow("Cache read", formatCost(breakdown.cacheRead))
        KVRow("Cache write", formatCost(breakdown.cacheWrite))
        Spacer(Modifier.height(6.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(6.dp))
        KVRow("Standard", formatCost(totals.cost))
        KVRow("Billed", formatCost(totals.actualCost), valueColor = scheme.primary)
        if (totals.cost > totals.actualCost) {
            KVRow("Saved", formatCost(totals.cost - totals.actualCost), valueColor = scheme.secondary)
        }
        if (!priced) {
            Spacer(Modifier.height(10.dp))
            MetaChip("unpriced models present", tone = Tone.WARNING, mono = true)
        }
    }
}

@Composable
private fun MobileRoleCard(roles: List<UsagePoint>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    PanelCard {
        PanelHeader("By role", subtitle = "router, worker, watchdog, subagent", icon = AgentIcons.Settings)
        Spacer(Modifier.height(10.dp))
        if (roles.isEmpty()) {
            CaptionText("No role activity recorded.", size = 13.sp)
        } else {
            roles.forEach { role ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            role.label.ifBlank { role.key }.ifBlank { "unknown" },
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        )
                        MonoText(formatCost(role.actualCost), size = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(6.dp))
                    Meter(role.tokens.toFloat() / total.toFloat(), height = 6.dp)
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        formatCount(role.tokens) + " tokens / " + percentOf(role.tokens, total) + "%",
                        size = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileModelCard(models: List<UsageModelBucket>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader("By model", subtitle = "share of recorded tokens", icon = AgentIcons.Bolt)
        Spacer(Modifier.height(10.dp))
        if (models.isEmpty()) {
            CaptionText("No model calls recorded.", size = 13.sp)
        } else {
            models.forEach { model ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            model.model.ifBlank { "unassigned" },
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        )
                        MonoText(formatCost(model.actualCost), size = 12.sp, color = scheme.onSurface)
                    }
                    CaptionText(
                        countOf(model.requests, "call", "calls") + " / " + model.provider.ifBlank { "local" },
                        Modifier.padding(top = 2.dp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Meter(model.tokens.toFloat() / total.toFloat(), height = 6.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoText(
                            formatCount(model.tokens) + " tokens / " + percentOf(model.tokens, total) + "%",
                            size = 11.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        if (!model.priced) {
                            MetaChip("unpriced", tone = Tone.WARNING, mono = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileToolCard(tools: List<UsageToolBucket>) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader(
            title = "Tool usage",
            subtitle = countOf(tools.sumOf { it.calls }, "call", "calls") + " / " + countOf(tools.sumOf { it.failures }, "failure", "failures"),
            icon = AgentIcons.Terminal,
        )
        Spacer(Modifier.height(10.dp))
        tools.forEach { tool ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    MonoText(tool.tool.ifBlank { "unknown" }, size = 13.sp, color = scheme.onSurface)
                    CaptionText(tool.avgDurationMs.toString() + "ms avg", Modifier.padding(top = 2.dp), maxLines = 1, size = 11.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    MonoText(tool.calls.toString() + " calls", size = 12.sp, color = scheme.onSurface)
                    CaptionText(
                        if (tool.failures > 0) tool.failures.toString() + " failed" else "no failures",
                        Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        size = 11.5.sp,
                        color = if (tool.failures > 0) scheme.error else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileErrorCard(errors: List<UsageErrorBucket>) {
    PanelCard {
        PanelHeader("Failures", subtitle = "by category", icon = AgentIcons.AlertTriangle)
        Spacer(Modifier.height(10.dp))
        errors.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.category.ifBlank { "uncategorised" },
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                )
                MonoText(entry.requests.toString(), size = 13.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MobileActivityCard(heatmap: List<UsageDayBucket>) {
    val daily = heatmap.associate { dayToEpochDay(it) to it.requests }
    val total = heatmap.sumOf { it.requests.toLong() }
    val peak = heatmap.maxOfOrNull { it.requests } ?: 0
    PanelCard {
        PanelHeader("Activity", subtitle = "requests per day, last 12 weeks", icon = AgentIcons.Grid)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HeatGrid(dailyCounts = daily, weeks = 12)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(formatCount(total) + " requests", size = 11.sp)
            Spacer(Modifier.weight(1f))
            MonoText("peak " + peak + " / day", size = 11.sp)
        }
    }
}

@Composable
private fun MobileExportCard(notice: String?, onCsv: () -> Unit, onJson: () -> Unit) {
    PanelCard {
        PanelHeader("Export", subtitle = "copy this range to the clipboard", icon = AgentIcons.Download)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCsv) {
                Icon(AgentIcons.Copy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy CSV")
            }
            TextButton(onClick = onJson) {
                Icon(AgentIcons.Code, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy JSON")
            }
        }
        Spacer(Modifier.height(6.dp))
        CaptionText(
            notice ?: "Every recorded row for this range is copied locally. Nothing leaves the device.",
            maxLines = 3,
            size = 12.sp,
        )
    }
}

@Composable
private fun MobileSessionsHeader(total: Int, filtered: Int, query: String, onQuery: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Sessions")
            Spacer(Modifier.width(8.dp))
            MonoText(filtered.toString() + " / " + total, size = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Filter sessions") },
            placeholder = { Text("Session, role or model") },
        )
    }
}

@Composable
private fun MobileSessionCard(session: UsageSessionBucket, now: Long) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarTile(
                AgentIcons.Users,
                size = 34.dp,
                container = scheme.surfaceContainerHigh,
                content = scheme.onSurfaceVariant,
                shape = RoundedCornerShape(Radius.sm),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                MonoText(
                    session.sessionId.take(22).ifBlank { "unassigned" },
                    size = 13.sp,
                    color = scheme.onSurface,
                    weight = FontWeight.Medium,
                )
                CaptionText(
                    session.role.ifBlank { "worker" } + " / " + session.models.joinToString(", ").ifBlank { "unknown model" },
                    Modifier.padding(top = 1.dp),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MonoText(formatCost(session.actualCost), size = 12.sp, color = scheme.onSurface)
                CaptionText(
                    formatRelative(session.lastActivity, now),
                    Modifier.padding(top = 1.dp),
                    maxLines = 1,
                    size = 10.5.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Column(Modifier.weight(1f)) {
                KVRow("tokens", formatCount(session.tokens))
                KVRow("calls", session.requests.toString())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                KVRow("tools", session.toolCalls.toString())
                KVRow("avg", session.avgDurationMs.toString() + "ms")
            }
        }
        if (session.failures > 0) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AgentIcons.AlertTriangle, null, Modifier.size(13.dp), tint = scheme.error)
                Spacer(Modifier.width(6.dp))
                CaptionText(countOf(session.failures, "failure", "failures"), color = scheme.error, maxLines = 1, size = 12.sp)
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = scheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val active = option.first == selected
                val container by animateColorAsState(
                    targetValue = if (active) scheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(Motion.Fast),
                    label = "segmentContainer",
                )
                val content by animateColorAsState(
                    targetValue = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    animationSpec = tween(Motion.Fast),
                    label = "segmentContent",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(container)
                        .clickable { onSelect(option.first) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option.second,
                        maxLines = 1,
                        color = content,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BusyIndicator()
        Spacer(Modifier.width(10.dp))
        CaptionText(message, size = 12.sp)
    }
}

@Composable
private fun HeaderCell(label: String, width: Dp) {
    Box(Modifier.width(width), contentAlignment = Alignment.CenterEnd) {
        SectionLabel(label)
    }
}

@Composable
private fun VerticalRule(height: Dp, inset: Dp = 8.dp) {
    Box(
        Modifier
            .padding(horizontal = inset)
            .width(1.dp)
            .height(height)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    )
}

private fun compositionParts(tokens: UsageTokens): List<Pair<String, Long>> = listOf(
    "Prompt" to tokens.input,
    "Completion" to tokens.output,
    "Cache read" to tokens.cacheRead,
    "Cache write" to tokens.cacheWrite,
)

private fun countOf(count: Int, singular: String, plural: String): String {
    return count.toString() + " " + (if (count == 1) singular else plural)
}

private fun percentOf(value: Long, total: Long): Int {
    if (total <= 0L) return 0
    return ((value * 100.0) / total.toDouble()).toInt()
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
