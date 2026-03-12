@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.data.model.Transaction
import org.example.project.data.model.User
import org.example.project.data.model.ZIMBABWE_TOWNS
import org.example.project.data.model.suburbsForTown
import org.example.project.presentation.ALL_TOWNS
import org.example.project.presentation.PropertyFilter
import org.example.project.presentation.PropertyListState
import org.example.project.presentation.SortOption
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.theme.RentOutBackgrounds
import org.example.project.ui.theme.RentOutTextColors
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ── Tenant dashboard colour tokens — inspired by the property detail screenshot
// Deep navy header, warm cream background, coral accent, slate greys for text.
private val TenantNavy       = Color(0xFF0F2A4A)   // deep navy — header bg
private val TenantNavyLight  = Color(0xFF1A3F6F)   // lighter navy — gradient end
private val TenantCream      = Color(0xFFF5F0EB)   // warm cream — page background
private val TenantCoral      = Color(0xFFE8724A)   // warm coral — CTA / accents
private val TenantAmber      = Color(0xFFF5A623)   // amber — unlocked badge / FAB
private val TenantCardBg     = Color(0xFFFFFFFF)   // white cards on cream bg
private val TenantSlate      = Color(0xFF4A5568)   // body text on cream
private val TenantSlateLight = Color(0xFF718096)   // secondary text
private val TenantMint       = Color(0xFF38B2AC)   // mint teal — stat accent
private val TenantSage       = Color(0xFF68D391)   // sage green — available badge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantHomeScreen(
    user: User,
    propertyListState: PropertyListState,
    searchQuery: String,
    selectedCity: String,
    unlockedPropertyIds: Set<String> = emptySet(),
    transactions: List<Transaction> = emptyList(),
    activeFilter: PropertyFilter = PropertyFilter(),
    notificationCount: Int = 0,
    onSearchQueryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onFilterChange: (PropertyFilter) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onPropertyClick: (Property) -> Unit,
    onUnlockedClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    applyFilters: (List<Property>, String, String, PropertyFilter) -> List<Property> = { props, query, city, _ ->
        props.filter { p ->
            (city.isBlank() || city.equals(ALL_TOWNS, ignoreCase = true) || p.city.equals(city, ignoreCase = true)) &&
            (query.isBlank() || p.title.contains(query, ignoreCase = true))
        }
    }
) {
    val listState = rememberLazyListState()
    val isFabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val properties = (propertyListState as? PropertyListState.Success)?.properties ?: emptyList()
    val filtered = remember(properties, searchQuery, selectedCity, activeFilter) {
        applyFilters(properties, searchQuery, selectedCity, activeFilter)
    }

    // Entrance animation
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerVisible = true }

    var showFilterSheet by remember { mutableStateOf(false) }

    // Animated time-of-day icon
    val tod = remember {
        val h = try { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) } catch (_: Exception) { 10 }
        when (h) {
            in 5..6   -> Triple(Icons.Default.WbTwilight, Color(0xFFFFB347), "Good morning")
            in 7..11  -> Triple(Icons.Default.WbSunny, Color(0xFFFFD700), "Good morning")
            in 12..16 -> Triple(Icons.Default.LightMode, Color(0xFFFF9500), "Good afternoon")
            in 17..19 -> Triple(Icons.Default.WbTwilight, Color(0xFFFF6B6B), "Good evening")
            else      -> Triple(Icons.Default.NightlightRound, Color(0xFFB0C4DE), "Good night")
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "icon_scale"
    )
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "icon_rot"
    )

    if (showFilterSheet) {
        PropertyFilterSheet(
            current = activeFilter,
            selectedCity = selectedCity,
            onApply = { newFilter -> onFilterChange(newFilter); showFilterSheet = false },
            onApplyCity = { city -> onCityChange(city) },
            onDismiss = { showFilterSheet = false },
            onReset = { onClearFilter(); onCityChange(ALL_TOWNS); showFilterSheet = false }
        )
    }

    val isDark = isSystemInDarkTheme()
    
    Scaffold(
        containerColor = if (isDark) MaterialTheme.colorScheme.background else TenantCream,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val fabInteraction = remember { MutableInteractionSource() }
                val isFabPressed by fabInteraction.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    if (isFabPressed) 0.88f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fab"
                )
                ExtendedFloatingActionButton(
                    onClick = onUnlockedClick,
                    icon = { Icon(Icons.Default.Key, null) },
                    text = { Text("My Unlocked", fontWeight = FontWeight.Bold) },
                    containerColor = TenantAmber,
                    contentColor = Color.White,
                    modifier = Modifier.scale(fabScale),
                    interactionSource = fabInteraction
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isDark) {
                        Modifier.background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier.background(TenantCream)
                    }
                ),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 100.dp)
        ) {
            // ── Hero header ────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(TenantNavy, TenantNavyLight)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 18.dp)
                ) {
                    Column {
                        // ── Row 1: greeting left | avatar + bell right ────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Greeting + name column
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                AnimatedVisibility(
                                    visible = headerVisible,
                                    enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -30 }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .offset(y = 3.dp)
                                                .scale(iconScale)
                                                .graphicsLayer { rotationZ = iconRotation }
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(tod.second.copy(alpha = 0.4f), Color.Transparent)
                                                    ),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(tod.first, null, tint = tod.second, modifier = Modifier.size(20.dp))
                                        }
                                        Text(tod.third, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.88f))
                                    }
                                }
                                AnimatedVisibility(
                                    visible = headerVisible,
                                    enter = fadeIn(tween(500, delayMillis = 120)) + slideInVertically(tween(500, delayMillis = 120)) { 20 }
                                ) {
                                    val firstName = user.name.ifBlank { "Tenant" }.split(" ").firstOrNull() ?: "Tenant"
                                    Text(
                                        firstName,
                                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                        modifier = Modifier.padding(start = 36.dp)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = headerVisible,
                                    enter = fadeIn(tween(500, delayMillis = 220))
                                ) {
                                    Text(
                                        "Find your perfect home 🏠",
                                        fontSize = 12.sp, color = Color.White.copy(0.65f),
                                        modifier = Modifier.padding(start = 36.dp)
                                    )
                                }
                            }

                            // Right: bell + avatar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Notification bell with unread badge
                                org.example.project.ui.screens.NotificationBadgeBox(
                                    count = notificationCount,
                                    onClick = onNotificationsClick
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape)
                                            .background(Color.White.copy(0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                // Profile avatar — rounded rect, image or initials
                                val profileInteraction = remember { MutableInteractionSource() }
                                val isProfilePressed by profileInteraction.collectIsPressedAsState()
                                val profileScale by animateFloatAsState(
                                    if (isProfilePressed) 0.90f else 1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pscale"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(56.dp).height(64.dp)
                                        .scale(profileScale)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(TenantCoral, TenantAmber)))
                                        .clickable(interactionSource = profileInteraction, indication = null) { onProfileClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.profilePhotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = user.profilePhotoUrl,
                                            contentDescription = "Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        val initials = user.name.split(" ").filter { it.isNotBlank() }
                                            .take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "T" }
                                        Text(initials, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Stats strip ───────────────────────────────────────
                        AnimatedVisibility(
                            visible = headerVisible,
                            enter = fadeIn(tween(500, delayMillis = 300)) + slideInVertically(tween(500, delayMillis = 300)) { 24 }
                        ) {
                            val total = properties.size
                            val available = properties.count { it.isAvailable }
                            // Derive unlocked count from successful transactions so
                            // the stat always tallies with payment history
                            val unlocked = transactions.count {
                                it.status.equals("success", ignoreCase = true)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(0.10f))
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TenantStatPill(total.toString(), "Listings", TenantMint)
                                Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.25f)))
                                TenantStatPill(available.toString(), "Available", TenantSage)
                                Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.25f)))
                                TenantStatPill(unlocked.toString(), "Unlocked", TenantAmber)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Search bar + filter button ─────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search field — cream background on navy
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(0.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(TenantCream)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = TenantNavy,
                                        fontSize = 14.sp
                                    ),
                                    placeholder = { Text("Search properties...", color = TenantSlateLight, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TenantCoral, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = if (searchQuery.isNotEmpty()) ({
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, null, tint = TenantSlateLight, modifier = Modifier.size(18.dp))
                                        }
                                    }) else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true
                                )
                            }
                            // Filter button — FilterList is the best icon for filtering
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .shadow(4.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (activeFilter.isActive) TenantCoral else TenantCream.copy(0.20f))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showFilterSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FilterList, "Filter", tint = if (activeFilter.isActive) Color.White else TenantCream, modifier = Modifier.size(24.dp))
                                }
                                if (activeFilter.activeCount > 0) {
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape)
                                            .background(TenantAmber)
                                            .align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${activeFilter.activeCount}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // ── Active filter chips ───────────────────────────────
                        val isAllTowns = selectedCity.isBlank() || selectedCity.equals(ALL_TOWNS, ignoreCase = true)
                        if (activeFilter.isActive || !isAllTowns) {
                            Spacer(Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                                if (!isAllTowns) item {
                                    ActiveFilterChip("📍 $selectedCity", onRemove = { onCityChange(ALL_TOWNS) })
                                }
                                if (activeFilter.minPrice != null || activeFilter.maxPrice != null) item {
                                    ActiveFilterChip("$${activeFilter.minPrice?.toInt() ?: 0}–$${activeFilter.maxPrice?.toInt()?.toString() ?: "∞"}", onRemove = { onFilterChange(activeFilter.copy(minPrice = null, maxPrice = null)) })
                                }
                                activeFilter.propertyTypes.forEach { type -> item { ActiveFilterChip(type.replaceFirstChar { it.uppercase() }, onRemove = { onFilterChange(activeFilter.copy(propertyTypes = activeFilter.propertyTypes - type)) }) } }
                                activeFilter.classifications.forEach { c -> item { ActiveFilterChip(c, onRemove = { onFilterChange(activeFilter.copy(classifications = activeFilter.classifications - c)) }) } }
                                activeFilter.locationTypes.forEach { lt -> item { ActiveFilterChip(lt, onRemove = { onFilterChange(activeFilter.copy(locationTypes = activeFilter.locationTypes - lt)) }) } }
                                activeFilter.providerTypes.forEach { pt ->
                                    item {
                                        val label = when (pt) {
                                            "landlord"  -> "🏠 Landlord"
                                            "agent"     -> "🤝 Agent"
                                            "brokerage" -> "🏢 Brokerage"
                                            else        -> pt.replaceFirstChar { it.uppercase() }
                                        }
                                        ActiveFilterChip(label, onRemove = { onFilterChange(activeFilter.copy(providerTypes = activeFilter.providerTypes - pt)) })
                                    }
                                }
                                if (activeFilter.minBedrooms != null) item {
                                    ActiveFilterChip(if (activeFilter.maxBedrooms != null) "${activeFilter.minBedrooms}–${activeFilter.maxBedrooms} beds" else "${activeFilter.minBedrooms}+ beds", onRemove = { onFilterChange(activeFilter.copy(minBedrooms = null, maxBedrooms = null)) })
                                }
                                if (activeFilter.minBathrooms != null) item { ActiveFilterChip("${activeFilter.minBathrooms}+ baths", onRemove = { onFilterChange(activeFilter.copy(minBathrooms = null)) }) }
                                if (activeFilter.availableOnly) item { ActiveFilterChip("Available Only", onRemove = { onFilterChange(activeFilter.copy(availableOnly = false)) }) }
                                if (activeFilter.verifiedOnly) item { ActiveFilterChip("Verified Only", onRemove = { onFilterChange(activeFilter.copy(verifiedOnly = false)) }) }
                                activeFilter.requiredAmenities.forEach { a -> item { ActiveFilterChip(a, onRemove = { onFilterChange(activeFilter.copy(requiredAmenities = activeFilter.requiredAmenities - a)) }) } }
                                if (activeFilter.sortBy != SortOption.NEWEST) item { ActiveFilterChip("Sort: ${activeFilter.sortBy.label}", onRemove = { onFilterChange(activeFilter.copy(sortBy = SortOption.NEWEST)) }) }
                                item {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.22f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClearFilter() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Clear All", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Results bar ────────────────────────────────────────────────────
            item {
                val isAllTowns = selectedCity.isBlank() || selectedCity.equals(ALL_TOWNS, ignoreCase = true)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        buildString {
                            append("${filtered.size} ${if (filtered.size == 1) "property" else "properties"}")
                            if (!isAllTowns) append(" in $selectedCity")
                            if (activeFilter.isActive) append(" · ${activeFilter.activeCount} filter${if (activeFilter.activeCount > 1) "s" else ""}")
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TenantSlate
                    )
                    if (filtered.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = TenantSage, modifier = Modifier.size(14.dp))
                            Text("Verified", fontSize = 12.sp, color = TenantSage, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            when {
                propertyListState is PropertyListState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = TenantCoral, strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
                            Text("Finding properties...", fontSize = 14.sp, color = TenantSlateLight, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                filtered.isEmpty() -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏘️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No properties found", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TenantSlate)
                        Text("Try adjusting your search or filters", color = TenantSlateLight, fontSize = 14.sp)
                        if (searchQuery.isNotEmpty() || !selectedCity.equals(ALL_TOWNS, ignoreCase = true) || activeFilter.isActive) {
                            Spacer(Modifier.height(16.dp))
                            RentOutSecondaryButton("Clear All Filters", onClick = { onSearchQueryChange(""); onCityChange(ALL_TOWNS); onClearFilter() })
                        }
                    }
                }
                else -> items(
                    items = filtered,
                    key = { property -> property.id.ifBlank { "fallback-${property.createdAt}-${property.title}-${property.location}" } }
                ) { property ->
                    PropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem(),
                        isUnlocked = unlockedPropertyIds.contains(property.id),
                        showBillsInfo = false
                    )
                }
            }
        }
    }
}

@Composable
private fun TenantTownPickerDialog(
    selectedTown: String,
    onSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val isAllTowns = selectedTown.isBlank() || selectedTown.equals(ALL_TOWNS, ignoreCase = true)

    val filtered = remember(search) {
        if (search.isBlank()) ZIMBABWE_TOWNS
        else ZIMBABWE_TOWNS.filter { it.name.contains(search, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(RentOutColors.Primary, RentOutColors.PrimaryLight)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationCity, null,
                                tint = Color.White, modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Select Town / City",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                            Text(
                                "Zimbabwe · ${ZIMBABWE_TOWNS.size} towns available",
                                fontSize = 12.sp, color = Color.White.copy(0.8f)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    placeholder = {
                        Text(
                            "Search Zimbabwe towns...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = RentOutColors.Primary)
                    },
                    trailingIcon = {
                        AnimatedVisibility(search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Clear, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))

                // "All Towns" option at top
                AnimatedVisibility(search.isBlank()) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isAllTowns) RentOutColors.Primary.copy(alpha = 0.07f)
                                    else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectAll() }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isAllTowns) RentOutColors.Primary.copy(0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Public, null,
                                    tint = if (isAllTowns) RentOutColors.Primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "All Towns",
                                    fontSize = 15.sp,
                                    fontWeight = if (isAllTowns) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllTowns) RentOutColors.Primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Browse properties across Zimbabwe",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isAllTowns) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = RentOutColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(0.15f)
                        )

                        // Launch-city note when browsing all towns
                        if (isAllTowns) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RentOutColors.Secondary.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb, null,
                                    tint = RentOutColors.Secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Gweru is one of our launch cities — browse all towns or pick one below.",
                                    fontSize = 11.sp,
                                    color = RentOutColors.Secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Towns list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filtered) { index, town ->
                        val isSelected = town.name == selectedTown
                        val visible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 25L)
                            visible.value = true
                        }
                        AnimatedVisibility(
                            visible = visible.value,
                            enter = slideInHorizontally(tween(200)) { -30 } + fadeIn(tween(200))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        animateColorAsState(
                                            if (isSelected) RentOutColors.Primary.copy(0.07f)
                                            else Color.Transparent,
                                            label = "row_bg_$index"
                                        ).value
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onSelect(town.name) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                // Highlight launch city without making it the default filter
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) RentOutColors.Primary.copy(0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocationCity, null,
                                            tint = if (isSelected) RentOutColors.Primary
                                                   else RentOutColors.IconTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // Launch-city star badge for Gweru
                                    if (town.name == "Gweru") {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(RentOutColors.Secondary)
                                                .align(Alignment.TopEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Star, null,
                                                tint = Color.White,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = town.name,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                                         else FontWeight.Medium,
                                            color = if (isSelected) RentOutColors.Primary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (town.name == "Gweru") {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(RentOutColors.Secondary.copy(0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Launch City",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RentOutColors.Secondary
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${town.suburbs.size} suburbs · Zimbabwe",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = RentOutColors.Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    indication = null,
    onClick = onClick
)

@Composable
private fun TenantStatPill(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(0.75f), fontWeight = FontWeight.Medium)
    }
}

// ── Active filter chip (dismissible pill shown below search bar) ──────────────
@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onRemove() }
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

// ── Property Filter Bottom Sheet ──────────────────────────────────────────────
// Full-featured real estate filter with: price range, property type, beds,
// baths, availability, verified, amenities, sort order.
@Composable
private fun PropertyFilterSheet(
    current: PropertyFilter,
    selectedCity: String,
    onApply: (PropertyFilter) -> Unit,
    onApplyCity: (String) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    // Local draft — user can cancel without affecting the active filter
    var draft by remember { mutableStateOf(current) }
    var draftCity by remember { mutableStateOf(selectedCity) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ── Sheet header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(TenantNavy, TenantNavyLight))
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                        Column {
                            Text("Filter Properties", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Narrow down your search", fontSize = 12.sp, color = Color.White.copy(0.8f))
                        }
                    }
                    TextButton(onClick = onReset) {
                        Text("Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Scrollable filter content ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── 0. Town / City ────────────────────────────────────────────
                FilterSection(title = "Town / City", icon = Icons.Default.LocationCity) {
                    val towns = listOf(ALL_TOWNS) + org.example.project.data.model.ZIMBABWE_TOWNS.map { it.name }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "All Towns" chip
                        val isAll = draftCity.isBlank() || draftCity.equals(ALL_TOWNS, ignoreCase = true)
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (isAll) RentOutColors.Primary.copy(0.10f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (isAll) 1.5.dp else 0.dp, if (isAll) RentOutColors.Primary else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { draftCity = ALL_TOWNS }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Public, null, tint = if (isAll) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text("All Towns", fontSize = 14.sp, fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal, color = if (isAll) RentOutColors.Primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (isAll) Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
                        }
                        // Town chips scrollable row
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(org.example.project.data.model.ZIMBABWE_TOWNS) { town ->
                                val isSelected = draftCity == town.name
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(if (isSelected) 0.dp else 0.dp, Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { draftCity = town.name }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        town.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 1. Sort By — collapsible branded dropdown ────────────────────
                FilterSection(title = "Sort By", icon = Icons.Default.Sort) {
                    var sortDropdownExpanded by remember { mutableStateOf(false) }
                    val dropdownRotation by animateFloatAsState(
                        targetValue = if (sortDropdownExpanded) 180f else 0f,
                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                        label = "dropdown_arrow"
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Trigger button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (draft.sortBy != SortOption.NEWEST)
                                        RentOutColors.Primary.copy(alpha = 0.10f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (draft.sortBy != SortOption.NEWEST) 1.5.dp else 1.dp,
                                    color = if (draft.sortBy != SortOption.NEWEST)
                                        RentOutColors.Primary
                                    else MaterialTheme.colorScheme.outline.copy(0.3f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { sortDropdownExpanded = !sortDropdownExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sort, null,
                                    tint = if (draft.sortBy != SortOption.NEWEST)
                                        RentOutColors.Primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    draft.sortBy.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (draft.sortBy != SortOption.NEWEST)
                                        FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (draft.sortBy != SortOption.NEWEST)
                                        RentOutColors.Primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = dropdownRotation }
                            )
                        }

                        // Dropdown menu
                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            SortOption.entries.forEachIndexed { index, option ->
                                val isSelected = draft.sortBy == option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) RentOutColors.Primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        width = if (isSelected) 0.dp else 1.5.dp,
                                                        color = if (isSelected) Color.Transparent
                                                        else MaterialTheme.colorScheme.outline.copy(0.4f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check, null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                option.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold
                                                else FontWeight.Normal,
                                                color = if (isSelected) RentOutColors.Primary
                                                else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        draft = draft.copy(sortBy = option)
                                        sortDropdownExpanded = false
                                    },
                                    modifier = Modifier.background(
                                        if (isSelected) RentOutColors.Primary.copy(alpha = 0.06f)
                                        else Color.Transparent
                                    )
                                )
                                if (index < SortOption.entries.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(0.12f)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 2. Monthly Rent — chips + interactive range slider ────────────
                FilterSection(title = "Monthly Rent (USD)", icon = Icons.Default.AttachMoney) {
                    // Local text state for manual input — kept as strings so the
                    // user can type freely; parsed to Double on change.
                    var minText by remember(draft.minPrice) {
                        mutableStateOf(draft.minPrice?.toInt()?.toString() ?: "")
                    }
                    var maxText by remember(draft.maxPrice) {
                        mutableStateOf(draft.maxPrice?.toInt()?.toString() ?: "")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // ── Minimum Price ────────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Minimum Price",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Any" button
                                SmallSelectChip(
                                    label = "Any",
                                    selected = draft.minPrice == null,
                                    onClick = {
                                        minText = ""
                                        draft = draft.copy(minPrice = null)
                                    }
                                )
                                // Manual input field
                                OutlinedTextField(
                                    value = minText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        minText = digits
                                        val parsed = digits.toDoubleOrNull()
                                        draft = draft.copy(minPrice = if (parsed != null && parsed > 0) parsed else null)
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text("e.g. 200", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    prefix = { Text("$", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.Primary) },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = RentOutColors.Primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f),
                                        focusedLabelColor    = RentOutColors.Primary
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // ── Maximum Price ────────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Maximum Price",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Any" button
                                SmallSelectChip(
                                    label = "Any",
                                    selected = draft.maxPrice == null,
                                    onClick = {
                                        maxText = ""
                                        draft = draft.copy(maxPrice = null)
                                    }
                                )
                                // Manual input field
                                OutlinedTextField(
                                    value = maxText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        maxText = digits
                                        val parsed = digits.toDoubleOrNull()
                                        draft = draft.copy(maxPrice = if (parsed != null && parsed > 0) parsed else null)
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text("e.g. 1000", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    prefix = { Text("$", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RentOutColors.Primary) },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = RentOutColors.Primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f),
                                        focusedLabelColor    = RentOutColors.Primary
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // ── Summary display ──────────────────────────────────
                        if (draft.minPrice != null || draft.maxPrice != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RentOutColors.Primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildString {
                                        append("Range: ")
                                        append(draft.minPrice?.let { "$${ it.toInt() }" } ?: "Any")
                                        append("  →  ")
                                        append(draft.maxPrice?.let { "$${ it.toInt() }" } ?: "Any")
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RentOutColors.Primary
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 3. Property Type ──────────────────────────────────────────
                FilterSection(title = "Property Type", icon = Icons.Default.Apartment) {
                    val types = listOf(
                        "apartment" to "🏢 Apartment",
                        "house"     to "🏠 House",
                        "room"      to "🛏️ Room",
                        "commercial" to "🏬 Commercial"
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { (key, label) ->
                            val selected = key in draft.propertyTypes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) RentOutColors.Primary.copy(0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) RentOutColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        draft = draft.copy(
                                            propertyTypes = if (selected)
                                                draft.propertyTypes - key
                                            else
                                                draft.propertyTypes + key
                                        )
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 4. Classification ───────────────────────────────────────────────
                FilterSection(title = "Property Classification", icon = Icons.Default.Category) {
                    val classificationOptions = listOf("Residential", "Commercial", "Industrial", "Land", "Mixed-Use")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        classificationOptions.forEach { classification ->
                            val selected = classification in draft.classifications
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) RentOutColors.Primary.copy(0.10f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) RentOutColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        draft = draft.copy(
                                            classifications = if (selected)
                                                draft.classifications - classification
                                            else
                                                draft.classifications + classification
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    classification,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 5. Location Type ───────────────────────────────────────────────
                FilterSection(title = "Location Type", icon = Icons.Default.LocationCity) {
                    val locationTypeOptions = listOf("Low Density", "Medium Density", "High Density", "Peri-Urban Residential", "Rural")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationTypeOptions.forEach { locationType ->
                            val selected = locationType in draft.locationTypes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) RentOutColors.Primary.copy(0.10f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) RentOutColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        draft = draft.copy(
                                            locationTypes = if (selected)
                                                draft.locationTypes - locationType
                                            else
                                                draft.locationTypes + locationType
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    locationType,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 5b. Property Provider / Lister ───────────────────────────
                FilterSection(title = "Property Provider", icon = Icons.Default.Person) {
                    val providers = listOf(
                        Triple("landlord",  "🏠", "Landlord"),
                        Triple("agent",     "🤝", "Freelancer Agent"),
                        Triple("brokerage", "🏢", "Brokerage")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Filter by who listed the property",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        providers.forEach { (key, emoji, label) ->
                            val selected = key in draft.providerTypes
                            val bgColor by animateColorAsState(
                                if (selected) RentOutColors.Primary.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(200),
                                label = "provider_bg_$key"
                            )
                            val borderColor by animateColorAsState(
                                if (selected) RentOutColors.Primary else Color.Transparent,
                                animationSpec = tween(200),
                                label = "provider_border_$key"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        draft = draft.copy(
                                            providerTypes = if (selected)
                                                draft.providerTypes - key
                                            else
                                                draft.providerTypes + key
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Emoji icon box
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) RentOutColors.Primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                                // Label + subtitle
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        label,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) RentOutColors.Primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        when (key) {
                                            "landlord"  -> "Direct owner-listed properties"
                                            "agent"     -> "Independent property agents"
                                            "brokerage" -> "Professional real estate firms"
                                            else        -> ""
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Animated check indicator
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                    exit  = scaleOut(tween(150)) + fadeOut(tween(150))
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = RentOutColors.Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 6. Bedrooms ───────────────────────────────────────────────
                FilterSection(title = "Bedrooms", icon = Icons.Default.Bed) {
                    val bedOptions = listOf(null to "Any", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5+")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bedOptions.forEach { (count, label) ->
                            val selected = draft.minBedrooms == count && (count == null || draft.maxBedrooms == null)
                            SmallSelectChip(
                                label = label,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    draft = if (count == null) {
                                        draft.copy(minBedrooms = null, maxBedrooms = null)
                                    } else if (count == 5) {
                                        draft.copy(minBedrooms = 5, maxBedrooms = null)
                                    } else {
                                        draft.copy(minBedrooms = count, maxBedrooms = count)
                                    }
                                }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 7. Bathrooms ───────────────────────────────────────────────
                FilterSection(title = "Bathrooms", icon = Icons.Default.Bathtub) {
                    val bathOptions = listOf(null to "Any", 1 to "1+", 2 to "2+", 3 to "3+")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bathOptions.forEach { (count, label) ->
                            SmallSelectChip(
                                label = label,
                                selected = draft.minBathrooms == count,
                                modifier = Modifier.weight(1f),
                                onClick = { draft = draft.copy(minBathrooms = count) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 8. Availability & Verification ────────────────────────────
                FilterSection(title = "Listing Status", icon = Icons.Default.CheckCircle) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterToggleRow(
                            label = "Available properties only",
                            subtitle = "Hide properties that are no longer available",
                            checked = draft.availableOnly,
                            onToggle = { draft = draft.copy(availableOnly = it) }
                        )
                        FilterToggleRow(
                            label = "Verified listings only",
                            subtitle = "Show only admin-verified properties",
                            checked = draft.verifiedOnly,
                            onToggle = { draft = draft.copy(verifiedOnly = it) }
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 9. Amenities ──────────────────────────────────────────────
                FilterSection(title = "Must-Have Amenities", icon = Icons.Default.Star) {
                    val amenities = listOf(
                        "WiFi / Internet", "Parking", "Water 24/7", "Generator / Backup Power",
                        "Security Guard", "CCTV", "Swimming Pool", "Gym", "Garden / Yard",
                        "Furnished", "Air Conditioning", "Laundry", "Borehole", "Solar Power",
                        "Electric Fence", "Satellite TV", "Study Room", "Pet Friendly"
                    )
                    val chunked = amenities.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunked.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { amenity ->
                                    val selected = amenity in draft.requiredAmenities
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (selected) RentOutColors.Primary.copy(0.10f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 0.dp,
                                                color = if (selected) RentOutColors.Primary else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                draft = draft.copy(
                                                    requiredAmenities = if (selected)
                                                        draft.requiredAmenities - amenity
                                                    else
                                                        draft.requiredAmenities + amenity
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            null,
                                            tint = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            amenity, fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Bottom padding
                Spacer(Modifier.height(8.dp))
            }

            // ── Apply button ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { onApply(draft); onApplyCity(draftCity) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RentOutColors.Primary)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Show Results",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }  // end Column
        }  // end Card
    }  // end Dialog
}

// ── Filter section wrapper ────────────────────────────────────────────────────
@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        content()
    }
}

// ── Single-select chip ────────────────────────────────────────────────────────
@Composable
private fun SmallSelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) RentOutColors.Primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Radio row ─────────────────────────────────────────────────────────────────
@Composable
private fun FilterRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RentOutColors.Primary.copy(0.08f) else Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = RentOutColors.Primary)
        )
        Text(
            label, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Toggle row ────────────────────────────────────────────────────────────────
@Composable
private fun FilterToggleRow(label: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) RentOutColors.Primary.copy(0.08f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val isDark = isSystemInDarkTheme()
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                // ON state
                checkedThumbColor        = Color.White,
                checkedTrackColor        = RentOutColors.Primary,
                checkedBorderColor       = RentOutColors.Primary,
                checkedIconColor         = RentOutColors.Primary,
                // OFF state — clearly visible in both light & dark
                uncheckedThumbColor      = if (isDark) Color(0xFFB0B8C8) else Color(0xFF6B7280),
                uncheckedTrackColor      = if (isDark) Color(0xFF2D3748) else Color(0xFFD1D5DB),
                uncheckedBorderColor     = if (isDark) Color(0xFF4A5568) else Color(0xFF9CA3AF),
            )
        )
    }
}

// ── Dropdown placeholder (for future date/range pickers) ──────────────────────
@Composable
private fun FilterDropdownButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
