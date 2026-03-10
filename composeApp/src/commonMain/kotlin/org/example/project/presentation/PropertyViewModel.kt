package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.example.project.data.model.Property
import org.example.project.ui.util.PickedImage
import org.example.project.ui.util.buildStorageData

sealed class PropertyListState {
    object Loading : PropertyListState()
    data class Success(val properties: List<Property>) : PropertyListState()
    data class Error(val message: String) : PropertyListState()
    object Empty : PropertyListState()
}

// ── Sort options ──────────────────────────────────────────────────────────────
enum class SortOption(val label: String) {
    NEWEST("Newest First"),
    PRICE_LOW_HIGH("Price: Low → High"),
    PRICE_HIGH_LOW("Price: High → Low"),
    MOST_ROOMS("Most Bedrooms"),
    LEAST_ROOMS("Fewest Bedrooms")
}

// ── Filter state — all real estate relevant dimensions ────────────────────────
data class PropertyFilter(
    val minPrice: Double? = null,           // null = no lower bound
    val maxPrice: Double? = null,           // null = no upper bound
    val propertyTypes: Set<String> = emptySet(),   // fine-grained property types
    val classifications: Set<String> = emptySet(), // "Residential"|"Commercial"|etc.
    val locationTypes: Set<String> = emptySet(),   // "Low Density"|"High Density"|etc.
    val providerTypes: Set<String> = emptySet(),   // "landlord"|"agent"|"brokerage"
    val minBedrooms: Int? = null,           // null = any
    val maxBedrooms: Int? = null,
    val minBathrooms: Int? = null,          // null = any
    val availableOnly: Boolean = false,
    val verifiedOnly: Boolean = false,
    val requiredAmenities: Set<String> = emptySet(),
    val sortBy: SortOption = SortOption.NEWEST
) {
    val isActive: Boolean get() =
        minPrice != null || maxPrice != null ||
        propertyTypes.isNotEmpty() ||
        classifications.isNotEmpty() ||
        locationTypes.isNotEmpty() ||
        providerTypes.isNotEmpty() ||
        minBedrooms != null || maxBedrooms != null ||
        minBathrooms != null ||
        availableOnly || verifiedOnly ||
        requiredAmenities.isNotEmpty() ||
        sortBy != SortOption.NEWEST

    val activeCount: Int get() = listOfNotNull(
        if (minPrice != null || maxPrice != null) "price" else null,
        if (propertyTypes.isNotEmpty()) "type" else null,
        if (classifications.isNotEmpty()) "class" else null,
        if (locationTypes.isNotEmpty()) "loctype" else null,
        if (providerTypes.isNotEmpty()) "provider" else null,
        if (minBedrooms != null || maxBedrooms != null) "beds" else null,
        if (minBathrooms != null) "baths" else null,
        if (availableOnly) "available" else null,
        if (verifiedOnly) "verified" else null,
        if (requiredAmenities.isNotEmpty()) "amenities" else null,
        if (sortBy != SortOption.NEWEST) "sort" else null
    ).size
}

sealed class PropertyFormState {
    object Idle      : PropertyFormState()
    object Uploading : PropertyFormState()
    object Success   : PropertyFormState()
    data class Error(val message: String) : PropertyFormState()
}

// ---------------------------------------------------------------------------
// Draft state — survives navigation between AddPropertyScreen ↔ PropertyImagesScreen
// ---------------------------------------------------------------------------
data class PropertyDraft(
    val title:                String = "",
    val price:                String = "",
    val securityDeposit:      String = "",
    val depositNotApplicable: Boolean = false,
    val rooms:                String = "",
    val customBedroomDetails: String = "",
    val bathrooms:            String = "",
    val bathroomType:         String = "",
    val customBathroomDetails: String = "",
    val hasSharedKitchen:     Boolean = false,
    val kitchenCount:         String = "",
    val description:          String = "",
    val classification:       String = "Residential",
    val propType:             String = "Apartment",
    val locationType:         String = "",
    val billsInclusive:       Set<String> = emptySet(),
    val billsExclusive:       Set<String> = emptySet(),
    val roomQuantity:         String = "",
    val proximityFacilities:  Set<String> = emptySet(),
    val latitude:             String = "",
    val longitude:            String = "",
    val availabilityDate:     String = "",
    val tenantRequirements:   Set<String> = emptySet(),
    val houseAndStreet:       String = "",
    val townOrCity:           String = "Gweru",
    val suburb:               String = "",
    val country:              String = "Zimbabwe",
    val contact:              String = "",
    val amenityKeys:          Set<String> = emptySet(),
    // Agent-specific
    val agentName:            String = "",
    val agentContactNumber:   String = "",
    val landlordContactName:  String = "",   // landlord's full name entered by agent
    // Brokerage-specific
    val brokerName:           String = "",
    val brokerContactNumber:  String = "",
    val brokerageAddress:     String = "",
    val brokerageContactNumber: String = "",
    // Temporarily holds picked images so they survive back-navigation
    val pickedImages:         List<PickedImage> = emptyList()
)

class PropertyViewModel : ViewModel() {

    private val _landlordProperties = MutableStateFlow<PropertyListState>(PropertyListState.Loading)
    val landlordProperties: StateFlow<PropertyListState> = _landlordProperties.asStateFlow()

    private val _tenantProperties = MutableStateFlow<PropertyListState>(PropertyListState.Loading)
    val tenantProperties: StateFlow<PropertyListState> = _tenantProperties.asStateFlow()

    private val _formState = MutableStateFlow<PropertyFormState>(PropertyFormState.Idle)
    val formState: StateFlow<PropertyFormState> = _formState.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCity = MutableStateFlow("Gweru")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _propertyFilter = MutableStateFlow(PropertyFilter())
    val propertyFilter: StateFlow<PropertyFilter> = _propertyFilter.asStateFlow()

    fun setFilter(filter: PropertyFilter) { _propertyFilter.value = filter }
    fun clearFilter() { _propertyFilter.value = PropertyFilter() }

    // ── Apply all active filters + sort to a property list ────────────────────
    fun applyFilters(
        properties: List<Property>,
        query: String,
        city: String,
        filter: PropertyFilter
    ): List<Property> {
        var result = properties

        // City / town filter
        if (city.isNotBlank() && city != "All") {
            result = result.filter { p ->
                p.city.equals(city, ignoreCase = true) ||
                p.location.contains(city, ignoreCase = true)
            }
        }

        // Search query
        if (query.isNotBlank()) {
            result = result.filter { p ->
                p.title.contains(query, ignoreCase = true) ||
                p.city.contains(query, ignoreCase = true) ||
                p.location.contains(query, ignoreCase = true) ||
                p.description.contains(query, ignoreCase = true)
            }
        }

        // Price range
        filter.minPrice?.let { min -> result = result.filter { it.price >= min } }
        filter.maxPrice?.let { max -> result = result.filter { it.price <= max } }

        // Property type (fine-grained)
        if (filter.propertyTypes.isNotEmpty()) {
            result = result.filter { it.propertyType in filter.propertyTypes }
        }

        // Classification
        if (filter.classifications.isNotEmpty()) {
            result = result.filter { it.classification in filter.classifications }
        }

        // Location type
        if (filter.locationTypes.isNotEmpty()) {
            result = result.filter { it.locationType in filter.locationTypes }
        }

        // Provider type (landlord | agent | brokerage)
        if (filter.providerTypes.isNotEmpty()) {
            result = result.filter { p ->
                filter.providerTypes.any { it.equals(p.providerSubtype, ignoreCase = true) }
            }
        }

        // Bedrooms
        filter.minBedrooms?.let { min -> result = result.filter { it.rooms >= min } }
        filter.maxBedrooms?.let { max -> result = result.filter { it.rooms <= max } }

        // Bathrooms
        filter.minBathrooms?.let { min -> result = result.filter { it.bathrooms >= min } }

        // Availability
        if (filter.availableOnly) result = result.filter { it.isAvailable }

        // Verified
        if (filter.verifiedOnly) result = result.filter { it.isVerified }

        // Amenities — must have ALL required amenities
        if (filter.requiredAmenities.isNotEmpty()) {
            result = result.filter { p ->
                filter.requiredAmenities.all { amenity ->
                    p.amenities.any { it.equals(amenity, ignoreCase = true) }
                }
            }
        }

        // Sort
        result = when (filter.sortBy) {
            SortOption.NEWEST        -> result.sortedByDescending { it.createdAt }
            SortOption.PRICE_LOW_HIGH -> result.sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> result.sortedByDescending { it.price }
            SortOption.MOST_ROOMS    -> result.sortedByDescending { it.rooms }
            SortOption.LEAST_ROOMS   -> result.sortedBy { it.rooms }
        }

        return result
    }

    // Draft — persists across AddPropertyScreen ↔ PropertyImagesScreen navigation
    private val _draft = MutableStateFlow(PropertyDraft())
    val draft: StateFlow<PropertyDraft> = _draft.asStateFlow()

    // ── Real-time listener jobs — cancelled when a new listener is started ────
    private var landlordListenerJob: Job? = null
    private var tenantListenerJob: Job? = null

    fun saveDraft(draft: PropertyDraft) { _draft.value = draft }
    fun clearDraft() { _draft.value = PropertyDraft() }

    // ── Generate a Firestore-compatible random document ID ────────────────────
    private fun generateId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..20).map { chars.random() }.joinToString("")
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCity(city: String) { _selectedCity.value = city }

    fun selectProperty(property: Property) { _selectedProperty.value = property }
    fun clearSelectedProperty() { _selectedProperty.value = null }
    fun resetFormState() { _formState.value = PropertyFormState.Idle }

    fun loadPropertyById(propertyId: String, onLoaded: (Boolean) -> Unit = {}) {
        if (propertyId.isBlank()) {
            onLoaded(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                val doc = Firebase.firestore.collection("properties").document(propertyId).get()
                if (doc.exists) {
                    val property = doc.data(Property.serializer())
                    if (property.imageUrls.isEmpty() && property.imageUrl.isNotBlank()) {
                        property.copy(imageUrls = listOf(property.imageUrl))
                    } else property
                } else null
            }.getOrNull()

            if (result != null) {
                _selectedProperty.value = result
                onLoaded(true)
            } else {
                onLoaded(false)
            }
        }
    }

    // ── Stop all active real-time listeners (call on logout) ──────────────────
    fun stopAllListeners() {
        landlordListenerJob?.cancel()
        landlordListenerJob = null
        tenantListenerJob?.cancel()
        tenantListenerJob = null
    }

    // ── Convenience wrappers (called from App.kt navigation) ─────────────────
    fun loadLandlordProperties(landlordId: String) {
        loadLandlordPropertiesFromFirestore(landlordId)
    }

    fun loadTenantProperties() {
        loadTenantPropertiesFromFirestore()
    }

    // ── Submit new property with images to Firestore + Storage ────────────────
    fun submitProperty(property: Property, imageBytes: List<ByteArray> = emptyList()) {
        viewModelScope.launch {
            _formState.value = PropertyFormState.Uploading
            try {
                val auth    = Firebase.auth
                val db      = Firebase.firestore
                val storage = Firebase.storage
                val uid     = auth.currentUser?.uid ?: throw Exception("Not authenticated")

                val docRef = db.collection("properties").document(generateId())

                val imageUrls = imageBytes.mapIndexed { index, bytes ->
                    val ref = storage.reference("property_images/$uid/${docRef.id}/$index.jpg")
                    ref.putData(buildStorageData(bytes))
                    ref.getDownloadUrl()
                }

                val userDoc         = db.collection("users").document(uid).get()
                val landlordName    = userDoc.get("name") as? String ?: ""
                val providerSubtype = userDoc.get("providerSubtype") as? String ?: ""

                val finalProperty = property.copy(
                    id              = docRef.id,
                    landlordId      = uid,
                    landlordName    = landlordName,
                    providerSubtype = providerSubtype,
                    imageUrl        = imageUrls.firstOrNull() ?: "",
                    imageUrls       = imageUrls,
                    status          = "pending",
                    isVerified      = false,
                    createdAt       = System.currentTimeMillis()
                )

                docRef.set(finalProperty)
                // Real-time listener on the landlord query will reflect the new doc automatically
                _formState.value = PropertyFormState.Success
                // Clear persisted images now that submission succeeded
                _draft.value = _draft.value.copy(pickedImages = emptyList())
            } catch (e: Exception) {
                _formState.value = PropertyFormState.Error(e.message ?: "Failed to submit property")
            }
        }
    }

    // ── Load landlord's own properties — real-time Firestore listener ─────────
    // Cancels any previous listener before starting a new one, so switching
    // accounts never leaks a stale subscription.
    fun loadLandlordPropertiesFromFirestore(landlordId: String) {
        landlordListenerJob?.cancel()
        landlordListenerJob = viewModelScope.launch {
            _landlordProperties.value = PropertyListState.Loading
            Firebase.firestore
                .collection("properties")
                .where { "landlordId" equalTo landlordId }
                .snapshots
                .catch { e ->
                    _landlordProperties.value = PropertyListState.Error(
                        e.message ?: "Failed to load properties. Check your connection."
                    )
                }
                .collect { snapshot ->
                    val props = snapshot.documents.map { doc ->
                        val prop = doc.data(Property.serializer())
                        // If imageUrls is empty but imageUrl exists, populate list for consistency
                        if (prop.imageUrls.isEmpty() && prop.imageUrl.isNotBlank()) {
                            prop.copy(imageUrls = listOf(prop.imageUrl))
                        } else prop
                    }
                    _landlordProperties.value =
                        if (props.isEmpty()) PropertyListState.Empty
                        else PropertyListState.Success(props)

                    // Keep selectedProperty in sync if it is one of the updated docs
                    val selected = _selectedProperty.value
                    if (selected != null) {
                        props.find { it.id == selected.id }?.let { updated ->
                            _selectedProperty.value = updated
                        }
                    }
                }
        }
    }

    // ── Load approved tenant-visible properties — real-time Firestore listener ─
    // Pushes updates instantly whenever admin approves / rejects a listing,
    // so tenants always see the current state without relaunching the app.
    fun loadTenantPropertiesFromFirestore() {
        tenantListenerJob?.cancel()
        tenantListenerJob = viewModelScope.launch {
            _tenantProperties.value = PropertyListState.Loading
            Firebase.firestore
                .collection("properties")
                .where { "status" equalTo "approved" }
                .snapshots
                .catch { e ->
                    _tenantProperties.value = PropertyListState.Error(
                        e.message ?: "Failed to load listings. Check your connection."
                    )
                }
                .collect { snapshot ->
                    val props = snapshot.documents.map { doc ->
                        val prop = doc.data(Property.serializer())
                        // If imageUrls is empty but imageUrl exists, populate list for consistency
                        if (prop.imageUrls.isEmpty() && prop.imageUrl.isNotBlank()) {
                            prop.copy(imageUrls = listOf(prop.imageUrl))
                        } else prop
                    }
                    _tenantProperties.value =
                        if (props.isEmpty()) PropertyListState.Empty
                        else PropertyListState.Success(props)
                }
        }
    }

    // ── Toggle availability on Firestore ──────────────────────────────────────
    fun toggleAvailability(propertyId: String) {
        viewModelScope.launch {
            try {
                val db      = Firebase.firestore
                val uid     = Firebase.auth.currentUser?.uid ?: return@launch
                val docRef  = db.collection("properties").document(propertyId)
                val doc     = docRef.get()
                val current = doc.get("isAvailable") as? Boolean ?: true
                val newValue = !current
                docRef.update("isAvailable" to newValue)

                // Update the in-memory list immediately so the UI reflects the change
                val currentState = _landlordProperties.value
                if (currentState is PropertyListState.Success) {
                    val updatedList = currentState.properties.map { prop ->
                        if (prop.id == propertyId) prop.copy(isAvailable = newValue) else prop
                    }
                    _landlordProperties.value = PropertyListState.Success(updatedList)
                }

                // Also update selectedProperty if it is the toggled one
                if (_selectedProperty.value?.id == propertyId) {
                    _selectedProperty.value = _selectedProperty.value?.copy(isAvailable = newValue)
                }
            } catch (_: Exception) { }
        }
    }

    // ── Update an existing property on Firestore (edit flow) ─────────────────
    // keepImageUrls  = existing remote image URLs the landlord chose to keep
    // newImageBytes  = freshly picked images to upload and append
    fun updateProperty(
        property: Property,
        keepImageUrls: List<String> = emptyList(),
        newImageBytes: List<ByteArray> = emptyList()
    ) {
        viewModelScope.launch {
            _formState.value = PropertyFormState.Uploading
            try {
                println("📝 PropertyViewModel.updateProperty() called")
                println("   Property ID: ${property.id}")
                println("   Property.imageUrl: ${property.imageUrl}")
                println("   Property.imageUrls: ${property.imageUrls}")
                println("   keepImageUrls (${keepImageUrls.size}): $keepImageUrls")
                println("   newImageBytes count: ${newImageBytes.size}")
                
                val auth    = Firebase.auth
                val db      = Firebase.firestore
                val storage = Firebase.storage
                val uid     = auth.currentUser?.uid ?: throw Exception("Not authenticated")

                val docRef = db.collection("properties").document(property.id)

                // Upload any newly picked images
                println("🔄 Uploading ${newImageBytes.size} new images...")
                val uploadedUrls = newImageBytes.mapIndexed { index, bytes ->
                    val slot = keepImageUrls.size + index
                    val ref  = storage.reference("property_images/$uid/${property.id}/$slot.jpg")
                    println("   Uploading image $index to slot $slot...")
                    ref.putData(buildStorageData(bytes))
                    val url = ref.getDownloadUrl()
                    println("   ✅ Uploaded: $url")
                    url
                }

                // Final image list = kept existing + newly uploaded
                val allImageUrls = keepImageUrls + uploadedUrls
                println("📦 Final image list (${allImageUrls.size} total):")
                allImageUrls.forEachIndexed { idx, url -> println("   [$idx] $url") }

                val updatedProperty = property.copy(
                    landlordId = uid,
                    imageUrl   = allImageUrls.firstOrNull() ?: property.imageUrl,
                    imageUrls  = allImageUrls
                )
                
                println("💾 Saving to Firestore...")
                println("   imageUrl: ${updatedProperty.imageUrl}")
                println("   imageUrls (${updatedProperty.imageUrls.size}): ${updatedProperty.imageUrls}")

                docRef.set(updatedProperty)
                // Keep _selectedProperty in sync immediately for detail/edit screens.
                // The real-time landlord listener will also push the update to the dashboard.
                _selectedProperty.value = updatedProperty
                println("✅ Property updated successfully!")
                _formState.value = PropertyFormState.Success
            } catch (e: Exception) {
                println("❌ Error updating property: ${e.message}")
                e.printStackTrace()
                _formState.value = PropertyFormState.Error(e.message ?: "Failed to update property")
            }
        }
    }

    // ── Delete property from Firestore ────────────────────────────────────────
    // The real-time listener on the landlord query will automatically reflect
    // the deletion — no manual reload needed.
    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("properties").document(propertyId).delete()
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllListeners()
    }
}