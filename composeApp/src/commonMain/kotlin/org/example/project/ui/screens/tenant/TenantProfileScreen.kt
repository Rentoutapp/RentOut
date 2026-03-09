@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.tenant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Transaction
import org.example.project.data.model.User
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.components.RentOutLoadingSpinner
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
    transactionsLoading: Boolean = false,
    onRefreshTransactions: () -> Unit = {},
    onPaymentHistoryClick: () -> Unit = {},
    onUnlockedClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    // Derive unlocked count from successful transactions — single source of truth
    // that keeps the stat card, action row subtitle, and payment history in sync
    val successfulUnlockCount = transactions.count { it.status.equals("success", ignoreCase = true) }
    val displayUnlockedCount = if (successfulUnlockCount > 0) successfulUnlockCount else unlockedCount

    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerVisible = true }

    var backPressed by remember { mutableStateOf(false) }
    val backScale by animateFloatAsState(if (backPressed) 0.82f else 1f, tween(180), label = "bs")
    val backRot   by animateFloatAsState(if (backPressed) -45f else 0f,  tween(180), label = "br")

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

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
                ProfileStatTile(Icons.Default.Key, "$displayUnlockedCount", "Unlocked", ProfileAmber)
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
                    // Gender — shown with accent colour matching the selected gender
                    val genderColor = when (user.gender) {
                        "Male"             -> Color(0xFF1565C0)
                        "Female"           -> Color(0xFFAD1457)
                        "Transgender"      -> Color(0xFF6A1B9A)
                        "Prefer not to say"-> Color(0xFF37474F)
                        else               -> ProfileSlateLight
                    }
                    val genderEmoji = when (user.gender) {
                        "Male"             -> "👨 "
                        "Female"           -> "👩 "
                        "Transgender"      -> "🏳️\u200D⚧️ "
                        "Prefer not to say"-> "🤝 "
                        else               -> ""
                    }
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Gender",
                        value = if (user.gender.isNotBlank()) "$genderEmoji${user.gender}" else "Not provided",
                        iconColor = genderColor
                    )
                    ProfileDetailDivider()
                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "National ID",
                        value = user.nationalId.ifBlank { "Not provided" },
                        iconColor = ProfileNavy
                    )
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
            ProfileActionItem(Icons.Default.Key, ProfileAmber, "My Unlocked Properties", "$displayUnlockedCount properties with visible contacts", onUnlockedClick)
            ProfileActionItem(
                Icons.Default.History,
                ProfileNavy,
                "Payment History",
                if (transactionsLoading && transactions.isEmpty()) "Loading payments…"
                else "${transactions.size} transactions • View all payments",
                onPaymentHistoryClick
            )
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
// -- Payment History Screen ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    transactions: List<Transaction>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onPropertyImageClick: (Transaction) -> Unit = {}
) {
    println("💳 PaymentHistoryScreen: Rendering with ${transactions.size} transactions (loading=$isLoading)")
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val successfulTransactions = transactions.filter { it.status.equals("success", ignoreCase = true) }
    val totalExpenditure = successfulTransactions.sumOf { it.amount }
    val successfulCount = successfulTransactions.size
    val refundCount = transactions.count {
        it.status.equals("failed", ignoreCase = true) ||
            it.status.equals("refund", ignoreCase = true) ||
            it.status.equals("refunded", ignoreCase = true)
    }
    val failedCount = transactions.count { it.status.equals("failed", ignoreCase = true) }
    val primaryCurrency = successfulTransactions.firstOrNull()?.currency?.uppercase()
        ?: transactions.firstOrNull()?.currency?.uppercase()
        ?: "USD"
    val expenditureAccent = currencyAccent(primaryCurrency)

    val scrollState = rememberScrollState()
    var pullDistance by remember { mutableStateOf(0f) }
    val pullProgress = (pullDistance / 180f).coerceIn(0f, 1f)
    val indicatorHeight = (pullProgress * 72f).dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileCream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ProfileNavy, ProfileNavyLight, Color(0xFF25548E))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wallet, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            "Payment History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "${transactions.size} ${if (transactions.size == 1) "transaction" else "transactions"}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PaymentSummaryCard(
                        modifier = Modifier.weight(1.2f),
                        title = "Total expenditure",
                        value = formatMoney(totalExpenditure, primaryCurrency),
                        subtitle = "$primaryCurrency • successful payments",
                        accent = expenditureAccent,
                        icon = Icons.Default.Payments
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentMiniSummaryCard("Successful", successfulCount.toString(), ProfileMint)
                        PaymentMiniSummaryCard("Refunds", refundCount.toString(), ProfileAmber)
                        PaymentMiniSummaryCard("Failed", failedCount.toString(), ProfileCoral)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLoading, scrollState.value) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (scrollState.value == 0 && dragAmount > 0f && !isLoading) {
                                    pullDistance = (pullDistance + dragAmount).coerceIn(0f, 220f)
                                    change.consume()
                                } else if (pullDistance > 0f && dragAmount < 0f) {
                                    pullDistance = (pullDistance + dragAmount).coerceAtLeast(0f)
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (pullDistance >= 140f && !isLoading) onRefresh()
                                pullDistance = 0f
                            },
                            onDragCancel = { pullDistance = 0f }
                        )
                    }
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pullDistance > 0f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(indicatorHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.South,
                            null,
                            tint = ProfileNavyLight.copy(alpha = 0.6f + (pullProgress * 0.4f)),
                            modifier = Modifier
                                .size(26.dp)
                                .graphicsLayer { translationY = (1f - pullProgress) * -12f }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (pullDistance >= 140f) "Release to refresh" else "Pull down to refresh",
                            fontSize = 12.sp,
                            color = ProfileSlateLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                when {
                    isLoading && transactions.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RentOutLoadingSpinner(color = ProfileNavy)
                            Text("Loading payment history…", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ProfileSlate)
                            Text(
                                "Fetching your transactions from Firestore",
                                fontSize = 14.sp,
                                color = ProfileSlateLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    transactions.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(ProfileNavy.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, null, tint = ProfileNavyLight, modifier = Modifier.size(42.dp))
                            }
                            Text("No Transactions Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ProfileSlate)
                            Text(
                                "Once you unlock a property, its payment will appear here with the property details and payment status.",
                                fontSize = 14.sp,
                                color = ProfileSlateLight,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Refresh history")
                            }
                        }
                    }

                    else -> {
                        transactions.forEachIndexed { index, transaction ->
                            PaymentHistoryItem(
                                transaction = transaction,
                                index = index,
                                onClick = { selectedTransaction = transaction },
                                onPropertyImageClick = onPropertyImageClick
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    selectedTransaction?.let { txn ->
        TransactionDetailDialog(transaction = txn, onDismiss = { selectedTransaction = null })
    }
}

@Composable
private fun PaymentHistoryItem(
    transaction: Transaction,
    index: Int,
    onClick: () -> Unit,
    onPropertyImageClick: (Transaction) -> Unit = {}
) {
    val statusColor = statusAccent(transaction.status)
    val statusIcon = when (transaction.status.lowercase()) {
        "success" -> Icons.Default.CheckCircle
        "failed" -> Icons.Default.Cancel
        else -> Icons.Default.Schedule
    }
    val accentGradient = Brush.verticalGradient(colors = listOf(statusColor, statusColor.copy(alpha = 0.72f)))

    var isPressed by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.982f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "item_scale"
    )

    LaunchedEffect(index) {
        kotlinx.coroutines.delay((index * 60).toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(320)) + slideInVertically(animationSpec = tween(320)) { it / 6 }
    ) {
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(152.dp)
                        .background(accentGradient)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (transaction.propertyImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = transaction.propertyImageUrl,
                                    contentDescription = transaction.propertyTitle.ifBlank { "Property image" },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onPropertyImageClick(transaction) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(statusColor.copy(alpha = 0.12f))
                                        .clickable(enabled = transaction.propertyId.isNotBlank()) { onPropertyImageClick(transaction) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(28.dp))
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    transaction.propertyTitle.ifBlank { "Unlocked property" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfileSlate,
                                    maxLines = 2
                                )
                                Text(
                                    transaction.propertyCity.takeIf { it.isNotBlank() }?.let { city ->
                                        listOf(transaction.propertyLocation.takeIf { it.isNotBlank() }, city)
                                            .filterNotNull()
                                            .joinToString(", ")
                                    } ?: transaction.propertyLocation.ifBlank { "Property details available in transaction" },
                                    fontSize = 12.sp,
                                    color = ProfileSlateLight,
                                    maxLines = 2
                                )
                                Text(
                                    formatMoney(transaction.amount, transaction.currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = currencyAccent(transaction.currency)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(shape = RoundedCornerShape(14.dp), color = statusColor.copy(alpha = 0.12f)) {
                                Text(
                                    transaction.status.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "#${index + 1}",
                                fontSize = 11.sp,
                                color = ProfileSlateLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMetaChip(Icons.Default.CalendarToday, formatDate(transaction.createdAt), Modifier.weight(1f))
                        PaymentMetaChip(Icons.Default.AccountBalanceWallet, transaction.paymentProvider.replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMetaChip(
                            icon = Icons.Default.Home,
                            text = transaction.propertyType.replaceFirstChar { it.uppercase() }.ifBlank { "Property" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMetaChip(
                            icon = Icons.Default.Bed,
                            text = if (transaction.propertyRooms > 0) "${transaction.propertyRooms} rooms" else "Property unlocked",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (transaction.paymentReference.isNotBlank()) {
                        Surface(
                            color = ProfileCream.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Reference", fontSize = 11.sp, color = ProfileSlateLight, fontWeight = FontWeight.Medium)
                                    Text(
                                        transaction.paymentReference,
                                        fontSize = 13.sp,
                                        color = ProfileSlate,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.Default.ArrowOutward, null, tint = ProfileSlateLight.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(110)
            isPressed = false
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun PaymentMiniSummaryCard(
    title: String,
    value: String,
    accent: Color
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun PaymentMetaChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ProfileCream.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = ProfileNavyLight, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = ProfileSlate,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun formatMoney(amount: Double, currency: String = "USD"): String {
    val whole = if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format(java.util.Locale.US, "%.2f", amount)
    return "$${whole} ${currency.uppercase()}"
}

private fun currencyAccent(currency: String): Color = when (currency.uppercase()) {
    "USD" -> Color(0xFF4F46E5)
    "ZWG", "ZWL" -> Color(0xFF0E9F6E)
    "RAND", "ZAR" -> Color(0xFFF59E0B)
    else -> ProfileAmber
}

private fun statusAccent(status: String): Color = when (status.lowercase()) {
    "success" -> ProfileMint
    "failed" -> ProfileCoral
    else -> ProfileAmber
}

@Composable
private fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val statusColor = statusAccent(transaction.status)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, null, tint = statusColor, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                text = transaction.propertyTitle.ifBlank { "Payment details" },
                fontWeight = FontWeight.ExtraBold,
                color = ProfileSlate
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TransactionDetailRow("Amount", formatMoney(transaction.amount, transaction.currency))
                TransactionDetailRow("Status", transaction.status.replaceFirstChar { it.uppercase() })
                TransactionDetailRow("Paid on", formatDate(transaction.createdAt))
                TransactionDetailRow("Provider", transaction.paymentProvider.replaceFirstChar { it.uppercase() })
                if (transaction.propertyLocation.isNotBlank() || transaction.propertyCity.isNotBlank()) {
                    TransactionDetailRow(
                        "Property",
                        listOf(
                            transaction.propertyLocation.takeIf { it.isNotBlank() },
                            transaction.propertyCity.takeIf { it.isNotBlank() }
                        ).filterNotNull().joinToString(", ")
                    )
                }
                if (transaction.paymentReference.isNotBlank()) {
                    TransactionDetailRow("Reference", transaction.paymentReference)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = statusColor)
            ) {
                Text("Close", color = Color.White)
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = ProfileSlateLight, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = ProfileSlate,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
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
        } catch (_: Exception) {
            "Invalid date"
        }
    } else {
        "Unknown"
    }
}
