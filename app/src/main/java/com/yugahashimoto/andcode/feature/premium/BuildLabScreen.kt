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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BuildConfig(
    val id: String,
    val name: String,
    val type: String,
    val isRunning: Boolean,
    val success: Boolean? = null,
    val duration: String = "",
    val logs: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var builds by remember {
        mutableStateOf(
            listOf(
                BuildConfig(
                    id = "1",
                    name = "Debug Build",
                    type = "debug",
                    isRunning = true,
                    duration = "2:34"
                ),
                BuildConfig(
                    id = "2",
                    name = "Release Build",
                    type = "release",
                    isRunning = false,
                    success = true,
                    duration = "5:12"
                ),
                BuildConfig(
                    id = "3",
                    name = "Profile Build",
                    type = "profile",
                    isRunning = false,
                    success = false,
                    duration = "1:45"
                )
            )
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Build Lab",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            item {
                // Build actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BuildActionButton(
                        icon = Icons.Default.Build,
                        label = "Build All",
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                    BuildActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Clean",
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                    BuildActionButton(
                        icon = Icons.Default.Speed,
                        label = "Profile",
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            items(builds) { build ->
                BuildStatusCard(
                    buildName = build.name,
                    isRunning = build.isRunning,
                    success = build.success,
                    message = if (build.duration.isNotBlank()) "Duration: ${build.duration}" else ""
                )
            }
            
            item {
                // Build configuration
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Build Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BuildConfigRow("Min SDK", "26")
                        BuildConfigRow("Target SDK", "34")
                        BuildConfigRow("Compile SDK", "34")
                        BuildConfigRow("Kotlin", "1.9.22")
                        BuildConfigRow("Compose", "1.6.2")
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
private fun BuildActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6366F1).copy(alpha = 0.3f)
        )
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.height(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun BuildConfigRow(
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
