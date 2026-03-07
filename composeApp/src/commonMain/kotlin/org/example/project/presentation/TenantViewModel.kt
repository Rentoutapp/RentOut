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
import kotlinx.datetime.Clock
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

    private val _transactionsLoading = MutableStateFlow(false)
    val transactionsLoading: StateFlow<Boolean> = _transactionsLoading.asStateFlow()

    // ── Real-time listener job for unlocks ────────────────────────────────────
    private var unlocksListenerJob: Job? = null
    private var transactionsListenerJob: Job? = null

    // ═══ Load unlocked properties derived from successful transactions ═══════
    //
    // ROOT CAUSE FIX (2026-03-07):
    // Querying the `unlocks` collection always returned 0 docs despite the docs
    // existing in Firestore. The logs confirmed:
    //   "TenantViewModel: 0 unlocked property IDs loaded"  ← every single poll
    // Two compounding causes:
    //   1. App Check errors ("No AppCheckProvider installed") silently blocked
    //      collection list queries on `unlocks` on Android.
    //   2. doc.get<Any?>("propertyId") on gitlive QueryDocumentSnapshot in a
    //      collection query result behaves differently from a direct doc read.
    //
    // THE FIX: Stop querying `unlocks` entirely from the client.
    // Derive `unlockedPropertyIds` directly from the `transactions` StateFlow,
    // which already works perfectly (6 docs returned every poll).
    // A transaction with status="success" IS the proof of unlock — the `unlocks`
    // collection is only needed server-side for the Cloud Function idempotency check.
    //
    // After transactions are loaded, we call this to sync the unlock state.
    private fun syncUnlockStateFromTransactions() {
        val successfulTxns = _transactions.value.filter {
            it.status.equals("success", ignoreCase = true)
        }
        val ids = successfulTxns.map { it.propertyId }.filter { it.isNotBlank() }.toSet()
        _unlockedPropertyIds.value = ids
        println("🔓 TenantViewModel: ${ids.size} unlocked IDs derived from ${successfulTxns.size} successful transactions")

        // Fetch full property docs for each unlocked property so "My Unlocked" list populates
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                val db = Firebase.firestore
                val props = ids.mapNotNull { pid ->
                    try {
                        val doc = db.collection("properties").document(pid).get()
                        if (doc.exists) doc.data(Property.serializer()) else null
                    } catch (_: Exception) { null }
                }
                _unlockedProperties.value = props
                println("🏠 TenantViewModel: ${props.size} unlocked property docs fetched")
            }
        } else {
            _unlockedProperties.value = emptyList()
        }
    }

    // Keep loadUnlockedProperties as a no-op stub so App.kt call sites don't break.
    // All unlock state is now derived from transactions — see syncUnlockStateFromTransactions().
    fun loadUnlockedProperties(tenantId: String) {
        // Sync immediately from whatever transactions are already loaded
        syncUnlockStateFromTransactions()
    }

    // ═══ Immediately refresh both unlocks AND transactions after payment ══════
    fun refreshAfterPayment(tenantId: String) {
        transactionsListenerJob?.cancel()
        loadTransactions(tenantId)
        // syncUnlockStateFromTransactions() is called inside loadTransactions
        // after the new transaction list is emitted
    }

    // ═══ Load payment transactions for this tenant ════════════════════════════════
    // Direct profile navigation is a sensitive path: if the first fetch hits an auth
    // timing or network startup issue, waiting 30 seconds before retrying makes the
    // payment history look permanently empty. We therefore fetch immediately, expose
    // loading state to the UI, and retry quickly until the first successful load.
    fun loadTransactions(tenantId: String) {
        println("🔄 TenantViewModel: loadTransactions called for tenantId=$tenantId")
        transactionsListenerJob?.cancel()
        transactionsListenerJob = viewModelScope.launch {
            var hasLoadedSuccessfully = false
            while (true) {
                val nextDelay = try {
                    fetchTransactions(tenantId)
                    hasLoadedSuccessfully = true
                    15_000L
                } catch (e: Exception) {
                    println("❌ TenantViewModel: loadTransactions error: ${e.message}")
                    e.printStackTrace()
                    _transactionsLoading.value = false
                    if (hasLoadedSuccessfully) 10_000L else 2_000L
                }
                delay(nextDelay)
            }
        }
    }

    fun refreshTransactions(tenantId: String) {
        println("🔁 TenantViewModel: refreshTransactions requested for tenantId=$tenantId")
        loadTransactions(tenantId)
    }

    private suspend fun fetchTransactions(tenantId: String) {
        _transactionsLoading.value = true

        val snapshot = Firebase.firestore
            .collection("transactions")
            .where { "tenantId" equalTo tenantId }
            .get()

        println("📸 TenantViewModel: Got ${snapshot.documents.size} docs from Firestore for tenantId=$tenantId")

        val parsedTransactions = snapshot.documents.mapNotNull { doc ->
            try {
                val tenantIdValue = try { doc.get<String>("tenantId") } catch (_: Exception) { "" }
                val propertyIdValue = try { doc.get<String>("propertyId") } catch (_: Exception) { "" }
                val landlordIdValue = try { doc.get<String>("landlordId") } catch (_: Exception) { "" }
                val amountValue = try {
                    doc.get<Double>("amount")
                } catch (_: Exception) {
                    try { doc.get<Long>("amount").toDouble() } catch (_: Exception) {
                        try { doc.get<Int>("amount").toDouble() } catch (_: Exception) { 10.0 }
                    }
                }
                val currencyValue = try { doc.get<String>("currency") } catch (_: Exception) { "USD" }
                val statusValue = try { doc.get<String>("status") } catch (_: Exception) { "pending" }
                val providerValue = try { doc.get<String>("paymentProvider") } catch (_: Exception) { "pesepay" }
                val referenceValue = try { doc.get<String>("paymentReference") } catch (_: Exception) { "" }
                val createdAtValue = try {
                    doc.get<Long>("createdAt")
                } catch (_: Exception) {
                    try { doc.get<Double>("createdAt").toLong() } catch (_: Exception) {
                        try { doc.get<Int>("createdAt").toLong() } catch (_: Exception) {
                            try { doc.get<String>("createdAt").toLongOrNull() ?: Clock.System.now().toEpochMilliseconds() } catch (_: Exception) {
                                Clock.System.now().toEpochMilliseconds()
                            }
                        }
                    }
                }

                val transaction = Transaction(
                    id = doc.id,
                    tenantId = tenantIdValue,
                    propertyId = propertyIdValue,
                    landlordId = landlordIdValue,
                    amount = amountValue,
                    currency = currencyValue,
                    status = statusValue,
                    paymentProvider = providerValue,
                    paymentReference = referenceValue,
                    createdAt = createdAtValue
                )
                println(
                    "✅ Parsed tx ${transaction.id}: \$${transaction.amount} ${transaction.currency} [${transaction.status}] createdAt=${transaction.createdAt}"
                )
                transaction
            } catch (e: Exception) {
                println("❌ Failed to parse tx doc ${doc.id}: ${e.message}")
                null
            }
        }

        val propertyMap = buildPropertyMap(parsedTransactions.map { it.propertyId })
        val enrichedTransactions = parsedTransactions.map { transaction ->
            val property = propertyMap[transaction.propertyId]
            if (property != null) {
                transaction.copy(
                    propertyTitle = property.title,
                    propertyLocation = property.location,
                    propertyCity = property.city,
                    propertyType = property.propertyType,
                    propertyImageUrl = property.imageUrls.firstOrNull().takeUnless { it.isNullOrBlank() }
                        ?: property.imageUrl,
                    propertyRooms = property.rooms
                )
            } else transaction
        }

        val sorted = enrichedTransactions.sortedByDescending { it.createdAt }
        println("🎯 TenantViewModel: Emitting ${sorted.size} transactions to UI")
        _transactions.value = sorted
        _transactionsLoading.value = false
        // Derive unlock state from successful transactions - single source of truth
        syncUnlockStateFromTransactions()
    }

    private suspend fun buildPropertyMap(propertyIds: List<String>): Map<String, Property> {
        val uniqueIds = propertyIds.filter { it.isNotBlank() }.distinct()
        if (uniqueIds.isEmpty()) return emptyMap()

        val db = Firebase.firestore
        return uniqueIds.associateWith { propertyId ->
            runCatching {
                val doc = db.collection("properties").document(propertyId).get()
                if (doc.exists) doc.data(Property.serializer()) else null
            }.getOrNull()
        }.mapNotNull { (id, property) -> property?.let { id to it } }.toMap()
    }

    // ── Stop the unlocks listener (call on logout) ────────────────────────────
    fun stopListeners() {
        unlocksListenerJob?.cancel()
        unlocksListenerJob = null
        transactionsListenerJob?.cancel()
        transactionsListenerJob = null
        _transactionsLoading.value = false
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

                // Step 2 – poll the transactions collection for this property until
                // status = "success" (max 10 s). The Cloud Function writes status="success"
                // synchronously in demo mode so the first poll usually succeeds immediately.
                // We use transactions (not the unlocks collection) because list queries on
                // unlocks are blocked by App Check interference on Android.
                val db = Firebase.firestore
                var unlocked = false
                repeat(10) {
                    if (unlocked) return@repeat
                    try {
                        val txSnap = db.collection("transactions")
                            .where { "tenantId" equalTo tenantId }
                            .where { "propertyId" equalTo property.id }
                            .get()
                        val successTx = txSnap.documents.any { doc ->
                            try { (doc.get<Any?>("status") as? String)?.equals("success", ignoreCase = true) == true }
                            catch (_: Exception) { false }
                        }
                        if (successTx) unlocked = true
                    } catch (_: Exception) { }
                    if (!unlocked) delay(1_000L)
                }

                if (unlocked) {
                    // Optimistically add to in-memory set immediately
                    _unlockedPropertyIds.value = _unlockedPropertyIds.value + property.id
                    val newList = _unlockedProperties.value.toMutableList()
                    if (newList.none { it.id == property.id }) newList.add(property)
                    _unlockedProperties.value = newList
                    _unlockState.value = UnlockState.Success
                    // Re-fetch full transaction list and re-sync unlock state
                    // so dashboard + profile stats update immediately
                    refreshAfterPayment(tenantId)
                } else {
                    _unlockState.value = UnlockState.Error(
                        "Payment processed but unlock confirmation is taking longer than expected. Please refresh."
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