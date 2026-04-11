package com.bolsaaf.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.math.sin

/**
 * Smooth slide-in animation from bottom
 */
@Composable
fun AnimatedSlideIn(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(600, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(600)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(400))
    ) {
        content()
    }
}

/**
 * Smooth fade and scale animation
 */
@Composable
fun AnimatedFadeScale(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(500, easing = EaseOutCubic)
        ),
        exit = fadeOut(animationSpec = tween(400)) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(400)
        )
    ) {
        content()
    }
}

/**
 * Floating animation (up and down)
 */
@Composable
fun FloatingAnimation(
    modifier: Modifier = Modifier,
    durationMs: Int = 3000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    Box(
        modifier = modifier.graphicsLayer(translationY = offsetY)
    ) {
        content()
    }
}

/**
 * Pulsing glow animation
 */
@Composable
fun PulsingGlow(
    modifier: Modifier = Modifier,
    color: Color,
    durationMs: Int = 1500
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .background(color.copy(alpha = 0.3f), CircleShape)
    )
}

/**
 * Bounce animation
 */
@Composable
fun BounceAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                0f at 0
                -10f at 250
                0f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce_offset"
    )

    Box(
        modifier = modifier.graphicsLayer(translationY = offsetY)
    ) {
        content()
    }
}

/**
 * Staggered list animation
 */
@Composable
fun AnimatedListItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { 100 },
            animationSpec = tween(
                durationMillis = 600,
                delayMillis = index * 100,
                easing = EaseOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 600,
                delayMillis = index * 100
            )
        ),
        modifier = modifier
    ) {
        Box {
            content()
        }
    }
}
