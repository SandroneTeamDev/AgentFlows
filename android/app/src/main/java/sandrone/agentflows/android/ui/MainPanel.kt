package sandrone.agentflows.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sandrone.agentflows.android.theme.AgentFlowsTheme

@Composable
fun MainPanel(modifier: Modifier = Modifier) {
    AgentFlowsTheme(colorScheme = null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "AgentFlows") },
                    colors = androidx.compose.material3.TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    ),
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(paddingValues),
                contentPadding = paddingValues,
            ) {
                items(5) { index ->
                    Text(
                        text = "Panel ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(width = Size.Unspecified, height = 48.dp)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
