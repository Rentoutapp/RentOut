@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Property
import org.example.project.presentation.PropertyDraft
import org.example.project.presentation.PropertyFormState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.components.CountryPickerField
import org.example.project.ui.components.TownPickerField
import org.example.project.ui.components.SuburbPickerField

// --- Data Models -----------------------------------------------------------

data class PropertyAddress(
    val houseAndStreet: String = "",
    val townOrCity: String = "Gweru",
    val suburb: String = "",
    val country: String = "Zimbabwe"
) {
    val isComplete: Boolean
        get() = houseAndStreet.isNotBlank() && suburb.isNotBlank() &&
                townOrCity.isNotBlank() && country.isNotBlank()
}

// --- Main Screen ------------------------------------------------------------

@Composable
fun AddPropertyScreen(
    formState: PropertyFormState,
    onSubmit: (Property) -> Unit,
    onBack: () -> Unit,
    onNavigateToImages: (Property) -> Unit = {},
    landlordPhoneNumber: String = "",   // auto-filled from landlord profile
    draft: PropertyDraft = PropertyDraft(),
    onSaveDraft: (PropertyDraft) -> Unit = {},
    isEditMode: Boolean = false,
    existingImageUrls: List<String> = emptyList()  // pre-loaded images shown in edit mode
) {
    // -- Form state: seeded from saved draft so values survive navigation --
    var title           by remember { mutableStateOf(draft.title) }
    var price           by remember { mutableStateOf(draft.price) }
    var securityDeposit by remember { mutableStateOf(draft.securityDeposit) }
    var rooms           by remember { mutableStateOf(draft.rooms) }
    var bathrooms       by remember { mutableStateOf(draft.bathrooms) }
    var description     by remember { mutableStateOf(draft.description) }
    var propType        by remember { mutableStateOf(draft.propType) }
    var address         by remember {
        mutableStateOf(
            PropertyAddress(
                houseAndStreet = draft.houseAndStreet,
                townOrCity     = draft.townOrCity,
                suburb         = draft.suburb,
                country        = draft.country
            )
        )
    }

    // -- Contact: prefer saved draft value, fall back to landlord profile number --
    var contact by remember {
        mutableStateOf(draft.contact.ifEmpty { landlordPhoneNumber })
    }
    val isContactAutoFilled = landlordPhoneNumber.isNotBlank()

    // -- Amenity selection -- seeded from draft; only reset when property TYPE changes --
    var selectedAmenityKeys by remember { mutableStateOf<Set<String>>(draft.amenityKeys) }
    var lastPropType        by remember { mutableStateOf(draft.propType) }
    LaunchedEffect(propType) {
        // Only wipe amenities when the user actively changes the property type,
        // not on initial composition (where propType == lastPropType from the draft).
        if (propType != lastPropType) {
            selectedAmenityKeys = emptySet()
            lastPropType = propType
        }
    }
    val amenityDefs = remember(propType) { PropertyAmenities.forType(propType) }

    // -- Validation errors --
    var titleErr   by remember { mutableStateOf("") }
    var priceErr   by remember { mutableStateOf("") }
    var roomsErr   by remember { mutableStateOf("") }
    var contactErr by remember { mutableStateOf("") }
    var descErr    by remember { mutableStateOf("") }
    var addressErr by remember { mutableStateOf("") }

    // Derived: suburb suffix for title
    val titleWithSuburb = remember(title, address.suburb) {
        val base = title.trim()
        val sub  = address.suburb.trim()
        if (base.isNotEmpty() && sub.isNotEmpty()) "$base in $sub"
        else if (base.isNotEmpty()) base
        else ""
    }

    val isLoading   = formState is PropertyFormState.Uploading
    val scrollState = rememberScrollState()

    // -- Back button animation --
    var backPressed  by remember { mutableStateOf(false) }
    val backScale    by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f, tween(200), label = "br")

    if (formState is PropertyFormState.Success) {
        LaunchedEffect(formState) { onBack() }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // -- Header gradient --
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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

            // -- Top bar --
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { backPressed = true; onBack() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isEditMode) "Edit Property" else "Add Property",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(28.dp))

            // -- Form card --
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // -- Section: Listing Information (centered) --
                    AddPropertySectionLabel(
                        icon = Icons.Default.Assignment,
                        text = "Listing Information",
                        centered = true
                    )
                    Spacer(Modifier.height(16.dp))

                    // -- Property Title --
                    RentOutTextField(
                        value = title,
                        onValueChange = { title = it; titleErr = "" },
                        label = "Property Title",
                        leadingIcon = Icons.Default.Apartment,
                        leadingIconTint = RentOutColors.IconBlue,
                        isError = titleErr.isNotEmpty(),
                        errorMessage = titleErr,
                        labelFontSize = 12.sp
                    )
                    // Title preview with suburb suffix
                    AnimatedVisibility(
                        visible = titleWithSuburb.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = RentOutColors.IconTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Preview: \"$titleWithSuburb\"",
                                fontSize = 12.sp,
                                color = RentOutColors.IconTeal,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // -- Property Type --
                    Text(
                        text = "Property Type",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(
                            listOf(
                                Triple("apartment", Icons.Default.Apartment,   "Apartment"),
                                Triple("house",     Icons.Default.House,       "House"),
                                Triple("room",      Icons.Default.MeetingRoom, "Room"),
                                Triple("commercial",Icons.Default.Store,       "Commercial")
                            )
                        ) { (type, icon, label) ->
                            PropertyTypeChip(
                                icon     = icon,
                                label    = label,
                                selected = propType == type,
                                onClick  = { propType = type }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    // -- Price & Security Deposit --
                    PricingSection(
                        price              = price,
                        onPriceChange      = { price = it; priceErr = "" },
                        priceError         = priceErr,
                        securityDeposit    = securityDeposit,
                        onDepositChange    = { securityDeposit = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    // -- Property Details --
                    AddPropertySectionLabel(Icons.Default.House, "Property Details")
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RentOutTextField(
                            value = rooms, onValueChange = { rooms = it; roomsErr = "" },
                            label = "Bedrooms",
                            leadingIcon = Icons.Default.Bed, leadingIconTint = RentOutColors.IconBlue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = roomsErr.isNotEmpty(), errorMessage = roomsErr,
                            modifier = Modifier.weight(1f),
                            labelFontSize = 12.sp
                        )
                        RentOutTextField(
                            value = bathrooms, onValueChange = { bathrooms = it },
                            label = "Bathrooms",
                            leadingIcon = Icons.Default.Bathtub, leadingIconTint = RentOutColors.IconTeal,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            labelFontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    // -- Amenities (property-type specific) --
                    AmenitiesSection(
                        propType     = propType,
                        amenityDefs  = amenityDefs,
                        selectedKeys = selectedAmenityKeys,
                        onToggle     = { key, on ->
                            selectedAmenityKeys = if (on) selectedAmenityKeys + key
                                                 else selectedAmenityKeys - key
                        }
                    )
                    Spacer(Modifier.height(20.dp))

                    // -- Description --
                    AddPropertySectionLabel(Icons.Default.Description, "Description")
                    Spacer(Modifier.height(12.dp))
                    RentOutTextField(
                        value = description, onValueChange = { description = it; descErr = "" },
                        label = "Describe the property...",
                        leadingIcon = Icons.Default.Article, leadingIconTint = RentOutColors.IconSlate,
                        singleLine = false, maxLines = 5,
                        isError = descErr.isNotEmpty(), errorMessage = descErr,
                        labelFontSize = 12.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    // -- Property Address --
                    AddPropertySectionLabel(Icons.Default.Place, "Property Address")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Fill in all fields � section collapses after 5 seconds so you can review",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    AnimatedVisibility(visible = addressErr.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = addressErr, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                    PropertyAddressSection(
                        address  = address,
                        onChange = { address = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    // -- Contact Details --
                    AddPropertySectionLabel(Icons.Default.Phone, "Contact Details")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Revealed to tenants only after they pay to unlock",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    ContactDetailsSection(
                        contact    = contact,
                        onContact  = { contact = it; contactErr = "" },
                        contactErr = contactErr,
                        isAutoFilled = isContactAutoFilled
                    )
                    Spacer(Modifier.height(24.dp))

                    // -- Form completeness indicator --
                    FormCompletenessIndicator(
                        hasTitle       = title.isNotBlank(),
                        hasPrice       = price.isNotBlank() && price.toDoubleOrNull() != null,
                        hasRooms       = rooms.isNotBlank() && rooms.toIntOrNull() != null,
                        hasDescription = description.isNotBlank(),
                        hasAddress     = address.isComplete,
                        hasContact     = contact.isNotBlank()
                    )

                    // -- Edit mode: image gallery with preview + manage button --
                    if (isEditMode) {
                        Spacer(Modifier.height(20.dp))
                        ExistingImagesGallery(
                            imageUrls = existingImageUrls,
                            onManagePhotos = {
                                onNavigateToImages(
                                    Property(
                                        title           = title.trim(),
                                        city            = address.townOrCity.trim(),
                                        location        = buildString {
                                            if (address.houseAndStreet.isNotBlank()) append(address.houseAndStreet)
                                            if (address.suburb.isNotBlank())         append(", ${address.suburb}")
                                            if (address.townOrCity.isNotBlank())     append(", ${address.townOrCity}")
                                            if (address.country.isNotBlank())        append(", ${address.country}")
                                        },
                                        price           = price.toDoubleOrNull() ?: 0.0,
                                        securityDeposit = securityDeposit.toDoubleOrNull() ?: 0.0,
                                        rooms           = rooms.toIntOrNull() ?: 1,
                                        bathrooms       = bathrooms.toIntOrNull() ?: 1,
                                        description     = description.trim(),
                                        contactNumber   = contact.trim(),
                                        propertyType    = propType,
                                        amenities       = selectedAmenityKeys.toList(),
                                        status          = "pending"
                                    )
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // -- Shared validation helper --
                    fun buildAndValidate(onValid: (Property) -> Unit) {
                        var valid = true
                        if (title.isBlank())                                   { titleErr   = "Property title is required"; valid = false }
                        if (price.isBlank() || price.toDoubleOrNull() == null) { priceErr   = "Enter a valid price";        valid = false }
                        if (rooms.isBlank() || rooms.toIntOrNull() == null)    { roomsErr   = "Enter valid bedroom count";  valid = false }
                        if (description.isBlank())                             { descErr    = "Description is required";    valid = false }
                        if (!address.isComplete)                               { addressErr = "Please complete all 4 address fields"; valid = false }
                        if (contact.isBlank())                                 { contactErr = "Contact number is required"; valid = false }
                        if (valid) {
                            val amenities    = selectedAmenityKeys.toList()
                            val fullLocation = buildString {
                                if (address.houseAndStreet.isNotBlank()) append(address.houseAndStreet)
                                if (address.suburb.isNotBlank())         append(", ${address.suburb}")
                                if (address.townOrCity.isNotBlank())     append(", ${address.townOrCity}")
                                if (address.country.isNotBlank())        append(", ${address.country}")
                            }
                            onValid(
                                Property(
                                    title           = titleWithSuburb.ifEmpty { title.trim() },
                                    city            = address.townOrCity.trim(),
                                    location        = fullLocation,
                                    price           = price.toDoubleOrNull() ?: 0.0,
                                    securityDeposit = securityDeposit.toDoubleOrNull() ?: 0.0,
                                    rooms           = rooms.toIntOrNull() ?: 1,
                                    bathrooms       = bathrooms.toIntOrNull() ?: 1,
                                    description     = description.trim(),
                                    contactNumber   = contact.trim(),
                                    propertyType    = propType,
                                    amenities       = amenities,
                                    status          = "pending"
                                )
                            )
                        }
                    }

                    // -- Submit / Save button --
                    AddImagesButton(
                        isLoading  = isLoading,
                        isEditMode = isEditMode,
                        onClick    = {
                            if (isEditMode) {
                                buildAndValidate { builtProperty -> onSubmit(builtProperty) }
                            } else {
                                buildAndValidate { builtProperty ->
                                    onSaveDraft(
                                        PropertyDraft(
                                            title           = title,
                                            price           = price,
                                            securityDeposit = securityDeposit,
                                            rooms           = rooms,
                                            bathrooms       = bathrooms,
                                            description     = description,
                                            propType        = propType,
                                            houseAndStreet  = address.houseAndStreet,
                                            townOrCity      = address.townOrCity,
                                            suburb          = address.suburb,
                                            country         = address.country,
                                            contact         = contact,
                                            amenityKeys     = selectedAmenityKeys
                                        )
                                    )
                                    onNavigateToImages(builtProperty)
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Your listing will be reviewed by our admin team before it goes live.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
// --- Helper Composables -----------------------------------------------------

// Section label (centered or left-aligned) � uses real Material Icons, no emoji
@Composable
private fun AddPropertySectionLabel(icon: ImageVector, text: String, centered: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RentOutColors.Primary,
                modifier = Modifier.size(if (centered) 22.dp else 18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = if (centered) 20.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
}

// -- Property type chip --
@Composable
private fun PropertyTypeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.outline,
        label = "chip_border"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// -- Pricing Section: monthly rent + security deposit side by side --
@Composable
private fun PricingSection(
    price: String,
    onPriceChange: (String) -> Unit,
    priceError: String,
    securityDeposit: String,
    onDepositChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AttachMoney,
                contentDescription = null,
                tint = RentOutColors.IconGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Pricing",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        // Monthly Rent � centered, prominent
        Text(
            text = "Monthly Rent",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text("Price in USD / month", fontSize = 12.sp) },
            leadingIcon = {
                Text(
                    text = "$",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RentOutColors.IconGreen,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = priceError.isNotEmpty(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .align(Alignment.CenterHorizontally),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (priceError.isNotBlank()) {
            Text(
                text = priceError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Security Deposit row with icon + info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.IconAmber.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = RentOutColors.IconAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Deposit",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Refundable amount collected upfront",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = securityDeposit,
            onValueChange = onDepositChange,
            label = { Text("Security Deposit (USD)", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = RentOutColors.IconAmber,
                    modifier = Modifier.size(20.dp)
                )
            },
            placeholder = { Text("e.g. 500", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "Optional � leave blank if no deposit required",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
            }
        )
    }
}

// --- PropertyAmenities catalogue --------------------------------------------
object PropertyAmenities {
    data class AmenityDef(val key: String, val label: String, val icon: ImageVector)

    val roomAmenities = listOf(
        AmenityDef("own_entrance",     "Own Entrance",       Icons.Default.DoorFront),
        AmenityDef("own_bathroom",     "Own Bathroom",       Icons.Default.Bathtub),
        AmenityDef("en_suite",         "En-Suite",           Icons.Default.Shower),
        AmenityDef("kitchenette",      "Kitchenette",        Icons.Default.Kitchen),
        AmenityDef("shared_kitchen",   "Shared Kitchen",     Icons.Default.OutdoorGrill),
        AmenityDef("wifi",             "WiFi",               Icons.Default.Wifi),
        AmenityDef("furnished",        "Furnished",          Icons.Default.Chair),
        AmenityDef("semi_furnished",   "Semi-Furnished",     Icons.Default.TableBar),
        AmenityDef("electricity",      "Electricity Incl.",  Icons.Default.ElectricBolt),
        AmenityDef("water",            "Water Incl.",        Icons.Default.WaterDrop),
        AmenityDef("laundry_access",   "Laundry Access",     Icons.Default.LocalLaundryService),
        AmenityDef("parking",          "Parking",            Icons.Default.LocalParking),
        AmenityDef("security",         "Security",           Icons.Default.Security),
        AmenityDef("garden_access",    "Garden Access",      Icons.Default.Yard),
        AmenityDef("pets_allowed",     "Pets Allowed",       Icons.Default.Pets),
        AmenityDef("air_conditioning", "Air Conditioning",   Icons.Default.AcUnit),
        AmenityDef("heater",           "Heater",             Icons.Default.Thermostat),
        AmenityDef("cctv",             "CCTV",               Icons.Default.Videocam),
        AmenityDef("backup_power",     "Backup Power",       Icons.Default.BatteryChargingFull),
        AmenityDef("storage",          "Storage Room",       Icons.Default.Inventory)
    )

    val apartmentAmenities = listOf(
        AmenityDef("wifi",             "WiFi",               Icons.Default.Wifi),
        AmenityDef("parking",          "Parking",            Icons.Default.LocalParking),
        AmenityDef("security",         "24/7 Security",      Icons.Default.Security),
        AmenityDef("cctv",             "CCTV",               Icons.Default.Videocam),
        AmenityDef("intercom",         "Intercom",           Icons.Default.Doorbell),
        AmenityDef("pool",             "Swimming Pool",      Icons.Default.Pool),
        AmenityDef("gym",              "Gym",                Icons.Default.FitnessCenter),
        AmenityDef("rooftop",          "Rooftop Terrace",    Icons.Default.Deck),
        AmenityDef("furnished",        "Furnished",          Icons.Default.Chair),
        AmenityDef("semi_furnished",   "Semi-Furnished",     Icons.Default.TableBar),
        AmenityDef("air_conditioning", "Air Conditioning",   Icons.Default.AcUnit),
        AmenityDef("backup_power",     "Backup Power",       Icons.Default.BatteryChargingFull),
        AmenityDef("borehole",         "Borehole Water",     Icons.Default.WaterDrop),
        AmenityDef("laundry",          "Laundry Room",       Icons.Default.LocalLaundryService),
        AmenityDef("storage",          "Storage Unit",       Icons.Default.Inventory),
        AmenityDef("lift",             "Lift / Elevator",    Icons.Default.Elevator),
        AmenityDef("concierge",        "Concierge",          Icons.Default.SupportAgent),
        AmenityDef("pets_allowed",     "Pets Allowed",       Icons.Default.Pets),
        AmenityDef("balcony",          "Balcony",            Icons.Default.Deck),
        AmenityDef("garden",           "Shared Garden",      Icons.Default.Yard)
    )

    val houseAmenities = listOf(
        AmenityDef("pool",             "Swimming Pool",      Icons.Default.Pool),
        AmenityDef("garage",           "Garage",             Icons.Default.Garage),
        AmenityDef("garden",           "Garden / Yard",      Icons.Default.Yard),
        AmenityDef("wifi",             "WiFi",               Icons.Default.Wifi),
        AmenityDef("security",         "Security System",    Icons.Default.Security),
        AmenityDef("cctv",             "CCTV",               Icons.Default.Videocam),
        AmenityDef("electric_fence",   "Electric Fence",     Icons.Default.Fence),
        AmenityDef("borehole",         "Borehole Water",     Icons.Default.WaterDrop),
        AmenityDef("backup_power",     "Backup Power",       Icons.Default.BatteryChargingFull),
        AmenityDef("servant_quarters", "Servant Quarters",   Icons.Default.OtherHouses),
        AmenityDef("double_garage",    "Double Garage",      Icons.Default.Garage),
        AmenityDef("air_conditioning", "Air Conditioning",   Icons.Default.AcUnit),
        AmenityDef("solar",            "Solar Power",        Icons.Default.WbSunny),
        AmenityDef("pets_allowed",     "Pets Allowed",       Icons.Default.Pets),
        AmenityDef("furnished",        "Furnished",          Icons.Default.Chair),
        AmenityDef("patio",            "Patio / Braai",      Icons.Default.Deck),
        AmenityDef("laundry",          "Laundry Room",       Icons.Default.LocalLaundryService),
        AmenityDef("study",            "Study / Office",     Icons.Default.MenuBook),
        AmenityDef("storage",          "Storage Room",       Icons.Default.Inventory),
        AmenityDef("alarm",            "Alarm System",       Icons.Default.NotificationImportant)
    )

    val commercialAmenities = listOf(
        AmenityDef("parking",          "Parking Bay(s)",     Icons.Default.LocalParking),
        AmenityDef("wifi",             "Fibre / WiFi",       Icons.Default.Wifi),
        AmenityDef("security",         "24/7 Security",      Icons.Default.Security),
        AmenityDef("cctv",             "CCTV",               Icons.Default.Videocam),
        AmenityDef("reception",        "Reception Area",     Icons.Default.MeetingRoom),
        AmenityDef("boardroom",        "Boardroom",          Icons.Default.Groups),
        AmenityDef("air_conditioning", "Air Conditioning",   Icons.Default.AcUnit),
        AmenityDef("backup_power",     "Backup Power",       Icons.Default.BatteryChargingFull),
        AmenityDef("lift",             "Lift / Elevator",    Icons.Default.Elevator),
        AmenityDef("loading_bay",      "Loading Bay",        Icons.Default.LocalShipping),
        AmenityDef("storage",          "Storage / Warehouse",Icons.Default.Inventory),
        AmenityDef("kitchenette",      "Kitchenette",        Icons.Default.Kitchen),
        AmenityDef("ablution",         "Ablution Facilities",Icons.Default.Wc),
        AmenityDef("signage",          "Signage Rights",     Icons.Default.Signpost),
        AmenityDef("open_plan",        "Open Plan",          Icons.Default.ViewQuilt),
        AmenityDef("partitioned",      "Partitioned Offices",Icons.Default.GridView),
        AmenityDef("disabled_access",  "Disabled Access",    Icons.Default.Accessible),
        AmenityDef("canteen",          "Canteen / Cafeteria",Icons.Default.Restaurant),
        AmenityDef("generator",        "Generator",          Icons.Default.OfflineBolt),
        AmenityDef("solar",            "Solar Power",        Icons.Default.WbSunny)
    )

    fun forType(propertyType: String): List<AmenityDef> = when (propertyType) {
        "room"       -> roomAmenities
        "house"      -> houseAmenities
        "commercial" -> commercialAmenities
        else         -> apartmentAmenities
    }
}

// --- AmenitiesSection -------------------------------------------------------
@Composable
private fun AmenitiesSection(
    propType: String,
    amenityDefs: List<PropertyAmenities.AmenityDef>,
    selectedKeys: Set<String>,
    onToggle: (key: String, on: Boolean) -> Unit
) {
    val typeLabel = when (propType) {
        "room"       -> "Room"
        "house"      -> "House"
        "commercial" -> "Commercial"
        else         -> "Apartment"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Amenities", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RentOutColors.Primary.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Info, null, tint = RentOutColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Showing amenities for: $typeLabel. Change property type above to see different options.",
                fontSize = 11.sp, color = RentOutColors.Primary, fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(10.dp))
        val selectedCount = selectedKeys.size
        AnimatedVisibility(visible = selectedCount > 0, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.Primary) {
                    Text(
                        text = "$selectedCount selected",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("Tap to deselect", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val chunked = amenityDefs.chunked(3)
        chunked.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { def ->
                    AmenityChip(
                        label    = def.label,
                        icon     = def.icon,
                        checked  = def.key in selectedKeys,
                        onToggle = { on -> onToggle(def.key, on) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AmenityChip(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "amenity_scale")
    val bgColor by animateColorAsState(if (checked) RentOutColors.Primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant, label = "amenity_bg")
    val borderColor by animateColorAsState(if (checked) RentOutColors.Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), label = "amenity_border")
    val iconTint by animateColorAsState(if (checked) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "amenity_icon")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onToggle(!checked) }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                if (checked) {
                    Icon(Icons.Default.Check, null, tint = RentOutColors.Primary,
                        modifier = Modifier.size(10.dp).align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
                }
            }
            Text(
                text = label, fontSize = 10.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                color = if (checked) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }
}

// --- FormCompletenessIndicator -----------------------------------------------
@Composable
private fun FormCompletenessIndicator(
    hasTitle: Boolean, hasPrice: Boolean, hasRooms: Boolean,
    hasDescription: Boolean, hasAddress: Boolean, hasContact: Boolean
) {
    val items = listOf(
        "Title" to hasTitle, "Price" to hasPrice, "Bedrooms" to hasRooms,
        "Description" to hasDescription, "Address" to hasAddress, "Contact" to hasContact
    )
    val completedCount = items.count { it.second }
    val total = items.size
    val animatedProgress by animateFloatAsState(completedCount / total.toFloat(), tween(600, easing = FastOutSlowInEasing), label = "form_progress")
    val isComplete = completedCount == total
    val barColor by animateColorAsState(if (isComplete) RentOutColors.Primary else RentOutColors.IconTeal, label = "form_bar")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isComplete) RentOutColors.Primary.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = if (isComplete) BorderStroke(1.dp, RentOutColors.Primary.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                        tint = if (isComplete) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isComplete) "All fields complete!" else "Form completion",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isComplete) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$completedCount / $total", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (isComplete) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = barColor, trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            AnimatedVisibility(visible = !isComplete) {
                val missing = items.filter { !it.second }.map { it.first }
                Column {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Still needed: ${missing.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// -- Property Address Section --
@Composable
private fun PropertyAddressSection(
    address: PropertyAddress,
    onChange: (PropertyAddress) -> Unit
) {
    val isComplete = address.isComplete
    var expanded by remember { mutableStateOf(true) }

    // Each time the user opens the edit view we bump this key so any in-flight
    // collapse coroutine is cancelled and the countdown restarts from scratch
    // only after the landlord has filled all fields again.
    var collapseKey by remember { mutableStateOf(0) }

    LaunchedEffect(collapseKey) {
        // Only run the countdown when the fields are already complete at the
        // time this coroutine starts (i.e. editing is done AND address is full).
        if (!isComplete) return@LaunchedEffect
        // Give the landlord 5 seconds to review what they entered.
        kotlinx.coroutines.delay(5_000)
        // Re-check: if the address is still complete, collapse.
        if (address.isComplete) expanded = false
    }

    // Whenever completeness flips to true (field just filled), trigger a new countdown.
    LaunchedEffect(isComplete) {
        if (isComplete) collapseKey++ // cancels old coroutine, starts a fresh 5-second wait
    }

    AnimatedVisibility(
        visible = isComplete && !expanded,
        enter = fadeIn() + expandVertically(),
        exit  = fadeOut() + shrinkVertically()
    ) {
        AddressCompletedCard(address = address, onEdit = {
            expanded = true
            collapseKey = 0  // reset so the countdown won't fire until address is completed again
        })
    }

    AnimatedVisibility(
        visible = !isComplete || expanded,
        enter = fadeIn() + expandVertically(),
        exit  = fadeOut() + shrinkVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Step 1 � House Number & Street (free text)
            AddressStepRow(stepNum = 1, isFilled = address.houseAndStreet.isNotBlank()) {
                OutlinedTextField(
                    value = address.houseAndStreet,
                    onValueChange = { onChange(address.copy(houseAndStreet = it)) },
                    label = { Text("House Number & Street") },
                    leadingIcon = {
                        Icon(Icons.Default.Home, null,
                            tint = RentOutColors.IconBlue, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Step 2 � Suburb picker (smart dropdown based on chosen town)
            AddressStepRow(stepNum = 2, isFilled = address.suburb.isNotBlank()) {
                SuburbPickerField(
                    selectedSuburb = address.suburb,
                    selectedTown = address.townOrCity,
                    selectedCountry = address.country,
                    onSuburbSelected = { onChange(address.copy(suburb = it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Step 3 � Town / City picker (smart dropdown for Zimbabwe, free text otherwise)
            AddressStepRow(stepNum = 3, isFilled = address.townOrCity.isNotBlank()) {
                TownPickerField(
                    selectedTown = address.townOrCity,
                    selectedCountry = address.country,
                    onTownSelected = { town ->
                        // Reset suburb when town changes so stale suburbs are cleared
                        onChange(address.copy(townOrCity = town, suburb = ""))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Step 4 � Country picker (SADC, default Zimbabwe)
            AddressStepRow(stepNum = 4, isFilled = address.country.isNotBlank()) {
                CountryPickerField(
                    selectedCountry = address.country,
                    onCountrySelected = { country ->
                        // When country changes, reset town & suburb if switching away from Zimbabwe
                        val newTown = if (country == "Zimbabwe") address.townOrCity else ""
                        val newSuburb = if (country == "Zimbabwe") "" else address.suburb
                        onChange(address.copy(country = country, townOrCity = newTown, suburb = newSuburb))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Suburb�Town context hint � shown when suburb is blank but town is already chosen
            AnimatedVisibility(
                visible = address.suburb.isBlank() && address.townOrCity.isNotBlank() &&
                          address.country.equals("Zimbabwe", ignoreCase = true),
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.IconTeal.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null,
                        tint = RentOutColors.IconTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tap the Suburb field to pick a suburb in ${address.townOrCity}",
                        fontSize = 12.sp,
                        color = RentOutColors.IconTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AddressProgressIndicator(address = address)
        }
    }
}

@Composable
private fun AddressStepRow(
    stepNum: Int,
    isFilled: Boolean,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isFilled) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isFilled) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    "$stepNum", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun AddressProgressIndicator(address: PropertyAddress) {
    val filledCount = listOf(
        address.houseAndStreet, address.suburb, address.townOrCity, address.country
    ).count { it.isNotBlank() }
    val progress = filledCount / 4f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "addr_progress"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Address completion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "$filledCount / 4",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (progress == 1f) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = if (progress == 1f) RentOutColors.Primary else RentOutColors.IconTeal,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun AddressCompletedCard(address: PropertyAddress, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RentOutColors.Primary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, RentOutColors.Primary.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Address Saved", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RentOutColors.Primary)
                Text(
                    text = "${address.houseAndStreet}, ${address.suburb}, ${address.townOrCity}, ${address.country}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Edit address", tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -- Contact Details Section --
@Composable
private fun ContactDetailsSection(
    contact: String,
    onContact: (String) -> Unit,
    contactErr: String,
    isAutoFilled: Boolean
) {
    Column {
        AnimatedVisibility(visible = isAutoFilled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.IconGreen.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = RentOutColors.IconGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Auto-filled from your profile. You can edit this to use a different number.",
                    fontSize = 11.sp, color = RentOutColors.IconGreen, fontWeight = FontWeight.Medium
                )
            }
        }
        RentOutTextField(
            value = contact, onValueChange = onContact,
            label = "Contact Number (hidden from tenants)",
            leadingIcon = Icons.Default.Call, leadingIconTint = RentOutColors.IconAmber,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = contactErr.isNotEmpty(), errorMessage = contactErr,
            labelFontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = RentOutColors.IconSlate, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Revealed to tenants only after they pay to unlock this listing",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Existing images gallery (edit mode only) ─────────────────────────────────
@Composable
private fun ExistingImagesGallery(imageUrls: List<String>, onManagePhotos: () -> Unit = {}) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RentOutColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = RentOutColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Property Photos",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${imageUrls.size} photo${if (imageUrls.size != 1) "s" else ""} on record",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Manage Photos button in header
            val mgmtInteraction = remember { MutableInteractionSource() }
            val mgmtPressed by mgmtInteraction.collectIsPressedAsState()
            val mgmtScale by animateFloatAsState(
                if (mgmtPressed) 0.93f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "mgmt_scale"
            )
            Box(
                modifier = Modifier
                    .scale(mgmtScale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Primary)
                    .clickable(interactionSource = mgmtInteraction, indication = null, onClick = onManagePhotos)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Edit, null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (imageUrls.isEmpty()) "Add Photos" else "Edit Photos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        if (imageUrls.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No photos yet. Tap \"Add Photos\" to upload.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        // Info banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RentOutColors.IconAmber.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = RentOutColors.IconAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Tap any photo to preview. Tap \"Edit Photos\" to add or remove.",
                fontSize = 11.sp,
                color = RentOutColors.IconAmber,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(12.dp))

        // Horizontal scrollable thumbnail row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            itemsIndexed(imageUrls) { index, url ->
                val isSelected = selectedIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "img_scale_$index"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) RentOutColors.Primary else Color.Transparent,
                    animationSpec = tween(200),
                    label = "img_border_$index"
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(width = 110.dp, height = 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(12.dp))
                        .clickable { selectedIndex = if (isSelected) null else index }
                ) {
                    coil3.compose.AsyncImage(
                        model             = url,
                        contentDescription = "Property photo ${index + 1}",
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize()
                    )
                    // Index badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    // Selected checkmark overlay — use graphicsLayer instead of
                    // AnimatedVisibility to avoid ColumnScope receiver conflict inside LazyListScope
                    val checkAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(200),
                        label = "check_alpha_$index"
                    )
                    val checkScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "check_scale_$index"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(20.dp)
                            .graphicsLayer { alpha = checkAlpha; scaleX = checkScale; scaleY = checkScale }
                            .clip(CircleShape)
                            .background(RentOutColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Full-size preview of selected image
        AnimatedVisibility(
            visible = selectedIndex != null,
            enter   = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            val url = selectedIndex?.let { imageUrls.getOrNull(it) }
            if (url != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    coil3.compose.AsyncImage(
                        model              = url,
                        contentDescription = "Photo preview",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                    // Gradient overlay at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )
                    // Photo N of M label
                    Text(
                        text = "Photo ${(selectedIndex ?: 0) + 1} of ${imageUrls.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    )
                    // Close button
                    IconButton(
                        onClick = { selectedIndex = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close preview",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddImagesButton(isLoading: Boolean, isEditMode: Boolean = false, onClick: () -> Unit) {
    val buttonLabel = if (isEditMode) "Save Changes" else "Add Photos & Submit"
    val buttonIcon  = if (isEditMode) Icons.Default.Save else Icons.Default.PhotoCamera
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "add_img_scale"
    )
    val elevation by animateDpAsState(if (isPressed) 2.dp else 10.dp, label = "add_img_elev")

    Button(
        onClick = onClick,
        enabled = !isLoading,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(16.dp))
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RentOutColors.Secondary,
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Icon(buttonIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(buttonLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
    }
}
