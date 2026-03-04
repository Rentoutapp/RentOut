package org.example.project.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun SuspendedScreen(onContactSupport: () -> Unit, onLogout: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "suspended_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "icon_pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF5F5), Color(0xFFFFECEC)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(100.dp).scale(pulse).clip(CircleShape)
                    .background(RentOutColors.StatusRejected.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Block, null, tint = RentOutColors.StatusRejected,
                    modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Account Suspended", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = RentOutColors.StatusRejected, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Your account has been suspended by an administrator. Please contact our support team to resolve this issue.",
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))
            RentOutPrimaryButton("Contact Support", onClick = onContactSupport, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Sign Out", color = RentOutColors.StatusRejected)
            }
        }
    }
}
