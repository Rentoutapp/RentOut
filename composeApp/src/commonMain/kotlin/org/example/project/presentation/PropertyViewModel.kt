package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val title:           String = "",
    val price:           String = "",
    val securityDeposit: String = "",
    val rooms:           String = "",
    val bathrooms:       String = "",
    val description:     String = "",
    val propType:        String = "apartment",
    val houseAndStreet:  String = "",
    val townOrCity:      String = "Gweru",
    val suburb:          String = "",
    val country:         String = "Zimbabwe",
    val contact:         String = "",
    val amenityKeys:     Set<String> = emptySet(),
    // Temporarily holds picked images so they survive back-navigation
    val pickedImages:    List<PickedImage> = emptyList()
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

    // Draft — persists across AddPropertyScreen ↔ PropertyImagesScreen navigation
    private val _draft = MutableStateFlow(PropertyDraft())
    val draft: StateFlow<PropertyDraft> = _draft.asStateFlow()

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

                val userDoc      = db.collection("users").document(uid).get()
                val landlordName = userDoc.get("name") as? String ?: ""

                val finalProperty = property.copy(
                    id           = docRef.id,
                    landlordId   = uid,
                    landlordName = landlordName,
                    imageUrl     = imageUrls.firstOrNull() ?: "",
                    imageUrls    = imageUrls,
                    status       = "pending",
                    isVerified   = false,
                    createdAt    = System.currentTimeMillis()
                )

                docRef.set(finalProperty)
                // Refresh the landlord's property list so the dashboard updates immediately
                loadLandlordPropertiesFromFirestore(uid)
                _formState.value = PropertyFormState.Success
                // Clear persisted images now that submission succeeded
                _draft.value = _draft.value.copy(pickedImages = emptyList())
            } catch (e: Exception) {
                _formState.value = PropertyFormState.Error(e.message ?: "Failed to submit property")
            }
        }
    }

    // ── Load landlord's own properties from Firestore ─────────────────────────
    fun loadLandlordPropertiesFromFirestore(landlordId: String) {
        viewModelScope.launch {
            _landlordProperties.value = PropertyListState.Loading
            try {
                val db       = Firebase.firestore
                val snapshot = db.collection("properties")
                    .where { "landlordId" equalTo landlordId }
                    .get()
                val props = snapshot.documents.map { doc ->
                    val prop = doc.data(Property.serializer())
                    // If imageUrls is empty but imageUrl exists, populate list for consistency
                    if (prop.imageUrls.isEmpty() && prop.imageUrl.isNotBlank()) {
                        prop.copy(imageUrls = listOf(prop.imageUrl))
                    } else prop
                }
                _landlordProperties.value = if (props.isEmpty()) PropertyListState.Empty
                                            else PropertyListState.Success(props)
            } catch (e: Exception) {
                _landlordProperties.value = PropertyListState.Error(
                    e.message ?: "Failed to load properties. Check your connection."
                )
            }
        }
    }

    // ── Load approved tenant-visible properties from Firestore ────────────────
    fun loadTenantPropertiesFromFirestore() {
        viewModelScope.launch {
            _tenantProperties.value = PropertyListState.Loading
            try {
                val db       = Firebase.firestore
                val snapshot = db.collection("properties")
                    .where { "status" equalTo "approved" }
                    .get()
                val props = snapshot.documents.map { doc ->
                    val prop = doc.data(Property.serializer())
                    // If imageUrls is empty but imageUrl exists, populate list for consistency
                    if (prop.imageUrls.isEmpty() && prop.imageUrl.isNotBlank()) {
                        prop.copy(imageUrls = listOf(prop.imageUrl))
                    } else prop
                }
                _tenantProperties.value = if (props.isEmpty()) PropertyListState.Empty
                                          else PropertyListState.Success(props)
            } catch (e: Exception) {
                _tenantProperties.value = PropertyListState.Error(
                    e.message ?: "Failed to load listings. Check your connection."
                )
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
                val auth    = Firebase.auth
                val db      = Firebase.firestore
                val storage = Firebase.storage
                val uid     = auth.currentUser?.uid ?: throw Exception("Not authenticated")

                val docRef = db.collection("properties").document(property.id)

                // Upload any newly picked images
                val uploadedUrls = newImageBytes.mapIndexed { index, bytes ->
                    val slot = keepImageUrls.size + index
                    val ref  = storage.reference("property_images/$uid/${property.id}/$slot.jpg")
                    ref.putData(buildStorageData(bytes))
                    ref.getDownloadUrl()
                }

                // Final image list = kept existing + newly uploaded
                val allImageUrls = keepImageUrls + uploadedUrls

                val updatedProperty = property.copy(
                    landlordId = uid,
                    imageUrl   = allImageUrls.firstOrNull() ?: property.imageUrl,
                    imageUrls  = allImageUrls
                )

                docRef.set(updatedProperty)
                // Keep _selectedProperty in sync so screens that observe it
                // (LandlordPropertyDetailScreen, EditPropertyImagesScreen) immediately
                // reflect the new image list without requiring a full Firestore refresh.
                _selectedProperty.value = updatedProperty
                loadLandlordPropertiesFromFirestore(uid)
                _formState.value = PropertyFormState.Success
            } catch (e: Exception) {
                _formState.value = PropertyFormState.Error(e.message ?: "Failed to update property")
            }
        }
    }

    // ── Delete property from Firestore ────────────────────────────────────────
    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("properties").document(propertyId).delete()
                // Refresh landlord list after delete
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                loadLandlordPropertiesFromFirestore(uid)
            } catch (_: Exception) { }
        }
    }
}