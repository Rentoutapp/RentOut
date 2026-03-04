package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.model.Property
import org.example.project.data.model.Transaction
import org.example.project.data.model.Unlock

sealed class UnlockState {
    object Idle       : UnlockState()
    object Loading    : UnlockState()
    object Success    : UnlockState()
    data class Error(val message: String) : UnlockState()
}

class TenantViewModel : ViewModel() {

    private val _unlockedPropertyIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedPropertyIds: StateFlow<Set<String>> = _unlockedPropertyIds.asStateFlow()

    private val _unlockedProperties = MutableStateFlow<List<Property>>(emptyList())
    val unlockedProperties: StateFlow<List<Property>> = _unlockedProperties.asStateFlow()

    private val _unlockState = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    fun checkUnlockStatus(tenantId: String, propertyId: String) {
        viewModelScope.launch {
            // Wire Firebase unlocks collection check here
        }
    }

    fun processPaymentSuccess(tenantId: String, property: Property) {
        viewModelScope.launch {
            _unlockState.value = UnlockState.Loading
            try {
                // In production: verify via Cloud Function → write unlock to Firestore
                kotlinx.coroutines.delay(1000)
                val newUnlocked = _unlockedPropertyIds.value + property.id
                _unlockedPropertyIds.value = newUnlocked
                val updatedList = _unlockedProperties.value + property
                _unlockedProperties.value = updatedList
                _unlockState.value = UnlockState.Success
            } catch (e: Exception) {
                _unlockState.value = UnlockState.Error(e.message ?: "Unlock failed.")
            }
        }
    }

    fun isPropertyUnlocked(propertyId: String): Boolean =
        _unlockedPropertyIds.value.contains(propertyId)

    fun resetUnlockState() {
        _unlockState.value = UnlockState.Idle
    }
}
