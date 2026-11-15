package sandrone.agentflows.shared.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ShellStyle { MOBILE, DESKTOP }

enum class WindowClass { COMPACT, MEDIUM, EXPANDED }

val LocalShellStyle = staticCompositionLocalOf { ShellStyle.MOBILE }

val LocalWindowClass = staticCompositionLocalOf { WindowClass.COMPACT }

fun windowClassOf(width: Dp): WindowClass = when {
    width < 600.dp -> WindowClass.COMPACT
    width < 1024.dp -> WindowClass.MEDIUM
    else -> WindowClass.EXPANDED
}

object Space {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 22.dp
    val xxl: Dp = 30.dp
    val huge: Dp = 40.dp
}

object Radius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 30.dp
}

object Motion {
    const val Instant: Int = 90
    const val Fast: Int = 140
    const val Base: Int = 200
    const val Slow: Int = 300
    val EaseOut: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

data class Metrics(
    val gutter: Dp,
    val cardGap: Dp,
    val cardPadding: Dp,
    val rowGap: Dp,
    val sectionGap: Dp,
    val sidebarWidth: Dp,
    val inspectorWidth: Dp,
    val contentMax: Dp,
    val composerMax: Dp,
    val bubbleMax: Dp,
    val bodySize: TextUnit,
    val captionSize: TextUnit,
    val microSize: TextUnit,
    val titleSize: TextUnit,
    val heroSize: TextUnit,
    val controlHeight: Dp,
    val touchTarget: Dp,
)

val MobileMetrics = Metrics(
    gutter = 16.dp,
    cardGap = 12.dp,
    cardPadding = 16.dp,
    rowGap = 10.dp,
    sectionGap = 18.dp,
    sidebarWidth = 280.dp,
    inspectorWidth = 300.dp,
    contentMax = 720.dp,
    composerMax = 720.dp,
    bubbleMax = 300.dp,
    bodySize = 15.sp,
    captionSize = 12.sp,
    microSize = 11.sp,
    titleSize = 17.sp,
    heroSize = 22.sp,
    controlHeight = 44.dp,
    touchTarget = 44.dp,
)

val DesktopMetrics = Metrics(
    gutter = 20.dp,
    cardGap = 12.dp,
    cardPadding = 14.dp,
    rowGap = 6.dp,
    sectionGap = 14.dp,
    sidebarWidth = 244.dp,
    inspectorWidth = 312.dp,
    contentMax = 1120.dp,
    composerMax = 980.dp,
    bubbleMax = 640.dp,
    bodySize = 13.sp,
    captionSize = 11.5.sp,
    microSize = 10.5.sp,
    titleSize = 14.sp,
    heroSize = 19.sp,
    controlHeight = 30.dp,
    touchTarget = 28.dp,
)

@Composable
fun metrics(): Metrics = if (LocalShellStyle.current == ShellStyle.DESKTOP) DesktopMetrics else MobileMetrics

@Composable
fun isDesktop(): Boolean = LocalShellStyle.current == ShellStyle.DESKTOP

enum class Tone { NEUTRAL, ACCENT, POSITIVE, WARNING, DANGER, MUTED }
