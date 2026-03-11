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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.data.model.Property
import org.example.project.presentation.PropertyDraft
import org.example.project.presentation.PropertyFormState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

// â”€â”€â”€ Data Models â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€ Classification & Property Type Catalogue â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

object PropertyClassification {

    val primaryOptions = listOf(
        "Residential", "Commercial", "Industrial", "Land", "Mixed-Use"
    )

    val secondaryTypes: Map<String, List<String>> = mapOf(
        "Residential" to listOf(
            "Bedsitter", "Full House", "Cottage", "Room",
            "Full House + Cottage", "Full House Without Cottage",
            "Apartment", "Shared-Apartment", "Garden Flat", "Cluster",
            "Townhouse", "Condo", "Duplex", "Triplex", "Fourplex",
            "Boarding/Student House"
        ),
        "Commercial" to listOf(
            "Office", "Store", "Strip Center", "Shopping Mall",
            "Restaurant", "Cafe", "Tuckshop", "Hotel", "Motel"
        ),
        "Industrial" to listOf(
            "Warehouse", "Cold Storage", "Storage Room",
            "Factory/Manufacturing Facility", "Laboratory", "Garage"
        ),
        "Land" to listOf(
            "Agriculture/Farmland", "Greenfield", "Brownfield",
            "Infill", "Sports Pitch/Field"
        ),
        "Mixed-Use" to listOf(
            "Residential + Commercial + Retail"
        )
    )

    val locationTypes = listOf(
        "Low Density", "Medium Density", "High Density",
        "Peri-Urban Residential", "Rural"
    )

    val proximityFacilities = listOf(
        "CBD", "Near Town", "Near Hospital", "Near School",
        "Near University/College", "Near Shopping Center",
        "Near Public Transport", "Near Police Station",
        "Near Main Road", "Near Park/Recreation",
        "Near Airport", "Near Industrial Area", "Near Religious Centre"
    )

    // Legacy mapping: fine-grained propType â†’ amenity catalogue key
    fun amenityKey(propType: String): String = when {
        propType == "Room" || propType == "Bedsitter" || propType == "Boarding/Student House" -> "room"
        propType == "Full House" || propType == "Full House + Cottage" ||
        propType == "Full House Without Cottage" || propType == "Cottage" ||
        propType == "Garden Flat" || propType == "Cluster" ||
        propType == "Townhouse" || propType == "Duplex" ||
        propType == "Triplex" || propType == "Fourplex" -> "house"
        propType == "Office" || propType == "Store" || propType == "Strip Center" ||
        propType == "Shopping Mall" || propType == "Restaurant" ||
        propType == "Cafe" || propType == "Tuckshop" ||
        propType == "Hotel" || propType == "Motel" -> "commercial"
        propType == "Warehouse" || propType == "Cold Storage" ||
        propType == "Storage Room" || propType == "Factory/Manufacturing Facility" ||
        propType == "Laboratory" || propType == "Garage" -> "commercial"
        else -> "apartment"
    }
}

// â”€â”€â”€ Main Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun AddPropertyScreen(
    formState: PropertyFormState,
    onSubmit: (Property) -> Unit,
    onBack: () -> Unit,
    onNavigateToImages: (Property) -> Unit = {},
    landlordPhoneNumber: String = "",
    landlordName: String = "",
    landlordCompanyName: String = "",       // brokerage: user.companyName
    landlordCompanyPhone: String = "",      // brokerage: user.companyPhone
    landlordCompanyEmail: String = "",      // brokerage: user.companyEmail
    landlordCompanyAddress: String = "",    // brokerage: user.companyStreet + companyCity
    providerSubtype: String = "landlord",   // "landlord" | "agent" | "brokerage"
    draft: PropertyDraft = PropertyDraft(),
    onSaveDraft: (PropertyDraft) -> Unit = {},
    isEditMode: Boolean = false,
    existingImageUrls: List<String> = emptyList()
) {
    // â”€â”€ Form state seeded from saved draft â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var classification       by remember { mutableStateOf(draft.classification) }
    var propType             by remember { mutableStateOf(draft.propType) }
    var locationType         by remember { mutableStateOf(draft.locationType) }
    var title                by remember { mutableStateOf(draft.title) }
    var price                by remember { mutableStateOf(draft.price) }
    var securityDeposit      by remember { mutableStateOf(draft.securityDeposit) }
    var depositNotApplicable by remember { mutableStateOf(draft.depositNotApplicable) }
    var rooms                by remember { mutableStateOf(draft.rooms) }
    var bathrooms            by remember { mutableStateOf(draft.bathrooms) }
    var bathroomType              by remember { mutableStateOf(draft.bathroomType) }
    var customBathroomDetails     by remember { mutableStateOf(draft.customBathroomDetails) }
    var hasSharedKitchen     by remember { mutableStateOf(draft.hasSharedKitchen) }
    var kitchenCount         by remember { mutableStateOf(draft.kitchenCount) }
    var roomQuantity         by remember { mutableStateOf(draft.roomQuantity) }
    var billsInclusive       by remember { mutableStateOf<Set<String>>(draft.billsInclusive) }
    var billsExclusive       by remember { mutableStateOf<Set<String>>(draft.billsExclusive) }
    var proximityFacilities  by remember { mutableStateOf<Set<String>>(draft.proximityFacilities) }
    var latitude             by remember { mutableStateOf(draft.latitude) }
    var longitude            by remember { mutableStateOf(draft.longitude) }
    var availabilityDate     by remember { mutableStateOf(draft.availabilityDate) }
    var tenantRequirements   by remember { mutableStateOf<Set<String>>(draft.tenantRequirements) }
    var description          by remember { mutableStateOf(draft.description) }
    var address              by remember {
        mutableStateOf(
            PropertyAddress(
                houseAndStreet = draft.houseAndStreet,
                townOrCity     = draft.townOrCity,
                suburb         = draft.suburb,
                country        = draft.country
            )
        )
    }
    // For agents: landlord contact is entered manually — never auto-fill from agent's own phone
    val isAgent     = providerSubtype == "agent"
    val isBrokerage = providerSubtype == "brokerage"
    var contact by remember {
        mutableStateOf(if (isAgent) draft.contact else draft.contact.ifEmpty { landlordPhoneNumber })
    }
    // Auto-fill badge shown only for plain landlords/brokerages, not for agents
    val isContactAutoFilled = !isAgent && landlordPhoneNumber.isNotBlank()

    // ── Agent / Brokerage extra contact fields ────────────────────────────────
    var agentName            by remember { mutableStateOf(draft.agentName.ifEmpty { landlordName }) }
    var agentContactNumber   by remember { mutableStateOf(draft.agentContactNumber.ifEmpty { landlordPhoneNumber }) }
    val isAgentNameAutoFilled    = landlordName.isNotBlank()
    val isAgentContactAutoFilled = landlordPhoneNumber.isNotBlank()
    // Landlord details entered manually by agent (always blank — never auto-filled)
    var landlordContactName  by remember { mutableStateOf(draft.landlordContactName) }
    // Brokerage: broker's own name auto-filled from user profile name
    var brokerName           by remember { mutableStateOf(draft.brokerName.ifEmpty { landlordName }) }
    var brokerContactNumber  by remember { mutableStateOf(draft.brokerContactNumber.ifEmpty { landlordPhoneNumber }) }
    // Brokerage office fields — auto-filled from company profile
    var brokerageName          by remember { mutableStateOf(draft.brokerageName.ifEmpty { landlordCompanyName }) }
    var brokerageAddress       by remember { mutableStateOf(draft.brokerageAddress.ifEmpty { landlordCompanyAddress }) }
    var brokerageContactNumber by remember { mutableStateOf(draft.brokerageContactNumber.ifEmpty { landlordCompanyPhone }) }
    var brokerageEmail         by remember { mutableStateOf(draft.brokerageEmail.ifEmpty { landlordCompanyEmail }) }
    // Auto-fill indicators for brokerage
    val isBrokerNameAutoFilled    = isBrokerage && landlordName.isNotBlank()
    val isBrokerContactAutoFilled = isBrokerage && landlordPhoneNumber.isNotBlank()
    val isBrokerageOfficeAutoFilled = isBrokerage && (landlordCompanyPhone.isNotBlank() || landlordCompanyAddress.isNotBlank() || landlordCompanyEmail.isNotBlank())
    var agentNameErr            by remember { mutableStateOf("") }
    var agentContactErr         by remember { mutableStateOf("") }
    var landlordContactNameErr  by remember { mutableStateOf("") }
    var brokerNameErr           by remember { mutableStateOf("") }
    var brokerContactErr        by remember { mutableStateOf("") }
    var brokerageAddressErr     by remember { mutableStateOf("") }
    var brokerageContactErr     by remember { mutableStateOf("") }
    var brokerageEmailErr       by remember { mutableStateOf("") }

    // â”€â”€ Amenity selection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var selectedAmenityKeys by remember { mutableStateOf<Set<String>>(draft.amenityKeys) }
    var lastPropType        by remember { mutableStateOf(draft.propType) }
    LaunchedEffect(propType) {
        if (propType != lastPropType) {
            selectedAmenityKeys = emptySet()
            lastPropType = propType
        }
    }
    val amenityKey   = remember(propType) { PropertyClassification.amenityKey(propType) }
    val amenityDefs  = remember(amenityKey) { PropertyAmenities.forType(amenityKey) }

    // When classification changes, reset propType to first in new list
    LaunchedEffect(classification) {
        val types = PropertyClassification.secondaryTypes[classification] ?: emptyList()
        if (propType !in types) {
            propType = types.firstOrNull() ?: ""
        }
    }

    // â”€â”€ Validation errors â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var titleErr   by remember { mutableStateOf("") }
    var priceErr   by remember { mutableStateOf("") }
    var roomsErr   by remember { mutableStateOf("") }
    var contactErr by remember { mutableStateOf("") }
    var descErr    by remember { mutableStateOf("") }
    var addressErr by remember { mutableStateOf("") }

    // Derived: suburb suffix for title preview
    val titleWithSuburb = remember(title, address.suburb) {
        val base = title.trim()
        val sub  = address.suburb.trim()
        if (base.isNotEmpty() && sub.isNotEmpty()) "$base in $sub"
        else if (base.isNotEmpty()) base
        else ""
    }

    val isResidential = classification == "Residential"
    val isRoomType    = propType == "Room"
    val isLoading     = formState is PropertyFormState.Uploading
    val scrollState   = rememberScrollState()

    // â”€â”€ Back button animation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    var backPressed  by remember { mutableStateOf(false) }
    val backScale    by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f, tween(200), label = "br")

    if (formState is PropertyFormState.Success) {
        LaunchedEffect(formState) { onBack() }
    }

    // â”€â”€ Shared build & validate helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    fun buildAndValidate(onValid: (Property) -> Unit) {
        var valid = true
        if (title.isBlank())                                   { titleErr   = "Property title is required"; valid = false }
        if (price.isBlank() || price.toDoubleOrNull() == null) { priceErr   = "Enter a valid price";        valid = false }
        if (isResidential) {
            when {
                rooms.isBlank() -> { roomsErr = "Enter number of bedrooms"; valid = false }
                rooms.toIntOrNull() == null || (rooms.toIntOrNull() ?: 0) < 1 -> { roomsErr = "Enter a valid bedroom count"; valid = false }
            }
        }
        if (description.isBlank())                             { descErr    = "Description is required";    valid = false }
        if (!address.isComplete)                               { addressErr = "Please complete all 4 address fields"; valid = false }

        // Role-specific contact validation
        when {
            isAgent -> {
                if (agentName.isBlank())             { agentNameErr           = "Agent full name is required";       valid = false }
                if (agentContactNumber.isBlank())    { agentContactErr        = "Agent contact number is required";  valid = false }
                if (landlordContactName.isBlank())   { landlordContactNameErr = "Landlord full name is required";    valid = false }
                if (contact.isBlank())               { contactErr             = "Landlord contact number is required"; valid = false }
            }
            isBrokerage -> {
                if (brokerName.isBlank())             { brokerNameErr      = "Broker full name is required";      valid = false }
                if (brokerContactNumber.isBlank())    { brokerContactErr   = "Broker contact number is required"; valid = false }
                if (brokerageAddress.isBlank())       { brokerageAddressErr = "Brokerage address is required";    valid = false }
                if (brokerageContactNumber.isBlank()) { brokerageContactErr = "Brokerage contact is required";   valid = false }
                if (brokerageEmail.isNotBlank() && !brokerageEmail.contains("@")) {
                    brokerageEmailErr = "Enter a valid email address"
                    valid = false
                }
            }
            else -> {
                if (contact.isBlank())            { contactErr = "Contact number is required"; valid = false }
            }
        }

        if (valid) {
            val fullLocation = buildString {
                if (address.houseAndStreet.isNotBlank()) append(address.houseAndStreet)
                if (address.suburb.isNotBlank())         append(", ${address.suburb}")
                if (address.townOrCity.isNotBlank())     append(", ${address.townOrCity}")
                if (address.country.isNotBlank())        append(", ${address.country}")
            }
            onValid(
                Property(
                    title                  = titleWithSuburb.ifEmpty { title.trim() },
                    city                   = address.townOrCity.trim(),
                    location               = fullLocation,
                    price                  = price.toDoubleOrNull() ?: 0.0,
                    securityDeposit        = securityDeposit.toDoubleOrNull() ?: 0.0,
                    depositNotApplicable   = depositNotApplicable,
                    rooms                  = rooms.toIntOrNull() ?: 0,
                    customBedroomDetails   = "",
                    bathrooms              = bathrooms.toIntOrNull() ?: 0,
                    bathroomType           = bathroomType,
                    customBathroomDetails  = customBathroomDetails.trim(),
                    hasSharedKitchen       = hasSharedKitchen,
                    kitchenCount           = kitchenCount.toIntOrNull() ?: 0,
                    description            = description.trim(),
                    contactNumber          = contact.trim(),
                    // Agent fields
                    agentName              = if (isAgent) agentName.trim() else "",
                    agentContactNumber     = if (isAgent) agentContactNumber.trim() else "",
                    landlordName           = if (isAgent) landlordContactName.trim() else "",
                    // Brokerage fields
                    brokerName             = if (isBrokerage) brokerName.trim() else "",
                    brokerContactNumber    = if (isBrokerage) brokerContactNumber.trim() else "",
                    brokerageName          = if (isBrokerage) brokerageName.trim() else "",
                    brokerageAddress       = if (isBrokerage) brokerageAddress.trim() else "",
                    brokerageContactNumber = if (isBrokerage) brokerageContactNumber.trim() else "",
                    brokerageEmail         = if (isBrokerage) brokerageEmail.trim() else "",
                    classification         = classification,
                    propertyType           = propType,
                    locationType           = locationType,
                    billsInclusive         = billsInclusive.toList(),
                    billsExclusive         = billsExclusive.toList(),
                    roomQuantity           = roomQuantity,
                    proximityFacilities    = proximityFacilities.toList(),
                    latitude               = latitude.toDoubleOrNull() ?: 0.0,
                    longitude              = longitude.toDoubleOrNull() ?: 0.0,
                    availabilityDate       = availabilityDate,
                    tenantRequirements     = tenantRequirements.toList(),
                    amenities              = selectedAmenityKeys.toList(),
                    status                 = "pending"
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background)) {

        // Header gradient
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

            // Top bar
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

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // â”€â”€ Section: Listing Information â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(
                        icon = Icons.Default.Assignment,
                        text = "Listing Information",
                        centered = true
                    )
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ 1. Property Classification â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.Category, "Property Classification")
                    Spacer(Modifier.height(12.dp))
                    ClassificationDropdown(
                        selected = classification,
                        onSelect = { classification = it }
                    )
                    Spacer(Modifier.height(16.dp))

                    // â”€â”€ 2. Secondary Property Type (dynamic) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AnimatedVisibility(
                        visible = classification.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            AddPropertySectionLabel(Icons.Default.HomeWork, "Property Type")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Select the specific type within ${classification}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(10.dp))
                            PropertyTypeGrid(
                                types    = PropertyClassification.secondaryTypes[classification] ?: emptyList(),
                                selected = propType,
                                onSelect = { propType = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // â”€â”€ 3. Property Location Type â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.LocationCity, "Location Type")
                    Spacer(Modifier.height(12.dp))
                    LocationTypeDropdown(
                        selected = locationType,
                        onSelect = { locationType = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ 4. Property Title â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.Title, "Property Title")
                    Spacer(Modifier.height(12.dp))
                    RentOutTextField(
                        value         = title,
                        onValueChange = { title = it; titleErr = "" },
                        label         = "e.g., Beautiful Modern Apartment, Cozy Bedsitter, Spacious Family Home",
                        leadingIcon   = Icons.Default.Apartment,
                        leadingIconTint = RentOutColors.IconBlue,
                        isError       = titleErr.isNotEmpty(),
                        errorMessage  = titleErr,
                        labelFontSize = 11.sp,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    // Title preview
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
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ 5. Map / Coordinates Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.Map, "Property Location & Coordinates")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pin your property on the map or enter coordinates manually. Auto-detects your current GPS position.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    MapPickerView(
                        latitude  = latitude,
                        longitude = longitude,
                        onLocationPicked = { lat, lng ->
                            latitude  = lat
                            longitude = lng
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // Coordinate fields — auto-filled by the map pin, also manually editable
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RentOutTextField(
                            value           = latitude,
                            onValueChange   = { latitude = it },
                            label           = "Latitude",
                            leadingIcon     = Icons.Default.MyLocation,
                            leadingIconTint = RentOutColors.IconTeal,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier        = Modifier.weight(1f),
                            labelFontSize   = 11.sp
                        )
                        RentOutTextField(
                            value           = longitude,
                            onValueChange   = { longitude = it },
                            label           = "Longitude",
                            leadingIcon     = Icons.Default.MyLocation,
                            leadingIconTint = RentOutColors.IconBlue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier        = Modifier.weight(1f),
                            labelFontSize   = 11.sp
                        )
                    }
                    // Helper note beneath coordinate fields
                    Text(
                        text  = "Auto-filled when you pin a location on the map above. You can also enter coordinates manually.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ 6. Proximity Facilities â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.Place, "Proximity Facilities")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select all nearby facilities to help tenants find this property",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    ProximityFacilitiesSection(
                        selected = proximityFacilities,
                        onToggle = { facility, on ->
                            proximityFacilities = if (on) proximityFacilities + facility
                                                  else proximityFacilities - facility
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                    // â”€â”€ 7. Residential-only fields â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AnimatedVisibility(
                        visible = isResidential,
                        enter = fadeIn() + expandVertically(animationSpec = tween(300)),
                        exit  = fadeOut() + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Column {
                            AddPropertySectionLabel(Icons.Default.House, "Property Details")
                            Spacer(Modifier.height(12.dp))

                            // Kitchen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RentOutTextField(
                                    value         = kitchenCount,
                                    onValueChange = { kitchenCount = it },
                                    label         = "Kitchens",
                                    leadingIcon   = Icons.Default.Kitchen,
                                    leadingIconTint = RentOutColors.IconOrange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier      = Modifier.weight(1f),
                                    labelFontSize = 12.sp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Shared Kitchen",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Checkbox(
                                        checked = hasSharedKitchen,
                                        onCheckedChange = { hasSharedKitchen = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = RentOutColors.Primary
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            // Bedrooms & Bathrooms row
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RentOutTextField(
                                    value           = rooms,
                                    onValueChange   = {
                                        // Only allow numeric input
                                        if (it.all { c -> c.isDigit() }) {
                                            rooms = it
                                            roomsErr = ""
                                        }
                                    },
                                    label           = "Bedrooms",
                                    leadingIcon     = Icons.Default.Bed,
                                    leadingIconTint = RentOutColors.IconBlue,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError         = roomsErr.isNotEmpty(),
                                    errorMessage    = roomsErr,
                                    modifier        = Modifier.weight(1f),
                                    labelFontSize   = 12.sp
                                )
                                RentOutTextField(
                                    value           = bathrooms,
                                    onValueChange   = { bathrooms = it },
                                    label           = "Bathrooms",
                                    leadingIcon     = Icons.Default.Bathtub,
                                    leadingIconTint = RentOutColors.IconTeal,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier        = Modifier.weight(1f),
                                    labelFontSize   = 12.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))

                            // Bathroom Type dropdown
                            Text(
                                text = "Bathroom Type",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            BathroomTypeDropdown(
                                selected = bathroomType,
                                onSelect = {
                                    bathroomType = it
                                    if (it != "Other") customBathroomDetails = ""
                                },
                                customDetails = customBathroomDetails,
                                onCustomDetailsChange = { customBathroomDetails = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // â”€â”€ 8. Room quantity (only when "Room" type selected) â”€â”€â”€â”€
                    AnimatedVisibility(
                        visible = isRoomType,
                        enter = fadeIn() + expandVertically(animationSpec = tween(300)),
                        exit  = fadeOut() + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Column {
                            AddPropertySectionLabel(Icons.Default.MeetingRoom, "Room Quantity")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "How many rooms are available?",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(10.dp))
                            RoomQuantitySelector(
                                selected = roomQuantity,
                                onSelect = { roomQuantity = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // â”€â”€ 9. Rental & Deposit â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    EnhancedPricingSection(
                        price                = price,
                        onPriceChange        = { price = it; priceErr = "" },
                        priceError           = priceErr,
                        securityDeposit      = securityDeposit,
                        onDepositChange      = { securityDeposit = it },
                        depositNotApplicable = depositNotApplicable,
                        onDepositNAChange    = { depositNotApplicable = it },
                        billsInclusive       = billsInclusive,
                        onBillsInclusiveToggle = { bill, on ->
                            billsInclusive = if (on) billsInclusive + bill else billsInclusive - bill
                            // Remove from exclusive if now inclusive
                            if (on) billsExclusive = billsExclusive - bill
                        },
                        billsExclusive       = billsExclusive,
                        onBillsExclusiveToggle = { bill, on ->
                            billsExclusive = if (on) billsExclusive + bill else billsExclusive - bill
                            // Remove from inclusive if now exclusive
                            if (on) billsInclusive = billsInclusive - bill
                        }
                    )
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ 10. Amenities (property-type specific) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    // ── 11. Availability Period ─────────────────────────────
                    AvailabilitySection(
                        availabilityDate = availabilityDate,
                        onDateChange     = { availabilityDate = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    // ── 11b. Tenant Requirements ─────────────────────────────
                    TenantRequirementsSection(
                        selected = tenantRequirements,
                        onToggle = { req, on ->
                            tenantRequirements = if (on) tenantRequirements + req
                                                else tenantRequirements - req
                        }
                    )
                    Spacer(Modifier.height(20.dp))

                    // ── 12. Property Description ────────────────────────────
                    AddPropertySectionLabel(Icons.Default.Description, "Property Description")
                    Spacer(Modifier.height(12.dp))
                    DescriptionFieldWithHint(
                        description = description,
                        onChange    = { description = it; descErr = "" },
                        isError     = descErr.isNotEmpty(),
                        errorMessage = descErr
                    )
                    Spacer(Modifier.height(20.dp))

                    // â”€â”€ Property Address â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    AddPropertySectionLabel(Icons.Default.Place, "Property Address")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Fill in all fields â€” section collapses after 5 seconds so you can review",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    AnimatedVisibility(
                        visible = addressErr.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = addressErr,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    PropertyAddressSection(
                        address  = address,
                        onChange = { address = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    // ── Contact Details (role-aware) ────────────────────────
                    AddPropertySectionLabel(Icons.Default.Phone, "Contact Details")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Revealed to tenants only after they pay to unlock",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))

                    when {
                        isAgent -> {
                            // Agent: agent name + agent contact + landlord contact
                            Text("Agent Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00897B))
                            Spacer(Modifier.height(8.dp))

                            // Auto-filled indicator for agent name
                            if (isAgentNameAutoFilled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00897B).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00897B), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto-filled from your profile", fontSize = 11.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            RentOutTextField(
                                value         = agentName,
                                onValueChange = { agentName = it; agentNameErr = "" },
                                label         = "Agent Full Name",
                                leadingIcon   = Icons.Default.Person,
                                leadingIconTint = Color(0xFF00897B),
                                isError       = agentNameErr.isNotEmpty(),
                                errorMessage  = agentNameErr
                            )
                            Spacer(Modifier.height(10.dp))

                            // Auto-filled indicator for agent contact
                            if (isAgentContactAutoFilled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00897B).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00897B), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto-filled from your profile", fontSize = 11.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            RentOutTextField(
                                value           = agentContactNumber,
                                onValueChange   = { agentContactNumber = it; agentContactErr = "" },
                                label           = "Agent Contact Number",
                                leadingIcon     = Icons.Default.Phone,
                                leadingIconTint = Color(0xFF00897B),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError         = agentContactErr.isNotEmpty(),
                                errorMessage    = agentContactErr
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Landlord Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Enter the landlord's details manually.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(8.dp))
                            RentOutTextField(
                                value           = landlordContactName,
                                onValueChange   = { landlordContactName = it; landlordContactNameErr = "" },
                                label           = "Landlord Full Name",
                                leadingIcon     = Icons.Default.Person,
                                leadingIconTint = RentOutColors.IconGreen,
                                isError         = landlordContactNameErr.isNotEmpty(),
                                errorMessage    = landlordContactNameErr
                            )
                            Spacer(Modifier.height(10.dp))
                            RentOutTextField(
                                value           = contact,
                                onValueChange   = { contact = it; contactErr = "" },
                                label           = "Landlord Contact Number",
                                leadingIcon     = Icons.Default.Phone,
                                leadingIconTint = RentOutColors.IconGreen,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError         = contactErr.isNotEmpty(),
                                errorMessage    = contactErr
                            )
                        }
                        isBrokerage -> {
                            // ── Broker Details ────────────────────────────────
                            Text("Broker Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7C5CBF))
                            Spacer(Modifier.height(8.dp))

                            // Auto-fill badge for broker name
                            if (isBrokerNameAutoFilled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C5CBF).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7C5CBF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto-filled from your profile", fontSize = 11.sp, color = Color(0xFF7C5CBF), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            RentOutTextField(
                                value         = brokerName,
                                onValueChange = { brokerName = it; brokerNameErr = "" },
                                label         = "Broker Full Name",
                                leadingIcon   = Icons.Default.Person,
                                leadingIconTint = Color(0xFF7C5CBF),
                                isError       = brokerNameErr.isNotEmpty(),
                                errorMessage  = brokerNameErr
                            )
                            Spacer(Modifier.height(10.dp))

                            // Auto-fill badge for broker contact
                            if (isBrokerContactAutoFilled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C5CBF).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7C5CBF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto-filled from your profile", fontSize = 11.sp, color = Color(0xFF7C5CBF), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            RentOutTextField(
                                value           = brokerContactNumber,
                                onValueChange   = { brokerContactNumber = it; brokerContactErr = "" },
                                label           = "Broker Contact Number",
                                leadingIcon     = Icons.Default.Phone,
                                leadingIconTint = Color(0xFF7C5CBF),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError         = brokerContactErr.isNotEmpty(),
                                errorMessage    = brokerContactErr
                            )
                            Spacer(Modifier.height(16.dp))

                            // ── Brokerage Office Details ──────────────────────
                            Text("Brokerage Office Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7C5CBF))
                            Spacer(Modifier.height(8.dp))

                            // Auto-fill badge for office fields
                            if (isBrokerageOfficeAutoFilled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C5CBF).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7C5CBF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto-filled from your company profile", fontSize = 11.sp, color = Color(0xFF7C5CBF), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            // Brokerage full name (read-only display, sourced from companyName)
                            RentOutTextField(
                                value           = brokerageName,
                                onValueChange   = { brokerageName = it },
                                label           = "Brokerage Full Name",
                                leadingIcon     = Icons.Default.Business,
                                leadingIconTint = Color(0xFF7C5CBF)
                            )
                            Spacer(Modifier.height(10.dp))
                            RentOutTextField(
                                value         = brokerageAddress,
                                onValueChange = { brokerageAddress = it; brokerageAddressErr = "" },
                                label         = "Brokerage Office Address",
                                leadingIcon   = Icons.Default.Place,
                                leadingIconTint = Color(0xFF7C5CBF),
                                isError       = brokerageAddressErr.isNotEmpty(),
                                errorMessage  = brokerageAddressErr
                            )
                            Spacer(Modifier.height(10.dp))
                            RentOutTextField(
                                value           = brokerageContactNumber,
                                onValueChange   = { brokerageContactNumber = it; brokerageContactErr = "" },
                                label           = "Brokerage Contact Number",
                                leadingIcon     = Icons.Default.Phone,
                                leadingIconTint = Color(0xFF7C5CBF),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError         = brokerageContactErr.isNotEmpty(),
                                errorMessage    = brokerageContactErr
                            )
                            Spacer(Modifier.height(10.dp))
                            RentOutTextField(
                                value           = brokerageEmail,
                                onValueChange   = { brokerageEmail = it; brokerageEmailErr = "" },
                                label           = "Brokerage Email Address",
                                leadingIcon     = Icons.Default.Email,
                                leadingIconTint = Color(0xFF7C5CBF),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                isError         = brokerageEmailErr.isNotEmpty(),
                                errorMessage    = brokerageEmailErr
                            )
                        }
                        else -> {
                            // Standard landlord contact
                            ContactDetailsSection(
                                contact      = contact,
                                onContact    = { contact = it; contactErr = "" },
                                contactErr   = contactErr,
                                isAutoFilled = isContactAutoFilled
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // ── Form completeness indicator ──────────────────────────────────────────
                    FormCompletenessIndicator(
                        hasTitle           = title.isNotBlank(),
                        hasClassification  = classification.isNotBlank() && propType.isNotBlank(),
                        hasLocationType    = locationType.isNotBlank(),
                        hasPrice           = price.isNotBlank() && price.toDoubleOrNull() != null,
                        hasRooms           = !isResidential || rooms.isNotBlank(),
                        hasBathroomType    = !isResidential || bathroomType.isNotBlank(),
                        hasDescription     = description.isNotBlank(),
                        hasAddress         = address.isComplete,
                        hasAvailability    = availabilityDate.isEmpty() || (availabilityDate.isNotBlank() && availabilityDate != "Select Date"),
                        hasTenantReqs      = tenantRequirements.isNotEmpty(),
                        hasProximity       = proximityFacilities.isNotEmpty(),
                        // Role-aware contact completeness
                        providerSubtype    = providerSubtype,
                        hasContact         = when {
                            isAgent     -> contact.isNotBlank()
                            isBrokerage -> brokerageContactNumber.isNotBlank()
                            else        -> contact.isNotBlank()
                        },
                        hasAgentName       = agentName.isNotBlank(),
                        hasAgentContact    = agentContactNumber.isNotBlank(),
                        hasLandlordName    = landlordContactName.isNotBlank(),
                        hasBrokerName      = brokerName.isNotBlank(),
                        hasBrokerContact   = brokerContactNumber.isNotBlank(),
                        hasBrokerageAddr   = brokerageAddress.isNotBlank(),
                        hasBrokerageEmail  = brokerageEmail.isNotBlank()
                    )

                    // â”€â”€ Edit mode: image gallery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    if (isEditMode) {
                        Spacer(Modifier.height(20.dp))
                        ExistingImagesGallery(
                            imageUrls = existingImageUrls,
                            onManagePhotos = {
                                buildAndValidate { builtProperty ->
                                    onNavigateToImages(builtProperty)
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // â”€â”€ Submit / Save & continue button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                                            title                  = title,
                                            price                  = price,
                                            securityDeposit        = securityDeposit,
                                            depositNotApplicable   = depositNotApplicable,
                                            rooms                  = rooms,
                                            bathrooms              = bathrooms,
                                            bathroomType           = bathroomType,
                                            customBathroomDetails  = customBathroomDetails,
                                            hasSharedKitchen       = hasSharedKitchen,
                                            kitchenCount           = kitchenCount,
                                            description            = description,
                                            classification         = classification,
                                            propType               = propType,
                                            locationType           = locationType,
                                            billsInclusive         = billsInclusive,
                                            billsExclusive         = billsExclusive,
                                            roomQuantity           = roomQuantity,
                                            proximityFacilities    = proximityFacilities,
                                            latitude               = latitude,
                                            longitude              = longitude,
                                            availabilityDate       = availabilityDate,
                                            tenantRequirements     = tenantRequirements,
                                            houseAndStreet         = address.houseAndStreet,
                                            townOrCity             = address.townOrCity,
                                            suburb                 = address.suburb,
                                            country                = address.country,
                                            contact                = contact,
                                            amenityKeys            = selectedAmenityKeys,
                                            agentName              = agentName,
                                            agentContactNumber     = agentContactNumber,
                                            landlordContactName    = landlordContactName,
                                            brokerName             = brokerName,
                                            brokerContactNumber    = brokerContactNumber,
                                            brokerageName          = brokerageName,
                                            brokerageAddress       = brokerageAddress,
                                            brokerageContactNumber = brokerageContactNumber,
                                            brokerageEmail         = brokerageEmail
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
                } // end Column (card)
            } // end Card
            Spacer(Modifier.height(32.dp))
        } // end Column (scroll)
    } // end Box
} // end AddPropertyScreen

// â”€â”€â”€ Helper Composables â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AddPropertySectionLabel(
    icon: ImageVector,
    text: String,
    centered: Boolean = false
) {
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

// ─── Classification Dropdown (centered) ──────────────────────────────────────

@Composable
private fun ClassificationDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = PropertyClassification.primaryOptions

    val iconMap = mapOf(
        "Residential" to Icons.Default.Home,
        "Commercial"  to Icons.Default.Store,
        "Industrial"  to Icons.Default.Factory,
        "Land"        to Icons.Default.Landscape,
        "Mixed-Use"   to Icons.Default.Layers
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            OutlinedTextField(
                value = selected.ifEmpty { "Select Classification" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Primary Classification", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = iconMap[selected] ?: Icons.Default.Category,
                        contentDescription = null,
                        tint = RentOutColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = iconMap[option] ?: Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (option == selected) RentOutColors.Primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = option,
                                    fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == selected) RentOutColors.Primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = { onSelect(option); expanded = false },
                        trailingIcon = if (option == selected) ({
                            Icon(Icons.Default.Check, null, tint = RentOutColors.Primary, modifier = Modifier.size(16.dp))
                        }) else null
                    )
                }
            }
        }
    }
}
// â”€â”€â”€ Property Type Grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PropertyTypeGrid(
    types: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val chunked = types.chunked(2)
    chunked.forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEach { type ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "ptg_scale"
                )
                val isSelected = type == selected
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "ptg_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "ptg_text"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) RentOutColors.Primary else MaterialTheme.colorScheme.outline,
                    label = "ptg_border"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable(interactionSource = interactionSource, indication = null) { onSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

// ─── Location Type — modern radio-button panel ────────────────────────────────

@Composable
private fun LocationTypeDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        Triple("Low Density", Icons.Default.Villa, "Quiet suburbs with spacious surroundings"),
        Triple("Medium Density", Icons.Default.Apartment, "Balanced residential neighborhoods"),
        Triple("High Density", Icons.Default.Business, "Compact urban living close to amenities"),
        Triple("Peri-Urban Residential", Icons.Default.NaturePeople, "On the edge of town with more open space"),
        Triple("Rural", Icons.Default.Park, "Countryside and farm-area properties")
    )

    val selectedOption = options.firstOrNull { it.first == selected }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clickable { showDialog = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (selected.isBlank()) MaterialTheme.colorScheme.surface
                else RentOutColors.Primary.copy(alpha = 0.06f)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (selected.isBlank()) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                else RentOutColors.Primary.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RentOutColors.Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedOption?.second ?: Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = RentOutColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Choose location type",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = selectedOption?.first ?: "Tap to select from available location types",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedOption == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open location type picker",
                    tint = RentOutColors.Primary
                )
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Location Type", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pick the setting that best matches your property",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEach { (option, icon, subtitle) ->
                            val isSelected = selected == option
                            val containerColor by animateColorAsState(
                                if (isSelected) RentOutColors.Primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                label = "location_dialog_bg"
                            )
                            val borderColor by animateColorAsState(
                                if (isSelected) RentOutColors.Primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                label = "location_dialog_border"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                                    .background(containerColor)
                                    .clickable {
                                        onSelect(option)
                                        showDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelect(option)
                                        showDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = RentOutColors.Primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(icon, null, tint = RentOutColors.Primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// â”€â”€â”€ Map Placeholder Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MapPlaceholderSection(
    latitude: String,
    longitude: String,
    onLatChange: (String) -> Unit,
    onLonChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Map container placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = RentOutColors.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Interactive Map",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Google Maps integration â€” pin your property location",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            // GPS button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = RentOutColors.Primary,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Use current location",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Coordinate fields
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = latitude,
                onValueChange = onLatChange,
                label = { Text("Latitude", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.SouthAmerica,
                        contentDescription = null,
                        tint = RentOutColors.IconTeal,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = { Text("-17.8292", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = longitude,
                onValueChange = onLonChange,
                label = { Text("Longitude", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.East,
                        contentDescription = null,
                        tint = RentOutColors.IconBlue,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = { Text("31.0522", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Coordinates auto-fill when you pin the map. You can also type them manually.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
// â”€â”€â”€ Proximity Facilities Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ProximityFacilitiesSection(
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    val facilities = PropertyClassification.proximityFacilities
    val selectedCount = selected.size

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = selectedCount > 0,
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.IconTeal) {
                    Text(
                        text = "$selectedCount selected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        val chunked = facilities.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(0.9f).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                rowItems.forEach { facility ->
                    val isSelected = facility in selected
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "prox_scale"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) RentOutColors.IconTeal.copy(alpha = 0.15f)
                                      else MaterialTheme.colorScheme.surfaceVariant,
                        label = "prox_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) RentOutColors.IconTeal else MaterialTheme.colorScheme.outline,
                        label = "prox_border"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) RentOutColors.IconTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "prox_text"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onToggle(facility, !isSelected)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = RentOutColors.IconTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = facility,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─── Bathroom Type — modern radio-button panel ────────────────────────────────

@Composable
private fun BathroomTypeDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    customDetails: String = "",
    onCustomDetailsChange: (String) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

    val options = listOf(
        Triple("En-suite", Icons.Default.Bathtub, "Private bathroom attached to the bedroom"),
        Triple("Shared Bathroom", Icons.Default.People, "Shared between a few tenants only"),
        Triple("Common Bathroom", Icons.Default.MeetingRoom, "Shared facility for all occupants"),
        Triple("Other", Icons.Default.MoreHoriz, "Different arrangement — specify below")
    )

    val selectedOption = options.firstOrNull { it.first == selected }
    val subtitleMap = options.associate { it.first to it.third }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tap-to-open card — outline disappears when Other is active (field takes focus)
        val isOther = selected == "Other"
        val cardBorderColor by animateColorAsState(
            targetValue = when {
                isOther -> Color.Transparent
                selected.isNotEmpty() -> RentOutColors.Primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            },
            animationSpec = tween(300),
            label = "bathroom_card_border"
        )
        val cardContainerColor by animateColorAsState(
            targetValue = when {
                isOther -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                selected.isNotEmpty() -> RentOutColors.Primary.copy(alpha = 0.04f)
                else -> MaterialTheme.colorScheme.surface
            },
            animationSpec = tween(300),
            label = "bathroom_card_bg"
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clickable { showDialog = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = cardContainerColor),
            border = BorderStroke(
                width = if (isOther) 0.dp else if (selected.isNotEmpty()) 2.dp else 1.dp,
                color = cardBorderColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected.isNotEmpty()) RentOutColors.Primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedOption?.second ?: Icons.Default.Bathtub,
                        contentDescription = null,
                        tint = if (selected.isNotEmpty()) RentOutColors.Primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selected.isNotEmpty()) selected else "Select bathroom type",
                        fontSize = 15.sp,
                        fontWeight = if (selected.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (selected == "Other" && customDetails.isNotBlank())
                                       customDetails
                                   else subtitleMap[selected] ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Select",
                    tint = if (selected.isNotEmpty()) RentOutColors.Primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // "Other" hidden text field — slides in with animation
        AnimatedVisibility(
            visible = selected == "Other",
            enter = fadeIn(tween(250)) + expandVertically(tween(300)),
            exit  = fadeOut(tween(200)) + shrinkVertically(tween(250))
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.94f).padding(top = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RentOutColors.Primary.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        null,
                        tint = RentOutColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Describe your bathroom arrangement so tenants know what to expect.",
                        fontSize = 11.sp,
                        color = RentOutColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Pulsing border to draw user attention
                var isFocused by remember { mutableStateOf(false) }
                val infiniteTransition = rememberInfiniteTransition(label = "bathroom_pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bathroom_pulse_alpha"
                )
                val fieldBorderColor by animateColorAsState(
                    targetValue = if (isFocused) RentOutColors.Primary
                                  else RentOutColors.Primary.copy(alpha = pulseAlpha),
                    animationSpec = tween(200),
                    label = "bathroom_field_border"
                )
                OutlinedTextField(
                    value = customDetails,
                    onValueChange = onCustomDetailsChange,
                    label = {
                        Text(
                            "Describe the bathroom arrangement",
                            fontSize = 12.sp,
                            color = RentOutColors.Primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = RentOutColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g. Outdoor bathroom shared between 2 units",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isFocused) 2.dp else 1.5.dp,
                            color = fieldBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
        }

        // Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bathtub,
                            null,
                            tint = RentOutColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Bathroom Type", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Choose the type of bathroom arrangement for this property.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        options.forEach { (option, icon, subtitle) ->
                            val isSelected = option == selected
                            val bgColor by animateColorAsState(
                                if (isSelected) RentOutColors.Primary.copy(alpha = 0.10f)
                                else Color.Transparent,
                                label = "bg"
                            )
                            val borderColor by animateColorAsState(
                                if (isSelected) RentOutColors.Primary
                                else MaterialTheme.colorScheme.outline.copy(0.25f),
                                label = "border"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .clickable {
                                        onSelect(option)
                                        showDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelect(option)
                                        showDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = RentOutColors.Primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    icon,
                                    null,
                                    tint = if (isSelected) RentOutColors.Primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        option,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) RentOutColors.Primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}


@Composable
private fun RoomQuantitySelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        Triple("One Room", Icons.Default.BedroomChild, "Perfect for compact single-room listings"),
        Triple("Two Rooms", Icons.Default.Bed, "A flexible setup for singles or couples"),
        Triple("Three Rooms", Icons.Default.BedroomParent, "Comfortable for small families"),
        Triple("Four Rooms", Icons.Default.HolidayVillage, "Spacious option for growing households"),
        Triple("Five+ Rooms", Icons.Default.Domain, "Large properties with multiple private rooms")
    )

    val selectedOption = options.firstOrNull { it.first == selected }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected.isBlank()) MaterialTheme.colorScheme.surface
            else RentOutColors.Primary.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (selected.isBlank()) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            else RentOutColors.Primary.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RentOutColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedOption?.second ?: Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = RentOutColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Choose room quantity",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = selectedOption?.first ?: "Tap to choose how many rooms are available",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedOption == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Open room quantity picker",
                tint = RentOutColors.Primary
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Room Quantity", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Choose the option that best matches the available rooms",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { (option, icon, subtitle) ->
                        val isSelected = selected == option
                        val containerColor by animateColorAsState(
                            if (isSelected) RentOutColors.Primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            label = "room_dialog_bg"
                        )
                        val borderColor by animateColorAsState(
                            if (isSelected) RentOutColors.Primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            label = "room_dialog_border"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                                .background(containerColor)
                                .clickable {
                                    onSelect(option)
                                    showDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(option)
                                    showDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = RentOutColors.Primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(icon, null, tint = RentOutColors.Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// â”€â”€â”€ Enhanced Pricing Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EnhancedPricingSection(
    price: String,
    onPriceChange: (String) -> Unit,
    priceError: String,
    securityDeposit: String,
    onDepositChange: (String) -> Unit,
    depositNotApplicable: Boolean,
    onDepositNAChange: (Boolean) -> Unit,
    billsInclusive: Set<String>,
    onBillsInclusiveToggle: (String, Boolean) -> Unit,
    billsExclusive: Set<String>,
    onBillsExclusiveToggle: (String, Boolean) -> Unit
) {
    val billItems = listOf("WiFi", "Water", "Electricity")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachMoney, null, tint = RentOutColors.IconGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pricing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        // Monthly rent
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

        // Bills inclusive/exclusive
        BillsSection(
            title = "Inclusive of (bills included in rent):",
            billItems = billItems,
            selected = billsInclusive,
            accentColor = RentOutColors.Tertiary,
            onToggle = onBillsInclusiveToggle
        )
        Spacer(Modifier.height(10.dp))
        BillsSection(
            title = "Exclusive of (bills paid separately):",
            billItems = billItems,
            selected = billsExclusive,
            accentColor = RentOutColors.IconAmber,
            onToggle = onBillsExclusiveToggle
        )

        Spacer(Modifier.height(16.dp))

        // Security Deposit
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
                Icon(Icons.Default.AccountBalance, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Security Deposit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Refundable amount collected upfront", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Not applicable checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onDepositNAChange(!depositNotApplicable) }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = depositNotApplicable,
                onCheckedChange = onDepositNAChange,
                colors = CheckboxDefaults.colors(checkedColor = RentOutColors.IconAmber)
            )
            Text(
                text = "Not Applicable / Not Offered",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        AnimatedVisibility(
            visible = !depositNotApplicable,
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = securityDeposit,
                onValueChange = onDepositChange,
                label = { Text("Security Deposit (USD)", fontSize = 12.sp) },
                leadingIcon = {
                    Text(
                        text = "$",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RentOutColors.IconAmber,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                placeholder = { Text("e.g. 500", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Optional — leave blank if no deposit required", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                }
            )
        }
    }
}

@Composable
private fun BillsSection(
    title: String,
    billItems: List<String>,
    selected: Set<String>,
    accentColor: Color,
    onToggle: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.06f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.Center
        ) {
            billItems.forEach { bill ->
                val isSelected = bill in selected
                val interactionSource = remember { MutableInteractionSource() }
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.surface,
                    label = "bill_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    label = "bill_text"
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable(interactionSource = interactionSource, indication = null) { onToggle(bill, !isSelected) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(bill, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor)
                }
            }
        }
    }
}

// â”€â”€â”€ Availability Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AvailabilitySection(
    availabilityDate: String,
    onDateChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Availability Period", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        // Status chips: Available Now vs Available From date
        val isAvailableNow = availabilityDate.isEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AvailabilityChip(
                label = "Available Now",
                icon  = Icons.Default.CheckCircle,
                selected = isAvailableNow,
                accentColor = RentOutColors.Tertiary,
                onClick = { onDateChange("") },
                modifier = Modifier.weight(1f)
            )
            AvailabilityChip(
                label = "Available From Date",
                icon  = Icons.Default.CalendarToday,
                selected = !isAvailableNow,
                accentColor = RentOutColors.Primary,
                onClick = {
                    if (availabilityDate.isEmpty()) onDateChange("Select Date")
                },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = !isAvailableNow,
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                // ── Interactive date picker ──────────────────────────────────
                InteractiveDatePicker(
                    selectedDate = if (availabilityDate == "Select Date") "" else availabilityDate,
                    onDateSelected = onDateChange
                )
            }
        }
    }
}

@Composable
private fun AvailabilityChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "avail_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "avail_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outline,
        label = "avail_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "avail_text"
    )
    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = textColor)
    }
}

// ─── Interactive Date Picker ──────────────────────────────────────────────────

@Composable
private fun InteractiveDatePicker(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // Format "yyyy-MM-dd" → "d MMMM yyyy" for display (pure string parsing, KMP-safe)
    val displayText = remember(selectedDate) {
        if (selectedDate.isBlank()) "Tap to pick a date"
        else {
            val parts = selectedDate.split("-")
            if (parts.size == 3) {
                val months = listOf("January","February","March","April","May","June",
                    "July","August","September","October","November","December")
                val y = parts[0]; val m = parts[1].toIntOrNull() ?: 0; val d = parts[2].trimStart('0')
                if (m in 1..12) "$d ${months[m-1]} $y" else selectedDate
            } else selectedDate
        }
    }

    if (showDialog) {
        PlatformDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { onDateSelected(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedDate.isBlank())
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    RentOutColors.Primary.copy(alpha = 0.08f)
            ),
            border = BorderStroke(
                1.5.dp,
                if (selectedDate.isBlank()) MaterialTheme.colorScheme.outline.copy(0.4f)
                else RentOutColors.Primary
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        null,
                        tint = if (selectedDate.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                               else RentOutColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Available From",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            displayText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedDate.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else RentOutColors.Primary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedDate.isNotBlank()) {
                        IconButton(
                            onClick = { onDateSelected("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Clear date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        "Pick date",
                        tint = RentOutColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── Tenant Requirements Section ──────────────────────────────────────────────

@Composable
private fun TenantRequirementsSection(
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    val requirements = listOf(
        "Single Person"        to Icons.Default.Person,
        "Couple"               to Icons.Default.Favorite,
        "Family"               to Icons.Default.FamilyRestroom,
        "Student"              to Icons.Default.School,
        "Female Only"          to Icons.Default.Female,
        "Male Only"            to Icons.Default.Male,
        "Working Professional" to Icons.Default.Work,
        "Retiree / Pensioner"  to Icons.Default.ElderlyWoman,
        "No Pets"              to Icons.Default.Pets,
        "No Smoking"           to Icons.Default.SmokeFree,
        "No Children"          to Icons.Default.ChildCare,
        "Quiet Tenant"         to Icons.Default.VolumeOff
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Group, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Preferred Tenant Type", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(4.dp))
        Text(
            "Select all that apply — leave blank to accept anyone",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            requirements.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (label, icon) ->
                        val isSelected = label in selected
                        val bgColor by animateColorAsState(
                            if (isSelected) RentOutColors.Primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            label = "tr_bg_$label"
                        )
                        val borderColor by animateColorAsState(
                            if (isSelected) RentOutColors.Primary
                            else MaterialTheme.colorScheme.outline.copy(0.3f),
                            label = "tr_border_$label"
                        )
                        val textColor by animateColorAsState(
                            if (isSelected) RentOutColors.Primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tr_text_$label"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                                .background(bgColor)
                                .clickable { onToggle(label, !isSelected) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                    // If odd number in last row, fill remaining space
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// â”€â”€â”€ Description Field with Hint Banner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DescriptionFieldWithHint(
    description: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = RentOutColors.Primary, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Describe unique features, nearby amenities, and why tenants will love this property. Keep it concise and appealing.",
                    fontSize = 11.sp,
                    color = RentOutColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = description,
            onValueChange = onChange,
            label = { Text("Describe the property...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Article, null, tint = RentOutColors.IconSlate, modifier = Modifier.size(20.dp))
            },
            isError = isError,
            singleLine = false,
            maxLines = 6,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
        if (isError && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}
// â”€â”€â”€ PropertyAmenities Catalogue â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

object PropertyAmenities {
    data class AmenityDef(val key: String, val label: String, val icon: ImageVector)

    // Exact casing as specified
    private val universalAmenities = listOf(
        AmenityDef("bic",               "BIC",                    Icons.Default.Checkroom),
        AmenityDef("ceiling",           "Ceiling",                Icons.Default.CropSquare),
        AmenityDef("tiled",             "Tiled",                  Icons.Default.GridOn),
        AmenityDef("durawall",          "Durawall",               Icons.Default.Fence),
        AmenityDef("gated",             "Gated",                  Icons.Default.Lock),
        AmenityDef("borehole",          "Borehole",               Icons.Default.WaterDrop),
        AmenityDef("solar_power",       "Solar Power",            Icons.Default.WbSunny),
        AmenityDef("backup_generator",  "Backup Generator",       Icons.Default.OfflineBolt),
        AmenityDef("air_conditioning",  "Air Conditioning",       Icons.Default.AcUnit),
        AmenityDef("fitted_kitchen",    "Fitted Kitchen",         Icons.Default.Kitchen),
        AmenityDef("paved_yard",        "Paved Yard",             Icons.Default.Square),
        AmenityDef("swimming_pool",     "Swimming Pool",          Icons.Default.Pool),
        AmenityDef("garden",            "Garden",                 Icons.Default.Yard),
        AmenityDef("parking",           "Parking",                Icons.Default.LocalParking),
        AmenityDef("security_guard",    "Security Guard",         Icons.Default.Security),
        AmenityDef("cctv",              "CCTV",                   Icons.Default.Videocam),
        AmenityDef("intercom",          "Intercom",               Icons.Default.Doorbell),
        AmenityDef("electric_fence",    "Electric Fence",         Icons.Default.Fence)
    )

    val roomAmenities = listOf(
        AmenityDef("own_entrance",      "Own Entrance",           Icons.Default.DoorFront),
        AmenityDef("own_bathroom",      "Own Bathroom",           Icons.Default.Bathtub),
        AmenityDef("en_suite",          "En-Suite",               Icons.Default.Shower),
        AmenityDef("kitchenette",       "Kitchenette",            Icons.Default.Kitchen),
        AmenityDef("shared_kitchen",    "Shared Kitchen",         Icons.Default.OutdoorGrill),
        AmenityDef("wifi",              "WiFi",                   Icons.Default.Wifi),
        AmenityDef("furnished",         "Furnished",              Icons.Default.Chair),
        AmenityDef("semi_furnished",    "Semi-Furnished",         Icons.Default.TableBar),
        AmenityDef("electricity",       "Electricity Incl.",      Icons.Default.ElectricBolt),
        AmenityDef("water",             "Water Incl.",            Icons.Default.WaterDrop),
        AmenityDef("laundry_access",    "Laundry Access",         Icons.Default.LocalLaundryService),
        AmenityDef("pets_allowed",      "Pets Allowed",           Icons.Default.Pets),
        AmenityDef("air_conditioning",  "Air Conditioning",       Icons.Default.AcUnit),
        AmenityDef("heater",            "Heater",                 Icons.Default.Thermostat),
        AmenityDef("cctv",              "CCTV",                   Icons.Default.Videocam),
        AmenityDef("backup_power",      "Backup Power",           Icons.Default.BatteryChargingFull),
        AmenityDef("storage",           "Storage Room",           Icons.Default.Inventory)
    ) + universalAmenities.filter { it.key !in setOf("air_conditioning", "cctv") }

    val apartmentAmenities = listOf(
        AmenityDef("wifi",              "WiFi",                   Icons.Default.Wifi),
        AmenityDef("security",          "24/7 Security",          Icons.Default.Security),
        AmenityDef("gym",               "Gym",                    Icons.Default.FitnessCenter),
        AmenityDef("rooftop",           "Rooftop Terrace",        Icons.Default.Deck),
        AmenityDef("furnished",         "Furnished",              Icons.Default.Chair),
        AmenityDef("semi_furnished",    "Semi-Furnished",         Icons.Default.TableBar),
        AmenityDef("backup_power",      "Backup Power",           Icons.Default.BatteryChargingFull),
        AmenityDef("laundry",           "Laundry Room",           Icons.Default.LocalLaundryService),
        AmenityDef("storage",           "Storage Unit",           Icons.Default.Inventory),
        AmenityDef("lift",              "Lift / Elevator",        Icons.Default.Elevator),
        AmenityDef("concierge",         "Concierge",              Icons.Default.SupportAgent),
        AmenityDef("pets_allowed",      "Pets Allowed",           Icons.Default.Pets),
        AmenityDef("balcony",           "Balcony",                Icons.Default.Deck)
    ) + universalAmenities

    val houseAmenities = listOf(
        AmenityDef("double_garage",     "Double Garage",          Icons.Default.Garage),
        AmenityDef("servant_quarters",  "Servant Quarters",       Icons.Default.OtherHouses),
        AmenityDef("wifi",              "WiFi",                   Icons.Default.Wifi),
        AmenityDef("backup_power",      "Backup Power",           Icons.Default.BatteryChargingFull),
        AmenityDef("pets_allowed",      "Pets Allowed",           Icons.Default.Pets),
        AmenityDef("furnished",         "Furnished",              Icons.Default.Chair),
        AmenityDef("patio",             "Patio / Braai",          Icons.Default.Deck),
        AmenityDef("laundry",           "Laundry Room",           Icons.Default.LocalLaundryService),
        AmenityDef("study",             "Study / Office",         Icons.Default.MenuBook),
        AmenityDef("storage",           "Storage Room",           Icons.Default.Inventory),
        AmenityDef("alarm",             "Alarm System",           Icons.Default.NotificationImportant)
    ) + universalAmenities

    val commercialAmenities = listOf(
        AmenityDef("wifi",              "Fibre / WiFi",           Icons.Default.Wifi),
        AmenityDef("reception",         "Reception Area",         Icons.Default.MeetingRoom),
        AmenityDef("boardroom",         "Boardroom",              Icons.Default.Groups),
        AmenityDef("lift",              "Lift / Elevator",        Icons.Default.Elevator),
        AmenityDef("loading_bay",       "Loading Bay",            Icons.Default.LocalShipping),
        AmenityDef("storage",           "Storage / Warehouse",    Icons.Default.Inventory),
        AmenityDef("kitchenette",       "Kitchenette",            Icons.Default.Kitchen),
        AmenityDef("ablution",          "Ablution Facilities",    Icons.Default.Wc),
        AmenityDef("signage",           "Signage Rights",         Icons.Default.Signpost),
        AmenityDef("open_plan",         "Open Plan",              Icons.Default.ViewQuilt),
        AmenityDef("partitioned",       "Partitioned Offices",    Icons.Default.GridView),
        AmenityDef("disabled_access",   "Disabled Access",        Icons.Default.Accessible),
        AmenityDef("canteen",           "Canteen / Cafeteria",    Icons.Default.Restaurant)
    ) + universalAmenities

    fun forType(amenityKey: String): List<AmenityDef> = when (amenityKey) {
        "room"       -> roomAmenities.distinctBy { it.key }
        "house"      -> houseAmenities.distinctBy { it.key }
        "commercial" -> commercialAmenities.distinctBy { it.key }
        else         -> apartmentAmenities.distinctBy { it.key }
    }
}

// â”€â”€â”€ AmenitiesSection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AmenitiesSection(
    propType: String,
    amenityDefs: List<PropertyAmenities.AmenityDef>,
    selectedKeys: Set<String>,
    onToggle: (key: String, on: Boolean) -> Unit
) {
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
                text = "Showing amenities for: $propType. Change property type above to see different options.",
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "amenity_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) RentOutColors.Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "amenity_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) RentOutColors.Primary else MaterialTheme.colorScheme.outline,
        label = "amenity_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (checked) RentOutColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "amenity_content"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onToggle(!checked) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

// â”€â”€â”€ Form Completeness Indicator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun FormCompletenessIndicator(
    hasTitle: Boolean,
    hasClassification: Boolean,
    hasLocationType: Boolean,
    hasPrice: Boolean,
    hasRooms: Boolean,
    hasBathroomType: Boolean,
    hasDescription: Boolean,
    hasAddress: Boolean,
    hasContact: Boolean,
    hasAvailability: Boolean,
    hasTenantReqs: Boolean,
    hasProximity: Boolean,
    // Role-aware fields
    providerSubtype: String = "landlord",
    hasAgentName: Boolean = false,
    hasAgentContact: Boolean = false,
    hasLandlordName: Boolean = false,
    hasBrokerName: Boolean = false,
    hasBrokerContact: Boolean = false,
    hasBrokerageAddr: Boolean = false,
    hasBrokerageEmail: Boolean = false,
) {
    val isAgent     = providerSubtype == "agent"
    val isBrokerage = providerSubtype == "brokerage"

    val items: List<Pair<String, Boolean>> = buildList {
        add("Title"        to hasTitle)
        add("Type"         to hasClassification)
        add("Location"     to hasLocationType)
        add("Price"        to hasPrice)
        add("Bedrooms"     to hasRooms)
        add("Bathroom"     to hasBathroomType)
        add("Description"  to hasDescription)
        add("Address"      to hasAddress)
        add("Availability" to hasAvailability)
        add("Tenant Type"  to hasTenantReqs)
        add("Proximity"    to hasProximity)
        when {
            isAgent -> {
                add("Agent Name"    to hasAgentName)
                add("Agent No."     to hasAgentContact)
                add("Lndlrd Name"   to hasLandlordName)
                add("Lndlrd No."    to hasContact)
            }
            isBrokerage -> {
                add("Broker Name"   to hasBrokerName)
                add("Broker No."    to hasBrokerContact)
                add("Office Addr."  to hasBrokerageAddr)
                add("Office No."    to hasContact)
                add("Office Email"  to hasBrokerageEmail)
            }
            else -> {
                add("Contact"       to hasContact)
            }
        }
    }

    val completed = items.count { it.second }

    val total = items.size

    val progress = completed.toFloat() / total.toFloat()



    val animatedProgress by animateFloatAsState(

        targetValue = progress,

        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),

        label = "completeness_progress"

    )



    Column(

        modifier = Modifier.fillMaxWidth(),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(0.94f),

            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Text(

                text = "Form Completeness",

                fontSize = 12.sp,

                fontWeight = FontWeight.Medium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Spacer(Modifier.width(8.dp))

            Text(

                text = "$completed / $total",

                fontSize = 12.sp,

                fontWeight = FontWeight.Bold,

                color = if (completed == total) RentOutColors.Tertiary else RentOutColors.Primary

            )

        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(

            progress = animatedProgress,

            modifier = Modifier

                .fillMaxWidth(0.94f)

                .clip(RoundedCornerShape(6.dp)),

            color = if (completed == total) RentOutColors.Tertiary else RentOutColors.Primary,

            trackColor = MaterialTheme.colorScheme.surfaceVariant

        )

        Spacer(Modifier.height(10.dp))

        Column(

            modifier = Modifier.fillMaxWidth(0.94f),

            verticalArrangement = Arrangement.spacedBy(8.dp),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            items.chunked(3).forEach { rowItems ->

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    rowItems.forEach { (label, done) ->

                        FormCompletenessChip(

                            label = label,

                            done = done,

                            modifier = Modifier.weight(1f)

                        )

                    }

                    repeat(3 - rowItems.size) {

                        Spacer(modifier = Modifier.weight(1f))

                    }

                }

            }

        }

    }

}



@Composable

private fun FormCompletenessChip(

    label: String,

    done: Boolean,

    modifier: Modifier = Modifier

) {

    val containerColor by animateColorAsState(

        targetValue = if (done) RentOutColors.Tertiary.copy(alpha = 0.12f)

        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),

        label = "form_chip_bg"

    )

    val contentColor by animateColorAsState(

        targetValue = if (done) RentOutColors.Tertiary else MaterialTheme.colorScheme.onSurfaceVariant,

        label = "form_chip_content"

    )

    val borderColor by animateColorAsState(

        targetValue = if (done) RentOutColors.Tertiary.copy(alpha = 0.6f)

        else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),

        label = "form_chip_border"

    )



    Row(

        modifier = modifier

            .clip(RoundedCornerShape(12.dp))

            .border(1.dp, borderColor, RoundedCornerShape(12.dp))

            .background(containerColor)

            .padding(horizontal = 8.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.Center

    ) {

        Icon(

            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,

            contentDescription = null,

            tint = contentColor,

            modifier = Modifier.size(14.dp)

        )

        Spacer(Modifier.width(6.dp))

        Text(

            text = label,

            fontSize = 10.sp,

            fontWeight = if (done) FontWeight.SemiBold else FontWeight.Medium,

            color = contentColor,

            textAlign = TextAlign.Center,

            maxLines = 1

        )

    }

}

// â”€â”€â”€ Property Address Section (kept from existing) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PropertyAddressSection(
    address: PropertyAddress,
    onChange: (PropertyAddress) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var allFilledTimer by remember { mutableStateOf(false) }

    val isAllFilled = address.isComplete
    LaunchedEffect(isAllFilled) {
        if (isAllFilled) {
            kotlinx.coroutines.delay(5000)
            isExpanded = false
            allFilledTimer = true
        } else {
            isExpanded = true
            allFilledTimer = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isAllFilled) RentOutColors.Tertiary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAllFilled) Icons.Default.CheckCircle else Icons.Default.EditLocation,
                contentDescription = null,
                tint = if (isAllFilled) RentOutColors.Tertiary else RentOutColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAllFilled) "Address complete" else "Enter property address",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAllFilled) RentOutColors.Tertiary else MaterialTheme.colorScheme.onSurface
                )
                if (isAllFilled) {
                    Text(
                        text = "${address.houseAndStreet}, ${address.suburb}, ${address.townOrCity}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                RentOutTextField(
                    value         = address.houseAndStreet,
                    onValueChange = { onChange(address.copy(houseAndStreet = it)) },
                    label         = "House No. & Street",
                    leadingIcon   = Icons.Default.Home,
                    leadingIconTint = RentOutColors.IconBlue,
                    labelFontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                SuburbPickerField(
                    selectedSuburb  = address.suburb,
                    selectedTown    = address.townOrCity,
                    selectedCountry = address.country,
                    onSuburbSelected = { onChange(address.copy(suburb = it)) }
                )
                Spacer(Modifier.height(10.dp))
                TownPickerField(
                    selectedTown    = address.townOrCity,
                    selectedCountry = address.country,
                    onTownSelected  = { onChange(address.copy(townOrCity = it)) }
                )
                Spacer(Modifier.height(10.dp))
                CountryPickerField(
                    selectedCountry   = address.country,
                    onCountrySelected = { onChange(address.copy(country = it)) }
                )
            }
        }
    }
}

// â”€â”€â”€ Contact Details Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ContactDetailsSection(
    contact: String,
    onContact: (String) -> Unit,
    contactErr: String,
    isAutoFilled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isAutoFilled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RentOutColors.Tertiary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = RentOutColors.Tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto-filled from your profile", fontSize = 11.sp, color = RentOutColors.Tertiary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
        }
        RentOutTextField(
            value           = contact,
            onValueChange   = onContact,
            label           = "Contact Number",
            leadingIcon     = Icons.Default.Phone,
            leadingIconTint = RentOutColors.IconGreen,
            isError         = contactErr.isNotEmpty(),
            errorMessage    = contactErr,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            labelFontSize   = 12.sp
        )
    }
}

// â”€â”€â”€ Existing Images Gallery (edit mode) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ExistingImagesGallery(
    imageUrls: List<String>,
    onManagePhotos: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhotoLibrary, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Property Photos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        if (imageUrls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    Text("No photos yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                items(imageUrls.withIndex().toList()) { (index, url) ->
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.5.dp,
                                if (index == 0) RentOutColors.Primary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = "Photo ${index + 1}",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = null,
                            placeholder = null
                        )
                        // Shimmer placeholder while loading
                        if (url.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        // Cover badge on first image
                        if (index == 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RentOutColors.Primary)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("Cover", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onManagePhotos,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, RentOutColors.Primary)
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = RentOutColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Manage Property Photos", color = RentOutColors.Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// â”€â”€â”€ Add Images / Submit Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun AddImagesButton(
    isLoading: Boolean,
    isEditMode: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "submit_scale"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RentOutColors.Primary),
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.width(10.dp))
            Text("Saving...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        } else if (isEditMode) {
            Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        } else {
            Icon(Icons.Default.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save & Add Photos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}



