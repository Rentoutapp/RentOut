package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import org.example.project.ui.util.buildStorageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.model.AppSettings
import org.example.project.data.model.User

// ─── UI State ─────────────────────────────────────────────────────────────────
sealed class AuthState {
    object Idle      : AuthState()
    object Loading   : AuthState()
    data class Success(val user: User) : AuthState()
    data class Registered(val email: String, val password: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object Suspended : AuthState()
}

sealed class AuthEvent {
    data class Login(val email: String, val password: String, val rememberMe: Boolean = false) : AuthEvent()
    data class Register(
        val name: String,
        val email: String,
        val password: String,
        val role: String,
        val phoneNumber: String = "",
        val profilePhotoUrl: String = "",
        val photoBytes: ByteArray? = null
    ) : AuthEvent()
    object Logout : AuthEvent()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    // ── Upload state ────────────────────────────────────────────────────────────
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    /**
     * Uploads raw image bytes to Firebase Storage at:
     *   profile_photos/{uid}/profile.jpg
     * Returns the public download URL on success, or throws on failure.
     *
     * Uses [dev.gitlive.firebase.storage.StorageReference.putData] with a
     * platform [dev.gitlive.firebase.storage.Data] instance built from the byte array,
     * then fetches the download URL via [getDownloadUrl].
     */
    suspend fun uploadProfilePhoto(uid: String, imageBytes: ByteArray): String {
        val ref = storage.reference.child("profile_photos/$uid/profile.jpg")
        val metadata = dev.gitlive.firebase.storage.storageMetadata {
            contentType = "image/jpeg"
        }
        ref.putData(buildStorageData(imageBytes), metadata)
        return ref.getDownloadUrl()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _selectedRole = MutableStateFlow("")
    val selectedRole: StateFlow<String> = _selectedRole.asStateFlow()

    /**
     * True once the remember-me session check has completed.
     * The UI should show a neutral loading state until this is true
     * so the user never sees a flash of the intro screen before being
     * auto-navigated to the dashboard.
     */
    private val _sessionChecked = MutableStateFlow(false)
    val sessionChecked: StateFlow<Boolean> = _sessionChecked.asStateFlow()

    /**
     * True if the session check found a valid rememberMe=true session.
     * App.kt uses this to decide which path to take from IntroScreen.
     */
    private val _rememberMeActive = MutableStateFlow(false)
    val rememberMeActive: StateFlow<Boolean> = _rememberMeActive.asStateFlow()

    init {
        checkSession()
    }

    /**
     * Called once at startup. Checks if Firebase still has an authenticated
     * user AND that user had rememberMe=true in their app_settings.
     * If both are true, auto-restores AuthState.Success so the UI can
     * skip the role/auth screens entirely.
     */
    private fun checkSession() {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val settings = loadAppSettings(firebaseUser.uid)
                    if (settings.rememberMe) {
                        // Restore full user profile from Firestore
                        val doc = firestore.collection("users").document(firebaseUser.uid).get()
                        val role = doc.get("role") as? String ?: "tenant"
                        val name = doc.get("name") as? String ?: firebaseUser.displayName ?: firebaseUser.email ?: ""
                        val user = User(
                            uid             = firebaseUser.uid,
                            name            = name,
                            email           = firebaseUser.email ?: "",
                            role            = role,
                            status          = doc.get("status")          as? String ?: "active",
                            phoneNumber     = doc.get("phoneNumber")     as? String ?: "",
                            profilePhotoUrl = doc.get("profilePhotoUrl") as? String ?: "",
                            createdAt       = doc.get("createdAt")       as? Long   ?: 0L
                        )
                        _rememberMeActive.value = true
                        _authState.value = if (role == "suspended") AuthState.Suspended else AuthState.Success(user)
                    }
                }
            } catch (e: Exception) {
                // Non-fatal — fall through to normal login flow
                println("[Session] checkSession error: ${e.message}")
            } finally {
                _sessionChecked.value = true
            }
        }
    }

    fun selectRole(role: String) {
        _selectedRole.value = role
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login    -> login(event.email, event.password, event.rememberMe)
            is AuthEvent.Register -> register(
                event.name, event.email, event.password, event.role,
                event.phoneNumber, event.profilePhotoUrl, event.photoBytes
            )
            is AuthEvent.Logout   -> logout()
        }
    }

    // ─── App Settings ──────────────────────────────────────────────────────────

    /**
     * Persists the user's app preferences to:
     *   users/{uid}/app_settings/preferences
     *
     * Using merge = true so that future fields added to AppSettings
     * don't wipe out fields written by other clients/versions.
     */
    private suspend fun saveAppSettings(uid: String, settings: AppSettings) {
        firestore
            .collection("users")
            .document(uid)
            .collection("app_settings")
            .document("preferences")
            .set(
                mapOf(
                    "rememberMe"        to settings.rememberMe,
                    "lastLoginAt"       to settings.lastLoginAt,
                    "lastLoginPlatform" to settings.lastLoginPlatform,
                    "theme"             to settings.theme
                ),
                merge = true
            )
    }

    /**
     * Reads the user's app preferences from Firestore.
     * Returns a default [AppSettings] if the document doesn't exist yet.
     */
    private suspend fun loadAppSettings(uid: String): AppSettings {
        val doc = firestore
            .collection("users")
            .document(uid)
            .collection("app_settings")
            .document("preferences")
            .get()

        return if (doc.exists) {
            AppSettings(
                rememberMe        = doc.get("rememberMe")        as? Boolean ?: false,
                lastLoginAt       = doc.get("lastLoginAt")       as? Long    ?: 0L,
                lastLoginPlatform = doc.get("lastLoginPlatform") as? String  ?: "",
                theme             = doc.get("theme")             as? String  ?: "system"
            )
        } else {
            AppSettings()
        }
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    private fun login(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password)
                val firebaseUser = result.user
                    ?: throw Exception("Login failed. Please try again.")

                // Fetch user profile from Firestore
                val doc = firestore.collection("users").document(firebaseUser.uid).get()
                val role = doc.get("role") as? String ?: "tenant"
                val name = doc.get("name") as? String ?: firebaseUser.displayName ?: email

                val user = User(
                    uid             = firebaseUser.uid,
                    name            = name,
                    email           = firebaseUser.email ?: email,
                    role            = role,
                    status          = doc.get("status")          as? String ?: "active",
                    phoneNumber     = doc.get("phoneNumber")     as? String ?: "",
                    profilePhotoUrl = doc.get("profilePhotoUrl") as? String ?: "",
                    createdAt       = doc.get("createdAt")       as? Long   ?: 0L
                )

                // Persist app settings — always update lastLoginAt; honour the
                // user's rememberMe choice by reading existing settings first so
                // other fields (e.g. theme) are not reset.
                // Isolated in its own try/catch so a settings write failure
                // never blocks a successful login.
                try {
                    val existingSettings = loadAppSettings(firebaseUser.uid)
                    saveAppSettings(
                        uid = firebaseUser.uid,
                        settings = existingSettings.copy(
                            rememberMe        = rememberMe,
                            lastLoginAt       = System.currentTimeMillis(),
                            lastLoginPlatform = "android"
                        )
                    )
                    println("[AppSettings] Saved — rememberMe=$rememberMe uid=${firebaseUser.uid}")
                } catch (e: Exception) {
                    // Non-fatal: log and continue. Never fail login over a settings write.
                    println("[AppSettings] ERROR saving settings: ${e.message}")
                }

                if (role == "suspended") {
                    _authState.value = AuthState.Suspended
                } else {
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }

    private fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phoneNumber: String = "",
        profilePhotoUrl: String = "",
        photoBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password)
                val firebaseUser = result.user
                    ?: throw Exception("Registration failed. Please try again.")

                val createdAt = System.currentTimeMillis()

                // Upload photo to Firebase Storage if bytes are available
                val finalPhotoUrl = if (photoBytes != null && photoBytes.isNotEmpty()) {
                    try {
                        uploadProfilePhoto(firebaseUser.uid, photoBytes)
                    } catch (e: Exception) {
                        println("[Storage] Photo upload failed: ${e.message}")
                        profilePhotoUrl  // fall back to placeholder URI
                    }
                } else {
                    profilePhotoUrl
                }

                // Persist user profile in Firestore
                val user = User(
                    uid             = firebaseUser.uid,
                    name            = name,
                    email           = firebaseUser.email ?: email,
                    role            = role,
                    status          = "active",
                    phoneNumber     = phoneNumber,
                    profilePhotoUrl = finalPhotoUrl,
                    createdAt       = createdAt
                )
                firestore.collection("users").document(firebaseUser.uid).set(
                    mapOf(
                        "uid"             to user.uid,
                        "name"            to user.name,
                        "email"           to user.email,
                        "role"            to user.role,
                        "status"          to user.status,
                        "phoneNumber"     to user.phoneNumber,
                        "profilePhotoUrl" to user.profilePhotoUrl,
                        "createdAt"       to user.createdAt
                    )
                )

                // Sign out immediately — user must explicitly log in
                auth.signOut()
                _authState.value = AuthState.Registered(email, password)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed. Please try again.")
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                // Reset rememberMe before signing out so the next cold launch
                // goes through the full role/auth flow as expected.
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    try {
                        val existingSettings = loadAppSettings(uid)
                        saveAppSettings(uid, existingSettings.copy(rememberMe = false))
                        println("[Session] rememberMe reset to false for uid=$uid")
                    } catch (e: Exception) {
                        println("[Session] Failed to reset rememberMe: ${e.message}")
                    }
                }
                auth.signOut()
            } finally {
                _rememberMeActive.value = false
                _authState.value = AuthState.Idle
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun clearRegistered() {
        if (_authState.value is AuthState.Registered) {
            _authState.value = AuthState.Idle
        }
    }
}
