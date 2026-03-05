package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.data.model.User
import org.example.project.presentation.PropertyListState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

// ── Time-of-day data ──────────────────────────────────────────────────────────
private enum class TimeOfDay(
    val greeting: String,
    val icon: ImageVector,
    val iconColor: Color,
    val glowColor: Color
) {
    DAWN   ("Good morning",  Icons.Default.WbTwilight,  Color(0xFFFFB347), Color(0xFFFFD580)),
    MORNING("Good morning",  Icons.Default.WbSunny,     Color(0xFFFFD700), Color(0xFFFFEC80)),
    NOON   ("Good afternoon",Icons.Default.LightMode,   Color(0xFFFF9500), Color(0xFFFFCC00)),
    EVENING("Good evening",  Icons.Default.WbTwilight,  Color(0xFFFF6B6B), Color(0xFFFFAA80)),
    NIGHT  ("Good night",    Icons.Default.NightlightRound, Color(0xFFB0C4DE), Color(0xFF7EC8E3)),
}

private fun getTimeOfDay(hour: Int): TimeOfDay = when (hour) {
    in 5..6   -> TimeOfDay.DAWN
    in 7..11  -> TimeOfDay.MORNING
    in 12..16 -> TimeOfDay.NOON
    in 17..19 -> TimeOfDay.EVENING
    else      -> TimeOfDay.NIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboardScreen(
    user: User,
    propertyListState: PropertyListState,
    onAddProperty: () -> Unit,
    onEditProperty: (Property) -> Unit,
    onDeleteProperty: (String) -> Unit,
    onToggleAvailability: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val listState = rememberLazyListState()
    val isFabVisible by remember {
        derivedStateOf {
            !listState.isScrollInProgress || listState.firstVisibleItemIndex == 0
        }
    }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    // Stats
    val properties = (propertyListState as? PropertyListState.Success)?.properties ?: emptyList()
    val total    = properties.size
    val approved = properties.count { it.status == "approved" }
    val pending  = properties.count { it.status == "pending" }
    val rejected = properties.count { it.status == "rejected" }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(RentOutColors.Primary, RentOutColors.PrimaryDark)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    val greetingInfo = rememberGreetingInfo(user.name.ifBlank { "Landlord" })

                    // Entrance animations — staggered so greeting fades in first,
                    // then the name slides up beneath it.
                    var headerVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { headerVisible = true }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ── Left: greeting + name ──────────────────────────────
                        val tod = getTimeOfDay(
                            java.util.Calendar.getInstance()
                                .get(java.util.Calendar.HOUR_OF_DAY)
                        )
                        val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 1.22f,
                            animationSpec = infiniteRepeatable(
                                tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
                            ), label = "icon_scale"
                        )
                        val iconRotation by infiniteTransition.animateFloat(
                            initialValue = -8f, targetValue = 8f,
                            animationSpec = infiniteRepeatable(
                                tween(2000, easing = EaseInOutSine), RepeatMode.Reverse
                            ), label = "icon_rot"
                        )

                        // Icon x-offset matches the greeting text start padding (none here —
                        // the icon IS the drawable-start of the greeting row).
                        // translateY nudges it slightly down so it sits between greeting & name.
                        val iconOffsetY = 4.dp

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

                            // Row 1: [icon]  "Good morning"
                            AnimatedVisibility(
                                visible = headerVisible,
                                enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -30 }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Daytime icon — translated slightly downward
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .offset(y = iconOffsetY)
                                            .scale(iconScale)
                                            .graphicsLayer { rotationZ = iconRotation }
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        tod.glowColor.copy(alpha = 0.45f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tod.icon,
                                            contentDescription = tod.greeting,
                                            tint = tod.iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = tod.greeting,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                            }

                            // Row 2: First name — start-padded to align with greeting text
                            // (greeting text starts after the icon + 8.dp gap = 30+8 = 38.dp)
                            AnimatedVisibility(
                                visible = headerVisible,
                                enter = fadeIn(tween(500, delayMillis = 150)) +
                                        slideInVertically(tween(500, delayMillis = 150)) { 20 }
                            ) {
                                val firstName = user.name.ifBlank { "Landlord" }
                                    .split(" ").firstOrNull() ?: "Landlord"
                                Text(
                                    text = firstName,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 38.dp)
                                )
                            }
                        }

                        // ── Right: notification + avatar ───────────────────────
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Notification bell — small, clearly smaller than the avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Profile avatar — larger rounded rectangle, full-bleed image
                            val profileInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val isProfilePressed by profileInteraction.collectIsPressedAsState()
                            val profileScale by animateFloatAsState(
                                targetValue = if (isProfilePressed) 0.90f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "profile_scale"
                            )
                            // Outer box: clip shape + gradient background (only visible when no photo)
                            Box(
                                modifier = Modifier
                                    .width(62.dp)
                                    .height(72.dp)
                                    .scale(profileScale)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(RentOutColors.Secondary, RentOutColors.SecondaryLight)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = profileInteraction,
                                        indication = null,
                                        onClick = onProfileClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.profilePhotoUrl.isNotBlank()) {
                                    // Full-bleed crop — fills the entire rounded rect, no background peek
                                    AsyncImage(
                                        model = user.profilePhotoUrl,
                                        contentDescription = "Profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val initials = user.name
                                        .split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .joinToString("") { it.first().uppercaseChar().toString() }
                                        .ifBlank { "L" }
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "fab_scale"
                )
                ExtendedFloatingActionButton(
                    onClick = onAddProperty,
                    icon = { Icon(Icons.Default.Add, "Add") },
                    text = { Text("Add Property", fontWeight = FontWeight.SemiBold) },
                    containerColor = RentOutColors.Secondary,
                    contentColor = Color.White,
                    modifier = Modifier.scale(fabScale),
                    interactionSource = interactionSource
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Stats row
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "My Dashboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Total", total.toString(), Icons.Default.Home, RentOutColors.IconBlue, Modifier.weight(1f))
                    StatCard("Approved", approved.toString(), Icons.Default.CheckCircle, RentOutColors.StatusApproved, Modifier.weight(1f))
                    StatCard("Pending", pending.toString(), Icons.Default.Schedule, RentOutColors.StatusPending, Modifier.weight(1f))
                    StatCard("Rejected", rejected.toString(), Icons.Default.Cancel, RentOutColors.StatusRejected, Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "My Listings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
            }

            when (propertyListState) {
                is PropertyListState.Loading -> item { FullScreenLoader("Loading your listings...") }
                is PropertyListState.Empty   -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏠", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No listings yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Tap the button below to add your first property", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        RentOutPrimaryButton("+ Add Your First Property", onAddProperty, Modifier.fillMaxWidth())
                    }
                }
                is PropertyListState.Success -> items(
                    propertyListState.properties,
                    key = { it.id }
                ) { property ->
                    PropertyCard(
                        property = property,
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem(),
                        showActions = true,
                        onEdit = { onEditProperty(property) },
                        onDelete = { showDeleteDialog = property.id },
                        onToggleAvailability = { onToggleAvailability(property.id) }
                    )
                }
                is PropertyListState.Error -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(propertyListState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { propertyId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                RentOutPrimaryButton(
                    "Delete",
                    onClick = { onDeleteProperty(propertyId); showDeleteDialog = null }
                )
            },
            dismissButton = {
                RentOutSecondaryButton("Cancel", onClick = { showDeleteDialog = null })
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloatOrNull() ?: 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "stat_anim"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                animatedValue.toInt().toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    indication = null,
    onClick = onClick
)
