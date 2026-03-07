package org.example.project.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

// ─── Animation Constants ──────────────────────────────────────────────────────

object AnimationDurations {
    const val Fast = 300
    const val Medium = 500
    const val Slow = 700
    const val ExtraSlow = 1000
}

object AnimationDelays {
    const val None = 0L
    const val Short = 100L
    const val Medium = 150L
    const val Long = 200L
}

// ─── Reusable Animation Specs ─────────────────────────────────────────────────

object AnimationSpecs {
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val smooth = tween<Float>(
        durationMillis = AnimationDurations.Medium,
        easing = FastOutSlowInEasing
    )
    
    val quick = tween<Float>(
        durationMillis = AnimationDurations.Fast,
        easing = FastOutSlowInEasing
    )
    
    val gentle = tween<Float>(
        durationMillis = AnimationDurations.Slow,
        easing = LinearOutSlowInEasing
    )
}

// ─── Entrance Animations ──────────────────────────────────────────────────────

/**
 * Standard fade in animation
 */
fun fadeInAnimation(duration: Int = AnimationDurations.Medium): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing))
}

/**
 * Slide in from top with fade
 */
fun slideInFromTopAnimation(
    duration: Int = AnimationDurations.Medium,
    offset: Int = -40
): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                initialOffsetY = { offset }
            )
}

/**
 * Slide in from bottom with fade
 */
fun slideInFromBottomAnimation(
    duration: Int = AnimationDurations.Medium,
    offsetFraction: Float = 0.5f
): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                initialOffsetY = { (it * offsetFraction).toInt() }
            )
}

/**
 * Scale in with fade (zoom effect)
 */
fun scaleInAnimation(
    duration: Int = AnimationDurations.Medium,
    initialScale: Float = 0.8f
): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
            scaleIn(
                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                initialScale = initialScale
            )
}

/**
 * Slide in horizontally from left
 */
fun slideInFromLeftAnimation(duration: Int = AnimationDurations.Medium): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
            slideInHorizontally(
                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                initialOffsetX = { -it / 2 }
            )
}

/**
 * Slide in horizontally from right
 */
fun slideInFromRightAnimation(duration: Int = AnimationDurations.Medium): EnterTransition {
    return fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
            slideInHorizontally(
                animationSpec = tween(duration, easing = FastOutSlowInEasing),
                initialOffsetX = { it / 2 }
            )
}

// ─── Staggered Animation Helper ───────────────────────────────────────────────

/**
 * Helper to create staggered animation states
 * Usage: 
 * ```
 * val animStates = rememberStaggeredAnimationStates(itemCount = 3, delayMs = 100L)
 * ```
 */
@Composable
fun rememberStaggeredAnimationStates(
    itemCount: Int,
    delayMs: Long = AnimationDelays.Medium
): List<MutableState<Boolean>> {
    val states = remember { List(itemCount) { mutableStateOf(false) } }
    
    LaunchedEffect(Unit) {
        states.forEachIndexed { index, state ->
            delay(index * delayMs)
            state.value = true
        }
    }
    
    return states
}

// ─── Continuous Animations ────────────────────────────────────────────────────

/**
 * Creates an infinite rotation animation
 */
@Composable
fun rememberInfiniteRotation(durationMillis: Int = 2000): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    ).value
}

/**
 * Creates a pulsing scale animation
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    ).value
}

/**
 * Creates a breathing opacity animation
 */
@Composable
fun rememberBreathingAnimation(
    minAlpha: Float = 0.4f,
    maxAlpha: Float = 1f,
    durationMillis: Int = 2000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    return infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    ).value
}

// ─── Micro-Interaction Modifiers ──────────────────────────────────────────────

/**
 * Adds a subtle hover/press scale effect
 */
fun Modifier.pressableScale(
    isPressed: Boolean,
    pressedScale: Float = 0.96f
): Modifier = this.graphicsLayer {
    val scale = if (isPressed) pressedScale else 1f
    scaleX = scale
    scaleY = scale
}

/**
 * Adds a parallax effect based on scroll offset
 */
fun Modifier.parallaxEffect(
    scrollOffset: Float,
    parallaxFactor: Float = 0.5f
): Modifier = this.graphicsLayer {
    translationY = scrollOffset * parallaxFactor
}

/**
 * Adds a shake animation trigger
 */
@Composable
fun rememberShakeAnimation(trigger: Boolean): Float {
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            for (i in 0..3) {
                shakeOffset.animateTo(
                    targetValue = if (i % 2 == 0) 10f else -10f,
                    animationSpec = tween(50)
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }
    
    return shakeOffset.value
}
