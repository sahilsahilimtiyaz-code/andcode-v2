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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMonitorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cpuUsage by remember { mutableFloatStateOf(0.45f) }
    var memoryUsage by remember { mutableFloatStateOf(0.62f) }
    var storageUsage by remember { mutableFloatStateOf(0.38f) }
    var temperature by remember { mutableFloatStateOf(32f) }
    
    // Simulate real-time updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            cpuUsage = (0.3f + Math.random() * 0.4f).toFloat()
            memoryUsage = (0.5f + Math.random() * 0.3f).toFloat()
            storageUsage = (0.35f + Math.random() * 0.1f).toFloat()
            temperature = (30f + Math.random() * 8f).toFloat()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Performance Monitor",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            item {
                // CPU Usage
                PerformanceCard(
                    icon = Icons.Default.Speed,
                    title = "CPU Usage",
                    value = "${(cpuUsage * 100).toInt()}%",
                    progress = cpuUsage,
                    color = when {
                        cpuUsage > 0.8f -> Color(0xFFEF4444)
                        cpuUsage > 0.6f -> Color(0xFFF59E0B)
                        else -> Color(0xFF22C55E)
                    }
                )
            }
            
            item {
                // Memory Usage
                PerformanceCard(
                    icon = Icons.Default.Memory,
                    title = "Memory Usage",
                    value = "${(memoryUsage * 100).toInt()}%",
                    progress = memoryUsage,
                    color = when {
                        memoryUsage > 0.8f -> Color(0xFFEF4444)
                        memoryUsage > 0.6f -> Color(0xFFF59E0B)
                        else -> Color(0xFF22C55E)
                    }
                )
            }
            
            item {
                // Storage Usage
                PerformanceCard(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    value = "${(storageUsage * 100).toInt()}%",
                    progress = storageUsage,
                    color = when {
                        storageUsage > 0.9f -> Color(0xFFEF4444)
                        storageUsage > 0.7f -> Color(0xFFF59E0B)
                        else -> Color(0xFF22C55E)
                    }
                )
            }
            
            item {
                // Temperature
                PerformanceCard(
                    icon = Icons.Default.Thermostat,
                    title = "Temperature",
                    value = "${temperature.toInt()}°C",
                    progress = temperature / 50f,
                    color = when {
                        temperature > 45f -> Color(0xFFEF4444)
                        temperature > 38f -> Color(0xFFF59E0B)
                        else -> Color(0xFF22C55E)
                    }
                )
            }
            
            item {
                // System Info
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "System Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SystemInfoRow("Runtime", "Local PRoot")
                        SystemInfoRow("Agent", "OpenCode")
                        SystemInfoRow("Version", "v2.0.0")
                        SystemInfoRow("Uptime", "2h 34m")
                        SystemInfoRow("Sessions", "12 active")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    progress: Float,
    color: Color
) {
    GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.height(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(6.dp)
                            .fillMaxWidth(0.6f),
                        color = color,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
