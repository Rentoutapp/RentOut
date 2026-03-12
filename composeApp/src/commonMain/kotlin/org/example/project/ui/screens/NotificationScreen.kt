@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.AppNotification
import org.example.project.data.model.typeColorInt
import org.example.project.data.model.typeEmoji
import org.example.project.presentation.NotificationListState
import org.example.project.ui.theme.RentOutColors

// ── Design tokens ─────────────────────────────────────────────────────────────
private val NavyDark  = Color(0xFF0A1F35)
private val NavyMid   = Color(0xFF0F2A4A)
private val NavyLight = Color(0xFF1A3F6F)
private val AccentRed = Color(0xFFE53935)

// ── Tab selection ─────────────────────────────────────────────────────────────
private enum class NotifTab { UNREAD, ALL }

// ─────────────────────────────────────────────────────────────────────────────
//  Main NotificationScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    state: NotificationListState,
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val listState   = rememberLazyListState()
    var activeTab   by remember { mutableStateOf(NotifTab.UNREAD) }
    var visible     by remember { mutableStateOf(false) }
    val isDark      = isSystemInDarkTheme()

    LaunchedEffect(Unit) { visible = true }

    // Switch to ALL tab when there are no unread notifications
    LaunchedEffect(unreadCount) {
        if (unreadCount == 0 && activeTab == NotifTab.UNREAD) {
            activeTab = NotifTab.ALL
        }
    }

    val bgColor = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF0F4FA)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            NotificationTopBar(
                unreadCount  = unreadCount,
                activeTab    = activeTab,
                onTabSelect  = { activeTab = it },
                onBack       = onBack,
                onMarkAllRead = onMarkAllRead,
                isDark       = isDark
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
            label = "notif_state"
        ) { s ->
            when (s) {
                is NotificationListState.Loading -> NotifLoadingState(padding)
                is NotificationListState.Error   -> NotifErrorState(s.message, padding)
                is NotificationListState.Empty   -> EmptyNotificationsState(
                    Modifier.fillMaxSize().padding(padding)
                )
                is NotificationListState.Success -> {
                    val unread = s.notifications.filter { !it.isRead }
                    val read   = s.notifications.filter {  it.isRead }
                    val displayed = when (activeTab) {
                        NotifTab.UNREAD -> unread
                        NotifTab.ALL    -> s.notifications
                    }

                    if (displayed.isEmpty()) {
                        EmptyTabState(
                            tab      = activeTab,
                            modifier = Modifier.fillMaxSize().padding(padding)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical   = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ── Section summary chip ───────────────────────────
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(400)) +
                                            slideInVertically(tween(400)) { -16 }
                                ) {
                                    NotifSummaryRow(
                                        total        = s.notifications.size,
                                        unreadCount  = unread.size,
                                        readCount    = read.size,
                                        activeTab    = activeTab,
                                        isDark       = isDark
                                    )
                                }
                            }

                            // ── Notification cards ─────────────────────────────
                            itemsIndexed(
                                displayed,
                                key = { _, n -> n.id }
                            ) { index, notif ->
                                val delay = (index * 35).coerceAtMost(360)
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(320, delayMillis = delay)) +
                                            slideInVertically(tween(320, delayMillis = delay)) { 40 }
                                ) {
                                    NotificationCard(
                                        notification = notif,
                                        isDark       = isDark,
                                        onMarkRead   = { onMarkRead(notif.id) },
                                        onDelete     = { onDelete(notif.id) }
                                    )
                                }
                            }

                            item { Spacer(Modifier.height(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top bar — gradient + tabs + badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationTopBar(
    unreadCount:  Int,
    activeTab:    NotifTab,
    onTabSelect:  (NotifTab) -> Unit,
    onBack:       () -> Unit,
    onMarkAllRead: () -> Unit,
    isDark:       Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(NavyDark, NavyMid, NavyLight)
                )
            )
            .statusBarsPadding()
    ) {
        // ── Row 1: back + title + mark-all ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notifications",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )
                AnimatedContent(
                    targetState = unreadCount,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { it } + fadeOut())
                    },
                    label = "unread_count_anim"
                ) { count ->
                    if (count > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Red count pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentRed)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (count > 99) "99+" else "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                "unread",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    } else {
                        Text(
                            "All caught up ✓",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Mark all read button
            AnimatedVisibility(
                visible = unreadCount > 0,
                enter = fadeIn() + slideInHorizontally { it },
                exit  = fadeOut() + slideOutHorizontally { it }
            ) {
                TextButton(
                    onClick = onMarkAllRead,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.90f)
                    )
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Mark all read",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Row 2: Unread / All tabs ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NotifTabPill(
                label    = "Unread",
                count    = unreadCount,
                selected = activeTab == NotifTab.UNREAD,
                onClick  = { onTabSelect(NotifTab.UNREAD) }
            )
            NotifTabPill(
                label    = "All",
                count    = null,
                selected = activeTab == NotifTab.ALL,
                onClick  = { onTabSelect(NotifTab.ALL) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab pill button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifTabPill(
    label:    String,
    count:    Int?,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tab_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
        animationSpec = tween(200),
        label = "tab_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) NavyDark else Color.White.copy(alpha = 0.80f),
        animationSpec = tween(200),
        label = "tab_text"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            // Show red count badge only on Unread tab when count > 0
            if (count != null && count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) AccentRed else AccentRed.copy(alpha = 0.85f))
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (count > 99) "99+" else "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Summary row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotifSummaryRow(
    total:       Int,
    unreadCount: Int,
    readCount:   Int,
    activeTab:   NotifTab,
    isDark:      Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label  = if (activeTab == NotifTab.UNREAD) "Showing unread" else "$total total",
            color  = RentOutColors.Primary,
            isDark = isDark
        )
        if (unreadCount > 0) {
            SummaryChip(
                label  = "$unreadCount unread",
                color  = AccentRed,
                isDark = isDark,
                isBold = true
            )
        }
        if (readCount > 0 && activeTab == NotifTab.ALL) {
            SummaryChip(
                label  = "$readCount read",
                color  = Color(0xFF718096),
                isDark = isDark
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label:  String,
    color:  Color,
    isDark: Boolean,
    isBold: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (isDark) 0.22f else 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isDark) color.copy(alpha = 0.95f) else color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Individual notification card — modern, interactive, dark-mode-aware
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationCard(
    notification: AppNotification,
    isDark:       Boolean,
    onMarkRead:   () -> Unit,
    onDelete:     () -> Unit
) {
    val accentColor   = Color(notification.typeColorInt())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Press scale
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    // Card background — unread is slightly tinted, read is neutral
    val cardBg = when {
        !notification.isRead && isDark  -> Color(0xFF0F2540)
        !notification.isRead && !isDark -> Color(0xFFEEF6FF)
        isDark                          -> Color(0xFF111D2B)
        else                            -> Color(0xFFFFFFFF)
    }
    val cardBorder = if (!notification.isRead) accentColor.copy(alpha = 0.35f)
                     else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
    ) {
        // ── Unread section label (shown only once above first unread card) ────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (!notification.isRead) 6.dp else 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = accentColor.copy(alpha = 0.08f),
                    spotColor   = accentColor.copy(alpha = 0.12f)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (!notification.isRead) onMarkRead()
                    isExpanded = !isExpanded
                },
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(
                width = if (!notification.isRead) 1.dp else 0.dp,
                color = cardBorder
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            // Left accent stripe for unread
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.padding(
                    start = 14.dp, end = 14.dp,
                    top = if (!notification.isRead) 12.dp else 14.dp,
                    bottom = 14.dp
                ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Icon badge ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(notification.typeEmoji(), fontSize = 24.sp)
                    // Pulsing unread dot on icon
                    if (!notification.isRead) {
                        val pulse = rememberInfiniteTransition(label = "dot_pulse")
                        val dotScale by pulse.animateFloat(
                            initialValue = 0.8f, targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                tween(900, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(10.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }

                // ── Content ──────────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    // Title row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            notification.title,
                            fontSize = 14.sp,
                            fontWeight = if (!notification.isRead) FontWeight.ExtraBold
                                        else FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0D1B2A),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Time
                        Text(
                            formatNotificationTime(notification.createdAt),
                            fontSize = 10.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.45f)
                                    else Color(0xFF9AA5B1),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Message — expandable
                    Text(
                        notification.message,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.72f)
                                else Color(0xFF4A5568),
                        lineHeight = 17.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = if (isExpanded) TextOverflow.Visible
                                   else TextOverflow.Ellipsis
                    )

                    // Property tag
                    if (notification.propertyTitle.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = if (isDark) 0.20f else 0.10f))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "🏠 ${notification.propertyTitle}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) accentColor.copy(alpha = 0.95f) else accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Action row ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Role badge
                        if (notification.role.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.09f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    notification.role.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) accentColor.copy(alpha = 0.90f) else accentColor
                                )
                            }
                        } else Spacer(Modifier.width(1.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mark read pill
                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                                        .clickable(onClick = onMarkRead)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            modifier = Modifier.size(11.dp),
                                            tint = accentColor
                                        )
                                        Text(
                                            "Mark read",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                            // Delete button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.07f)
                                        else Color(0xFFF0F0F0)
                                    )
                                    .clickable { showDeleteConfirm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    "Delete",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isDark) Color.White.copy(alpha = 0.45f)
                                           else Color(0xFFADB5BD)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirm dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1A2840) else Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete notification?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0D1B2A)
                )
            },
            text = {
                Text(
                    "This will permanently remove this notification. You can't undo this.",
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.70f)
                            else Color(0xFF4A5568),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { onDelete(); showDeleteConfirm = false }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFF0F0F0)
                        )
                        .clickable { showDeleteConfirm = false }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Cancel",
                        color = if (isDark) Color.White.copy(alpha = 0.80f) else Color(0xFF4A5568),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty states
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyTabState(tab: NotifTab, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text(if (tab == NotifTab.UNREAD) "✅" else "🔔", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (tab == NotifTab.UNREAD) "No unread notifications" else "No notifications yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (tab == NotifTab.UNREAD)
                    "You've read everything. Switch to All to see past notifications."
                else
                    "When something important happens, you'll see it here.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "bell_pulse")
    val bellScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bell"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(bellScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(RentOutColors.Primary.copy(alpha = 0.14f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) { Text("🔔", fontSize = 52.sp) }
            Spacer(Modifier.height(24.dp))
            Text(
                "You're all caught up!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No notifications yet.\nWhen something important happens, you'll see it here.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun NotifLoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = RentOutColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading notifications…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NotifErrorState(message: String, padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Time formatting
// ─────────────────────────────────────────────────────────────────────────────
private fun formatNotificationTime(epochMillis: Long): String {
    if (epochMillis == 0L) return ""
    val now     = System.currentTimeMillis()
    val diff    = now - epochMillis
    val minutes = diff / 60_000
    val hours   = diff / 3_600_000
    val days    = diff / 86_400_000
    return when {
        minutes < 1   -> "Just now"
        minutes < 60  -> "${minutes}m ago"
        hours   < 24  -> "${hours}h ago"
        days    < 7   -> "${days}d ago"
        days    < 30  -> "${days / 7}w ago"
        else          -> "${days / 30}mo ago"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NotificationBadgeBox — reusable pulsing red badge overlay
//  (used on dashboard bell icons in all dashboards)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NotificationBadgeBox(
    count:   Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        content()
        if (count > 0) {
            val pulse = rememberInfiniteTransition(label = "badge_pulse")
            val badgeScale by pulse.animateFloat(
                initialValue = 1f, targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse
                ),
                label = "badge_scale"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .scale(badgeScale)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(AccentRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
            }
        }
    }
}
