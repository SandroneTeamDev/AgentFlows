package sandrone.agentflows.shared.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Tone.container(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        Tone.NEUTRAL -> scheme.surfaceContainerHigh
        Tone.ACCENT -> scheme.primaryContainer
        Tone.POSITIVE -> scheme.secondaryContainer
        Tone.WARNING -> scheme.tertiaryContainer
        Tone.DANGER -> scheme.errorContainer
        Tone.MUTED -> scheme.surfaceContainer
    }
}

@Composable
fun Tone.content(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        Tone.NEUTRAL -> scheme.onSurfaceVariant
        Tone.ACCENT -> scheme.onPrimaryContainer
        Tone.POSITIVE -> scheme.onSecondaryContainer
        Tone.WARNING -> scheme.onTertiaryContainer
        Tone.DANGER -> scheme.onErrorContainer
        Tone.MUTED -> scheme.onSurfaceVariant
    }
}

@Composable
fun Tone.accent(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        Tone.NEUTRAL -> scheme.onSurfaceVariant
        Tone.ACCENT -> scheme.primary
        Tone.POSITIVE -> scheme.secondary
        Tone.WARNING -> scheme.tertiary
        Tone.DANGER -> scheme.error
        Tone.MUTED -> scheme.outline
    }
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: TextUnit = metrics().microSize,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = size,
            fontWeight = weight,
        ),
    )
}

@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = 3,
    size: TextUnit = metrics().captionSize,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = size),
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = metrics().bodySize,
            fontWeight = weight,
        ),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = metrics().microSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        ),
    )
}

@Composable
fun ThinRule(modifier: Modifier = Modifier, inset: Dp = 0.dp, alpha: Float = 0.55f) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)),
    )
}

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    tone: Tone = Tone.MUTED,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (isDesktop()) Radius.sm else Radius.lg)
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isDesktop()) scheme.surfaceContainerLow else tone.container(),
    ) {
        Box(
            if (isDesktop()) {
                Modifier.border(1.dp, scheme.outlineVariant.copy(alpha = 0.7f), shape)
            } else {
                Modifier
            },
        ) {
            Column(Modifier.padding(metrics().cardPadding), content = content)
        }
    }
}

@Composable
fun PanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(if (isDesktop()) 14.dp else 18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(if (isDesktop()) 8.dp else 10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = metrics().titleSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                CaptionText(subtitle, Modifier.padding(top = 1.dp), maxLines = 2)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    mono: Boolean = false,
) {
    val shape = RoundedCornerShape(Radius.xs)
    val scheme = MaterialTheme.colorScheme
    val container = when {
        selected -> scheme.primaryContainer
        isDesktop() -> Color.Transparent
        else -> tone.container()
    }
    val content = when {
        selected -> scheme.onPrimaryContainer
        else -> tone.content()
    }
    val borderColor = when {
        selected -> scheme.primary.copy(alpha = 0.55f)
        isDesktop() -> scheme.outlineVariant.copy(alpha = 0.8f)
        else -> Color.Transparent
    }
    Row(
        modifier
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = if (isDesktop()) 7.dp else 10.dp,
                vertical = if (isDesktop()) 2.dp else 5.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(if (isDesktop()) 11.dp else 13.dp), tint = content)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = content,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (isDesktop()) 10.5.sp else 11.5.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            ),
        )
    }
}

@Composable
fun GhostIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = if (isDesktop()) 26.dp else 38.dp,
    iconSize: Dp = if (isDesktop()) 15.dp else 19.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.xs))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            Modifier.size(iconSize),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun StatusDot(active: Boolean, modifier: Modifier = Modifier, size: Dp = if (isDesktop()) 6.dp else 8.dp) {
    val color by animateFloatAsState(if (active) 1f else 0.45f, tween(Motion.Base), label = "dotAlpha")
    Box(
        modifier
            .size(size)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = color) else MaterialTheme.colorScheme.outline.copy(alpha = color),
                CircleShape,
            ),
    )
}

@Composable
fun StatusPill(active: Boolean, onText: String, offText: String, modifier: Modifier = Modifier) {
    val tone = if (active) Tone.ACCENT else Tone.NEUTRAL
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(if (isDesktop()) Color.Transparent else tone.container())
            .then(
                if (isDesktop()) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(Radius.xs))
                else Modifier,
            )
            .padding(horizontal = if (isDesktop()) 7.dp else 10.dp, vertical = if (isDesktop()) 2.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(active, size = if (isDesktop()) 5.dp else 7.dp)
        Spacer(Modifier.width(5.dp))
        Text(
            if (active) onText else offText,
            color = tone.content(),
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isDesktop()) 10.5.sp else 11.5.sp),
        )
    }
}

@Composable
fun AvatarTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = if (isDesktop()) 30.dp else 40.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    shape: RoundedCornerShape = RoundedCornerShape(if (isDesktop()) Radius.xs else Radius.md),
) {
    Surface(modifier.size(size), shape = shape, color = container, contentColor = content) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(size * 0.52f))
        }
    }
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accent: Color? = null,
    mono: Boolean = isDesktop(),
) {
    Column(modifier) {
        SectionLabel(label)
        Text(
            value,
            Modifier.padding(top = if (isDesktop()) 2.dp else 4.dp),
            color = accent ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = metrics().heroSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            ),
        )
        if (caption != null) {
            CaptionText(caption, Modifier.padding(top = 2.dp), maxLines = 1)
        }
    }
}

@Composable
fun KVRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Row(
        modifier.fillMaxWidth().padding(vertical = if (isDesktop()) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaptionText(label, Modifier.weight(1f), maxLines = 1, size = if (isDesktop()) metrics().captionSize else 13.sp)
        MonoText(
            value,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            size = if (isDesktop()) metrics().microSize else 12.sp,
        )
    }
}

@Composable
fun DenseRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(if (isDesktop()) Radius.xs else Radius.md)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = if (isDesktop()) 8.dp else 12.dp,
                vertical = if (isDesktop()) 6.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(if (isDesktop()) 8.dp else 12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = metrics().bodySize,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                ),
            )
            if (subtitle != null) {
                CaptionText(subtitle, Modifier.padding(top = 1.dp), maxLines = 1)
            }
        }
        if (meta != null) {
            Spacer(Modifier.width(8.dp))
            MonoText(meta)
        }
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}

@Composable
fun UnderlineTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int>? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, label ->
            val active = index == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(topStart = Radius.xs, topEnd = Radius.xs))
                    .clickable { onSelect(index) }
                    .padding(horizontal = if (isDesktop()) 10.dp else 14.dp, vertical = if (isDesktop()) 6.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        maxLines = 1,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = if (isDesktop()) 12.sp else 13.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                    val count = counts?.getOrNull(index)
                    if (count != null && count > 0) {
                        Spacer(Modifier.width(5.dp))
                        MonoText(count.toString(), size = if (isDesktop()) 9.5.sp else 10.5.sp)
                    }
                }
                Spacer(Modifier.height(if (isDesktop()) 4.dp else 7.dp))
                Box(
                    Modifier
                        .width(if (active) (if (isDesktop()) 18.dp else 22.dp) else 0.dp)
                        .height(2.dp)
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
fun ChipFilterRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<String, Int> = emptyMap(),
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(if (isDesktop()) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (key, label) ->
            val count = counts[key]
            MetaChip(
                text = if (count != null) "$label $count" else label,
                selected = key == selected,
                onClick = { onSelect(key) },
                tone = Tone.MUTED,
            )
        }
    }
}

@Composable
fun EmptyHint(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    dense: Boolean = isDesktop(),
) {
    Column(
        modifier.fillMaxWidth().padding(if (dense) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            modifier = Modifier.size(if (dense) 42.dp else 64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(if (dense) 19.dp else 27.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            title,
            Modifier.padding(top = if (dense) 12.dp else 16.dp),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = metrics().titleSize,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
        CaptionText(
            detail,
            Modifier.padding(top = 6.dp).widthIn(max = if (dense) 420.dp else 300.dp),
            maxLines = 4,
        )
    }
}

@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = if (isDesktop()) 4.dp else 6.dp,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(clamped, tween(Motion.Base, easing = Motion.EaseOut), label = "meter")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun BarStrip(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    height: Dp = if (isDesktop()) 72.dp else 88.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    showLabels: Boolean = true,
) {
    val peak = remember(values) { values.maxOrNull()?.takeIf { it > 0f } ?: 1f }
    Row(
        modifier.fillMaxWidth().height(height + if (showLabels) 16.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isDesktop()) 3.dp else 5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value / peak).coerceIn(0f, 1f)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((height * fraction).coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(if (fraction > 0f) color.copy(alpha = 0.85f) else color.copy(alpha = 0.18f)),
                )
                if (showLabels) {
                    Text(
                        labels.getOrNull(index).orEmpty(),
                        Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isDesktop()) 8.5.sp else 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 36.dp,
) {
    if (values.size < 2) {
        Box(modifier.fillMaxWidth().height(height))
        return
    }
    val peak = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Canvas(modifier.fillMaxWidth().height(height)) {
        val stepX = size.width / (values.size - 1).toFloat()
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - (value / peak).coerceIn(0f, 1f) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 1.6f * density))
    }
}

@Composable
fun Donut(
    slices: List<Pair<String, Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = if (isDesktop()) 92.dp else 116.dp,
    strokeRatio: Float = 0.2f,
) {
    val total = slices.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * strokeRatio
        val diameter = this.size.minDimension - stroke
        val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var start = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = 360f * (slice.second / total)
            if (sweep > 0f) {
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            }
            start += sweep
        }
    }
}

@Composable
fun CodeSurface(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(if (isDesktop()) Radius.xs else Radius.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(if (isDesktop()) 10.dp else 12.dp)
                .horizontalScroll(rememberScrollState()),
            softWrap = false,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isDesktop()) 11.sp else 12.sp,
            ),
        )
    }
}

@Composable
fun Collapsible(expanded: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(Motion.Fast)) +
            expandVertically(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(tween(Motion.Instant)) +
            shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)),
    ) {
        content()
    }
}

@Composable
fun BusyIndicator(modifier: Modifier = Modifier, size: Dp = if (isDesktop()) 12.dp else 16.dp) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = if (isDesktop()) 1.5.dp else 2.dp,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun PulsingDot(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, size: Dp = 6.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760, easing = Motion.EaseInOut)),
        label = "pulseAlpha",
    )
    Box(modifier.size(size).background(color.copy(alpha = alpha), CircleShape))
}

@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(label, Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun DividerListRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        content()
        ThinRule(alpha = 0.35f)
    }
}

@Composable
fun InlineIconText(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(if (isDesktop()) 12.dp else 14.dp), tint = tint)
        Spacer(Modifier.width(4.dp))
        MonoText(text, color = tint)
    }
}

@Composable
fun KeyHint(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(Radius.xs),
        modifier = modifier,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
fun NumberCell(text: String, modifier: Modifier = Modifier, width: Dp = 74.dp, color: Color = MaterialTheme.colorScheme.onSurface) {
    Box(modifier.width(width), contentAlignment = Alignment.CenterEnd) {
        MonoText(text, color = color, size = if (isDesktop()) 11.sp else 12.sp)
    }
}

@Composable
fun TouchRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (isDesktop()) 26.dp else 44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        content()
    }
}

@Composable
fun HeatGrid(
    dailyCounts: Map<Long, Int>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    today: Long = 0L,
) {
    val day = if (today > 0L) today else sandrone.agentflows.shared.nowMillis() / 86_400_000L
    val first = day - (weeks * 7L - 1L)
    val peak = dailyCounts.values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val cell = if (isDesktop()) 9.dp else 12.dp
    val gap = if (isDesktop()) 2.dp else 3.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (week in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (index in 0 until 7) {
                    val key = first + week * 7L + index
                    val count = dailyCounts[key] ?: 0
                    val intensity = if (count == 0) 0f else (count.toFloat() / peak).coerceIn(0.22f, 1f)
                    val color = if (intensity == 0f) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                    }
                    Box(Modifier.size(cell).clip(RoundedCornerShape(if (isDesktop()) 2.dp else 3.dp)).background(color))
                }
            }
        }
    }
}

