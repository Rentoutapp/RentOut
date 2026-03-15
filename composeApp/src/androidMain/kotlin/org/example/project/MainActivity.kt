package org.example.project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.messaging.FirebaseMessaging
import org.example.project.ui.util.AppContext

class MainActivity : ComponentActivity() {

    // ── Multi-permission launcher: requests CAMERA + notifications together ───
    // Using RequestMultiplePermissions so that on first launch the user sees
    // both prompts in sequence rather than being asked one at a time across
    // different sessions. CAMERA is declared in the manifest so it must also
    // be granted at runtime on API 23+ (Android 6.0+).
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Camera: if denied, the ImagePicker gracefully shows a toast when the
        // user tries to use the camera — they can still use gallery upload.
        // Notifications: FCM still delivers data messages even if denied on API 33+.
        // No blocking behaviour — both are handled gracefully by the app.
        @Suppress("UNUSED_VARIABLE")
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // Required for imePadding() to correctly inset content above the keyboard
        // on all API levels when edge-to-edge is enabled.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Initialise the application-level Context holder so showToast() can
        // obtain a Context from outside of a @Composable scope (e.g. from
        // a coroutine launched by the double-back-press exit handler).
        AppContext.init(this)

        // Create the notification channel (safe to call multiple times)
        RentOutFirebaseMessagingService.createNotificationChannel(this)

        // ── Request runtime permissions on first launch ───────────────────────
        // Build the list of permissions that have not yet been granted.
        // Requesting only what is needed avoids showing unnecessary dialogs.
        val permissionsToRequest = buildList {
            // CAMERA — required for taking property photos (API 23+).
            // Prompting at startup lets users understand the feature exists
            // before they navigate to the Edit Photos screen.
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }

            // POST_NOTIFICATIONS — required on Android 13+ (API 33+) for FCM
            // foreground notifications (data messages still arrive without it).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Fetch current FCM token and upload it to Firestore so Cloud Functions can
        // target this device for push notifications.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            RentOutFirebaseMessagingService.uploadToken(token)
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
