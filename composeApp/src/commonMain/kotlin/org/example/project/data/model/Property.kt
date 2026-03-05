package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: String = "",
    val landlordId: String = "",
    val landlordName: String = "",
    val title: String = "",
    val location: String = "",
    val city: String = "",
    val price: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val rooms: Int = 0,
    val bathrooms: Int = 0,
    val description: String = "",
    val contactNumber: String = "",   // NEVER returned in list queries — only post-unlock
    val imageUrl: String = "",
    val propertyType: String = "apartment", // "apartment" | "house" | "room" | "commercial"
    val amenities: List<String> = emptyList(),
    val status: String = "pending",   // "pending" | "approved" | "rejected"
    val isAvailable: Boolean = true,
    val isVerified: Boolean = false,
    val isFlagged: Boolean = false,
    val createdAt: Long = 0L
)
