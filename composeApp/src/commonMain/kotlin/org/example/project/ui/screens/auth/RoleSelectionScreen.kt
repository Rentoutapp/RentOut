package org.example.project.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String) -> Unit
) {
    var selectedRole by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Text(
                "I want to...",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose how you'll use RentOut",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Landlord card
            RoleCard(
                icon = Icons.Default.Home,
                iconTint = RentOutColors.IconBlue,
                title = "Landlord",
                subtitle = "List your properties &\nearn from tenants",
                emoji = "🏠",
                isSelected = selectedRole == "landlord",
                gradientColors = listOf(
                    RentOutColors.SurfaceVariant,
                    Color(0xFFDCEAFF)
                ),
                selectedBorderColor = RentOutColors.Primary,
                onClick = { selectedRole = "landlord" }
            )

            Spacer(Modifier.height(20.dp))

            // Tenant card
            RoleCard(
                icon = Icons.Default.Key,
                iconTint = RentOutColors.IconAmber,
                title = "Tenant",
                subtitle = "Find rentals & pay \$10\nto unlock landlord contacts",
                emoji = "🔑",
                isSelected = selectedRole == "tenant",
                gradientColors = listOf(
                    Color(0xFFFFF4EE),
                    Color(0xFFFFE4D6)
                ),
                selectedBorderColor = RentOutColors.Secondary,
                onClick = { selectedRole = "tenant" }
            )

            Spacer(Modifier.height(40.dp))

            RentOutPrimaryButton(
                text = "Continue →",
                onClick = { if (selectedRole.isNotEmpty()) onRoleSelected(selectedRole) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedRole.isNotEmpty()
            )
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    gradientColors: List<Color>,
    selectedBorderColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed  -> 0.96f
            isSelected -> 1.02f
            else       -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "role_card_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        label = "role_card_elevation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(gradientColors))
            .then(
                if (isSelected) Modifier.border(2.5.dp, selectedBorderColor, RoundedCornerShape(24.dp))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 32.sp)
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = selectedBorderColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
