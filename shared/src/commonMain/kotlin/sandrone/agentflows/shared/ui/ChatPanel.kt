package sandrone.agentflows.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sandrone.agentflows.shared.EffectPrefs
import sandrone.agentflows.shared.MotionTokens
import sandrone.agentflows.shared.WorkspaceSnapshot
import sandrone.agentflows.shared.design.AvatarTile
import sandrone.agentflows.shared.design.BusyIndicator
import sandrone.agentflows.shared.design.CaptionText
import sandrone.agentflows.shared.design.CodeSurface
import sandrone.agentflows.shared.design.Collapsible
import sandrone.agentflows.shared.design.GhostIconButton
import sandrone.agentflows.shared.design.MetaChip
import sandrone.agentflows.shared.design.MonoText
import sandrone.agentflows.shared.design.Radius
import sandrone.agentflows.shared.design.SectionLabel
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.effectSurface
import sandrone.agentflows.shared.design.isDesktop
import sandrone.agentflows.shared.design.metrics
import sandrone.agentflows.shared.nowMillis

private val Suggestions = listOf(
    "Summarise what the router is doing right now",
    "Show the most recent tool calls",
    "Explain why the last worker stopped",
)

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    snapshot: WorkspaceSnapshot,
    prefs: EffectPrefs,
    input: String,
    busy: Boolean,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val desktop = isDesktop()
    Column(modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            if (desktop) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    Box(Modifier.padding(top = 16.dp)) { DesktopIntro(snapshot, onInput) }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MobileIntro(snapshot, onInput)
                }
            }
        } else {
            if (desktop) DesktopMessageList(messages, Modifier.weight(1f)) else MobileMessageList(messages, Modifier.weight(1f))
        }
        if (desktop) {
            DesktopComposer(
                input = input,
                busy = busy,
                enabled = snapshot.connected,
                prefs = prefs,
                onInput = onInput,
                onSend = onSend,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
            )
        } else {
            MobileComposer(
                input = input,
                busy = busy,
                enabled = snapshot.connected,
                prefs = prefs,
                onInput = onInput,
                onSend = onSend,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun MobileIntro(snapshot: WorkspaceSnapshot, onInput: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AvatarTile(AgentIcons.Wave, size = 64.dp, shape = RoundedCornerShape(Radius.xl))
        Text(
            "AgentFlows",
            Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        CaptionText(
            if (snapshot.connected) "Local event-flow workspace is online" else "The local engine is not reachable on this device",
            Modifier.padding(top = 6.dp),
            maxLines = 2,
            size = 13.sp,
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaChip(if (snapshot.modelConfigured) "model ready" else "model off", tone = if (snapshot.modelConfigured) Tone.ACCENT else Tone.MUTED)
            MetaChip("${snapshot.workers.size} workers")
            MetaChip("${snapshot.flows.size} events")
        }
        Column(
            Modifier.padding(top = 22.dp).widthIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Suggestions.forEach { suggestion ->
                SuggestionRow(suggestion) { onInput(suggestion) }
            }
        }
    }
}

@Composable
private fun DesktopIntro(snapshot: WorkspaceSnapshot, onInput: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 560.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarTile(AgentIcons.Wave, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "AgentFlows",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                )
                CaptionText(
                    if (snapshot.connected) "Local engine online" else "Local engine unreachable",
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            MetaChip(if (snapshot.modelConfigured) "model ready" else "model off", tone = if (snapshot.modelConfigured) Tone.ACCENT else Tone.MUTED)
        }
        Spacer(Modifier.height(14.dp))
        CaptionText(
            "Send a task and the Router records it as a Trigger. Workers appear in the Flows timeline with their tool arguments and results.",
            maxLines = 3,
            size = 12.sp,
        )
        Spacer(Modifier.height(14.dp))
        SectionLabel("Try")
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Suggestions.forEach { suggestion ->
                SuggestionRow(suggestion) { onInput(suggestion) }
            }
        }
    }
}

@Composable
private fun SuggestionRow(text: String, onClick: () -> Unit) {
    val desktop = isDesktop()
    val shape = RoundedCornerShape(if (desktop) Radius.xs else Radius.md)
    val decoration = if (desktop) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(decoration)
            .clickable(onClick = onClick)
            .padding(horizontal = if (desktop) 9.dp else 12.dp, vertical = if (desktop) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AgentIcons.Sparkle,
            null,
            Modifier.size(if (desktop) 11.dp else 14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(if (desktop) 7.dp else 9.dp))
        Text(
            text,
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = if (desktop) 11.5.sp else 13.sp),
        )
        Icon(
            AgentIcons.ChevronRight,
            null,
            Modifier.size(if (desktop) 11.dp else 14.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun MobileMessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) state.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        modifier.fillMaxWidth(),
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            when (message.author) {
                MessageAuthor.USER -> MobileUserMessage(message)
                MessageAuthor.ENGINE -> MobileEngineMessage(message)
            }
        }
    }
}

@Composable
private fun MobileUserMessage(message: ChatMessage) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(Radius.lg, Radius.lg, Radius.xs, Radius.lg),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                message.text,
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            )
        }
        MonoText(
            formatClock(message.timestamp),
            Modifier.padding(top = 4.dp, end = 4.dp),
            size = 10.sp,
        )
    }
}

@Composable
private fun MobileEngineMessage(message: ChatMessage) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        AvatarTile(AgentIcons.Bolt, size = 28.dp, shape = RoundedCornerShape(Radius.sm))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Router",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (message.status != null) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip(message.status, tone = Tone.MUTED, mono = true)
                }
                Spacer(Modifier.weight(1f))
                MonoText(formatClock(message.timestamp), size = 10.sp)
            }
            Text(
                message.text,
                Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            )
            if (message.detail != null) {
                Spacer(Modifier.height(8.dp))
                PayloadToggle(expanded, message.detail.lines().size) { expanded = !expanded }
                Collapsible(expanded) {
                    Box(Modifier.padding(top = 6.dp)) { CodeSurface(message.detail) }
                }
            }
        }
    }
}

@Composable
private fun DesktopMessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) state.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        modifier.fillMaxWidth(),
        state = state,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            Box(Modifier.fillMaxWidth().widthIn(max = metrics().contentMax)) {
                when (message.author) {
                    MessageAuthor.USER -> DesktopUserMessage(message)
                    MessageAuthor.ENGINE -> DesktopEngineMessage(message)
                }
            }
        }
    }
}

@Composable
private fun DesktopUserMessage(message: ChatMessage) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("You")
            Spacer(Modifier.weight(1f))
            MonoText(formatClock(message.timestamp), size = 9.5.sp)
        }
        Spacer(Modifier.height(4.dp))
        val shape = RoundedCornerShape(Radius.xs)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape)
                .padding(horizontal = 11.dp, vertical = 8.dp),
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            )
        }
    }
}

@Composable
private fun DesktopEngineMessage(message: ChatMessage) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AgentIcons.Bolt, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                "Router",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
            if (message.status != null) {
                Spacer(Modifier.width(6.dp))
                MetaChip(message.status, tone = Tone.MUTED, mono = true)
            }
            Spacer(Modifier.weight(1f))
            MonoText(formatClock(message.timestamp), size = 9.5.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            message.text,
            Modifier.padding(start = 18.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
        )
        if (message.detail != null) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(start = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                PayloadToggle(expanded, message.detail.lines().size) { expanded = !expanded }
            }
            Collapsible(expanded) {
                Box(Modifier.padding(start = 18.dp, top = 6.dp)) { CodeSurface(message.detail) }
            }
        }
    }
}

@Composable
private fun PayloadToggle(expanded: Boolean, lineCount: Int, onClick: () -> Unit) {
    val desktop = isDesktop()
    Row(
        Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (expanded) AgentIcons.ChevronUp else AgentIcons.ChevronDown,
            null,
            Modifier.size(if (desktop) 11.dp else 13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        MonoText(
            if (expanded) "hide payload" else "payload · ${lineCount}L",
            size = if (desktop) 10.sp else 11.sp,
        )
    }
}

@Composable
private fun MobileComposer(
    input: String,
    busy: Boolean,
    enabled: Boolean,
    prefs: EffectPrefs,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.xl)
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .effectSurface(prefs, container)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ComposerField(
            input = input,
            onInput = onInput,
            onSend = onSend,
            placeholder = "Send a task to the Router",
            modifier = Modifier.weight(1f),
        )
        SendButton(
            busy = busy,
            enabled = enabled && input.isNotBlank(),
            onClick = onSend,
            size = 40.dp,
            modifier = Modifier.padding(bottom = 4.dp, end = 2.dp),
        )
    }
}

@Composable
private fun DesktopComposer(
    input: String,
    busy: Boolean,
    enabled: Boolean,
    prefs: EffectPrefs,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.md)
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .effectSurface(prefs, container)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f), shape)
                .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            GhostIconButton(AgentIcons.Plus, onClick = { }, enabled = false, size = 24.dp, iconSize = 14.dp)
            ComposerField(
                input = input,
                onInput = onInput,
                onSend = onSend,
                placeholder = "Message the Router, or describe a task",
                modifier = Modifier.weight(1f),
            )
            if (busy) {
                BusyIndicator(Modifier.padding(end = 6.dp, bottom = 6.dp))
            }
            SendButton(
                busy = busy,
                enabled = enabled && input.isNotBlank(),
                onClick = onSend,
                size = 26.dp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoText("enter to send · shift+enter for a new line", size = 9.5.sp)
        }
    }
}

@Composable
private fun ComposerField(
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val desktop = isDesktop()
    TextField(
        value = input,
        onValueChange = onInput,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (desktop && event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                onSend()
                true
            } else {
                false
            }
        },
        maxLines = if (desktop) 8 else 6,
        placeholder = {
            Text(
                placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = if (desktop) 12.5.sp else 15.sp),
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = if (desktop) 12.5.sp else 15.sp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SendButton(
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        when {
            busy -> MaterialTheme.colorScheme.tertiaryContainer
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        tween(MotionTokens.ShellColorMillis),
        label = "sendContainer",
    )
    val content = when {
        busy -> MaterialTheme.colorScheme.onTertiaryContainer
        enabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = modifier.size(size).clickable(enabled = enabled || busy, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (busy) AgentIcons.Stop else AgentIcons.ArrowUp,
                null,
                Modifier.size(size * 0.46f),
            )
        }
    }
}

@Composable
fun FlowDetailBlock(detail: String, modifier: Modifier = Modifier) {
    CodeSurface(detail, modifier)
}

@Composable
fun TimestampLabel(millis: Long, modifier: Modifier = Modifier) {
    MonoText(formatRelative(millis, nowMillis()), modifier, size = 10.sp)
}

@Composable
fun BodyMuted(text: String, modifier: Modifier = Modifier) {
    CaptionText(text, modifier, maxLines = 4)
}

@Composable
fun LabelAccent(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        maxLines = 1,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun DividerSoft(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
}
