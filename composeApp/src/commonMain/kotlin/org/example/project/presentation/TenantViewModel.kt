package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.example.project.data.model.Property
import org.example.project.data.model.Transaction
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

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // ── Real-time listener job for unlocks ────────────────────────────────────
    private var unlocksListenerJob: Job? = null
    private var transactionsListenerJob: Job? = null

    // ── Load unlocks for this tenant — real-time Firestore listener ───────────
    // Whenever a Cloud Function writes a new unlock doc (after payment), this
    // flow fires automatically and fetches the full property details, so the
    // tenant's unlocked list updates without any user action.
    fun loadUnlockedProperties(tenantId: String) {
        unlocksListenerJob?.cancel()
        unlocksListenerJob = viewModelScope.launch {
            Firebase.firestore
                .collection("unlocks")
                .where { "tenantId" equalTo tenantId }
                .snapshots
                .catch { /* Silent — unlocked list stays as-is on error */ }
                .collect { snapshot ->
                    val ids = snapshot.documents
                        .map { it.get("propertyId") as? String ?: "" }
                        .filter { it.isNotBlank() }
                        .toSet()
                    _unlockedPropertyIds.value = ids

                    // Fetch full property docs for each unlocked id
                    if (ids.isNotEmpty()) {
                        val db = Firebase.firestore
                        val props = ids.mapNotNull { pid ->
                            try {
                                val doc = db.collection("properties").document(pid).get()
                                if (doc.exists) doc.data(Property.serializer()) else null
                            } catch (_: Exception) { null }
                        }
                        _unlockedProperties.value = props
                    } else {
                        _unlockedProperties.value = emptyList()
                    }
                }
        }
    }

    // ── Load payment transactions for this tenant — real-time listener ───────
    fun loadTransactions(tenantId: String) {
        println("🔍 TenantViewModel: loadTransactions called with tenantId=$tenantId")
        transactionsListenerJob?.cancel()
        transactionsListenerJob = viewModelScope.launch {
            try {
                Firebase.firestore
                    .collection("transactions")
                    .where { "tenantId" equalTo tenantId }
                    .snapshots
                    .catch { e -> 
                        println("❌ TenantViewModel: Error loading transactions: ${e.message}")
                        e.printStackTrace()
                        // Emit empty list on error so UI doesn't hang
                        emit(Firebase.firestore.collection("transactions").get())
                    }
                    .collect { snapshot ->
                        println("📊 TenantViewModel: Received ${snapshot.documents.size} transaction documents")
                        
                        if (snapshot.documents.isEmpty()) {
                            println("ℹ️ TenantViewModel: No transactions found for tenantId=$tenantId")
                            _transactions.value = emptyList()
                            return@collect
                        }
                        
                        val parsedTransactions = mutableListOf<Transaction>()
                        
                        snapshot.documents.forEach { doc ->
                            try {
                                // Use doc.get() to access fields directly without serialization
                                // This avoids "Serializer for class 'kotlin.Any' is not found" error
                                val id = doc.id
                                val tenantId = doc.get("tenantId") as? String ?: ""
                                val propertyId = doc.get("propertyId") as? String ?: ""
                                val landlordId = doc.get("landlordId") as? String ?: ""
                                val amount = (doc.get("amount") as? Number)?.toDouble() ?: 10.0
                                val currency = doc.get("currency") as? String ?: "USD"
                                val status = doc.get("status") as? String ?: "pending"
                                val paymentProvider = doc.get("paymentProvider") as? String ?: "pesepay"
                                val paymentReference = doc.get("paymentReference") as? String ?: ""
                                
                                // Handle createdAt - could be Timestamp, Long, or Number
                                val createdAtRaw: Any? = doc.get("createdAt")
                                val createdAt = when (createdAtRaw) {
                                    is Long -> {
                                        println("   → createdAt is Long: $createdAtRaw")
                                        createdAtRaw
                                    }
                                    is Int -> {
                                        println("   → createdAt is Int: $createdAtRaw")
                                        createdAtRaw.toLong()
                                    }
                                    is Double -> {
                                        println("   → createdAt is Double: $createdAtRaw")
                                        createdAtRaw.toLong()
                                    }
                                    is Number -> {
                                        println("   → createdAt is Number: $createdAtRaw")
                                        createdAtRaw.toLong()
                                    }
                                    else -> {
                                        // Try to extract milliseconds from Timestamp object
                                        try {
                                            // GitLive Firebase Timestamp has seconds and nanoseconds properties
                                            val timestampMap = createdAtRaw as? Map<*, *>
                                            if (timestampMap != null) {
                                                val seconds = (timestampMap["seconds"] as? Number)?.toLong() ?: 0L
                                                val nanoseconds = (timestampMap["nanoseconds"] as? Number)?.toLong() ?: 0L
                                                val millis = (seconds * 1000) + (nanoseconds / 1_000_000)
                                                println("   → createdAt is Timestamp: seconds=$seconds, nanos=$nanoseconds, millis=$millis")
                                                millis
                                            } else {
                                                println("   → createdAt is unknown type: $createdAtRaw (${createdAtRaw?.let { it::class.simpleName }})")
                                                System.currentTimeMillis()
                                            }
                                        } catch (e: Exception) {
                                            println("⚠️ Could not parse createdAt for transaction ${doc.id}: $createdAtRaw - ${e.message}")
                                            System.currentTimeMillis()
                                        }
                                    }
                                }
                                
                                val transaction = Transaction(
                                    id = id,
                                    tenantId = tenantId,
                                    propertyId = propertyId,
                                    landlordId = landlordId,
                                    amount = amount,
                                    currency = currency,
                                    status = status,
                                    paymentProvider = paymentProvider,
                                    paymentReference = paymentReference,
                                    createdAt = createdAt
                                )
                                
                                println("✅ Parsed Transaction: id=${transaction.id}, amount=$${transaction.amount}, status=${transaction.status}, createdAt=${transaction.createdAt}")
                                parsedTransactions.add(transaction)
                            } catch (e: Exception) { 
                                println("❌ Failed to parse transaction doc ${doc.id}: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        
                        val txns = parsedTransactions.sortedByDescending { it.createdAt }
                        println("💰 TenantViewModel: Setting ${txns.size} transactions in state")
                        _transactions.value = txns
                    }
            } catch (e: Exception) {
                println("❌ TenantViewModel: Fatal error in loadTransactions: ${e.message}")
                e.printStackTrace()
                _transactions.value = emptyList()
            }
        }
    }

    // ── Stop the unlocks listener (call on logout) ────────────────────────────
    fun stopListeners() {
        unlocksListenerJob?.cancel()
        unlocksListenerJob = null
        transactionsListenerJob?.cancel()
        transactionsListenerJob = null
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

    // ── Initiate payment via Cloud Function, then confirm unlock ─────────────
    // Calls the `initiatePayment` Firebase callable which:
    //   • In demo/test mode: auto-writes the unlock doc immediately and returns demoMode=true
    //   • In production: creates a PesePay checkout and the webhook writes the unlock doc
    // After calling, we poll Firestore for the unlock doc (up to 10s) so the
    // real-time listener always has a confirmed doc before we emit Success.
    fun initiateAndConfirmPayment(tenantId: String, property: Property) {
        viewModelScope.launch {
            _unlockState.value = UnlockState.Loading
            try {
                // Step 1 — call the Cloud Function
                val functions = Firebase.functions
                val callable  = functions.httpsCallable("initiatePayment")
                callable.invoke(
                    mapOf(
                        "propertyId" to property.id,
                        "landlordId" to property.landlordId,
                        "successUrl" to "rentout://payment-success",
                        "cancelUrl"  to "rentout://payment-cancel"
                    )
                )

                // Step 2 — poll Firestore until the unlock doc appears (max ~10 s)
                // In demo mode the doc is written synchronously by the function,
                // so the first check usually succeeds immediately.
                val db       = Firebase.firestore
                val unlockId = "${tenantId}_${property.id}"
                var unlocked = false
                repeat(10) { attempt ->
                    if (unlocked) return@repeat
                    val doc = db.collection("unlocks").document(unlockId).get()
                    if (doc.exists) {
                        unlocked = true
                    } else {
                        delay(1_000L) // wait 1 s before next poll
                    }
                }

                if (unlocked) {
                    // Update in-memory state; real-time listener will also confirm
                    _unlockedPropertyIds.value = _unlockedPropertyIds.value + property.id
                    val newList = _unlockedProperties.value.toMutableList()
                    if (newList.none { it.id == property.id }) newList.add(property)
                    _unlockedProperties.value = newList
                    _unlockState.value = UnlockState.Success
                } else {
                    _unlockState.value = UnlockState.Error(
                        "Payment processed but unlock is taking longer than expected. Please refresh in a moment."
                    )
                }
            } catch (e: Exception) {
                _unlockState.value = UnlockState.Error(
                    e.message?.take(120) ?: "Payment failed. Please try again."
                )
            }
        }
    }

    fun isPropertyUnlocked(propertyId: String): Boolean =
        _unlockedPropertyIds.value.contains(propertyId)

    fun resetUnlockState() { _unlockState.value = UnlockState.Idle }

    override fun onCleared() {
        super.onCleared()
        stopListeners()
    }
}