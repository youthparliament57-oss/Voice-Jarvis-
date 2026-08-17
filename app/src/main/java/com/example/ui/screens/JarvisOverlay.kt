package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AssistantState
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText

@Composable
fun JarvisOverlay(
    state: AssistantState = AssistantState.LISTENING,
    errorMessage: String? = null,
    onDismiss: () -> Unit = {}
) {
    if (state == AssistantState.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "creature_glow")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.SPEAKING) 250 else 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val auraColor = when (state) {
        AssistantState.LISTENING -> PureWhite
        AssistantState.UNDERSTANDING -> Color(0xFFD4D4D8)
        AssistantState.SPEAKING -> PureWhite
        AssistantState.ERROR -> Color(0xFFEF4444)
        else -> PureWhite
    }

    val statusText = when (state) {
        AssistantState.LISTENING -> "JARVIS Listening..."
        AssistantState.UNDERSTANDING -> "Processing request..."
        AssistantState.SPEAKING -> "JARVIS Speaking..."
        AssistantState.ERROR -> errorMessage ?: "Voice Connection Error"
        else -> "JARVIS Active"
    }

    Surface(
        modifier = Modifier
            .wrapContentSize()
            .padding(12.dp),
        shape = RoundedCornerShape(32.dp),
        color = OledBlack.copy(alpha = 0.94f),
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, auraColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Alien Creature 👽 High-Contrast Glow Orb Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .drawBehind {
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(auraColor.copy(alpha = 0.45f), Color.Transparent),
                                center = center,
                                radius = (size.width / 2) * pulseAnim
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(auraColor, auraColor.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state == AssistantState.ERROR) Icons.Default.Warning else Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = OledBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Live Assistant Status Message
            Column(
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "J.A.R.V.I.S",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = SilverText,
                    letterSpacing = 2.sp
                )
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Close / Dismiss Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close JARVIS",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
