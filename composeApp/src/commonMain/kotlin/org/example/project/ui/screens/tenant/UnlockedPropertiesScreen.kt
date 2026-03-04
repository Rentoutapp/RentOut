@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Property
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

@Composable
fun UnlockedPropertiesScreen(
    unlockedProperties: List<Property>,
    onPropertyClick: (Property) -> Unit,
    onBack: () -> Unit
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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))))
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Column {
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
                    Column {
                        Text("My Unlocked", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("Properties 🔑", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (unlockedProperties.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🔑", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No unlocked properties yet", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Browse properties and pay \$10 to unlock landlord contact details.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    RentOutSecondaryButton("Browse Properties", onClick = onBack, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RentOutColors.StatusApproved.copy(alpha = 0.08f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = RentOutColors.StatusApproved)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${unlockedProperties.size} unlocked ${if (unlockedProperties.size == 1) "property" else "properties"} — contact details are visible",
                                fontSize = 13.sp, color = RentOutColors.TertiaryDark, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                items(unlockedProperties, key = { it.id }) { property ->
                    UnlockedPropertyCard(property = property, onClick = { onPropertyClick(property) })
                }
            }
        }
    }
}

@Composable
private fun UnlockedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(RentOutColors.IconBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏠", fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(property.city, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, tint = RentOutColors.StatusApproved, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(property.contactNumber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = RentOutColors.StatusApproved)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
