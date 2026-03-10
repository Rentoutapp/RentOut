package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.RentOutColors

/**
 * iOS fallback for DirectionsMapView.
 * Shows property coordinates, estimated travel time chips, a share button,
 * and a "Navigate" button that opens Apple Maps / Google Maps via URL scheme.
 * A full interactive map requires native MapKit integration (future enhancement).
 */
@Composable
actual fun DirectionsMapView(
    propertyLat: Double,
    propertyLng: Double,
    propertyTitle: String,
    modifier: Modifier,
    onOpenNavigation: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Map placeholder card ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint     = RentOutColors.Primary.copy(alpha = 0.45f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Interactive map available on Android",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Navigate to open in Maps",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Coordinates chip — bottom left
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                shape    = RoundedCornerShape(10.dp),
                color    = Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%.4f, %.4f".format(propertyLat, propertyLng),
                        color      = Color.White,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Expand hint — top right
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f)) {
                    Icon(
                        Icons.Default.OpenInFull, "Expand",
                        tint     = Color.White,
                        modifier = Modifier.padding(6.dp).size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Coordinate info card ─────────────────────────────────────────────
        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(12.dp),
            color           = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
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
                            "%.6f, %.6f".format(propertyLat, propertyLng),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = RentOutColors.Primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Estimated travel time chips ──────────────────────────────────────
        // iOS can't get live user location easily here, so we show a static
        // "distance unknown" placeholder until MapKit integration is added.
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IosTravelTimeChip(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.DirectionsCar,
                mode     = "Driving",
                estimate = "—",
                tint     = RentOutColors.Primary
            )
            IosTravelTimeChip(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.DirectionsWalk,
                mode     = "Walking",
                estimate = "—",
                tint     = RentOutColors.IconTeal
            )
            IosTravelTimeChip(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.DirectionsBus,
                mode     = "Transit",
                estimate = "—",
                tint     = RentOutColors.IconAmber
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Action buttons ───────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Share button (placeholder — real share needs platform interop)
            OutlinedButton(
                onClick  = { /* iOS share sheet via interop — future */ },
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, RentOutColors.Primary)
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

            // Navigate button
            Button(
                onClick  = onOpenNavigation,
                modifier = Modifier.weight(2f).height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RentOutColors.Primary)
            ) {
                Icon(Icons.Default.Navigation, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Navigate", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Travel time chip (iOS) ────────────────────────────────────────────────────

@Composable
private fun IosTravelTimeChip(
    modifier: Modifier,
    icon:     ImageVector,
    mode:     String,
    estimate: String,
    tint:     Color
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = tint.copy(alpha = 0.09f)
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(estimate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
            Text(mode, fontSize = 9.sp, color = tint.copy(alpha = 0.7f))
        }
    }
}
