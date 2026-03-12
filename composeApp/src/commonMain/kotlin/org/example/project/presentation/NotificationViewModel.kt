package org.example.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.example.project.data.model.AppNotification

sealed class NotificationListState {
    object Loading  : NotificationListState()
    object Empty    : NotificationListState()
    data class Success(val notifications: List<AppNotification>) : NotificationListState()
    data class Error(val message: String) : NotificationListState()
}

class NotificationViewModel : ViewModel() {

    private val _state = MutableStateFlow<NotificationListState>(NotificationListState.Loading)
    val state: StateFlow<NotificationListState> = _state.asStateFlow()

    /** Number of unread notifications — drives the badge on the bell icon. */
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var listenerJob: Job? = null
    private var currentUid: String? = null

    /** Exposes the UID currently being listened to — used by App.kt to avoid
     *  redundant listener restarts when authState and currentUser?.uid both
     *  fire on the same user during the splash → dashboard transition. */
    val currentListeningUid: String? get() = currentUid

    // ── Start real-time listener for the given user ──────────────────────────
    fun startListening(uid: String) {
        if (uid == currentUid && listenerJob?.isActive == true) return
        currentUid = uid
        listenerJob?.cancel()
        listenerJob = viewModelScope.launch {
            _state.value = NotificationListState.Loading
            Firebase.firestore
                .collection("notifications")
                .where { "recipientId" equalTo uid }
                .snapshots
                .catch { e ->
                    _state.value = NotificationListState.Error(
                        e.message ?: "Failed to load notifications"
                    )
                }
                .collect { snapshot ->
                    val notifications = snapshot.documents
                        .map { doc -> doc.data(AppNotification.serializer()).copy(id = doc.id) }
                        .sortedByDescending { it.createdAt }

                    _state.value = if (notifications.isEmpty())
                        NotificationListState.Empty
                    else
                        NotificationListState.Success(notifications)

                    _unreadCount.value = notifications.count { !it.isRead }
                }
        }
    }

    // ── Mark a single notification as read ───────────────────────────────────
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Firebase.firestore
                    .collection("notifications")
                    .document(notificationId)
                    .update("isRead" to true)
            } catch (_: Exception) { }
        }
    }

    // ── Mark ALL notifications as read ───────────────────────────────────────
    fun markAllAsRead() {
        val currentState = _state.value
        if (currentState !is NotificationListState.Success) return
        val unread = currentState.notifications.filter { !it.isRead }
        viewModelScope.launch {
            unread.forEach { notif ->
                try {
                    Firebase.firestore
                        .collection("notifications")
                        .document(notif.id)
                        .update("isRead" to true)
                } catch (_: Exception) { }
            }
        }
    }

    // ── Delete a single notification ─────────────────────────────────────────
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                Firebase.firestore
                    .collection("notifications")
                    .document(notificationId)
                    .delete()
            } catch (_: Exception) { }
        }
    }

    // ── Stop listener (called on logout) ─────────────────────────────────────
    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        currentUid = null
        _state.value = NotificationListState.Loading
        _unreadCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
