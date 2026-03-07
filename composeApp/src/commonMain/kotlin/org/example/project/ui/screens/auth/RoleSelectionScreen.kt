package org.example.project.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDark) {
                    Modifier.background(MaterialTheme.colorScheme.background)
                } else {
                    // Light mode: subtle darker background for better visibility
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE8EDF5), // Subtle blue-gray
                                Color(0xFFF2F4F7)  // Lighter gray-white
                            )
                        )
                    )
                }
            )
    ) {
        // Top decorative gradient header - adapts to theme
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Centered content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header section with rich typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Who are you?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        Color(0xFF1A1F36)
                    },
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp, // Tighter spacing for impact
                    lineHeight = 40.sp,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Choose how you'll use RentOut",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    } else {
                        Color(0xFF5B6B8C)
                    },
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.15.sp, // Slightly open for readability
                    lineHeight = 24.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(Modifier.height(40.dp))

            // Landlord card
            RoleCard(
                icon = Icons.Default.Home,
                iconTint = RentOutColors.IconBlue,
                title = "Landlord",
                subtitle = "List your properties &\nearn from tenants",
                emoji = "🏠",
                isSelected = selectedRole == "landlord",
                selectedBorderColor = MaterialTheme.colorScheme.primary,
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
                selectedBorderColor = MaterialTheme.colorScheme.secondary,
                onClick = { selectedRole = "tenant" }
            )

            Spacer(Modifier.height(32.dp))

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
    selectedBorderColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
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
        targetValue = if (isSelected) {
            12.dp
        } else {
            if (isDark) 6.dp else 8.dp // More elevation in light mode for better visibility
        },
        label = "role_card_elevation"
    )

    // Theme-adaptive card background
    val cardBackground = if (isDark) {
        // Dark mode: elevated surface with subtle gradient
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        )
    } else {
        // Light mode: solid white with subtle depth for better visibility
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFAFBFC)
            )
        )
    }

    // Border color - more prominent when selected
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            selectedBorderColor
        } else {
            if (isDark) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            } else {
                // Light mode: more visible border for better card definition
                Color(0xFFE0E5ED)
            }
        },
        label = "border_color"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 1.5.dp,
        label = "border_width"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon box with theme-adaptive background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDark) {
                            iconTint.copy(alpha = 0.25f)
                        } else {
                            iconTint.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 32.sp)
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp, // Tight for bold headlines
                    lineHeight = 26.sp,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isDark) 0.85f else 0.7f
                    ),
                    letterSpacing = 0.1.sp, // Slightly open for body text
                    lineHeight = 20.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal
                    )
                )
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
