@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.User
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun TenantProfileScreen(
    user: User,
    unlockedCount: Int,
    onUnlockedClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
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

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(listOf(RentOutColors.Primary, RentOutColors.PrimaryDark)))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { backPressed = true; onBack() },
                        modifier = Modifier.graphicsLayer {
                            scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                }
                // Avatar — shows profile photo if available, else initials
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profilePhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.profilePhotoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = user.name.firstOrNull()?.uppercase() ?: "T",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(user.name.ifBlank { "Tenant" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(user.email, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-20).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProfileStat("🔑", "$unlockedCount", "Unlocked")
                Divider(modifier = Modifier.width(1.dp).height(40.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ProfileStat("✅", "Tenant", "Role")
                Divider(modifier = Modifier.width(1.dp).height(40.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ProfileStat("⭐", "Active", "Status")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Menu items
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            ProfileMenuItem(
                icon = Icons.Default.Key,
                iconTint = RentOutColors.IconAmber,
                title = "My Unlocked Properties",
                subtitle = "$unlockedCount properties unlocked",
                onClick = onUnlockedClick
            )
            ProfileMenuItem(
                icon = Icons.Default.Receipt,
                iconTint = RentOutColors.IconBlue,
                title = "Payment History",
                subtitle = "View all transactions",
                onClick = {}
            )
            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                iconTint = RentOutColors.IconPurple,
                title = "Notifications",
                subtitle = "Manage notifications",
                onClick = {}
            )
            ProfileMenuItem(
                icon = Icons.Default.Help,
                iconTint = RentOutColors.IconTeal,
                title = "Help & Support",
                subtitle = "Get help with your account",
                onClick = {}
            )
        }

        Spacer(Modifier.height(16.dp))

        // Logout
        Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            RentOutPrimaryButton(
                text = "Sign Out",
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
