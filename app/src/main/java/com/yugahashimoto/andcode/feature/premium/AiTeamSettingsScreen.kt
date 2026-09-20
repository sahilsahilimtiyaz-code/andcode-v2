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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.feature.premium.engines.AgentRole
import com.yugahashimoto.andcode.feature.premium.engines.MissionEngine
import com.yugahashimoto.andcode.feature.premium.engines.MissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTeamSettingsScreen(
    missionState: MissionState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autonomyLevel by remember { mutableStateOf(3) }
    var budgetLimit by remember { mutableStateOf(100) }
    var agentRoles by remember { mutableStateOf(
        AgentRole.entries.associateWith { true }
    ) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Team Settings", fontWeight = FontWeight.SemiBold) },
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
                        Text("Autonomy Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Level $autonomyLevel of 5", color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Slider(
                            value = autonomyLevel.toFloat(),
                            onValueChange = { autonomyLevel = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = Color(0xFF00D4FF),
                                activeTrackColor = Color(0xFF00D4FF)
                            )
                        )
                    }
                }
            }

            item {
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Budget Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("\$$budgetLimit per mission", color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Slider(
                            value = budgetLimit.toFloat(),
                            onValueChange = { budgetLimit = it.toInt() },
                            valueRange = 10f..500f,
                            steps = 9,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = Color(0xFFB84CFF),
                                activeTrackColor = Color(0xFFB84CFF)
                            )
                        )
                    }
                }
            }

            item {
                GlassMorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Agent Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        AgentRole.entries.forEach { role ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${role.icon} ${role.displayName}", color = Color.White)
                                }
                                Switch(
                                    checked = agentRoles[role] ?: true,
                                    onCheckedChange = { enabled ->
                                        agentRoles = agentRoles + (role to enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00D4FF),
                                        checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
