package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",              // "landlord" | "tenant" | "admin"
    val providerSubtype: String = "",   // "landlord" | "agent" | "brokerage" | "" (tenant/admin)
    val status: String = "active",      // "active" | "suspended"
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0L,
    val gender: String = "",            // "Male" | "Female" | "Transgender" | "Prefer not to say"
    val nationalId: String = "",        // Zimbabwe national ID, e.g. "63-123456 A78"
    // Agent-specific fields
    val agentLicenseNumber: String = "",    // Agent licence / accreditation number
    val yearsOfExperience: String = "",     // Years active as an agent
    // Brokerage-specific fields
    val companyName: String = "",           // Registered company / agency name
    val companyRegNumber: String = "",      // Companies Registry number
    val companyAddress: String = "",        // Physical office address
    val taxId: String = ""                  // ZIMRA / tax clearance number (optional)
)

// ── Convenience extension helpers used across screens ─────────────────────────

val User.isProvider: Boolean
    get() = role == "landlord"

val User.isAgent: Boolean
    get() = role == "landlord" && providerSubtype == "agent"

val User.isBrokerage: Boolean
    get() = role == "landlord" && providerSubtype == "brokerage"

val User.isLandlord: Boolean
    get() = role == "landlord" && (providerSubtype == "landlord" || providerSubtype == "")

val User.providerDisplayName: String
    get() = when (providerSubtype) {
        "agent"     -> "Freelancer Agent"
        "brokerage" -> "Brokerage"
        else        -> "Landlord"
    }

val User.providerEmoji: String
    get() = when (providerSubtype) {
        "agent"     -> "🤝"
        "brokerage" -> "🏢"
        else        -> "🏠"
    }

val User.dashboardTitle: String
    get() = when (providerSubtype) {
        "agent"     -> "Your Listings"
        "brokerage" -> "Portfolio"
        else        -> "Your Properties"
    }
