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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Mission(
    val id: String,
    val name: String,
    val description: String,
    val status: MissionStatus,
    val progress: Float,
    val currentStep: String,
    val totalSteps: Int,
    val completedSteps: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var missions by remember {
        mutableStateOf(
            listOf(
                Mission(
                    id = "1",
                    name = "Code Analysis",
                    description = "Analyzing codebase for improvements",
                    status = MissionStatus.RUNNING,
                    progress = 0.65f,
                    currentStep = "Scanning dependencies...",
                    totalSteps = 10,
                    completedSteps = 6
                ),
                Mission(
                    id = "2",
                    name = "Build Optimization",
                    description = "Optimizing build configuration",
                    status = MissionStatus.COMPLETED,
                    progress = 1f,
                    currentStep = "Completed",
                    totalSteps = 5,
                    completedSteps = 5
                ),
                Mission(
                    id = "3",
                    name = "Test Suite",
                    description = "Running automated tests",
                    status = MissionStatus.IDLE,
                    progress = 0f,
                    currentStep = "Waiting to start",
                    totalSteps = 8,
                    completedSteps = 0
                )
            )
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Missions",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Create new mission */ },
                containerColor = Color(0xFF6366F1)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Mission",
                    tint = Color.White
                )
            }
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
            
            items(missions) { mission ->
                MissionProgressCard(
                    missionName = mission.name,
                    status = mission.status,
                    progress = mission.progress,
                    currentStep = mission.currentStep,
                    totalSteps = mission.totalSteps,
                    completedSteps = mission.completedSteps
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
