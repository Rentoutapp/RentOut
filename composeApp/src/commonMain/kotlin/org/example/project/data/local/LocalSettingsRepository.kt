package org.example.project.data.local

import com.russhwolf.settings.Settings

/**
 * Device-local key-value store backed by [Settings] (multiplatform-settings).
 *
 * On Android this maps to SharedPreferences.
 * On iOS this maps to NSUserDefaults.
 *
 * IMPORTANT: Any value stored here is device-specific and is NEVER synced
 * to the cloud. This is intentional — "Remember Me" must only apply to the
 * device on which the user checked the checkbox.
 *
 * Usage:
 *   val repo = LocalSettingsRepository(Settings())
 *   repo.setRememberMe(true)
 *   val remembered = repo.getRememberMe()  // true only on THIS device
 */
class LocalSettingsRepository(private val settings: Settings) {

    companion object {
        private const val KEY_REMEMBER_ME   = "remember_me"
        private const val KEY_REMEMBERED_UID = "remembered_uid"
    }

    /**
     * Persists the "Remember Me" flag for a specific user UID on this device.
     * Storing the UID alongside the flag ensures that if a different user logs
     * in on the same device without checking "Remember Me", the previous user's
     * flag is not incorrectly applied.
     */
    fun setRememberMe(uid: String, rememberMe: Boolean) {
        settings.putBoolean(KEY_REMEMBER_ME, rememberMe)
        settings.putString(KEY_REMEMBERED_UID, if (rememberMe) uid else "")
    }

    /**
     * Returns true only if "Remember Me" was set to true for the given [uid]
     * on this device. Returns false if:
     * - The flag was never set
     * - The flag was set for a different user (multi-account scenario)
     * - The flag was explicitly set to false
     */
    fun getRememberMe(uid: String): Boolean {
        val storedUid = settings.getString(KEY_REMEMBERED_UID, "")
        val flag      = settings.getBoolean(KEY_REMEMBER_ME, false)
        return flag && storedUid == uid
    }

    /**
     * Clears the "Remember Me" flag on this device.
     * Called on logout so the next cold launch requires full authentication.
     */
    fun clearRememberMe() {
        settings.putBoolean(KEY_REMEMBER_ME, false)
        settings.putString(KEY_REMEMBERED_UID, "")
    }
}
