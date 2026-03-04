@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.landlord

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import org.example.project.data.model.Property
import org.example.project.presentation.PropertyFormState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

@Composable
fun AddPropertyScreen(
    formState: PropertyFormState,
    onSubmit: (Property) -> Unit,
    onBack: () -> Unit
) {
    var title       by remember { mutableStateOf("") }
    var city        by remember { mutableStateOf("") }
    var location    by remember { mutableStateOf("") }
    var price       by remember { mutableStateOf("") }
    var rooms       by remember { mutableStateOf("") }
    var bathrooms   by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contact     by remember { mutableStateOf("") }
    var propType    by remember { mutableStateOf("apartment") }

    // Validation errors
    var titleErr   by remember { mutableStateOf("") }
    var cityErr    by remember { mutableStateOf("") }
    var priceErr   by remember { mutableStateOf("") }
    var roomsErr   by remember { mutableStateOf("") }
    var contactErr by remember { mutableStateOf("") }
    var descErr    by remember { mutableStateOf("") }

    val isLoading = formState is PropertyFormState.Uploading
    val scrollState = rememberScrollState()

    // Back button animation state
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.8f else 1f,
        animationSpec = tween(200), label = "back_scale"
    )
    val backRotation by animateFloatAsState(
        targetValue = if (backPressed) -45f else 0f,
        animationSpec = tween(200), label = "back_rotation"
    )

    // Show success snackbar
    if (formState is PropertyFormState.Success) {
        LaunchedEffect(formState) { onBack() }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { backPressed = true; onBack() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("Add Property", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(28.dp))

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    SectionLabel("📋 Basic Information")
                    Spacer(Modifier.height(12.dp))

                    RentOutTextField(
                        value = title, onValueChange = { title = it; titleErr = "" },
                        label = "Property Title",
                        leadingIcon = Icons.Default.Apartment, leadingIconTint = RentOutColors.IconBlue,
                        isError = titleErr.isNotEmpty(), errorMessage = titleErr
                    )
                    Spacer(Modifier.height(14.dp))

                    // Property type selector
                    Text("Property Type", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("apartment", "house", "room", "commercial").forEach { type ->
                            val selected = propType == type
                            FilterChip(
                                selected = selected,
                                onClick = { propType = type },
                                label = { Text(type.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RentOutColors.Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RentOutTextField(
                            value = city, onValueChange = { city = it; cityErr = "" },
                            label = "City",
                            leadingIcon = Icons.Default.LocationCity, leadingIconTint = RentOutColors.IconTeal,
                            isError = cityErr.isNotEmpty(), errorMessage = cityErr,
                            modifier = Modifier.weight(1f)
                        )
                        RentOutTextField(
                            value = rooms, onValueChange = { rooms = it; roomsErr = "" },
                            label = "Rooms",
                            leadingIcon = Icons.Default.Home, leadingIconTint = RentOutColors.IconBlue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = roomsErr.isNotEmpty(), errorMessage = roomsErr,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RentOutTextField(
                            value = price, onValueChange = { price = it; priceErr = "" },
                            label = "Price (USD/mo)",
                            leadingIcon = Icons.Default.Payments, leadingIconTint = RentOutColors.IconGreen,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = priceErr.isNotEmpty(), errorMessage = priceErr,
                            modifier = Modifier.weight(1f)
                        )
                        RentOutTextField(
                            value = bathrooms, onValueChange = { bathrooms = it },
                            label = "Bathrooms",
                            leadingIcon = Icons.Default.Star, leadingIconTint = RentOutColors.IconTeal,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    RentOutTextField(
                        value = location, onValueChange = { location = it },
                        label = "Full Address / Suburb",
                        leadingIcon = Icons.Default.LocationOn, leadingIconTint = RentOutColors.IconRose
                    )
                    Spacer(Modifier.height(20.dp))

                    SectionLabel("📝 Description")
                    Spacer(Modifier.height(12.dp))
                    RentOutTextField(
                        value = description, onValueChange = { description = it; descErr = "" },
                        label = "Describe the property...",
                        leadingIcon = Icons.Default.Article, leadingIconTint = RentOutColors.IconSlate,
                        singleLine = false, maxLines = 5,
                        isError = descErr.isNotEmpty(), errorMessage = descErr
                    )
                    Spacer(Modifier.height(20.dp))

                    SectionLabel("📞 Contact Details")
                    Spacer(Modifier.height(12.dp))
                    RentOutTextField(
                        value = contact, onValueChange = { contact = it; contactErr = "" },
                        label = "Contact Number (hidden from tenants)",
                        leadingIcon = Icons.Default.Call, leadingIconTint = RentOutColors.IconAmber,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = contactErr.isNotEmpty(), errorMessage = contactErr
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = RentOutColors.IconSlate, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Your contact number is only revealed after a tenant pays \$10",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    // Submit button
                    RentOutPrimaryButton(
                        text = "Submit for Review",
                        onClick = {
                            var valid = true
                            if (title.isBlank()) { titleErr = "Title is required"; valid = false }
                            if (city.isBlank()) { cityErr = "City is required"; valid = false }
                            if (price.isBlank() || price.toDoubleOrNull() == null) { priceErr = "Enter a valid price"; valid = false }
                            if (rooms.isBlank() || rooms.toIntOrNull() == null) { roomsErr = "Enter valid room count"; valid = false }
                            if (contact.isBlank()) { contactErr = "Contact number is required"; valid = false }
                            if (description.isBlank()) { descErr = "Description is required"; valid = false }
                            if (valid) {
                                onSubmit(
                                    Property(
                                        title = title.trim(),
                                        city = city.trim(),
                                        location = location.trim(),
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        rooms = rooms.toIntOrNull() ?: 1,
                                        bathrooms = bathrooms.toIntOrNull() ?: 1,
                                        description = description.trim(),
                                        contactNumber = contact.trim(),
                                        propertyType = propType,
                                        status = "pending"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoading
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⏳ Your listing will be reviewed by our admin team before it goes live.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(2.dp))
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}
