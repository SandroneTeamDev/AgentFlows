package sandrone.agentflows.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sandrone.agentflows.shared.generated.resources.Res
import sandrone.agentflows.shared.generated.resources.brand_logo
import sandrone.agentflows.shared.generated.resources.brand_logo_small

object AgentBrand {
    val Navy = Color(0xFF020852)
    val Yellow = Color(0xFFF9DB09)
    val CompactThreshold = 26.dp

    val Detailed: DrawableResource get() = Res.drawable.brand_logo
    val Compact: DrawableResource get() = Res.drawable.brand_logo_small
}

@Composable
fun BrandTile(
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRatio: Float = 0.3f,
) {
    val asset = if (size < AgentBrand.CompactThreshold) AgentBrand.Compact else AgentBrand.Detailed
    Image(
        painter = painterResource(asset),
        contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * cornerRatio)),
        contentScale = ContentScale.Crop,
    )
}
