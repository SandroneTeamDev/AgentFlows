package sandrone.agentflows.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AgentBrand {
    val Navy = Color(0xFF020852)
    val Yellow = Color(0xFFF9DB09)
    const val Silhouette =
        "M 36.5 7 C 31 26 25 48 22.2 66 C 19.6 81 31 89.6 47.5 89.6 C 64 89.6 74.6 82 74 66 C 73.4 50 68 26 62.5 6.5 L 50.5 45.5 L 36.5 7 Z"
    const val Eye = "M 38 46.5 L 38 64.5"
    const val Chevron = "M 69 49 L 57 55 L 68.5 61.5"
    const val StrokeWidth = 6.5f
}

val AgentLogoGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "AgentLogoGlyph",
        defaultWidth = 100.dp,
        defaultHeight = 100.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
    )
        .addPath(
            pathData = PathParser().parsePathString(AgentBrand.Silhouette).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = AgentBrand.StrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

val AgentLogoMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "AgentLogoMark",
        defaultWidth = 100.dp,
        defaultHeight = 100.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
    )
        .addPath(
            pathData = PathParser().parsePathString(AgentBrand.Silhouette).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = AgentBrand.StrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            pathData = PathParser().parsePathString(AgentBrand.Eye).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = AgentBrand.StrokeWidth,
            strokeLineCap = StrokeCap.Round,
        )
        .addPath(
            pathData = PathParser().parsePathString(AgentBrand.Chevron).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = AgentBrand.StrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

@Composable
fun BrandTile(
    size: Dp,
    modifier: Modifier = Modifier,
    markRatio: Float = 0.72f,
    cornerRatio: Float = 0.3f,
) {
    val detailed = size >= 26.dp
    Surface(
        modifier.size(size),
        shape = RoundedCornerShape(size * cornerRatio),
        color = AgentBrand.Navy,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (detailed) AgentLogoMark else AgentLogoGlyph,
                contentDescription = null,
                modifier = Modifier.size(size * if (detailed) markRatio else markRatio + 0.04f),
                tint = AgentBrand.Yellow,
            )
        }
    }
}
