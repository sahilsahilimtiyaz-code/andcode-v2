package com.yugahashimoto.andcode.feature.premium

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MissionStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED
}

@Composable
fun MissionProgressCard(
    missionName: String,
    status: MissionStatus,
    progress: Float = 0f,
    currentStep: String = "",
    totalSteps: Int = 0,
    completedSteps: Int = 0,
    modifier: Modifier = Modifier
) {
    GlassMorphismCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = missionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (currentStep.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                MissionStatusIcon(status = status)
            }
            
            if (totalSteps > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 500),
                    label = "progress"
                )
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when (status) {
                        MissionStatus.COMPLETED -> Color(0xFF22C55E)
                        MissionStatus.FAILED -> Color(0xFFEF4444)
                        else -> Color(0xFF6366F1)
                    },
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Step $completedSteps of $totalSteps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionStatusIcon(status: MissionStatus) {
    when (status) {
        MissionStatus.IDLE -> {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = "Idle",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        MissionStatus.RUNNING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF6366F1),
                strokeWidth = 2.dp
            )
        }
        MissionStatus.COMPLETED -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(24.dp)
            )
        }
        MissionStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Failed",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
