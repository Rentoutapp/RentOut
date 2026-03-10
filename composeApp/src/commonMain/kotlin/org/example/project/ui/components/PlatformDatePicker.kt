package org.example.project.ui.components

import androidx.compose.runtime.Composable

/**
 * Platform-specific date picker dialog.
 * Shows a native/Material3 date picker on Android, and a simple text-input fallback on iOS.
 *
 * @param initialDate Pre-selected date in "yyyy-MM-dd" format, or blank for none.
 * @param onDateSelected Called with the selected date in "yyyy-MM-dd" format.
 * @param onDismiss Called when the dialog is dismissed without a selection.
 */
@Composable
expect fun PlatformDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
)
