package org.example.project.ui.util

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation: shows a system Toast with SHORT duration.
 * Safe to call from a coroutine — uses the application context stored
 * in AppContext, which is initialised in MainActivity.onCreate().
 */
actual fun showToast(message: String) {
    Toast.makeText(AppContext.get(), message, Toast.LENGTH_SHORT).show()
}

/**
 * Android implementation: returns a lambda that calls Activity.finish()
 * on the Activity hosting the current Compose hierarchy.
 */
@Composable
actual fun rememberExitApp(): () -> Unit {
    val context = LocalContext.current
    return { (context as? Activity)?.finish() }
}

/**
 * Android implementation: registers a BackHandler that intercepts the system
 * back button on dashboard/home screens and calls [onBackPress] instead of
 * finishing the Activity directly.
 *
 * BackHandler is always enabled (enabled = true) because dashboards are root
 * screens with no meaningful parent to pop back to. The double-back-press
 * logic in onBackPress (App.kt) decides whether to show a toast or exit.
 */
@Composable
actual fun DashboardBackHandler(onBackPress: () -> Unit) {
    BackHandler(enabled = true, onBack = onBackPress)
}

/**
 * Application-level Context holder populated in MainActivity.onCreate().
 * Used by showToast() which may be called outside of a @Composable scope.
 */
object AppContext {
    private var appContext: android.content.Context? = null
    fun init(context: android.content.Context) { appContext = context.applicationContext }
    fun get(): android.content.Context = requireNotNull(appContext) {
        "AppContext.init() must be called in MainActivity.onCreate() before showToast()"
    }
}
