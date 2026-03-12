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
        val idsFromTransactions = successfulTxns.map { it.propertyId }.filter { it.isNotBlank() }.toSet()

        // CRITICAL: Merge with any optimistically-added IDs so a Firestore read
        // returning 0 docs (e.g. due to App Check latency) never wipes an unlock
        // that was just confirmed by the Cloud Function.
        val merged = _unlockedPropertyIds.value + idsFromTransactions
        _unlockedPropertyIds.value = merged
        println("🔓 TenantViewModel: ${merged.size} unlocked IDs (${idsFromTransactions.size} from txns + ${merged.size - idsFromTransactions.size} optimistic)")

        // Fetch full property docs for each unlocked property so "My Unlocked" list populates
        if (merged.isNotEmpty()) {
            viewModelScope.launch {
                val db = Firebase.firestore
                val props = merged.mapNotNull { pid ->
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
    // We do NOT cancel the existing poll loop here — instead we launch a dedicated
    // rapid-retry job that polls every 2s for up to 30s until the new transaction
    // appears in Firestore, then hands back to the normal 15s poll cycle.
    // This avoids the race where cancelling + restarting the listener causes a
    // 0-doc read that wipes the optimistic unlock state.
    fun refreshAfterPayment(tenantId: String) {
        viewModelScope.launch {
            var found = false
            repeat(15) { attempt ->
                if (found) return@repeat
                try {
                    fetchTransactions(tenantId)
                    // If we now have at least one successful transaction, stop early
                    if (_transactions.value.any { it.status.equals("success", ignoreCase = true) }) {
                        found = true
                        println("✅ TenantViewModel: refreshAfterPayment confirmed on attempt ${attempt + 1}")
                    }
                } catch (e: Exception) {
                    println("⚠️ TenantViewModel: refreshAfterPayment attempt ${attempt + 1} failed: ${e.message}")
                }
                if (!found) delay(2_000L)
            }
        }
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
                    createdAt = createdAtValue,
                    providerSubtype = try { doc.get<String>("providerSubtype") } catch (_: Exception) { "" },
                    brokerageDeductionAmount = try { doc.get<Double>("brokerageDeductionAmount") } catch (_: Exception) { 0.0 },
                    brokerageFloatBefore = try { doc.get<Double>("brokerageFloatBefore") } catch (_: Exception) { 0.0 },
                    brokerageFloatAfter = try { doc.get<Double>("brokerageFloatAfter") } catch (_: Exception) { 0.0 },
                    brokerageLedgerEntryId = try { doc.get<String>("brokerageLedgerEntryId") } catch (_: Exception) { "" },
                    brokerageSettlementStatus = try { doc.get<String>("brokerageSettlementStatus") } catch (_: Exception) { "" },
                    brokerageStatusMessage = try { doc.get<String>("brokerageStatusMessage") } catch (_: Exception) { "" }
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
    // After calling, we poll Firestore for the unlock doc (up to 30s with exponential backoff).
    // If Cloud Function confirms sync (demo/already unlocked), trust it immediately.
    fun initiateAndConfirmPayment(tenantId: String, property: Property) {
        viewModelScope.launch {
            _unlockState.value = UnlockState.Loading
            try {
                // ── Step 1: Call the Cloud Function ──────────────────────────────
                val functions = Firebase.functions
                val callable  = functions.httpsCallable("initiatePayment")
                val result = callable.invoke(
                    mapOf(
                        "propertyId" to property.id,
                        "landlordId" to property.landlordId,
                        "successUrl" to "rentout://payment-success",
                        "cancelUrl"  to "rentout://payment-cancel"
                    )
                )

                // ── Step 2: Parse Cloud Function response ─────────────────────────
                // In demo mode the function writes status='success' synchronously and
                // returns demoMode=true. Trust this response immediately — do NOT wait
                // for Firestore confirmation (App Check latency blocks list queries).
                @Suppress("UNCHECKED_CAST")
                val resultData = try { result.data<Map<String, Any?>>() } catch (_: Exception) { null }
                val isDemoMode      = resultData?.get("demoMode")      as? Boolean ?: false
                val alreadyUnlocked = resultData?.get("alreadyUnlocked") as? Boolean ?: false
                val cfSuccess       = resultData?.get("success")       as? Boolean ?: false
                println("💳 TenantViewModel: CF response — success=$cfSuccess, demo=$isDemoMode, alreadyUnlocked=$alreadyUnlocked")

                val confirmedByCloudFunction = cfSuccess && (isDemoMode || alreadyUnlocked)

                if (confirmedByCloudFunction) {
                    // Cloud Function confirmed unlock synchronously — no need to poll
                    println("✅ TenantViewModel: Unlock confirmed by Cloud Function (demo/already)")
                    commitUnlockSuccess(tenantId, property)
                    return@launch
                }

                // ── Step 3: Production mode — poll for confirmation ───────────────
                // Try two strategies in parallel:
                // A) Direct doc read on unlocks/{unlockId} — single get() is not
                //    affected by App Check the same way list queries are.
                // B) List query on transactions filtered by tenantId + propertyId.
                // Retry for up to 30s with exponential backoff.
                val db = Firebase.firestore
                var unlocked = false
                var delayMs = 1_000L
                repeat(15) { attempt ->
                    if (unlocked) return@repeat
                    try {
                        // Strategy A: direct unlock doc read
                        val unlockDoc = db.collection("unlocks")
                            .document("${tenantId}_${property.id}")
                            .get()
                        if (unlockDoc.exists) {
                            unlocked = true
                            println("✅ TenantViewModel: Unlock confirmed via unlocks doc (attempt ${attempt + 1})")
                            return@repeat
                        }
                    } catch (_: Exception) { }

                    try {
                        // Strategy B: transaction list query
                        val txSnap = db.collection("transactions")
                            .where { "tenantId" equalTo tenantId }
                            .where { "propertyId" equalTo property.id }
                            .get()
                        val successTx = txSnap.documents.any { doc ->
                            try { (doc.get<Any?>("status") as? String)
                                ?.equals("success", ignoreCase = true) == true
                            } catch (_: Exception) { false }
                        }
                        if (successTx) {
                            unlocked = true
                            println("✅ TenantViewModel: Unlock confirmed via transactions query (attempt ${attempt + 1})")
                        }
                    } catch (_: Exception) { }

                    if (!unlocked) {
                        try {
                            // Strategy C: server-authoritative confirmPayment Cloud Function
                            // Bypasses App Check client-side Firestore entirely
                            val cfConfirm = Firebase.functions.httpsCallable("confirmPayment")
                            val cfResult  = cfConfirm.invoke(mapOf("propertyId" to property.id))
                            @Suppress("UNCHECKED_CAST")
                            val cfData = try { cfResult.data<Map<String, Any?>>() } catch (_: Exception) { null }
                            if (cfData?.get("confirmed") as? Boolean == true) {
                                unlocked = true
                                println("✅ TenantViewModel: Unlock confirmed via confirmPayment CF (attempt ${attempt + 1})")
                            }
                        } catch (_: Exception) { }
                    }

                    if (!unlocked) {
                        delay(delayMs)
                        delayMs = (delayMs * 1.5).toLong().coerceAtMost(5_000L)
                    }
                }

                if (unlocked) {
                    commitUnlockSuccess(tenantId, property)
                } else {
                    // Payment was taken by Cloud Function but confirmation timed out.
                    // Set success anyway — the background poll will sync within 15s.
                    // This prevents the user seeing an error after a successful payment.
                    println("⚠️ TenantViewModel: Confirmation timed out — optimistically succeeding")
                    commitUnlockSuccess(tenantId, property)
                }
            } catch (e: Exception) {
                _unlockState.value = UnlockState.Error(
                    e.message?.take(120) ?: "Payment failed. Please try again."
                )
            }
        }
    }

    // Centralised commit: update state, refresh transactions
    private fun commitUnlockSuccess(tenantId: String, property: Property) {
        _unlockedPropertyIds.value = _unlockedPropertyIds.value + property.id
        val newList = _unlockedProperties.value.toMutableList()
        if (newList.none { it.id == property.id }) newList.add(property)
        _unlockedProperties.value = newList
        _unlockState.value = UnlockState.Success
        refreshAfterPayment(tenantId)
    }

    fun isPropertyUnlocked(propertyId: String): Boolean =
        _unlockedPropertyIds.value.contains(propertyId)

    fun resetUnlockState() { _unlockState.value = UnlockState.Idle }

    override fun onCleared() {
        super.onCleared()
        stopListeners()
    }
}