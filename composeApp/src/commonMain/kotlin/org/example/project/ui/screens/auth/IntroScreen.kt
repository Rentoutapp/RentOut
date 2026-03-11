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
    var videoEnded     by remember { mutableStateOf(false) }
    var showPills      by remember { mutableStateOf(false) }
    var showTrustLine  by remember { mutableStateOf(false) }
    var showScrim      by remember { mutableStateOf(false) }

    // Safety timeout — if the video never fires onVideoEnded (e.g. codec issue),
    // we still start the sequence after 7 seconds so the user is never stuck.
    LaunchedEffect(Unit) {
        delay(7_000)
        if (!videoEnded) videoEnded = true
    }

    // Sequenced reveal: wait for the video's own animated logo to finish,
    // then stagger each element in one by one before auto-navigating.
    LaunchedEffect(videoEnded) {
        if (!videoEnded) return@LaunchedEffect

        if (rememberMeActive && onAutoLogin != null) {
            // ── Short route: Remember Me is active ───────────────────────
            // Play the video (already playing), then go straight to splash
            // which will route to the correct dashboard by role.
            showScrim = true
            delay(600)
            onAutoLogin()
            return@LaunchedEffect
        }

        // ── Normal route: no active session ──────────────────────────────
        // 1. Fade in the gradient scrim so text becomes readable
        showScrim = true
        delay(400)

        // 2. Pills pop in
        showPills = true
        delay(700)

        // 3. Trust line fades in
        showTrustLine = true

        // 4. Hold so the user can read everything comfortably
        delay(3_200)

        // 5. Auto-navigate to Role Selection
        onGetStarted()
    }

    // Scrim alpha animation
    val scrimAlpha by animateFloatAsState(
        targetValue   = if (showScrim) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
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
                            0.00f to Color.Black.copy(alpha = 0.10f * scrimAlpha),
                            0.50f to Color.Black.copy(alpha = 0.20f * scrimAlpha),
                            0.72f to Color.Black.copy(alpha = 0.60f * scrimAlpha),
                            1.00f to Color.Black.copy(alpha = 0.92f * scrimAlpha)
                        )
                    )
                )
        )

        // ── Staggered content overlay ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            // ── Pills — each pops in with a spring scale ──────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = showPills,
                enter   = androidx.compose.animation.scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    initialScale  = 0.6f
                ) + androidx.compose.animation.fadeIn(tween(400))
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

            Spacer(Modifier.height(24.dp))

            // ── Trust line — gentle fade in last ─────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = showTrustLine,
                enter   = androidx.compose.animation.fadeIn(tween(800))
            ) {
                Text(
                    text      = "Trusted by landlords & tenants across Zimbabwe",
                    fontSize  = 12.sp,
                    color     = Color.White.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(80.dp))
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
