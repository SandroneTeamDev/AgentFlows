package sandrone.agentflows.shared.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sandrone.agentflows.shared.EffectPrefs
import sandrone.agentflows.shared.MotionTokens
import sandrone.agentflows.shared.WorkspaceSnapshot
import sandrone.agentflows.shared.effectSurface
import sandrone.agentflows.shared.nowMillis

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
    Column(modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ConversationIntro(snapshot)
            }
        } else {
            MessageList(messages, Modifier.weight(1f))
        }
        Composer(
            input = input,
            busy = busy,
            enabled = snapshot.connected && input.isNotBlank(),
            prefs = prefs,
            onInput = onInput,
            onSend = onSend,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ConversationIntro(snapshot: WorkspaceSnapshot) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AgentIcons.Wave, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("AgentFlows", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (snapshot.connected) {
                "Your local event-flow workspace is online"
            } else {
                "The local engine is not reachable on this device"
            },
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (snapshot.modelConfigured) "Model ready" else "Model not set")
            Pill("${snapshot.workers.size} workers")
            Pill("v0.0.1-rc1-alpha2")
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Send a task and the Router records it as a Trigger. Workers appear in the Flows timeline with their tool arguments and results.",
            Modifier.widthIn(max = 460.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) state.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        modifier.fillMaxWidth(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            when (message.author) {
                MessageAuthor.USER -> UserBubble(message)
                MessageAuthor.ENGINE -> EngineCard(message)
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatClock(message.timestamp),
                    Modifier.padding(top = 6.dp).align(Alignment.End),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun EngineCard(message: ChatMessage) {
    var expanded by remember(message.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp),
            modifier = Modifier.widthIn(max = 600.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AgentIcons.Bolt, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Router", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (message.status != null) {
                        Spacer(Modifier.width(8.dp))
                        Pill(message.status)
                    }
                }
                Text(message.text, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium)
                if (message.detail != null) {
                    Text(
                        if (expanded) "Hide payload" else "Show payload",
                        Modifier.padding(top = 10.dp).clickable { expanded = !expanded },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExpandableArea(expanded) {
                        Box(Modifier.padding(top = 8.dp)) { MonospaceBlock(message.detail) }
                    }
                }
                Text(
                    formatClock(message.timestamp),
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Composer(
    input: String,
    busy: Boolean,
    enabled: Boolean,
    prefs: EffectPrefs,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .effectSurface(prefs, container)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.weight(1f),
            maxLines = 5,
            placeholder = { Text("Send a task to the Router") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        SendButton(busy = busy, enabled = enabled, onClick = onSend, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun SendButton(busy: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        when {
            busy -> MaterialTheme.colorScheme.tertiaryContainer
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        tween(MotionTokens.ShellColorMillis),
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
        modifier = modifier.size(44.dp).clickable(enabled || busy, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(if (busy) AgentIcons.Stop else AgentIcons.Send, null, Modifier.size(20.dp))
        }
    }
}

@Composable
fun FlowDetailBlock(detail: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            detail,
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
fun TimestampLabel(millis: Long, modifier: Modifier = Modifier) {
    Text(
        formatRelative(millis, nowMillis()),
        modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ToneSurface(tone: FlowTone, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val color = when (tone) {
        FlowTone.ALERT -> MaterialTheme.colorScheme.errorContainer
        FlowTone.ROUTER -> MaterialTheme.colorScheme.primaryContainer
        FlowTone.WORKER -> MaterialTheme.colorScheme.secondaryContainer
        FlowTone.TRIGGER -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(color = color, shape = MaterialTheme.shapes.medium, modifier = modifier, content = content)
}

@Composable
fun BodyMuted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun LabelAccent(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun DividerSoft(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
}
