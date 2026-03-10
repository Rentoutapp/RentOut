package org.example.project.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
// Provider card data
// ─────────────────────────────────────────────────────────────────────────────

private data class ProviderTileData(
    val subtype:  String,
    val emoji:    String,
    val label:    String,
    val sublabel: String,
    val greeting: String,
    val color:    Color
)

// ─────────────────────────────────────────────────────────────────────────────
// Property Provider card — morphing header + size-shifting tappable tiles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderRoleCard(
    selectedSubtype: String,
    onSubtypeSelected: (String) -> Unit
) {
    val isDark        = isSystemInDarkTheme()
    val isAnySelected = selectedSubtype.isNotBlank()

    val tiles = remember {
        listOf(
            ProviderTileData("landlord",  "🏠", "Landlord",         "I own the\nproperties I list",    "👋 Welcome, Landlord!",          Color(0xFF2196F3)),
            ProviderTileData("agent",     "🤝", "Freelancer Agent", "I list on behalf\nof owners",     "👋 Welcome, Freelancer Agent!",   Color(0xFF00897B)),
            ProviderTileData("brokerage", "🏢", "Brokerage",        "I represent\na company",          "👋 Welcome, Brokerage!",          Color(0xFF7C5CBF))
        )
    }

    val selectedTile = tiles.find { it.subtype == selectedSubtype }

    // Header morphing: title & subtitle animate between default & selected values
    val headerTitle    = if (isAnySelected) selectedTile?.label    ?: "Property Provider" else "Property Provider"
    val headerSubtitle = if (isAnySelected) selectedTile?.sublabel ?: "List properties & earn money"
                         else "List properties & earn money"
    val headerEmoji    = if (isAnySelected) selectedTile?.emoji    ?: "🏘️" else "🏘️"
    val headerColor    = selectedTile?.color ?: RentOutColors.IconBlue

    val haloWidth by animateDpAsState(
        targetValue = if (isAnySelected) 2.5.dp else 1.dp,
        animationSpec = tween(300), label = "halo_w"
    )
    val haloColor by animateColorAsState(
        targetValue = if (isAnySelected) headerColor
                      else if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      else Color(0xFFE0E5ED),
        animationSpec = tween(350), label = "halo_c"
    )
    val iconBgColor by animateColorAsState(
        targetValue = headerColor.copy(alpha = if (isDark) 0.28f else 0.13f),
        animationSpec = tween(350), label = "icon_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(haloWidth, haloColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
            .padding(20.dp)
    ) {
        Column {

            // ── Morphing header ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    // AnimatedContent swaps the emoji smoothly
                    AnimatedContent(
                        targetState = headerEmoji,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.7f))
                                .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f))
                        },
                        label = "header_emoji"
                    ) { emoji ->
                        Text(text = emoji, fontSize = 26.sp)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title morphs between "Property Provider" and selected role name
                    AnimatedContent(
                        targetState = headerTitle,
                        transitionSpec = {
                            (fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 3 })
                                .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 })
                        },
                        label = "header_title"
                    ) { title ->
                        Text(
                            text = title,
                            color = if (isAnySelected) headerColor else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    // Subtitle morphs too
                    AnimatedContent(
                        targetState = headerSubtitle,
                        transitionSpec = {
                            (fadeIn(tween(280, delayMillis = 60)))
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "header_subtitle"
                    ) { subtitle ->
                        Text(
                            text = subtitle.replace("\n", " "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isDark) 0.80f else 0.62f
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Outer checkmark when any subtype is selected
                val outerCheckScale by animateFloatAsState(
                    targetValue = if (isAnySelected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "outer_check"
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint     = headerColor,
                    modifier = Modifier.size(24.dp).scale(outerCheckScale)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── 3 size-morphing tiles ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tiles.forEach { tile ->
                    val isTileSelected  = selectedSubtype == tile.subtype
                    val isSomeSelected  = isAnySelected
                    // Selected tile gets weight 1.8, unselected tiles share 0.6 each
                    val tileWeight by animateFloatAsState(
                        targetValue = when {
                            isTileSelected -> 1.8f
                            isSomeSelected -> 0.6f
                            else           -> 1f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "tile_weight_${tile.subtype}"
                    )
                    ProviderSubtypeTile(
                        tile       = tile,
                        isSelected = isTileSelected,
                        anySelected = isSomeSelected,
                        modifier   = Modifier.weight(tileWeight),
                        onClick    = { onSubtypeSelected(tile.subtype) }
                    )
                }
            }

            // ── Bottom greeting (centred) / hint ──────────────────────────
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hint — shown when nothing is selected
                AnimatedVisibility(
                    visible = !isAnySelected,
                    enter   = fadeIn(tween(220)),
                    exit    = fadeOut(tween(180))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint     = RentOutColors.Primary.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text  = "Tap a tile to select your provider type",
                            fontSize = 11.sp,
                            color = RentOutColors.Primary.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Greeting — shown when a tile is selected, centred inside the card
                AnimatedVisibility(
                    visible = isAnySelected,
                    enter   = fadeIn(tween(280)) + expandVertically(tween(260)),
                    exit    = fadeOut(tween(180)) + shrinkVertically(tween(200))
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = headerColor.copy(alpha = 0.11f)
                    ) {
                        Text(
                            text      = selectedTile?.greeting ?: "",
                            fontSize  = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color     = headerColor,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .padding(horizontal = 18.dp, vertical = 7.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual subtype tile — enlarges when selected, shrinks when not
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderSubtypeTile(
    tile:        ProviderTileData,
    isSelected:  Boolean,
    anySelected: Boolean,
    modifier:    Modifier = Modifier,
    onClick:     () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_${tile.subtype}"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) (if (isDark) 0.32f else 0.14f)
                      else            (if (isDark) 0.09f else 0.05f),
        animationSpec = tween(280), label = "bg_${tile.subtype}"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.8.dp,
        animationSpec = tween(220), label = "bw_${tile.subtype}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tile.color else tile.color.copy(alpha = 0.20f),
        animationSpec = tween(260), label = "bc_${tile.subtype}"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "check_${tile.subtype}"
    )
    val emojiSize by animateFloatAsState(
        targetValue = if (isSelected) 28f else if (anySelected) 18f else 24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "emoji_sz_${tile.subtype}"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (!anySelected || isSelected) 1f else 0.45f,
        animationSpec = tween(220), label = "lbl_alpha_${tile.subtype}"
    )
    val labelSize by animateFloatAsState(
        targetValue = if (isSelected) 11f else if (anySelected) 8f else 10f,
        animationSpec = tween(220), label = "lbl_sz_${tile.subtype}"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(tile.color.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Checkmark — top right, springs in on select
            Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint     = tile.color,
                    modifier = Modifier
                        .size(15.dp)
                        .scale(checkScale)
                        .align(Alignment.TopEnd)
                )
            }

            // Emoji — size-animates
            Text(
                text     = tile.emoji,
                fontSize = emojiSize.sp
            )

            Spacer(Modifier.height(5.dp))

            // Label — fades/shrinks on unselected when another is active
            Text(
                text       = tile.label,
                fontSize   = labelSize.sp,
                fontWeight = FontWeight.Bold,
                color      = (if (isSelected) tile.color
                              else MaterialTheme.colorScheme.onSurface).copy(alpha = labelAlpha),
                textAlign  = TextAlign.Center
            )

            // Sublabel — only show on selected tile
            AnimatedVisibility(
                visible = isSelected,
                enter   = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit    = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Text(
                    text      = tile.sublabel.replace("\n", " "),
                    fontSize  = 9.sp,
                    color     = tile.color.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(top = 2.dp)
                )
            }
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
