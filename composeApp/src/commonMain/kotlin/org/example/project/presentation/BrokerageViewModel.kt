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
import org.example.project.data.model.BrokerageLedgerEntry
import org.example.project.data.model.BrokerageTopUpRequest
import org.example.project.data.model.User

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
                        "amountUsd" to amountUsd,
                        "successUrl" to "rentout://payment-success",
                        "cancelUrl"  to "rentout://payment-cancel"
                    ))

                // Firebase KMP returns the Cloud Function result in different shapes
                // depending on platform (Android/iOS/JS). Try the typed decode first;
                // if it throws (e.g. Android wraps the map in a different container),
                // fall back to extracting raw values from the HttpsCallableResult
                // by converting the result to a String representation and parsing it,
                // or by using the dynamic/Any accessor pattern below.
                //
                // Strategy: attempt typed decode, then fall back to raw Any extraction.
                var data: Map<String, Any?>? = null
                try {
                    @Suppress("UNCHECKED_CAST")
                    data = result.data<Map<String, Any?>>()
                } catch (e: Exception) {
                    println("[BrokerageViewModel] topUpFloat: typed decode failed: ${e.message}")
                }
                // Second fallback: if data is null or empty after typed decode, try
                // casting the raw result data directly as a Map.
                if (data == null || data.isEmpty()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val raw = result.data<Any?>()
                        if (raw is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            data = raw as Map<String, Any?>
                        }
                    } catch (e: Exception) {
                        println("[BrokerageViewModel] topUpFloat: raw Any decode failed: ${e.message}")
                    }
                }

                println("[BrokerageViewModel] topUpFloat: decoded data=$data")

                // If we still have no data, treat it as a network/decode failure.
                if (data == null) {
                    _topUpState.value = BrokerageTopUpState.Error(
                        "Top-up failed: could not read server response. Please try again."
                    )
                    return@launch
                }

                // Boolean — Firebase may return it as Boolean or as a String "true"
                val success: Boolean = when (val raw = data["success"]) {
                    is Boolean -> raw
                    is String  -> raw.equals("true", ignoreCase = true)
                    else       -> false
                }
                val requestId   = (data["requestId"]   as? String) ?: ""
                val checkoutUrl = (data["checkoutUrl"] as? String) ?: ""
                val paymentMode = (data["paymentMode"] as? String) ?: "demo"
                val message     = (data["message"]     as? String) ?: ""
                // newBalance may arrive as Double, Long, Int, or a JS Number — convert safely
                val newBalance: Double? = when (val raw = data["newBalance"]) {
                    is Double -> raw
                    is Long   -> raw.toDouble()
                    is Int    -> raw.toDouble()
                    is Number -> raw.toDouble()
                    else      -> null
                }

                println("[BrokerageViewModel] topUpFloat: success=$success checkoutUrl='$checkoutUrl' requestId='$requestId' newBalance=$newBalance")

                when {
                    // Demo / direct-success path — top-up already applied server-side,
                    // no checkout URL needed. Show success immediately.
                    success && checkoutUrl.isBlank() -> {
                        _topUpState.value = BrokerageTopUpState.Success(amountUsd)
                    }

                    // Live / PesePay path — open checkout URL and poll for completion
                    success && requestId.isNotBlank() && checkoutUrl.isNotBlank() -> {
                        _topUpState.value = BrokerageTopUpState.AwaitingCheckout(
                            amount      = amountUsd,
                            requestId   = requestId,
                            checkoutUrl = checkoutUrl,
                            paymentMode = paymentMode,
                            message     = message
                        )
                        waitForTopUpCompletion(requestId, amountUsd)
                    }

                    // Server returned success=false with an error message
                    else -> {
                        _topUpState.value = BrokerageTopUpState.Error(
                            message.ifBlank { "Top-up failed. Please try again." }
                        )
                    }
                }
            } catch (e: Exception) {
                println("[BrokerageViewModel] topUpFloat: exception: ${e.message}")
                _topUpState.value = BrokerageTopUpState.Error(
                    e.message?.substringAfter("INTERNAL: ")?.substringAfter(": ")
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
