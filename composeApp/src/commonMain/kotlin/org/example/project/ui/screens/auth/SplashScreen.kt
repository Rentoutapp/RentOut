package org.example.project.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.ui.theme.RentOutColors

@Composable
fun SplashScreen(
    onNavigateToRole: () -> Unit,
    onNavigateToLandlord: () -> Unit,
    onNavigateToTenant: () -> Unit,
    currentUserRole: String? = null
) {
    val isDark = isSystemInDarkTheme()
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    // Animated logo scale + pulse
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "logo_scale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "text_alpha"
    )

    LaunchedEffect(Unit) {
        delay(300)
        logoVisible = true
        delay(600)
        textVisible = true
        delay(2_400)   // let logo pulse + text fully settle before routing
        when (currentUserRole) {
            "landlord" -> onNavigateToLandlord()
            "tenant"   -> onNavigateToTenant()
            else       -> onNavigateToRole()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(RentOutColors.Primary, Color(0xFF001A80))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(RentOutColors.Primary, RentOutColors.PrimaryLight)
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative ring
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(logoScale * pulse)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "RentOut",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "RentOut",
                color = Color.White,
                modifier = Modifier.alpha(textAlpha),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Setting up your experience...",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.alpha(textAlpha),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(32.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(textAlpha)
            ) {
                repeat(3) { index ->
                    val dotPulse by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = index * 200),
                            RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(dotPulse)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = dotPulse))
                    )
                }
            }
        }
    }
}
