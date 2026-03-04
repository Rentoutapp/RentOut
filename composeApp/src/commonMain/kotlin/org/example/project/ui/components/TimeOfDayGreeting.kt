package org.example.project.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a time-of-day period with its greeting and a matching
 * Material icon that renders crisp at any density.
 *
 * Hour boundaries follow widely accepted UX conventions:
 *   Morning   05:00 – 11:59  "Good morning"   WbSunny
 *   Afternoon 12:00 – 16:59  "Good afternoon" Brightness5
 *   Evening   17:00 – 20:59  "Good evening"   WbTwilight
 *   Night     21:00 – 04:59  "Good night"     Brightness3 (crescent moon)
 */
enum class TimeOfDay(val greeting: String, val icon: ImageVector) {
    MORNING  ("Good morning",   Icons.Default.WbSunny),
    AFTERNOON("Good afternoon", Icons.Default.Brightness5),
    EVENING  ("Good evening",   Icons.Default.WbTwilight),
    NIGHT    ("Good night",     Icons.Default.Brightness3)
}

/**
 * Derives the current [TimeOfDay] from the device clock using
 * the platform-specific [getCurrentHour] expect function.
 */
fun getTimeOfDay(): TimeOfDay {
    val hour = getCurrentHour()
    return when (hour) {
        in 5..11  -> TimeOfDay.MORNING
        in 12..16 -> TimeOfDay.AFTERNOON
        in 17..20 -> TimeOfDay.EVENING
        else      -> TimeOfDay.NIGHT   // 21–23 and 0–4
    }
}

/**
 * Extracts the first name from a full name string.
 * "Walter Doe" → "Walter"
 */
fun firstNameOf(fullName: String): String =
    fullName.trim().split(" ").firstOrNull { it.isNotBlank() } ?: fullName.trim()

/**
 * Returns the greeting label and the matching [TimeOfDay] for the
 * current hour. Separating the two lets callers render them independently
 * (e.g. greeting on one row, name on the next).
 */
data class GreetingInfo(
    val greeting: String,        // e.g. "Good night"
    val firstName: String,       // e.g. "Walter"
    val timeOfDay: TimeOfDay     // carries the icon
)

fun buildGreetingInfo(name: String): GreetingInfo {
    val tod = getTimeOfDay()
    return GreetingInfo(
        greeting  = tod.greeting,
        firstName = firstNameOf(name),
        timeOfDay = tod
    )
}

/**
 * Composable helper that memoises the [GreetingInfo] for the lifetime
 * of the composition. Recomputes only when [name] changes.
 */
@Composable
fun rememberGreetingInfo(name: String): GreetingInfo =
    remember(name) { buildGreetingInfo(name) }

// ── Legacy compat — kept so TenantHomeScreen still compiles ──────────────────
@Composable
fun rememberGreeting(name: String): Pair<String, String> {
    val info = rememberGreetingInfo(name)
    return info.greeting to info.timeOfDay.name  // emoji-free; callers should migrate to rememberGreetingInfo
}
