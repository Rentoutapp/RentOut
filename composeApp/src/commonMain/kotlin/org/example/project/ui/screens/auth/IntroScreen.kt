package org.example.project.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    // Animated logo scale
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RentOutColors.Primary,
                        RentOutColors.PrimaryDark,
                        Color(0xFF001A80)
                    )
                )
            )
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(RentOutColors.Secondary.copy(alpha = 0.15f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "RentOut Logo",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // App name
            Text(
                text = "RentOut",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Zimbabwe's Premier\nRental Marketplace",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(Modifier.height(16.dp))

            // Feature pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(contentAlpha)
            ) {
                FeaturePill("🏠 List Properties")
                FeaturePill("🔑 Find Rentals")
                FeaturePill("✅ Verified")
            }

            Spacer(Modifier.height(64.dp))

            // CTA Button
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RentOutPrimaryButton(
                        text = "Get Started →",
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Trusted by landlords & tenants across Zimbabwe",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
