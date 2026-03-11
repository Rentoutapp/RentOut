package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.User
import org.example.project.data.model.isAgent
import org.example.project.data.model.isBrokerage
import org.example.project.data.model.providerEmoji
import org.example.project.data.model.providerDisplayName
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.components.DeleteAccountConfirmationDialog

@Composable
fun LandlordProfileScreen(
    user: User,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Animate content in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header with gradient ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(RentOutColors.PrimaryDark, RentOutColors.Primary)
                    )
                )
                .statusBarsPadding()
        ) {
            // Back button — top start
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }

            // ── Horizontal header layout ──────────────────────────────────────
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
                // ── Left: name + role badge (constrained to avatar height) ────
                val avatarHeight = 180.dp
                Column(
                    modifier = Modifier
                        .height(avatarHeight)
                        .weight(1f)
                        .padding(end = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = user.name.ifBlank { "Landlord" },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    ) {
                        Text(
                            text = "${user.providerEmoji} ${user.providerDisplayName}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // ── Right: rounded-rectangle avatar ──────────────────────────
                val avatarInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isAvatarPressed by avatarInteraction.collectIsPressedAsState()
                val avatarScale by animateFloatAsState(
                    targetValue = if (isAvatarPressed) 0.93f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "avatar_scale"
                )

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(180.dp)
                        .scale(avatarScale)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(RentOutColors.Secondary, RentOutColors.SecondaryLight)
                            )
                        )
                        .border(
                            width = 2.dp,
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
                        val initials = user.name
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercaseChar().toString() }
                            .ifBlank { "L" }
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // ── Info cards ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    "Account Information",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )

                ProfileInfoCard {
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = user.name.ifBlank { "—" },
                        iconTint = RentOutColors.IconBlue
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user.email.ifBlank { "—" },
                        iconTint = RentOutColors.IconTeal
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = user.phoneNumber.ifBlank { "Not provided" },
                        iconTint = RentOutColors.IconGreen
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    // Gender — always shown; "Not provided" when blank
                    val genderTint = when (user.gender) {
                        "Male"              -> Color(0xFF1565C0)
                        "Female"            -> Color(0xFFAD1457)
                        "Transgender"       -> Color(0xFF6A1B9A)
                        "Prefer not to say" -> Color(0xFF37474F)
                        else                -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val genderEmoji = when (user.gender) {
                        "Male"              -> "👨 "
                        "Female"            -> "👩 "
                        "Transgender"       -> "🏳️\u200D⚧️ "
                        "Prefer not to say" -> "🤝 "
                        else                -> ""
                    }
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Gender",
                        value = if (user.gender.isNotBlank()) "$genderEmoji${user.gender}" else "Not provided",
                        iconTint = if (user.gender.isNotBlank()) genderTint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    // National ID — always shown; "Not provided" when blank
                    ProfileInfoRow(
                        icon = Icons.Default.Badge,
                        label = "National ID",
                        value = user.nationalId.ifBlank { "Not provided" },
                        iconTint = RentOutColors.IconPurple
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.Shield,
                        label = "Account Status",
                        value = user.status.replaceFirstChar { it.uppercaseChar() },
                        iconTint = if (user.status == "active") RentOutColors.StatusApproved else RentOutColors.StatusRejected,
                        valueColor = if (user.status == "active") RentOutColors.StatusApproved else RentOutColors.StatusRejected
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Account",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )

                ProfileInfoCard {
                    ProfileInfoRow(
                        icon = Icons.Default.Badge,
                        label = "Role",
                        value = "${user.providerEmoji} ${user.providerDisplayName}",
                        iconTint = RentOutColors.IconPurple
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Member Since",
                        value = if (user.createdAt > 0L) {
                            // Convert epoch millis → readable date without java.time (KMP-safe)
                            val totalSecs  = user.createdAt / 1000L
                            val totalDays  = (totalSecs / 86400L).toInt()
                            // Gregorian approximation accurate to the month
                            var year = 1970; var days = totalDays
                            while (true) {
                                val diy = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
                                if (days < diy) break
                                days -= diy; year++
                            }
                            val monthLengths = intArrayOf(31, if (year % 4 == 0) 29 else 28,
                                31,30,31,30,31,31,30,31,30,31)
                            var month = 0
                            for (m in monthLengths.indices) {
                                if (days < monthLengths[m]) { month = m + 1; break }
                                days -= monthLengths[m]
                            }
                            val monthName = listOf("Jan","Feb","Mar","Apr","May","Jun",
                                "Jul","Aug","Sep","Oct","Nov","Dec").getOrElse(month - 1) { "—" }
                            "$monthName $year"
                        } else "—",
                        iconTint = RentOutColors.IconAmber
                    )
                }

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = user.isAgent || user.isBrokerage,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section title
                            Text(
                                text = if (user.isAgent) "🤝 Agent Details" else "🏢 Company Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            // Agent fields
                            if (user.isAgent) {
                                ProfileInfoRow(icon = Icons.Default.Badge, label = "License Number", value = user.agentLicenseNumber.ifBlank { "—" }, iconTint = RentOutColors.IconBlue)
                                ProfileInfoRow(icon = Icons.Default.DateRange, label = "Years of Experience", value = user.yearsOfExperience.ifBlank { "—" }, iconTint = RentOutColors.IconAmber)
                            }
                            
                            // Brokerage fields
                            if (user.isBrokerage) {

                                // ── Company Logo ──────────────────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(20.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (user.companyLogoUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = user.companyLogoUrl,
                                                contentDescription = "Company logo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Business,
                                                contentDescription = "Company logo placeholder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(horizontal = 0.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                ProfileInfoRow(icon = Icons.Default.Business, label = "Company Name", value = user.companyName.ifBlank { "—" }, iconTint = RentOutColors.IconBlue)
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                ProfileInfoRow(icon = Icons.Default.Info, label = "Reg. Number", value = user.companyRegNumber.ifBlank { "—" }, iconTint = RentOutColors.IconTeal)
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                ProfileInfoRow(icon = Icons.Default.Phone, label = "Company Phone", value = user.companyPhone.ifBlank { "—" }, iconTint = RentOutColors.IconGreen)
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                ProfileInfoRow(icon = Icons.Default.Email, label = "Company Email", value = user.companyEmail.ifBlank { "—" }, iconTint = RentOutColors.IconTeal)
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                ProfileInfoRow(icon = Icons.Default.LocationOn, label = "Office Address", value = listOf(user.companyStreet, user.companyCity, user.companyCountry).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "—" }, iconTint = RentOutColors.IconGreen)
                                if (user.taxId.isNotBlank()) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    ProfileInfoRow(icon = Icons.Default.Receipt, label = "Tax ID", value = user.taxId, iconTint = RentOutColors.IconAmber)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Logout button ─────────────────────────────────────────────
                val logoutInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isLogoutPressed by logoutInteraction.collectIsPressedAsState()
                val logoutScale by animateFloatAsState(
                    targetValue = if (isLogoutPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "logout_scale"
                )

                Button(
                    onClick = { showLogoutDialog = true },
                    interactionSource = logoutInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .scale(logoutScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Log Out", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(12.dp))

                // ── Delete Account button ─────────────────────────────────────
                val deleteInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isDeletePressed by deleteInteraction.collectIsPressedAsState()
                val deleteScale by animateFloatAsState(
                    targetValue = if (isDeletePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "delete_scale"
                )

                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    interactionSource = deleteInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .scale(deleteScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RentOutColors.IconRose
                    ),
                    border = BorderStroke(1.5.dp, RentOutColors.IconRose)
                ) {
                    Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Delete Account", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Log Out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // ── Delete Account confirmation dialog ────────────────────────────────────
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
}

// ── Reusable card wrapper ─────────────────────────────────────────────────────
@Composable
private fun ProfileInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Column(content = content) }
    )
}

// ── Single info row ───────────────────────────────────────────────────────────
@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, color = valueColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
