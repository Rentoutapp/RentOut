package org.example.project.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.theme.RentOutColors

/**
 * Reusable progress button with multiple animation variants
 * Based on the pattern from PropertyImagesScreen SubmitForReviewButton
 */
@Composable
fun ProgressButton(
    itemCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    buttonText: String = "Submit",
    loadingText: String = "Processing",
    successText: String = "Complete!",
    variant: ProgressVariant = ProgressVariant.LINEAR,
    modifier: Modifier = Modifier
) {
    // ── States ────────────────────────────────────────────────────────────────
    val enabled = itemCount > 0 && !isLoading

    // Progress state
    var simulatedProgress by remember { mutableStateOf(0f) }
    var isDone by remember { mutableStateOf(false) }

    // ── Progress Logic: Multiple Variants ─────────────────────────────────────
    LaunchedEffect(isLoading) {
        if (isLoading) {
            simulatedProgress = 0f
            isDone = false

            when (variant) {
                ProgressVariant.LINEAR -> {
                    // Original: Linear progression to 92%
                    val steps = 46
                    repeat(steps) { i ->
                        delay(60L)
                        simulatedProgress = (i + 1) / 50f  // Max 0.92
                    }
                }

                ProgressVariant.RANDOM -> {
                    // Random increments with variable delays
                    while (simulatedProgress < 0.92f) {
                        // Random increment: 2% to 8%
                        val increment = (0.02f..0.08f).random()
                        simulatedProgress = (simulatedProgress + increment).coerceAtMost(0.92f)
                        
                        // Random delay: 40ms to 150ms
                        delay((40L..150L).random())
                    }
                }

                ProgressVariant.FAST_BURST -> {
                    // Fast bursts with occasional pauses
                    while (simulatedProgress < 0.92f) {
                        // Large burst: 5% to 12%
                        val burst = (0.05f..0.12f).random()
                        simulatedProgress = (simulatedProgress + burst).coerceAtMost(0.92f)
                        
                        // Quick delay or occasional pause
                        val delayMs = if (Math.random() > 0.7) {
                            (150L..300L).random()  // Occasional thinking pause
                        } else {
                            (20L..60L).random()    // Fast increment
                        }
                        delay(delayMs)
                    }
                }

                ProgressVariant.STEPPED -> {
                    // Milestone-based progression (0% → 25% → 50% → 75% → 92%)
                    val milestones = listOf(0.25f, 0.50f, 0.75f, 0.92f)
                    for (milestone in milestones) {
                        val steps = 8
                        val start = simulatedProgress
                        repeat(steps) { i ->
                            simulatedProgress = start + (milestone - start) * (i + 1) / steps
                            delay((40L..100L).random())
                        }
                        delay((100L..200L).random())  // Pause at milestone
                    }
                }
            }
        } else if (simulatedProgress > 0f) {
            // Backend operation completed — snap to 100%
            simulatedProgress = 1f
            delay(400)
            isDone = true
        }
    }

    // ── Animated Values ───────────────────────────────────────────────────────
    
    // Smooth progress interpolation
    val animatedProgress by animateFloatAsState(
        targetValue = simulatedProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "upload_progress"
    )

    // Button press spring
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "submit_scale"
    )

    // Elevation depth
    val elevation by animateDpAsState(
        targetValue = if (isPressed || isLoading) 2.dp else 10.dp,
        label = "submit_elev"
    )

    // Corner radius morphing: button → pill bar
    val cornerRadius by animateDpAsState(
        targetValue = if (isLoading) 28.dp else 16.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "corner_anim"
    )

    // Shimmer sweep (infinite while loading)
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Checkmark pop scale
    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    // Icon rotation (infinite while loading)
    val iconTransition = rememberInfiniteTransition(label = "icon_spin")
    val spinAngle by iconTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(buttonScale)
            .shadow(
                elevation = if (enabled || isLoading) elevation else 0.dp,
                shape = RoundedCornerShape(cornerRadius)
            )
            .height(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (enabled && !isLoading)
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading || isDone) {
            // ── Loading/Success State ─────────────────────────────────────────
            
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RentOutColors.Primary.copy(alpha = 0.15f))
            )

            // Filled progress portion with gradient
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                RentOutColors.Primary,
                                RentOutColors.PrimaryLight,
                                RentOutColors.Primary
                            )
                        )
                    )
            )

            // Shimmer overlay (hidden when done)
            if (!isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset * 400f,
                                endX = shimmerOffset * 400f + 300f
                            )
                        )
                )
            }

            // Content: percentage counter → checkmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isDone) {
                    // ✓ Success: Checkmark pop
                    Box(
                        modifier = Modifier
                            .scale(checkScale)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = successText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                } else {
                    // ⟳ Loading: Spinning icon + percentage
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = spinAngle }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$loadingText… ${(animatedProgress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }

        } else {
            // ── Normal Button State ───────────────────────────────────────────
            val bgColor by animateColorAsState(
                targetValue = if (enabled) RentOutColors.Primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                label = "submit_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) Color.White
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (itemCount == 0) "Add items first" else buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = if (enabled) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Progress Variant Enum ─────────────────────────────────────────────────────
enum class ProgressVariant {
    LINEAR,        // Steady, predictable progression
    RANDOM,        // Variable speed with random increments
    FAST_BURST,    // Rapid bursts with occasional pauses
    STEPPED        // Milestone-based with distinct stages
}

// ── Kotlin Extension: FloatRange.random() ─────────────────────────────────────
private fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + Math.random().toFloat() * (endInclusive - start)
}

private fun LongRange.random(): Long {
    return start + (Math.random() * (endInclusive - start + 1)).toLong()
}
