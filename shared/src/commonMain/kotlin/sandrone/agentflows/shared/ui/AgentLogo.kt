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
        "M 36.4 7.2 C 31.5 25 26 47 21.8 66.5 C 19.5 80.5 31 89.2 47.3 89.2 C 64 89.2 74.5 83 75.8 70 C 76.6 60 70 28 62.3 5.7 L 50.5 46.1 L 36.4 7.2 Z"
    const val Eye = "M 37.9 50.2 L 37.9 62.7"
    const val Chevron = "M 67.4 51.4 L 58.4 54.8 L 67.4 60.4"
    const val ThinStroke = 2.2f
    const val MediumStroke = 4.4f
    const val BoldStroke = 6.5f
    const val GlyphStroke = 7.6f
}

private fun buildMark(name: String, strokeWidth: Float, withDetail: Boolean): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 100.dp,
        defaultHeight = 100.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
    ).addPath(
        pathData = PathParser().parsePathString(AgentBrand.Silhouette).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
    if (withDetail) {
        builder.addPath(
            pathData = PathParser().parsePathString(AgentBrand.Eye).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
        )
        builder.addPath(
            pathData = PathParser().parsePathString(AgentBrand.Chevron).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

val AgentLogoThin: ImageVector by lazy { buildMark("AgentLogoThin", AgentBrand.ThinStroke, true) }

val AgentLogoMedium: ImageVector by lazy { buildMark("AgentLogoMedium", AgentBrand.MediumStroke, true) }

val AgentLogoMark: ImageVector by lazy { buildMark("AgentLogoMark", AgentBrand.BoldStroke, true) }

val AgentLogoGlyph: ImageVector by lazy { buildMark("AgentLogoGlyph", AgentBrand.GlyphStroke, false) }

@Composable
fun BrandTile(
    size: Dp,
    modifier: Modifier = Modifier,
    markRatio: Float = 0.72f,
    cornerRatio: Float = 0.3f,
) {
    val glyphOnly = size < 26.dp
    val mark = when {
        glyphOnly -> AgentLogoGlyph
        size < 48.dp -> AgentLogoMark
        size < 96.dp -> AgentLogoMedium
        else -> AgentLogoThin
    }
    val ratio = if (glyphOnly) markRatio + 0.04f else markRatio
    Surface(
        modifier.size(size),
        shape = RoundedCornerShape(size * cornerRatio),
        color = AgentBrand.Navy,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = mark,
                contentDescription = null,
                modifier = Modifier.size(size * ratio),
                tint = AgentBrand.Yellow,
            )
        }
    }
}
