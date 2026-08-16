package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.AssistantState

@Composable
fun JarvisOverlay(
    state: AssistantState = AssistantState.LISTENING,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "overlay_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlay_scale"
    )

    val color = when (state) {
        AssistantState.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        AssistantState.LISTENING -> MaterialTheme.colorScheme.tertiary
        AssistantState.UNDERSTANDING -> MaterialTheme.colorScheme.secondary
        AssistantState.SPEAKING -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(if (state != AssistantState.IDLE) scale else 1f)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        )
    }
}
