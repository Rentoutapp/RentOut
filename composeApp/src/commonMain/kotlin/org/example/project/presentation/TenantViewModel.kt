package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.model.Property
import org.example.project.data.model.Unlock

sealed class UnlockState {
    object Idle    : UnlockState()
    object Loading : UnlockState()
    object Success : UnlockState()
    data class Error(val message: String) : UnlockState()
}

class TenantViewModel : ViewModel() {

    private val _unlockedPropertyIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedPropertyIds: StateFlow<Set<String>> = _unlockedPropertyIds.asStateFlow()

    private val _unlockedProperties = MutableStateFlow<List<Property>>(emptyList())
    val unlockedProperties: StateFlow<List<Property>> = _unlockedProperties.asStateFlow()

    private val _unlockState = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    // ── Load unlocks for this tenant from Firestore ───────────────────────────
    fun loadUnlockedProperties(tenantId: String) {
        viewModelScope.launch {
            try {
                val db       = Firebase.firestore
                val snapshot = db.collection("unlocks")
                    .where { "tenantId" equalTo tenantId }
                    .get()
                val ids = snapshot.documents.map { it.get("propertyId") as? String ?: "" }.toSet()
                _unlockedPropertyIds.value = ids

                // Fetch full property docs for each unlocked id
                if (ids.isNotEmpty()) {
                    val props = ids.mapNotNull { pid ->
                        try {
                            val doc = db.collection("properties").document(pid).get()
                            if (doc.exists) doc.data(Property.serializer()) else null
                        } catch (_: Exception) { null }
                    }
                    _unlockedProperties.value = props
                }
            } catch (_: Exception) {
                // Silent — unlocked list stays empty until data is available
            }
        }
    }

    // ── Check if a single property is unlocked ────────────────────────────────
    fun checkUnlockStatus(tenantId: String, propertyId: String) {
        viewModelScope.launch {
            try {
                val db  = Firebase.firestore
                val doc = db.collection("unlocks")
                    .document("${tenantId}_${propertyId}")
                    .get()
                if (doc.exists) {
                    _unlockedPropertyIds.value = _unlockedPropertyIds.value + propertyId
                }
            } catch (_: Exception) { }
        }
    }

    // ── Called by Cloud Function webhook after payment verified ───────────────
    // In production this is written server-side; here we poll Firestore to
    // detect the unlock doc created by the Cloud Function.
    fun processPaymentSuccess(tenantId: String, property: Property) {
        viewModelScope.launch {
            _unlockState.value = UnlockState.Loading
            try {
                val db      = Firebase.firestore
                val unlockId = "${tenantId}_${property.id}"
                val doc = db.collection("unlocks").document(unlockId).get()
                if (doc.exists) {
                    val newIds  = _unlockedPropertyIds.value + property.id
                    _unlockedPropertyIds.value = newIds
                    val newList = _unlockedProperties.value.toMutableList()
                    if (newList.none { it.id == property.id }) newList.add(property)
                    _unlockedProperties.value = newList
                    _unlockState.value = UnlockState.Success
                } else {
                    _unlockState.value = UnlockState.Error(
                        "Payment received but unlock not confirmed yet. Please refresh."
                    )
                }
            } catch (e: Exception) {
                _unlockState.value = UnlockState.Error(e.message ?: "Unlock failed.")
            }
        }
    }

    fun isPropertyUnlocked(propertyId: String): Boolean =
        _unlockedPropertyIds.value.contains(propertyId)

    fun resetUnlockState() { _unlockState.value = UnlockState.Idle }
}