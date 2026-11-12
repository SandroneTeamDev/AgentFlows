package sandrone.agentflows.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Panels = listOf("Flows", "Chat", "Tasks", "Files", "Terminal", "Git", "MCP", "Plugins", "Usage", "Settings")

@Composable
fun AgentFlowsApp(client: EngineClient = remember { createEngineClient() }) {
    var snapshot by remember { mutableStateOf(WorkspaceSnapshot()) }
    var selected by remember { mutableStateOf("Flows") }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
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
                snapshot.copy(connected = false, message = "Local engine unavailable · no remote fallback")
            }
            delay(2000)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 840.dp
        val showTaskPanel = maxWidth >= 1180.dp
        Column(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("AgentFlows", style = MaterialTheme.typography.headlineSmall)
                        Text("Your local event-flow workspace", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        if (snapshot.connected) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (snapshot.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Row(Modifier.weight(1f)) {
                if (wide) {
                    Column(
                        Modifier.width(192.dp).fillMaxHeight().padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Panels.forEach { name ->
                            Surface(
                                color = if (name == selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth().clickable { selected = name },
                            ) {
                                Text(name, Modifier.padding(horizontal = 20.dp, vertical = 14.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().padding(if (wide) 24.dp else 16.dp)) {
                    Text(selected, style = MaterialTheme.typography.headlineMedium)
                    Text(snapshot.message, Modifier.padding(top = 6.dp, bottom = 20.dp), style = MaterialTheme.typography.bodySmall)
                    when (selected) {
                        "Flows", "Chat" -> Timeline(snapshot.flows, Modifier.weight(1f))
                        "Tasks" -> TaskPanel(snapshot.tasks, Modifier.weight(1f))
                        "Settings" -> SettingsPanel(snapshot, Modifier.weight(1f))
                        else -> PreviewPanel(selected, Modifier.weight(1f))
                    }
                    if (selected == "Flows" || selected == "Chat") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text("Send a task to the Router") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                shape = MaterialTheme.shapes.large,
                                enabled = snapshot.connected && !sending,
                            )
                            Button(
                                enabled = snapshot.connected && !sending && input.isNotBlank(),
                                onClick = {
                                    val text = input.trim()
                                    sending = true
                                    scope.launch {
                                        try {
                                            client.submit(text)
                                            input = ""
                                            snapshot = client.snapshot()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            snapshot = snapshot.copy(message = "Task submission failed · input preserved")
                                        } finally {
                                            sending = false
                                        }
                                    }
                                },
                            ) { Text(if (sending) "Sending" else "Send") }
                        }
                    }
                }
                if (wide && showTaskPanel && selected != "Tasks") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Worker tasks", style = MaterialTheme.typography.titleLarge)
                            TaskPanel(snapshot.tasks, Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
            if (!wide) {
                NavigationBar {
                    listOf("Flows", "Chat", "Tasks", "Settings").forEach { name ->
                        NavigationBarItem(
                            selected = selected == name,
                            onClick = { selected = name },
                            icon = { Text(name.take(1), style = MaterialTheme.typography.titleMedium) },
                            label = { Text(name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Timeline(flows: List<FlowEntry>, modifier: Modifier = Modifier) {
    if (flows.isEmpty()) {
        EmptyPanel("No events yet", "Triggers, Router decisions, and Worker results will appear here. No simulated tasks are shown.", modifier)
        return
    }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(flows.asReversed(), key = { it.id }) { entry -> FlowCard(entry) }
    }
}

@Composable
private fun FlowCard(entry: FlowEntry) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.type.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(if (expanded) "Hide" else "Inspect", style = MaterialTheme.typography.labelSmall)
            }
            Text(entry.label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
            ) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text(entry.details, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun TaskPanel(tasks: List<TaskEntry>, modifier: Modifier = Modifier) {
    if (tasks.isEmpty()) {
        EmptyPanel("No active Workers", "Tasks are created by the Router after a model has been configured.", modifier)
        return
    }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tasks, key = { it.id }) { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    Text(task.status.uppercase(), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(snapshot: WorkspaceSnapshot, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local runtime", style = MaterialTheme.typography.titleMedium)
                Text(snapshot.message)
                Text("State stays on this device. An offline engine is never silently replaced by a hosted service.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alpha preview", style = MaterialTheme.typography.titleMedium)
                Text("Provider settings, secure key storage, and device-runtime installation are still being connected.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PreviewPanel(name: String, modifier: Modifier) {
    EmptyPanel(name, "This panel is not connected in the current preview. Its presence does not grant tool permissions or imply implementation is complete.", modifier)
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
