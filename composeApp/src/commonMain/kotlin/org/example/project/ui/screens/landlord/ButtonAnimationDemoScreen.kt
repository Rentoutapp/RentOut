@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.landlord

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.ui.components.ProgressButton
import org.example.project.ui.components.ProgressVariant
import org.example.project.ui.theme.RentOutColors

/**
 * Demo screen showcasing all ProgressButton animation variants side-by-side
 */
@Composable
fun ButtonAnimationDemoScreen(onBack: () -> Unit) {
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f, tween(200), label = "br")

    // Coroutine scope for launching coroutines
    val coroutineScope = rememberCoroutineScope()

    // Individual loading states for each variant
    var linearLoading by remember { mutableStateOf(false) }
    var randomLoading by remember { mutableStateOf(false) }
    var burstLoading by remember { mutableStateOf(false) }
    var steppedLoading by remember { mutableStateOf(false) }

    // All-in-one test
    var allLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))

            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                IconButton(
                    onClick = { backPressed = true; onBack() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Button Animations",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(28.dp))

            // Main content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "🎯 Progress Button Variants",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap each button to see different animation styles. Watch how progress animates from 0% to 100%.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(Modifier.height(28.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(24.dp))

                    // ── Variant 1: LINEAR ─────────────────────────────────────────
                    VariantSection(
                        title = "1. Linear Progression",
                        description = "Steady, predictable increments. Best for file uploads.",
                        timing = "~2.76s to 92%",
                        icon = "📊"
                    ) {
                        ProgressButton(
                            itemCount = 1,
                            isLoading = linearLoading,
                            onClick = {
                                linearLoading = true
                                // Simulate backend completion after 3s
                                coroutineScope.launch {
                                    delay(3000)
                                    linearLoading = false
                                }
                            },
                            buttonText = "Test Linear",
                            loadingText = "Uploading",
                            successText = "Upload Complete!",
                            variant = ProgressVariant.LINEAR
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(24.dp))

                    // ── Variant 2: RANDOM ─────────────────────────────────────────
                    VariantSection(
                        title = "2. Random Increments",
                        description = "Variable speed with 2-8% jumps. Dynamic, \"thinking\" feel.",
                        timing = "2-4s (randomized)",
                        icon = "🎲"
                    ) {
                        ProgressButton(
                            itemCount = 1,
                            isLoading = randomLoading,
                            onClick = {
                                randomLoading = true
                                coroutineScope.launch {
                                    delay(3500)
                                    randomLoading = false
                                }
                            },
                            buttonText = "Test Random",
                            loadingText = "Processing",
                            successText = "Done!",
                            variant = ProgressVariant.RANDOM
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(24.dp))

                    // ── Variant 3: FAST_BURST ─────────────────────────────────────
                    VariantSection(
                        title = "3. Fast Burst",
                        description = "Rapid 5-12% bursts with thinking pauses. Energetic feel.",
                        timing = "1.5-3s (variable)",
                        icon = "⚡"
                    ) {
                        ProgressButton(
                            itemCount = 1,
                            isLoading = burstLoading,
                            onClick = {
                                burstLoading = true
                                coroutineScope.launch {
                                    delay(2500)
                                    burstLoading = false
                                }
                            },
                            buttonText = "Test Fast Burst",
                            loadingText = "Analyzing",
                            successText = "Analysis Complete!",
                            variant = ProgressVariant.FAST_BURST
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(24.dp))

                    // ── Variant 4: STEPPED ────────────────────────────────────────
                    VariantSection(
                        title = "4. Stepped Milestones",
                        description = "Smooth ramps to 25%, 50%, 75%, 92% with pauses. Clear stages.",
                        timing = "3-5s (staged)",
                        icon = "📈"
                    ) {
                        ProgressButton(
                            itemCount = 1,
                            isLoading = steppedLoading,
                            onClick = {
                                steppedLoading = true
                                coroutineScope.launch {
                                    delay(4000)
                                    steppedLoading = false
                                }
                            },
                            buttonText = "Test Stepped",
                            loadingText = "Processing",
                            successText = "All Stages Complete!",
                            variant = ProgressVariant.STEPPED
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                    Divider(
                        thickness = 2.dp,
                        color = RentOutColors.Primary.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(24.dp))

                    // ── Test All Button ───────────────────────────────────────────
                    Text(
                        text = "🎬 Run All Variants",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Trigger all 4 animations simultaneously to compare them.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            linearLoading = true
                            randomLoading = true
                            burstLoading = true
                            steppedLoading = true
                            
                            // Stagger the completions
                            coroutineScope.launch {
                                delay(2500)
                                burstLoading = false
                                delay(500)
                                linearLoading = false
                                delay(1000)
                                randomLoading = false
                                delay(500)
                                steppedLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RentOutColors.Secondary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "▶ Test All Variants",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Technical info
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = RentOutColors.SurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 Technical Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RentOutColors.Primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• All variants hold at 92% until backend completes\n" +
                                       "• Smooth 300ms interpolation with FastOutSlowInEasing\n" +
                                       "• Shimmer sweep + spinning icon during loading\n" +
                                       "• Bouncy checkmark pop on completion\n" +
                                       "• Spring-based press interaction (94% scale)\n" +
                                       "• Corner morphing: 16dp → 28dp (pill shape)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VariantSection(
    title: String,
    description: String,
    timing: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = RentOutColors.Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "⏱️ $timing",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = RentOutColors.Primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
