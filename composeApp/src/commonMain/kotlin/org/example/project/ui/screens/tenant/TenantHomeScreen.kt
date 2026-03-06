@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Property
import org.example.project.data.model.User
import org.example.project.data.model.ZIMBABWE_TOWNS
import org.example.project.data.model.suburbsForTown
import org.example.project.presentation.PropertyFilter
import org.example.project.presentation.PropertyListState
import org.example.project.presentation.SortOption
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantHomeScreen(
    user: User,
    propertyListState: PropertyListState,
    searchQuery: String,
    selectedCity: String,
    unlockedPropertyIds: Set<String> = emptySet(),
    activeFilter: PropertyFilter = PropertyFilter(),
    onSearchQueryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onFilterChange: (PropertyFilter) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onPropertyClick: (Property) -> Unit,
    onUnlockedClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    applyFilters: (List<Property>, String, String, PropertyFilter) -> List<Property> = { props, query, city, filter ->
        // Fallback local implementation if not provided
        props.filter { p ->
            (city.isBlank() || city == "All" || p.city.equals(city, ignoreCase = true)) &&
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

    var showTownPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Town picker dialog
    if (showTownPicker) {
        TenantTownPickerDialog(
            selectedTown = selectedCity,
            onSelect = { town -> onCityChange(town); showTownPicker = false },
            onSelectAll = { onCityChange("All"); showTownPicker = false },
            onDismiss = { showTownPicker = false }
        )
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        PropertyFilterSheet(
            current = activeFilter,
            onApply = { newFilter -> onFilterChange(newFilter); showFilterSheet = false },
            onDismiss = { showFilterSheet = false },
            onReset = { onClearFilter(); showFilterSheet = false }
        )
    }

    Scaffold(
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
                FloatingActionButton(
                    onClick = onUnlockedClick,
                    containerColor = RentOutColors.Secondary,
                    contentColor = Color.White,
                    modifier = Modifier.scale(fabScale),
                    interactionSource = interactionSource
                ) {
                    Icon(Icons.Default.Key, "My Unlocked Properties")
                }
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
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f)),
                                endY = 400f
                            )
                        )
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (greeting, emoji) = rememberGreeting(user.name.ifBlank { "Tenant" })
                            Column {
                                Text("$emoji  $greeting", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                                Text("Find Your Perfect Home 🔑", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .noRippleClickable(onClick = onProfileClick),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .noRippleClickable(onClick = onLogout),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Logout, "Logout", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Search bar + filter button row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        "Search by city, location, name...",
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.8f))
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) ({
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.8f))
                                    }
                                }) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            // Filter button with active badge
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .shadow(4.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (activeFilter.isActive)
                                                RentOutColors.Secondary
                                            else
                                                Color.White.copy(alpha = 0.18f)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showFilterSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Filter",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // Active filter count badge
                                if (activeFilter.activeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${activeFilter.activeCount}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RentOutColors.Secondary
                                        )
                                    }
                                }
                            }
                        }

                        // Active filter chips row
                        if (activeFilter.isActive) {
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                // Price chip
                                if (activeFilter.minPrice != null || activeFilter.maxPrice != null) {
                                    item {
                                        ActiveFilterChip(
                                            label = buildString {
                                                append("$")
                                                append(activeFilter.minPrice?.toInt() ?: 0)
                                                append(" – $")
                                                append(activeFilter.maxPrice?.toInt()?.toString() ?: "∞")
                                            },
                                            onRemove = { onFilterChange(activeFilter.copy(minPrice = null, maxPrice = null)) }
                                        )
                                    }
                                }
                                // Property type chips
                                activeFilter.propertyTypes.forEach { type ->
                                    item {
                                        ActiveFilterChip(
                                            label = type.replaceFirstChar { it.uppercase() },
                                            onRemove = { onFilterChange(activeFilter.copy(propertyTypes = activeFilter.propertyTypes - type)) }
                                        )
                                    }
                                }
                                // Bedrooms chip
                                if (activeFilter.minBedrooms != null) {
                                    item {
                                        val label = if (activeFilter.maxBedrooms != null)
                                            "${activeFilter.minBedrooms}–${activeFilter.maxBedrooms} beds"
                                        else "${activeFilter.minBedrooms}+ beds"
                                        ActiveFilterChip(label = label, onRemove = { onFilterChange(activeFilter.copy(minBedrooms = null, maxBedrooms = null)) })
                                    }
                                }
                                // Bathrooms chip
                                if (activeFilter.minBathrooms != null) {
                                    item {
                                        ActiveFilterChip(
                                            label = "${activeFilter.minBathrooms}+ baths",
                                            onRemove = { onFilterChange(activeFilter.copy(minBathrooms = null)) }
                                        )
                                    }
                                }
                                // Available only
                                if (activeFilter.availableOnly) {
                                    item { ActiveFilterChip("Available Only", onRemove = { onFilterChange(activeFilter.copy(availableOnly = false)) }) }
                                }
                                // Verified only
                                if (activeFilter.verifiedOnly) {
                                    item { ActiveFilterChip("Verified Only", onRemove = { onFilterChange(activeFilter.copy(verifiedOnly = false)) }) }
                                }
                                // Amenity chips
                                activeFilter.requiredAmenities.forEach { amenity ->
                                    item {
                                        ActiveFilterChip(
                                            label = amenity,
                                            onRemove = { onFilterChange(activeFilter.copy(requiredAmenities = activeFilter.requiredAmenities - amenity)) }
                                        )
                                    }
                                }
                                // Sort chip
                                if (activeFilter.sortBy != SortOption.NEWEST) {
                                    item { ActiveFilterChip("Sort: ${activeFilter.sortBy.label}", onRemove = { onFilterChange(activeFilter.copy(sortBy = SortOption.NEWEST)) }) }
                                }
                                // Clear all chip
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onClearFilter() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Clear All", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Town picker + results bar
            item {
                Spacer(Modifier.height(8.dp))

                // ── Town picker tappable field ────────────────────────────────
                val isAllTowns = selectedCity.isBlank() || selectedCity == "All"
                val suburbCount = if (!isAllTowns) suburbsForTown(selectedCity).size else 0

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Town picker button
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "town_btn_scale"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (!isAllTowns) 2.dp else 1.dp,
                                color = if (!isAllTowns) RentOutColors.Primary
                                        else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(
                                if (!isAllTowns) RentOutColors.Primary.copy(alpha = 0.06f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { showTownPicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = if (!isAllTowns) RentOutColors.Primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Town / City",
                                    fontSize = 10.sp,
                                    color = if (!isAllTowns) RentOutColors.Primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isAllTowns) "All Towns" else selectedCity,
                                    fontSize = 14.sp,
                                    fontWeight = if (!isAllTowns) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isAllTowns) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (!isAllTowns) RentOutColors.Primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // "All" clear button — only visible when a town is selected
                    AnimatedVisibility(
                        visible = !isAllTowns,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit  = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable { onCityChange("All") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear town filter",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Suburb count hint when a town is selected and has known suburbs
                AnimatedVisibility(
                    visible = !isAllTowns && suburbCount > 0,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RentOutColors.IconTeal.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Map, null,
                            tint = RentOutColors.IconTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Showing properties in $selectedCity · $suburbCount suburbs covered",
                            fontSize = 11.sp,
                            color = RentOutColors.IconTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Results count row
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        buildString {
                            append("${filtered.size} ${if (filtered.size == 1) "property" else "properties"} found")
                            if (!isAllTowns) append(" in $selectedCity")
                            if (activeFilter.isActive) append(" · ${activeFilter.activeCount} filter${if (activeFilter.activeCount > 1) "s" else ""} active")
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filtered.isNotEmpty()) {
                        Text(
                            "✓ Verified",
                            fontSize = 12.sp, color = RentOutColors.StatusApproved,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            when {
                propertyListState is PropertyListState.Loading -> item { FullScreenLoader("Finding properties...") }
                filtered.isEmpty() -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏘️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No properties found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Try adjusting your search or filters", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        if (searchQuery.isNotEmpty() || selectedCity != "All" || activeFilter.isActive) {
                            Spacer(Modifier.height(16.dp))
                            RentOutSecondaryButton("Clear All Filters", onClick = {
                                onSearchQueryChange("")
                                onCityChange("All")
                                onClearFilter()
                            })
                        }
                    }
                }
                else -> items(filtered, key = { it.id }) { property ->
                    PropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem(),
                        isUnlocked = unlockedPropertyIds.contains(property.id)
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
    val isAllTowns = selectedTown.isBlank() || selectedTown == "All"

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
                    placeholder = { Text("Search Zimbabwe towns...", fontSize = 14.sp) },
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

                        // "Gweru — default" banner when nothing is selected yet
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
                                    "Gweru is our launch city — most listings are here!",
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
                                // Icon with "Gweru = default" badge
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
                                    // "Default" star badge for Gweru
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
    onApply: (PropertyFilter) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    // Local draft — user can cancel without affecting the active filter
    var draft by remember { mutableStateOf(current) }

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
                        Brush.horizontalGradient(listOf(RentOutColors.Primary, RentOutColors.PrimaryLight))
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

                // ── 1. Sort By ────────────────────────────────────────────────
                FilterSection(title = "Sort By", icon = Icons.Default.Sort) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortOption.entries.forEach { option ->
                            FilterRadioRow(
                                label = option.label,
                                selected = draft.sortBy == option,
                                onClick = { draft = draft.copy(sortBy = option) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.15f))

                // ── 2. Price Range ────────────────────────────────────────────
                FilterSection(title = "Monthly Rent (USD)", icon = Icons.Default.AttachMoney) {
                    val priceOptions = listOf(null, 50.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 750.0, 1000.0, 1500.0, 2000.0)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Min price
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Min Price", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                FilterDropdownButton(
                                    label = draft.minPrice?.let { "$${it.toInt()}" } ?: "No min",
                                    onClick = {}
                                )
                                // Quick select chips for min
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp)) {
                                    items(priceOptions.take(8)) { price ->
                                        SmallSelectChip(
                                            label = price?.let { "$${it.toInt()}" } ?: "Any",
                                            selected = draft.minPrice == price,
                                            onClick = { draft = draft.copy(minPrice = price) }
                                        )
                                    }
                                }
                            }
                        }
                        // Max price chips
                        Text("Max Price", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(null, 100.0, 200.0, 300.0, 500.0, 750.0, 1000.0, 1500.0, 2000.0)) { price ->
                                SmallSelectChip(
                                    label = price?.let { "$${it.toInt()}" } ?: "Any",
                                    selected = draft.maxPrice == price,
                                    onClick = { draft = draft.copy(maxPrice = price) }
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

                // ── 4. Bedrooms ───────────────────────────────────────────────
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

                // ── 5. Bathrooms ──────────────────────────────────────────────
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

                // ── 6. Availability & Verification ────────────────────────────
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

                // ── 7. Amenities ──────────────────────────────────────────────
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
                    onClick = { onApply(draft) },
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
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RentOutColors.Primary)
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
