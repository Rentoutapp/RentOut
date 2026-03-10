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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.components.IntroVideoPlayer

@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    var videoEnded   by remember { mutableStateOf(false) }
    var overlayReady by remember { mutableStateOf(false) }

    // Safety timeout — navigate regardless after 6 s if video never fires onVideoEnded
    LaunchedEffect(Unit) {
        delay(6_000)
        videoEnded = true
    }

    // Once video ends: show overlay briefly, then auto-navigate
    LaunchedEffect(videoEnded) {
        if (videoEnded) {
            delay(150)   // tiny pause so overlay animates in cleanly
            overlayReady = true
            delay(2_200) // let the user read the overlay, then navigate
            onGetStarted()
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue   = if (overlayReady) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen video ─────────────────────────────────────────────
        IntroVideoPlayer(
            modifier     = Modifier.fillMaxSize(),
            onVideoEnded = { videoEnded = true }
        )

        // ── Gradient scrim (always visible for readability) ───────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.20f),
                            0.45f to Color.Black.copy(alpha = 0.10f),
                            0.72f to Color.Black.copy(alpha = 0.50f),
                            1.00f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // ── Overlay content ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Logo image (already contains the RentOut name — no duplicate text)
            // The logo is displayed by the parent screen/splash; here we show
            // only the tagline, pills, and trust line below the video overlay.

            // ── Tagline ───────────────────────────────────────────────────
            Text(
                text      = "Zimbabwe's Premier\nRental Marketplace",
                color     = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight   = FontWeight.Bold,
                    fontSize     = 22.sp,
                    lineHeight   = 30.sp
                )
            )

            Spacer(Modifier.height(18.dp))

            // ── Feature pills ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                IntroPill("🏠 List Properties")
                IntroPill("🔑 Find Rentals")
                IntroPill("✅ Verified")
            }

            Spacer(Modifier.height(28.dp))

            // ── Trust line ────────────────────────────────────────────────
            Text(
                text      = "Trusted by landlords & tenants across Zimbabwe",
                fontSize  = 12.sp,
                color     = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))
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
