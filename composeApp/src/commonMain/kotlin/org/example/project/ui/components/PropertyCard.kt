package org.example.project.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.ui.theme.RentOutColors

@Composable
fun PropertyCard(
    property: Property,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = false,
    isUnlocked: Boolean = true,          // false = tenant view, address hidden until paid
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onToggleAvailability: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            propertyTitle = property.title,
            onConfirm = {
                showDeleteDialog = false
                onDelete?.invoke()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── Image ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = property.imageUrl,
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                startY = 80f
                            )
                        )
                )
                // Price chip
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RentOutColors.Secondary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$${property.price.toInt()}/mo",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Classification + property type chip (top-left)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(RentOutColors.Primary.copy(alpha = 0.85f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = property.classification,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = property.propertyType,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Provider subtype chip
                    val providerChipText = when (property.providerSubtype) {
                        "agent"     -> "🤝 Agent"
                        "brokerage" -> "🏢 Brokerage"
                        else        -> ""
                    }
                    if (providerChipText.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        val chipColor = when (property.providerSubtype) {
                            "agent"     -> Color(0xFF00897B)
                            "brokerage" -> Color(0xFF7C5CBF)
                            else        -> Color.Transparent
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = chipColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = providerChipText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = chipColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                // Bottom-left city
                Text(
                    text = property.city,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            // ── Details ────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                // Title with suburb suffix (if not already present)
                val displayTitle = remember(property.title, property.location) {
                    // If title already contains "in", assume it has the suburb suffix
                    if (property.title.contains(" in ", ignoreCase = true)) {
                        property.title
                    } else {
                        // Extract suburb from location (format: "street, suburb, city, country")
                        val locationParts = property.location.split(", ")
                        val suburb = if (locationParts.size >= 2) locationParts[1].trim() else ""
                        if (suburb.isNotBlank()) {
                            "${property.title} in $suburb"
                        } else {
                            property.title
                        }
                    }
                }
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                // Location — hidden until tenant unlocks the property
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUnlocked) {
                        // ── Revealed: show full address ───────────────────────
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = RentOutColors.IconRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = property.location.ifBlank { property.city },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // ── Locked: show lock badge ───────────────────────────
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = RentOutColors.IconAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Address hidden · unlock to view",
                            style = MaterialTheme.typography.bodySmall,
                            color = RentOutColors.IconAmber,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (property.rooms > 0) PropertyStat(icon = Icons.Default.Bed, label = "${property.rooms} Beds", tint = RentOutColors.IconBlue)
                    if (property.bathrooms > 0) PropertyStat(icon = Icons.Default.Bathtub, label = "${property.bathrooms} Bath", tint = RentOutColors.IconTeal)
                    if (property.locationType.isNotBlank()) PropertyStat(icon = Icons.Default.LocationCity, label = property.locationType, tint = RentOutColors.IconPurple)
                }
                // Bills row
                if (property.billsInclusive.isNotEmpty() || property.billsExclusive.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        if (property.billsInclusive.isNotEmpty()) {
                            PropertyStat(
                                icon = Icons.Default.CheckCircle,
                                label = "Incl: ${property.billsInclusive.joinToString(", ")}",
                                tint = RentOutColors.Tertiary
                            )
                        }
                        if (property.billsExclusive.isNotEmpty()) {
                            PropertyStat(
                                icon = Icons.Default.Cancel,
                                label = "Excl: ${property.billsExclusive.joinToString(", ")}",
                                tint = RentOutColors.IconAmber
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Badges row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (property.isVerified) VerifiedBadge()
                    AvailabilityBadge(property.isAvailable)
                    if (showActions) StatusBadge(property.status)
                }

                // ── Landlord Actions ──────────────────────────────────────────
                if (showActions) {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        onToggleAvailability?.let {
                            OutlinedButton(
                                onClick = it,
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    if (property.isAvailable) "Mark Unavailable" else "Mark Available",
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // Edit is only allowed while the property is pending or rejected.
                        // Once approved by the admin, the listing is locked for editing.
                        if (property.status != "approved") {
                            onEdit?.let {
                                IconButton(onClick = it, modifier = Modifier.size(38.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", tint = RentOutColors.IconBlue)
                                }
                            }
                        }
                        onDelete?.let {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = RentOutColors.IconRose)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeleteConfirmationDialog(
    propertyTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RentOutColors.IconRose.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RentOutColors.IconRose,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Property?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this property?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = propertyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RentOutColors.IconAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RentOutColors.IconAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RentOutColors.IconRose,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delete Property",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    )
}
