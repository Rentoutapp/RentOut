@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.data.model.SADC_COUNTRIES
import org.example.project.data.model.SadcCountry
import org.example.project.data.model.ZIMBABWE_TOWNS
import org.example.project.data.model.ZimTown
import org.example.project.data.model.suburbsForTown
import org.example.project.ui.theme.RentOutColors

// ─── Country Picker Field ────────────────────────────────────────────────────

@Composable
fun CountryPickerField(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }

    LocationPickerField(
        value = selectedCountry,
        label = "Country",
        leadingIcon = Icons.Default.Public,
        leadingIconTint = RentOutColors.IconBlue,
        placeholder = "Select country",
        trailingIcon = Icons.Default.KeyboardArrowDown,
        onClick = { showDialog = true },
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage
    )

    if (showDialog) {
        CountryPickerDialog(
            selectedCountry = selectedCountry,
            onSelect = { onCountrySelected(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

// ─── Town Picker Field ────────────────────────────────────────────────────────

@Composable
fun TownPickerField(
    selectedTown: String,
    selectedCountry: String,
    onTownSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }
    val isZimbabwe = selectedCountry.equals("Zimbabwe", ignoreCase = true)

    LocationPickerField(
        value = selectedTown,
        label = "Town / City",
        leadingIcon = Icons.Default.LocationCity,
        leadingIconTint = RentOutColors.IconTeal,
        placeholder = if (isZimbabwe) "Select town" else "Enter town / city",
        trailingIcon = if (isZimbabwe) Icons.Default.KeyboardArrowDown else null,
        onClick = { if (isZimbabwe) showDialog = true },
        isTextField = !isZimbabwe,
        onTextChange = { if (!isZimbabwe) onTownSelected(it) },
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage
    )

    if (showDialog && isZimbabwe) {
        TownPickerDialog(
            selectedTown = selectedTown,
            onSelect = { onTownSelected(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

// ─── Suburb Picker Field ──────────────────────────────────────────────────────

@Composable
fun SuburbPickerField(
    selectedSuburb: String,
    selectedTown: String,
    selectedCountry: String,
    onSuburbSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }
    val isZimbabwe = selectedCountry.equals("Zimbabwe", ignoreCase = true)
    val suburbs = remember(selectedTown) { suburbsForTown(selectedTown) }
    val hasTownSuburbs = isZimbabwe && suburbs.isNotEmpty()

    LocationPickerField(
        value = selectedSuburb,
        label = "Suburb",
        leadingIcon = Icons.Default.Map,
        leadingIconTint = RentOutColors.IconPurple,
        placeholder = when {
            !isZimbabwe -> "Enter suburb"
            selectedTown.isBlank() -> "Select town first"
            suburbs.isEmpty() -> "Enter suburb"
            else -> "Select suburb in $selectedTown"
        },
        trailingIcon = if (hasTownSuburbs) Icons.Default.KeyboardArrowDown else null,
        onClick = { if (hasTownSuburbs) showDialog = true },
        isTextField = !hasTownSuburbs,
        onTextChange = { if (!hasTownSuburbs) onSuburbSelected(it) },
        enabled = isZimbabwe && selectedTown.isBlank() || !isZimbabwe || selectedTown.isNotBlank(),
        modifier = modifier,
        isError = isError,
        errorMessage = errorMessage
    )

    if (showDialog && hasTownSuburbs) {
        SuburbPickerDialog(
            town = selectedTown,
            suburbs = suburbs,
            selectedSuburb = selectedSuburb,
            onSelect = { onSuburbSelected(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

// ─── Generic Picker Field (tappable or text) ──────────────────────────────────

@Composable
private fun LocationPickerField(
    value: String,
    label: String,
    leadingIcon: ImageVector,
    leadingIconTint: Color,
    placeholder: String,
    trailingIcon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTextField: Boolean = false,
    onTextChange: (String) -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier) {
        if (isTextField) {
            OutlinedTextField(
                value = value,
                onValueChange = onTextChange,
                label = { Text(label) },
                leadingIcon = {
                    Icon(leadingIcon, null, tint = leadingIconTint, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                enabled = enabled,
                isError = isError,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val borderColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    value.isNotBlank() -> RentOutColors.Primary
                    else -> MaterialTheme.colorScheme.outline
                },
                label = "border"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (value.isNotBlank()) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (enabled) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled
                    ) { onClick() }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        leadingIcon, null,
                        tint = if (value.isNotBlank()) leadingIconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (value.isNotBlank()) borderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = value.ifBlank { placeholder },
                            fontSize = 15.sp,
                            color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (trailingIcon != null) {
                        Icon(
                            trailingIcon, null,
                            tint = if (value.isNotBlank()) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        if (isError && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ─── Country Picker Dialog ────────────────────────────────────────────────────

@Composable
private fun CountryPickerDialog(
    selectedCountry: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) SADC_COUNTRIES
        else SADC_COUNTRIES.filter { it.name.contains(search, ignoreCase = true) }
    }

    PickerDialog(
        title = "Select Country",
        icon = Icons.Default.Public,
        iconTint = RentOutColors.IconBlue,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Search SADC countries...",
        onDismiss = onDismiss
    ) {
        itemsIndexed(filtered) { index, country ->
            CountryRow(
                country = country,
                isSelected = country.name == selectedCountry,
                index = index,
                onClick = { onSelect(country.name) }
            )
        }
    }
}

// ─── Town Picker Dialog ───────────────────────────────────────────────────────

@Composable
private fun TownPickerDialog(
    selectedTown: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) ZIMBABWE_TOWNS
        else ZIMBABWE_TOWNS.filter { it.name.contains(search, ignoreCase = true) }
    }

    PickerDialog(
        title = "Select Town / City",
        icon = Icons.Default.LocationCity,
        iconTint = RentOutColors.IconTeal,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Search Zimbabwe towns...",
        onDismiss = onDismiss
    ) {
        itemsIndexed(filtered) { index, town ->
            TownRow(
                town = town,
                isSelected = town.name == selectedTown,
                index = index,
                onClick = { onSelect(town.name) }
            )
        }
    }
}

// ─── Suburb Picker Dialog ─────────────────────────────────────────────────────

@Composable
private fun SuburbPickerDialog(
    town: String,
    suburbs: List<String>,
    selectedSuburb: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) suburbs
        else suburbs.filter { it.contains(search, ignoreCase = true) }
    }

    PickerDialog(
        title = "Select Suburb",
        subtitle = town,
        icon = Icons.Default.Map,
        iconTint = RentOutColors.IconPurple,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Search suburbs in $town...",
        onDismiss = onDismiss
    ) {
        itemsIndexed(filtered) { index, suburb ->
            SuburbRow(
                suburb = suburb,
                isSelected = suburb == selectedSuburb,
                index = index,
                onClick = { onSelect(suburb) }
            )
        }
    }
}

// ─── Shared Picker Dialog Shell ───────────────────────────────────────────────

@Composable
private fun PickerDialog(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    iconTint: Color,
    searchValue: String,
    onSearchChange: (String) -> Unit,
    searchPlaceholder: String,
    onDismiss: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
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

                // Header gradient
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
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, fontSize = 13.sp, color = Color.White.copy(0.8f))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchValue,
                    onValueChange = onSearchChange,
                    placeholder = { Text(searchPlaceholder, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = RentOutColors.Primary) },
                    trailingIcon = {
                        AnimatedVisibility(searchValue.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))

                // Scrollable list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// ─── Row Composables ──────────────────────────────────────────────────────────

@Composable
private fun CountryRow(
    country: SadcCountry,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    // No staggered delay — same fix as SuburbRow; delayed visibility in a
    // LazyColumn breaks virtualisation and prevents scrolling past visible items.
    PickerRow(
        isSelected = isSelected,
        onClick = onClick
    ) {
        Text(text = country.flag, fontSize = 26.sp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = country.name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = country.dialCode,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun TownRow(
    town: ZimTown,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    // No staggered delay — same fix as SuburbRow and CountryRow.
    PickerRow(isSelected = isSelected, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) RentOutColors.Primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationCity, null,
                tint = if (isSelected) RentOutColors.Primary else RentOutColors.IconTeal,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = town.name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${town.suburbs.size} suburbs available",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SuburbRow(
    suburb: String,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    // No staggered delay — LazyColumn virtualises items so delayed visibility
    // causes items further down the list to never appear, breaking scroll.
    PickerRow(isSelected = isSelected, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) RentOutColors.Primary
                    else RentOutColors.IconPurple.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Place, null,
                tint = if (isSelected) Color.White else RentOutColors.IconPurple,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = suburb,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(22.dp))
        }
    }
}

// ─── Generic Row Shell ────────────────────────────────────────────────────────

@Composable
private fun PickerRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) RentOutColors.Primary.copy(alpha = 0.07f)
                      else Color.Transparent,
        label = "row_bg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        content = content
    )
}
