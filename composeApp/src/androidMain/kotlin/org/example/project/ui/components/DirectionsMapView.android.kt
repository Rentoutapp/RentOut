package org.example.project.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import org.example.project.ui.theme.RentOutColors

@SuppressLint("MissingPermission")
@Composable
actual fun DirectionsMapView(
    propertyLat: Double,
    propertyLng: Double,
    propertyTitle: String,
    modifier: Modifier,
    onOpenNavigation: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val propertyPos = remember { LatLng(propertyLat, propertyLng) }

    var userLocation  by remember { mutableStateOf<LatLng?>(null) }
    var showDialog    by remember { mutableStateOf(false) }
    var isLocating    by remember { mutableStateOf(false) }
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
        if (hasPermission) locateUser(context, scope) { userLocation = it }
    }

    // Auto-locate on first composition
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLocating = true
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    isLocating = false
                    loc?.let { userLocation = LatLng(it.latitude, it.longitude) }
                }
                .addOnFailureListener { isLocating = false }
        }
    }

    // Preview card camera
    val previewCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(propertyPos, 13f)
    }

    // ── Preview card ──────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Static non-interactive preview map
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
            // Property marker
            Marker(
                state = MarkerState(position = propertyPos),
                title = propertyTitle,
                icon  = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }

        // Dark scrim
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))

        // "Open Directions" overlay button — centre
        Surface(
            modifier      = Modifier.align(Alignment.Center),
            shape         = RoundedCornerShape(24.dp),
            color         = RentOutColors.Primary,
            shadowElevation = 8.dp,
            onClick       = { showDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Directions, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Directions", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        // Coordinates chip — bottom left
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            shape    = RoundedCornerShape(10.dp),
            color    = Color.Black.copy(alpha = 0.60f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "%.4f, %.4f".format(propertyLat, propertyLng),
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        // Expand icon — top right
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.50f)) {
                Icon(
                    Icons.Default.OpenInFull, "Open map",
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp).size(14.dp)
                )
            }
        }
    }

    // ── Full-screen directions dialog ─────────────────────────────────────────
    if (showDialog) {
        DirectionsDialog(
            propertyPos   = propertyPos,
            propertyTitle = propertyTitle,
            userLocation  = userLocation,
            hasPermission = hasPermission,
            isLocating    = isLocating,
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onNavigate = {
                // Launch Google Maps navigation intent
                val uri = Uri.parse(
                    "google.navigation:q=$propertyLat,$propertyLng&mode=d"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback — open in browser
                    val browserUri = Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=$propertyLat,$propertyLng"
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
                onOpenNavigation()
            },
            onDismiss = { showDialog = false },
            scope     = scope
        )
    }
}

// ── Helper to locate user ─────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
private fun locateUser(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LatLng?) -> Unit
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { loc ->
            onResult(loc?.let { LatLng(it.latitude, it.longitude) })
        }
        .addOnFailureListener { onResult(null) }
}

// ── Full-screen directions dialog ─────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
private fun DirectionsDialog(
    propertyPos:        LatLng,
    propertyTitle:      String,
    userLocation:       LatLng?,
    hasPermission:      Boolean,
    isLocating:         Boolean,
    onRequestPermission: () -> Unit,
    onNavigate:         () -> Unit,
    onDismiss:          () -> Unit,
    scope:              kotlinx.coroutines.CoroutineScope
) {
    val context      = LocalContext.current
    val dialogCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(propertyPos, 13f)
    }

    // Fly camera to show both user + property when both are known
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            val bounds = LatLngBounds.builder()
                .include(userLocation)
                .include(propertyPos)
                .build()
            scope.launch {
                dialogCamera.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 160),
                    durationMs = 900
                )
            }
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
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter   = scaleIn(
                initialScale  = 0.55f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(220)),
            exit    = scaleOut(targetScale = 0.75f, animationSpec = tween(180)) +
                      fadeOut(tween(160))
        ) {
            Surface(
                modifier        = Modifier.fillMaxSize().padding(10.dp),
                shape           = RoundedCornerShape(24.dp),
                shadowElevation = 24.dp,
                color           = MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.fillMaxSize()) {

                    // ── Interactive map ───────────────────────────────────
                    GoogleMap(
                        modifier            = Modifier.fillMaxSize(),
                        cameraPositionState = dialogCamera,
                        properties          = MapProperties(
                            isMyLocationEnabled = hasPermission
                        ),
                        uiSettings          = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled     = true,
                            compassEnabled          = true,
                            mapToolbarEnabled       = true
                        )
                    ) {
                        // Property marker — green
                        Marker(
                            state = MarkerState(position = propertyPos),
                            title = propertyTitle,
                            snippet = "Property Location",
                            icon  = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            )
                        )

                        // User location marker — blue
                        userLocation?.let { userPos ->
                            Marker(
                                state   = MarkerState(position = userPos),
                                title   = "Your Location",
                                snippet = "You are here",
                                icon    = BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_AZURE
                                )
                            )

                            // Dashed polyline — user to property
                            Polyline(
                                points  = listOf(userPos, propertyPos),
                                color   = RentOutColors.Primary,
                                width   = 6f,
                                pattern = listOf(
                                    com.google.android.gms.maps.model.Dash(20f),
                                    com.google.android.gms.maps.model.Gap(10f)
                                )
                            )
                        }
                    }

                    // ── Top bar ───────────────────────────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
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
                                    Icons.Default.Directions,
                                    null,
                                    tint     = RentOutColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Directions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        propertyTitle,
                                        fontSize = 11.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Locating spinner ──────────────────────────────────
                    AnimatedVisibility(
                        visible  = isLocating,
                        modifier = Modifier.align(Alignment.Center),
                        enter    = fadeIn(), exit = fadeOut()
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
                                    Modifier.size(16.dp), Color.White, strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Locating you…", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    // ── Permission request banner ─────────────────────────
                    AnimatedVisibility(
                        visible  = !hasPermission,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                        enter = fadeIn() + slideInVertically { it },
                        exit  = fadeOut() + slideOutVertically { it }
                    ) {
                        Surface(
                            shape           = RoundedCornerShape(16.dp),
                            color           = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            onClick         = onRequestPermission
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOff,
                                    null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Location needed",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 13.sp
                                    )
                                    Text(
                                        "Tap to grant permission and show directions from your location",
                                        fontSize = 11.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // ── Bottom action bar ─────────────────────────────────
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        shape    = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // Coordinate info row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null,
                                        tint     = RentOutColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Property Coordinates",
                                            fontSize = 10.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "%.6f, %.6f".format(
                                                propertyPos.latitude,
                                                propertyPos.longitude
                                            ),
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = RentOutColors.Primary
                                        )
                                    }
                                }
                                // Distance chip if user location known
                                userLocation?.let { userPos ->
                                    val distKm = haversineKm(
                                        userPos.latitude, userPos.longitude,
                                        propertyPos.latitude, propertyPos.longitude
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RentOutColors.Primary.copy(alpha = 0.10f)
                                    ) {
                                        Text(
                                            if (distKm < 1.0) "%.0f m".format(distKm * 1000)
                                            else "%.1f km".format(distKm),
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = RentOutColors.Primary,
                                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // ── Estimated travel time cards (shown when distance is known) ──
                            userLocation?.let { userPos ->
                                val distKm = haversineKm(
                                    userPos.latitude, userPos.longitude,
                                    propertyPos.latitude, propertyPos.longitude
                                )

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Driving estimate (~50 km/h avg city speed)
                                    val driveMin = (distKm / 50.0 * 60).toInt().coerceAtLeast(1)
                                    val driveLabel = when {
                                        driveMin < 60  -> "$driveMin min"
                                        else           -> "${driveMin / 60}h ${driveMin % 60}m"
                                    }
                                    TravelTimeChip(
                                        modifier    = Modifier.weight(1f),
                                        icon        = Icons.Default.DirectionsCar,
                                        mode        = "Driving",
                                        estimate    = driveLabel,
                                        tint        = RentOutColors.Primary
                                    )

                                    // Walking estimate (~5 km/h avg walk speed)
                                    val walkMin = (distKm / 5.0 * 60).toInt().coerceAtLeast(1)
                                    val walkLabel = when {
                                        walkMin < 60   -> "$walkMin min"
                                        else           -> "${walkMin / 60}h ${walkMin % 60}m"
                                    }
                                    TravelTimeChip(
                                        modifier    = Modifier.weight(1f),
                                        icon        = Icons.Default.DirectionsWalk,
                                        mode        = "Walking",
                                        estimate    = walkLabel,
                                        tint        = RentOutColors.IconTeal
                                    )

                                    // Transit estimate (~25 km/h avg transit speed)
                                    val transitMin = (distKm / 25.0 * 60).toInt().coerceAtLeast(1)
                                    val transitLabel = when {
                                        transitMin < 60 -> "$transitMin min"
                                        else            -> "${transitMin / 60}h ${transitMin % 60}m"
                                    }
                                    TravelTimeChip(
                                        modifier    = Modifier.weight(1f),
                                        icon        = Icons.Default.DirectionsBus,
                                        mode        = "Transit",
                                        estimate    = transitLabel,
                                        tint        = RentOutColors.IconAmber
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // ── Action buttons row ────────────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Share Location button
                                OutlinedButton(
                                    onClick  = {
                                        val shareText = buildString {
                                            append("📍 $propertyTitle\n")
                                            append("Coordinates: %.6f, %.6f\n".format(
                                                propertyPos.latitude, propertyPos.longitude
                                            ))
                                            append("Google Maps: https://www.google.com/maps?q=")
                                            append("%.6f,%.6f".format(
                                                propertyPos.latitude, propertyPos.longitude
                                            ))
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            putExtra(Intent.EXTRA_SUBJECT, "Property Location — $propertyTitle")
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share location via…")
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape    = RoundedCornerShape(16.dp),
                                    border   = androidx.compose.foundation.BorderStroke(
                                        1.5.dp, RentOutColors.Primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        null,
                                        tint     = RentOutColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Share",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = RentOutColors.Primary
                                    )
                                }

                                // Start Navigation button
                                Button(
                                    onClick  = onNavigate,
                                    modifier = Modifier.weight(2f).height(52.dp),
                                    shape    = RoundedCornerShape(16.dp),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = RentOutColors.Primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Start Navigation",
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Travel time chip ─────────────────────────────────────────────────────────

@Composable
private fun TravelTimeChip(
    modifier:  Modifier,
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    mode:      String,
    estimate:  String,
    tint:      Color
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = tint.copy(alpha = 0.09f)
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                estimate,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = tint
            )
            Text(
                mode,
                fontSize = 9.sp,
                color    = tint.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Haversine distance formula ────────────────────────────────────────────────

private fun haversineKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r      = 6371.0
    val dLat   = Math.toRadians(lat2 - lat1)
    val dLon   = Math.toRadians(lon2 - lon1)
    val a      = Math.sin(dLat / 2).let { it * it } +
                 Math.cos(Math.toRadians(lat1)) *
                 Math.cos(Math.toRadians(lat2)) *
                 Math.sin(dLon / 2).let { it * it }
    return r * 2 * Math.asin(Math.sqrt(a))
}
