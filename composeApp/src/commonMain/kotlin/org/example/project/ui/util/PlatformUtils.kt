package org.example.project.ui.util

import androidx.compose.runtime.Composable

/**
 * Shows a short platform-native toast/snackbar message.
 * Android: android.widget.Toast (SHORT duration)
 * iOS:     no-op (iOS has no system toast)
 */
expect fun showToast(message: String)

/**
 * Exits / terminates the application process cleanly.
 * Android: calls Activity.finish() on the current Activity.
 * iOS:     no-op (iOS apps are not terminated programmatically per HIG).
 */
@Composable
expect fun rememberExitApp(): () -> Unit

/**
 * Registers a platform back-press handler for dashboard/home screens.
 *
 * Android: uses androidx.activity.compose.BackHandler to intercept the system
 *          back button and call [onBackPress] instead of finishing the Activity.
 * iOS:     no-op — iOS navigation uses swipe gestures, not a hardware back button,
 *          so no interception is needed or appropriate.
 *
 * This is an expect/actual so that the Android-only BackHandler import stays
 * out of commonMain (where it would cause an iOS compilation error).
 */
@Composable
expect fun DashboardBackHandler(onBackPress: () -> Unit)
