package org.example.project.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.components.IntroVideoPlayer

@Composable
fun IntroScreen(
    onGetStarted: () -> Unit,
    onAutoLogin: (() -> Unit)? = null,   // called instead of onGetStarted when rememberMeActive
    rememberMeActive: Boolean = false
) {

    // ── Animation stage flags ─────────────────────────────────────────────
    var videoEnded       by remember { mutableStateOf(false) }
    var showScrim        by remember { mutableStateOf(false) }
    var showPills        by remember { mutableStateOf(false) }
    var showTrustLine    by remember { mutableStateOf(false) }
    // Safety timeout — if the video never fires onVideoEnded (e.g. codec issue),
    // we still start the sequence after 8 seconds so the user is never stuck.
    LaunchedEffect(Unit) {
        delay(8_000)
        if (!videoEnded) videoEnded = true
    }

    // Sequenced reveal after video ends
    LaunchedEffect(videoEnded) {
        if (!videoEnded) return@LaunchedEffect

        // Step 1 — fade in gradient scrim (600ms)
        showScrim = true
        delay(600)

        if (rememberMeActive && onAutoLogin != null) {
            // ── Remember Me route ─────────────────────────────────────────
            // Play the full staggered reveal so nothing is abruptly cut,
            // then auto-continue to splash → dashboard.
            showPills = true
            delay(800)
            showTrustLine = true
            delay(2_400)   // hold so the user sees the full intro before continuing
            onAutoLogin()
            return@LaunchedEffect
        }

        // ── Normal route ──────────────────────────────────────────────────
        // Step 2 — pills spring in (800ms gap)
        showPills = true
        delay(800)

        // Step 3 — trust line fades in (600ms gap)
        showTrustLine = true

        // Step 4 — hold so the user can read everything, then auto-navigate
        delay(3_200)
        onGetStarted()
    }

    // ── Animations ────────────────────────────────────────────────────────
    val scrimAlpha by animateFloatAsState(
        targetValue   = if (showScrim) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "scrim_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen video (plays uninterrupted) ───────────────────────
        IntroVideoPlayer(
            modifier     = Modifier.fillMaxSize(),
            onVideoEnded = { videoEnded = true }
        )

        // ── Gradient scrim — fades in gently after video ends ─────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.05f * scrimAlpha),
                            0.45f to Color.Black.copy(alpha = 0.18f * scrimAlpha),
                            0.70f to Color.Black.copy(alpha = 0.62f * scrimAlpha),
                            1.00f to Color.Black.copy(alpha = 0.94f * scrimAlpha)
                        )
                    )
                )
        )

        // ── Staggered content overlay ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            // ── Pills ─────────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = showPills,
                enter   = androidx.compose.animation.scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    initialScale  = 0.55f
                ) + androidx.compose.animation.fadeIn(tween(500))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    IntroPill("🏠 List Properties")
                    IntroPill("🔑 Find Rentals")
                    IntroPill("✅ Verified")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Trust line ────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = showTrustLine,
                enter   = androidx.compose.animation.fadeIn(tween(900))
            ) {
                Text(
                    text      = "Trusted by landlords & tenants across Zimbabwe",
                    fontSize  = 12.sp,
                    color     = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

        }
    }
}

@Composable
private fun IntroPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text       = text,
            color      = Color.White,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
