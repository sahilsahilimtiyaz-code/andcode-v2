package com.yugahashimoto.andcode.feature.premium

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class StatusType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    LOADING
}

@Composable
fun StatusCard(
    title: String,
    status: StatusType,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    GlassMorphismSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIcon(status = status)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: StatusType) {
    val icon: ImageVector = when (status) {
        StatusType.SUCCESS -> Icons.Default.CheckCircle
        StatusType.ERROR -> Icons.Default.Error
        StatusType.WARNING -> Icons.Default.Warning
        StatusType.INFO -> Icons.Default.Info
        StatusType.LOADING -> Icons.Default.Refresh
    }
    
    val tint: Color = when (status) {
        StatusType.SUCCESS -> Color(0xFF22C55E)
        StatusType.ERROR -> Color(0xFFEF4444)
        StatusType.WARNING -> Color(0xFFF59E0B)
        StatusType.INFO -> Color(0xFF3B82F6)
        StatusType.LOADING -> Color.White.copy(alpha = 0.7f)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun BuildStatusCard(
    buildName: String,
    isRunning: Boolean,
    success: Boolean? = null,
    message: String = "",
    modifier: Modifier = Modifier
) {
    val status = when {
        isRunning -> StatusType.LOADING
        success == true -> StatusType.SUCCESS
        success == false -> StatusType.ERROR
        else -> StatusType.INFO
    }
    
    val subtitle = when {
        isRunning -> "Building..."
        success == true -> "Build successful"
        success == false -> "Build failed"
        message.isNotBlank() -> message
        else -> "Ready"
    }
    
    StatusCard(
        title = buildName,
        status = status,
        subtitle = subtitle,
        modifier = modifier
    )
}
