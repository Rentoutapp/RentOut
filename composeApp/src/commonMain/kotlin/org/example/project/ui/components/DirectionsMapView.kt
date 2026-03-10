package org.example.project.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific directions map.
 * Shows a Google Map with a route from the user's current location
 * to the property coordinates ([propertyLat], [propertyLng]).
 *
 * Android: Full Google Maps with polyline route + navigation intent.
 * iOS:     Opens Apple Maps / Google Maps via URL scheme.
 *
 * [onOpenNavigation] is called when the user taps "Navigate" —
 * the platform implementation launches the native navigation app.
 */
@Composable
expect fun DirectionsMapView(
    propertyLat: Double,
    propertyLng: Double,
    propertyTitle: String,
    modifier: Modifier = Modifier,
    onOpenNavigation: () -> Unit
)
