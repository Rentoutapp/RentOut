package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor

// ─────────────────────────────────────────────────────────────────────────────
// PropertyDetailScreen — Tenant view
// Mirrors the LandlordPropertyDetailScreen layout: full hero, thumbnail strip,
// sliding white card, tab bar (Overview / Amenities / Contact), sticky bottom
// bar with price + unlock/call actions.
// Using TenantNavy & TenantNavyLight as base colors for consistency
// ─────────────────────────────────────────────────────────────────────────────

// Base colors from TenantHomeScreen for consistent tenant UI
private val DetailNavy = Color(0xFF0F2A4A)
private val DetailNavyLight = Color(0xFF1A3F6F)

private enum class TenantDetailTab { OVERVIEW, AMENITIES, CONTACT, DIRECTIONS }

// WhatsApp Icon using official WhatsApp logo path
@Composable
private fun rememberWhatsAppIcon(): ImageVector {
    return remember {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "WhatsApp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                // WhatsApp logo path
                moveTo(17.472f, 14.382f)
                curveToRelative(-0.297f, -0.149f, -1.758f, -0.867f, -2.03f, -0.967f)
                curveToRelative(-0.273f, -0.099f, -0.471f, -0.149f, -0.67f, 0.15f)
                curveToRelative(-0.197f, 0.297f, -0.767f, 0.966f, -0.94f, 1.164f)
                curveToRelative(-0.173f, 0.199f, -0.347f, 0.223f, -0.644f, 0.075f)
                curveToRelative(-0.297f, -0.15f, -1.255f, -0.463f, -2.39f, -1.475f)
                curveToRelative(-0.883f, -0.788f, -1.48f, -1.761f, -1.653f, -2.059f)
                curveToRelative(-0.173f, -0.297f, -0.018f, -0.458f, 0.13f, -0.606f)
                curveToRelative(0.134f, -0.133f, 0.297f, -0.347f, 0.446f, -0.521f)
                curveToRelative(0.151f, -0.172f, 0.2f, -0.296f, 0.3f, -0.495f)
                curveToRelative(0.099f, -0.198f, 0.05f, -0.372f, -0.025f, -0.521f)
                curveToRelative(-0.075f, -0.148f, -0.669f, -1.611f, -0.916f, -2.206f)
                curveToRelative(-0.242f, -0.579f, -0.487f, -0.5f, -0.669f, -0.51f)
                curveToRelative(-0.173f, -0.008f, -0.371f, -0.01f, -0.57f, -0.01f)
                curveToRelative(-0.198f, 0f, -0.52f, 0.074f, -0.792f, 0.372f)
                curveToRelative(-0.272f, 0.297f, -1.04f, 1.016f, -1.04f, 2.479f)
                curveToRelative(0f, 1.462f, 1.065f, 2.875f, 1.213f, 3.074f)
                curveToRelative(0.149f, 0.198f, 2.096f, 3.2f, 5.077f, 4.487f)
                curveToRelative(0.709f, 0.306f, 1.262f, 0.489f, 1.694f, 0.625f)
                curveToRelative(0.712f, 0.227f, 1.36f, 0.195f, 1.871f, 0.118f)
                curveToRelative(0.571f, -0.085f, 1.758f, -0.719f, 2.006f, -1.413f)
                curveToRelative(0.248f, -0.694f, 0.248f, -1.289f, 0.173f, 1.413f)
                curveToRelative(-0.074f, -0.124f, -0.272f, -0.198f, -0.57f, -0.347f)
                moveTo(12.04f, 21.73f)
                lineToRelative(-0.004f, 0f)
                curveToRelative(-1.776f, 0f, -3.524f, -0.477f, -5.055f, -1.377f)
                lineToRelative(-0.362f, -0.215f)
                lineToRelative(-3.754f, 0.984f)
                lineToRelative(1.001f, -3.656f)
                lineToRelative(-0.237f, -0.375f)
                arcToRelative(9.869f, 9.869f, 0f, false, true, -1.513f, -5.26f)
                curveToRelative(0f, -5.45f, 4.436f, -9.884f, 9.888f, -9.884f)
                curveToRelative(2.64f, 0f, 5.122f, 1.03f, 6.988f, 2.898f)
                arcToRelative(9.825f, 9.825f, 0f, false, true, 2.893f, 6.994f)
                curveToRelative(-0.003f, 5.45f, -4.437f, 9.884f, -9.885f, 9.884f)
                moveTo(20.52f, 3.449f)
                arcTo(11.885f, 11.885f, 0f, false, false, 12.043f, 0f)
                curveTo(5.463f, 0f, 0.104f, 5.359f, 0.101f, 11.937f)
                curveToRelative(-0.001f, 2.101f, 0.548f, 4.153f, 1.59f, 5.945f)
                lineTo(0f, 24f)
                lineToRelative(6.335f, -1.652f)
                arcToRelative(11.946f, 11.946f, 0f, false, false, 5.713f, 1.456f)
                horizontalLineToRelative(0.005f)
                curveToRelative(6.585f, 0f, 11.942f, -5.359f, 11.945f, -11.94f)
                arcToRelative(11.859f, 11.859f, 0f, false, false, -3.48f, -8.413f)
                close()
            }
        }.build()
    }
}

@Composable
fun PropertyDetailScreen(
    property: Property,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    onCall: (String) -> Unit = {},
    onWhatsApp: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Back button animation
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.82f else 1f, tween(180), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f, tween(180), label = "br")

    // Tab state
    var selectedTab by remember { mutableStateOf(TenantDetailTab.OVERVIEW) }

    // Image list — prefer full imageUrls, fall back to imageUrl
    val images = remember(property.id) {
        property.imageUrls.ifEmpty {
            listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
        }
    }
    var selectedImageIndex by remember { mutableStateOf(0) }
    val heroImageUrl = images.getOrElse(selectedImageIndex) { property.imageUrl }

    // Hero parallax offset
    val heroOffset by remember { derivedStateOf { scrollState.value * 0.35f } }

    // Card entrance animation
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // ── Hero image ────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                AsyncImage(
                    model = heroImageUrl.ifBlank { null },
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { translationY = heroOffset }
                )
                // Gradient overlay
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f   to Color.Black.copy(alpha = 0.35f),
                            0.4f to Color.Transparent,
                            1f   to Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
                // Thumbnail strip
                if (images.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(images) { index, url ->
                            val isSelected = index == selectedImageIndex
                            val thumbSize by animateDpAsState(
                                targetValue = if (isSelected) 68.dp else 58.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "thumb_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedImageIndex = index }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(DetailNavy.copy(alpha = 0.25f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Sliding white card — overlaps hero ────────────────────────────
            AnimatedVisibility(
                visible = cardVisible,
                enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
                        + fadeIn(tween(400))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().offset(y = (-24).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {

                        // ── Property type + badges row ───────────────────────
                        // Provider subtype badge — full width row
                        val providerLabel = when (property.providerSubtype) {
                            "agent"     -> "🤝 Listed by Agent"
                            "brokerage" -> "🏢 Listed by Brokerage"
                            else        -> "🏠 Listed by Landlord"
                        }
                        val providerColor = when (property.providerSubtype) {
                            "agent"     -> Color(0xFF00897B)
                            "brokerage" -> Color(0xFF7C5CBF)
                            else        -> MaterialTheme.colorScheme.primary
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = providerColor.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = providerLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = providerColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Apartment, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    property.propertyType.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (property.isVerified) VerifiedBadge(property.providerSubtype)
                                AvailabilityBadge(property.isAvailable)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Title ────────────────────────────────────────────
                        Text(
                            property.title,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        // ── Address row — locked until unlocked ───────────────
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn, null,
                                tint = RentOutColors.IconRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            if (isUnlocked) {
                                Text(
                                    property.location.ifBlank { property.city },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    property.city,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Lock, null,
                                    tint = RentOutColors.IconAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Full address locked",
                                    fontSize = 12.sp,
                                    color = RentOutColors.IconAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Tab bar ──────────────────────────────────────────
                        TenantDetailTabBar(
                            selected = selectedTab,
                            onSelect = { selectedTab = it }
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── Tab content ──────────────────────────────────────
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                            label = "tenant_tab"
                        ) { tab ->
                            when (tab) {
                                TenantDetailTab.OVERVIEW  -> TenantOverviewContent(property)
                                TenantDetailTab.AMENITIES -> TenantAmenitiesContent(property)
                                TenantDetailTab.CONTACT   -> TenantContactContent(
                                    property   = property,
                                    isUnlocked = isUnlocked,
                                    onUnlock   = onUnlock,
                                    onCall     = onCall,
                                    onWhatsApp = onWhatsApp
                                )
                                TenantDetailTab.DIRECTIONS -> TenantDirectionsContent(
                                    property   = property,
                                    isUnlocked = isUnlocked,
                                    onUnlock   = onUnlock
                                )
                            }
                        }

                        // Bottom spacer — room for sticky bar
                        Spacer(Modifier.height(110.dp))
                    }
                }
            }

        }

        // ── Floating back button — fixed, never scrolls ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .graphicsLayer { scaleX = backScale; scaleY = backScale; rotationZ = backRotation },
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { backPressed = true; onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }
        }

        // ── Sticky bottom bar — price left, unlock/call right ────────────────
        val surfaceColor = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.00f to surfaceColor.copy(alpha = 0.00f),
                        0.20f to surfaceColor.copy(alpha = 0.25f),
                        0.42f to surfaceColor.copy(alpha = 0.60f),
                        0.62f to surfaceColor.copy(alpha = 0.82f),
                        0.75f to surfaceColor.copy(alpha = 0.94f),
                        1.00f to surfaceColor.copy(alpha = 1.00f)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Price + deposit column (left)
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$${property.price.toInt()}",
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = RentOutColors.Secondary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "/mth",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = RentOutColors.Secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (property.securityDeposit > 0) {
                        Text(
                            "Deposit: $${property.securityDeposit.toInt()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action button — hidden on the Contact tab (the tab has its own unlock button,
                // so showing it here too is redundant). Animates in/out smoothly.
                AnimatedVisibility(
                    visible = selectedTab != TenantDetailTab.CONTACT && selectedTab != TenantDetailTab.DIRECTIONS,
                    enter = fadeIn(tween(250)) + slideInHorizontally(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        initialOffsetX = { it }
                    ),
                    exit  = fadeOut(tween(200)) + slideOutHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetX = { it }
                    )
                ) {
                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Call button
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(RentOutColors.StatusApproved)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCall(property.contactNumber) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, "Call", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        // WhatsApp button
                        Box(
                            modifier = Modifier
                                .height(54.dp)
                                .shadow(6.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF25D366)) // Official WhatsApp green
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onWhatsApp(property.contactNumber) }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = rememberWhatsAppIcon(),
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (property.isAvailable) RentOutColors.Secondary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = property.isAvailable
                            ) { if (property.isAvailable) onUnlock() }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (property.isAvailable) Icons.Default.LockOpen else Icons.Default.Lock,
                                null,
                                tint = if (property.isAvailable) Color.White
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (property.isAvailable) "Unlock — \$10"
                                else "Unavailable",
                                color = if (property.isAvailable) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } // end if (isUnlocked)
                } // end AnimatedVisibility (action button)
            }
        }
    }
}

// ── Tab bar ───────────────────────────────────────────────────────────────────
@Composable
private fun TenantDetailTabBar(
    selected: TenantDetailTab,
    onSelect: (TenantDetailTab) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // Use primary color in dark mode (visible on dark bg), DetailNavy in light mode
    val activeColor = if (isDark) MaterialTheme.colorScheme.primary else DetailNavy

    val tabs = listOf(
        TenantDetailTab.OVERVIEW   to "Overview",
        TenantDetailTab.AMENITIES  to "Amenities",
        TenantDetailTab.CONTACT    to "Contact",
        TenantDetailTab.DIRECTIONS to "Directions"
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        tabs.forEach { (tab, label) ->
            val isSelected = selected == tab
            val tabTextColor by animateColorAsState(
                targetValue = if (isSelected) activeColor
                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                animationSpec = tween(200),
                label = "tab_color_$label"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(tab) }
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = tabTextColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(visible = isSelected) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(activeColor)
                    )
                }
            }
        }
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

// ── Overview tab ──────────────────────────────────────────────────────────────
@Composable
private fun TenantOverviewContent(property: Property) {
    Column {
        // Stats strip — beds / baths / type / deposit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (property.rooms > 0) {
                TenantOverviewStat(Icons.Default.Bed, "${property.rooms}", "Beds", RentOutColors.IconBlue)
                TenantVerticalDivider()
            }
            if (property.bathrooms > 0) {
                TenantOverviewStat(Icons.Default.Bathtub, "${property.bathrooms}", "Baths", RentOutColors.IconTeal)
                TenantVerticalDivider()
            }
            TenantOverviewStat(Icons.Default.HomeWork, property.propertyType, "Type", RentOutColors.IconPurple)
            if (property.securityDeposit > 0 && !property.depositNotApplicable) {
                TenantVerticalDivider()
                TenantOverviewStat(Icons.Default.Shield, "$${property.securityDeposit.toInt()}", "Deposit", RentOutColors.IconAmber)
            } else if (property.depositNotApplicable) {
                TenantVerticalDivider()
                TenantOverviewStat(Icons.Default.Shield, "None", "Deposit", RentOutColors.Tertiary)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Property Details & Location heading ──────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp).height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RentOutColors.Primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Property Details & Location",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        // Classification & Location Type chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (property.classification.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RentOutColors.Primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, null, tint = RentOutColors.Primary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(property.classification, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.Primary)
                    }
                }
            }
            if (property.locationType.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RentOutColors.IconPurple.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationCity, null, tint = RentOutColors.IconPurple, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(property.locationType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.IconPurple)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Detail rows — room quantity, bathroom type, kitchen
        // Each shows: icon  |  value (no redundant label prefix)
        if (property.roomQuantity.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MeetingRoom, null, tint = RentOutColors.IconBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("${property.roomQuantity} Room(s)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
        }

        // Bathroom type — icon only, no "Bathroom:" label prefix
        if (property.bathroomType.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bathtub, null, tint = RentOutColors.IconTeal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(property.bathroomType, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
        }

        // Kitchen details — derived from hasSharedKitchen + kitchenCount
        val kitchenDetail: String? = when {
            property.kitchenCount > 0 && property.hasSharedKitchen ->
                "${property.kitchenCount} Shared Kitchen${if (property.kitchenCount > 1) "s" else ""}"
            property.kitchenCount > 0 ->
                "${property.kitchenCount} Kitchen${if (property.kitchenCount > 1) "s" else ""}"
            property.hasSharedKitchen -> "Shared Kitchen"
            property.amenities.any { it.contains("kitchen", ignoreCase = true) } ->
                property.amenities.first { it.contains("kitchen", ignoreCase = true) }
            else -> null
        }
        if (kitchenDetail != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Kitchen, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(kitchenDetail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
        }

        // Bills inclusive / exclusive
        if (property.billsInclusive.isNotEmpty() || property.billsExclusive.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Bills", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            if (property.billsInclusive.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.Tertiary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Tertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Included: ${property.billsInclusive.joinToString(" · ")}", fontSize = 12.sp, color = RentOutColors.Tertiary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }
            if (property.billsExclusive.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.IconAmber.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cancel, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Excluded: ${property.billsExclusive.joinToString(" · ")}", fontSize = 12.sp, color = RentOutColors.IconAmber, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Availability
        if (property.availabilityDate.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Primary.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = RentOutColors.Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Available from: ${property.availabilityDate}", fontSize = 12.sp, color = RentOutColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Tertiary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Available Now", fontSize = 12.sp, color = RentOutColors.Tertiary, fontWeight = FontWeight.Bold)
            }
        }

        // Proximity facilities
        if (property.proximityFacilities.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Nearby", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            val isDark = isSystemInDarkTheme()
            // Use theme-aware colors so dots and text are visible in both light & dark mode
            val nearbyDotColor = if (isDark) RentOutColors.PrimaryLight else RentOutColors.Primary
            val nearbyBgColor  = if (isDark) RentOutColors.Primary.copy(alpha = 0.18f)
                                 else        RentOutColors.Primary.copy(alpha = 0.07f)
            val chunked = property.proximityFacilities.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { facility ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(nearbyBgColor)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filled dot — clearly visible in both light and dark mode
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(nearbyDotColor)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                facility,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Description", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            property.description.ifBlank { "No description provided." },
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp
        )
    }
}

@Composable
private fun TenantOverviewStat(icon: ImageVector, value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TenantVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp).height(40.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

// ── Amenities tab ─────────────────────────────────────────────────────────────
@Composable
private fun TenantAmenitiesContent(property: Property) {
    if (property.amenities.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Info, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No amenities listed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        val chunked = property.amenities.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { amenity ->
                        AmenityChip(amenity = amenity, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun amenityIcon(amenity: String): ImageVector {
    return when {
        amenity.contains("parking", ignoreCase = true) -> Icons.Default.DirectionsCar
        amenity.contains("pool", ignoreCase = true) -> Icons.Default.Pool
        amenity.contains("gym", ignoreCase = true) || amenity.contains("fitness", ignoreCase = true) -> Icons.Default.FitnessCenter
        amenity.contains("wifi", ignoreCase = true) || amenity.contains("internet", ignoreCase = true) -> Icons.Default.Wifi
        amenity.contains("security", ignoreCase = true) || amenity.contains("guard", ignoreCase = true) -> Icons.Default.Security
        amenity.contains("garden", ignoreCase = true) || amenity.contains("yard", ignoreCase = true) -> Icons.Default.Park
        amenity.contains("pet", ignoreCase = true) -> Icons.Default.Pets
        amenity.contains("borehole", ignoreCase = true) || amenity.contains("water", ignoreCase = true) -> Icons.Default.WaterDrop
        amenity.contains("power", ignoreCase = true) || amenity.contains("solar", ignoreCase = true) || amenity.contains("backup", ignoreCase = true) -> Icons.Default.ElectricBolt
        amenity.contains("laundry", ignoreCase = true) || amenity.contains("wash", ignoreCase = true) -> Icons.Default.LocalLaundryService
        amenity.contains("balcony", ignoreCase = true) -> Icons.Default.Balcony
        amenity.contains("air", ignoreCase = true) || amenity.contains("ac", ignoreCase = true) -> Icons.Default.AcUnit
        amenity.contains("kitchen", ignoreCase = true) -> Icons.Default.Kitchen
        amenity.contains("furnished", ignoreCase = true) -> Icons.Default.Chair
        amenity.contains("elevator", ignoreCase = true) || amenity.contains("lift", ignoreCase = true) -> Icons.Default.Elevator
        else -> Icons.Default.CheckCircle
    }
}

private fun amenityColor(amenity: String): Color {
    return when {
        amenity.contains("parking", ignoreCase = true) -> Color(0xFF4A90E2)
        amenity.contains("pool", ignoreCase = true) -> Color(0xFF00BCD4)
        amenity.contains("gym", ignoreCase = true) || amenity.contains("fitness", ignoreCase = true) -> Color(0xFFE91E63)
        amenity.contains("wifi", ignoreCase = true) || amenity.contains("internet", ignoreCase = true) -> Color(0xFF9C27B0)
        amenity.contains("security", ignoreCase = true) -> Color(0xFF4CAF50)
        amenity.contains("garden", ignoreCase = true) || amenity.contains("yard", ignoreCase = true) -> Color(0xFF8BC34A)
        amenity.contains("pet", ignoreCase = true) -> Color(0xFFFF9800)
        amenity.contains("borehole", ignoreCase = true) || amenity.contains("water", ignoreCase = true) -> Color(0xFF03A9F4)
        amenity.contains("power", ignoreCase = true) || amenity.contains("solar", ignoreCase = true) || amenity.contains("backup", ignoreCase = true) -> Color(0xFFFFC107)
        amenity.contains("laundry", ignoreCase = true) -> Color(0xFF00BCD4)
        amenity.contains("balcony", ignoreCase = true) -> Color(0xFF009688)
        amenity.contains("air", ignoreCase = true) || amenity.contains("ac", ignoreCase = true) -> Color(0xFF29B6F6)
        amenity.contains("kitchen", ignoreCase = true) -> Color(0xFFFF7043)
        amenity.contains("furnished", ignoreCase = true) -> Color(0xFF795548)
        amenity.contains("elevator", ignoreCase = true) -> Color(0xFF607D8B)
        else -> Color(0xFF4CAF50)
    }
}

@Composable
private fun AmenityChip(amenity: String, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val icon = amenityIcon(amenity)
    val color = amenityColor(amenity)

    val displayName = amenity
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) color.copy(alpha = 0.20f)
                else color.copy(alpha = 0.10f)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.35f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = displayName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// ── Contact tab ───────────────────────────────────────────────────────────────
@Composable
private fun TenantContactContent(
    property: Property,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit
) {
    val contactAlpha by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "contact_reveal"
    )

    val isAgent     = property.providerSubtype == "agent"
    val isBrokerage = property.providerSubtype == "brokerage"
    val agentColor     = Color(0xFF00897B)
    val brokerageColor = Color(0xFF7C5CBF)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                RentOutColors.StatusApproved.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (isUnlocked) {
                when {
                    // ── BROKERAGE: broker name + contact, then brokerage address + contact ──
                    isBrokerage -> {
                        // Broker name
                        ContactRevealRow(
                            label = "Broker",
                            value = property.brokerName.ifBlank { property.landlordName },
                            icon = Icons.Default.Person,
                            iconBg = brokerageColor.copy(alpha = 0.12f),
                            iconTint = brokerageColor,
                            alpha = contactAlpha
                        )
                        Spacer(Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))
                        // Broker contact
                        ContactRevealRow(
                            label = "Broker Contact",
                            value = property.brokerContactNumber,
                            icon = Icons.Default.Call,
                            iconBg = brokerageColor.copy(alpha = 0.15f),
                            iconTint = brokerageColor,
                            alpha = contactAlpha,
                            isPhone = true
                        )
                        Spacer(Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))
                        // Brokerage address
                        if (property.brokerageAddress.isNotBlank()) {
                            ContactRevealRow(
                                label = "Brokerage Address",
                                value = property.brokerageAddress,
                                icon = Icons.Default.Business,
                                iconBg = brokerageColor.copy(alpha = 0.12f),
                                iconTint = brokerageColor,
                                alpha = contactAlpha
                            )
                            Spacer(Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(Modifier.height(14.dp))
                        }
                        // Brokerage contact
                        ContactRevealRow(
                            label = "Brokerage Contact",
                            value = property.brokerageContactNumber,
                            icon = Icons.Default.Call,
                            iconBg = brokerageColor.copy(alpha = 0.15f),
                            iconTint = brokerageColor,
                            alpha = contactAlpha,
                            isPhone = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RentOutPrimaryButton(
                                text = "📞 Call Broker",
                                onClick = { onCall(property.brokerContactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                            RentOutSecondaryButton(
                                text = "💬 WhatsApp",
                                onClick = { onWhatsApp(property.brokerContactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── AGENT: agent name + contact, then landlord address + contact ──
                    isAgent -> {
                        // Agent name
                        ContactRevealRow(
                            label = "Agent",
                            value = property.agentName.ifBlank { property.landlordName },
                            icon = Icons.Default.Person,
                            iconBg = agentColor.copy(alpha = 0.12f),
                            iconTint = agentColor,
                            alpha = contactAlpha
                        )
                        Spacer(Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))
                        // Agent contact
                        ContactRevealRow(
                            label = "Agent Contact",
                            value = property.agentContactNumber,
                            icon = Icons.Default.Call,
                            iconBg = agentColor.copy(alpha = 0.15f),
                            iconTint = agentColor,
                            alpha = contactAlpha,
                            isPhone = true
                        )
                        Spacer(Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))
                        // Property address (landlord's)
                        if (property.location.isNotBlank()) {
                            ContactRevealRow(
                                label = "Property Address",
                                value = property.location,
                                icon = Icons.Default.LocationOn,
                                iconBg = DetailNavy.copy(alpha = 0.10f),
                                iconTint = DetailNavy,
                                alpha = contactAlpha
                            )
                            Spacer(Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(Modifier.height(14.dp))
                        }
                        // Landlord contact
                        ContactRevealRow(
                            label = "Landlord Contact",
                            value = property.contactNumber,
                            icon = Icons.Default.Call,
                            iconBg = RentOutColors.StatusApproved.copy(alpha = 0.15f),
                            iconTint = RentOutColors.StatusApproved,
                            alpha = contactAlpha,
                            isPhone = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RentOutPrimaryButton(
                                text = "📞 Call Agent",
                                onClick = { onCall(property.agentContactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                            RentOutSecondaryButton(
                                text = "💬 WhatsApp",
                                onClick = { onWhatsApp(property.agentContactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── LANDLORD: landlord name + property address + contact ──
                    else -> {
                        if (property.landlordName.isNotBlank()) {
                            ContactRevealRow(
                                label = "Landlord",
                                value = property.landlordName,
                                icon = Icons.Default.Person,
                                iconBg = DetailNavy.copy(alpha = 0.12f),
                                iconTint = DetailNavy,
                                alpha = contactAlpha
                            )
                            Spacer(Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(Modifier.height(14.dp))
                        }
                        if (property.location.isNotBlank()) {
                            ContactRevealRow(
                                label = "Property Address",
                                value = property.location,
                                icon = Icons.Default.LocationOn,
                                iconBg = DetailNavy.copy(alpha = 0.10f),
                                iconTint = DetailNavy,
                                alpha = contactAlpha
                            )
                            Spacer(Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(Modifier.height(14.dp))
                        }
                        ContactRevealRow(
                            label = "Contact Number",
                            value = property.contactNumber,
                            icon = Icons.Default.Call,
                            iconBg = RentOutColors.StatusApproved.copy(alpha = 0.15f),
                            iconTint = RentOutColors.StatusApproved,
                            alpha = contactAlpha,
                            isPhone = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RentOutPrimaryButton(
                                text = "📞 Call",
                                onClick = { onCall(property.contactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                            RentOutSecondaryButton(
                                text = "💬 WhatsApp",
                                onClick = { onWhatsApp(property.contactNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // ── LOCKED state — generic masked rows ────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(RentOutColors.IconAmber.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            when {
                                isBrokerage -> "Broker"
                                isAgent     -> "Agent"
                                else        -> "Landlord"
                            },
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "••••••••••••",
                            fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(RentOutColors.IconAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Contact Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "•••••••••••••",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (property.isAvailable) {
                    RentOutPrimaryButton(
                        text = "🔓 Unlock Contact — $10",
                        onClick = onUnlock,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RentOutColors.StatusRejected.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "🔴 This property is no longer available",
                            modifier = Modifier.padding(12.dp),
                            color = RentOutColors.StatusRejected,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable contact reveal row ───────────────────────────────────────────────
@Composable
private fun ContactRevealRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    alpha: Float,
    isPhone: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value.ifBlank { "—" },
                fontSize = if (isPhone) 20.sp else 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

// ── Directions tab ────────────────────────────────────────────────────────────
@Composable
private fun TenantDirectionsContent(
    property: Property,
    isUnlocked: Boolean,
    onUnlock: () -> Unit
) {
    val hasCoords = property.latitude != 0.0 || property.longitude != 0.0

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Section header ────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp).height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RentOutColors.Primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Directions to Property",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "Navigate from your current location to this property using GPS coordinates logged by the landlord.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(16.dp))

        if (!isUnlocked) {
            // ── Locked state ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing lock icon
                    var pulse by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { pulse = true }
                    val lockScale by animateFloatAsState(
                        targetValue = if (pulse) 1.08f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "lock_pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(lockScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        RentOutColors.IconAmber.copy(alpha = 0.25f),
                                        RentOutColors.IconAmber.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = RentOutColors.IconAmber,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Directions Locked",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Unlock this property for ${'$'}10 to access GPS coordinates and get turn-by-turn directions from your current location.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // What's included chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(
                            Icons.Default.LocationOn to "GPS Coords",
                            Icons.Default.Map        to "Live Map",
                            Icons.Default.Navigation to "Navigation"
                        ).forEach { (icon, label) ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = RentOutColors.Primary.copy(alpha = 0.10f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, null, tint = RentOutColors.Primary, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.Primary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Unlock button with press animation
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "unlock_btn_scale"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(btnScale)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(RentOutColors.Primary, RentOutColors.Secondary)
                                )
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = property.isAvailable
                            ) { if (property.isAvailable) onUnlock() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(
                                "Unlock Directions — ${'$'}10",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "One-time payment · Unlocks contacts & directions",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

        } else {
            // ── Unlocked state ────────────────────────────────────────────────

            // Unlocked badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = RentOutColors.StatusApproved.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = RentOutColors.StatusApproved,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Property unlocked · Full directions available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RentOutColors.StatusApproved
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (hasCoords) {
                // Coordinates info card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                null,
                                tint = RentOutColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "GPS Coordinates",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = RentOutColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Latitude", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "%.6f".format(property.latitude),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RentOutColors.Primary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = RentOutColors.IconTeal.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Longitude", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "%.6f".format(property.longitude),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RentOutColors.IconTeal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Map section heading
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, null, tint = RentOutColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Interactive Map",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RentOutColors.Primary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            "Tap to expand",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RentOutColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // DirectionsMapView — Android: full Google Maps + expandable dialog; iOS: placeholder + navigate button
                DirectionsMapView(
                    propertyLat      = property.latitude,
                    propertyLng      = property.longitude,
                    propertyTitle    = property.title,
                    modifier         = Modifier.fillMaxWidth(),
                    onOpenNavigation = { /* navigation launched inside the component */ }
                )

            } else {
                // No coordinates stored for this property
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = RentOutColors.IconAmber.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOff, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "No coordinates available",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "The landlord has not yet logged GPS coordinates for this property.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
