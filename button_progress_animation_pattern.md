# 🎯 Button Progress Animation Pattern

**Reusable Reference for Implementing Animated Progress Buttons in Compose Multiplatform**

---

## 📋 Overview

This document provides a complete, production-ready pattern for creating sophisticated button animations with progress indicators, suitable for upload operations, submissions, and async actions.

### **Key Features**
- ✅ Smooth 0% → 100% progress animation
- ✅ Random or linear increment strategies
- ✅ Visual feedback: shimmer, rotation, morphing
- ✅ Spring-based micro-interactions
- ✅ Success celebration with checkmark pop
- ✅ Fully customizable timing and effects

---

## 🎨 Animation Variants

### **Variant A: Linear Progress (Original Implementation)**
- Predictable, steady increments
- Best for: Network uploads, file processing
- Duration: ~2.76 seconds to 92%

### **Variant B: Random Increments (New)**
- Variable-speed progression with realistic "thinking" delays
- Best for: AI processing, complex calculations, data analysis
- Duration: 2-4 seconds (randomized)

### **Variant C: Fast Random Bursts**
- Rapid micro-bursts with occasional pauses
- Best for: Image processing, batch operations
- Duration: 1.5-3 seconds

---

## 💻 Implementation: Random Increment Pattern

### **Complete Code Example**

```kotlin
@Composable
fun ProgressButton(
    imageCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    buttonText: String = "Submit",
    loadingText: String = "Processing",
    successText: String = "Complete!",
    variant: ProgressVariant = ProgressVariant.RANDOM
) {
    // ── States ────────────────────────────────────────────────────────────────
    val enabled = imageCount > 0 && !isLoading

    // Progress state
    var simulatedProgress by remember { mutableStateOf(0f) }
    var isDone by remember { mutableStateOf(false) }

    // ── Progress Logic: Random Increments ─────────────────────────────────────
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
                        val delay = if (Math.random() > 0.7) {
                            (150L..300L).random()  // Occasional thinking pause
                        } else {
                            (20L..60L).random()    // Fast increment
                        }
                        delay(delay)
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
        modifier = Modifier
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
                        text = if (imageCount == 0) "Add items first" else buttonText,
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

// ── Variant Enum ──────────────────────────────────────────────────────────────
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
```

---

## 🎬 Animation Timeline Comparison

### **Linear Variant (Original)**
```
0ms ────────────> 2760ms ──> completion
0%  ────────────>  92%   ──>   100%
     (steady 2% steps)
```

### **Random Variant (New)**
```
0ms ──> ? ──> ? ──> ? ──> completion
0%  ──> 15% ─> 40% ─> 70% ─> 92% ─> 100%
     (2-8% bursts, 40-150ms delays)
```

### **Fast Burst Variant**
```
0ms ──> [quick] ─> [pause] ─> [quick] ─> completion
0%  ──>   35%   ─>   35%    ─>   85%   ─>  92% ─> 100%
     (5-12% bursts, mixed delays)
```

### **Stepped Variant**
```
0ms ──> milestone 1 ──> milestone 2 ──> milestone 3 ──> completion
0%  ──>     25%     ──>     50%     ──>     75%     ──>    92% ─> 100%
     (smooth ramps with pauses at each milestone)
```

---

## ⚙️ Customization Options

### **Timing Parameters**

```kotlin
// Adjust these values to fine-tune behavior

// Random Variant
val minIncrement = 0.02f      // Minimum progress jump (2%)
val maxIncrement = 0.08f      // Maximum progress jump (8%)
val minDelay = 40L            // Fastest update speed (ms)
val maxDelay = 150L           // Slowest update speed (ms)
val holdAtPercent = 0.92f     // Where to pause before completion

// Fast Burst Variant
val burstMin = 0.05f          // Minimum burst size (5%)
val burstMax = 0.12f          // Maximum burst size (12%)
val pauseChance = 0.7         // 70% chance of pause
val pauseDuration = 150L..300L  // Pause length range
val quickDelay = 20L..60L     // Fast increment delay

// Stepped Variant
val milestones = listOf(0.25f, 0.50f, 0.75f, 0.92f)
val stepsPerMilestone = 8     // Smoothness of ramp
val milestonePause = 100L..200L  // Pause at each milestone
```

### **Visual Customization**

```kotlin
// Colors
val primaryColor = RentOutColors.Primary
val primaryLight = RentOutColors.PrimaryLight
val shimmerColor = Color.White.copy(alpha = 0.35f)
val trackColor = primaryColor.copy(alpha = 0.15f)

// Dimensions
val buttonHeight = 56.dp
val normalCornerRadius = 16.dp
val loadingCornerRadius = 28.dp  // Pill shape
val normalElevation = 10.dp
val pressedElevation = 2.dp

// Icon
val iconSize = 18.dp
val checkmarkSize = 20.dp
val spinDuration = 900  // Icon rotation speed (ms)

// Timing
val progressSmoothness = 300    // Progress interpolation (ms)
val cornerMorphDuration = 400   // Button → pill transition (ms)
val shimmerCycleDuration = 1200 // Shimmer sweep speed (ms)
val completionDelay = 400L      // Delay before checkmark (ms)
```

---

## 📱 Usage Examples

### **Example 1: Upload Button**
```kotlin
ProgressButton(
    imageCount = uploadedFiles.size,
    isLoading = uploadState is UploadState.Uploading,
    onClick = { viewModel.startUpload() },
    buttonText = "Upload Files",
    loadingText = "Uploading",
    successText = "Upload Complete!",
    variant = ProgressVariant.RANDOM
)
```

### **Example 2: AI Processing**
```kotlin
ProgressButton(
    imageCount = if (promptReady) 1 else 0,
    isLoading = aiState is AIState.Processing,
    onClick = { viewModel.generateContent() },
    buttonText = "Generate",
    loadingText = "AI Processing",
    successText = "Generated!",
    variant = ProgressVariant.STEPPED
)
```

### **Example 3: Batch Operation**
```kotlin
ProgressButton(
    imageCount = selectedItems.size,
    isLoading = batchState is BatchState.Running,
    onClick = { viewModel.processBatch() },
    buttonText = "Process ${selectedItems.size} items",
    loadingText = "Processing",
    successText = "Batch Complete!",
    variant = ProgressVariant.FAST_BURST
)
```

---

## 🎯 When to Use Each Variant

| Variant | Best For | User Perception | Duration |
|---------|----------|----------------|----------|
| **LINEAR** | File uploads, downloads | Steady, reliable | ~2.8s |
| **RANDOM** | AI processing, complex calculations | Dynamic, "thinking" | 2-4s |
| **FAST_BURST** | Image processing, batch ops | Energetic, powerful | 1.5-3s |
| **STEPPED** | Multi-stage processes | Clear progress milestones | 3-5s |

---

## 🔧 Advanced: Custom Increment Strategy

Create your own progression logic:

```kotlin
ProgressVariant.CUSTOM -> {
    // Example: Exponential slowdown (fast start, slow end)
    var speed = 0.15f  // Start fast (15% jumps)
    while (simulatedProgress < 0.92f) {
        val increment = speed * (1f - simulatedProgress)  // Exponential decay
        simulatedProgress = (simulatedProgress + increment).coerceAtMost(0.92f)
        
        speed *= 0.95f  // Slow down over time
        delay(50L)
    }
}

// Example: Fibonacci-inspired progression
ProgressVariant.FIBONACCI -> {
    val sequence = listOf(0.03f, 0.05f, 0.08f, 0.13f, 0.21f, 0.34f)
    for (increment in sequence) {
        if (simulatedProgress >= 0.92f) break
        simulatedProgress = (simulatedProgress + increment).coerceAtMost(0.92f)
        delay((60L..120L).random())
    }
}
```

---

## 📊 Performance Considerations

### **Memory**
- Uses `remember` for state persistence
- Infinite transitions clean up automatically
- No memory leaks from coroutines (LaunchedEffect scoped)

### **Rendering**
- GPU-accelerated (`graphicsLayer` for transformations)
- Efficient recomposition (animated values don't trigger full recompose)
- Shimmer gradient reuses same brush pattern

### **Optimization Tips**
```kotlin
// For low-end devices: reduce shimmer complexity
val useShimmer = !isLowEndDevice()

// Reduce animation steps on slow devices
val stepDelay = if (isLowEndDevice()) 100L else 60L

// Disable spring animations on very old devices
val scaleSpec = if (Build.VERSION.SDK_INT >= 26) {
    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
} else {
    tween(200)  // Simple tween fallback
}
```

---

## ✅ Testing Checklist

- [ ] Button disabled when `imageCount == 0`
- [ ] Progress starts at 0% when `isLoading` becomes `true`
- [ ] Progress stops at 92% and waits for backend
- [ ] Snaps to 100% when `isLoading` becomes `false`
- [ ] Checkmark appears after 400ms delay
- [ ] Press interaction works (scale + elevation)
- [ ] Shimmer animates smoothly during loading
- [ ] Icon rotation is smooth (no jank)
- [ ] Corner radius morphs correctly
- [ ] Colors match brand guidelines
- [ ] Random increments feel natural (not too fast/slow)
- [ ] Works on iOS, Android, and Web targets

---

## 🎨 Color Customization Reference

```kotlin
// From your RentOut theme
object RentOutColors {
    val Primary = Color(0xFF6200EE)           // Main brand color
    val PrimaryLight = Color(0xFF9D46FF)      // Gradient accent
    val Success = Color(0xFF4CAF50)           // For checkmark
    val Warning = Color(0xFFFFC107)           // For paused states
    val Error = Color(0xFFE53935)             // For failures
}

// Usage in button
val loadingGradient = Brush.horizontalGradient(
    colors = listOf(
        RentOutColors.Primary,
        RentOutColors.PrimaryLight,
        RentOutColors.Primary
    )
)
```

---

## 📚 Related Patterns

- **Loading Spinner**: For indefinite loading states
- **Skeleton Screens**: For content loading previews
- **Toast Messages**: For post-completion notifications
- **Haptic Feedback**: Pair with progress milestones

---

## 🐛 Common Issues & Solutions

### **Issue**: Progress jumps erratically
**Solution**: Increase `progressSmoothness` duration (e.g., 300ms → 500ms)

### **Issue**: Animation stutters on Android
**Solution**: Use `graphicsLayer` instead of `Modifier.scale()` for transformations

### **Issue**: Shimmer doesn't show
**Solution**: Ensure shimmer box has proper `fillMaxWidth(fraction)` matching progress

### **Issue**: Button doesn't re-enable after completion
**Solution**: Reset `simulatedProgress = 0f` when navigating away or on screen recompose

### **Issue**: Random increments too fast/slow
**Solution**: Adjust delay ranges - increase for slower, decrease for faster

---

## 📖 Further Reading

- [Material Design Motion Guidelines](https://material.io/design/motion)
- [Compose Animation Spec Reference](https://developer.android.com/jetpack/compose/animation)
- [Spring Physics in Compose](https://developer.android.com/jetpack/compose/animation#spring)

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-03-07 | Initial pattern extracted from PropertyImagesScreen |
| 1.1 | 2026-03-07 | Added random increment variants (RANDOM, FAST_BURST, STEPPED) |
| 1.2 | 2026-03-07 | Added customization options and advanced examples |

---

**🎯 Pro Tip**: For critical operations (payments, deletions), use LINEAR variant for predictability. For creative operations (AI, uploads), use RANDOM for engaging UX.

---

**Created by**: RentOut Development Team  
**License**: Internal use - RentOut Project  
**Last Updated**: March 7, 2026
