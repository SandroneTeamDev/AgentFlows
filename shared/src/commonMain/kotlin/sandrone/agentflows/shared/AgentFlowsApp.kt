package sandrone.agentflows.shared

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sandrone.agentflows.shared.design.AvatarTile
import sandrone.agentflows.shared.design.CaptionText
import sandrone.agentflows.shared.design.DesktopShell
import sandrone.agentflows.shared.design.GhostIconButton
import sandrone.agentflows.shared.design.KVRow
import sandrone.agentflows.shared.design.MetaChip
import sandrone.agentflows.shared.design.MobileShell
import sandrone.agentflows.shared.design.MonoText
import sandrone.agentflows.shared.design.ShellNavItem
import sandrone.agentflows.shared.design.SidebarPanel
import sandrone.agentflows.shared.design.StatusBarItem
import sandrone.agentflows.shared.design.StatusBarSeparator
import sandrone.agentflows.shared.design.StatusDot
import sandrone.agentflows.shared.design.ThinRule
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.ui.AgentIcons
import sandrone.agentflows.shared.ui.AgentTab
import sandrone.agentflows.shared.ui.ChatMessage
import sandrone.agentflows.shared.ui.ChatPanel
import sandrone.agentflows.shared.ui.FlowsPanel
import sandrone.agentflows.shared.ui.MessageAuthor
import sandrone.agentflows.shared.ui.SettingsPanel
import sandrone.agentflows.shared.ui.UsagePanel
import sandrone.agentflows.shared.ui.formatDuration

private val AppVersion = "v0.0.1-rc1-alpha3"

@Composable
fun AgentFlowsApp(
    client: EngineClient = remember { createEngineClient() },
    dynamicScheme: ColorScheme? = null,
) {
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

    fun refresh() {
        scope.launch {
            snapshot = try {
                client.snapshot()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                snapshot.copy(connected = false, message = "Local engine unavailable")
            }
        }
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
                    detail = "{\n  \"triggerId\": \"${triggerId}\",\n  \"state\": \"${state}\"\n}",
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

    val density = LocalDensity.current
    var availableWidth by remember { mutableStateOf(0.dp) }
    var availableHeight by remember { mutableStateOf(0.dp) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                availableWidth = with(density) { it.width.toDp() }
                availableHeight = with(density) { it.height.toDp() }
            },
    ) {
        val desktopShell = availableWidth >= 840.dp && availableHeight >= 560.dp
        AgentFlowsTheme(
            colorScheme = if (desktopShell) null else dynamicScheme,
            flavor = if (desktopShell) ThemeFlavor.GRAPHITE else ThemeFlavor.EXPRESSIVE,
        ) {
            EffectRoot {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val navItems = AgentTab.entries.map { ShellNavItem(it.label, iconFor(it)) }
                val content: @Composable () -> Unit = {
                    Crossfade(
                        targetState = tab,
                        animationSpec = tween(MotionTokens.ContentMillis),
                        label = "panel",
                    ) { active ->
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
                            AgentTab.Usage -> UsagePanel(client, Modifier.fillMaxSize())
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
                                            client.registerProvider(
                                                id,
                                                baseUrl,
                                                models.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                                            )
                                            probeResult = "Provider ${id} registered."
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

                if (desktopShell) {
                    DesktopShell(
                        navItems = navItems,
                        selectedNav = tab.ordinal,
                        onNavSelect = { tab = AgentTab.entries[it] },
                        headerTitle = desktopTitle(tab),
                        headerSubtitle = snapshot.message,
                        headerChips = {
                            MetaChip(
                                if (snapshot.modelConfigured) "model ready" else "model off",
                                tone = if (snapshot.modelConfigured) Tone.ACCENT else Tone.MUTED,
                            )
                            MetaChip("${snapshot.workers.size} workers", mono = true)
                            MetaChip("${snapshot.flows.size} events", mono = true)
                        },
                        headerActions = {
                            GhostIconButton(AgentIcons.Refresh, onClick = { refresh() })
                            GhostIconButton(AgentIcons.Settings, onClick = { tab = AgentTab.Settings })
                        },
                        sidebarTop = {
                            SidebarPanel {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusDot(snapshot.connected)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Local engine",
                                        Modifier.weight(1f),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    )
                                    MonoText(if (snapshot.connected) "online" else "offline", size = 10.sp)
                                }
                                CaptionText(snapshot.message, maxLines = 2, size = 10.5.sp)
                                ThinRule(alpha = 0.4f)
                                KVRow("model", if (snapshot.modelConfigured) "ready" else "not set")
                                KVRow("watchdog", if (snapshot.watchdog.healthy) "healthy" else "attention")
                            }
                        },
                        sidebarBottom = {
                            KVRow("uptime", formatDuration(snapshot.uptimeMs))
                            KVRow("workers", snapshot.workers.size.toString())
                            KVRow("tasks", snapshot.tasks.size.toString())
                            ThinRule(alpha = 0.4f)
                            MonoText(AppVersion, size = 9.5.sp)
                        },
                        statusBar = {
                            StatusDot(snapshot.connected)
                            StatusBarItem("engine", if (snapshot.connected) "online" else "offline")
                            StatusBarSeparator()
                            StatusBarItem("events", snapshot.flows.size.toString())
                            StatusBarSeparator()
                            StatusBarItem("workers", snapshot.workers.size.toString())
                            StatusBarSeparator()
                            StatusBarItem("uptime", formatDuration(snapshot.uptimeMs))
                            Spacer(Modifier.weight(1f))
                            MonoText(AppVersion, size = 9.5.sp)
                        },
                        content = { content() },
                    )
                } else {
                    MobileShell(
                        navItems = navItems,
                        selectedNav = tab.ordinal,
                        onNavSelect = { tab = AgentTab.entries[it] },
                        title = "AgentFlows",
                        subtitle = snapshot.message,
                        leading = { AvatarTile(AgentIcons.Wave, size = 40.dp) },
                        actions = {
                            GhostIconButton(AgentIcons.Refresh, onClick = { refresh() })
                            GhostIconButton(AgentIcons.Settings, onClick = { tab = AgentTab.Settings })
                        },
                        content = { content() },
                    )
                }
                }
            }
        }
    }
}

private fun desktopTitle(tab: AgentTab): String = when (tab) {
    AgentTab.Chat -> "Conversation"
    AgentTab.Flows -> "Event timeline"
    AgentTab.Usage -> "Usage ledger"
    AgentTab.Settings -> "Workspace settings"
}

private fun iconFor(tab: AgentTab): ImageVector = when (tab) {
    AgentTab.Chat -> AgentIcons.Chat
    AgentTab.Flows -> AgentIcons.Bolt
    AgentTab.Usage -> AgentIcons.Chart
    AgentTab.Settings -> AgentIcons.Settings
}
