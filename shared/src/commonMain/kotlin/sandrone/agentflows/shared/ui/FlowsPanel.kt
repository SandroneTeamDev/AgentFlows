package sandrone.agentflows.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sandrone.agentflows.shared.AgentFlowEntry
import sandrone.agentflows.shared.design.CaptionText
import sandrone.agentflows.shared.design.ChipFilterRow
import sandrone.agentflows.shared.design.CodeSurface
import sandrone.agentflows.shared.design.Collapsible
import sandrone.agentflows.shared.design.EmptyHint
import sandrone.agentflows.shared.design.MetaChip
import sandrone.agentflows.shared.design.MonoText
import sandrone.agentflows.shared.design.NumberCell
import sandrone.agentflows.shared.design.PulsingDot
import sandrone.agentflows.shared.design.Radius
import sandrone.agentflows.shared.design.SectionLabel
import sandrone.agentflows.shared.design.Space
import sandrone.agentflows.shared.design.StatBlock
import sandrone.agentflows.shared.design.ThinRule
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.design.UnderlineTabs
import sandrone.agentflows.shared.design.isDesktop
import sandrone.agentflows.shared.design.metrics
import sandrone.agentflows.shared.i18n.flowLabel
import sandrone.agentflows.shared.i18n.pick
import sandrone.agentflows.shared.nowMillis

private fun flowFilters(): List<Pair<String, String>> = listOf(
    "all" to pick("全部", "All"),
    "trigger" to pick("触发", "Trigger"),
    "router_decision" to pick("路由", "Router"),
    "worker_start" to pick("执行体", "Worker"),
    "tool_call" to pick("工具", "Tool"),
    "error" to pick("告警", "Alert"),
)

private fun flowTypeText(type: String): String = when (type) {
    "trigger" -> pick("触发", "trigger")
    "router_decision" -> pick("路由", "router decision")
    "worker_start" -> pick("执行体启动", "worker start")
    "worker_done" -> pick("执行体完成", "worker done")
    "tool_call" -> pick("工具调用", "tool call")
    "error" -> pick("告警", "error")
    "watchdog_alert" -> pick("看门狗告警", "watchdog alert")
    "speculative" -> pick("推测", "speculative")
    else -> type.replace('_', ' ')
}

private val TimeColumn = 64.dp
private val TypeColumn = 132.dp
private val AgeColumn = 62.dp
private val ChevronColumn = 16.dp
private val MobileRail = 22.dp
private val MobileDot = 10.dp
private val MobileSpineInset = 10.dp
private const val LiveWindowMillis = 120_000L

private fun countOf(flows: List<AgentFlowEntry>, key: String): Int =
    if (key == "all") flows.size else flows.count { it.type == key }

private fun filterCounts(flows: List<AgentFlowEntry>): Map<String, Int> =
    flowFilters().associate { (key, _) -> key to countOf(flows, key) }

private fun newestFirst(flows: List<AgentFlowEntry>): List<AgentFlowEntry> =
    flows.sortedByDescending { it.timestamp }

private fun clockOf(millis: Long): String =
    if (millis <= 0L) "--:--:--" else formatClock(millis) + ":" + ((millis / 1000) % 60).toString().padStart(2, '0')

private fun flowAgeLabel(millis: Long, now: Long): String {
    val delta = (now - millis).coerceAtLeast(0)
    if (millis <= 0L) return pick("未知", "unknown")
    if (delta < 60_000L) return pick("刚刚", "just now")
    if (delta < 3_600_000L) {
        val minutes = delta / 60_000L
        return pick("$minutes 分钟前", "${minutes}m ago")
    }
    if (delta < MILLIS_PER_DAY) {
        val hours = delta / 3_600_000L
        return pick("$hours 小时前", "${hours}h ago")
    }
    val days = delta / MILLIS_PER_DAY
    return pick("$days 天前", "${days}d ago")
}

private fun filterLabel(key: String): String =
    flowFilters().firstOrNull { it.first == key }?.second ?: pick("匹配", "matching")

private fun flowTone(type: String): Tone = when (flowToneOf(type)) {
    FlowTone.TRIGGER -> Tone.WARNING
    FlowTone.ROUTER -> Tone.ACCENT
    FlowTone.WORKER -> Tone.POSITIVE
    FlowTone.TOOL -> Tone.NEUTRAL
    FlowTone.RESULT -> Tone.NEUTRAL
    FlowTone.ALERT -> Tone.DANGER
    FlowTone.NEUTRAL -> Tone.NEUTRAL
}

@Composable
private fun flowAccent(type: String): Color = when (flowToneOf(type)) {
    FlowTone.TRIGGER -> MaterialTheme.colorScheme.tertiary
    FlowTone.ROUTER -> MaterialTheme.colorScheme.primary
    FlowTone.WORKER -> MaterialTheme.colorScheme.secondary
    FlowTone.ALERT -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}

@Composable
fun FlowsPanel(
    flows: List<AgentFlowEntry>,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf("all") }
    val newest = remember(flows) { newestFirst(flows) }
    val visible = remember(newest, filter) {
        if (filter == "all") newest else newest.filter { it.type == filter }
    }
    val counts = remember(flows) { filterCounts(flows) }
    val now = nowMillis()
    if (isDesktop()) {
        DesktopFlowsPanel(
            flows = flows,
            visible = visible,
            counts = counts,
            filter = filter,
            now = now,
            onFilter = { filter = it },
            modifier = modifier,
        )
    } else {
        MobileFlowsPanel(
            flows = flows,
            visible = visible,
            counts = counts,
            filter = filter,
            now = now,
            onFilter = { filter = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun DesktopFlowsPanel(
    flows: List<AgentFlowEntry>,
    visible: List<AgentFlowEntry>,
    counts: Map<String, Int>,
    filter: String,
    now: Long,
    onFilter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = flowFilters()
    val keys = filters.map { it.first }
    val labels = filters.map { it.second }
    val selected = keys.indexOf(filter).coerceAtLeast(0)
    val newestStamp = flows.maxOfOrNull { it.timestamp } ?: 0L
    val live = newestStamp > 0L && now - newestStamp < LiveWindowMillis
    Column(modifier.fillMaxSize().padding(horizontal = metrics().gutter)) {
        Row(
            Modifier.fillMaxWidth().padding(top = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                UnderlineTabs(
                    tabs = labels,
                    selected = selected,
                    onSelect = { index -> onFilter(keys.getOrElse(index) { "all" }) },
                    counts = keys.map { counts[it] ?: 0 },
                )
            }
            Spacer(Modifier.width(Space.lg))
            FlowSummaryStrip(flows)
            Spacer(Modifier.width(Space.md))
            LiveBadge(live)
        }
        Spacer(Modifier.height(Space.xs))
        ThinRule(alpha = 0.7f)
        ColumnHeader()
        ThinRule(alpha = 0.55f)
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (flows.isEmpty()) {
                    EmptyHint(
                        AgentIcons.Bolt,
                        pick("暂无事件", "No flow events yet"),
                        pick("触发、路由决策、执行体启动、工具调用与告警会在发生时实时出现在这里。", "Triggers, Router decisions, Worker starts, tool calls and alerts appear here as they happen."),
                    )
                } else {
                    val filterName = filterLabel(filter)
                    EmptyHint(
                        AgentIcons.Filter,
                        pick("暂无${filterName}事件", "No ${filterName} events"),
                        pick("没有符合此筛选的事件。选择「全部」即可查看完整的会话时间线。", "Nothing matches this filter. Pick All to see the full session timeline."),
                    )
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(visible, key = { it.id }) { entry ->
                    DesktopFlowRow(entry, now)
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(pick("时间", "Time"), Modifier.width(TimeColumn))
        SectionLabel(pick("类型", "Type"), Modifier.width(TypeColumn))
        SectionLabel(pick("事件", "Event"), Modifier.weight(1f))
        Box(Modifier.width(AgeColumn), contentAlignment = Alignment.CenterEnd) {
            SectionLabel(pick("距今", "Age"))
        }
        Spacer(Modifier.width(ChevronColumn))
    }
}

@Composable
private fun DesktopFlowRow(entry: AgentFlowEntry, now: Long) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText(clockOf(entry.timestamp), Modifier.width(TimeColumn), size = 10.5.sp)
            Box(Modifier.width(TypeColumn)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(flowAccent(entry.type)),
                    )
                    Spacer(Modifier.width(Space.xs + Space.xxs))
                    MetaChip(flowTypeText(entry.type), tone = Tone.MUTED)
                }
            }
            Text(
                flowLabel(entry.label),
                Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scheme.onSurface,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            )
            NumberCell(
                flowAgeLabel(entry.timestamp, now),
                width = AgeColumn,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Space.xs))
            Icon(
                if (expanded) AgentIcons.ChevronUp else AgentIcons.ChevronDown,
                null,
                Modifier.size(12.dp),
                tint = scheme.outline,
            )
        }
        Collapsible(expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = TimeColumn + TypeColumn, end = Space.lg, top = Space.xxs, bottom = Space.sm),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(pick("载荷", "Payload"))
                    Spacer(Modifier.weight(1f))
                    val lineCount = entry.details.lines().size
                    MonoText(entry.type + " · " + pick("$lineCount 行", "${lineCount}L"), size = 10.sp)
                }
                Spacer(Modifier.height(Space.xs))
                if (entry.details.isBlank()) {
                    CaptionText(pick("此事件没有记录载荷。", "No payload recorded for this event."), maxLines = 1, size = 11.sp)
                } else {
                    CodeSurface(entry.details)
                }
            }
        }
        ThinRule(alpha = 0.3f)
    }
}

@Composable
private fun MobileFlowsPanel(
    flows: List<AgentFlowEntry>,
    visible: List<AgentFlowEntry>,
    counts: Map<String, Int>,
    filter: String,
    now: Long,
    onFilter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = metrics().gutter
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = Space.md)) {
            ChipFilterRow(
                options = flowFilters(),
                selected = filter,
                onSelect = onFilter,
                counts = counts,
            )
            if (flows.isNotEmpty()) {
                Spacer(Modifier.height(Space.md))
                FlowSummaryStrip(flows)
            }
        }
        Spacer(Modifier.height(Space.md))
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (flows.isEmpty()) {
                    EmptyHint(
                        AgentIcons.Bolt,
                        pick("暂无事件", "No flow events yet"),
                        pick("触发、路由决策、执行体启动、工具调用与告警会在发生时实时出现在这里。", "Triggers, Router decisions, Worker starts, tool calls and alerts appear here as they happen."),
                    )
                } else {
                    val filterName = filterLabel(filter)
                    EmptyHint(
                        AgentIcons.Filter,
                        pick("暂无${filterName}事件", "No ${filterName} events"),
                        pick("没有符合此筛选的事件。选择「全部」即可查看完整的会话时间线。", "Nothing matches this filter. Pick All to see the full session timeline."),
                    )
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    Modifier
                        .padding(start = gutter + MobileSpineInset, top = Space.md, bottom = Space.xl)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    items(visible, key = { it.id }) { entry ->
                        MobileFlowCard(entry, now)
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun MobileFlowCard(entry: AgentFlowEntry, now: Long) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.lg)
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier.width(MobileRail).padding(top = Space.lg + Space.xs),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.size(MobileDot).clip(CircleShape).background(flowAccent(entry.type)))
        }
        Surface(
            color = scheme.surfaceContainer,
            shape = shape,
            modifier = Modifier.weight(1f).clip(shape).clickable { expanded = !expanded },
        ) {
            Column(Modifier.padding(horizontal = Space.md + Space.xxs, vertical = Space.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaChip(flowTypeText(entry.type), tone = flowTone(entry.type))
                    Spacer(Modifier.width(Space.sm))
                    MonoText(flowAgeLabel(entry.timestamp, now), size = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (expanded) AgentIcons.ChevronUp else AgentIcons.ChevronDown,
                        null,
                        Modifier.size(18.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
                Text(
                    flowLabel(entry.label),
                    Modifier.padding(top = Space.sm),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                )
                Collapsible(expanded) {
                    Column(Modifier.fillMaxWidth().padding(top = Space.sm)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            MonoText(entry.type, size = 10.sp)
                            Spacer(Modifier.weight(1f))
                            MonoText(clockOf(entry.timestamp), size = 10.sp)
                        }
                        Spacer(Modifier.height(Space.xs))
                        if (entry.details.isBlank()) {
                            CaptionText(pick("此事件没有记录载荷。", "No payload recorded for this event."), maxLines = 1, size = 11.sp)
                        } else {
                            CodeSurface(entry.details)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowSummaryStrip(flows: List<AgentFlowEntry>, modifier: Modifier = Modifier) {
    val triggers = flows.count { it.type == "trigger" }
    val workers = flows.count { it.type == "worker_start" }
    val tools = flows.count { it.type == "tool_call" }
    val alerts = flows.count { it.type == "error" || it.type == "watchdog_alert" }
    if (isDesktop()) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InlineStat(pick("触发", "Triggers"), triggers.toString())
            InlineStat(pick("执行体", "Workers"), workers.toString())
            InlineStat(pick("工具调用", "Tool calls"), tools.toString())
            InlineStat(pick("告警", "Alerts"), alerts.toString(), accent = MaterialTheme.colorScheme.error)
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(Radius.lg),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatBlock(pick("触发", "Triggers"), triggers.toString(), Modifier.weight(1f))
                SummaryDivider()
                StatBlock(pick("执行体", "Workers"), workers.toString(), Modifier.weight(1f))
                SummaryDivider()
                StatBlock(pick("工具调用", "Tool calls"), tools.toString(), Modifier.weight(1f))
                SummaryDivider()
                StatBlock(
                    pick("告警", "Alerts"),
                    alerts.toString(),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        Modifier
            .padding(horizontal = Space.xxs)
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}

@Composable
private fun InlineStat(label: String, value: String, accent: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(label)
        Spacer(Modifier.width(Space.xs))
        MonoText(
            value,
            color = accent ?: MaterialTheme.colorScheme.onSurface,
            size = 11.sp,
            weight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LiveBadge(live: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (live) {
            PulsingDot(size = 5.dp)
        } else {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            )
        }
        Spacer(Modifier.width(Space.xs))
        MonoText(if (live) pick("实时", "live") else pick("空闲", "idle"), size = 10.sp)
    }
}

@Composable
fun RecentFlowPreview(flows: List<AgentFlowEntry>, modifier: Modifier = Modifier) {
    val desktop = isDesktop()
    val latest = remember(flows) { newestFirst(flows).firstOrNull() }
    if (latest == null) {
        CaptionText(
            pick("此会话暂无事件记录。", "No flow events recorded in this session."),
            modifier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            size = if (desktop) metrics().captionSize else 13.sp,
        )
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MetaChip(flowTypeText(latest.type), tone = flowTone(latest.type), mono = desktop)
        Spacer(Modifier.width(if (desktop) Space.sm else Space.md))
        Text(
            flowLabel(latest.label),
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = if (desktop) 11.5.sp else 13.sp),
        )
        Spacer(Modifier.width(Space.sm))
        MonoText(flowAgeLabel(latest.timestamp, nowMillis()), size = if (desktop) 10.sp else 11.sp)
    }
}
