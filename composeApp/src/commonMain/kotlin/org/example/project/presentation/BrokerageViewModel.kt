package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.example.project.data.model.BrokerageLedgerEntry
import org.example.project.data.model.BrokerageTopUpRequest
import org.example.project.data.model.User

/**
 * Mirrors the Cloud Function `initiateBrokerageTopUp` return shape.
 * Every field is optional with a safe default so partial responses
 * (e.g. live-mode path that omits newBalance) never cause a crash.
 */
@Serializable
private data class TopUpResponse(
    val success: Boolean = false,
    val requestId: String = "",
    val paymentMode: String = "demo",
    val newBalance: Double = 0.0,
    val amountCredited: Double = 0.0,
    val message: String = "",
    val checkoutUrl: String = "",
)

sealed class BrokerageTopUpState {
    object Idle : BrokerageTopUpState()
    object Loading : BrokerageTopUpState()
    data class AwaitingCheckout(
        val amount: Double,
        val requestId: String,
        val checkoutUrl: String,
        val paymentMode: String,
        val message: String = ""
    ) : BrokerageTopUpState()
    data class Success(val amount: Double) : BrokerageTopUpState()
    data class Error(val message: String) : BrokerageTopUpState()
}

class BrokerageViewModel : ViewModel() {
    private val _brokerageAccount = MutableStateFlow(User())
    val brokerageAccount: StateFlow<User> = _brokerageAccount.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<BrokerageLedgerEntry>>(emptyList())
    val ledgerEntries: StateFlow<List<BrokerageLedgerEntry>> = _ledgerEntries.asStateFlow()

    private val _isAccountLoading = MutableStateFlow(false)
    val isAccountLoading: StateFlow<Boolean> = _isAccountLoading.asStateFlow()

    private val _isLedgerLoading = MutableStateFlow(false)
    val isLedgerLoading: StateFlow<Boolean> = _isLedgerLoading.asStateFlow()

    private val _topUpState = MutableStateFlow<BrokerageTopUpState>(BrokerageTopUpState.Idle)
    val topUpState: StateFlow<BrokerageTopUpState> = _topUpState.asStateFlow()

    private var accountListenerJob: Job? = null
    private var ledgerListenerJob: Job? = null

    fun observeBrokerageAccount(uid: String) {
        accountListenerJob?.cancel()
        accountListenerJob = viewModelScope.launch {
            _isAccountLoading.value = true
            Firebase.firestore.collection("users")
                .document(uid)
                .snapshots
                .catch {
                    _isAccountLoading.value = false
                }
                .collect { doc ->
                    _brokerageAccount.value = doc.data(User.serializer())
                    _isAccountLoading.value = false
                }
        }
    }

    fun observeLedger(uid: String) {
        ledgerListenerJob?.cancel()
        ledgerListenerJob = viewModelScope.launch {
            _isLedgerLoading.value = true
            Firebase.firestore.collection("brokerage_float_ledger")
                .where { "brokerageId" equalTo uid }
                .snapshots
                .catch {
                    _isLedgerLoading.value = false
                }
                .collect { snapshot ->
                    _ledgerEntries.value = snapshot.documents
                        .map { it.data(BrokerageLedgerEntry.serializer()) }
                        .sortedByDescending { it.createdAt }
                    _isLedgerLoading.value = false
                }
        }
    }

    fun topUpFloat(amountUsd: Double) {
        if (amountUsd <= 0.0) {
            _topUpState.value = BrokerageTopUpState.Error("Enter a valid top-up amount.")
            return
        }
        viewModelScope.launch {
            _topUpState.value = BrokerageTopUpState.Loading
            try {
                val result = Firebase.functions
                    .httpsCallable("initiateBrokerageTopUp")
                    .invoke(mapOf(
                        "amountUsd"  to amountUsd,
                        "successUrl" to "rentout://payment-success",
                        "cancelUrl"  to "rentout://payment-cancel"
                    ))

                // Use a @Serializable data class — the ONLY approach that works with
                // gitlive Firebase KMP on Android. Using data<Map<String, Any?>>() or
                // data<Any?>() throws "Serializer for class 'kotlin.Any' is not found."
                // because kotlinx.serialization cannot handle raw Any types.
                val response = result.data<TopUpResponse>()

                println("[BrokerageViewModel] topUpFloat: success=${response.success} " +
                        "checkoutUrl='${response.checkoutUrl}' requestId='${response.requestId}' " +
                        "newBalance=${response.newBalance}")

                when {
                    // Demo / direct-success path — top-up already applied server-side,
                    // checkoutUrl is blank, show success immediately.
                    response.success && response.checkoutUrl.isBlank() -> {
                        _topUpState.value = BrokerageTopUpState.Success(amountUsd)
                    }

                    // Live / PesePay path — redirect to checkout URL, then poll.
                    response.success && response.requestId.isNotBlank() && response.checkoutUrl.isNotBlank() -> {
                        _topUpState.value = BrokerageTopUpState.AwaitingCheckout(
                            amount      = amountUsd,
                            requestId   = response.requestId,
                            checkoutUrl = response.checkoutUrl,
                            paymentMode = response.paymentMode,
                            message     = response.message
                        )
                        waitForTopUpCompletion(response.requestId, amountUsd)
                    }

                    // Server returned success=false — surface the server's message.
                    else -> {
                        _topUpState.value = BrokerageTopUpState.Error(
                            response.message.ifBlank { "Top-up failed. Please try again." }
                        )
                    }
                }
            } catch (e: Exception) {
                println("[BrokerageViewModel] topUpFloat: exception: ${e.message}")
                _topUpState.value = BrokerageTopUpState.Error(
                    e.message
                        ?.substringAfter("INTERNAL: ")
                        ?.takeIf { it.isNotBlank() }
                        ?: "Top-up failed. Please try again."
                )
            }
        }
    }

    private fun waitForTopUpCompletion(requestId: String, amountUsd: Double) {
        viewModelScope.launch {
            repeat(30) {
                try {
                    val doc = Firebase.firestore.collection("brokerage_topup_requests").document(requestId).get()
                    if (doc.exists) {
                        val request = doc.data(BrokerageTopUpRequest.serializer())
                        when (request.status.lowercase()) {
                            "success" -> {
                                _topUpState.value = BrokerageTopUpState.Success(amountUsd)
                                return@launch
                            }
                            "failed" -> {
                                _topUpState.value = BrokerageTopUpState.Error(request.message.ifBlank { "Top-up failed." })
                                return@launch
                            }
                        }
                    }
                } catch (_: Exception) { }
                delay(1500)
            }
            _topUpState.value = BrokerageTopUpState.Error("Top-up confirmation timed out. Return to the app and refresh your brokerage account.")
        }
    }

    fun resetTopUpState() {
        _topUpState.value = BrokerageTopUpState.Idle
    }

    fun stop() {
        accountListenerJob?.cancel()
        ledgerListenerJob?.cancel()
        accountListenerJob = null
        ledgerListenerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
