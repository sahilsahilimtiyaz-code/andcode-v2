package com.yugahashimoto.andcode.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.feature.premium.engines.Plugin
import com.yugahashimoto.andcode.feature.premium.engines.PluginSandboxState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    pluginState: PluginSandboxState,
    onTogglePlugin: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Plugins", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(pluginState.plugins) { plugin ->
                PluginCard(
                    plugin = plugin,
                    onToggle = { onTogglePlugin(plugin.id) }
                )
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: Plugin,
    onToggle: () -> Unit
) {
    GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = plugin.description,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "v${plugin.version} • ${plugin.author}",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00D4FF),
                    checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.3f)
                )
            )
        }
    }
}
