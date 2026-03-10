package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.RentOutColors

/**
 * iOS fallback: manual lat/lng entry with a styled placeholder map box.
 */
@Composable
actual fun MapPickerView(
    latitude: String,
    longitude: String,
    onLocationPicked: (lat: String, lng: String) -> Unit,
    modifier: Modifier
) {
    var lat by remember { mutableStateOf(latitude) }
    var lng by remember { mutableStateOf(longitude) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Map placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    null,
                    tint = RentOutColors.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Interactive map available on Android",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Enter coordinates manually below",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = lat,
                onValueChange = {
                    lat = it
                    onLocationPicked(it, lng)
                },
                label = { Text("Latitude") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lng,
                onValueChange = {
                    lng = it
                    onLocationPicked(lat, it)
                },
                label = { Text("Longitude") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = RentOutColors.Primary, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
