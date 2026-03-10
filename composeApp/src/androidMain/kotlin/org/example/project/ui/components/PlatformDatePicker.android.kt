package org.example.project.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.ui.theme.RentOutColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initialDate ("yyyy-MM-dd") into epoch millis for the state
    val initialMillis = remember(initialDate) {
        if (initialDate.isNotBlank()) {
            try {
                val parts = initialDate.split("-")
                if (parts.size == 3) {
                    val cal = Calendar.getInstance()
                    cal.set(
                        parts[0].toInt(),
                        parts[1].toInt() - 1,
                        parts[2].toInt(),
                        12, 0, 0
                    )
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else null
            } catch (_: Exception) { null }
        } else null
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    // Brand-coloured DatePicker theme: primary = RentOutColors.Primary
    val brandColors = MaterialTheme.colorScheme.copy(
        primary          = RentOutColors.Primary,
        onPrimary        = Color.White,
        primaryContainer = RentOutColors.SurfaceVariant,
        onPrimaryContainer = RentOutColors.PrimaryDark,
        secondary        = RentOutColors.Secondary,
        surface          = MaterialTheme.colorScheme.surface,
        onSurface        = MaterialTheme.colorScheme.onSurface,
        surfaceVariant   = RentOutColors.SurfaceVariant,
        onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
        outline          = MaterialTheme.colorScheme.outline,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MaterialTheme(colorScheme = brandColors) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    // ── Branded gradient header ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(RentOutColors.Primary, RentOutColors.PrimaryLight)
                                ),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Select Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            // Live preview of chosen date
                            val previewText = remember(datePickerState.selectedDateMillis) {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                                    val months = listOf(
                                        "January","February","March","April","May","June",
                                        "July","August","September","October","November","December"
                                    )
                                    val d = cal.get(Calendar.DAY_OF_MONTH)
                                    val m = cal.get(Calendar.MONTH)
                                    val y = cal.get(Calendar.YEAR)
                                    "$d ${months[m]} $y"
                                } else "No date selected"
                            }
                            Text(
                                text = previewText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // ── Material3 DatePicker (no title — we have our own header) ──
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = DatePickerDefaults.colors(
                            containerColor             = MaterialTheme.colorScheme.surface,
                            titleContentColor          = RentOutColors.Primary,
                            headlineContentColor       = RentOutColors.Primary,
                            weekdayContentColor        = RentOutColors.Primary.copy(alpha = 0.6f),
                            yearContentColor           = MaterialTheme.colorScheme.onSurface,
                            currentYearContentColor    = RentOutColors.Primary,
                            selectedYearContentColor   = Color.White,
                            selectedYearContainerColor = RentOutColors.Primary,
                            dayContentColor            = MaterialTheme.colorScheme.onSurface,
                            selectedDayContentColor    = Color.White,
                            selectedDayContainerColor  = RentOutColors.Primary,
                            todayContentColor          = RentOutColors.Primary,
                            todayDateBorderColor       = RentOutColors.Primary,
                            disabledDayContentColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            dayInSelectionRangeContentColor   = Color.White,
                            dayInSelectionRangeContainerColor = RentOutColors.PrimaryLight,
                        )
                    )

                    // ── Action buttons ───────────────────────────────────────
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel
                        TextButton(onClick = onDismiss) {
                            Text(
                                "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        // Confirm — animated scale button using RentOutColors.Primary
                        var confirmPressed by remember { mutableStateOf(false) }
                        val confirmScale by animateFloatAsState(
                            targetValue = if (confirmPressed) 0.93f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "confirm_scale"
                        )
                        val isDateChosen = datePickerState.selectedDateMillis != null
                        Button(
                            onClick = {
                                confirmPressed = true
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                                    val y = cal.get(Calendar.YEAR)
                                    val m = cal.get(Calendar.MONTH) + 1
                                    val d = cal.get(Calendar.DAY_OF_MONTH)
                                    onDateSelected(
                                        "%04d-%02d-%02d".format(y, m, d)
                                    )
                                }
                            },
                            enabled = isDateChosen,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RentOutColors.Primary,
                                contentColor   = Color.White,
                                disabledContainerColor = RentOutColors.Primary.copy(alpha = 0.3f),
                                disabledContentColor   = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.scale(confirmScale)
                        ) {
                            Text("Confirm Date", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
