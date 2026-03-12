package org.example.project

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM messages and token refresh events.
 *
 * Two modes:
 *  1. App in foreground — data-only message → we build a local notification ourselves
 *  2. App in background / killed — FCM SDK auto-displays the `notification` payload;
 *     we only need to handle data extras on tap.
 */
class RentOutFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID          = "rentout_default"
        const val CHANNEL_NAME        = "RentOut Notifications"
        const val CHANNEL_DESCRIPTION = "Property listing updates, unlock confirmations and account alerts"

        /**
         * Call this once at app startup (MainActivity.onCreate) to ensure the
         * notification channel exists on Android 8+.
         */
        fun createNotificationChannel(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
        }

        /**
         * Upload the current FCM token to the signed-in user's Firestore document.
         * Called from MainActivity after sign-in and from onNewToken.
         */
        fun uploadToken(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { /* silent — token will sync next launch */ }
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        uploadToken(token)
    }

    // ── Message received ──────────────────────────────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Extract title / body — prefer data payload (so we get it in foreground too)
        val title   = message.data["title"]   ?: message.notification?.title   ?: "RentOut"
        val body    = message.data["message"] ?: message.notification?.body    ?: ""
        val type    = message.data["type"]    ?: ""
        val notifId = System.currentTimeMillis().toInt()

        // Build tap intent — opens app and navigates to notifications screen
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
            putExtra("notification_type", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notifId, notification)
        }
    }
}
