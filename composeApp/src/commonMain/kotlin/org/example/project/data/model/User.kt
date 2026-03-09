package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",          // "landlord" | "tenant" | "admin"
    val status: String = "active",  // "active" | "suspended"
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0L,
    val gender: String = "",        // "Male" | "Female" | "Transgender" | "Prefer not to say"
    val nationalId: String = ""     // Zimbabwe national ID, e.g. "63-123456 A78"
)
