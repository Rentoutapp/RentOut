@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Transaction
import org.example.project.data.model.User
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.components.DeleteAccountConfirmationDialog
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Reuse the same colour tokens as TenantHomeScreen
private val ProfileNavy      = Color(0xFF0F2A4A)
private val ProfileNavyLight = Color(0xFF1A3F6F)
private val ProfileCream     = Color(0xFFF5F0EB)
private val ProfileCoral     = Color(0xFFE8724A)
private val ProfileAmber     = Color(0xFFF5A623)
private val ProfileMint      = Color(0xFF38B2AC)
private val ProfileSlate     = Color(0xFF4A5568)
private val ProfileSlateLight= Color(0xFF718096)

@Composable
fun TenantProfileScreen(
    user: User,
    unlockedCount: Int,
    transactions: List<Transaction> = emptyList(),
    onUnlockedClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerVisible = true }

    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.82f else 1f, tween(180), label = "bs")
    val backRot   by animateFloatAsState(if (backPressed) -45f else 0f,  tween(180), label = "br")

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPaymentHistory by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Payment History Dialog
    if (showPaymentHistory) {
        println("🔔 TenantProfileScreen: Opening PaymentHistoryDialog with ${transactions.size} transactions")
        PaymentHistoryDialog(
            transactions = transactions,
            onDismiss = { showPaymentHistory = false }
        )
    }

    // Logout confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null, tint = ProfileCoral) },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your account?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileCoral)
                ) { Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Delete Account confirmation
    if (showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            userEmail = user.email,
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccount()
            },
            onDismiss = {
                showDeleteAccountDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ProfileCream).verticalScroll(rememberScrollState())
    ) {
        // ── Hero header — redesigned to match landlord profile (name left, image right) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(ProfileNavy, ProfileNavyLight)))
                .statusBarsPadding()
        ) {
            // Back button — top start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        backPressed = true
                        onBack()
                    }
                    .graphicsLayer { scaleX = backScale; scaleY = backScale; rotationZ = backRot },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ── Horizontal header layout (name left, image right) ────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Left: name + role badge (constrained to avatar height) ────────
                val avatarHeight = 180.dp
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(500)) + slideInHorizontally(tween(500)) { -50 }
                ) {
                    Column(
                        modifier = Modifier
                            .height(avatarHeight)
                            .weight(1f)
                            .padding(end = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = user.name.ifBlank { "Tenant" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        // Role badge with thin border
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Tenant",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Right: vertical rectangle avatar with thin halo ──────────────
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(500)) + scaleIn(
                        tween(500, easing = FastOutSlowInEasing),
                        initialScale = 0.8f
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ProfileCoral, ProfileAmber)
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profilePhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.profilePhotoUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = user.name.split(" ").filter { it.isNotBlank() }
                                .take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() }
                                .ifBlank { "T" }
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // ── Stats card — no overlap needed with new compact header ────────────────
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatTile(Icons.Default.Key, "$unlockedCount", "Unlocked", ProfileAmber)
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(ProfileCream))
                ProfileStatTile(Icons.Default.CheckCircle, "Active", "Status", ProfileMint)
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(ProfileCream))
                ProfileStatTile(Icons.Default.Shield, "Tenant", "Role", ProfileCoral)
            }
        }

        // ── Account details section ────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            
            Spacer(Modifier.height(20.dp))

            Text("Account Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProfileSlate, modifier = Modifier.padding(bottom = 10.dp))

            // Details card — shows all registration data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    ProfileDetailRow(Icons.Default.Person, "Full Name", user.name.ifBlank { "Not set" }, ProfileCoral)
                    ProfileDetailDivider()
                    ProfileDetailRow(Icons.Default.Email, "Email Address", user.email.ifBlank { "Not set" }, ProfileNavy)
                    ProfileDetailDivider()
                    ProfileDetailRow(Icons.Default.Phone, "Phone Number",
                        user.phoneNumber.ifBlank { "Not provided" }, ProfileMint)
                    ProfileDetailDivider()
                    ProfileDetailRow(Icons.Default.VerifiedUser, "Account Status",
                        user.status.replaceFirstChar { it.uppercase() }, if (user.status == "active") ProfileMint else ProfileCoral)
                    ProfileDetailDivider()
                    ProfileDetailRow(Icons.Default.DateRange, "Member Since",
                        if (user.createdAt > 0L) {
                            val date = java.util.Date(user.createdAt)
                            val fmt  = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            fmt.format(date)
                        } else "Unknown", ProfileSlateLight)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProfileSlate, modifier = Modifier.padding(bottom = 10.dp))

            // Actions
            ProfileActionItem(Icons.Default.Key, ProfileAmber, "My Unlocked Properties", "$unlockedCount properties with visible contacts", onUnlockedClick)
            ProfileActionItem(Icons.Default.History, ProfileNavy, "Payment History", "${transactions.size} transactions • View all payments", { showPaymentHistory = true })
            ProfileActionItem(Icons.Default.Notifications, RentOutColors.IconPurple, "Notifications", "Manage your notification preferences", {})
            ProfileActionItem(Icons.Default.Help, ProfileMint, "Help & Support", "Get help or report an issue", {})

            Spacer(Modifier.height(24.dp))

            // ── Logout button — coral, full width, embedded in profile ─────────
            val logoutInteraction = remember { MutableInteractionSource() }
            val isLogoutPressed by logoutInteraction.collectIsPressedAsState()
            val logoutScale by animateFloatAsState(
                if (isLogoutPressed) 0.95f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "logout_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(logoutScale)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(ProfileCoral, Color(0xFFD45A38))))
                    .clickable(interactionSource = logoutInteraction, indication = null) { showLogoutDialog = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Sign Out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Delete Account button ─────────────────────────────────────
            val deleteInteraction = remember { MutableInteractionSource() }
            val isDeletePressed by deleteInteraction.collectIsPressedAsState()
            val deleteScale by animateFloatAsState(
                if (isDeletePressed) 0.95f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "delete_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(deleteScale)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, RentOutColors.IconRose, RoundedCornerShape(16.dp))
                    .clickable(interactionSource = deleteInteraction, indication = null) { showDeleteAccountDialog = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.PersonRemove, null, tint = RentOutColors.IconRose, modifier = Modifier.size(20.dp))
                    Text("Delete Account", color = RentOutColors.IconRose, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Stat tile ─────────────────────────────────────────────────────────────────
@Composable
private fun ProfileStatTile(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ProfileSlate)
        Text(label, fontSize = 10.sp, color = ProfileSlateLight)
    }
}

// ── Account detail row ────────────────────────────────────────────────────────
@Composable
private fun ProfileDetailRow(icon: ImageVector, label: String, value: String, iconColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = ProfileSlateLight, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ProfileSlate)
        }
    }
}

@Composable
private fun ProfileDetailDivider() {
    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = ProfileCream)
}

// ── Action menu item ──────────────────────────────────────────────────────────
@Composable
private fun ProfileActionItem(icon: ImageVector, iconColor: Color, title: String, subtitle: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "ai_s")
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ProfileSlate)
                Text(subtitle, fontSize = 12.sp, color = ProfileSlateLight)
            }
            Icon(Icons.Default.ChevronRight, null, tint = ProfileSlateLight.copy(0.6f))
        }
    }
}

// -- Payment History Dialog ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentHistoryDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    println("💳 PaymentHistoryDialog: Rendering with ${transactions.size} transactions")
    transactions.forEach { 
        println("   → Transaction: ${it.id} - $${it.amount} ${it.currency} - ${it.status} - ${it.createdAt}")
    }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // -- Header with gradient --------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(ProfileNavy, ProfileNavyLight)))
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Payment History",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    "${transactions.size} ${if (transactions.size == 1) "transaction" else "transactions"}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(0.85f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // -- Transaction List ------------------------------------------
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                null,
                                tint = ProfileSlateLight.copy(0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "No Transactions Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfileSlate
                            )
                            Text(
                                "Your payment history will appear here",
                                fontSize = 14.sp,
                                color = ProfileSlateLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        transactions.forEach { transaction ->
                            PaymentHistoryItem(
                                transaction = transaction,
                                onClick = { selectedTransaction = transaction }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    selectedTransaction?.let { txn ->
        TransactionDetailDialog(
            transaction = txn,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
private fun PaymentHistoryItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val statusColor = when (transaction.status) {
        "success" -> ProfileMint
        "failed" -> ProfileCoral
        else -> ProfileAmber
    }

    val statusIcon = when (transaction.status) {
        "success" -> Icons.Default.CheckCircle
        "failed" -> Icons.Default.Cancel
        else -> Icons.Default.Schedule
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "item_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileCream.copy(0.5f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    null,
                    tint = statusColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$${transaction.amount.toInt()} ${transaction.currency}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfileSlate
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatDate(transaction.createdAt),
                    fontSize = 13.sp,
                    color = ProfileSlateLight
                )
                if (transaction.paymentReference.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Ref: ${transaction.paymentReference.take(12)}...",
                        fontSize = 11.sp,
                        color = ProfileSlateLight.copy(0.7f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    transaction.status.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val statusColor = when (transaction.status) {
        "success" -> ProfileMint
        "failed" -> ProfileCoral
        else -> ProfileAmber
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Receipt,
                    null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Transaction Details",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TransactionDetailRow("Amount", "$${transaction.amount} ${transaction.currency}")
                TransactionDetailRow("Status", transaction.status.replaceFirstChar { it.uppercase() })
                TransactionDetailRow("Date", formatDate(transaction.createdAt))
                TransactionDetailRow("Provider", transaction.paymentProvider.replaceFirstChar { it.uppercase() })
                if (transaction.paymentReference.isNotBlank()) {
                    TransactionDetailRow("Reference", transaction.paymentReference)
                }
                TransactionDetailRow("Transaction ID", transaction.id.take(16) + "...")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ProfileNavy)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = ProfileSlateLight,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProfileSlate,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0L) {
        try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val month = when (dateTime.monthNumber) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                else -> ""
            }
            "${dateTime.dayOfMonth.toString().padStart(2, '0')} $month ${dateTime.year}, " +
                    "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "Invalid date"
        }
    } else {
        "Unknown"
    }
}
