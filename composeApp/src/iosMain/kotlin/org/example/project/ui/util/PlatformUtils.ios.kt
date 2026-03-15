package org.example.project.ui.util

import androidx.compose.runtime.Composable

/**
 * iOS implementation: no-op.
 * iOS has no system Toast equivalent.
 */
actual fun showToast(message: String) { /* no-op on iOS */ }

/**
 * iOS implementation: no-op.
 * iOS apps must not terminate programmatically per Apple's HIG.
 */
@Composable
actual fun rememberExitApp(): () -> Unit = { /* no-op on iOS */ }

/**
 * iOS implementation: no-op.
 * iOS uses swipe-back gestures for navigation; there is no hardware back
 * button to intercept. Dashboard screens on iOS have no back affordance,
 * which is consistent with iOS UX conventions.
 */
@Composable
actual fun DashboardBackHandler(onBackPress: () -> Unit) { /* no-op on iOS */ }
