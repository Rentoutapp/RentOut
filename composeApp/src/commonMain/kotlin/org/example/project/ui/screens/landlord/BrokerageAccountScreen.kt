@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.model.BrokerageLedgerEntry
import org.example.project.data.model.User
import org.example.project.data.model.isBrokerageLowFloat
import org.example.project.presentation.BrokerageTopUpState
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.components.RentOutSecondaryButton
import org.example.project.ui.theme.RentOutColors

@Composable
fun BrokerageAccountScreen(
    user: User,
    ledgerEntries: List<BrokerageLedgerEntry>,
    isLoading: Boolean,
    topUpState: BrokerageTopUpState,
    onTopUp: (Double) -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
    onResetTopUpState: () -> Unit
) {
    var showTopUpDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val totalDeductions = ledgerEntries.filter { it.direction == "debit" }.sumOf { it.amount }
    val totalTopUps = ledgerEntries.filter { it.direction == "credit" }.sumOf { it.amount }
    val unlockDeductionCount = ledgerEntries.count { it.type == "unlock_deduction" }
    val safeRange = (user.brokerageFloatBalanceUsd - user.brokerageMinimumFloatUsd).coerceAtLeast(0.0)
    val lastUpdated = listOf(user.brokerageLastDeductionAt, user.brokerageLastTopUpAt).maxOrNull() ?: 0L
    val financeOverviewItems = remember(user, unlockDeductionCount, totalDeductions, totalTopUps, lastUpdated, safeRange) {
        listOf(
            BrokerageOverviewItem(
                title = "Unlock deductions",
                value = unlockDeductionCount.toString(),
                icon = Icons.Default.Payments,
                tint = RentOutColors.IconRose
            ),
            BrokerageOverviewItem(
                title = "Total debited",
                value = "$${"%.2f".format(totalDeductions)}",
                icon = Icons.Default.TrendingDown,
                tint = RentOutColors.IconAmber
            ),
            BrokerageOverviewItem(
                title = "Top-ups",
                value = "$${"%.2f".format(totalTopUps)}",
                icon = Icons.Default.Savings,
                tint = RentOutColors.StatusApproved
            ),
            BrokerageOverviewItem(
                title = "Last update",
                value = formatBrokerageDate(lastUpdated),
                icon = Icons.Default.Schedule,
                tint = RentOutColors.IconPurple
            ),
            BrokerageOverviewItem(
                title = "Subscription",
                value = "$${"%.0f".format(user.brokerageSubscriptionFeeUsd)}",
                icon = Icons.Default.AccountBalanceWallet,
                tint = RentOutColors.Primary
            ),
            BrokerageOverviewItem(
                title = "Minimum float",
                value = "$${"%.0f".format(user.brokerageMinimumFloatUsd)}",
                icon = Icons.Default.Security,
                tint = RentOutColors.Secondary
            ),
            BrokerageOverviewItem(
                title = "Safe buffer",
                value = "$${"%.2f".format(safeRange)}",
                icon = Icons.Default.Shield,
                tint = if (safeRange > 0.0) RentOutColors.StatusApproved else RentOutColors.IconAmber
            )
        )
    }

    LaunchedEffect(topUpState) {
        when (val state = topUpState) {
            is BrokerageTopUpState.Success -> showTopUpDialog = false
            is BrokerageTopUpState.AwaitingCheckout -> {
                showTopUpDialog = true
                runCatching { uriHandler.openUri(state.checkoutUrl) }
            }
            else -> Unit
        }
    }

    if (showTopUpDialog) {
        BrokerageTopUpDialog(
            currentBalance = user.brokerageFloatBalanceUsd,
            minBalance = user.brokerageMinimumFloatUsd,
            topUpState = topUpState,
            onConfirm = onTopUp,
            onDismiss = {
                showTopUpDialog = false
                onResetTopUpState()
            }
        )
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Float progress with animated value
    val floatProgress = if (user.brokerageSubscriptionFeeUsd > 0.0)
        (user.brokerageFloatBalanceUsd / user.brokerageSubscriptionFeeUsd).toFloat().coerceIn(0f, 1f)
    else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = floatProgress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "float_progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // ── Hero header ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(RentOutColors.PrimaryDark, RentOutColors.Primary)))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Back + title row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Brokerage Account", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                user.companyName.ifBlank { user.name.ifBlank { "Brokerage" } },
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // ── Float balance card — dark-mode-aware ──────────────────
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                                // Balance + status badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "Insurance float",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "$${"%.2f".format(user.brokerageFloatBalanceUsd)}",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (user.brokerageIsFrozen) RentOutColors.IconRose
                                                    else if (user.isBrokerageLowFloat) RentOutColors.IconAmber
                                                    else RentOutColors.Secondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = when {
                                            user.brokerageIsFrozen    -> RentOutColors.IconRose.copy(alpha = 0.14f)
                                            user.isBrokerageLowFloat  -> RentOutColors.IconAmber.copy(alpha = 0.14f)
                                            else                       -> RentOutColors.StatusApproved.copy(alpha = 0.14f)
                                        }
                                    ) {
                                        Text(
                                            when {
                                                user.brokerageIsFrozen   -> "❄️ Frozen"
                                                user.isBrokerageLowFloat -> "⚠️ Low float"
                                                else                      -> "✅ Healthy"
                                            },
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            color = when {
                                                user.brokerageIsFrozen   -> RentOutColors.IconRose
                                                user.isBrokerageLowFloat -> RentOutColors.IconAmber
                                                else                      -> RentOutColors.StatusApproved
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Animated progress bar
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = animatedProgress,
                                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                                        color = if (user.brokerageIsFrozen) RentOutColors.IconRose
                                                else if (user.isBrokerageLowFloat) RentOutColors.IconAmber
                                                else RentOutColors.Secondary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            "$${"%.2f".format(user.brokerageFloatBalanceUsd)} remaining",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "of $${"%.0f".format(user.brokerageSubscriptionFeeUsd)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Metric chips row
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BrokerageMetricChip(Icons.Default.AccountBalanceWallet, "Subscription", "$${"%.0f".format(user.brokerageSubscriptionFeeUsd)}", Modifier.weight(1f))
                                    BrokerageMetricChip(Icons.Default.Security, "Minimum", "$${"%.0f".format(user.brokerageMinimumFloatUsd)}", Modifier.weight(1f))
                                    BrokerageMetricChip(Icons.Default.TrendingUp, "Buffer", "$${"%.2f".format(safeRange)}", Modifier.weight(1f))
                                }

                                // Frozen / low-float alert banner
                                AnimatedVisibility(visible = user.brokerageIsFrozen || user.isBrokerageLowFloat) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (user.brokerageIsFrozen)
                                            RentOutColors.IconRose.copy(alpha = 0.10f)
                                        else
                                            RentOutColors.IconAmber.copy(alpha = 0.10f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                if (user.brokerageIsFrozen) Icons.Default.AcUnit else Icons.Default.Warning,
                                                null,
                                                tint = if (user.brokerageIsFrozen) RentOutColors.IconRose else RentOutColors.IconAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                if (user.brokerageIsFrozen)
                                                    "Tenant unlocks are frozen. The insurance float must stay at or above $${"%.0f".format(user.brokerageMinimumFloatUsd)}. Top up now to restore acquisitions."
                                                else
                                                    "Your float is nearing the safety floor. Top up before it drops below $${"%.0f".format(user.brokerageMinimumFloatUsd)}.",
                                                color = if (user.brokerageIsFrozen) RentOutColors.IconRose else RentOutColors.IconAmber,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }

                                // Action buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val topUpInteraction = remember { MutableInteractionSource() }
                                    val isTopUpPressed by topUpInteraction.collectIsPressedAsState()
                                    val topUpScale by animateFloatAsState(
                                        if (isTopUpPressed) 0.94f else 1f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "topup_scale"
                                    )
                                    RentOutPrimaryButton(
                                        text = if (user.brokerageIsFrozen) "💳 Top up & unfreeze" else "➕ Top up float",
                                        onClick = { showTopUpDialog = true },
                                        modifier = Modifier.weight(1f).scale(topUpScale)
                                    )
                                    RentOutSecondaryButton(
                                        text = "📜 History",
                                        onClick = onOpenHistory,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(tween(600, delayMillis = 200)) { 30 }
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Finance Overview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Swipe horizontally to review every finance metric.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(financeOverviewItems) { item ->
                            BrokerageInsightCard(
                                Modifier.width(220.dp),
                                item.title,
                                item.value,
                                item.icon,
                                item.tint
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "See all",
                    color = RentOutColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenHistory() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (isLoading && ledgerEntries.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (ledgerEntries.isEmpty()) {
            item {
                BrokerageEmptyState(
                    title = "No finance activity yet",
                    subtitle = "Your $100 float is ready. Unlock deductions and future top-ups will appear here."
                )
            }
        } else {
            items(ledgerEntries.take(6), key = { it.id }) { entry ->
                BrokerageLedgerCard(entry = entry)
            }
        }
    }
}

private data class BrokerageOverviewItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun BrokeragePaymentHistoryScreen(
    user: User,
    ledgerEntries: List<BrokerageLedgerEntry>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    val filteredEntries = remember(ledgerEntries, selectedFilter) {
        when (selectedFilter) {
            "debits" -> ledgerEntries.filter { it.direction == "debit" }
            "credits" -> ledgerEntries.filter { it.direction == "credit" }
            "unlocks" -> ledgerEntries.filter { it.type == "unlock_deduction" }
            "topups" -> ledgerEntries.filter { it.type == "top_up" }
            else -> ledgerEntries
        }
    }
    val debitCount = ledgerEntries.count { it.direction == "debit" }
    val creditCount = ledgerEntries.count { it.direction == "credit" }
    val lastFour = ledgerEntries.take(4)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // ── Subtle header: short gradient strip for back/title,
            // then chips sit on the page background for full contrast ──────────
            Column {
                // Title strip — softened gradient, less dominant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    RentOutColors.Primary.copy(alpha = 0.92f),
                                    RentOutColors.Primary.copy(alpha = 0.72f)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.20f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        Text(
                            "Finance Records",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${ledgerEntries.size} records • ${user.companyName.ifBlank { user.name }}",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 13.sp
                        )
                    }
                }

                // ── Filter chips on surface background — always high contrast ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryFilterChip("All",     selectedFilter == "all")     { selectedFilter = "all" }
                        HistoryFilterChip("Debits",  selectedFilter == "debits")  { selectedFilter = "debits" }
                        HistoryFilterChip("Credits", selectedFilter == "credits") { selectedFilter = "credits" }
                        HistoryFilterChip("Unlocks", selectedFilter == "unlocks") { selectedFilter = "unlocks" }
                        HistoryFilterChip("Top-ups", selectedFilter == "topups")  { selectedFilter = "topups" }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrokerageInsightCard(Modifier.weight(1f), "Debits", debitCount.toString(), Icons.Default.RemoveCircle, RentOutColors.IconRose)
                BrokerageInsightCard(Modifier.weight(1f), "Credits", creditCount.toString(), Icons.Default.AddCircle, RentOutColors.StatusApproved)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Recent Balance Movement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (lastFour.isEmpty()) {
                        Text(
                            "No ledger activity yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Bar chart: amount label above bar, date label below
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            lastFour.reversed().forEach { entry ->
                                val barColor = if (entry.direction == "credit")
                                    RentOutColors.StatusApproved else RentOutColors.IconRose
                                val ratio = if (user.brokerageSubscriptionFeeUsd > 0)
                                    (entry.balanceAfter / user.brokerageSubscriptionFeeUsd).coerceIn(0.08, 1.0)
                                else 0.08
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Amount above bar
                                    Text(
                                        "${'$'}${"%.0f".format(entry.balanceAfter)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor,
                                        maxLines = 1
                                    )
                                    // The bar itself
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((ratio * 88).dp)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                            .background(barColor.copy(alpha = 0.85f))
                                    )
                                    // Date below bar
                                    Text(
                                        formatBrokerageDate(entry.createdAt).take(6), // e.g. "Mar 12"
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    // Debit / Credit label
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = barColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            if (entry.direction == "credit") "CR" else "DR",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = barColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Balance after each of the last ${lastFour.size} transactions.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoading && ledgerEntries.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredEntries.isEmpty()) {
            item {
                BrokerageEmptyState(
                    title = "No brokerage ledger entries yet",
                    subtitle = "Every unlock deduction and top-up will be listed here with before/after balances."
                )
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                BrokerageLedgerCard(entry = entry)
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale_$label"
    )
    // Theme-aware colors: selected = solid Primary with white text (readable in both modes)
    // Unselected = surfaceVariant with onSurfaceVariant text (standard Material contrast)
    val containerColor by animateColorAsState(
        targetValue = if (selected) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "chip_bg_$label"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chip_label_$label"
    )
    Surface(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(50.dp),
        color = containerColor,
        border = if (!selected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}

@Composable
private fun BrokerageMetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = RentOutColors.Primary)
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BrokerageInsightCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "insight_scale"
    )
    Card(
        modifier = modifier.scale(cardScale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BrokerageLedgerCard(entry: BrokerageLedgerEntry) {
    val tint = if (entry.direction == "credit") RentOutColors.StatusApproved else RentOutColors.IconRose
    val icon = when (entry.type) {
        "top_up" -> Icons.Default.AddCircle
        "subscription_activation" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.RemoveCircle
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ledger_scale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(cardScale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Left accent bar coloured by direction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(tint, tint.copy(alpha = 0f)))
                )
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.title.ifBlank { entry.type.replace('_', ' ').replaceFirstChar { it.uppercase() } },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        entry.description.ifBlank { "Balance $${"%.2f".format(entry.balanceBefore)} → $${"%.2f".format(entry.balanceAfter)}" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (entry.direction == "credit") "+" else "-") + "$${"%.2f".format(entry.amount)}",
                        color = tint,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "After: $${"%.2f".format(entry.balanceAfter)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatBrokerageDate(entry.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tint.copy(alpha = 0.10f)
                ) {
                    Text(
                        if (entry.direction == "credit") "Credit" else "Debit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrokerageEmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RentOutColors.Primary, modifier = Modifier.size(44.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun BrokerageTopUpDialog(
    currentBalance: Double,
    minBalance: Double,
    topUpState: BrokerageTopUpState,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("25") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top up brokerage float") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Keep the brokerage insurance float above $${"%.0f".format(minBalance)} so tenant acquisitions stay active.")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (USD)") },
                    singleLine = true,
                    leadingIcon = { Text("$") }
                )
                Text("Current: $${"%.2f".format(currentBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (topUpState) {
                    is BrokerageTopUpState.Error -> Text(topUpState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    is BrokerageTopUpState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is BrokerageTopUpState.AwaitingCheckout -> {
                        Text(
                            (topUpState as BrokerageTopUpState.AwaitingCheckout).message.ifBlank {
                                "Checkout opened. Complete the payment page and return to the app."
                            },
                            color = RentOutColors.Primary,
                            fontSize = 12.sp
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is BrokerageTopUpState.Success -> Text("Top-up recorded successfully.", color = RentOutColors.StatusApproved, fontSize = 12.sp)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            RentOutPrimaryButton(
                text = "Confirm top-up",
                onClick = { onConfirm(amountText.toDoubleOrNull() ?: 0.0) },
                isLoading = topUpState is BrokerageTopUpState.Loading
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(22.dp)
    )
}

private fun formatBrokerageDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[dateTime.monthNumber - 1]
    return "$month ${dateTime.dayOfMonth}, ${dateTime.year}"
}
