package sandrone.agentflows.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sandrone.agentflows.shared.MotionTokens
import sandrone.agentflows.shared.nowMillis

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun TonePill(tone: FlowTone, text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container = when (tone) {
        FlowTone.TRIGGER -> scheme.tertiaryContainer
        FlowTone.ROUTER -> scheme.primaryContainer
        FlowTone.WORKER -> scheme.secondaryContainer
        FlowTone.TOOL -> scheme.surfaceContainerHighest
        FlowTone.RESULT -> scheme.secondaryContainer
        FlowTone.ALERT -> scheme.errorContainer
        FlowTone.NEUTRAL -> scheme.surfaceContainerHighest
    }
    val content = when (tone) {
        FlowTone.TRIGGER -> scheme.onTertiaryContainer
        FlowTone.ROUTER -> scheme.onPrimaryContainer
        FlowTone.WORKER -> scheme.onSecondaryContainer
        FlowTone.TOOL -> scheme.onSurfaceVariant
        FlowTone.RESULT -> scheme.onSecondaryContainer
        FlowTone.ALERT -> scheme.onErrorContainer
        FlowTone.NEUTRAL -> scheme.onSurfaceVariant
    }
    Pill(text, modifier, container, content)
}

@Composable
fun StatusDot(active: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        androidx.compose.animation.core.tween(MotionTokens.ShellColorMillis),
    )
    Box(modifier.size(8.dp).background(color, CircleShape))
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.headlineSmall, color = accent)
            if (caption != null) {
                Text(
                    caption,
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 92.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val peak = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Row(
        modifier.fillMaxWidth().height(chartHeight + 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value / peak).coerceIn(0f, 1f)
            val barHeight = (chartHeight * fraction).coerceAtLeast(3.dp)
            Column(
                Modifier.weight(1f).height(chartHeight + 20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(accent.copy(alpha = if (fraction > 0f) 0.85f else 0.25f), RoundedCornerShape(6.dp)),
                )
                Text(
                    labels.getOrNull(index).orEmpty(),
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ActivityGrid(
    dailyCounts: Map<Long, Int>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
) {
    val today = nowMillis() / MILLIS_PER_DAY
    val firstDay = today - (weeks * 7L - 1L)
    val peak = dailyCounts.values.maxOrNull()?.takeIf { it > 0 } ?: 1
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (week in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (day in 0 until 7) {
                    val index = firstDay + week * 7L + day
                    val count = dailyCounts[index] ?: 0
                    val intensity = if (count == 0) 0f else (count.toFloat() / peak).coerceIn(0.2f, 1f)
                    val color = if (intensity == 0f) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                    }
                    Box(Modifier.size(11.dp).background(color, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

@Composable
fun MonospaceBlock(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp).horizontalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            softWrap = false,
        )
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
    }
}

@Composable
fun ExpandableArea(expanded: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(androidx.compose.animation.core.tween(MotionTokens.ContentMillis)) +
            expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(androidx.compose.animation.core.tween(140)) +
            shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)),
    ) {
        content()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(title, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
        Text(
            detail,
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SelectablePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
}
