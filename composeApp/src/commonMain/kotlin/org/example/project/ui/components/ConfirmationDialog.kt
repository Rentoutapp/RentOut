package org.example.project.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.RentOutColors

/**
 * Reusable confirmation dialog for destructive actions.
 * 
 * @param title Dialog title (e.g., "Delete Image?")
 * @param message Main message explaining the action
 * @param icon Icon to display at the top
 * @param iconTint Color of the icon and confirm button
 * @param confirmText Text for the confirm button (e.g., "Delete")
 * @param cancelText Text for the cancel button (default: "Cancel")
 * @param itemName Optional name of the item being affected (shown in highlighted box)
 * @param warningText Optional warning text shown at the bottom (e.g., "This action cannot be undone")
 * @param onConfirm Callback when user confirms
 * @param onDismiss Callback when user cancels or dismisses
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    icon: ImageVector,
    iconTint: Color,
    confirmText: String,
    cancelText: String = "Cancel",
    itemName: String? = null,
    warningText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Animation state
    var visible by remember { mutableStateOf(false) }
    
    // Trigger animation on composition
    LaunchedEffect(Unit) {
        visible = true
    }
    
    // Animated scale and alpha
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialog_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "dialog_alpha"
    )
    
    // Icon bounce animation
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_bounce"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconTint.copy(alpha = 0.1f))
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Optional item name display
                itemName?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Optional warning text
                warningText?.let {
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
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = RentOutColors.IconAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconTint,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = confirmText,
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
                    text = cancelText,
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
}

/**
 * Specialized confirmation dialog for image removal.
 */
@Composable
fun RemoveImageConfirmationDialog(
    imageType: String = "image",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Remove ${imageType.replaceFirstChar { it.uppercase() }}?",
        message = "Are you sure you want to remove this $imageType from the property?",
        icon = Icons.Default.Delete,
        iconTint = RentOutColors.IconRose,
        confirmText = "Remove",
        itemName = null,
        warningText = "You can re-add it later if needed.",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized confirmation dialog for account deletion.
 */
@Composable
fun DeleteAccountConfirmationDialog(
    userEmail: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete Account?",
        message = "This will permanently delete your account and all associated data including properties, transactions, and unlocked listings.",
        icon = Icons.Default.PersonRemove,
        iconTint = RentOutColors.IconRose,
        confirmText = "Delete Account",
        itemName = userEmail,
        warningText = "This action cannot be undone. All your data will be permanently deleted.",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
