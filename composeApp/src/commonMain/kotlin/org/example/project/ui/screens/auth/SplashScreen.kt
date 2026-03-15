package org.example.project.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.util.DashboardBackHandler

// Brand colors extracted directly from the RentOut logo
private val SplashDeepNavy    = Color(0xFF0E1C3E)   // darkest navy (logo left/shadow)
private val SplashNavy        = Color(0xFF1A2B5E)   // primary navy (house & R left half)
private val SplashTeal        = Color(0xFF00B4AE)   // vibrant teal (key & R right half)
private val SplashTealLight   = Color(0xFF1ED8C8)   // bright teal highlight
private val SplashTealDark    = Color(0xFF007A75)   // deep teal shadow
private val SplashWhite       = Color(0xFFFFFFFF)

@Composable
fun SplashScreen(
    onNavigateToRole: () -> Unit,
    onNavigateToLandlord: () -> Unit,
    onNavigateToTenant: () -> Unit,
    currentUserRole: String? = null
) {
    // Splash is a transition-only screen — it auto-navigates and has no
    // meaningful back destination. Block back presses entirely so the user
    // cannot interrupt the routing logic mid-flight.
    DashboardBackHandler(onBackPress = { /* consume silently during splash */ })

    // ── Entrance visibility states ──────────────────────────────────────────────
    var logoVisible    by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var dotsVisible    by remember { mutableStateOf(false) }

    // ── Infinite transition for all ambient animations ───────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")

    // Animated background color: very slow navy → teal breathe (sine-like via LinearEasing)
    val bgShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(8_000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "bg_shift"
    )

    // Outer ring — very slow constant rotation, perfectly smooth
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "ring_rotation"
    )

    // Inner ring — slightly faster counter-rotation, also linear
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(tween(13_000, easing = LinearEasing)),
        label = "ring_rotation2"
    )

    // Logo glow pulse — very slow and gentle, linear for smoothness
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(3_500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    // Orb float — very slow, linear drift
    val orbFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 14f,
        animationSpec = infiniteRepeatable(
            tween(5_000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "orb_float"
    )

    // Shimmer — constant speed sweep, never eases in/out
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(tween(3_500, easing = LinearEasing)),
        label = "shimmer"
    )

    // Teal glow pulse — very slow, linear so no sudden acceleration
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue  = 0.58f,
        animationSpec = infiniteRepeatable(
            tween(3_000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Animated background: interpolate between deep navy and a teal-tinged navy
    val animatedBgTop    = lerp(SplashDeepNavy, SplashNavy,     bgShift * 0.4f)
    val animatedBgMid    = lerp(SplashNavy,     SplashTealDark, bgShift * 0.5f)
    val animatedBgBottom = lerp(SplashTealDark, SplashTeal,     bgShift * 0.6f)

    // ── Entrance animations — all use LinearEasing for buttery smoothness ────────
    val logoScale by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0.5f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue  = if (logoVisible) 1f else 0f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "logo_alpha"
    )
    val taglineOffsetY by animateFloatAsState(
        targetValue  = if (taglineVisible) 0f else 20f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "tagline_offset"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue  = if (taglineVisible) 1f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "tagline_alpha"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue  = if (dotsVisible) 1f else 0f,
        animationSpec = tween(700, easing = LinearEasing),
        label = "dots_alpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
        delay(500)
        taglineVisible = true
        delay(400)
        dotsVisible = true
        delay(2_100)
        when (currentUserRole) {
            "landlord" -> onNavigateToLandlord()
            "tenant"   -> onNavigateToTenant()
            else       -> onNavigateToRole()
        }
    }

    // ── Animated background: navy top → teal-infused bottom, breathing slowly ───
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to animatedBgTop,
                        0.50f to animatedBgMid,
                        1.00f to animatedBgBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Ambient orb: large teal bloom top-left (mirrors logo teal side) ──────
        Box(
            Modifier
                .size(380.dp)
                .offset(x = (-80).dp, y = (-130).dp - orbFloat.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashTeal.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .blur(50.dp)
        )

        // ── Ambient orb: navy-teal bloom bottom-right ─────────────────────────────
        Box(
            Modifier
                .size(300.dp)
                .offset(x = 90.dp, y = 160.dp + orbFloat.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashTealLight.copy(alpha = 0.13f),
                            Color.Transparent
                        )
                    )
                )
                .blur(45.dp)
        )

        // ── Ambient orb: deep navy accent top-right ───────────────────────────────
        Box(
            Modifier
                .size(220.dp)
                .offset(x = 110.dp, y = (-160).dp + orbFloat.dp * 0.5f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashNavy.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
                .blur(30.dp)
        )

        // ── Outer slowly rotating ring (teal dots — like the key shaft) ──────────
        Box(
            Modifier
                .size(240.dp)
                .rotate(ringRotation)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(SplashTeal.copy(alpha = 0.75f))
            )
            Box(
                Modifier
                    .size(6.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .clip(CircleShape)
                    .background(SplashTealLight.copy(alpha = 0.6f))
            )
            Box(
                Modifier
                    .size(5.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = 6.dp)
                    .clip(CircleShape)
                    .background(SplashWhite.copy(alpha = 0.45f))
            )
        }

        // ── Inner counter-rotating ring (navy dots) ───────────────────────────────
        Box(
            Modifier
                .size(170.dp)
                .rotate(ringRotation2)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = 12.dp)
                    .clip(CircleShape)
                    .background(SplashTealLight.copy(alpha = 0.55f))
            )
            Box(
                Modifier
                    .size(7.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
                    .clip(CircleShape)
                    .background(SplashTeal.copy(alpha = 0.5f))
            )
        }

        // ── Main content column ──────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo container ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale * logoPulse)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Animated teal glow halo behind logo
                Box(
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SplashTeal.copy(alpha = glowPulse),
                                    SplashTealDark.copy(alpha = glowPulse * 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(18.dp)
                )

                // Frosted glass card — navy-tinted to blend with background
                val shimmerPos = ((shimmer + 1f) / 3f).coerceIn(0.01f, 0.99f)
                Box(
                    modifier = Modifier
                        .size(142.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f         to SplashNavy.copy(alpha = 0.55f),
                                    shimmerPos to SplashWhite.copy(alpha = 0.12f),
                                    1f         to SplashTealDark.copy(alpha = 0.45f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Logo — white bg is transparent so navy/teal card shows through
                    Image(
                        painter            = painterResource(id = org.example.project.R.drawable.rentlogo_clean),
                        contentDescription = "RentOut Logo",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier
                            .size(118.dp)
                            .padding(6.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // ── Brand name ────────────────────────────────────────────────────────
            Text(
                text     = "RentOut",
                color    = SplashWhite,
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffsetY.dp),
                style    = MaterialTheme.typography.displaySmall.copy(
                    fontSize      = 44.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(Modifier.height(6.dp))

            // ── Teal → teal-light gradient underline (mirrors the logo's key color) ──
            Box(
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffsetY.dp)
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(SplashTealDark, SplashTeal, SplashTealLight)
                        )
                    )
            )

            Spacer(Modifier.height(10.dp))

            // ── Tagline ────────────────────────────────────────────────────────────
            Text(
                text     = "Find your perfect space",
                color    = SplashTealLight.copy(alpha = 0.80f),
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineOffsetY.dp),
                style    = MaterialTheme.typography.bodyMedium.copy(
                    fontSize      = 14.sp,
                    letterSpacing = 0.8.sp
                )
            )

            Spacer(Modifier.height(60.dp))

            // ── Staggered teal loading dots ───────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.alpha(dotsAlpha)
            ) {
                val dotColors = listOf(SplashTealDark, SplashTeal, SplashTealLight)
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.45f,
                        targetValue  = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(900, delayMillis = index * 300, easing = LinearEasing),
                            RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColors[index].copy(alpha = dotScale))
                    )
                }
            }
        }

        // ── Bottom caption ─────────────────────────────────────────────────────────
        Text(
            text     = "Powered by RentOut",
            color    = SplashTeal.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(dotsAlpha),
            style    = MaterialTheme.typography.bodySmall.copy(
                fontSize      = 11.sp,
                letterSpacing = 1.2.sp
            )
        )
    }
}
