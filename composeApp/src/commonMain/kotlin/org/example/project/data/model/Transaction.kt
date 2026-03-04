package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String = "",
    val tenantId: String = "",
    val propertyId: String = "",
    val landlordId: String = "",
    val amount: Double = 10.0,
    val currency: String = "USD",
    val status: String = "pending",  // "pending" | "success" | "failed"
    val paymentProvider: String = "pesepay",
    val paymentReference: String = "",
    val createdAt: Long = 0L
)
