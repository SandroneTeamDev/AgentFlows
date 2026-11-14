package sandrone.agentflows.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun glyph(name: String, builder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) { builder() }
        .build()

object AgentIcons {

    val Chat: ImageVector by lazy {
        glyph("AgentFlows.Chat") {
            moveTo(4f, 3f)
            lineTo(20f, 3f)
            lineTo(20f, 15f)
            lineTo(10.5f, 15f)
            lineTo(5f, 20f)
            lineTo(5f, 15f)
            lineTo(4f, 15f)
            close()
        }
    }

    val Bolt: ImageVector by lazy {
        glyph("AgentFlows.Bolt") {
            moveTo(13f, 2f)
            lineTo(4.5f, 13.5f)
            lineTo(11f, 13.5f)
            lineTo(10f, 22f)
            lineTo(18.5f, 10.5f)
            lineTo(12f, 10.5f)
            close()
        }
    }

    val Chart: ImageVector by lazy {
        glyph("AgentFlows.Chart") {
            moveTo(4f, 20f)
            lineTo(7f, 20f)
            lineTo(7f, 11f)
            lineTo(4f, 11f)
            close()
            moveTo(10.5f, 20f)
            lineTo(13.5f, 20f)
            lineTo(13.5f, 4f)
            lineTo(10.5f, 4f)
            close()
            moveTo(17f, 20f)
            lineTo(20f, 20f)
            lineTo(20f, 14f)
            lineTo(17f, 14f)
            close()
        }
    }

    val Settings: ImageVector by lazy {
        glyph("AgentFlows.Settings") {
            moveTo(3f, 5f)
            lineTo(14f, 5f)
            lineTo(14f, 7f)
            lineTo(3f, 7f)
            close()
            moveTo(17f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 8f)
            lineTo(17f, 8f)
            close()
            moveTo(10f, 11f)
            lineTo(21f, 11f)
            lineTo(21f, 13f)
            lineTo(10f, 13f)
            close()
            moveTo(4f, 10f)
            lineTo(7f, 10f)
            lineTo(7f, 14f)
            lineTo(4f, 14f)
            close()
            moveTo(3f, 17f)
            lineTo(12f, 17f)
            lineTo(12f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(15f, 16f)
            lineTo(18f, 16f)
            lineTo(18f, 20f)
            lineTo(15f, 20f)
            close()
        }
    }

    val Send: ImageVector by lazy {
        glyph("AgentFlows.Send") {
            moveTo(3f, 20f)
            lineTo(21f, 12f)
            lineTo(3f, 4f)
            lineTo(6.2f, 12f)
            close()
        }
    }

    val Stop: ImageVector by lazy {
        glyph("AgentFlows.Stop") {
            moveTo(7f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 17f)
            lineTo(7f, 17f)
            close()
        }
    }

    val ChevronDown: ImageVector by lazy {
        glyph("AgentFlows.ChevronDown") {
            moveTo(5.8f, 9.4f)
            lineTo(12f, 15.6f)
            lineTo(18.2f, 9.4f)
            lineTo(19.6f, 10.8f)
            lineTo(12f, 18.4f)
            lineTo(4.4f, 10.8f)
            close()
        }
    }

    val ChevronUp: ImageVector by lazy {
        glyph("AgentFlows.ChevronUp") {
            moveTo(5.8f, 14.6f)
            lineTo(12f, 8.4f)
            lineTo(18.2f, 14.6f)
            lineTo(19.6f, 13.2f)
            lineTo(12f, 5.6f)
            lineTo(4.4f, 13.2f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        glyph("AgentFlows.Check") {
            moveTo(9f, 16.2f)
            lineTo(4.8f, 12f)
            lineTo(3.4f, 13.4f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.6f, 5.6f)
            close()
        }
    }

    val Close: ImageVector by lazy {
        glyph("AgentFlows.Close") {
            moveTo(6.4f, 5f)
            lineTo(12f, 10.6f)
            lineTo(17.6f, 5f)
            lineTo(19f, 6.4f)
            lineTo(13.4f, 12f)
            lineTo(19f, 17.6f)
            lineTo(17.6f, 19f)
            lineTo(12f, 13.4f)
            lineTo(6.4f, 19f)
            lineTo(5f, 17.6f)
            lineTo(10.6f, 12f)
            lineTo(5f, 6.4f)
            close()
        }
    }

    val Plus: ImageVector by lazy {
        glyph("AgentFlows.Plus") {
            moveTo(11f, 4f)
            lineTo(13f, 4f)
            lineTo(13f, 11f)
            lineTo(20f, 11f)
            lineTo(20f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 20f)
            lineTo(11f, 20f)
            lineTo(11f, 13f)
            lineTo(4f, 13f)
            lineTo(4f, 11f)
            lineTo(11f, 11f)
            close()
        }
    }

    val Wave: ImageVector by lazy {
        glyph("AgentFlows.Wave") {
            moveTo(2f, 12f)
            lineTo(5f, 12f)
            lineTo(7f, 6f)
            lineTo(10f, 18f)
            lineTo(13f, 9f)
            lineTo(15.5f, 15f)
            lineTo(17.5f, 12f)
            lineTo(22f, 12f)
            lineTo(22f, 14f)
            lineTo(18.5f, 14f)
            lineTo(16f, 18f)
            lineTo(13.5f, 12.5f)
            lineTo(10.5f, 21f)
            lineTo(7.5f, 9.5f)
            lineTo(6.5f, 14f)
            lineTo(2f, 14f)
            close()
        }
    }
}
