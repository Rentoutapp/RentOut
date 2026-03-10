package org.example.project.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.components.ProgressButton
import org.example.project.ui.components.ProgressVariant
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.util.rememberPulseAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (role: String, subtype: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf("") }
    var selectedSubtype by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()
    
    // Animation states - staggered entrance
    var headerVisible by remember { mutableStateOf(false) }
    var landlordCardVisible by remember { mutableStateOf(false) }
    var tenantCardVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        kotlinx.coroutines.delay(150)
        landlordCardVisible = true
        kotlinx.coroutines.delay(100)
        tenantCardVisible = true
        kotlinx.coroutines.delay(100)
        buttonVisible = true
    }

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
            // Header section with rich typography and animations
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) + 
                        slideInVertically(
                            animationSpec = tween(600, easing = FastOutSlowInEasing),
                            initialOffsetY = { -40 }
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Who are you?",
                        color = if (isDark) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            Color(0xFF1A1F36)
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Choose how you'll use RentOut",
                        color = if (isDark) {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        } else {
                            Color(0xFF5B6B8C)
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Property Provider card with animation and expandable subtypes
            AnimatedVisibility(
                visible = landlordCardVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column {
                    ProviderRoleCard(
                        isSelected = selectedRole == "landlord",
                        onClick = {
                            selectedRole = "landlord"
                            if (selectedSubtype.isBlank()) selectedSubtype = "landlord"
                        }
                    )

                    // Expandable subtype pills
                    AnimatedVisibility(
                        visible = selectedRole == "landlord",
                        enter = expandVertically(animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(10.dp))
                            ProviderSubtypePill(
                                emoji = "🏠",
                                title = "Landlord",
                                subtitle = "I own the properties I list",
                                subtype = "landlord",
                                isSelected = selectedSubtype == "landlord",
                                onSelect = { selectedSubtype = "landlord" }
                            )
                            Spacer(Modifier.height(8.dp))
                            ProviderSubtypePill(
                                emoji = "🤝",
                                title = "Freelancer Agent",
                                subtitle = "I list on behalf of property owners",
                                subtype = "agent",
                                isSelected = selectedSubtype == "agent",
                                onSelect = { selectedSubtype = "agent" }
                            )
                            Spacer(Modifier.height(8.dp))
                            ProviderSubtypePill(
                                emoji = "🏢",
                                title = "Brokerage",
                                subtitle = "I represent a company or agency",
                                subtype = "brokerage",
                                isSelected = selectedSubtype == "brokerage",
                                onSelect = { selectedSubtype = "brokerage" }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Tenant card with animation
            AnimatedVisibility(
                visible = tenantCardVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + 
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                RoleCard(
                    icon = Icons.Default.Key,
                    iconTint = RentOutColors.IconAmber,
                    title = "Tenant",
                    subtitle = "Find rentals & pay a small fee to unlock landlord details",
                    emoji = "🔑",
                    isSelected = selectedRole == "tenant",
                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                    onClick = { 
                        selectedRole = "tenant"
                        selectedSubtype = ""
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Button with animation - Linear Progression Button
            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) + 
                        slideInVertically(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            initialOffsetY = { 20 }
                        )
            ) {
                var isLoading by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                
                ProgressButton(
                    itemCount = if (selectedRole == "tenant" || (selectedRole == "landlord" && selectedSubtype.isNotBlank())) 1 else 0,
                    isLoading = isLoading,
                    onClick = {
                        isLoading = true
                        // 2.5-second delay synced to the 0→100% linear animation
                        coroutineScope.launch {
                            delay(2500)
                            onRoleSelected(selectedRole, selectedSubtype)
                        }
                    },
                    buttonText = "Continue →",
                    loadingText = "Loading",
                    successText = "Let's go!",
                    variant = ProgressVariant.LINEAR,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Property Provider card — shows all 3 subtypes as preview badges before tap
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderRoleCard(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pulseScale = if (isSelected) rememberPulseAnimation(0.98f, 1.02f, 2000) else 1f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "provider_card_scale"
    )

    val haloWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        animationSpec = tween(300),
        label = "provider_halo_width"
    )

    val haloColor by animateColorAsState(
        targetValue = if (isSelected) RentOutColors.Primary
        else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        else Color(0xFFE0E5ED),
        animationSpec = tween(300),
        label = "provider_halo_color"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "provider_check_scale"
    )

    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
                shadowElevation = if (isSelected) 20f else 8f
                shape = RoundedCornerShape(24.dp)
                clip = false
            }
            .border(haloWidth, haloColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp)
    ) {
        Column {
            // ── Top row: icon box + title + checkmark ─────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(RentOutColors.IconBlue.copy(alpha = if (isDark) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏘️",
                        fontSize = 30.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (isSelected) 8f else 0f
                            scaleX = if (isSelected) 1.1f else 1f
                            scaleY = if (isSelected) 1.1f else 1f
                        }
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Property Provider",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "List properties & earn — tap to choose your type",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.85f else 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = RentOutColors.Primary,
                        modifier = Modifier.size(28.dp).scale(checkScale)
                    )
                }
            }

            // ── Preview badges row ────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Landlord badge
                ProviderPreviewBadge(
                    emoji = "🏠",
                    label = "Landlord",
                    color = RentOutColors.IconBlue,
                    modifier = Modifier.weight(1f)
                )
                // Agent badge
                ProviderPreviewBadge(
                    emoji = "🤝",
                    label = "Agent",
                    color = RentOutColors.IconTeal,
                    modifier = Modifier.weight(1f)
                )
                // Brokerage badge
                ProviderPreviewBadge(
                    emoji = "🏢",
                    label = "Brokerage",
                    color = RentOutColors.IconBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Tap hint ─────────────────────────────────────────────────
            if (!isSelected) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = RentOutColors.Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Tap to select your provider type",
                        fontSize = 11.sp,
                        color = RentOutColors.Primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderPreviewBadge(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = if (isDark) 0.18f else 0.09f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProviderSubtypePill(
    emoji: String,
    title: String,
    subtitle: String,
    subtype: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pill_scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) RentOutColors.Primary else {
            if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E5ED)
        },
        animationSpec = tween(300),
        label = "pill_border_color"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        tonalElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Column {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isDark) 0.75f else 0.65f
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RentOutColors.Primary
                )
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
    
    // Micro-interaction: subtle pulse when selected
    val pulseScale = if (isSelected) rememberPulseAnimation(0.98f, 1.02f, 2000) else 1f
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed  -> 0.96f
            else       -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "role_card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) {
            12.dp
        } else {
            if (isDark) 6.dp else 8.dp
        },
        label = "role_card_elevation"
    )
    
    // Micro-interaction: emoji rotation on selection
    val emojiRotation by animateFloatAsState(
        targetValue = if (isSelected) 15f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "emoji_rotation"
    )
    
    val emojiScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "emoji_scale"
    )

    // Card always stays white (light) or surface (dark) - no color change inside
    val cardBackgroundColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    // Icon box - always use subtle icon tint, no color change
    val iconBoxColor = if (isDark) iconTint.copy(alpha = 0.25f) else iconTint.copy(alpha = 0.12f)

    // Animated checkmark scale
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )

    // Halo glow width - only outside the card
    val haloWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        animationSpec = tween(300),
        label = "halo_width"
    )

    val haloColor by animateColorAsState(
        targetValue = if (isSelected) selectedBorderColor else {
            if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE0E5ED)
        },
        animationSpec = tween(300),
        label = "halo_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
                shadowElevation = if (isSelected) 20f else 8f
                shape = RoundedCornerShape(24.dp)
                clip = false // Don't clip - let halo show outside
            }
            .border(haloWidth, haloColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconBoxColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 32.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = emojiRotation
                        scaleX = emojiScale
                        scaleY = emojiScale
                    }
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isDark) 0.85f else 0.7f
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = selectedBorderColor,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(checkScale)
                )
            }
        }
    }
}
