package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: String = "",
    val landlordId: String = "",
    val landlordName: String = "",
    val providerSubtype: String = "",   // "landlord" | "agent" | "brokerage" | ""
    val title: String = "",
    val location: String = "",
    val city: String = "",
    val price: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val depositNotApplicable: Boolean = false,
    val rooms: Int = 0,
    val customBedroomDetails: String = "",
    val bathrooms: Int = 0,
    val bathroomType: String = "",            // "En-suite" | "Shared" | "Common Bathroom" | "Other"
    val customBathroomDetails: String = "",  // free-text when bathroomType == "Other"
    val hasSharedKitchen: Boolean = false,
    val kitchenCount: Int = 0,
    val description: String = "",
    val contactNumber: String = "",       // NEVER returned in list queries — only post-unlock
    // Agent-specific fields (providerSubtype == "agent")
    val agentName: String = "",           // Freelance agent's full name
    val agentContactNumber: String = "",  // Freelance agent's contact number
    // Brokerage-specific fields (providerSubtype == "brokerage")
    val brokerName: String = "",          // Individual broker's full name
    val brokerContactNumber: String = "", // Individual broker's contact number
    val brokerageName: String = "",       // Registered company / brokerage name
    val brokerageLogoUrl: String = "",    // Company logo URL (from user.companyLogoUrl)
    val brokerageAddress: String = "",    // Company office address
    val brokerageContactNumber: String = "", // Company contact number
    val brokerageEmail: String = "",      // Company email address
    val imageUrl: String = "",            // primary / cover image (backward compat)
    val imageUrls: List<String> = emptyList(), // all uploaded images
    // Classification hierarchy
    val classification: String = "Residential",   // "Residential"|"Commercial"|"Industrial"|"Land"|"Mixed-Use"
    val propertyType: String = "apartment",        // fine-grained type within classification
    val locationType: String = "",                 // "Low Density"|"Medium Density"|"High Density"|"Peri-Urban Residential"|"Rural"
    // Bills inclusion/exclusion
    val billsInclusive: List<String> = emptyList(),  // WiFi, Water, Electricity
    val billsExclusive: List<String> = emptyList(),
    // Room-type quantity
    val roomQuantity: String = "",        // "One Room"|"Two Rooms"|...|"Five+ Rooms"
    // Proximity facilities
    val proximityFacilities: List<String> = emptyList(),
    // Geo coordinates
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // Availability
    val availabilityDate: String = "",    // ISO date string or empty = available now
    // Tenant requirements — who the landlord prefers
    val tenantRequirements: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val status: String = "pending",       // "pending" | "approved" | "rejected"
    val isAvailable: Boolean = true,
    val isVerified: Boolean = false,
    val isFlagged: Boolean = false,
    val createdAt: Long = 0L
)
