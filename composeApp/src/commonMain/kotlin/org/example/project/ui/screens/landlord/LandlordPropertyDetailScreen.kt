package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import org.example.project.ui.components.AvailabilityBadge
import org.example.project.ui.components.StatusBadge
import org.example.project.ui.components.VerifiedBadge
import org.example.project.ui.theme.RentOutColors

// ─────────────────────────────────────────────────────────────────────────────
// LandlordPropertyDetailScreen
// Inspired by the Suncrest Manor design — full hero image, floating stat pills,
// thumbnail strip, sliding white card, tab bar, and sticky Book Now bar.
// ─────────────────────────────────────────────────────────────────────────────

private enum class DetailTab { OVERVIEW, AMENITIES, STATUS }

@Composable
fun LandlordPropertyDetailScreen(
    property: Property,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Back button animation
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.82f else 1f, tween(180), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f, tween(180), label = "br")

    // Tab state
    var selectedTab by remember { mutableStateOf(DetailTab.OVERVIEW) }

    // Image thumbnail selection — prefer full imageUrls list, fall back to imageUrl
    val images = remember(property.id) {
        property.imageUrls.ifEmpty {
            listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
        }
    }
    var selectedImageIndex by remember { mutableStateOf(0) }
    val heroImageUrl = images.getOrElse(selectedImageIndex) { property.imageUrl }

    // Hero parallax offset
    val heroOffset by remember {
        derivedStateOf { (scrollState.value * 0.35f) }
    }

    // Card entrance animation
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(RentOutColors.IconRose.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = RentOutColors.IconRose,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Property?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "\"${property.title}\" will be permanently removed. This action cannot be undone.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RentOutColors.IconRose)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showDeleteDialog = false
                            onDelete()
                        }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Yes, Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDeleteDialog = false }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Scrollable content ────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // ── Hero Image ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Hero photo with subtle parallax
                AsyncImage(
                    model = heroImageUrl.ifBlank { null },
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = heroOffset }
                )

                // Dark gradient overlay — top & bottom
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f   to Color.Black.copy(alpha = 0.35f),
                            0.4f to Color.Transparent,
                            1f   to Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )

                // ── Thumbnail strip — lifted above the card boundary ─────────
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
                                label = "thumb_size_$index"
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
                                // Selected tint overlay
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(RentOutColors.Primary.copy(alpha = 0.25f))
                                    )
                                }
                                // White border ring on selected
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Transparent)
                                            .then(
                                                Modifier.shadow(0.dp, RoundedCornerShape(12.dp))
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── White sliding card — overlaps hero by 24dp via negative offset ─
            AnimatedVisibility(
                visible = cardVisible,
                enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
                        + fadeIn(tween(400))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp),
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
                                    Icons.Default.Apartment,
                                    null,
                                    tint = RentOutColors.Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    property.propertyType.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RentOutColors.Primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (property.isVerified) VerifiedBadge()
                                AvailabilityBadge(property.isAvailable)
                                StatusBadge(property.status)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ── Title ────────────────────────────────────────────
                        Text(
                            property.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))

                        // ── Location ─────────────────────────────────────────
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = RentOutColors.IconRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                property.location.ifBlank { property.city },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // ── Landlord phone number ─────────────────────────────
                        if (property.contactNumber.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    property.contactNumber,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Tab bar (Overview / Amenities / Status) ───────────
                        DetailTabBar(
                            selected = selectedTab,
                            onSelect = { selectedTab = it }
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── Tab content ───────────────────────────────────────
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                            },
                            label = "tab_content"
                        ) { tab ->
                            when (tab) {
                                DetailTab.OVERVIEW  -> OverviewContent(property)
                                DetailTab.AMENITIES -> AmenitiesContent(property)
                                DetailTab.STATUS    -> StatusContent(property)
                            }
                        }

                        // Bottom spacer — room for sticky bar
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        // ── Floating back button only — fixed over the hero, never scrolls ──
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
                    .graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    },
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { backPressed = true; onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }
        }

        // ── Sticky bottom bar — price left, actions right ────────────────────
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
                // ── Price column (left) ───────────────────────────────────────
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$${property.price.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RentOutColors.Secondary
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "/mth",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RentOutColors.Secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ── Action buttons (right) ────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delete button
                    val deleteInteraction = remember { MutableInteractionSource() }
                    val deletePressed by deleteInteraction.collectIsPressedAsState()
                    val deleteScale by animateFloatAsState(
                        targetValue = if (deletePressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "delete_scale"
                    )
                    Box(
                        modifier = Modifier
                            .scale(deleteScale)
                            .size(54.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(RentOutColors.IconRose)
                            .clickable(
                                interactionSource = deleteInteraction,
                                indication = null,
                                onClick = { showDeleteDialog = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Edit Listing button
                    val editInteraction = remember { MutableInteractionSource() }
                    val editPressed by editInteraction.collectIsPressedAsState()
                    val editScale by animateFloatAsState(
                        targetValue = if (editPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "edit_scale"
                    )
                    Box(
                        modifier = Modifier
                            .scale(editScale)
                            .height(54.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(RentOutColors.PrimaryDark)
                            .clickable(
                                interactionSource = editInteraction,
                                indication = null,
                                onClick = onEdit
                            )
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Edit Listing",
                                color = Color.White,
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
private fun DetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit
) {
    val tabs = listOf(
        DetailTab.OVERVIEW  to "Overview",
        DetailTab.AMENITIES to "Amenities",
        DetailTab.STATUS    to "Status"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
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
                // Underline indicator
                AnimatedVisibility(visible = isSelected) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
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
private fun OverviewContent(property: Property) {
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
                OverviewStat(Icons.Default.Bed, "${property.rooms}", "Beds", RentOutColors.IconBlue)
                VerticalDivider()
            }
            if (property.bathrooms > 0) {
                OverviewStat(Icons.Default.Bathtub, "${property.bathrooms}", "Baths", RentOutColors.IconTeal)
                VerticalDivider()
            }
            OverviewStat(Icons.Default.HomeWork, property.propertyType, "Type", RentOutColors.IconPurple)
            if (property.securityDeposit > 0 && !property.depositNotApplicable) {
                VerticalDivider()
                OverviewStat(Icons.Default.Shield, "$${property.securityDeposit.toInt()}", "Deposit", RentOutColors.IconAmber)
            } else if (property.depositNotApplicable) {
                VerticalDivider()
                OverviewStat(Icons.Default.Shield, "N/A", "Deposit", RentOutColors.Tertiary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Classification & Location Type chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (property.classification.isNotBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.Primary.copy(alpha = 0.10f)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, null, tint = RentOutColors.Primary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(property.classification, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.Primary)
                    }
                }
            }
            if (property.locationType.isNotBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.IconPurple.copy(alpha = 0.10f)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationCity, null, tint = RentOutColors.IconPurple, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(property.locationType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.IconPurple)
                    }
                }
            }
        }

        // Room quantity
        if (property.roomQuantity.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MeetingRoom, null, tint = RentOutColors.IconBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Room Quantity: ${property.roomQuantity}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }

        // Bathroom type
        if (property.bathroomType.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bathtub, null, tint = RentOutColors.IconTeal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bathroom: ${property.bathroomType}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }

        // Kitchen
        if (property.kitchenCount > 0) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Kitchen, null, tint = RentOutColors.IconOrange, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Kitchen: ${property.kitchenCount}" + if (property.hasSharedKitchen) " (Shared)" else "",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium
                )
            }
        }

        // Bills
        if (property.billsInclusive.isNotEmpty() || property.billsExclusive.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Bills", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            if (property.billsInclusive.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.Tertiary.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp),
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.IconAmber.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cancel, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Excluded: ${property.billsExclusive.joinToString(" · ")}", fontSize = 12.sp, color = RentOutColors.IconAmber, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Availability
        Spacer(Modifier.height(12.dp))
        if (property.availabilityDate.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Primary.copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = RentOutColors.Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Available from: ${property.availabilityDate}", fontSize = 12.sp, color = RentOutColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Tertiary.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp),
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
            Text("Nearby Facilities", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            val chunked = property.proximityFacilities.chunked(2)
            chunked.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { facility ->
                        Row(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(RentOutColors.Primary.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, null, tint = RentOutColors.Primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(facility, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
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
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OverviewStat(icon: ImageVector, value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

// ── Amenities tab ─────────────────────────────────────────────────────────────
@Composable
private fun AmenitiesContent(property: Property) {
    if (property.amenities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Info,
                    null,
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
        // Simple 2-column grid of amenity chips
        val chunked = property.amenities.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = RentOutColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                amenity,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Fill empty slot in odd row
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Status tab ────────────────────────────────────────────────────────────────
@Composable
private fun StatusContent(property: Property) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusRow(Icons.Default.Verified, "Verification",
            if (property.isVerified) "Verified ✓" else "Not Verified",
            if (property.isVerified) RentOutColors.StatusApproved else MaterialTheme.colorScheme.onSurfaceVariant)
        StatusRow(Icons.Default.Visibility, "Availability",
            if (property.isAvailable) "Available" else "Unavailable",
            if (property.isAvailable) RentOutColors.StatusApproved else RentOutColors.StatusRejected)
        StatusRow(Icons.Default.AdminPanelSettings, "Admin Status",
            property.status.replaceFirstChar { it.uppercase() },
            when (property.status) {
                "approved" -> RentOutColors.StatusApproved
                "rejected" -> RentOutColors.StatusRejected
                else       -> RentOutColors.IconAmber
            })
        StatusRow(Icons.Default.Flag, "Flagged",
            if (property.isFlagged) "Flagged ⚠" else "Not Flagged",
            if (property.isFlagged) RentOutColors.StatusRejected else RentOutColors.StatusApproved)
        StatusRow(Icons.Default.AttachMoney, "Price / month",
            "$${property.price.toInt()} USD", RentOutColors.Primary)
        StatusRow(Icons.Default.Shield, "Security Deposit",
            if (property.depositNotApplicable) "Not Applicable"
            else if (property.securityDeposit > 0) "$${property.securityDeposit.toInt()} USD"
            else "Not set",
            if (property.depositNotApplicable) RentOutColors.Tertiary else RentOutColors.IconPurple)
        StatusRow(Icons.Default.Category, "Classification",
            property.classification.ifBlank { "—" }, RentOutColors.Primary)
        StatusRow(Icons.Default.HomeWork, "Property Type",
            property.propertyType.ifBlank { "—" }, RentOutColors.IconBlue)
        if (property.locationType.isNotBlank()) {
            StatusRow(Icons.Default.LocationCity, "Location Type", property.locationType, RentOutColors.IconPurple)
        }
        if (property.latitude != 0.0 || property.longitude != 0.0) {
            StatusRow(Icons.Default.GpsFixed, "Coordinates",
                "%.5f, %.5f".format(property.latitude, property.longitude), RentOutColors.IconTeal)
        }
        if (property.availabilityDate.isNotBlank()) {
            StatusRow(Icons.Default.CalendarToday, "Available From", property.availabilityDate, RentOutColors.Primary)
        } else {
            StatusRow(Icons.Default.CheckCircle, "Availability", "Available Now", RentOutColors.Tertiary)
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
