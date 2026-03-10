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
    val scope = rememberCoroutineScope()

    val defaultLat = -17.8252
    val defaultLng = 31.0335

    val initialLat = latitude.toDoubleOrNull() ?: defaultLat
    val initialLng = longitude.toDoubleOrNull() ?: defaultLng
    val initialPos = LatLng(initialLat, initialLng)

    var markerPos by remember { mutableStateOf(initialPos) }
    var showDialog by remember { mutableStateOf(false) }
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
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
    }

    // Sync markerPos when latitude/longitude props change externally
    LaunchedEffect(latitude, longitude) {
        val lat = latitude.toDoubleOrNull()
        val lng = longitude.toDoubleOrNull()
        if (lat != null && lng != null) {
            markerPos = LatLng(lat, lng)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Tappable preview thumbnail ──────────────────────────────────────
        MapPreviewCard(
            latitude = latitude,
            longitude = longitude,
            markerPos = markerPos,
            onClick = { showDialog = true }
        )

        Spacer(Modifier.height(10.dp))

        // ── Pinned coordinate chip ──────────────────────────────────────────
        AnimatedVisibility(
            visible = latitude.isNotBlank() && longitude.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RentOutColors.Primary.copy(alpha = 0.07f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(RentOutColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = RentOutColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Pinned Location",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Lat: $latitude  ·  Lng: $longitude",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RentOutColors.Primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            onLocationPicked("", "")
                            markerPos = LatLng(defaultLat, defaultLng)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Clear location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Full-screen map dialog ──────────────────────────────────────────────
    if (showDialog) {
        MapPickerDialog(
            latitude = latitude,
            longitude = longitude,
            markerPos = markerPos,
            hasPermission = hasPermission,
            onMarkerMoved = { pos ->
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
            scope = scope
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview thumbnail card (non-interactive, tap to open dialog)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapPreviewCard(
    latitude: String,
    longitude: String,
    markerPos: LatLng,
    onClick: () -> Unit
) {
    // Pulse animation for the "tap to open" badge when no pin is set
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val previewCameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            markerPos,
            if (latitude.isNotBlank()) 14f else 11f
        )
    }

    // Keep preview camera centred on marker when it changes
    LaunchedEffect(markerPos) {
        previewCameraState.move(CameraUpdateFactory.newLatLng(markerPos))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.5.dp,
                color = RentOutColors.Primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Static non-interactive map — all gestures disabled so it never fights the scroll
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = previewCameraState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            if (latitude.isNotBlank()) {
                Marker(
                    state = MarkerState(position = markerPos),
                    title = "Property Location"
                )
            }
        }

        // Dark scrim so the badge is readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
        )

        // Centre badge — pulses when no pin, static when pinned
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(if (latitude.isBlank()) pulseScale else 1f)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (latitude.isBlank()) RentOutColors.Primary else Color.Black.copy(alpha = 0.65f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (latitude.isBlank()) Icons.Default.TouchApp else Icons.Default.EditLocationAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (latitude.isBlank()) "Tap to pick location" else "Tap to adjust pin",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Expand icon — top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "Open map",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
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
    latitude: String,
    longitude: String,
    markerPos: LatLng,
    hasPermission: Boolean,
    onMarkerMoved: (LatLng) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current

    var dialogMarkerPos by remember { mutableStateOf(markerPos) }
    var hasPinBeenSet by remember { mutableStateOf(latitude.isNotBlank()) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf("") }

    // ── Guard: ignore map clicks for the first 400 ms after the dialog opens.
    // The tap that opened the dialog can propagate through to onMapClick;
    // this window swallows it so the pin is never placed by the opening tap.
    var mapClicksEnabled by remember { mutableStateOf(false) }

    val dialogCameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            markerPos,
            if (latitude.isNotBlank()) 15f else 5f   // wide zoom until we fly to user
        )
    }

    // On first open: enable click guard after delay, then auto-locate if no pin set
    LaunchedEffect(Unit) {
        // 1. Wait for the dialog open animation + touch-up before enabling map clicks
        delay(450)
        mapClicksEnabled = true

        // 2. If no pin is set yet, fly to the user's current location
        if (latitude.isBlank()) {
            if (hasPermission) {
                isLocating = true
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        isLocating = false
                        if (loc != null) {
                            val pos = LatLng(loc.latitude, loc.longitude)
                            // Move camera to user location but do NOT set/move the pin yet
                            scope.launch {
                                dialogCameraState.animate(
                                    CameraUpdateFactory.newLatLngZoom(pos, 15f),
                                    durationMs = 900
                                )
                            }
                        }
                    }
                    .addOnFailureListener {
                        isLocating = false
                    }
            } else {
                onRequestPermission()
            }
        }
    }

    // GPS locate button function — moves camera AND sets the pin
    fun locateMe() {
        if (!hasPermission) { onRequestPermission(); return }
        isLocating = true
        locationError = ""
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                isLocating = false
                if (loc != null) {
                    val pos = LatLng(loc.latitude, loc.longitude)
                    dialogMarkerPos = pos
                    hasPinBeenSet = true
                    onMarkerMoved(pos)
                    scope.launch {
                        dialogCameraState.animate(
                            CameraUpdateFactory.newLatLngZoom(pos, 16f),
                            durationMs = 700
                        )
                    }
                } else {
                    locationError = "Could not determine location. Try again."
                }
            }
            .addOnFailureListener {
                isLocating = false
                locationError = "GPS error: ${it.message}"
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Zoom-out entrance animation
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.55f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f, animationSpec = tween(180)) +
                    fadeOut(animationSpec = tween(160))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 24.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Interactive full-screen map
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = dialogCameraState,
                        properties = MapProperties(isMyLocationEnabled = hasPermission),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            mapToolbarEnabled = false
                        ),
                        onMapClick = { latLng ->
                            // Ignore taps during the opening guard window
                            if (!mapClicksEnabled) return@GoogleMap
                            dialogMarkerPos = latLng
                            hasPinBeenSet = true
                            onMarkerMoved(latLng)
                        }
                    ) {
                        // Only show marker once the user has actually pinned a location
                        if (hasPinBeenSet) {
                            Marker(
                                state = MarkerState(position = dialogMarkerPos),
                                title = "Property Location",
                                draggable = true,
                                onInfoWindowClick = {}
                            )
                        }
                    }

                    // ── Top bar ──────────────────────────────────────────
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = RentOutColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pick Property Location",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RentOutColors.Primary
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ── Locating spinner overlay ──────────────────────────
                    AnimatedVisibility(
                        visible = isLocating,
                        modifier = Modifier.align(Alignment.Center),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Finding your location…",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // ── GPS FAB ──────────────────────────────────────────
                    FloatingActionButton(
                        onClick = { locateMe() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 90.dp)
                            .size(52.dp),
                        shape = CircleShape,
                        containerColor = RentOutColors.Primary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Use my location",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // ── Hint banner ───────────────────────────────────────
                    AnimatedVisibility(
                        visible = !hasPinBeenSet && !isLocating,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Tap the map or drag the marker to pin your property",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // ── Error snackbar ────────────────────────────────────
                    AnimatedVisibility(
                        visible = locationError.isNotBlank(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    locationError,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
