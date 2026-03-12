package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BrokerageLedgerEntry(
    val id: String = "",
    val brokerageId: String = "",
    val type: String = "", // subscription_activation | unlock_deduction | top_up | adjustment
    val direction: String = "", // debit | credit
    val amount: Double = 0.0,
    val currency: String = "USD",
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val createdAt: Long = 0L,
    val relatedTransactionId: String = "",
    val propertyId: String = "",
    val tenantId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "success",
    val performedBy: String = "system"
)
