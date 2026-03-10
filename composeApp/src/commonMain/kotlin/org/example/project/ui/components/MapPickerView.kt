package org.example.project.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific interactive map picker.
 * Android: full Google Maps with draggable marker + GPS button.
 * iOS: coordinate text-input fallback.
 *
 * @param latitude  Current latitude string (empty = not set).
 * @param longitude Current longitude string (empty = not set).
 * @param onLocationPicked Called with (lat, lng) as strings when user picks a location.
 * @param modifier  Modifier for the map container.
 */
@Composable
expect fun MapPickerView(
    latitude: String,
    longitude: String,
    onLocationPicked: (lat: String, lng: String) -> Unit,
    modifier: Modifier = Modifier
)
