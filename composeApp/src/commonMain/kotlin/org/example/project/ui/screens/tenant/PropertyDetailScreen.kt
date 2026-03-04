package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

@Composable
fun PropertyDetailScreen(
    property: Property,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    onCall: (String) -> Unit = {},
    onWhatsApp: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.8f else 1f,
        animationSpec = tween(200), label = "back_scale"
    )
    val backRotation by animateFloatAsState(
        targetValue = if (backPressed) -45f else 0f,
        animationSpec = tween(200), label = "back_rotation"
    )

    // Contact reveal animation
    val contactAlpha by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "contact_reveal"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            // ── Hero image ────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = property.imageUrl,
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay top
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.4f), Color.Transparent, Color.Black.copy(0.3f))
                        )
                    )
                )
                // Back button
                Box(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .graphicsLayer { scaleX = backScale; scaleY = backScale; rotationZ = backRotation },
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { backPressed = true; onBack() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                }
                // Price badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RentOutColors.Secondary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "$${property.price.toInt()}/mo",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(20.dp)) {
                // Title & badges
                Text(property.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (property.isVerified) VerifiedBadge()
                    AvailabilityBadge(property.isAvailable)
                }

                Spacer(Modifier.height(16.dp))

                // Location row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = RentOutColors.IconRose, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(property.location.ifBlank { property.city }, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(20.dp))

                // Stats row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DetailStat(Icons.Default.Bed, "${property.rooms}", "Bedrooms", RentOutColors.IconBlue)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        DetailStat(Icons.Default.Bathtub, "${property.bathrooms}", "Bathrooms", RentOutColors.IconTeal)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        DetailStat(Icons.Default.Home, property.propertyType.replaceFirstChar { it.uppercase() }, "Type", RentOutColors.IconPurple)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Description
                Text("About This Property", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text(property.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)

                // Amenities
                if (property.amenities.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Amenities", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        property.amenities.forEach { amenity ->
                            AmenityChip(amenity)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                // ── Contact section ───────────────────────────────────────────
                Text("Contact Landlord", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked)
                            RentOutColors.StatusApproved.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (isUnlocked) {
                            // Revealed contact
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                        .background(RentOutColors.StatusApproved.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = RentOutColors.StatusApproved, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Contact Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        property.contactNumber,
                                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.graphicsLayer { alpha = contactAlpha }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RentOutPrimaryButton(
                                    text = "📞 Call",
                                    onClick = { onCall(property.contactNumber) },
                                    modifier = Modifier.weight(1f)
                                )
                                RentOutSecondaryButton(
                                    text = "💬 WhatsApp",
                                    onClick = { onWhatsApp(property.contactNumber) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Locked state
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                        .background(RentOutColors.IconAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = RentOutColors.IconAmber, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Contact Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("•••••••••••••", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            if (property.isAvailable) {
                                RentOutPrimaryButton(
                                    text = "🔓 Unlock Contact — \$10",
                                    onClick = onUnlock,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RentOutColors.StatusRejected.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "🔴 This property is no longer available",
                                        modifier = Modifier.padding(12.dp),
                                        color = RentOutColors.StatusRejected,
                                        fontSize = 14.sp, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AmenityChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("✓ $text", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

// graphicsLayer alpha helper — applied inline via Modifier.graphicsLayer { alpha = value }
