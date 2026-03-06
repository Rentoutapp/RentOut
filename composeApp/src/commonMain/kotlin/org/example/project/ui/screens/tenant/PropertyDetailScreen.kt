package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

// ─────────────────────────────────────────────────────────────────────────────
// PropertyDetailScreen — Tenant view
// Mirrors the LandlordPropertyDetailScreen layout: full hero, thumbnail strip,
// sliding white card, tab bar (Overview / Amenities / Contact), sticky bottom
// bar with price + unlock/call actions.
// ─────────────────────────────────────────────────────────────────────────────

private enum class TenantDetailTab { OVERVIEW, AMENITIES, CONTACT }

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
                                            .background(RentOutColors.Primary.copy(alpha = 0.25f))
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
                                    tint = RentOutColors.Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    property.propertyType.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = RentOutColors.Primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (property.isVerified) VerifiedBadge()
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
                                    property  = property,
                                    isUnlocked = isUnlocked,
                                    onUnlock  = onUnlock,
                                    onCall    = onCall,
                                    onWhatsApp = onWhatsApp
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

                // Action button (right)
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
                                .background(RentOutColors.PrimaryDark)
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
                                Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                }
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
    val tabs = listOf(
        TenantDetailTab.OVERVIEW  to "Overview",
        TenantDetailTab.AMENITIES to "Amenities",
        TenantDetailTab.CONTACT   to "Contact"
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        tabs.forEach { (tab, label) ->
            val isSelected = selected == tab
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
                    color = if (isSelected) RentOutColors.Primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(visible = isSelected) {
                    Box(
                        modifier = Modifier
                            .width(32.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(RentOutColors.Primary)
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
            TenantOverviewStat(Icons.Default.Bed, "${property.rooms}", "Beds", RentOutColors.IconBlue)
            TenantVerticalDivider()
            TenantOverviewStat(Icons.Default.Bathtub, "${property.bathrooms}", "Baths", RentOutColors.IconTeal)
            TenantVerticalDivider()
            TenantOverviewStat(Icons.Default.Apartment, property.propertyType.replaceFirstChar { it.uppercase() }, "Type", RentOutColors.IconPurple)
            if (property.securityDeposit > 0) {
                TenantVerticalDivider()
                TenantOverviewStat(Icons.Default.Shield, "$${property.securityDeposit.toInt()}", "Deposit", RentOutColors.IconAmber)
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
                Text("No amenities listed", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
            }
        }
    } else {
        val chunked = property.amenities.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunked.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { amenity ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RentOutColors.Primary.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(16.dp))
                            Text(
                                amenity, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
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
                // ── Landlord name (revealed) ──────────────────────────────────
                if (property.landlordName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(RentOutColors.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = RentOutColors.Primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Landlord", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                property.landlordName,
                                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.graphicsLayer { alpha = contactAlpha }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(14.dp))
                }
                // ── Phone number (revealed) ───────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(RentOutColors.StatusApproved.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, null, tint = RentOutColors.StatusApproved, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Contact Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            property.contactNumber,
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer { alpha = contactAlpha }
                        )
                    }
                }
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
            } else {
                // ── Landlord name (locked) ────────────────────────────────────
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
                        Text("Landlord", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // ── Phone number (locked) ─────────────────────────────────────
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
                        text = "🔓 Unlock Contact — \$10",
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
