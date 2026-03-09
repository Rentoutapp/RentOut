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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Property
import org.example.project.presentation.UnlockState
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

@Composable
fun PaymentScreen(
    property: Property,
    unlockState: UnlockState,
    onPay: () -> Unit,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.8f else 1f,
        animationSpec = tween(200), label = "back_scale"
    )
    val backRotation by animateFloatAsState(
        targetValue = if (backPressed) -45f else 0f,
        animationSpec = tween(200), label = "back_rotation"
    )

    // Success animation
    val successScale by animateFloatAsState(
        targetValue = if (unlockState is UnlockState.Success) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success_scale"
    )

    LaunchedEffect(unlockState) {
        if (unlockState is UnlockState.Success) {
            kotlinx.coroutines.delay(2000)
            onSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background)
    ) {
        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // Top bar with back button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { backPressed = true; onBack() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("Unlock Contact", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            // Success state
            AnimatedVisibility(
                visible = unlockState is UnlockState.Success,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().scale(successScale),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = RentOutColors.StatusApproved.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(RentOutColors.StatusApproved.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅", fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Contact Unlocked! 🎉", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = RentOutColors.StatusApproved, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("You can now see the landlord's contact details.",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            // Error state
            AnimatedVisibility(visible = unlockState is UnlockState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text((unlockState as? UnlockState.Error)?.message ?: "Payment failed. Please try again.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (unlockState !is UnlockState.Success) {
                Spacer(Modifier.height(8.dp))

                // Order summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Order Summary", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        OrderRow("Property", property.title, Icons.Default.Apartment, RentOutColors.IconBlue)
                        Spacer(Modifier.height(10.dp))
                        OrderRow("Location", property.city, Icons.Default.LocationOn, RentOutColors.IconRose)
                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unlock Fee", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("$10.00 USD", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                                color = RentOutColors.Secondary)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Payment method card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Payment via PesePay", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Secure payment powered by PesePay", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))

                        // Supported methods grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("💳 Visa/MC", "📱 Ecocash", "📲 OneMoney", "📳 Telecash").forEach { method ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(method, fontSize = 10.sp, textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = RentOutColors.StatusApproved,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Your payment is encrypted & secure", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Pay button
                RentOutPrimaryButton(
                    text = "💳 Pay \$10 to Unlock",
                    onClick = onPay,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = unlockState is UnlockState.Loading
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "By paying, you agree to our Terms of Service. This is a one-time fee to reveal landlord contact details for this property.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OrderRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
