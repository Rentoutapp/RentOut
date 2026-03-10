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

            // Property Provider card — subtypes are directly clickable inside
            AnimatedVisibility(
                visible = landlordCardVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                ProviderRoleCard(
                    selectedSubtype = selectedSubtype,
                    onSubtypeSelected = { subtype ->
                        selectedRole    = "landlord"
                        selectedSubtype = subtype
                    }
                )
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
// Property Provider card — 3 directly-tappable subtype badges inside the card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderRoleCard(
    selectedSubtype: String,
    onSubtypeSelected: (String) -> Unit
) {
    val isDark   = isSystemInDarkTheme()
    val isAnySelected = selectedSubtype.isNotBlank()

    val haloWidth by animateDpAsState(
        targetValue = if (isAnySelected) 3.dp else 1.dp,
        animationSpec = tween(300), label = "provider_halo_width"
    )
    val haloColor by animateColorAsState(
        targetValue = if (isAnySelected) RentOutColors.Primary
                      else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      else Color(0xFFE0E5ED),
        animationSpec = tween(300), label = "provider_halo_color"
    )
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    // Greeting text shown when a subtype is selected
    val greetingText = when (selectedSubtype) {
        "landlord"  -> "👋 Welcome, Landlord!"
        "agent"     -> "👋 Welcome, Freelancer Agent!"
        "brokerage" -> "👋 Welcome, Brokerage!"
        else        -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(haloWidth, haloColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .padding(22.dp)
    ) {
        Column {
            // ── Header row ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RentOutColors.IconBlue.copy(alpha = if (isDark) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏘️",
                        fontSize = 28.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (isAnySelected) 8f else 0f
                            scaleX    = if (isAnySelected) 1.1f else 1f
                            scaleY    = if (isAnySelected) 1.1f else 1f
                        }
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Property Provider",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "List properties & earn — choose your type below",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isDark) 0.85f else 0.65f
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 3 clickable subtype tiles ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderSubtypeTile(
                    emoji    = "🏠",
                    label    = "Landlord",
                    sublabel = "I own the\nproperties I list",
                    color    = RentOutColors.IconBlue,
                    isSelected = selectedSubtype == "landlord",
                    modifier = Modifier.weight(1f),
                    onClick  = { onSubtypeSelected("landlord") }
                )
                ProviderSubtypeTile(
                    emoji    = "🤝",
                    label    = "Agent",
                    sublabel = "I list on behalf\nof owners",
                    color    = RentOutColors.IconTeal,
                    isSelected = selectedSubtype == "agent",
                    modifier = Modifier.weight(1f),
                    onClick  = { onSubtypeSelected("agent") }
                )
                ProviderSubtypeTile(
                    emoji    = "🏢",
                    label    = "Brokerage",
                    sublabel = "I represent\na company",
                    color    = Color(0xFF7C5CBF),
                    isSelected = selectedSubtype == "brokerage",
                    modifier = Modifier.weight(1f),
                    onClick  = { onSubtypeSelected("brokerage") }
                )
            }

            // ── Greeting / hint row ───────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            AnimatedVisibility(
                visible = isAnySelected,
                enter   = fadeIn(tween(300)) + expandVertically(),
                exit    = fadeOut(tween(200)) + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RentOutColors.Primary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = greetingText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RentOutColors.Primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = !isAnySelected,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint   = RentOutColors.Primary.copy(alpha = 0.55f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Tap a tile to select your provider type",
                        fontSize = 11.sp,
                        color = RentOutColors.Primary.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual subtype tile — tappable, animated checkmark, colour glow on select
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderSubtypeTile(
    emoji:      String,
    label:      String,
    sublabel:   String,
    color:      Color,
    isSelected: Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) (if (isDark) 0.30f else 0.13f) else (if (isDark) 0.12f else 0.06f),
        animationSpec = tween(250), label = "tile_bg"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(200), label = "tile_border_w"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else color.copy(alpha = 0.25f),
        animationSpec = tween(250), label = "tile_border_c"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tile_check"
    )
    val emojiScale by animateFloatAsState(
        targetValue = if (isSelected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_emoji_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Checkmark — top right corner
            Box(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.fillMaxWidth())
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint     = color,
                    modifier = Modifier
                        .size(16.dp)
                        .scale(checkScale)
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(Modifier.height(2.dp))

            // Emoji
            Text(
                text = emoji,
                fontSize = 26.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = emojiScale; scaleY = emojiScale
                }
            )

            Spacer(Modifier.height(6.dp))

            // Label
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(3.dp))

            // Sublabel
            Text(
                text = sublabel,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.7f else 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
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
