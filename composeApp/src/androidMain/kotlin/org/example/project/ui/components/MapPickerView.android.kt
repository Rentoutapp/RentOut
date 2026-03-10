package org.example.project.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.ui.theme.RentOutColors

@SuppressLint("MissingPermission")
@Composable
actual fun MapPickerView(
    latitude: String,
    longitude: String,
    onLocationPicked: (lat: String, lng: String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val defaultLat = -17.8252
    val defaultLng =  31.0335

    val initialLat = latitude.toDoubleOrNull() ?: defaultLat
    val initialLng = longitude.toDoubleOrNull() ?: defaultLng

    // markerPos tracks where the pin is displayed in the preview thumbnail
    var markerPos    by remember { mutableStateOf(LatLng(initialLat, initialLng)) }
    var showDialog   by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Keep markerPos in sync when the parent updates latitude/longitude externally
    LaunchedEffect(latitude, longitude) {
        val lat = latitude.toDoubleOrNull()
        val lng = longitude.toDoubleOrNull()
        if (lat != null && lng != null) markerPos = LatLng(lat, lng)
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Static tappable preview ─────────────────────────────────────────
        MapPreviewCard(
            latitude  = latitude,
            longitude = longitude,
            markerPos = markerPos,
            onClick   = { showDialog = true }
        )
    }

    // ── Full-screen animated map dialog ────────────────────────────────────
    if (showDialog) {
        MapPickerDialog(
            latitude           = latitude,
            longitude          = longitude,
            markerPos          = markerPos,
            hasPermission      = hasPermission,
            onLocationPinned   = { pos ->
                markerPos = pos
                onLocationPicked("%.6f".format(pos.latitude), "%.6f".format(pos.longitude))
            },
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showDialog = false },
            scope     = scope
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview thumbnail — non-interactive, tap to open dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapPreviewCard(
    latitude:  String,
    longitude: String,
    markerPos: LatLng,
    onClick:   () -> Unit
) {
    val pulseAnim  = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ps"
    )

    val previewCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPos, if (latitude.isNotBlank()) 14f else 11f)
    }
    LaunchedEffect(markerPos) { previewCamera.move(CameraUpdateFactory.newLatLng(markerPos)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, RentOutColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        // All gestures disabled — prevents scroll conflict with the parent form
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = previewCamera,
            properties          = MapProperties(isMyLocationEnabled = false),
            uiSettings          = MapUiSettings(
                scrollGesturesEnabled   = false,
                zoomGesturesEnabled     = false,
                tiltGesturesEnabled     = false,
                rotationGesturesEnabled = false,
                myLocationButtonEnabled = false,
                zoomControlsEnabled     = false,
                compassEnabled          = false,
                mapToolbarEnabled       = false
            )
        ) {
            if (latitude.isNotBlank()) {
                Marker(state = MarkerState(position = markerPos), title = "Property Location")
            }
        }

        // Scrim
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))

        // Centre badge — pulses when no pin, static when pinned
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(if (latitude.isBlank()) pulseScale else 1f)
        ) {
            Surface(
                shape       = RoundedCornerShape(24.dp),
                color       = if (latitude.isBlank()) RentOutColors.Primary else Color.Black.copy(alpha = 0.65f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = if (latitude.isBlank()) Icons.Default.TouchApp else Icons.Default.EditLocationAlt,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = if (latitude.isBlank()) "Tap to pick location" else "Tap to adjust pin",
                        color      = Color.White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Expand icon — top right
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "Open map",
                    tint               = Color.White,
                    modifier           = Modifier.padding(6.dp).size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full-screen animated map dialog
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
private fun MapPickerDialog(
    latitude:            String,
    longitude:           String,
    markerPos:           LatLng,
    hasPermission:       Boolean,
    onLocationPinned:    (LatLng) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss:           () -> Unit,
    scope:               kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current

    // Start with the existing pin if one was already set, otherwise use the fallback
    var dialogMarkerPos by remember { mutableStateOf(markerPos) }
    // hasPinBeenSet is true immediately if there's already a saved location
    var hasPinBeenSet   by remember { mutableStateOf(latitude.isNotBlank()) }
    var isLocating      by remember { mutableStateOf(false) }
    var locationError   by remember { mutableStateOf("") }

    // Guard: swallow map clicks for the first 450 ms so the opening tap
    // (which dismissed the preview) cannot accidentally move the pin
    var mapClicksEnabled by remember { mutableStateOf(false) }

    val dialogCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            markerPos,
            if (latitude.isNotBlank()) 15f else 5f
        )
    }

    // ── On open: enable clicks, then auto-locate + auto-pin if no pin is set ──
    LaunchedEffect(Unit) {
        delay(450)
        mapClicksEnabled = true

        if (latitude.isBlank()) {
            // No existing pin — try to locate and auto-pin the user's position
            if (hasPermission) {
                isLocating = true
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        isLocating = false
                        if (loc != null) {
                            val pos = LatLng(loc.latitude, loc.longitude)
                            // Auto-pin + fly camera — user sees their location pinned immediately
                            dialogMarkerPos = pos
                            hasPinBeenSet   = true
                            onLocationPinned(pos)
                            scope.launch {
                                dialogCamera.animate(
                                    CameraUpdateFactory.newLatLngZoom(pos, 15f),
                                    durationMs = 900
                                )
                            }
                        }
                    }
                    .addOnFailureListener { isLocating = false }
            } else {
                onRequestPermission()
            }
        }
    }

    // ── GPS FAB — re-locate and move pin to current position ─────────────────
    fun locateMe() {
        if (!hasPermission) { onRequestPermission(); return }
        isLocating    = true
        locationError = ""
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                isLocating = false
                if (loc != null) {
                    val pos = LatLng(loc.latitude, loc.longitude)
                    dialogMarkerPos = pos
                    hasPinBeenSet   = true
                    onLocationPinned(pos)
                    scope.launch {
                        dialogCamera.animate(
                            CameraUpdateFactory.newLatLngZoom(pos, 16f),
                            durationMs = 700
                        )
                    }
                } else {
                    locationError = "Could not determine location. Try again."
                }
            }
            .addOnFailureListener {
                isLocating    = false
                locationError = "GPS error: ${it.message}"
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false
        )
    ) {
        // Zoom-out spring entrance animation
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale  = 0.55f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f, animationSpec = tween(180)) +
                   fadeOut(animationSpec = tween(160))
        ) {
            Surface(
                modifier        = Modifier.fillMaxSize().padding(12.dp),
                shape           = RoundedCornerShape(24.dp),
                shadowElevation = 24.dp,
                color           = MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.fillMaxSize()) {

                    // ── Interactive map ───────────────────────────────────
                    GoogleMap(
                        modifier            = Modifier.fillMaxSize(),
                        cameraPositionState = dialogCamera,
                        properties          = MapProperties(isMyLocationEnabled = hasPermission),
                        uiSettings          = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled     = true,
                            compassEnabled          = true,
                            mapToolbarEnabled       = false
                        ),
                        onMapClick = { latLng ->
                            if (!mapClicksEnabled) return@GoogleMap
                            dialogMarkerPos = latLng
                            hasPinBeenSet   = true
                            onLocationPinned(latLng)
                        }
                    ) {
                        if (hasPinBeenSet) {
                            Marker(
                                state    = MarkerState(position = dialogMarkerPos),
                                title    = "Property Location",
                                draggable = true,
                                onInfoWindowClick = {}
                            )
                        }
                    }

                    // ── Top bar ───────────────────────────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape    = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint               = RentOutColors.Primary,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pick Property Location",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick          = onDismiss,
                                shape            = RoundedCornerShape(12.dp),
                                colors           = ButtonDefaults.buttonColors(containerColor = RentOutColors.Primary),
                                contentPadding   = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ── "Finding your location…" spinner overlay ──────────
                    AnimatedVisibility(
                        visible  = isLocating,
                        modifier = Modifier.align(Alignment.Center),
                        enter    = fadeIn(),
                        exit     = fadeOut()
                    ) {
                        Surface(
                            shape           = RoundedCornerShape(16.dp),
                            color           = Color.Black.copy(alpha = 0.65f),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(18.dp),
                                    color       = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Finding your location…", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    // ── GPS FAB ───────────────────────────────────────────
                    FloatingActionButton(
                        onClick          = { locateMe() },
                        modifier         = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 90.dp)
                            .size(52.dp),
                        shape            = CircleShape,
                        containerColor   = RentOutColors.Primary,
                        contentColor     = Color.White,
                        elevation        = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(Modifier.size(24.dp), Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, "Use my location", Modifier.size(24.dp))
                        }
                    }

                    // ── Hint banner (no pin placed yet) ───────────────────
                    AnimatedVisibility(
                        visible  = !hasPinBeenSet && !isLocating,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        enter    = fadeIn() + slideInVertically { it },
                        exit     = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.65f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tap the map or drag the marker to pin your property", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // ── Coordinate chip (shown while a pin is active) ─────
                    AnimatedVisibility(
                        visible  = hasPinBeenSet,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        enter    = fadeIn() + slideInVertically { it },
                        exit     = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(
                            shape           = RoundedCornerShape(16.dp),
                            color           = RentOutColors.Primary,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text       = "%.5f, %.5f".format(dialogMarkerPos.latitude, dialogMarkerPos.longitude),
                                    color      = Color.White,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Error snackbar ────────────────────────────────────
                    AnimatedVisibility(
                        visible  = locationError.isNotBlank(),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        enter    = fadeIn() + slideInVertically { it },
                        exit     = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(locationError, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}
