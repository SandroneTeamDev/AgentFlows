package sandrone.agentflows.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sandrone.agentflows.shared.EffectMode
import sandrone.agentflows.shared.EffectPrefs
import sandrone.agentflows.shared.ProviderEntry
import sandrone.agentflows.shared.TaskEntry
import sandrone.agentflows.shared.WorkerEntry
import sandrone.agentflows.shared.WorkspaceSnapshot
import sandrone.agentflows.shared.design.AvatarTile
import sandrone.agentflows.shared.design.BodyText
import sandrone.agentflows.shared.design.BusyIndicator
import sandrone.agentflows.shared.design.CaptionText
import sandrone.agentflows.shared.design.ChipFilterRow
import sandrone.agentflows.shared.design.CodeSurface
import sandrone.agentflows.shared.design.EmptyHint
import sandrone.agentflows.shared.design.KVRow
import sandrone.agentflows.shared.design.MetaChip
import sandrone.agentflows.shared.design.MonoText
import sandrone.agentflows.shared.design.PanelCard
import sandrone.agentflows.shared.design.PulsingDot
import sandrone.agentflows.shared.design.Radius
import sandrone.agentflows.shared.design.SectionLabel
import sandrone.agentflows.shared.design.Space
import sandrone.agentflows.shared.design.StatBlock
import sandrone.agentflows.shared.design.StatusPill
import sandrone.agentflows.shared.design.ThinRule
import sandrone.agentflows.shared.design.Tone
import sandrone.agentflows.shared.design.container
import sandrone.agentflows.shared.design.content
import sandrone.agentflows.shared.design.isDesktop
import sandrone.agentflows.shared.design.metrics
import sandrone.agentflows.shared.i18n.AppLanguage
import sandrone.agentflows.shared.i18n.pick
import sandrone.agentflows.shared.i18n.statusLabel
import sandrone.agentflows.shared.effectSurface
import sandrone.agentflows.shared.nowMillis

private val LanguageOptions = AppLanguage.entries.map { language -> language.tag to language.nativeLabel }

private fun effectOptions(): List<Pair<String, String>> =
    EffectMode.entries.map { mode -> mode.name to effectLabel(mode) }

private const val ProviderIdHint = "local"
private const val ProviderUrlHint = "http://127.0.0.1:11434/v1"
private const val ProviderModelsHint = "llama3, qwen2.5"

private fun watchdogQuestionHint(): String = pick("为什么上一个执行体停止了？", "Why did the last worker stop?")

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
    if (isDesktop()) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Row(
                    Modifier.widthIn(max = metrics().contentMax).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics().cardGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(metrics().cardGap)) {
                        RuntimeSection(snapshot)
                        AppearanceSection(prefs, onPrefs)
                        WatchdogSection(snapshot, watchdogAnswer, watchdogBusy, onAskWatchdog)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(metrics().cardGap)) {
                        ModelsSection(snapshot, onRegisterProvider, onProbe, probeResult, providerBusy)
                        WorkersSection(snapshot, onCancelTask)
                    }
                }
            }
            Spacer(Modifier.height(Space.lg))
        }
    } else {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = metrics().gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(metrics().sectionGap),
        ) {
            RuntimeSection(snapshot)
            AppearanceSection(prefs, onPrefs)
            ModelsSection(snapshot, onRegisterProvider, onProbe, probeResult, providerBusy)
            WatchdogSection(snapshot, watchdogAnswer, watchdogBusy, onAskWatchdog)
            WorkersSection(snapshot, onCancelTask)
            Spacer(Modifier.height(Space.sm))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: Tone = Tone.NEUTRAL,
    status: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    PanelCard(modifier = modifier) {
        if (desktop) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(13.dp), tint = scheme.primary)
                Spacer(Modifier.width(7.dp))
                Text(
                    title,
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                )
                if (status != null) {
                    Spacer(Modifier.width(Space.sm))
                    status()
                }
            }
            Spacer(Modifier.height(7.dp))
            ThinRule(alpha = 0.6f)
            Spacer(Modifier.height(Space.sm))
            Column(Modifier.fillMaxWidth(), content = content)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(
                    icon = icon,
                    size = 40.dp,
                    shape = RoundedCornerShape(Radius.md),
                    container = tone.container(),
                    content = tone.content(),
                )
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = metrics().titleSize,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    if (subtitle != null) {
                        CaptionText(subtitle, Modifier.padding(top = 1.dp), maxLines = 2)
                    }
                }
                if (status != null) {
                    Spacer(Modifier.width(Space.sm))
                    status()
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun RuntimeSection(snapshot: WorkspaceSnapshot) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    SettingsSection(
        title = pick("本地运行时", "Local runtime"),
        icon = AgentIcons.Wave,
        subtitle = snapshot.message,
        tone = Tone.ACCENT,
        status = { StatusPill(snapshot.connected, statusLabel("online"), statusLabel("offline")) },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (desktop) Space.lg else Space.md),
        ) {
            StatBlock(pick("运行时长", "uptime"), formatDuration(snapshot.uptimeMs), Modifier.weight(1f))
            StatBlock(pick("执行体", "workers"), snapshot.workers.size.toString(), Modifier.weight(1f))
            StatBlock(pick("事件", "events"), snapshot.flows.size.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.sm))
        ThinRule(alpha = if (desktop) 0.3f else 0.35f)
        KVRow(
            pick("引擎", "Engine"),
            statusLabel(if (snapshot.connected) "online" else "offline"),
            valueColor = if (snapshot.connected) scheme.primary else scheme.error,
        )
        KVRow(
            pick("模型路由", "Model routing"),
            if (snapshot.modelConfigured) pick("已配置", "configured") else pick("未配置", "not configured"),
        )
        KVRow(
            pick("看门狗", "Watchdog"),
            statusLabel(if (snapshot.watchdog.healthy) "healthy" else "attention"),
            valueColor = if (snapshot.watchdog.healthy) null else scheme.error,
        )
        if (desktop) {
            Spacer(Modifier.height(Space.xs))
            CaptionText(snapshot.message, maxLines = 2, size = 10.5.sp)
        }
    }
}

@Composable
private fun AppearanceSection(prefs: EffectPrefs, onPrefs: (EffectPrefs) -> Unit) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    SettingsSection(
        title = pick("外观", "Appearance"),
        icon = AgentIcons.Settings,
        subtitle = pick("胶囊表面的背景效果", "backdrop effect for capsule surfaces"),
        tone = Tone.NEUTRAL,
    ) {
        if (desktop) {
            ChipFilterRow(
                options = effectOptions(),
                selected = prefs.mode.name,
                onSelect = { key ->
                    EffectMode.entries.firstOrNull { it.name == key }?.let { mode -> onPrefs(prefs.copy(mode = mode)) }
                },
            )
        } else {
            EffectSegments(prefs.mode) { mode -> onPrefs(prefs.copy(mode = mode)) }
        }
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        EffectPreview(prefs)
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (desktop) {
                SectionLabel(pick("模糊半径", "Blur radius"), Modifier.weight(1f))
            } else {
                BodyText(pick("模糊半径", "Blur radius"), Modifier.weight(1f), maxLines = 1)
            }
            MonoText(
                "${prefs.blurRadiusDp.toInt()} dp",
                size = if (desktop) 10.sp else 12.sp,
                color = scheme.onSurface,
            )
        }
        Slider(
            value = prefs.blurRadiusDp,
            onValueChange = { value -> onPrefs(prefs.copy(blurRadiusDp = value)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.mode == EffectMode.BLUR,
            valueRange = 4f..48f,
            steps = 10,
        )
        CaptionText(
            effectExplanation(prefs.mode),
            maxLines = 3,
            size = if (desktop) 11.sp else 13.sp,
        )
        Spacer(Modifier.height(Space.xs))
        MonoText(
            pick(
                "半径 ${prefs.blurRadiusDp.toInt()} dp · 色调 ${(prefs.glassTintAlpha * 100f).toInt()}%",
                "radius ${prefs.blurRadiusDp.toInt()} dp · tint ${(prefs.glassTintAlpha * 100f).toInt()}%",
            ),
            size = if (desktop) 9.5.sp else 11.sp,
        )
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        ThinRule(alpha = if (desktop) 0.3f else 0.35f)
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (desktop) {
                SectionLabel(pick("语言", "Language"), Modifier.weight(1f))
            } else {
                BodyText(pick("语言", "Language"), Modifier.weight(1f), maxLines = 1)
            }
        }
        CaptionText(
            pick("界面语言，默认中文", "Interface language, Chinese by default"),
            Modifier.padding(top = 2.dp),
            maxLines = 2,
            size = if (desktop) 11.sp else 13.sp,
        )
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        if (desktop) {
            ChipFilterRow(
                options = LanguageOptions,
                selected = prefs.language.tag,
                onSelect = { tag -> onPrefs(prefs.copy(language = AppLanguage.fromTag(tag))) },
            )
        } else {
            LanguageSegments(prefs.language) { language -> onPrefs(prefs.copy(language = language)) }
        }
    }
}

@Composable
private fun EffectSegments(selected: EffectMode, onSelect: (EffectMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(scheme.surfaceContainerHighest).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EffectMode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(if (active) scheme.primary else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    effectLabel(mode),
                    maxLines = 1,
                    color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LanguageSegments(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(scheme.surfaceContainerHighest).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLanguage.entries.forEach { language ->
            val active = language == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(if (active) scheme.primary else Color.Transparent)
                    .clickable { onSelect(language) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    language.nativeLabel,
                    maxLines = 1,
                    color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EffectPreview(prefs: EffectPrefs) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (desktop) Radius.sm else Radius.lg)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .effectSurface(prefs, scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant.copy(alpha = if (desktop) 0.7f else 0.4f), shape)
            .padding(horizontal = if (desktop) 10.dp else 14.dp, vertical = if (desktop) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulsingDot(size = if (desktop) 5.dp else 6.dp)
        Spacer(Modifier.width(if (desktop) 7.dp else 9.dp))
        MonoText(
            if (prefs.mode == EffectMode.DISABLED) pick("平面底色", "flat surface") else pick("实时背景", "live backdrop"),
            size = if (desktop) 10.sp else 11.sp,
        )
        Spacer(Modifier.weight(1f))
        MonoText(effectLabel(prefs.mode), size = if (desktop) 9.5.sp else 10.5.sp)
    }
}

@Composable
private fun ModelsSection(
    snapshot: WorkspaceSnapshot,
    onRegisterProvider: (String, String, String) -> Unit,
    onProbe: () -> Unit,
    probeResult: String?,
    providerBusy: Boolean,
) {
    val desktop = isDesktop()
    var providerId by remember { mutableStateOf("local") }
    var baseUrl by remember { mutableStateOf("http://127.0.0.1:11434/v1") }
    var models by remember { mutableStateOf("") }
    val canRegister = !providerBusy && providerId.isNotBlank() && baseUrl.isNotBlank()
    val register = {
        onRegisterProvider(providerId.trim(), baseUrl.trim(), parseModels(models))
    }
    SettingsSection(
        title = pick("模型", "Models"),
        icon = AgentIcons.Bolt,
        subtitle = pick("已注册的服务商仅保存在本机", "registered providers stay on this device"),
        tone = Tone.POSITIVE,
        status = {
            MetaChip(
                text = when (snapshot.providers.size) {
                    0 -> pick("无", "none")
                    1 -> pick("1 个服务商", "1 provider")
                    else -> pick("${snapshot.providers.size} 个服务商", "${snapshot.providers.size} providers")
                },
                tone = if (snapshot.providers.isEmpty()) Tone.MUTED else Tone.POSITIVE,
                mono = true,
            )
        },
    ) {
        if (snapshot.providers.isEmpty()) {
            EmptyHint(
                icon = AgentIcons.Database,
                title = pick("尚未注册服务商", "No providers registered"),
                detail = pick(
                    "注册一个本地端点，让路由在本机分派模型调用。",
                    "Register a local endpoint to let the Router dispatch model calls on this device.",
                ),
                dense = desktop,
            )
        } else {
            Column(Modifier.fillMaxWidth()) {
                snapshot.providers.forEachIndexed { index, provider ->
                    if (desktop && index > 0) ThinRule(alpha = 0.25f)
                    ProviderRow(provider)
                }
            }
        }
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        ThinRule(alpha = if (desktop) 0.3f else 0.35f)
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        if (desktop) {
            SectionLabel(pick("注册服务商", "Register provider"))
            Spacer(Modifier.height(Space.xs))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsField(
                    label = pick("服务商 ID", "Provider id"),
                    value = providerId,
                    onValueChange = { providerId = it },
                    modifier = Modifier.weight(0.8f),
                    placeholder = ProviderIdHint,
                    enabled = !providerBusy,
                )
                SettingsField(
                    label = pick("基础地址", "Base URL"),
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier.weight(1.4f),
                    placeholder = ProviderUrlHint,
                    enabled = !providerBusy,
                )
                SettingsField(
                    label = pick("模型", "Models"),
                    value = models,
                    onValueChange = { models = it },
                    modifier = Modifier.weight(1.2f),
                    placeholder = ProviderModelsHint,
                    enabled = !providerBusy,
                )
            }
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsAction(pick("注册", "Register"), AgentIcons.Plus, onClick = register, enabled = canRegister)
                SettingsAction(
                    pick("探测", "Probe"),
                    AgentIcons.Wave,
                    onClick = onProbe,
                    enabled = !providerBusy,
                    filled = false,
                )
                Spacer(Modifier.weight(1f))
                if (providerBusy) {
                    BusyIndicator()
                    Spacer(Modifier.width(6.dp))
                    MonoText(pick("正在连接服务商", "contacting provider"), size = 9.5.sp)
                } else {
                    MonoText(pick("逗号分隔的模型", "comma separated models"), size = 9.5.sp)
                }
            }
        } else {
            SettingsField(
                label = pick("服务商 ID", "Provider id"),
                value = providerId,
                onValueChange = { providerId = it },
                placeholder = ProviderIdHint,
                enabled = !providerBusy,
            )
            Spacer(Modifier.height(Space.md))
            SettingsField(
                label = pick("基础地址", "Base URL"),
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = ProviderUrlHint,
                enabled = !providerBusy,
            )
            Spacer(Modifier.height(Space.md))
            SettingsField(
                label = pick("模型，逗号分隔", "Models, comma separated"),
                value = models,
                onValueChange = { models = it },
                placeholder = ProviderModelsHint,
                enabled = !providerBusy,
            )
            Spacer(Modifier.height(Space.md))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsAction(
                    pick("注册", "Register"),
                    AgentIcons.Plus,
                    onClick = register,
                    modifier = Modifier.weight(1f),
                    enabled = canRegister,
                )
                SettingsAction(
                    pick("探测", "Probe"),
                    AgentIcons.Wave,
                    onClick = onProbe,
                    enabled = !providerBusy,
                    filled = false,
                )
            }
            if (providerBusy) {
                Spacer(Modifier.height(Space.sm))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BusyIndicator()
                    Spacer(Modifier.width(Space.sm))
                    MonoText(pick("正在连接服务商", "contacting provider"), size = 11.sp)
                }
            }
        }
        if (probeResult != null) {
            Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
            ProbeResultStrip(probeResult)
        }
        if (desktop) {
            Spacer(Modifier.height(Space.xs))
            CaptionText(
                pick(
                    "探测会先向端点获取模型列表，再保存。",
                    "Probe asks the endpoint for its model list before saving anything.",
                ),
                maxLines = 2,
                size = 10.5.sp,
            )
        }
    }
}

@Composable
private fun ProviderRow(provider: ProviderEntry) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = if (desktop) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarTile(
            icon = AgentIcons.Database,
            size = if (desktop) 24.dp else 36.dp,
            container = if (desktop) scheme.surfaceContainerHigh else scheme.primaryContainer,
            content = if (desktop) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(if (desktop) Space.sm else Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                provider.id.ifBlank { pick("未命名服务商", "unnamed provider") },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = metrics().bodySize,
                    fontWeight = FontWeight.Medium,
                ),
            )
            CaptionText(
                provider.baseUrl.ifBlank { pick("无基础地址", "no base url") },
                Modifier.padding(top = 1.dp),
                maxLines = 1,
                size = if (desktop) 10.5.sp else 12.sp,
            )
        }
        Spacer(Modifier.width(Space.sm))
        val modelLabel = when (provider.models.size) {
            0 -> pick("无模型", "no models")
            1 -> pick("1 个模型", "1 model")
            else -> pick("${provider.models.size} 个模型", "${provider.models.size} models")
        }
        if (desktop) {
            MonoText(modelLabel, size = 10.5.sp)
            Spacer(Modifier.width(Space.sm))
            MetaChip(provider.kind.ifBlank { "local" }, mono = true)
        } else {
            MetaChip(modelLabel, mono = true)
        }
    }
}

@Composable
private fun ProbeResultStrip(text: String) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (desktop) Radius.xs else Radius.sm)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = if (desktop) Space.sm else Space.md, vertical = if (desktop) 6.dp else 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            AgentIcons.Info,
            null,
            Modifier.size(if (desktop) 11.dp else 14.dp),
            tint = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(if (desktop) 6.dp else Space.sm))
        CaptionText(text, maxLines = 3, size = if (desktop) 11.sp else 13.sp)
    }
}

@Composable
private fun WatchdogSection(
    snapshot: WorkspaceSnapshot,
    answer: String?,
    busy: Boolean,
    onAsk: (String) -> Unit,
) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    var question by remember { mutableStateOf("") }
    val alerts = snapshot.watchdog.alerts
    val submit = {
        val text = question.trim()
        if (text.isNotEmpty() && !busy) {
            onAsk(text)
            question = ""
        }
    }
    SettingsSection(
        title = pick("看门狗", "Watchdog"),
        icon = AgentIcons.Shield,
        subtitle = pick("独立控制循环", "independent control loop"),
        tone = if (snapshot.watchdog.healthy) Tone.POSITIVE else Tone.DANGER,
        status = { StatusPill(snapshot.watchdog.healthy, statusLabel("healthy"), statusLabel("attention")) },
    ) {
        KVRow(pick("最后心跳", "Last heartbeat"), formatRelative(snapshot.watchdog.lastHeartbeat, nowMillis()))
        if (alerts.isEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AgentIcons.CheckCircle,
                    null,
                    Modifier.size(if (desktop) 12.dp else 16.dp),
                    tint = scheme.secondary,
                )
                Spacer(Modifier.width(if (desktop) 6.dp else Space.sm))
                CaptionText(
                    pick("暂无告警，循环平稳。", "No alerts recorded. The loop is quiet."),
                    maxLines = 2,
                    size = if (desktop) 11.sp else 13.sp,
                )
            }
        } else {
            SectionLabel(pick("最近告警", "Recent alerts"), color = scheme.error)
            Spacer(Modifier.height(Space.xs))
            alerts.takeLast(4).forEach { alert ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Icon(
                        AgentIcons.AlertTriangle,
                        null,
                        Modifier.size(if (desktop) 11.dp else 15.dp),
                        tint = scheme.error,
                    )
                    Spacer(Modifier.width(if (desktop) 6.dp else Space.sm))
                    Text(
                        alert,
                        maxLines = if (desktop) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (desktop) 11.sp else 13.sp),
                    )
                }
                Spacer(Modifier.height(if (desktop) 3.dp else 6.dp))
            }
        }
        Spacer(Modifier.height(Space.xs))
        ThinRule(alpha = if (desktop) 0.3f else 0.35f)
        Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
        if (desktop) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                SettingsField(
                    label = pick("向看门狗提问", "Ask the watchdog"),
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f),
                    placeholder = watchdogQuestionHint(),
                    enabled = !busy,
                )
                SettingsAction(
                    pick("提问", "Ask"),
                    AgentIcons.Send,
                    onClick = submit,
                    modifier = Modifier.padding(bottom = 15.dp),
                    enabled = !busy && question.isNotBlank(),
                )
                if (busy) {
                    Spacer(Modifier.width(2.dp))
                    BusyIndicator(Modifier.padding(bottom = 21.dp))
                }
            }
        } else {
            SettingsField(
                label = pick("向看门狗提问", "Ask the watchdog"),
                value = question,
                onValueChange = { question = it },
                placeholder = watchdogQuestionHint(),
                enabled = !busy,
            )
            Spacer(Modifier.height(Space.md))
            SettingsAction(
                if (busy) pick("提问中", "Asking") else pick("向看门狗提问", "Ask the watchdog"),
                AgentIcons.Send,
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && question.isNotBlank(),
            )
        }
        if (busy) {
            Spacer(Modifier.height(if (desktop) Space.xs else Space.sm))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(size = if (desktop) 5.dp else 6.dp)
                Spacer(Modifier.width(6.dp))
                MonoText(pick("看门狗正在工作", "watchdog is working"), size = if (desktop) 9.5.sp else 11.sp)
            }
        }
        if (answer != null) {
            Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
            SectionLabel(pick("回答", "Answer"))
            Spacer(Modifier.height(Space.xs))
            CodeSurface(answer, maxLines = if (desktop) 12 else 8)
        }
    }
}

@Composable
private fun WorkersSection(snapshot: WorkspaceSnapshot, onCancelTask: (String) -> Unit) {
    val desktop = isDesktop()
    val empty = snapshot.tasks.isEmpty() && snapshot.workers.isEmpty()
    SettingsSection(
        title = pick("执行体与任务", "Workers and tasks"),
        icon = AgentIcons.Users,
        subtitle = pick("取消会停止执行体，并把任务标记为失败", "cancel stops the worker and marks the task failed"),
        tone = Tone.WARNING,
        status = {
            MetaChip(
                text = if (empty) {
                    pick("空闲", "idle")
                } else {
                    pick("${snapshot.tasks.size} 个任务", "${snapshot.tasks.size} tasks")
                },
                tone = if (empty) Tone.MUTED else Tone.NEUTRAL,
                mono = true,
            )
        },
    ) {
        if (empty) {
            EmptyHint(
                icon = AgentIcons.Users,
                title = pick("尚未分派执行体", "No workers dispatched"),
                detail = pick(
                    "先配置模型，再发送任务。执行体的运行记录与任务会显示在这里。",
                    "Configure a model, then send a task. Worker runs and their tasks show up here.",
                ),
                dense = desktop,
            )
        } else {
            if (snapshot.workers.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(if (desktop) Space.xs else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    snapshot.workers.forEach { worker -> WorkerChip(worker) }
                }
                Spacer(Modifier.height(if (desktop) Space.sm else Space.md))
            }
            if (snapshot.tasks.isEmpty()) {
                CaptionText(
                    pick(
                        "暂无任务记录。执行体领取任务后，引擎会在这里上报。",
                        "No task rows yet. The engine reports tasks once a worker claims one.",
                    ),
                    maxLines = 2,
                    size = if (desktop) 11.sp else 13.sp,
                )
            } else {
                Column(Modifier.fillMaxWidth()) {
                    snapshot.tasks.forEachIndexed { index, task ->
                        if (desktop && index > 0) ThinRule(alpha = 0.25f)
                        TaskRow(task, onCancelTask)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerChip(worker: WorkerEntry) {
    val running = worker.status.equals("running", ignoreCase = true) ||
        worker.status.equals("busy", ignoreCase = true)
    val label = worker.id.ifBlank { pick("执行体", "worker") } + " · " +
        statusLabel(worker.status.ifBlank { "unknown" })
    MetaChip(text = label, tone = if (running) Tone.ACCENT else Tone.NEUTRAL, mono = true)
}

@Composable
private fun TaskRow(task: TaskEntry, onCancelTask: (String) -> Unit) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = if (desktop) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarTile(
            icon = AgentIcons.Cpu,
            size = if (desktop) 22.dp else 34.dp,
            container = if (desktop) scheme.surfaceContainerHigh else scheme.secondaryContainer,
            content = if (desktop) scheme.onSurfaceVariant else scheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(if (desktop) Space.sm else Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                task.title.ifBlank { pick("未命名任务", "untitled task") },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = metrics().bodySize,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaptionText(
                    statusLabel(task.status.ifBlank { "unknown" }),
                    maxLines = 1,
                    size = if (desktop) 10.5.sp else 12.sp,
                )
                if (desktop) {
                    Spacer(Modifier.width(6.dp))
                    MonoText(task.id.take(12), size = 9.5.sp)
                }
            }
        }
        Spacer(Modifier.width(Space.sm))
        CancelTaskAction { onCancelTask(task.id) }
    }
}

@Composable
private fun CancelTaskAction(onClick: () -> Unit) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .height(if (desktop) 26.dp else 44.dp)
            .clip(RoundedCornerShape(Radius.xs))
            .clickable(onClick = onClick)
            .padding(horizontal = if (desktop) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AgentIcons.Stop,
            null,
            Modifier.size(if (desktop) 10.dp else 13.dp),
            tint = scheme.error,
        )
        Spacer(Modifier.width(Space.xs))
        Text(
            pick("取消", "Cancel"),
            maxLines = 1,
            color = scheme.error,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (desktop) 10.5.sp else 12.sp),
        )
    }
}

@Composable
private fun SettingsAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (desktop) Radius.xs else Radius.md)
    val container = when {
        !filled -> Color.Transparent
        enabled -> scheme.primary
        else -> scheme.surfaceContainerHighest
    }
    val contentColor = when {
        !filled && enabled -> scheme.onSurfaceVariant
        !filled -> scheme.onSurfaceVariant.copy(alpha = 0.4f)
        enabled -> scheme.onPrimary
        else -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Row(
        modifier
            .height(if (desktop) 28.dp else 48.dp)
            .clip(shape)
            .background(container)
            .border(
                1.dp,
                if (filled) Color.Transparent else scheme.outlineVariant.copy(alpha = if (enabled) 0.7f else 0.35f),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (desktop) 9.dp else 16.dp),
        horizontalArrangement = if (desktop) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            Modifier.size(if (desktop) 12.dp else 18.dp),
            tint = contentColor,
        )
        Spacer(Modifier.width(if (desktop) 5.dp else Space.sm))
        Text(
            label,
            maxLines = 1,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = if (desktop) 11.5.sp else 15.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    val desktop = isDesktop()
    val scheme = MaterialTheme.colorScheme
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
        focusedContainerColor = if (desktop) Color.Transparent else scheme.surfaceContainerHighest,
        unfocusedContainerColor = if (desktop) Color.Transparent else scheme.surfaceContainerHighest,
        disabledContainerColor = if (desktop) Color.Transparent else scheme.surfaceContainerHighest,
        focusedBorderColor = if (desktop) scheme.primary.copy(alpha = 0.7f) else Color.Transparent,
        unfocusedBorderColor = if (desktop) scheme.outlineVariant.copy(alpha = 0.7f) else Color.Transparent,
        disabledBorderColor = if (desktop) scheme.outlineVariant.copy(alpha = 0.35f) else Color.Transparent,
        cursorColor = scheme.primary,
        focusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
    if (desktop) {
        Column(modifier.fillMaxWidth()) {
            SectionLabel(label)
            Spacer(Modifier.height(Space.xs))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                placeholder = {
                    Text(
                        placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    )
                },
                shape = RoundedCornerShape(Radius.xs),
                colors = colors,
            )
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            label = {
                Text(
                    label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                )
            },
            placeholder = {
                Text(
                    placeholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                )
            },
            shape = RoundedCornerShape(Radius.md),
            colors = colors,
        )
    }
}

private fun parseModels(text: String): String =
    text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")

private fun effectLabel(mode: EffectMode): String = when (mode) {
    EffectMode.DISABLED -> pick("关闭", "Off")
    EffectMode.BLUR -> pick("模糊", "Blur")
    EffectMode.GLASS -> pick("玻璃", "Glass")
}

private fun effectExplanation(mode: EffectMode): String = when (mode) {
    EffectMode.DISABLED -> pick(
        "效果已关闭。胶囊表面回退为纯色容器。",
        "Effects are off. Capsule surfaces fall back to a flat container colour.",
    )
    EffectMode.BLUR -> pick(
        "模糊按上方半径对背景采样。Android 10 及以上通过截图层渲染。",
        "Blur samples the backdrop at the radius above. Android 10 and later renders it through a capture-backed layer.",
    )
    EffectMode.GLASS -> pick(
        "玻璃使用固定的光学预设。半径仅对模糊生效。",
        "Glass applies a fixed optical preset. The radius control only applies to Blur.",
    )
}
