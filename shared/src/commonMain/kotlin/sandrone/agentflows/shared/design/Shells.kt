package sandrone.agentflows.shared.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sandrone.agentflows.shared.ui.AgentIcons

data class ShellNavItem(
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
)

@Composable
fun DesktopShell(
    navItems: List<ShellNavItem>,
    selectedNav: Int,
    onNavSelect: (Int) -> Unit,
    headerTitle: String,
    headerSubtitle: String? = null,
    headerChips: @Composable RowScope.() -> Unit = {},
    headerActions: @Composable RowScope.() -> Unit = {},
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTabSelect: (Int) -> Unit = {},
    tabCounts: List<Int>? = null,
    sidebarTop: @Composable ColumnScope.() -> Unit = {},
    sidebarBottom: @Composable ColumnScope.() -> Unit = {},
    statusBar: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxSize().background(scheme.background)) {
        Column(
            Modifier
                .width(metrics().sidebarWidth)
                .fillMaxHeight()
                .background(scheme.surfaceContainerLow),
        ) {
            BrandRow()
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sidebarTop()
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel("Workspace", Modifier.padding(start = 18.dp, bottom = 4.dp))
            Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                navItems.forEachIndexed { index, item ->
                    SidebarNavItem(
                        item = item,
                        selected = index == selectedNav,
                        onClick = { onNavSelect(index) },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sidebarBottom()
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outlineVariant.copy(alpha = 0.6f)))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        headerTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = metrics().titleSize,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    if (headerSubtitle != null) {
                        CaptionText(headerSubtitle, Modifier.padding(top = 1.dp), maxLines = 1)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = headerChips,
                )
                Spacer(Modifier.width(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = headerActions,
                )
            }
            if (tabs.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    UnderlineTabs(tabs = tabs, selected = selectedTab, onSelect = onTabSelect, counts = tabCounts)
                }
            }
            ThinRule(alpha = 0.7f)
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            ThinRule(alpha = 0.7f)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = statusBar,
            )
        }
    }
}

@Composable
private fun BrandRow() {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(Radius.xs),
            modifier = Modifier.size(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AgentIcons.Wave, null, Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "AgentFlows",
            maxLines = 1,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .clip(RoundedCornerShape(Radius.xs))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f), RoundedCornerShape(Radius.xs))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            MonoText("alpha3", size = 9.sp)
        }
    }
}

@Composable
fun SidebarNavItem(item: ShellNavItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.xs)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            item.icon,
            null,
            Modifier.size(14.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            item.label,
            Modifier.weight(1f),
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
        )
        if (item.badge != null) {
            MonoText(item.badge, size = 9.5.sp)
        }
    }
}

@Composable
fun SidebarPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.xs)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
fun MobileShell(
    navItems: List<ShellNavItem>,
    selectedNav: Int,
    onNavSelect: (Int) -> Unit,
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    showNavigation: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(11.dp))
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
                    CaptionText(subtitle, Modifier.padding(top = 1.dp), maxLines = 1)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (showNavigation) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = index == selectedNav,
                        onClick = { onNavSelect(index) },
                        icon = { Icon(item.icon, null, Modifier.size(21.dp)) },
                        label = {
                            Text(
                                item.label,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBarItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        )
        Spacer(Modifier.width(4.dp))
        MonoText(value, size = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StatusBarSeparator() {
    Box(Modifier.size(2.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
}

@Composable
fun PrimaryActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xs))
            .background(if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}
