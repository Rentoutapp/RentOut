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
import org.example.project.data.local.LocalSettingsRepository
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

class AuthViewModel(
    private val localSettings: LocalSettingsRepository
) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    // ── Registration progress (0f → 1f) exposed to UI ────────────────────────
    private val _registrationProgress = MutableStateFlow(0f)
    val registrationProgress: StateFlow<Float> = _registrationProgress.asStateFlow()

    private val _registrationStep = MutableStateFlow("")
    val registrationStep: StateFlow<String> = _registrationStep.asStateFlow()

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
     * user AND that the "Remember Me" flag is set to true on THIS device
     * for that specific user UID.
     *
     * Using [LocalSettingsRepository] (device-local storage) guarantees that
     * the flag is device-specific — checking "Remember Me" on Device A will
     * never auto-login on Device B.
     */
    private fun checkSession() {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    // Read rememberMe from LOCAL device storage only — not Firestore
                    val rememberMe = localSettings.getRememberMe(firebaseUser.uid)
                    println("[Session] checkSession uid=${firebaseUser.uid} rememberMe=$rememberMe")
                    if (rememberMe) {
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

                // 1. Save rememberMe to LOCAL device storage — device-specific,
                //    never synced to cloud. This is the source of truth for session restore.
                localSettings.setRememberMe(firebaseUser.uid, rememberMe)
                println("[Session] rememberMe=$rememberMe saved locally for uid=${firebaseUser.uid}")

                // 2. Save non-sensitive cross-device metadata to Firestore app_settings.
                //    rememberMe is intentionally NOT written here — it stays local only.
                try {
                    val existingSettings = loadAppSettings(firebaseUser.uid)
                    saveAppSettings(
                        uid = firebaseUser.uid,
                        settings = existingSettings.copy(
                            lastLoginAt       = System.currentTimeMillis(),
                            lastLoginPlatform = "android"
                        )
                    )
                } catch (e: Exception) {
                    println("[AppSettings] ERROR saving cloud settings: ${e.message}")
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
            _registrationProgress.value = 0f
            try {
                // Step 1 — Create Firebase Auth account (0 → 30%)
                _registrationStep.value = "Creating your account…"
                _registrationProgress.value = 0.1f
                val result = auth.createUserWithEmailAndPassword(email, password)
                val firebaseUser = result.user
                    ?: throw Exception("Registration failed. Please try again.")
                _registrationProgress.value = 0.3f

                val createdAt = System.currentTimeMillis()

                // Step 2 — Upload photo (30 → 70%)
                _registrationStep.value = "Uploading profile photo…"
                _registrationProgress.value = 0.35f
                val finalPhotoUrl = if (photoBytes != null && photoBytes.isNotEmpty()) {
                    try {
                        val url = uploadProfilePhoto(firebaseUser.uid, photoBytes)
                        _registrationProgress.value = 0.70f
                        url
                    } catch (e: Exception) {
                        println("[Storage] Photo upload failed: ${e.message}")
                        _registrationProgress.value = 0.70f
                        profilePhotoUrl
                    }
                } else {
                    _registrationProgress.value = 0.70f
                    profilePhotoUrl
                }

                // Step 3 — Save to Firestore (70 → 90%)
                _registrationStep.value = "Saving your profile details…"
                _registrationProgress.value = 0.75f
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
                _registrationProgress.value = 0.90f

                // Step 4 — Finalise (90 → 100%)
                _registrationStep.value = "Almost done…"
                _registrationProgress.value = 1.0f

                // Sign out immediately — user must explicitly log in
                auth.signOut()
                _authState.value = AuthState.Registered(email, password)
            } catch (e: Exception) {
                _registrationProgress.value = 0f
                _registrationStep.value = ""
                _authState.value = AuthState.Error(e.message ?: "Registration failed. Please try again.")
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                // Clear rememberMe from local device storage only.
                // No Firestore write needed — rememberMe was never stored there.
                localSettings.clearRememberMe()
                println("[Session] rememberMe cleared from local storage on logout")
                auth.signOut()
            } finally {
                _rememberMeActive.value = false
                _authState.value = AuthState.Idle
            }
        }
    }

    /**
     * Re-fetches the current user's Firestore profile and updates [authState].
     * Call this whenever the user's profile may have changed (e.g. after a
     * photo upload that previously failed, or after a profile edit).
     * No-op if the user is not currently logged in.
     */
    fun refreshUser() {
        viewModelScope.launch {
            val firebaseUser = auth.currentUser ?: return@launch
            try {
                val doc = firestore.collection("users").document(firebaseUser.uid).get()
                val role            = doc.get("role")            as? String ?: "tenant"
                val name            = doc.get("name")            as? String ?: firebaseUser.displayName ?: firebaseUser.email ?: ""
                val profilePhotoUrl = doc.get("profilePhotoUrl") as? String ?: ""
                val user = User(
                    uid             = firebaseUser.uid,
                    name            = name,
                    email           = firebaseUser.email ?: "",
                    role            = role,
                    status          = doc.get("status")      as? String ?: "active",
                    phoneNumber     = doc.get("phoneNumber") as? String ?: "",
                    profilePhotoUrl = profilePhotoUrl,
                    createdAt       = doc.get("createdAt")   as? Long   ?: 0L
                )
                println("[AuthViewModel] refreshUser — profilePhotoUrl=$profilePhotoUrl")
                if (role != "suspended") {
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                println("[AuthViewModel] refreshUser error: ${e.message}")
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

    /**
     * Deletes the current user's account permanently.
     * This will:
     * 1. Delete the user's Firestore document
     * 2. Delete all properties owned by the user
     * 3. Delete all transactions related to the user
     * 4. Delete the Firebase Auth account
     * 5. Clear local storage
     */
    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    onError("No user is currently signed in")
                    return@launch
                }
                
                val uid = currentUser.uid
                println("🗑️ [AuthViewModel] Starting account deletion for user: $uid")
                
                _authState.value = AuthState.Loading
                
                // 1. Delete user's properties
                println("   Deleting user properties...")
                val propertiesSnapshot = firestore
                    .collection("properties")
                    .where { "landlordId" equalTo uid }
                    .get()
                
                propertiesSnapshot.documents.forEach { doc ->
                    println("   Deleting property: ${doc.id}")
                    firestore.collection("properties").document(doc.id).delete()
                }
                
                // 2. Delete user's transactions (as tenant)
                println("   Deleting user transactions (tenant)...")
                val tenantTransactionsSnapshot = firestore
                    .collection("transactions")
                    .where { "tenantId" equalTo uid }
                    .get()
                
                tenantTransactionsSnapshot.documents.forEach { doc ->
                    println("   Deleting transaction: ${doc.id}")
                    firestore.collection("transactions").document(doc.id).delete()
                }
                
                // 3. Delete user's transactions (as landlord)
                println("   Deleting user transactions (landlord)...")
                val landlordTransactionsSnapshot = firestore
                    .collection("transactions")
                    .where { "landlordId" equalTo uid }
                    .get()
                
                landlordTransactionsSnapshot.documents.forEach { doc ->
                    println("   Deleting transaction: ${doc.id}")
                    firestore.collection("transactions").document(doc.id).delete()
                }
                
                // 4. Delete user's unlocked properties
                println("   Deleting unlocked properties...")
                val unlocksSnapshot = firestore
                    .collection("unlocks")
                    .where { "tenantId" equalTo uid }
                    .get()
                
                unlocksSnapshot.documents.forEach { doc ->
                    println("   Deleting unlock: ${doc.id}")
                    firestore.collection("unlocks").document(doc.id).delete()
                }
                
                // 5. Delete user's Firestore document
                println("   Deleting user document...")
                firestore.collection("users").document(uid).delete()
                
                // 6. Clear local storage
                println("   Clearing local storage...")
                localSettings.clearRememberMe()
                
                // 7. Delete Firebase Auth account
                println("   Deleting Firebase Auth account...")
                currentUser.delete()
                
                // 8. Reset state
                _rememberMeActive.value = false
                _authState.value = AuthState.Idle
                
                println("✅ [AuthViewModel] Account deletion completed successfully")
                onSuccess()
                
            } catch (e: Exception) {
                println("❌ [AuthViewModel] Account deletion failed: ${e.message}")
                e.printStackTrace()
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
                onError(e.message ?: "Failed to delete account")
            }
        }
    }
}
