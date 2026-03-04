package org.example.project.data.model

import kotlinx.serialization.Serializable

/**
 * Stored at: users/{uid}/app_settings/preferences
 *
 * A per-user settings document that lives as a subcollection under the user's
 * Firestore document. Keeping it in a subcollection (rather than embedding it
 * in the user doc) means settings can grow independently without touching the
 * top-level user record, and security rules can be scoped precisely.
 */
@Serializable
data class AppSettings(
    /** Whether the user opted into "Remember Me" on their last login. */
    val rememberMe: Boolean = false,

    /** Epoch-millis of the last successful login. Useful for session auditing. */
    val lastLoginAt: Long = 0L,

    /** The device/platform the user last logged in from (e.g. "android", "ios"). */
    val lastLoginPlatform: String = "",

    /** Theme preference — reserved for future use. */
    val theme: String = "system"   // "system" | "light" | "dark"
)
