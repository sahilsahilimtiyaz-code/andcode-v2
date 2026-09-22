package com.yugahashimoto.andcode.feature.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.AndCodeTheme
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel

/**
 * Creator credits card displayed at the top of the Settings screen.
 *
 * Features a glass-morphism surface with a subtle shimmer sweep animation
 * on the creator name. Uses the premium neon color palette.
 */
@Composable
fun CreatorCreditsCard(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "creditsShimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = PremiumTokens.ShimmerSweep,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(
                shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
                borderColor = PremiumColorValues.GlassBorder,
            )
            .padding(20.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = PremiumColorValues.ElectricPurple,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Crafted with",
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumColorValues.ForegroundMuted,
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    PremiumColorValues.NeonBlue,
                                    PremiumColorValues.ElectricPurple,
                                    PremiumColorValues.NeonBlue,
                                ),
                                start = Offset(x = shimmerX * 300f, y = 0f),
                                end = Offset(x = shimmerX * 300f + 300f, y = 100f),
                                tileMode = TileMode.Clamp,
                            ),
                        ),
                    ) {
                        append("Sahil (Octavian)")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ContactRow(
                icon = Icons.Default.Code,
                label = "Email",
                value = "sahilsahilimtiyaz@gmail.com",
            )
            ContactRow(
                icon = Icons.Default.PlayArrow,
                label = "TikTok",
                value = "@mog-octavian",
            )
            ContactRow(
                icon = Icons.Default.Code,
                label = "YouTube",
                value = "CHADFRAMED",
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PremiumColorValues.NeonBlue.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumColorValues.ForegroundPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatorCreditsCardPreview() {
    AndCodeTheme {
        Box(
            modifier = Modifier
                .background(PremiumColorValues.SpaceBlack)
                .padding(16.dp),
        ) {
            CreatorCreditsCard()
        }
    }
}
