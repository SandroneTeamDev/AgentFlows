package sandrone.agentflows.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sandrone.agentflows.shared.EffectMode
import sandrone.agentflows.shared.EffectPrefs
import sandrone.agentflows.shared.WorkspaceSnapshot
import sandrone.agentflows.shared.formatDuration

@Composable
fun SettingsPanel(
    snapshot: WorkspaceSnapshot,
    prefs: EffectPrefs,
    onPrefs: (EffectPrefs) -> Unit,
    watchdogAnswer: String?,
    watchdogBusy: Boolean,
    onAskWatchdog: (String) -> Unit,
    onRegisterProvider: (String, String, String) -> Unit,
    onProbe: () -> Unit,
    probeResult: String?,
    providerBusy: Boolean,
    onCancelTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RuntimeCard(snapshot)
        AppearanceCard(prefs, onPrefs)
        ModelCard(snapshot, onRegisterProvider, onProbe, probeResult, providerBusy)
        WatchdogCard(snapshot, watchdogAnswer, watchdogBusy, onAskWatchdog)
        WorkersCard(snapshot, onCancelTask)
    }
}

@Composable
private fun RuntimeCard(snapshot: WorkspaceSnapshot) {
    SectionCard(
        title = "Local runtime",
        subtitle = snapshot.message,
        icon = AgentIcons.Wave,
        trailing = { StatusDot(snapshot.connected) },
    ) {
        KeyValueRow("Engine", if (snapshot.connected) "online" else "offline")
        KeyValueRow("Uptime", formatDuration(snapshot.uptimeMs))
        KeyValueRow("Model routing", if (snapshot.modelConfigured) "configured" else "not configured")
        KeyValueRow("Active workers", snapshot.workers.size.toString())
        KeyValueRow("Recorded events", snapshot.flows.size.toString())
        KeyValueRow("Watchdog", if (snapshot.watchdog.healthy) "healthy" else "attention")
    }
}

@Composable
private fun AppearanceCard(prefs: EffectPrefs, onPrefs: (EffectPrefs) -> Unit) {
    SectionCard(
        title = "Appearance",
        subtitle = "backdrop effect for the capsule surfaces",
        icon = AgentIcons.Settings,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectablePill("Off", prefs.mode == EffectMode.DISABLED) { onPrefs(prefs.copy(mode = EffectMode.DISABLED)) }
            SelectablePill("Blur", prefs.mode == EffectMode.BLUR) { onPrefs(prefs.copy(mode = EffectMode.BLUR)) }
            SelectablePill("Glass", prefs.mode == EffectMode.GLASS) { onPrefs(prefs.copy(mode = EffectMode.GLASS)) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blur radius", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                "${prefs.blurRadiusDp.toInt()} dp",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
        Slider(
            value = prefs.blurRadiusDp,
            onValueChange = { onPrefs(prefs.copy(blurRadiusDp = it)) },
            valueRange = 4f..48f,
            steps = 10,
            enabled = prefs.mode == EffectMode.BLUR,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when (prefs.mode) {
                EffectMode.DISABLED -> "Effects are off. Surfaces use a flat container colour."
                EffectMode.BLUR -> "Blur follows the radius above. It works on Android 10 and later through a capture-backed renderer."
                EffectMode.GLASS -> "Glass uses a fixed optical preset and does not expose a radius control."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelCard(
    snapshot: WorkspaceSnapshot,
    onRegisterProvider: (String, String, String) -> Unit,
    onProbe: () -> Unit,
    probeResult: String?,
    providerBusy: Boolean,
) {
    var id by remember { mutableStateOf("local") }
    var baseUrl by remember { mutableStateOf("http://127.0.0.1:11434/v1") }
    var models by remember { mutableStateOf("") }

    SectionCard(
        title = "Models",
        subtitle = "registered providers stay on this device",
        icon = AgentIcons.Bolt,
    ) {
        if (snapshot.providers.isEmpty()) {
            Text("No provider registered yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            snapshot.providers.forEach { provider ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(provider.id, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            provider.baseUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Pill(if (provider.models.isEmpty()) "no models" else "${provider.models.size} models")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("Provider id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = models,
            onValueChange = { models = it },
            label = { Text("Models, comma separated") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val parsed = models.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    onRegisterProvider(id.trim(), baseUrl.trim(), parsed.joinToString(","))
                },
                enabled = !providerBusy && id.isNotBlank() && baseUrl.isNotBlank(),
            ) {
                Icon(AgentIcons.Plus, null, Modifier.width(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Register")
            }
            TextButton(onClick = onProbe, enabled = !providerBusy) { Text("Probe") }
        }
        if (probeResult != null) {
            Text(
                probeResult,
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WatchdogCard(
    snapshot: WorkspaceSnapshot,
    answer: String?,
    busy: Boolean,
    onAsk: (String) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    SectionCard(
        title = "Watchdog",
        subtitle = "independent control loop",
        icon = AgentIcons.Wave,
        trailing = { StatusDot(snapshot.watchdog.healthy) },
    ) {
        if (snapshot.watchdog.alerts.isEmpty()) {
            Text("No alerts recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            snapshot.watchdog.alerts.takeLast(4).forEach { alert ->
                Text("• $alert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Ask the watchdog") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onAsk(question.trim()); question = "" },
            enabled = !busy && question.isNotBlank(),
        ) { Text(if (busy) "Asking…" else "Ask") }
        if (answer != null) {
            Column(Modifier.padding(top = 12.dp)) {
                LabelAccent("Answer")
                Spacer(Modifier.height(6.dp))
                MonospaceBlock(answer)
            }
        }
    }
}

@Composable
private fun WorkersCard(snapshot: WorkspaceSnapshot, onCancelTask: (String) -> Unit) {
    SectionCard(
        title = "Workers and tasks",
        subtitle = "cancel stops the worker and marks the task failed",
        icon = AgentIcons.Chart,
    ) {
        if (snapshot.tasks.isEmpty() && snapshot.workers.isEmpty()) {
            Text(
                "No workers have been dispatched. Configure a model, then send a task.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        snapshot.tasks.forEach { task ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(task.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onCancelTask(task.id) }) { Text("Cancel") }
            }
        }
    }
}
