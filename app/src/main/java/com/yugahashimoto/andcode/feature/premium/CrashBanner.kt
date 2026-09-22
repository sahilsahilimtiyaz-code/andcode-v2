package com.yugahashimoto.andcode.feature.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.core.reliability.RecoveryManager
import com.yugahashimoto.andcode.core.reliability.RecoveryResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CrashBanner(
    recoveryManager: RecoveryManager?,
    modifier: Modifier = Modifier
) {
    if (recoveryManager == null) return

    val recoveryState by recoveryManager.recoveryState.collectAsState()
    var visibleFailedMissions by remember { mutableStateOf<Map<String, RecoveryResult>>(emptyMap()) }

    // Collect failed missions from recovery state
    LaunchedEffect(recoveryState) {
        val failed = recoveryState.filterValues { result ->
            result is RecoveryResult.Failed
        }
        visibleFailedMissions = failed
    }

    // Auto-dismiss after 10 seconds
    LaunchedEffect(visibleFailedMissions) {
        if (visibleFailedMissions.isNotEmpty()) {
            delay(10_000)
            visibleFailedMissions = emptyMap()
        }
    }

    AnimatedVisibility(
        visible = visibleFailedMissions.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEF4444).copy(alpha = 0.9f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.White
                )
                Text(
                    "Mission Recovery Available",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            visibleFailedMissions.forEach { (missionId, result) ->
                val reason = when (result) {
                    is RecoveryResult.Failed -> result.reason
                    else -> "Unknown error"
                }
                Text(
                    "Mission ${missionId.takeLast(8)}: $reason",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Attempt recovery for all failed missions
                        visibleFailedMissions.keys.forEach { missionId ->
                            kotlinx.coroutines.MainScope().launch {
                                recoveryManager.attemptRecovery(missionId)
                            }
                        }
                        visibleFailedMissions = emptyMap()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Recover All", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { visibleFailedMissions = emptyMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("Dismiss", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
