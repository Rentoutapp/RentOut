package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BrokerageTopUpRequest(
    val id: String = "",
    val brokerageId: String = "",
    val amountUsd: Double = 0.0,
    val currency: String = "USD",
    val status: String = "pending",
    val paymentProvider: String = "pesepay",
    val paymentReference: String = "",
    val paymentMode: String = "demo",
    val checkoutUrl: String = "",
    val createdAt: Long = 0L,
    val completedAt: Long = 0L,
    val ledgerEntryId: String = "",
    val message: String = ""
)
