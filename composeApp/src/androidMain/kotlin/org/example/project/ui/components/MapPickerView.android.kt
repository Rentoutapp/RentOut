package org.example.project.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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

    // Default to Harare, Zimbabwe if no coordinates set
    val defaultLat = -17.8252
    val defaultLng = 31.0335

    val initialLat = latitude.toDoubleOrNull() ?: defaultLat
    val initialLng = longitude.toDoubleOrNull() ?: defaultLng
    val initialPos = LatLng(initialLat, initialLng)

    var markerPos by remember { mutableStateOf(initialPos) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf("") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, if (latitude.isNotBlank()) 15f else 12f)
    }

    // Permission launcher — after grant, immediately locate if no pin set yet
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
    }

    // Auto-locate on first open when no pin is set yet
    LaunchedEffect(Unit) {
        if (latitude.isBlank()) {
            if (hasPermission) {
                // Permission already granted — go straight to GPS
                isLocating = true
                locationError = ""
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        isLocating = false
                        if (loc != null) {
                            val pos = LatLng(loc.latitude, loc.longitude)
                            markerPos = pos
                            onLocationPicked(
                                "%.6f".format(loc.latitude),
                                "%.6f".format(loc.longitude)
                            )
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(pos, 16f),
                                    durationMs = 800
                                )
                            }
                        }
                    }
                    .addOnFailureListener {
                        isLocating = false
                        // Silent fail — user can tap GPS button manually
                    }
            } else {
                // No permission yet — request it; locateMe() will be called via the GPS button after grant
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // Auto-locate after permission is freshly granted and no pin is set
    LaunchedEffect(hasPermission) {
        if (hasPermission && latitude.isBlank()) {
            isLocating = true
            locationError = ""
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    isLocating = false
                    if (loc != null) {
                        val pos = LatLng(loc.latitude, loc.longitude)
                        markerPos = pos
                        onLocationPicked(
                            "%.6f".format(loc.latitude),
                            "%.6f".format(loc.longitude)
                        )
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(pos, 16f),
                                durationMs = 800
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    isLocating = false
                }
        }
    }

    // GPS locate function
    fun locateMe() {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        isLocating = true
        locationError = ""
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                isLocating = false
                if (loc != null) {
                    val pos = LatLng(loc.latitude, loc.longitude)
                    markerPos = pos
                    onLocationPicked(
                        "%.6f".format(loc.latitude),
                        "%.6f".format(loc.longitude)
                    )
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(pos, 16f),
                            durationMs = 800
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

    Column(modifier = modifier.fillMaxWidth()) {

        // Nested scroll interceptor — consumes all scroll so parent column doesn't move
        val mapScrollConsumer = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    available // consume everything — map handles its own panning
                override suspend fun onPreFling(available: Velocity): Velocity =
                    available
            }
        }

        // Map container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .nestedScroll(mapScrollConsumer)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasPermission),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                ),
                onMapClick = { latLng ->
                    markerPos = latLng
                    onLocationPicked(
                        "%.6f".format(latLng.latitude),
                        "%.6f".format(latLng.longitude)
                    )
                }
            ) {
                Marker(
                    state = MarkerState(position = markerPos),
                    title = "Property Location",
                    draggable = true,
                    onInfoWindowClick = {}
                )
            }

            // GPS button — top right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { locateMe() },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    containerColor = RentOutColors.Primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Detect my location",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tap-to-pin hint overlay — shown when no pin set yet
            if (latitude.isBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Tap map to pin property location",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Error message
        AnimatedVisibility(visible = locationError.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(locationError, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        // Coordinate display card
        AnimatedVisibility(visible = latitude.isNotBlank() && longitude.isNotBlank()) {
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
                        onClick = { onLocationPicked("", "") },
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
}
