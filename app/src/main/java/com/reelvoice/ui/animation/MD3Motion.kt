package com.reelvoice.ui.animation

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

// Material Design 3 Motion System
// Emphasized: 500ms - Important transitions
// Standard: 300ms - Common transitions
// Expressive: 400ms - Secondary transitions

object MD3Motion {
    // Durations (Material Design 3)
    const val EmphasizedDurationMs = 500
    const val StandardDurationMs = 300
    const val ExpressiveDurationMs = 400
    
    // Easing curves
    val EmphasizedEasing = EaseInOutCubic
    val StandardEasing = FastOutSlowInEasing
    val ExpressiveEasing = FastOutLinearInEasing
    
    // Tween configurations
    val EmphasizedTween = tween<Float>(
        durationMillis = EmphasizedDurationMs,
        easing = EmphasizedEasing
    )
    
    val StandardTween = tween<Float>(
        durationMillis = StandardDurationMs,
        easing = StandardEasing
    )
    
    val ExpressiveTween = tween<Float>(
        durationMillis = ExpressiveDurationMs,
        easing = ExpressiveEasing
    )
}

// Slide In transitions
fun slideInFromBottom() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(
        durationMillis = MD3Motion.EmphasizedDurationMs,
        easing = MD3Motion.EmphasizedEasing
    )
)

fun slideInFromTop() = slideInVertically(
    initialOffsetY = { -it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun slideInFromStart() = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun slideInFromEnd() = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

// Slide Out transitions
fun slideOutToBottom() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun slideOutToTop() = slideOutVertically(
    targetOffsetY = { -it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun slideOutToStart() = slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun slideOutToEnd() = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

// Fade transitions
fun fadeInTransition() = fadeIn(
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

fun fadeOutTransition() = fadeOut(
    animationSpec = tween(
        durationMillis = MD3Motion.StandardDurationMs,
        easing = MD3Motion.StandardEasing
    )
)

// Combined transitions
fun slideInBottomWithFade() = slideInFromBottom() + fadeInTransition()
fun slideOutBottomWithFade() = slideOutToBottom() + fadeOutTransition()
