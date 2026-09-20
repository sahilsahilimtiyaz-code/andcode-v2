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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.feature.premium.engines.ModelProvider
import com.yugahashimoto.andcode.feature.premium.engines.ModelRouterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelRouterScreen(
    routerState: ModelRouterState,
    onSelectProvider: (String) -> Unit,
    onSelectStrategy: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Model Router", fontWeight = FontWeight.SemiBold) },
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

            item {
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Provider Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        routerState.providers.forEach { provider ->
                            ProviderRow(
                                provider = provider,
                                isSelected = provider.id == routerState.currentProviderId,
                                onClick = { onSelectProvider(provider.id) }
                            )
                        }
                    }
                }
            }

            item {
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Fallback Strategy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        routerState.strategies.forEach { strategy ->
                            StrategyRow(
                                name = strategy.name,
                                providers = strategy.providers.joinToString(" → "),
                                isActive = strategy.id == routerState.activeStrategyId,
                                onClick = { onSelectStrategy(strategy.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ModelProvider,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(provider.name, color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                "${provider.latencyMs}ms • ${if (provider.isAvailable) "Healthy" else "Down"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (provider.isAvailable) Color(0xFF22C55E) else Color(0xFFEF4444)
            )
        }
        if (isSelected) {
            Text("Active", color = Color(0xFF00D4FF), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StrategyRow(
    name: String,
    providers: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Color.White, fontWeight = FontWeight.Medium)
            Text(providers, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        if (isActive) {
            Text("Active", color = Color(0xFF00D4FF), fontWeight = FontWeight.Bold)
        }
    }
}
