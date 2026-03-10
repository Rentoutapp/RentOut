package org.example.project.ui.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
actual fun PlatformDatePickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val initialCalendar = remember(initialDate) {
        Calendar.getInstance().apply {
            if (initialDate.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.isLenient = false
                    time = sdf.parse(initialDate) ?: time
                } catch (_: Exception) {
                    // Keep current date if parsing fails
                }
            }
        }
    }

    var handled by remember { mutableStateOf(false) }

    DisposableEffect(context, initialDate) {
        handled = false

        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                if (!handled) {
                    handled = true
                    onDateSelected(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    )
                }
            },
            initialCalendar.get(Calendar.YEAR),
            initialCalendar.get(Calendar.MONTH),
            initialCalendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.setOnDismissListener {
            if (!handled) {
                handled = true
                onDismiss()
            }
        }

        dialog.show()

        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}
