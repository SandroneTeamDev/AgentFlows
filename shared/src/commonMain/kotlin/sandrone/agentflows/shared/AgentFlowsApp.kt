package sandrone.agentflows.shared

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sandrone.agentflows.shared.ui.AgentIcons
import sandrone.agentflows.shared.ui.AgentTab
import sandrone.agentflows.shared.ui.ChatMessage
import sandrone.agentflows.shared.ui.ChatPanel
import sandrone.agentflows.shared.ui.FlowsPanel
import sandrone.agentflows.shared.ui.MessageAuthor
import sandrone.agentflows.shared.ui.Pill
import sandrone.agentflows.shared.ui.SettingsPanel
import sandrone.agentflows.shared.ui.StatusDot
import sandrone.agentflows.shared.ui.UsagePanel
import sandrone.agentflows.shared.ui.formatDuration

@Composable
fun AgentFlowsApp(client: EngineClient = remember { createEngineClient() }) {
    val store = remember { createEffectPrefsStore() }
    var prefs by remember { mutableStateOf(store.load()) }
    var snapshot by remember { mutableStateOf(WorkspaceSnapshot()) }
    var tab by remember { mutableStateOf(AgentTab.Chat) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var watchdogAnswer by remember { mutableStateOf<String?>(null) }
    var watchdogBusy by remember { mutableStateOf(false) }
    var probeResult by remember { mutableStateOf<String?>(null) }
    var providerBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(client) {
        onDispose { client.close() }
    }

    LaunchedEffect(client) {
        while (true) {
            snapshot = try {
                client.snapshot()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                snapshot.copy(connected = false, message = "Local engine unavailable")
            }
            delay(2000)
        }
    }

    fun updatePrefs(next: EffectPrefs) {
        prefs = next
        store.save(next)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
        messages = messages + ChatMessage(
            id = randomToken(),
            author = MessageAuthor.USER,
            text = text,
            timestamp = nowMillis(),
        )
        busy = true
        scope.launch {
            try {
                val result = client.submit(text)
                val triggerId = result.substringBefore('|').ifBlank { "unavailable" }
                val state = result.substringAfter('|', "").ifBlank { "accepted" }
                val body = if (state == "pending_configuration") {
                    "Task recorded as a Trigger. The Router is waiting for a model before it dispatches a worker."
                } else {
                    "Trigger queued for the Router."
                }
                messages = messages + ChatMessage(
                    id = randomToken(),
                    author = MessageAuthor.ENGINE,
                    text = body,
                    status = state,
                    detail = "{\n  \"triggerId\": \"$triggerId\",\n  \"state\": \"$state\"\n}",
                    timestamp = nowMillis(),
                )
                snapshot = client.snapshot()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                messages = messages + ChatMessage(
                    id = randomToken(),
                    author = MessageAuthor.ENGINE,
                    text = "Submission failed: ${error.message ?: "unknown error"}",
                    status = "error",
                    timestamp = nowMillis(),
                )
            } finally {
                busy = false
            }
        }
    }

    EffectRoot {
        AgentFlowsTheme {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 840.dp
                Column(Modifier.fillMaxSize()) {
                    TopCapsule(snapshot, prefs, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    Row(Modifier.weight(1f)) {
                        if (wide) {
                            NavigationRail(containerColor = Color.Transparent) {
                                Spacer(Modifier.weight(1f))
                                AgentTab.entries.forEach { entry ->
                                    NavigationRailItem(
                                        selected = tab == entry,
                                        onClick = { tab = entry },
                                        icon = { Icon(iconFor(entry), null) },
                                        label = { Text(entry.label) },
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Crossfade(targetState = tab, animationSpec = tween(MotionTokens.ContentMillis), label = "panel") { active ->
                            Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp)) {
                                when (active) {
                                    AgentTab.Chat -> ChatPanel(
                                        messages = messages,
                                        snapshot = snapshot,
                                        prefs = prefs,
                                        input = input,
                                        busy = busy,
                                        onInput = { input = it },
                                        onSend = { send() },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    AgentTab.Flows -> FlowsPanel(snapshot.flows, Modifier.fillMaxSize())
                                    AgentTab.Usage -> UsagePanel(snapshot.usage, Modifier.fillMaxSize())
                                    AgentTab.Settings -> SettingsPanel(
                                        snapshot = snapshot,
                                        prefs = prefs,
                                        onPrefs = { updatePrefs(it) },
                                        watchdogAnswer = watchdogAnswer,
                                        watchdogBusy = watchdogBusy,
                                        onAskWatchdog = { question ->
                                            watchdogBusy = true
                                            scope.launch {
                                                watchdogAnswer = try {
                                                    client.askWatchdog(question)
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (error: Exception) {
                                                    "Watchdog is unreachable: ${error.message ?: "unknown error"}"
                                                }
                                                watchdogBusy = false
                                            }
                                        },
                                        onRegisterProvider = { id, baseUrl, models ->
                                            providerBusy = true
                                            scope.launch {
                                                try {
                                                    client.registerProvider(id, baseUrl, models.split(',').map { it.trim() }.filter { it.isNotEmpty() })
                                                    probeResult = "Provider $id registered."
                                                    snapshot = client.snapshot()
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (error: Exception) {
                                                    probeResult = "Registration failed: ${error.message ?: "unknown error"}"
                                                }
                                                providerBusy = false
                                            }
                                        },
                                        onProbe = {
                                            providerBusy = true
                                            scope.launch {
                                                probeResult = try {
                                                    val healthy = client.probeModels()
                                                    if (healthy) "Provider responded." else "Provider did not respond."
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (error: Exception) {
                                                    "Probe failed: ${error.message ?: "unknown error"}"
                                                }
                                                providerBusy = false
                                            }
                                        },
                                        probeResult = probeResult,
                                        providerBusy = providerBusy,
                                        onCancelTask = { workerId ->
                                            scope.launch {
                                                try {
                                                    client.cancelTask(workerId)
                                                    snapshot = client.snapshot()
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (_: Exception) {
                                                    snapshot = snapshot.copy(connected = false, message = "Cancel request failed")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                    if (!wide) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            AgentTab.entries.forEach { entry ->
                                NavigationBarItem(
                                    selected = tab == entry,
                                    onClick = { tab = entry },
                                    icon = { Icon(iconFor(entry), null) },
                                    label = { Text(entry.label) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCapsule(snapshot: WorkspaceSnapshot, prefs: EffectPrefs, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .effectSurface(prefs, MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AgentIcons.Wave, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text("AgentFlows", style = MaterialTheme.typography.titleMedium)
            Text(
                snapshot.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (snapshot.modelConfigured) Pill("model ready") else Pill("model off")
        Pill(formatDuration(snapshot.uptimeMs))
        StatusDot(snapshot.connected)
    }
}

private fun iconFor(tab: AgentTab) = when (tab) {
    AgentTab.Chat -> sandrone.agentflows.shared.ui.AgentIcons.Chat
    AgentTab.Flows -> sandrone.agentflows.shared.ui.AgentIcons.Bolt
    AgentTab.Usage -> sandrone.agentflows.shared.ui.AgentIcons.Chart
    AgentTab.Settings -> sandrone.agentflows.shared.ui.AgentIcons.Settings
}
