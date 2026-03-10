package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.RentOutColors

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status.lowercase()) {
        "approved" -> Triple("✅ Approved",  RentOutColors.StatusApproved.copy(alpha = 0.15f), RentOutColors.StatusApproved)
        "pending"  -> Triple("⏳ Pending",   RentOutColors.StatusPending.copy(alpha = 0.15f),  RentOutColors.StatusPending)
        "rejected" -> Triple("❌ Rejected",  RentOutColors.StatusRejected.copy(alpha = 0.15f), RentOutColors.StatusRejected)
        else       -> Triple(status,          Color.Gray.copy(alpha = 0.15f),                  Color.Gray)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VerifiedBadge(providerSubtype: String = "", modifier: Modifier = Modifier) {
    val label = when (providerSubtype) {
        "agent"     -> "✓ Verified by Agent"
        "brokerage" -> "✓ Verified by Brokerage"
        else        -> "✓ Verified by Landlord"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RentOutColors.StatusApproved.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = RentOutColors.StatusApproved,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AvailabilityBadge(isAvailable: Boolean, modifier: Modifier = Modifier) {
    val (label, bg, fg) = if (isAvailable)
        Triple("🟢 Available", RentOutColors.StatusApproved.copy(alpha = 0.12f), RentOutColors.StatusApproved)
    else
        Triple("🔴 Unavailable", RentOutColors.StatusRejected.copy(alpha = 0.12f), RentOutColors.StatusRejected)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
