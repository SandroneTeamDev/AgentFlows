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
import androidx.compose.ui.unit.TextUnit
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
import sandrone.agentflows.shared.design.ThinRule
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.design.isDesktop
import sandrone.agentflows.shared.design.metrics
import sandrone.agentflows.shared.i18n.errorLabel
import sandrone.agentflows.shared.i18n.pick
import sandrone.agentflows.shared.i18n.roleLabel
import sandrone.agentflows.shared.i18n.statusLabel
import sandrone.agentflows.shared.nowMillis

private val WidthTokens = 60.dp
private val WidthCost = 62.dp
private val WidthCalls = 44.dp
private val WidthFail = 42.dp
private val WidthAvg = 66.dp
private val WidthProvider = 76.dp
private val WidthRole = 58.dp
private val WidthLast = 58.dp
private val WidthTools = 40.dp
private val WidthSuccess = 58.dp
private val WidthCache = 54.dp
private val WidthShare = 80.dp

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
            exportNotice = pick(
                "已复制 " + payload.lines().size + " 行，格式 " + format.uppercase() + "。",
                "Copied " + payload.lines().size + " rows as " + format.uppercase() + ".",
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            exportNotice = pick(
                "导出失败：" + (error.message ?: "未知错误"),
                "Export failed: " + (error.message ?: "unknown error"),
            )
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
            failure = error.message ?: pick("用量服务不可达", "usage service unreachable")
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
                        text = rangeText(option),
                        selected = option == range,
                        onClick = { onRange(option) },
                        mono = true,
                    )
                }
                VerticalRule(14.dp)
                UsageGranularity.entries.forEach { option ->
                    MetaChip(
                        text = granularityText(option),
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
                MonoText(statusLabel("offline"), color = MaterialTheme.colorScheme.error)
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
                item { PanelCard { LoadingBlock(pick("正在收集该范围的用量", "Collecting usage for this range")) } }
            } else if (!hasData && failureText == null) {
                item {
                    PanelCard {
                        EmptyHint(
                            icon = AgentIcons.Chart,
                            title = pick("暂无用量记录", "No usage recorded"),
                            detail = pick(
                                "Token 与费用统计只保留在本机。只要引擎为所选范围记录了一次模型调用，条目就会立即出现。",
                                "Token and cost accounting stays on this device. Entries appear as soon as the engine records a model call for the selected range.",
                            ),
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
        failureText != null -> pick("引擎不可达，保留上次快照", "Engine unreachable, last snapshot kept")
        initialLoading -> pick("正在收集首个快照", "Collecting the first snapshot")
        summary.statsUpdatedAt > 0 -> pick("更新于 ", "Updated ") + formatRelative(summary.statsUpdatedAt, now)
        else -> pick("等待首个快照", "Waiting for the first snapshot")
    }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(AgentIcons.Chart, size = 40.dp, shape = RoundedCornerShape(Radius.md))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pick("用量台账", "Usage ledger"),
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
                options = UsageRange.entries.map { it.name to rangeText(it) },
                selected = range.name,
                onSelect = { key ->
                    UsageRange.entries.firstOrNull { it.name == key }?.let { onRange(it) }
                },
            )
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = UsageGranularity.entries.map { it.name to granularityText(it) },
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
                item { PanelCard { LoadingBlock(pick("正在收集该范围的用量", "Collecting usage for this range")) } }
            } else if (!hasData && failureText == null) {
                item {
                    PanelCard {
                        EmptyHint(
                            icon = AgentIcons.Chart,
                            title = pick("暂无用量记录", "No usage recorded"),
                            detail = pick(
                                "Token 与费用统计只保留在本机。只要引擎为所选范围记录了一次模型调用，条目就会立即出现。",
                                "Token and cost accounting stays on this device. Entries appear as soon as the engine records a model call for the selected range.",
                            ),
                            dense = false,
                        )
                    }
                }
            }
            if (hasData) {
                item { MobileMetricTiles(summary) }
                item { MobileTrendCard(summary, range, granularity) }
                item { MobileContextCard(summary) }
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
                    item { CaptionText(pick("没有会话符合该筛选条件。", "No session matches that filter."), Modifier.padding(horizontal = 4.dp), size = 13.sp) }
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
    val stats = usageMetrics(summary)
    PanelCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            KpiBlock(
                pick("Token", "Tokens"),
                formatCount(stats.tokens),
                Modifier.weight(1f),
                pick(
                    formatCount(stats.tokensIn) + " 入 / " + formatCount(stats.tokensOut) + " 出",
                    formatCount(stats.tokensIn) + " in / " + formatCount(stats.tokensOut) + " out",
                ),
            )
            KpiBlock(
                pick("费用", "Cost"),
                formatCost(stats.actualCost),
                Modifier.weight(1f),
                if (stats.priced) pick("实收估算", "billed estimate") else pick("含未定价模型", "unpriced models"),
                accent = if (stats.priced) null else scheme.tertiary,
            )
            KpiBlock(pick("调用", "Calls"), stats.requests.toString(), Modifier.weight(1f), pick("模型调用", "model calls"))
            KpiBlock(pick("会话", "Sessions"), stats.sessions.toString(), Modifier.weight(1f), pick("有活动", "with activity"))
            KpiBlock(
                pick("缓存命中率", "Cache hit rate"),
                formatPercent1(stats.cacheRate),
                Modifier.weight(1f),
                pick(
                    formatCount(stats.cacheHits) + " 复用 / " + formatCount(stats.tokensIn) + " 输入",
                    formatCount(stats.cacheHits) + " reused / " + formatCount(stats.tokensIn) + " in",
                ),
                accent = scheme.primary,
            )
            KpiBlock(
                pick("成功率", "Success rate"),
                formatPercent1(stats.successRate),
                Modifier.weight(1f),
                if (stats.failures == 0) pick("无失败", "no failures") else pick(stats.failures.toString() + " 次失败", countOf(stats.failures, "failure", "failures")),
                accent = if (stats.failures > 0) scheme.error else null,
            )
            KpiBlock(
                pick("平均延迟", "Avg latency"),
                if (stats.hasLatency) formatLatency(stats.latencyMs) else "—",
                Modifier.weight(1f),
                pick("按调用加权", "call weighted"),
            )
            KpiBlock(
                pick("单次 Token", "Tokens per call"),
                formatCount(stats.perCall),
                Modifier.weight(1f),
                pick("每次调用", "per call"),
            )
        }
    }
}

@Composable
private fun KpiBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Color? = null,
    valueSize: TextUnit = 19.sp,
    captionSize: TextUnit = 10.5.sp,
) {
    Column(modifier) {
        SectionLabel(label)
        MonoText(
            value,
            Modifier.padding(top = 3.dp),
            color = accent ?: MaterialTheme.colorScheme.onSurface,
            size = valueSize,
            weight = FontWeight.SemiBold,
        )
        if (caption != null) {
            CaptionText(caption, Modifier.padding(top = 2.dp), maxLines = 1, size = captionSize)
        }
    }
}

@Composable
private fun DesktopBody(summary: UsageSummary, tools: List<UsageToolBucket>, granularity: UsageGranularity) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopTrendCard(summary, granularity)
            DesktopSessionRankCard(summary.bySession, summary.totals.actualCost)
            DesktopModelTable(summary.byModel, summary.totals.tokens)
            DesktopRoleTable(summary.byRole, summary.totals.tokens)
            DesktopActivityCard(summary.heatmap)
        }
        Column(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopContextCard(summary)
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
    val bars = trendBars(summary)
    val total = bars.sumOf { it.total }
    val peak = bars.maxOfOrNull { it.total } ?: 0L
    val average = if (bars.isEmpty()) 0L else total / bars.size
    PanelCard {
        PanelHeader(
            title = pick("趋势", "Trend"),
            subtitle = trendSubtitle(bars.size, granularity),
            icon = AgentIcons.Chart,
            trailing = { TrendLegend(10.sp) },
        )
        Spacer(Modifier.height(10.dp))
        if (bars.isEmpty()) {
            CaptionText(pick("该范围内没有记录。", "Nothing recorded in this range."), size = 11.sp)
        } else {
            StackedBarStrip(bars, height = 76.dp, showLabels = bars.size <= 24)
        }
        Spacer(Modifier.height(8.dp))
        ThinRule(alpha = 0.3f)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText(pick("峰值 ", "peak ") + formatCount(peak) + pick(" / 均值 ", " / avg ") + formatCount(average))
            Spacer(Modifier.weight(1f))
            MonoText(pick("范围 " + rangeWireText(summary.range) + " / " + granularityWireText(summary.granularity), "range " + summary.range + " / " + summary.granularity))
        }
    }
}

@Composable
private fun StackedBarStrip(bars: List<TrendBar>, height: Dp, showLabels: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val peak = bars.maxOfOrNull { it.total }?.takeIf { it > 0L } ?: 1L
    val labelEvery = if (bars.size <= 8) 1 else (bars.size + 7) / 8
    Row(
        Modifier.fillMaxWidth().height(height + if (showLabels) 16.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isDesktop()) 3.dp else 5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, bar ->
            val stack = bar.input + bar.output + bar.cacheRead
            val fraction = (bar.total.toFloat() / peak.toFloat()).coerceIn(0f, 1f)
            val barHeight = if (bar.total > 0L) (height * fraction).coerceAtLeast(2.dp) else 1.dp
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.fillMaxWidth().height(barHeight).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (bar.total > 0L && stack <= 0L) {
                        Box(Modifier.fillMaxWidth().height(barHeight).background(scheme.primary.copy(alpha = 0.85f)))
                    } else if (bar.total > 0L) {
                        val cacheHeight = barHeight * (bar.cacheRead.toFloat() / stack.toFloat())
                        val outputHeight = barHeight * (bar.output.toFloat() / stack.toFloat())
                        val inputHeight = barHeight * (bar.input.toFloat() / stack.toFloat())
                        if (bar.cacheRead > 0L) {
                            Box(Modifier.fillMaxWidth().height(cacheHeight).background(scheme.secondary.copy(alpha = 0.7f)))
                        }
                        if (bar.output > 0L) {
                            Box(Modifier.fillMaxWidth().height(outputHeight).background(scheme.tertiary.copy(alpha = 0.85f)))
                        }
                        if (bar.input > 0L) {
                            Box(Modifier.fillMaxWidth().height(inputHeight).background(scheme.primary.copy(alpha = 0.85f)))
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().height(2.dp).background(scheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
                if (showLabels) {
                    MonoText(
                        if (index % labelEvery == 0) bar.label else "",
                        Modifier.padding(top = 3.dp),
                        size = if (isDesktop()) 8.5.sp else 9.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendLegend(size: TextUnit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendDot(scheme.primary.copy(alpha = 0.85f), pick("输入", "Input"), size)
        LegendDot(scheme.tertiary.copy(alpha = 0.85f), pick("输出", "Output"), size)
        LegendDot(scheme.secondary.copy(alpha = 0.7f), pick("缓存读", "Cache read"), size)
    }
}

@Composable
private fun LegendDot(color: Color, label: String, size: TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        MonoText(label, size = size)
    }
}

@Composable
private fun DesktopContextCard(summary: UsageSummary) {
    val scheme = MaterialTheme.colorScheme
    val stats = usageMetrics(summary)
    val reuse = if (stats.tokens > 0L) stats.cacheHits.toDouble() / stats.tokens.toDouble() else 0.0
    val top = topCacheSessions(summary.bySession, 3)
    val cacheBase = summary.bySession.sumOf { it.cacheHits }.coerceAtLeast(1L)
    PanelCard {
        PanelHeader(
            title = pick("上下文洞察", "Context insight"),
            subtitle = pick("命中率、延迟与复用", "hit rate, latency and reuse"),
            icon = AgentIcons.Wave,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Donut(
                    slices = listOf(
                        pick("命中", "hit") to (stats.cacheRate * 100.0).toFloat(),
                        pick("未命中", "miss") to ((1.0 - stats.cacheRate) * 100.0).toFloat(),
                    ),
                    colors = listOf(scheme.primary, scheme.outlineVariant.copy(alpha = 0.45f)),
                    size = 84.dp,
                    strokeRatio = 0.2f,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MonoText(
                        formatPercent1(stats.cacheRate),
                        color = scheme.onSurface,
                        size = 14.sp,
                        weight = FontWeight.SemiBold,
                    )
                    CaptionText(pick("命中率", "hit rate"), size = 9.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                KVRow(pick("单次平均 Token", "Tokens per call"), if (stats.requests > 0) formatCount(stats.perCall) else "—")
                KVRow(pick("平均延迟", "Avg latency"), if (stats.hasLatency) formatLatency(stats.latencyMs) else "—")
                KVRow(pick("上下文复用", "Context reuse"), formatPercent1(reuse))
            }
        }
        Spacer(Modifier.height(6.dp))
        ThinRule(alpha = 0.4f)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("复用最高会话", "Top reuse sessions"))
            Spacer(Modifier.weight(1f))
            SectionLabel(pick("命中 Token", "cached tokens"))
        }
        Spacer(Modifier.height(4.dp))
        if (top.isEmpty()) {
            CaptionText(pick("该范围内没有会话记录。", "No sessions recorded in this range."), Modifier.padding(vertical = 4.dp), size = 10.5.sp)
        } else {
            top.forEachIndexed { index, entry ->
                if (index > 0) {
                    ThinRule(alpha = 0.25f)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonoText(
                        entry.sessionId.take(14).ifBlank { pick("未分配", "unassigned") },
                        Modifier.weight(1f),
                        size = 10.sp,
                        color = scheme.onSurface,
                    )
                    NumberCell(formatCount(entry.hits), width = WidthTokens)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(64.dp)) {
                        Meter(entry.hits.toFloat() / cacheBase.toFloat(), height = 3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSessionRankCard(sessions: List<UsageSessionBucket>, totalCost: Double) {
    val scheme = MaterialTheme.colorScheme
    val ranked = sessions.sortedByDescending { it.actualCost }.take(5)
    val costBase = totalCost.coerceAtLeast(0.01)
    PanelCard {
        PanelHeader(
            title = pick("会话排行 · 按费用", "Session ranking by cost"),
            subtitle = pick("费用最高的 5 个会话", "top 5 sessions by cost"),
            icon = AgentIcons.Users,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("会话", "Session"), Modifier.weight(1f))
            HeaderCell(pick("角色", "Role"), WidthRole)
            HeaderCell(pick("调用", "Calls"), WidthCalls)
            HeaderCell(pick("Token", "Tokens"), WidthTokens)
            Spacer(Modifier.width(10.dp))
            HeaderCell(pick("平均", "Avg"), WidthAvg)
            Spacer(Modifier.width(8.dp))
            HeaderCell(pick("费用", "Cost"), WidthCost)
            Spacer(Modifier.width(10.dp))
            HeaderCell(pick("占比", "Share"), WidthShare)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        if (ranked.isEmpty()) {
            CaptionText(pick("暂无会话记录。", "No sessions recorded."), Modifier.padding(vertical = 8.dp), size = 11.sp)
        } else {
            ranked.forEachIndexed { index, session ->
                if (index > 0) {
                    ThinRule(alpha = 0.3f)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonoText(
                        session.sessionId.take(16).ifBlank { pick("未分配", "unassigned") },
                        Modifier.weight(1f),
                        size = 10.5.sp,
                        color = scheme.onSurface,
                    )
                    MonoText(roleLabel(session.role).ifBlank { pick("执行体", "worker") }, Modifier.width(WidthRole), size = 10.5.sp)
                    NumberCell(session.requests.toString(), width = WidthCalls)
                    NumberCell(formatCount(session.tokens), width = WidthTokens)
                    Spacer(Modifier.width(10.dp))
                    NumberCell(pick(session.avgDurationMs.toString() + " ms", session.avgDurationMs.toString() + "ms"), width = WidthAvg)
                    Spacer(Modifier.width(8.dp))
                    NumberCell(formatCost(session.actualCost), width = WidthCost)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(WidthShare)) {
                        Meter((session.actualCost / costBase).toFloat(), height = 4.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopModelTable(models: List<UsageModelBucket>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    PanelCard {
        PanelHeader(pick("按模型", "By model"), subtitle = pick("占已记录 Token 的比例", "share of recorded tokens"), icon = AgentIcons.Bolt)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("模型", "Model"), Modifier.weight(1f))
            SectionLabel(pick("占比", "Share"), Modifier.weight(0.9f))
            HeaderCell(pick("服务商", "Provider"), WidthProvider)
            HeaderCell(pick("调用", "Calls"), WidthCalls)
            HeaderCell(pick("成功率", "Success"), WidthSuccess)
            HeaderCell(pick("缓存", "Cache"), WidthCache)
            HeaderCell(pick("Token", "Tokens"), WidthTokens)
            HeaderCell(pick("费用", "Cost"), WidthCost)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        if (models.isEmpty()) {
            CaptionText(pick("暂无模型调用记录。", "No model calls recorded."), Modifier.padding(vertical = 8.dp), size = 11.sp)
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
                            model.model.ifBlank { pick("未分配", "unassigned") },
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        )
                        if (!model.priced) {
                            Spacer(Modifier.width(6.dp))
                            MetaChip(pick("未定价", "unpriced"), tone = Tone.WARNING, mono = true)
                        }
                    }
                    Box(Modifier.weight(0.9f).padding(start = 10.dp, end = 10.dp)) {
                        Meter(model.tokens.toFloat() / total.toFloat(), height = 4.dp)
                    }
                    MonoText(model.provider.ifBlank { pick("本地", "local") }, Modifier.width(WidthProvider), size = 10.5.sp)
                    NumberCell(model.requests.toString(), width = WidthCalls)
                    NumberCell(modelSuccessText(model), width = WidthSuccess)
                    NumberCell(modelCacheText(model), width = WidthCache)
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
        PanelHeader(pick("按角色", "By role"), subtitle = pick("路由、执行体、看门狗、子代理", "router, worker, watchdog, subagent"), icon = AgentIcons.Settings)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("角色", "Role"), Modifier.weight(1f))
            SectionLabel(pick("占比", "Share"), Modifier.weight(0.9f))
            HeaderCell(pick("Token", "Tokens"), WidthTokens)
            HeaderCell(pick("费用", "Cost"), WidthCost)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(alpha = 0.4f)
        if (roles.isEmpty()) {
            CaptionText(pick("暂无角色活动记录。", "No role activity recorded."), Modifier.padding(vertical = 8.dp), size = 11.sp)
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
                        roleLabel(role.label.ifBlank { role.key }).ifBlank { pick("未知", "unknown") },
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
            title = pick("上下文构成", "Context composition"),
            subtitle = pick("提示、补全、缓存读、缓存写", "prompt, completion, cache read, cache write"),
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
            MonoText(pick("上下文 Token", "context tokens"))
            Spacer(Modifier.weight(1f))
            MonoText(formatCount(total), size = 10.sp, color = scheme.onSurface)
        }
    }
}

@Composable
private fun DesktopCostCard(breakdown: UsageCost, totals: UsageBucket, priced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader(pick("费用明细", "Cost breakdown"), subtitle = pick("标准价与实收", "standard vs billed"), icon = AgentIcons.Bolt)
        Spacer(Modifier.height(6.dp))
        KVRow(pick("提示", "Prompt"), formatCost(breakdown.input))
        KVRow(pick("补全", "Completion"), formatCost(breakdown.output))
        KVRow(pick("缓存读", "Cache read"), formatCost(breakdown.cacheRead))
        KVRow(pick("缓存写", "Cache write"), formatCost(breakdown.cacheWrite))
        Spacer(Modifier.height(2.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(2.dp))
        KVRow(pick("标准价", "Standard"), formatCost(totals.cost))
        KVRow(pick("实收", "Billed"), formatCost(totals.actualCost), valueColor = scheme.primary)
        if (totals.cost > totals.actualCost) {
            KVRow(pick("节省", "Saved"), formatCost(totals.cost - totals.actualCost), valueColor = scheme.secondary)
        }
        if (!priced) {
            Spacer(Modifier.height(6.dp))
            MetaChip(pick("含未定价模型", "unpriced models present"), tone = Tone.WARNING, mono = true)
        }
    }
}

@Composable
private fun DesktopToolTable(tools: List<UsageToolBucket>) {
    PanelCard {
        PanelHeader(
            title = pick("工具用量", "Tool usage"),
            subtitle = toolUsageSubtitle(tools),
            icon = AgentIcons.Terminal,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("工具", "Tool"), Modifier.weight(1f))
            HeaderCell(pick("调用", "Calls"), WidthCalls)
            HeaderCell(pick("失败", "Fail"), WidthFail)
            HeaderCell(pick("平均", "Avg"), WidthAvg)
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
                    tool.tool.ifBlank { pick("未知", "unknown") },
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
                NumberCell(pick(tool.avgDurationMs.toString() + " ms", tool.avgDurationMs.toString() + "ms"), width = WidthAvg)
            }
        }
    }
}

@Composable
private fun DesktopErrorTable(errors: List<UsageErrorBucket>) {
    PanelCard {
        PanelHeader(pick("失败", "Failures"), subtitle = pick("按类别", "by category"), icon = AgentIcons.AlertTriangle)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("类别", "Category"), Modifier.weight(1f))
            HeaderCell(pick("数量", "Count"), WidthCalls)
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
                    errorLabel(entry.category).ifBlank { pick("未分类", "uncategorised") },
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
        PanelHeader(pick("活动", "Activity"), subtitle = pick("每日请求数，最近 12 周", "requests per day, last 12 weeks"), icon = AgentIcons.Grid)
        Spacer(Modifier.height(10.dp))
        HeatGrid(dailyCounts = daily, weeks = 12)
        Spacer(Modifier.height(8.dp))
        ThinRule(alpha = 0.3f)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText(pick(formatCount(total) + " 次请求", formatCount(total) + " requests"))
            Spacer(Modifier.weight(1f))
            MonoText(pick("峰值 " + peak + " / 天", "peak " + peak + " / day"))
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
            SectionLabel(pick("会话", "Sessions"))
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
                        pick("筛选会话、角色或模型", "Filter session, role or model"),
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
                SectionLabel(pick("会话", "Session"), Modifier.weight(1.3f))
                SectionLabel(pick("角色", "Role"), Modifier.width(WidthRole))
                SectionLabel(pick("模型", "Models"), Modifier.weight(1.2f))
                HeaderCell(pick("Token", "Tokens"), WidthTokens)
                HeaderCell(pick("调用", "Calls"), WidthCalls)
                HeaderCell(pick("工具", "Tools"), WidthTools)
                HeaderCell(pick("平均", "Avg"), WidthAvg)
                HeaderCell(pick("最近", "Last"), WidthLast)
                HeaderCell(pick("费用", "Cost"), WidthCost)
                HeaderCell(pick("失败", "Fail"), WidthFail)
            }
            Spacer(Modifier.height(4.dp))
            ThinRule(alpha = 0.4f)
            if (sessions.isEmpty()) {
                CaptionText(pick("没有会话符合该筛选条件。", "No session matches that filter."), Modifier.padding(vertical = 10.dp), size = 11.sp)
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
                            session.sessionId.take(20).ifBlank { pick("未分配", "unassigned") },
                            Modifier.weight(1.3f),
                            size = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        MonoText(roleLabel(session.role).ifBlank { pick("执行体", "worker") }, Modifier.width(WidthRole), size = 10.5.sp)
                        MonoText(
                            session.models.joinToString(", ").ifBlank { pick("未知模型", "unknown model") },
                            Modifier.weight(1.2f),
                            size = 10.5.sp,
                        )
                        NumberCell(formatCount(session.tokens), width = WidthTokens)
                        NumberCell(session.requests.toString(), width = WidthCalls)
                        NumberCell(session.toolCalls.toString(), width = WidthTools)
                        NumberCell(pick(session.avgDurationMs.toString() + " ms", session.avgDurationMs.toString() + "ms"), width = WidthAvg)
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
            SectionLabel(pick("用量不可用", "Usage unavailable"), color = scheme.error)
            CaptionText(message, Modifier.padding(top = 1.dp), maxLines = 2, size = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        MonoText(pick("重试", "retry"), size = 10.sp, color = scheme.error)
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
                    pick("用量不可用", "Usage unavailable"),
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
                Text(pick("重试", "Retry"), color = scheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun MobileMetricTiles(summary: UsageSummary) {
    val scheme = MaterialTheme.colorScheme
    val stats = usageMetrics(summary)
    val gap = metrics().cardGap
    val valueSize = 22.sp
    val captionSize = 11.5.sp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("Token", "Tokens"),
                    formatCount(stats.tokens),
                    caption = pick(
                        formatCount(stats.tokensIn) + " 入 / " + formatCount(stats.tokensOut) + " 出",
                        formatCount(stats.tokensIn) + " in / " + formatCount(stats.tokensOut) + " out",
                    ),
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("费用", "Cost"),
                    formatCost(stats.actualCost),
                    caption = if (stats.priced) pick("实收估算", "billed estimate") else pick("含未定价模型", "unpriced models"),
                    accent = if (stats.priced) null else scheme.tertiary,
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("调用", "Calls"),
                    stats.requests.toString(),
                    caption = pick("模型调用", "model calls"),
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("会话", "Sessions"),
                    stats.sessions.toString(),
                    caption = pick("有活动", "with activity"),
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("缓存命中率", "Cache hit rate"),
                    formatPercent1(stats.cacheRate),
                    caption = pick(
                        formatCount(stats.cacheHits) + " 复用 / " + formatCount(stats.tokensIn) + " 输入",
                        formatCount(stats.cacheHits) + " reused / " + formatCount(stats.tokensIn) + " in",
                    ),
                    accent = scheme.primary,
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("成功率", "Success rate"),
                    formatPercent1(stats.successRate),
                    caption = if (stats.failures == 0) pick("无失败", "no failures") else pick(stats.failures.toString() + " 次失败", countOf(stats.failures, "failure", "failures")),
                    accent = if (stats.failures > 0) scheme.error else null,
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("平均延迟", "Avg latency"),
                    if (stats.hasLatency) formatLatency(stats.latencyMs) else "—",
                    caption = pick("按调用加权", "call weighted"),
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
            PanelCard(Modifier.weight(1f)) {
                KpiBlock(
                    pick("单次 Token", "Tokens per call"),
                    formatCount(stats.perCall),
                    caption = pick("每次调用", "per call"),
                    valueSize = valueSize,
                    captionSize = captionSize,
                )
            }
        }
    }
}

@Composable
private fun MobileTrendCard(summary: UsageSummary, range: UsageRange, granularity: UsageGranularity) {
    val bars = trendBars(summary)
    val total = bars.sumOf { it.total }
    val peak = bars.maxOfOrNull { it.total } ?: 0L
    val average = if (bars.isEmpty()) 0L else total / bars.size
    PanelCard {
        PanelHeader(
            title = pick("趋势", "Trend"),
            subtitle = trendSubtitle(bars.size, granularity),
            icon = AgentIcons.Chart,
            trailing = { TrendLegend(9.5.sp) },
        )
        Spacer(Modifier.height(12.dp))
        if (bars.isEmpty()) {
            CaptionText(pick("该范围内没有记录。", "Nothing recorded in this range."), size = 13.sp)
        } else {
            StackedBarStrip(bars, height = 96.dp, showLabels = bars.size <= 12)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(pick("峰值 ", "peak ") + formatCount(peak) + pick(" / 均值 ", " / avg ") + formatCount(average), size = 11.sp)
            Spacer(Modifier.weight(1f))
            MonoText(pick(rangeText(range) + " / " + granularityText(granularity), range.label + " / " + granularity.label.lowercase()), size = 11.sp)
        }
    }
}

@Composable
private fun MobileContextCard(summary: UsageSummary) {
    val scheme = MaterialTheme.colorScheme
    val stats = usageMetrics(summary)
    val reuse = if (stats.tokens > 0L) stats.cacheHits.toDouble() / stats.tokens.toDouble() else 0.0
    val top = topCacheSessions(summary.bySession, 3)
    val cacheBase = summary.bySession.sumOf { it.cacheHits }.coerceAtLeast(1L)
    PanelCard {
        PanelHeader(
            title = pick("上下文洞察", "Context insight"),
            subtitle = pick("命中率、延迟与复用", "hit rate, latency and reuse"),
            icon = AgentIcons.Wave,
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Donut(
                    slices = listOf(
                        pick("命中", "hit") to (stats.cacheRate * 100.0).toFloat(),
                        pick("未命中", "miss") to ((1.0 - stats.cacheRate) * 100.0).toFloat(),
                    ),
                    colors = listOf(scheme.primary, scheme.outlineVariant.copy(alpha = 0.45f)),
                    size = 132.dp,
                    strokeRatio = 0.18f,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MonoText(
                        formatPercent1(stats.cacheRate),
                        color = scheme.onSurface,
                        size = 20.sp,
                        weight = FontWeight.SemiBold,
                    )
                    CaptionText(pick("命中率", "hit rate"), size = 10.5.sp, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        KVRow(pick("单次平均 Token", "Tokens per call"), if (stats.requests > 0) formatCount(stats.perCall) else "—")
        KVRow(pick("平均延迟", "Avg latency"), if (stats.hasLatency) formatLatency(stats.latencyMs) else "—")
        KVRow(pick("上下文复用", "Context reuse"), formatPercent1(reuse))
        Spacer(Modifier.height(6.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("复用最高会话", "Top reuse sessions"))
            Spacer(Modifier.weight(1f))
            SectionLabel(pick("命中 Token", "cached tokens"))
        }
        Spacer(Modifier.height(4.dp))
        if (top.isEmpty()) {
            CaptionText(pick("该范围内没有会话记录。", "No sessions recorded in this range."), Modifier.padding(vertical = 4.dp), size = 12.sp)
        } else {
            top.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText(
                        entry.sessionId.take(16).ifBlank { pick("未分配", "unassigned") },
                        Modifier.weight(1f),
                        size = 12.sp,
                        color = scheme.onSurface,
                    )
                    NumberCell(formatCount(entry.hits), width = WidthTokens)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(88.dp)) {
                        Meter(entry.hits.toFloat() / cacheBase.toFloat(), height = 6.dp)
                    }
                }
            }
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
            title = pick("上下文构成", "Context composition"),
            subtitle = pick("提示、补全、缓存读、缓存写", "prompt, completion, cache read, cache write"),
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
        KVRow(pick("上下文 Token", "Context tokens"), formatCount(total))
    }
}

@Composable
private fun MobileCostCard(breakdown: UsageCost, totals: UsageBucket, priced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    PanelCard {
        PanelHeader(pick("费用明细", "Cost breakdown"), subtitle = pick("标准价与实收", "standard vs billed"), icon = AgentIcons.Bolt)
        Spacer(Modifier.height(10.dp))
        KVRow(pick("提示", "Prompt"), formatCost(breakdown.input))
        KVRow(pick("补全", "Completion"), formatCost(breakdown.output))
        KVRow(pick("缓存读", "Cache read"), formatCost(breakdown.cacheRead))
        KVRow(pick("缓存写", "Cache write"), formatCost(breakdown.cacheWrite))
        Spacer(Modifier.height(6.dp))
        ThinRule(alpha = 0.35f)
        Spacer(Modifier.height(6.dp))
        KVRow(pick("标准价", "Standard"), formatCost(totals.cost))
        KVRow(pick("实收", "Billed"), formatCost(totals.actualCost), valueColor = scheme.primary)
        if (totals.cost > totals.actualCost) {
            KVRow(pick("节省", "Saved"), formatCost(totals.cost - totals.actualCost), valueColor = scheme.secondary)
        }
        if (!priced) {
            Spacer(Modifier.height(10.dp))
            MetaChip(pick("含未定价模型", "unpriced models present"), tone = Tone.WARNING, mono = true)
        }
    }
}

@Composable
private fun MobileRoleCard(roles: List<UsagePoint>, totalTokens: Long) {
    val total = totalTokens.coerceAtLeast(1L)
    PanelCard {
        PanelHeader(pick("按角色", "By role"), subtitle = pick("路由、执行体、看门狗、子代理", "router, worker, watchdog, subagent"), icon = AgentIcons.Settings)
        Spacer(Modifier.height(10.dp))
        if (roles.isEmpty()) {
            CaptionText(pick("暂无角色活动记录。", "No role activity recorded."), size = 13.sp)
        } else {
            roles.forEach { role ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            roleLabel(role.label.ifBlank { role.key }).ifBlank { pick("未知", "unknown") },
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
                        pick(formatCount(role.tokens) + " Token / " + percentOf(role.tokens, total) + "%", formatCount(role.tokens) + " tokens / " + percentOf(role.tokens, total) + "%"),
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
        PanelHeader(pick("按模型", "By model"), subtitle = pick("占已记录 Token 的比例", "share of recorded tokens"), icon = AgentIcons.Bolt)
        Spacer(Modifier.height(10.dp))
        if (models.isEmpty()) {
            CaptionText(pick("暂无模型调用记录。", "No model calls recorded."), size = 13.sp)
        } else {
            models.forEach { model ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            model.model.ifBlank { pick("未分配", "unassigned") },
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        )
                        MonoText(formatCost(model.actualCost), size = 12.sp, color = scheme.onSurface)
                    }
                    CaptionText(
                        pick(model.requests.toString() + " 次调用", countOf(model.requests, "call", "calls")) + " / " + model.provider.ifBlank { pick("本地", "local") },
                        Modifier.padding(top = 2.dp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Meter(model.tokens.toFloat() / total.toFloat(), height = 6.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoText(
                            pick(formatCount(model.tokens) + " Token / " + percentOf(model.tokens, total) + "%", formatCount(model.tokens) + " tokens / " + percentOf(model.tokens, total) + "%"),
                            size = 11.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        if (!model.priced) {
                            MetaChip(pick("未定价", "unpriced"), tone = Tone.WARNING, mono = true)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoText(pick("成功率 ", "success ") + modelSuccessText(model), size = 11.sp)
                        Spacer(Modifier.width(14.dp))
                        MonoText(pick("缓存 ", "cache ") + modelCacheText(model), size = 11.sp)
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
            title = pick("工具用量", "Tool usage"),
            subtitle = toolUsageSubtitle(tools),
            icon = AgentIcons.Terminal,
        )
        Spacer(Modifier.height(10.dp))
        tools.forEach { tool ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    MonoText(tool.tool.ifBlank { pick("未知", "unknown") }, size = 13.sp, color = scheme.onSurface)
                    CaptionText(pick("平均 " + tool.avgDurationMs.toString() + " ms", tool.avgDurationMs.toString() + "ms avg"), Modifier.padding(top = 2.dp), maxLines = 1, size = 11.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    MonoText(pick(tool.calls.toString() + " 次调用", tool.calls.toString() + " calls"), size = 12.sp, color = scheme.onSurface)
                    CaptionText(
                        if (tool.failures > 0) pick(tool.failures.toString() + " 次失败", tool.failures.toString() + " failed") else pick("无失败", "no failures"),
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
        PanelHeader(pick("失败", "Failures"), subtitle = pick("按类别", "by category"), icon = AgentIcons.AlertTriangle)
        Spacer(Modifier.height(10.dp))
        errors.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    errorLabel(entry.category).ifBlank { pick("未分类", "uncategorised") },
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
        PanelHeader(pick("活动", "Activity"), subtitle = pick("每日请求数，最近 12 周", "requests per day, last 12 weeks"), icon = AgentIcons.Grid)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HeatGrid(dailyCounts = daily, weeks = 12)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(pick(formatCount(total) + " 次请求", formatCount(total) + " requests"), size = 11.sp)
            Spacer(Modifier.weight(1f))
            MonoText(pick("峰值 " + peak + " / 天", "peak " + peak + " / day"), size = 11.sp)
        }
    }
}

@Composable
private fun MobileExportCard(notice: String?, onCsv: () -> Unit, onJson: () -> Unit) {
    PanelCard {
        PanelHeader(pick("导出", "Export"), subtitle = pick("将该范围复制到剪贴板", "copy this range to the clipboard"), icon = AgentIcons.Download)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCsv) {
                Icon(AgentIcons.Copy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(pick("复制 CSV", "Copy CSV"))
            }
            TextButton(onClick = onJson) {
                Icon(AgentIcons.Code, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(pick("复制 JSON", "Copy JSON"))
            }
        }
        Spacer(Modifier.height(6.dp))
        CaptionText(
            notice ?: pick("该范围的每一行记录都会复制到本机，数据不会离开设备。", "Every recorded row for this range is copied locally. Nothing leaves the device."),
            maxLines = 3,
            size = 12.sp,
        )
    }
}

@Composable
private fun MobileSessionsHeader(total: Int, filtered: Int, query: String, onQuery: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pick("会话", "Sessions"))
            Spacer(Modifier.width(8.dp))
            MonoText(filtered.toString() + " / " + total, size = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(pick("筛选会话", "Filter sessions")) },
            placeholder = { Text(pick("会话、角色或模型", "Session, role or model")) },
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
                    session.sessionId.take(22).ifBlank { pick("未分配", "unassigned") },
                    size = 13.sp,
                    color = scheme.onSurface,
                    weight = FontWeight.Medium,
                )
                CaptionText(
                    roleLabel(session.role).ifBlank { pick("执行体", "worker") } + " / " + session.models.joinToString(", ").ifBlank { pick("未知模型", "unknown model") },
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
                KVRow(pick("Token", "tokens"), formatCount(session.tokens))
                KVRow(pick("调用", "calls"), session.requests.toString())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                KVRow(pick("工具", "tools"), session.toolCalls.toString())
                KVRow(pick("平均", "avg"), pick(session.avgDurationMs.toString() + " ms", session.avgDurationMs.toString() + "ms"))
            }
        }
        if (session.failures > 0) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AgentIcons.AlertTriangle, null, Modifier.size(13.dp), tint = scheme.error)
                Spacer(Modifier.width(6.dp))
                CaptionText(pick(session.failures.toString() + " 次失败", countOf(session.failures, "failure", "failures")), color = scheme.error, maxLines = 1, size = 12.sp)
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

private data class UsageMetrics(
    val tokens: Long,
    val tokensIn: Long,
    val tokensOut: Long,
    val cacheHits: Long,
    val cacheRate: Double,
    val requests: Int,
    val failures: Int,
    val successRate: Double,
    val latencyMs: Int,
    val hasLatency: Boolean,
    val perCall: Long,
    val actualCost: Double,
    val priced: Boolean,
    val sessions: Int,
)

private data class TrendBar(val label: String, val input: Long, val output: Long, val cacheRead: Long, val total: Long)

private data class CachedSession(val sessionId: String, val hits: Long)

private fun usageMetrics(summary: UsageSummary): UsageMetrics {
    val totals = summary.totals
    val cacheBase = totals.tokensIn + totals.cacheHits
    val cacheRate = if (cacheBase > 0L) totals.cacheHits.toDouble() / cacheBase.toDouble() else 0.0
    val successRate = if (totals.requests > 0) {
        (totals.requests - totals.failures).toDouble() / totals.requests.toDouble()
    } else {
        0.0
    }
    val weighted = summary.bySession.sumOf { it.avgDurationMs.toLong() * it.requests.toLong() }
    val calls = summary.bySession.sumOf { it.requests.toLong() }
    val latency = if (calls > 0L) (weighted / calls).toInt() else 0
    val perCall = if (totals.requests > 0) totals.tokens / totals.requests.toLong() else 0L
    return UsageMetrics(
        tokens = totals.tokens,
        tokensIn = totals.tokensIn,
        tokensOut = totals.tokensOut,
        cacheHits = totals.cacheHits,
        cacheRate = cacheRate,
        requests = totals.requests,
        failures = totals.failures,
        successRate = successRate,
        latencyMs = latency,
        hasLatency = calls > 0L && weighted > 0L,
        perCall = perCall,
        actualCost = totals.actualCost,
        priced = summary.priced,
        sessions = summary.bySession.size,
    )
}

private fun trendBars(summary: UsageSummary): List<TrendBar> {
    return summary.byPoint.takeLast(30).map { point ->
        val split = point.tokensIn + point.tokensOut + point.cacheHits
        val total = if (point.tokens > 0L) point.tokens else split
        if (split > 0L) {
            TrendBar(point.key.takeLast(5), point.tokensIn, point.tokensOut, point.cacheHits, total)
        } else {
            TrendBar(point.key.takeLast(5), point.tokens, 0L, 0L, total)
        }
    }
}

private fun topCacheSessions(sessions: List<UsageSessionBucket>, limit: Int): List<CachedSession> =
    sessions
        .map { CachedSession(it.sessionId.ifBlank { it.workerId }, it.cacheHits) }
        .filter { it.hits > 0L }
        .sortedByDescending { it.hits }
        .take(limit)

private fun modelSuccessText(model: UsageModelBucket): String {
    if (model.requests <= 0) return "—"
    val rate = (model.requests - model.failures).toDouble() / model.requests.toDouble()
    return formatPercent1(rate.coerceIn(0.0, 1.0))
}

private fun modelCacheText(model: UsageModelBucket): String {
    if (model.tokens <= 0L) return "—"
    return formatPercent1((model.cacheHits.toDouble() / model.tokens.toDouble()).coerceIn(0.0, 1.0))
}

private fun formatPercent1(value: Double): String {
    val tenths = (value.coerceIn(0.0, 1.0) * 1000.0).toLong()
    return (tenths / 10).toString() + "." + (tenths % 10).toString() + "%"
}

private fun formatLatency(ms: Int): String {
    if (ms <= 0) return "—"
    if (ms < 1000) return ms.toString() + " ms"
    val tenths = ms / 100
    return (tenths / 10).toString() + "." + (tenths % 10).toString() + " s"
}

private fun compositionParts(tokens: UsageTokens): List<Pair<String, Long>> = listOf(
    pick("提示", "Prompt") to tokens.input,
    pick("补全", "Completion") to tokens.output,
    pick("缓存读", "Cache read") to tokens.cacheRead,
    pick("缓存写", "Cache write") to tokens.cacheWrite,
)

private fun rangeText(range: UsageRange): String = when (range) {
    UsageRange.TODAY -> pick("今天", "Today")
    UsageRange.WEEK -> pick("近 7 天", "7 days")
    UsageRange.MONTH -> pick("近 30 天", "30 days")
    UsageRange.ALL -> pick("全部时间", "All time")
}

private fun granularityText(granularity: UsageGranularity): String = when (granularity) {
    UsageGranularity.HOUR -> pick("按小时", "Hourly")
    UsageGranularity.DAY -> pick("按天", "Daily")
    UsageGranularity.WEEK -> pick("按周", "Weekly")
}

private fun granularityWord(granularity: UsageGranularity): String = when (granularity) {
    UsageGranularity.HOUR -> "hourly"
    UsageGranularity.DAY -> "daily"
    UsageGranularity.WEEK -> "weekly"
}

private fun trendSubtitle(count: Int, granularity: UsageGranularity): String = pick(
    granularityText(granularity) + " · " + count + " 个区间",
    countOf(count, granularityWord(granularity) + " bucket", granularityWord(granularity) + " buckets"),
)

private fun toolUsageSubtitle(tools: List<UsageToolBucket>): String {
    val calls = tools.sumOf { it.calls }
    val failures = tools.sumOf { it.failures }
    return pick(
        calls.toString() + " 次调用 / " + failures.toString() + " 次失败",
        countOf(calls, "call", "calls") + " / " + countOf(failures, "failure", "failures"),
    )
}

private fun rangeWireText(wire: String): String = when (wire) {
    "today" -> "今天"
    "7d" -> "7 天"
    "30d" -> "30 天"
    "all" -> "全部时间"
    else -> wire
}

private fun granularityWireText(wire: String): String = when (wire) {
    "hour" -> "按小时"
    "day" -> "按天"
    "week" -> "按周"
    else -> wire
}

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
