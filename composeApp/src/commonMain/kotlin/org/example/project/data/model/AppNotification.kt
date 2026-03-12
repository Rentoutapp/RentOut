package org.example.project.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single in-app notification stored in Firestore under
 * the `notifications` collection.
 *
 * Firestore document shape:
 * {
 *   id:          String  — document ID (auto-generated)
 *   recipientId: String  — UID of the user who should receive this notification
 *   role:        String  — "landlord" | "tenant" | "agent" | "brokerage"
 *   type:        String  — see NotificationType constants below
 *   title:       String  — short heading shown on the notification card
 *   message:     String  — full body text
 *   propertyId:  String  — optional — links to the related property
 *   propertyTitle: String — optional — human-readable property name
 *   isRead:      Boolean — false = unread / contributes to badge count
 *   createdAt:   Long    — epoch milliseconds
 * }
 */
@Serializable
data class AppNotification(
    val id:            String  = "",
    val recipientId:   String  = "",
    val role:          String  = "",   // "landlord" | "tenant" | "agent" | "brokerage"
    val type:          String  = "",   // see NotificationType
    val title:         String  = "",
    val message:       String  = "",
    val propertyId:    String  = "",
    val propertyTitle: String  = "",
    val isRead:        Boolean = false,
    val createdAt:     Long    = 0L
)

/** Canonical notification type strings written into Firestore. */
object NotificationType {
    // ── Provider (landlord / agent / brokerage) notifications ────────────────
    const val LISTING_APPROVED   = "listing_approved"
    const val LISTING_REJECTED   = "listing_rejected"
    const val LISTING_PENDING    = "listing_pending"
    const val LISTING_FLAGGED    = "listing_flagged"
    const val PROPERTY_UNLOCKED  = "property_unlocked"   // a tenant unlocked their property
    const val ACCOUNT_SUSPENDED  = "account_suspended"

    // ── Tenant notifications ─────────────────────────────────────────────────
    const val UNLOCK_SUCCESS     = "unlock_success"
    const val UNLOCK_FAILED      = "unlock_failed"
    const val PAYMENT_CONFIRMED  = "payment_confirmed"
    const val PROPERTY_UPDATED   = "property_updated"    // a property they unlocked was edited
    const val PROPERTY_REMOVED   = "property_removed"    // listing was taken down

    // ── Generic / system ────────────────────────────────────────────────────
    const val SYSTEM             = "system"
    const val WELCOME            = "welcome"
}

/** Returns the emoji icon appropriate for a notification type. */
fun AppNotification.typeEmoji(): String = when (type) {
    NotificationType.LISTING_APPROVED   -> "✅"
    NotificationType.LISTING_REJECTED   -> "❌"
    NotificationType.LISTING_PENDING    -> "⏳"
    NotificationType.LISTING_FLAGGED    -> "🚩"
    NotificationType.PROPERTY_UNLOCKED  -> "🔓"
    NotificationType.ACCOUNT_SUSPENDED  -> "⚠️"
    NotificationType.UNLOCK_SUCCESS     -> "🔑"
    NotificationType.UNLOCK_FAILED      -> "❗"
    NotificationType.PAYMENT_CONFIRMED  -> "💳"
    NotificationType.PROPERTY_UPDATED   -> "📝"
    NotificationType.PROPERTY_REMOVED   -> "🗑️"
    NotificationType.WELCOME            -> "👋"
    else                                -> "🔔"
}

/** Returns the accent colour hex int appropriate for a notification type. */
fun AppNotification.typeColorInt(): Long = when (type) {
    NotificationType.LISTING_APPROVED,
    NotificationType.UNLOCK_SUCCESS,
    NotificationType.PAYMENT_CONFIRMED  -> 0xFF2ECC71   // green
    NotificationType.LISTING_REJECTED,
    NotificationType.UNLOCK_FAILED,
    NotificationType.PROPERTY_REMOVED,
    NotificationType.ACCOUNT_SUSPENDED  -> 0xFFE74C3C   // red
    NotificationType.LISTING_PENDING    -> 0xFFF39C12   // amber
    NotificationType.LISTING_FLAGGED    -> 0xFFE67E22   // orange
    NotificationType.PROPERTY_UNLOCKED  -> 0xFF9B59B6   // purple
    NotificationType.PROPERTY_UPDATED   -> 0xFF3498DB   // blue
    NotificationType.WELCOME            -> 0xFF1ABC9C   // teal
    else                                -> 0xFF95A5A6   // grey
}
