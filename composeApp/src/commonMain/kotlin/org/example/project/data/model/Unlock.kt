package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Unlock(
    val id: String = "",
    val tenantId: String = "",
    val propertyId: String = "",
    val transactionId: String = "",
    val unlockedAt: Long = 0L
)
