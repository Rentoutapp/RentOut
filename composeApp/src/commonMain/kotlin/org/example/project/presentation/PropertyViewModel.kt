package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.model.Property

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

class PropertyViewModel : ViewModel() {

    private val _landlordProperties = MutableStateFlow<PropertyListState>(PropertyListState.Loading)
    val landlordProperties: StateFlow<PropertyListState> = _landlordProperties.asStateFlow()

    private val _tenantProperties = MutableStateFlow<PropertyListState>(PropertyListState.Loading)
    val tenantProperties: StateFlow<PropertyListState> = _tenantProperties.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _formState = MutableStateFlow<PropertyFormState>(PropertyFormState.Idle)
    val formState: StateFlow<PropertyFormState> = _formState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCity = MutableStateFlow("All")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    // Demo data for MVP scaffold
    private val demoProperties = listOf(
        Property(id = "p1", landlordId = "demo_landlord", title = "Luxury Villa in Borrowdale", city = "Harare",
            location = "Borrowdale, Harare", price = 1200.0, rooms = 4, bathrooms = 3,
            description = "A stunning 4-bedroom villa with a swimming pool, modern finishes, and a beautiful garden. Located in the prestigious Borrowdale suburb.",
            imageUrl = "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800",
            status = "approved", isVerified = true, isAvailable = true, amenities = listOf("Pool", "Garden", "Garage", "WiFi"),
            propertyType = "house", contactNumber = "+263 77 123 4567", createdAt = System.currentTimeMillis()),
        Property(id = "p2", landlordId = "demo_landlord", title = "Modern 2-Bed Apartment", city = "Harare",
            location = "Avondale, Harare", price = 550.0, rooms = 2, bathrooms = 1,
            description = "Stylish apartment in the heart of Avondale. Features open-plan living, fitted kitchen, and secure parking.",
            imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800",
            status = "approved", isVerified = true, isAvailable = true, amenities = listOf("Parking", "Security", "WiFi"),
            propertyType = "apartment", contactNumber = "+263 71 234 5678", createdAt = System.currentTimeMillis()),
        Property(id = "p3", landlordId = "demo_landlord", title = "Cozy Studio Room – CBD", city = "Bulawayo",
            location = "City Centre, Bulawayo", price = 200.0, rooms = 1, bathrooms = 1,
            description = "Affordable studio room perfect for a single professional. Walking distance to shops and transport.",
            imageUrl = "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=800",
            status = "approved", isVerified = true, isAvailable = true, amenities = listOf("Water", "Electricity"),
            propertyType = "room", contactNumber = "+263 73 345 6789", createdAt = System.currentTimeMillis()),
        Property(id = "p4", landlordId = "demo_landlord", title = "Spacious 3-Bed House", city = "Harare",
            location = "Mt Pleasant, Harare", price = 800.0, rooms = 3, bathrooms = 2,
            description = "Family home with a large yard and servant quarters. Quiet neighbourhood with excellent security.",
            imageUrl = "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800",
            status = "pending", isVerified = false, isAvailable = true, amenities = listOf("Garden", "Garage", "Borehole"),
            propertyType = "house", contactNumber = "+263 77 456 7890", createdAt = System.currentTimeMillis()),
        Property(id = "p5", landlordId = "demo_landlord", title = "Executive Apartment – Newlands", city = "Harare",
            location = "Newlands, Harare", price = 950.0, rooms = 3, bathrooms = 2,
            description = "High-end apartment with panoramic city views, backup power, and rooftop terrace access.",
            imageUrl = "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800",
            status = "approved", isVerified = true, isAvailable = false, amenities = listOf("Generator", "Rooftop", "Gym", "Pool"),
            propertyType = "apartment", contactNumber = "+263 71 567 8901", createdAt = System.currentTimeMillis()),
    )

    fun loadLandlordProperties(landlordId: String) {
        viewModelScope.launch {
            _landlordProperties.value = PropertyListState.Loading
            val props = demoProperties.filter { it.landlordId == landlordId }
            _landlordProperties.value = if (props.isEmpty()) PropertyListState.Empty
                                        else PropertyListState.Success(props)
        }
    }

    fun loadTenantProperties() {
        viewModelScope.launch {
            _tenantProperties.value = PropertyListState.Loading
            val approved = demoProperties.filter { it.status == "approved" }
            _tenantProperties.value = if (approved.isEmpty()) PropertyListState.Empty
                                      else PropertyListState.Success(approved)
        }
    }

    fun selectProperty(property: Property) {
        _selectedProperty.value = property
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    fun submitProperty(property: Property) {
        viewModelScope.launch {
            _formState.value = PropertyFormState.Uploading
            // Wire Firebase here — for MVP demo, just simulate success
            kotlinx.coroutines.delay(1500)
            _formState.value = PropertyFormState.Success
        }
    }

    fun toggleAvailability(propertyId: String) {
        viewModelScope.launch {
            // Wire Firebase updateProperty here
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            // Wire Firebase deleteProperty here
        }
    }

    fun clearFormState() {
        _formState.value = PropertyFormState.Idle
    }
}
