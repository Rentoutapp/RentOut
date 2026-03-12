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
    val createdAt: Long = 0L,
    val propertyTitle: String = "",
    val propertyLocation: String = "",
    val propertyCity: String = "",
    val propertyType: String = "",
    val propertyImageUrl: String = "",
    val propertyRooms: Int = 0,
    val providerSubtype: String = "",
    val brokerageDeductionAmount: Double = 0.0,
    val brokerageFloatBefore: Double = 0.0,
    val brokerageFloatAfter: Double = 0.0,
    val brokerageLedgerEntryId: String = "",
    val brokerageSettlementStatus: String = "",
    val brokerageStatusMessage: String = ""
)
