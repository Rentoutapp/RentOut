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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Property
import org.example.project.data.model.User
import org.example.project.data.model.ZIMBABWE_TOWNS
import org.example.project.data.model.suburbsForTown
import org.example.project.presentation.PropertyListState
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
    onSearchQueryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    onUnlockedClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val listState = rememberLazyListState()
    val isFabVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val properties = (propertyListState as? PropertyListState.Success)?.properties ?: emptyList()
    val filtered = properties.filter { prop ->
        (selectedCity.isBlank() || selectedCity == "All" ||
         prop.city.equals(selectedCity, ignoreCase = true) ||
         prop.location.contains(selectedCity, ignoreCase = true)) &&
        (searchQuery.isBlank() || prop.title.contains(searchQuery, ignoreCase = true) ||
         prop.city.contains(searchQuery, ignoreCase = true) ||
         prop.location.contains(searchQuery, ignoreCase = true))
    }
    var showTownPicker by remember { mutableStateOf(false) }

    // Town picker dialog — rendered outside LazyColumn to avoid Dialog-in-list issues
    if (showTownPicker) {
        TenantTownPickerDialog(
            selectedTown = selectedCity,
            onSelect = { town ->
                onCityChange(town)
                showTownPicker = false
            },
            onSelectAll = {
                onCityChange("All")
                showTownPicker = false
            },
            onDismiss = { showTownPicker = false }
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

                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search by city, location, name...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = RentOutColors.IconBlue) },
                            trailingIcon = if (searchQuery.isNotEmpty()) ({
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }) else null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
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
                        "${filtered.size} ${if (filtered.size == 1) "property" else "properties"} found" +
                        if (!isAllTowns) " in $selectedCity" else "",
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
                        if (searchQuery.isNotEmpty() || selectedCity != "All") {
                            Spacer(Modifier.height(16.dp))
                            RentOutSecondaryButton("Clear Filters", onClick = {
                                onSearchQueryChange(""); onCityChange("All")
                            })
                        }
                    }
                }
                else -> items(filtered, key = { it.id }) { property ->
                    PropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateItem()
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
