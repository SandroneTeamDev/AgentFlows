package sandrone.agentflows.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun glyph(name: String, builder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) { builder() }
        .build()

private fun PathBuilder.poly(vararg points: Float) {
    if (points.size < 6) return
    moveTo(points[0], points[1])
    var index = 2
    while (index + 1 < points.size) {
        lineTo(points[index], points[index + 1])
        index += 2
    }
    close()
}

private fun PathBuilder.box(x: Float, y: Float, width: Float, height: Float) {
    moveTo(x, y)
    lineTo(x + width, y)
    lineTo(x + width, y + height)
    lineTo(x, y + height)
    close()
}

private fun PathBuilder.hole(x: Float, y: Float, width: Float, height: Float) {
    moveTo(x, y)
    lineTo(x, y + height)
    lineTo(x + width, y + height)
    lineTo(x + width, y)
    close()
}

private fun PathBuilder.frame(x: Float, y: Float, width: Float, height: Float, thickness: Float) {
    box(x, y, width, height)
    hole(x + thickness, y + thickness, width - thickness * 2f, height - thickness * 2f)
}

private fun PathBuilder.band(
    cx: Float,
    cy: Float,
    outer: Float,
    inner: Float,
    startDeg: Float = 0f,
    sweepDeg: Float = 360f,
    segments: Int = 40,
) {
    val start = startDeg * PI / 180.0
    val sweep = sweepDeg * PI / 180.0
    for (index in 0..segments) {
        val angle = start + sweep * index / segments
        val x = cx + (cos(angle) * outer).toFloat()
        val y = cy + (sin(angle) * outer).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    for (index in segments downTo 0) {
        val angle = start + sweep * index / segments
        val x = cx + (cos(angle) * inner).toFloat()
        val y = cy + (sin(angle) * inner).toFloat()
        lineTo(x, y)
    }
    close()
}

private fun PathBuilder.disc(cx: Float, cy: Float, radius: Float) {
    band(cx, cy, radius, 0f, 0f, 360f, 32)
}

private fun PathBuilder.lens(cx: Float, cy: Float, halfWidth: Float, bulge: Float, segments: Int = 24) {
    for (index in 0..segments) {
        val t = index.toFloat() / segments
        val x = cx - halfWidth + halfWidth * 2f * t
        val y = cy - bulge * sin(PI * t).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    for (index in segments downTo 0) {
        val t = index.toFloat() / segments
        val x = cx - halfWidth + halfWidth * 2f * t
        val y = cy + bulge * sin(PI * t).toFloat()
        lineTo(x, y)
    }
    close()
}

object AgentIcons {

    val Chat: ImageVector by lazy {
        glyph("AgentFlows.Chat") {
            poly(4f, 3f, 20f, 3f, 20f, 15f, 10.5f, 15f, 5f, 20f, 5f, 15f, 4f, 15f)
        }
    }

    val Bolt: ImageVector by lazy {
        glyph("AgentFlows.Bolt") {
            poly(13f, 2f, 4.5f, 13.5f, 11f, 13.5f, 10f, 22f, 18.5f, 10.5f, 12f, 10.5f)
        }
    }

    val Chart: ImageVector by lazy {
        glyph("AgentFlows.Chart") {
            box(4f, 11f, 3f, 9f)
            box(10.5f, 4f, 3f, 16f)
            box(17f, 14f, 3f, 6f)
        }
    }

    val Settings: ImageVector by lazy {
        glyph("AgentFlows.Settings") {
            box(3f, 5f, 11f, 2f)
            box(17f, 4f, 3f, 4f)
            box(10f, 11f, 11f, 2f)
            box(4f, 10f, 3f, 4f)
            box(3f, 17f, 9f, 2f)
            box(15f, 16f, 3f, 4f)
        }
    }

    val Send: ImageVector by lazy {
        glyph("AgentFlows.Send") {
            poly(3f, 20f, 21f, 12f, 3f, 4f, 6.2f, 12f)
        }
    }

    val Stop: ImageVector by lazy {
        glyph("AgentFlows.Stop") {
            box(7f, 7f, 10f, 10f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        glyph("AgentFlows.ChevronDown") {
            poly(5.8f, 9.4f, 12f, 15.6f, 18.2f, 9.4f, 19.6f, 10.8f, 12f, 18.4f, 4.4f, 10.8f)
        }
    }

    val ChevronUp: ImageVector by lazy {
        glyph("AgentFlows.ChevronUp") {
            poly(5.8f, 14.6f, 12f, 8.4f, 18.2f, 14.6f, 19.6f, 13.2f, 12f, 5.6f, 4.4f, 13.2f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        glyph("AgentFlows.ChevronRight") {
            poly(9.4f, 5.8f, 15.6f, 12f, 9.4f, 18.2f, 10.8f, 19.6f, 18.4f, 12f, 10.8f, 4.4f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        glyph("AgentFlows.ChevronLeft") {
            poly(14.6f, 5.8f, 8.4f, 12f, 14.6f, 18.2f, 13.2f, 19.6f, 5.6f, 12f, 13.2f, 4.4f)
        }
    }

    val Check: ImageVector by lazy {
        glyph("AgentFlows.Check") {
            poly(9f, 16.2f, 4.8f, 12f, 3.4f, 13.4f, 9f, 19f, 21f, 7f, 19.6f, 5.6f)
        }
    }

    val Close: ImageVector by lazy {
        glyph("AgentFlows.Close") {
            poly(6.4f, 5f, 12f, 10.6f, 17.6f, 5f, 19f, 6.4f, 13.4f, 12f, 19f, 17.6f, 17.6f, 19f, 12f, 13.4f, 6.4f, 19f, 5f, 17.6f, 10.6f, 12f, 5f, 6.4f)
        }
    }

    val Plus: ImageVector by lazy {
        glyph("AgentFlows.Plus") {
            poly(11f, 4f, 13f, 4f, 13f, 11f, 20f, 11f, 20f, 13f, 13f, 13f, 13f, 20f, 11f, 20f, 11f, 13f, 4f, 13f, 4f, 11f, 11f, 11f)
        }
    }

    val Wave: ImageVector by lazy {
        glyph("AgentFlows.Wave") {
            poly(
                2f, 12f, 5f, 12f, 7f, 6f, 10f, 18f, 13f, 9f, 15.5f, 15f, 17.5f, 12f, 22f, 12f,
                22f, 14f, 18.5f, 14f, 16f, 18f, 13.5f, 12.5f, 10.5f, 21f, 7.5f, 9.5f, 6.5f, 14f, 2f, 14f,
            )
        }
    }

    val Search: ImageVector by lazy {
        glyph("AgentFlows.Search") {
            band(10.5f, 10.5f, 7f, 5.2f)
            poly(14.6f, 15.4f, 15.9f, 14.1f, 20.8f, 19f, 19.5f, 20.3f)
        }
    }

    val Menu: ImageVector by lazy {
        glyph("AgentFlows.Menu") {
            box(3f, 6f, 18f, 2f)
            box(3f, 11f, 18f, 2f)
            box(3f, 16f, 18f, 2f)
        }
    }

    val MoreH: ImageVector by lazy {
        glyph("AgentFlows.MoreH") {
            disc(5.5f, 12f, 1.8f)
            disc(12f, 12f, 1.8f)
            disc(18.5f, 12f, 1.8f)
        }
    }

    val Terminal: ImageVector by lazy {
        glyph("AgentFlows.Terminal") {
            frame(3f, 4f, 18f, 16f, 2f)
            poly(7.5f, 9f, 11f, 12f, 7.5f, 15f, 8.7f, 16.2f, 13.2f, 12f, 8.7f, 7.8f)
            box(12.5f, 15f, 5f, 1.6f)
        }
    }

    val Code: ImageVector by lazy {
        glyph("AgentFlows.Code") {
            poly(8.6f, 7.6f, 3.2f, 12f, 8.6f, 16.4f, 10f, 15f, 5.9f, 12f, 10f, 9f)
            poly(15.4f, 7.6f, 20.8f, 12f, 15.4f, 16.4f, 14f, 15f, 18.1f, 12f, 14f, 9f)
        }
    }

    val Brain: ImageVector by lazy {
        glyph("AgentFlows.Brain") {
            band(12f, 12f, 8.5f, 6.4f)
            box(11.1f, 4.4f, 1.8f, 15.2f)
        }
    }

    val Play: ImageVector by lazy {
        glyph("AgentFlows.Play") {
            poly(7f, 4f, 20f, 12f, 7f, 20f)
        }
    }

    val Pause: ImageVector by lazy {
        glyph("AgentFlows.Pause") {
            box(7f, 4f, 3f, 16f)
            box(14f, 4f, 3f, 16f)
        }
    }

    val Copy: ImageVector by lazy {
        glyph("AgentFlows.Copy") {
            box(8f, 3f, 11f, 11f)
            frame(3f, 9f, 13f, 12f, 1.8f)
        }
    }

    val Refresh: ImageVector by lazy {
        glyph("AgentFlows.Refresh") {
            band(12f, 12f, 8.4f, 6.2f, -60f, 300f)
            poly(14.2f, 2.6f, 19.6f, 4.2f, 16.2f, 9.6f)
        }
    }

    val Download: ImageVector by lazy {
        glyph("AgentFlows.Download") {
            poly(11f, 3f, 13f, 3f, 13f, 12f, 17f, 8f, 18.4f, 9.4f, 12f, 15.8f, 5.6f, 9.4f, 7f, 8f, 11f, 12f)
            box(4f, 17f, 16f, 2f)
        }
    }

    val Upload: ImageVector by lazy {
        glyph("AgentFlows.Upload") {
            poly(11f, 15.8f, 13f, 15.8f, 13f, 6.8f, 17f, 10.8f, 18.4f, 9.4f, 12f, 3f, 5.6f, 9.4f, 7f, 10.8f, 11f, 6.8f)
            box(4f, 17f, 16f, 2f)
        }
    }

    val Trash: ImageVector by lazy {
        glyph("AgentFlows.Trash") {
            box(4f, 5f, 16f, 2f)
            box(9f, 2f, 6f, 2f)
            frame(6f, 7f, 12f, 14f, 1.6f)
        }
    }

    val Edit: ImageVector by lazy {
        glyph("AgentFlows.Edit") {
            poly(4f, 16.6f, 15.6f, 5f, 19f, 8.4f, 7.4f, 20f, 4f, 20f)
            poly(16.8f, 3.8f, 18.2f, 2.4f, 21.6f, 5.8f, 20.2f, 7.2f)
        }
    }

    val Link: ImageVector by lazy {
        glyph("AgentFlows.Link") {
            poly(
                9.5f, 7.5f, 6.5f, 7.5f, 3f, 11f, 6.5f, 14.5f, 9.5f, 14.5f, 9.5f, 12.5f,
                7.3f, 12.5f, 5f, 11f, 7.3f, 9.5f, 9.5f, 9.5f,
            )
            poly(
                14.5f, 7.5f, 17.5f, 7.5f, 21f, 11f, 17.5f, 14.5f, 14.5f, 14.5f, 14.5f, 12.5f,
                16.7f, 12.5f, 19f, 11f, 16.7f, 9.5f, 14.5f, 9.5f,
            )
            box(8.4f, 10.2f, 7.2f, 1.6f)
        }
    }

    val Eye: ImageVector by lazy {
        glyph("AgentFlows.Eye") {
            lens(12f, 12f, 9.6f, 7.2f)
            disc(12f, 12f, 3.2f)
        }
    }

    val Clock: ImageVector by lazy {
        glyph("AgentFlows.Clock") {
            band(12f, 12f, 8.6f, 6.6f)
            poly(11.2f, 6.6f, 12.8f, 6.6f, 12.8f, 12.4f, 16.6f, 12.4f, 16.6f, 14f, 11.2f, 14f)
        }
    }

    val CheckCircle: ImageVector by lazy {
        glyph("AgentFlows.CheckCircle") {
            band(12f, 12f, 9.5f, 7.6f)
            poly(7.6f, 12.2f, 10.6f, 15.2f, 16.6f, 9.2f, 17.8f, 10.4f, 10.6f, 17.6f, 6.4f, 13.4f)
        }
    }

    val AlertTriangle: ImageVector by lazy {
        glyph("AgentFlows.AlertTriangle") {
            poly(12f, 2.6f, 22f, 20.6f, 2f, 20.6f)
            hole(12f, 7.2f, 7.2f, 9.4f)
            poly(11.1f, 9.6f, 12.9f, 9.6f, 12.9f, 14.6f, 11.1f, 14.6f)
            disc(12f, 17.4f, 1.2f)
        }
    }

    val XCircle: ImageVector by lazy {
        glyph("AgentFlows.XCircle") {
            band(12f, 12f, 9.5f, 7.6f)
            poly(8.6f, 7.4f, 12f, 10.8f, 15.4f, 7.4f, 16.6f, 8.6f, 13.2f, 12f, 16.6f, 15.4f, 15.4f, 16.6f, 12f, 13.2f, 8.6f, 16.6f, 7.4f, 15.4f, 10.8f, 12f, 7.4f, 8.6f)
        }
    }

    val Info: ImageVector by lazy {
        glyph("AgentFlows.Info") {
            band(12f, 12f, 9.5f, 7.6f)
            disc(12f, 7.6f, 1.5f)
            box(11.1f, 10.6f, 1.8f, 6f)
        }
    }

    val Sparkle: ImageVector by lazy {
        glyph("AgentFlows.Sparkle") {
            poly(12f, 2f, 14f, 10f, 22f, 12f, 14f, 14f, 12f, 22f, 10f, 14f, 2f, 12f, 10f, 10f)
        }
    }

    val Layers: ImageVector by lazy {
        glyph("AgentFlows.Layers") {
            poly(12f, 2.6f, 21f, 7.6f, 12f, 12.6f, 3f, 7.6f)
            poly(3f, 11.6f, 12f, 16.6f, 21f, 11.6f, 21f, 14f, 12f, 19f, 3f, 14f)
        }
    }

    val Folder: ImageVector by lazy {
        glyph("AgentFlows.Folder") {
            poly(3f, 4.6f, 10f, 4.6f, 11.8f, 7.2f, 21f, 7.2f, 21f, 19.4f, 3f, 19.4f)
        }
    }

    val Cpu: ImageVector by lazy {
        glyph("AgentFlows.Cpu") {
            frame(6f, 6f, 12f, 12f, 1.8f)
            box(9.4f, 9.4f, 5.2f, 5.2f)
            box(10.6f, 2f, 2.8f, 4f)
            box(10.6f, 18f, 2.8f, 4f)
            box(2f, 10.6f, 4f, 2.8f)
            box(18f, 10.6f, 4f, 2.8f)
        }
    }

    val Database: ImageVector by lazy {
        glyph("AgentFlows.Database") {
            box(4f, 5f, 16f, 2f)
            box(4f, 11f, 16f, 2f)
            box(4f, 17f, 16f, 2f)
            box(4f, 5f, 2f, 14f)
            box(18f, 5f, 2f, 14f)
        }
    }

    val Shield: ImageVector by lazy {
        glyph("AgentFlows.Shield") {
            poly(12f, 2f, 21f, 5.5f, 21f, 12f, 12f, 22f, 3f, 12f, 3f, 5.5f)
            hole(12f, 5.4f, 6.4f, 13.2f)
        }
    }

    val Users: ImageVector by lazy {
        glyph("AgentFlows.Users") {
            disc(8.4f, 8.6f, 3.4f)
            poly(2f, 20f, 2f, 17.6f, 5.2f, 15.4f, 11.6f, 15.4f, 14.8f, 17.6f, 14.8f, 20f)
            disc(17f, 9.2f, 2.6f)
            poly(15.4f, 20f, 15.4f, 17.8f, 17.4f, 16.4f, 20.4f, 16.4f, 22f, 17.6f, 22f, 20f)
        }
    }

    val Activity: ImageVector by lazy {
        glyph("AgentFlows.Activity") {
            poly(
                2f, 12.4f, 7f, 12.4f, 9.6f, 5.6f, 13f, 18.6f, 15.4f, 10.8f, 17.6f, 12.4f, 22f, 12.4f,
                22f, 14.4f, 16.8f, 14.4f, 15.6f, 13.4f, 12.6f, 21.4f, 9f, 8.2f, 8f, 14.4f, 2f, 14.4f,
            )
        }
    }

    val Grid: ImageVector by lazy {
        glyph("AgentFlows.Grid") {
            box(4f, 4f, 7f, 7f)
            box(13f, 4f, 7f, 7f)
            box(4f, 13f, 7f, 7f)
            box(13f, 13f, 7f, 7f)
        }
    }

    val ListIcon: ImageVector by lazy {
        glyph("AgentFlows.ListIcon") {
            box(3f, 5f, 13f, 2f)
            box(3f, 11f, 13f, 2f)
            box(3f, 17f, 13f, 2f)
            disc(20f, 6f, 1.6f)
            disc(20f, 12f, 1.6f)
            disc(20f, 18f, 1.6f)
        }
    }

    val Sidebar: ImageVector by lazy {
        glyph("AgentFlows.Sidebar") {
            frame(3f, 4f, 18f, 16f, 1.8f)
            box(9.2f, 4f, 1.8f, 16f)
        }
    }

    val Expand: ImageVector by lazy {
        glyph("AgentFlows.Expand") {
            poly(4f, 9f, 4f, 4f, 9f, 4f, 9f, 6f, 6f, 6f, 6f, 9f)
            poly(20f, 15f, 20f, 20f, 15f, 20f, 15f, 18f, 18f, 18f, 18f, 15f)
        }
    }

    val ArrowUp: ImageVector by lazy {
        glyph("AgentFlows.ArrowUp") {
            poly(12f, 3f, 19f, 11f, 17.4f, 12.4f, 13f, 7.6f, 13f, 21f, 11f, 21f, 11f, 7.6f, 6.6f, 12.4f, 5f, 11f)
        }
    }

    val Filter: ImageVector by lazy {
        glyph("AgentFlows.Filter") {
            poly(3f, 5f, 21f, 5f, 14f, 13f, 14f, 20f, 10f, 22f, 10f, 13f)
        }
    }

    val History: ImageVector by lazy {
        glyph("AgentFlows.History") {
            band(12f, 12f, 8.4f, 6.4f, -40f, 290f)
            poly(13.4f, 2.4f, 19f, 3.8f, 15.4f, 9.4f)
            box(11.2f, 8.4f, 1.6f, 5.2f)
            box(11.2f, 12f, 4.6f, 1.6f)
        }
    }

    val Star: ImageVector by lazy {
        glyph("AgentFlows.Star") {
            poly(12f, 2.5f, 14.8f, 9.2f, 22f, 9.8f, 16.5f, 14.6f, 18.3f, 21.6f, 12f, 17.8f, 5.7f, 21.6f, 7.5f, 14.6f, 2f, 9.8f, 9.2f, 9.2f)
        }
    }

    val Key: ImageVector by lazy {
        glyph("AgentFlows.Key") {
            band(8f, 12f, 4.8f, 2.8f)
            box(12.4f, 11.2f, 8.8f, 1.8f)
            box(16.4f, 13f, 1.8f, 3f)
            box(19.4f, 13f, 1.8f, 2.2f)
        }
    }

    val Globe: ImageVector by lazy {
        glyph("AgentFlows.Globe") {
            band(12f, 12f, 9.4f, 7.4f)
            box(11.2f, 2.6f, 1.6f, 18.8f)
            box(3f, 11.2f, 18f, 1.6f)
        }
    }

    val Dot: ImageVector by lazy {
        glyph("AgentFlows.Dot") {
            disc(12f, 12f, 4f)
        }
    }
}
