package sandrone.agentflows.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface ChatItem {
    val id: String
}

private data class UserMessage(override val id: String, val text: String) : ChatItem
private data class EngineReceipt(override val id: String, val text: String, val triggerId: String) : ChatItem

private val CapsuleShape = RoundedCornerShape(28.dp)

@Composable
fun AgentFlowsApp(client: EngineClient = remember { createEngineClient() }) {
    var snapshot by remember { mutableStateOf(WorkspaceSnapshot()) }
    var selected by rememberSaveable { mutableStateOf("Chat") }
    var items by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var lastReceipt by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val effect = rememberEffectSettings()
    val hazeState = rememberHazeState()

    DisposableEffect(client) { onDispose { client.close() } }
    LaunchedEffect(client) {
        while (true) {
            snapshot = try {
                client.snapshot()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                snapshot.copy(connected = false, message = "Local engine unavailable · no remote fallback")
            }
            delay(2000)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hazeSource(hazeState),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            Column(Modifier.fillMaxSize()) {
                TopCapsule(snapshot, hazeState, effect.state.value, Modifier.fillMaxWidth())
                when (selected) {
                    "Chat" -> ChatFeed(items, snapshot, Modifier.weight(1f), lastReceipt)
                    "Flows" -> FlowTimeline(snapshot, Modifier.weight(1f))
                    "Usage" -> UsagePanel(snapshot, Modifier.weight(1f))
                    "Settings" -> SettingsPanel(snapshot, effect, Modifier.weight(1f))
                }
                if (selected == "Chat") {
                    ChatInput(
                        hazeState = hazeState,
                        effect = effect,
                        snapshot = snapshot,
                        input = input,
                        onInput = { input = it },
                        sending = sending,
                        onSend = {
                            val text = input.trim()
                            if (text.isEmpty() || sending) return@ChatInput
                            sending = true
                            scope.launch {
                                try {
                                    val triggerId = client.submit(text)
                                    items = items + UserMessage(triggerId, text) +
                                        EngineReceipt("r-$triggerId", text, triggerId)
                                    input = ""
                                    lastReceipt = triggerId
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    lastReceipt = "failed"
                                } finally {
                                    sending = false
                                }
                            }
                        },
                    )
                }
                if (!wide) {
                    BottomNav(selected, { selected = it })
                }
            }
        }
    }
}

@Composable
private fun TopCapsule(
    snapshot: WorkspaceSnapshot,
    hazeState: HazeState,
    effect: EffectSettings,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(12.dp),
        shape = CapsuleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier
                .effectSurface(hazeState, effect, CapsuleShape, MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AgentFlows", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(
                "Local event-flow workspace",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (snapshot.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    !snapshot.connected -> "OFFLINE"
                    snapshot.modelConfigured -> "ONLINE"
                    else -> "ONLINE · NO MODEL"
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ChatFeed(items: List<ChatItem>, snapshot: WorkspaceSnapshot, modifier: Modifier, lastReceipt: String) {
    val listState = rememberLazyListState()
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
    }
    LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), state = listState) {
        item(key = "header") {
            Text(
                snapshot.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyPanel(
                    "No conversations yet",
                    "Tasks you send are routed to the Router and answered here with their trigger receipt. " +
                        "No replies are invented before a model is configured.",
                    Modifier.padding(vertical = 32.dp),
                )
            }
        }
        items(items, key = { it.id }) { item ->
            when (item) {
                is UserMessage -> UserBubble(item.text)
                is EngineReceipt -> ReceiptCard(item, snapshot, lastReceipt)
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ReceiptCard(receipt: EngineReceipt, snapshot: WorkspaceSnapshot, lastReceipt: String) {
    var expanded by remember(receipt.triggerId) { mutableStateOf(false) }
    val flow = snapshot.flows.find { it.id == receipt.triggerId || (it.details.contains(receipt.triggerId)) }
    val status = when {
        lastReceipt == "failed" -> "submission failed"
        snapshot.modelConfigured -> "queued to Router"
        else -> "held · model not configured"
    }
    Row(Modifier.fillMaxWidth()) {
        Card(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Router", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(receipt.text.take(40), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessLow)),
                    exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                ) {
                    Column(Modifier.padding(top = 10.dp)) {
                        Text("Trigger ${receipt.triggerId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        (flow?.details ?: "{}").let { details ->
                            Text(details, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 8)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    hazeState: HazeState,
    effect: EffectSettingsController,
    snapshot: WorkspaceSnapshot,
    input: String,
    onInput: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = CapsuleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier
                    .effectSurface(hazeState, effect.state.value, CapsuleShape, MaterialTheme.colorScheme.surfaceContainer)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    placeholder = { Text("Send a task to the Router", style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = snapshot.connected && !sending,
                    colors = TextFieldColors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                )
                IconButton(onClick = onSend, enabled = snapshot.connected && !sending && input.isNotBlank()) {
                    Text(
                        if (sending) "…" else "→",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowTimeline(snapshot: WorkspaceSnapshot, modifier: Modifier) {
    if (snapshot.flows.isEmpty()) {
        EmptyPanel(
            "No events yet",
            "Triggers, Router decisions, and Worker results will appear here in timeline order.",
            modifier,
        )
        return
    }
    LazyColumn(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.flows.asReversed(), key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(entry.type.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(entry.label, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall)
                    Text(entry.details, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 6, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UsagePanel(snapshot: WorkspaceSnapshot, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (snapshot.usage.isEmpty()) {
            EmptyPanel(
                "No usage recorded",
                "Token ledgers and cost estimates are recorded per model call and shown here across sessions.",
                Modifier,
            )
        } else {
            val totalIn = snapshot.usage.sumOf { it.tokensIn }
            val totalOut = snapshot.usage.sumOf { it.tokensOut }
            val totalCost = snapshot.usage.sumOf { it.cost }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("This device", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$totalIn in / $totalOut out", style = MaterialTheme.typography.labelMedium)
                        Text("%.2f est. cost".format(totalCost), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            snapshot.usage.asReversed().forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.model, style = MaterialTheme.typography.titleSmall)
                            Text("${entry.provider} · ${entry.tokensIn}/${entry.tokensOut} tokens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("%.2f".format(entry.cost), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    snapshot: WorkspaceSnapshot,
    effect: EffectSettingsController,
    modifier: Modifier,
) {
    LazyColumn(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Local runtime", style = MaterialTheme.typography.titleMedium)
                    Text(snapshot.message, style = MaterialTheme.typography.bodySmall)
                    Text("Uptime ${(snapshot.uptimeMs / 1000)}s · Watchdog ${if (snapshot.watchdog.healthy) "healthy" else snapshot.watchdog.alerts.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Background effect", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable blur / glass", style = MaterialTheme.typography.bodyMedium)
                            Text("Glass uses a fixed optical preset; blur size is adjustable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = effect.state.value.enabled, onCheckedChange = { effect.update { s -> s.copy(enabled = it) } })
                    }
                    if (effect.state.value.enabled) {
                        val options = listOf(EffectMode.Blur to "Blur", EffectMode.Glass to "Glass")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = effect.state.value.mode == mode,
                                    onClick = { effect.update { s -> s.copy(mode = mode) } },
                                    shape = SegmentedButtonDefaults.ItemShape(index, options.size),
                                ) { Text(label) }
                            }
                        }
                        if (effect.state.value.mode == EffectMode.Blur) {
                            Column {
                                Text("Blur size · ${effect.state.value.blurRadiusDp}dp", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = effect.state.value.blurRadiusDp.toFloat(),
                                    onValueChange = { effect.update { s -> s.copy(blurRadiusDp = it.toInt()) } },
                                    valueRange = 4f..40f,
                                    steps = 16,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Providers", style = MaterialTheme.typography.titleMedium)
                    if (snapshot.providers.isEmpty()) {
                        Text("No providers registered. Register an OpenAI-compatible base URL and model list to configure the Router and Workers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        snapshot.providers.forEach { provider ->
                            Text("${provider.id} (${provider.kind}) · ${provider.models.size} models", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, detail: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
