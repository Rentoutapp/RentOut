package org.example.project.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.components.IntroVideoPlayer
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    // videoEnded = true either when video finishes or as a fallback after a timeout
    var videoEnded   by remember { mutableStateOf(false) }
    var overlayReady by remember { mutableStateOf(false) }

    // Safety timeout — if video never fires onVideoEnded (e.g. missing file),
    // we still show the UI after 6 seconds so the user is never stuck.
    LaunchedEffect(Unit) {
        delay(6_000)
        videoEnded = true
    }

    // Animate the logo + text overlay in once video ends
    LaunchedEffect(videoEnded) {
        if (videoEnded) {
            delay(120)
            overlayReady = true
        }
    }

    val logoScale by animateFloatAsState(
        targetValue = if (overlayReady) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (overlayReady) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen video (behind everything) ─────────────────────────
        IntroVideoPlayer(
            modifier    = Modifier.fillMaxSize(),
            onVideoEnded = { videoEnded = true }
        )

        // ── Dark gradient scrim over video ────────────────────────────────
        // Always visible so the logo/text is readable even before the video ends
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.25f),
                            0.5f to Color.Black.copy(alpha = 0.15f),
                            0.75f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ── Content overlay (fades in after video ends) ───────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top: Logo + app name ──────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 64.dp)
                    .alpha(contentAlpha)
            ) {
                // Logo wordmark
                Box(
                    modifier = Modifier
                        .scale(logoScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "RentOut",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.2).sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Zimbabwe's Premier\nRental Marketplace",
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 25.sp
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Feature pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IntroPill("🏠 List Properties")
                    IntroPill("🔑 Find Rentals")
                    IntroPill("✅ Verified")
                }
            }

            // ── Bottom: CTA button (slides up after video ends) ───────────
            AnimatedVisibility(
                visible = overlayReady,
                enter   = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    RentOutPrimaryButton(
                        text     = "Get Started →",
                        onClick  = onGetStarted,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Trusted by landlords & tenants across Zimbabwe",
                        fontSize  = 12.sp,
                        color     = Color.White.copy(alpha = 0.60f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text       = text,
            color      = Color.White,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
