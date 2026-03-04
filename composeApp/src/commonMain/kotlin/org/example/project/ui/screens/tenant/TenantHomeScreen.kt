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
import org.example.project.presentation.PropertyListState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

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

    val cities = listOf("All", "Harare", "Bulawayo", "Mutare", "Gweru", "Masvingo")
    val properties = (propertyListState as? PropertyListState.Success)?.properties ?: emptyList()
    val filtered = properties.filter { prop ->
        (selectedCity == "All" || prop.city.equals(selectedCity, ignoreCase = true)) &&
        (searchQuery.isBlank() || prop.title.contains(searchQuery, ignoreCase = true) ||
         prop.city.contains(searchQuery, ignoreCase = true) ||
         prop.location.contains(searchQuery, ignoreCase = true))
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
                            Column {
                                Text("Find Your", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("Perfect Home 🔑", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = RentOutColors.Primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // City filter row
            item {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cities) { city ->
                        val isSelected = selectedCity == city
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCityChange(city) },
                            label = { Text(city, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RentOutColors.Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Results count
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filtered.size} properties found",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filtered.isNotEmpty()) {
                        Text(
                            "✅ All Verified",
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

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    indication = null,
    onClick = onClick
)
